package com.laker.postman.service.http;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.*;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.collections.GroupInheritanceHelper;
import com.laker.postman.service.setting.SettingManager;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BASIC;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BEARER;

/**
 * 负责构建 PreparedRequest
 */
@Slf4j
@UtilityClass
public class PreparedRequestBuilder {

    // ==================== 常量定义 ====================

    private static final String HEADER_AUTHORIZATION = "Authorization";

    // ==================== 预计算缓存（方案B：性能优化） ====================

    /**
     * 缓存版本号：Collection 修改时递增，使所有缓存失效
     * 使用场景：编辑分组的 auth/headers/scripts 时调用 invalidateCache()
     */
    private static final AtomicLong cacheVersion = new AtomicLong(0);

    /**
     * 预计算缓存：requestId -> CachedEffectiveItem
     * 存储已应用 group 继承的请求配置，避免重复计算
     * 线程安全：使用 ConcurrentHashMap 支持高并发访问
     */
    private static final Map<String, CachedEffectiveItem> effectiveItemCache = new ConcurrentHashMap<>();

    /**
     * 缓存的已继承请求配置
     */
    private static class CachedEffectiveItem {
        HttpRequestItem effectiveItem;
        long version;

        CachedEffectiveItem(HttpRequestItem effectiveItem, long version) {
            this.effectiveItem = effectiveItem;
            this.version = version;
        }
    }

    /**
     * 使所有预计算缓存失效
     * 调用时机：
     * 1. 修改分组的 auth/headers/scripts
     * 2. 添加/删除/移动节点
     * 3. 导入 Collection
     */
    public static void invalidateCache() {
        long oldVersion = cacheVersion.get();
        long newVersion = cacheVersion.incrementAndGet();
        effectiveItemCache.clear(); // 清空缓存以释放内存
        log.debug("预计算缓存已失效：版本 {} -> {}",
            oldVersion, newVersion);
    }

    /**
     * 使特定请求的缓存失效（精细化缓存控制）
     * 调用时机：仅修改单个请求时
     *
     * @param requestId 请求ID
     */
    @SuppressWarnings("unused") // 公共 API，供未来使用
    public static void invalidateCacheForRequest(String requestId) {
        if (requestId != null) {
            CachedEffectiveItem removed = effectiveItemCache.remove(requestId);
            if (removed != null) {
                log.debug("请求缓存已失效：{}", requestId);
            }
        }
    }

    /**
     * 获取缓存统计信息（用于监控和调试）
     *
     * @return 格式：版本号=x, 缓存项=y
     */
    @SuppressWarnings("unused") // 公共 API，供未来使用
    public static String getCacheStats() {
        return String.format("版本号=%d, 缓存项=%d",
            cacheVersion.get(), effectiveItemCache.size());
    }

    /**
     * 构建 PreparedRequest
     * <p>
     * 自动应用 group 继承规则：
     * - 如果请求来自 Collections，会自动合并父级 group 的配置（认证、脚本、请求头）
     * - 合并后的脚本会存储在 PreparedRequest 中，供后续使用
     *
     * @param item 请求项
     * @return 构建好的 PreparedRequest（包含合并后的脚本）
     */
    public static PreparedRequest build(HttpRequestItem item) {
        // 1. 先应用 group 继承（如果适用）
        HttpRequestItem effectiveItem = applyGroupInheritance(item);

        // 2. 构建 PreparedRequest
        PreparedRequest req = new PreparedRequest();
        req.id = effectiveItem.getId();
        req.method = effectiveItem.getMethod();
        req.body = effectiveItem.getBody();
        req.bodyType = effectiveItem.getBodyType();

        // 构建 URL（拼接参数但暂不替换变量）
        req.url = buildUrlWithParams(effectiveItem);

        // 判断是否为 multipart 请求
        req.isMultipart = checkIsMultipart(effectiveItem.getFormDataList());
        req.followRedirects = SettingManager.isFollowRedirects();

        // 填充 List 数据，支持相同 key
        req.headersList = buildHeadersListWithAuth(effectiveItem);
        req.formDataList = effectiveItem.getFormDataList();
        req.urlencodedList = effectiveItem.getUrlencodedList();
        req.paramsList = effectiveItem.getParamsList();

        // 3. 存储合并后的脚本（供 ScriptExecutionPipeline 使用）
        req.prescript = effectiveItem.getPrescript();
        req.postscript = effectiveItem.getPostscript();

        return req;
    }

    /**
     * 构建包含参数的 URL（暂不替换变量）
     */
    private static String buildUrlWithParams(HttpRequestItem item) {
        Map<String, String> params = new LinkedHashMap<>();
        if (item.getParamsList() != null) {
            for (HttpParam param : item.getParamsList()) {
                if (param.isEnabled()) {
                    params.put(param.getKey(), param.getValue());
                }
            }
        }
        String urlString = HttpRequestUtil.buildUrlWithParams(item.getUrl(), params);
        return HttpRequestUtil.encodeUrlParams(urlString);
    }

    /**
     * 检查是否为 multipart 请求
     */
    private static boolean checkIsMultipart(List<HttpFormData> formDataList) {
        if (formDataList == null) {
            return false;
        }

        for (HttpFormData data : formDataList) {
            if (data.isEnabled() && (data.isText() || data.isFile())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 应用 group 继承规则
     * <p>
     * 尝试从 Collections 树中查找该请求，如果找到则应用父级 group 的配置（认证、脚本、请求头）。
     * 如果请求不在 Collections 中（例如来自 Functional/Performance 的独立请求），则直接返回原始请求。
     * <p>
     * 使用场景：
     * - Collections 面板：在执行请求前应用继承
     * - Functional 面板：在执行批量测试前应用继承
     * - Performance 面板：在执行压测前应用继承
     *
     * @param item 原始请求项
     * @return 应用了 group 继承后的请求项（新对象），如果不适用则返回原始请求
     */
    public static HttpRequestItem applyGroupInheritance(HttpRequestItem item) {
        if (item == null) {
            return null;
        }

        // 性能优化：检查预计算缓存
        String requestId = item.getId();
        if (requestId != null) {
            CachedEffectiveItem cached = effectiveItemCache.get(requestId);
            long currentVersion = cacheVersion.get();

            if (cached != null && cached.version == currentVersion) {
                // 缓存命中！直接返回（节省树遍历和合并计算）
                log.trace("预计算缓存命中：{}", item.getName());
                return cached.effectiveItem;
            }
        }

        // 缓存未命中，执行完整的继承计算
        HttpRequestItem effectiveItem = applyGroupInheritanceInternal(item);

        // 存入缓存
        if (requestId != null && effectiveItem != null) {
            effectiveItemCache.put(requestId,
                new CachedEffectiveItem(effectiveItem, cacheVersion.get()));
        }

        return effectiveItem;
    }

    /**
     * 内部方法：执行实际的 group 继承计算（不使用缓存）
     */
    private static HttpRequestItem applyGroupInheritanceInternal(HttpRequestItem item) {
        if (item == null) {
            return null;
        }

        try {
            // 尝试获取 Collections 树的根节点
            RequestCollectionsLeftPanel leftPanel =
                    SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);

            DefaultMutableTreeNode rootNode = leftPanel.getRootTreeNode();
            if (rootNode == null) {
                log.trace("Collections 树根节点为空，跳过 group 继承");
                return item;
            }

            // 在树中查找该请求的节点（已优化为 O(1) 索引查找）
            DefaultMutableTreeNode requestNode =
                    GroupInheritanceHelper.findRequestNode(rootNode, item.getId());

            if (requestNode != null) {
                // 找到了！应用 group 继承
                log.debug("为请求 [{}] 应用 group 继承", item.getName());
                return GroupInheritanceHelper.mergeGroupSettings(item, requestNode);
            } else {
                // 没找到，可能是从其他地方（Functional/Performance）导入的请求
                log.trace("请求 [{}] 不在 Collections 树中，使用原始配置", item.getName());
                return item;
            }
        } catch (Exception e) {
            // 任何异常都不应该影响请求的正常执行
            log.debug("应用 group 继承时发生异常（将使用原始配置）: {}", e.getMessage());
            return item;
        }
    }

    /**
     * 构建包含认证信息的 headersList
     * 如果配置了认证，会自动添加 Authorization 头（如果不存在）
     * <p>
     * 优化点：
     * - 提前判断认证类型，避免不必要的遍历
     * - 使用 Stream API 简化代码
     */
    private static List<HttpHeader> buildHeadersListWithAuth(HttpRequestItem item) {
        List<HttpHeader> headersList = new ArrayList<>();

        // 复制原始的 headers
        if (item.getHeadersList() != null) {
            headersList.addAll(item.getHeadersList());
        }

        // 提前判断：如果认证类型无效，直接返回
        String authType = item.getAuthType();
        if (authType == null ||
            (!AUTH_TYPE_BASIC.equals(authType) && !AUTH_TYPE_BEARER.equals(authType))) {
            return headersList;
        }

        // 检查是否已有 Authorization 头（使用 Stream API）
        boolean hasAuthHeader = item.getHeadersList() != null &&
            item.getHeadersList().stream()
                .anyMatch(h -> h.isEnabled() && HEADER_AUTHORIZATION.equalsIgnoreCase(h.getKey()));

        // 如果已有 Authorization 头，直接返回
        if (hasAuthHeader) {
            return headersList;
        }

        // 添加认证头
        HttpHeader authHeader = createAuthHeader(item, authType);
        if (authHeader != null) {
            headersList.add(authHeader);
        }

        return headersList;
    }

    /**
     * 创建认证头
     */
    private static HttpHeader createAuthHeader(HttpRequestItem item, String authType) {
        if (AUTH_TYPE_BASIC.equals(authType)) {
            return createBasicAuthHeader(item);
        } else if (AUTH_TYPE_BEARER.equals(authType)) {
            return createBearerAuthHeader(item);
        }
        return null;
    }

    /**
     * 创建 Basic 认证头
     */
    private static HttpHeader createBasicAuthHeader(HttpRequestItem item) {
        String username = EnvironmentService.replaceVariables(item.getAuthUsername());
        if (username == null || username.isEmpty()) {
            return null;
        }

        String password = EnvironmentService.replaceVariables(item.getAuthPassword());
        String credentials = username + ":" + (password == null ? "" : password);
        String token = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());

        HttpHeader authHeader = new HttpHeader();
        authHeader.setKey(HEADER_AUTHORIZATION);
        authHeader.setValue("Basic " + token);
        authHeader.setEnabled(true);
        return authHeader;
    }

    /**
     * 创建 Bearer 认证头
     */
    private static HttpHeader createBearerAuthHeader(HttpRequestItem item) {
        String token = EnvironmentService.replaceVariables(item.getAuthToken());
        if (token == null || token.isEmpty()) {
            return null;
        }

        HttpHeader authHeader = new HttpHeader();
        authHeader.setKey(HEADER_AUTHORIZATION);
        authHeader.setValue("Bearer " + token);
        authHeader.setEnabled(true);
        return authHeader;
    }

    /**
     * 在前置脚本执行后，替换所有变量占位符
     */
    public static void replaceVariablesAfterPreScript(PreparedRequest req) {
        // 替换 List 中的变量，支持相同 key
        replaceVariablesInHeadersList(req.headersList);
        replaceVariablesInFormDataList(req.formDataList);
        replaceVariablesInUrlencodedList(req.urlencodedList);
        replaceVariablesInParamsList(req.paramsList);

        // 重新构建 URL（包含脚本动态添加的 params）
        rebuildUrlWithParams(req);

        // 替换URL中的变量
        req.url = EnvironmentService.replaceVariables(req.url);

        // 替换Body中的变量
        req.body = EnvironmentService.replaceVariables(req.body);
    }

    /**
     * 重新构建 URL，包含脚本中动态添加的 params
     * buildUrlWithParams 会自动避免重复的 key
     */
    private static void rebuildUrlWithParams(PreparedRequest req) {
        if (req.paramsList == null || req.paramsList.isEmpty()) return;

        // 提取所有启用的 params 到 Map（脚本可能添加了新的 params）
        Map<String, String> params = new LinkedHashMap<>();
        for (HttpParam param : req.paramsList) {
            if (param.isEnabled()) {
                params.put(param.getKey(), param.getValue());
            }
        }

        // 重新构建 URL（buildUrlWithParams 会自动避免重复的 key）
        if (!params.isEmpty()) {
            req.url = HttpRequestUtil.buildUrlWithParams(req.url, params);
        }
    }

    private static void replaceVariablesInHeadersList(List<HttpHeader> list) {
        if (list == null) return;
        for (HttpHeader item : list) {
            if (item.isEnabled()) {
                item.setKey(EnvironmentService.replaceVariables(item.getKey()));
                item.setValue(EnvironmentService.replaceVariables(item.getValue()));
            }
        }
    }

    private static void replaceVariablesInFormDataList(List<HttpFormData> list) {
        if (list == null) return;
        for (HttpFormData item : list) {
            if (item.isEnabled()) {
                item.setKey(EnvironmentService.replaceVariables(item.getKey()));
                item.setValue(EnvironmentService.replaceVariables(item.getValue()));
            }
        }
    }

    private static void replaceVariablesInUrlencodedList(List<HttpFormUrlencoded> list) {
        if (list == null) return;
        for (HttpFormUrlencoded item : list) {
            if (item.isEnabled()) {
                item.setKey(EnvironmentService.replaceVariables(item.getKey()));
                item.setValue(EnvironmentService.replaceVariables(item.getValue()));
            }
        }
    }

    private static void replaceVariablesInParamsList(List<HttpParam> list) {
        if (list == null) return;
        for (HttpParam item : list) {
            if (item.isEnabled()) {
                item.setKey(EnvironmentService.replaceVariables(item.getKey()));
                item.setValue(EnvironmentService.replaceVariables(item.getValue()));
            }
        }
    }
}