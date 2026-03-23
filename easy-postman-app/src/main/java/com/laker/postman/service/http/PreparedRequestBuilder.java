package com.laker.postman.service.http;

import com.laker.postman.model.*;
import com.laker.postman.service.collections.InheritanceService;
import com.laker.postman.service.variable.RequestContext;
import com.laker.postman.service.variable.VariableResolver;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.laker.postman.common.constants.HttpConstants.HEADER_AUTHORIZATION;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BASIC;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BEARER;

/**
 * 负责构建 PreparedRequest
 */
@Slf4j
@UtilityClass
public class PreparedRequestBuilder {


    /**
     * 负责所有继承逻辑和缓存管理
     */
    private static final InheritanceService inheritanceService = new InheritanceService();


    /**
     * 使所有预计算缓存失效
     * <p>
     * 调用时机：
     * 1. 修改分组的 auth/headers/scripts
     * 2. 添加/删除/移动节点
     * 3. 导入 Collection
     * <p>
     * 委托给 InheritanceService 处理
     */
    public static void invalidateCache() {
        inheritanceService.invalidateCache();
    }

    /**
     * 使特定请求的缓存失效（精细化缓存控制）
     * <p>
     * 调用时机：仅修改单个请求时
     *
     * @param requestId 请求ID
     */
    public static void invalidateCacheForRequest(String requestId) {
        inheritanceService.invalidateCache(requestId);
    }


    /**
     * 构建 PreparedRequest
     * <p>
     * 自动应用 group 继承规则：
     * - 如果请求来自 Collections，会自动合并父级 group 的配置（认证、脚本、请求头）
     * - 合并后的脚本会存储在 PreparedRequest 中，供后续使用
     * <p>
     * 智能缓存：
     * - 如果请求在 Collections 中，使用缓存（基于保存的版本）
     * - 如果请求可能被修改但未保存（来自 UI），不使用缓存
     *
     * @param item 请求项
     * @return 构建好的 PreparedRequest（包含合并后的脚本）
     */
    public static PreparedRequest build(HttpRequestItem item) {
        return build(item, true); // 默认使用缓存
    }

    /**
     * 构建 PreparedRequest（可选是否使用缓存）
     * <p>
     * 用于处理未保存的请求（如 UI 中修改但未保存到 Collections）
     *
     * @param item     请求项
     * @param useCache 是否使用缓存（false 表示强制重新计算继承）
     * @return 构建好的 PreparedRequest（包含合并后的脚本）
     */
    public static PreparedRequest build(HttpRequestItem item, boolean useCache) {
        // 1. 先应用 group 继承（如果适用）
        HttpRequestItem effectiveItem = inheritanceService.applyInheritance(item, useCache);

        // 2. 构建 PreparedRequest
        PreparedRequest req = new PreparedRequest();
        req.id = effectiveItem.getId();
        req.method = effectiveItem.getMethod();
        req.body = effectiveItem.getBody();
        req.bodyType = effectiveItem.getBodyType();

        // 构建 URL（拼接参数但暂不替换变量；编码在此处理，发请求前只需替换变量）
        req.url = buildRawUrlWithParams(effectiveItem);

        // 判断是否为 multipart 请求
        req.isMultipart = checkIsMultipart(effectiveItem.getFormDataList());
        req.followRedirects = RequestSettingsResolver.resolveFollowRedirects(effectiveItem);
        req.cookieJarEnabled = RequestSettingsResolver.resolveCookieJarEnabled(effectiveItem);
        req.sslVerificationEnabled = RequestSettingsResolver.resolveSslVerificationEnabled(effectiveItem);
        req.httpVersion = RequestSettingsResolver.resolveHttpVersion(effectiveItem);
        req.requestTimeoutMs = RequestSettingsResolver.resolveRequestTimeoutMs(effectiveItem);

        // 填充 List 数据，支持相同 key
        // 每次执行都使用独立副本，避免并发压测时线程间互相污染变量替换结果
        req.headersList = cloneHeaders(buildHeadersListWithAuth(effectiveItem));
        req.formDataList = cloneFormData(effectiveItem.getFormDataList());
        req.urlencodedList = cloneUrlencoded(effectiveItem.getUrlencodedList());
        req.paramsList = cloneParams(effectiveItem.getParamsList());

        // 3. 存储合并后的脚本（供 ScriptExecutionPipeline 使用）
        req.prescript = effectiveItem.getPrescript();
        req.postscript = effectiveItem.getPostscript();

        return req;
    }


    /**
     * 在前置脚本执行后，替换所有变量占位符
     * <p>
     * 注意：此方法会在完成后清除 RequestContext 的 ThreadLocal
     */
    public static void replaceVariablesAfterPreScript(PreparedRequest req) {
        try {
            // 替换 List 中的变量，支持相同 key
            replaceVariablesInHeadersList(req.headersList);
            replaceVariablesInFormDataList(req.formDataList);
            replaceVariablesInUrlencodedList(req.urlencodedList);

            // 先替换 URL 和 paramsList 中的变量，然后再重建 URL
            // 这样可以避免重复参数的问题（例如：URL 中有 {{a}}=3，paramsList 中也有 {{a}}=3）
            req.url = VariableResolver.resolve(req.url);
            replaceVariablesInParamsList(req.paramsList);
            // 变量替换完成后，用 buildEncodedUrl 一次性完成去重 + URL 编码，生成最终可发送的 URL
            req.url = HttpRequestUtil.buildEncodedUrl(req.url, req.paramsList);

            // 替换Body中的变量
            req.body = VariableResolver.resolve(req.body);
        } finally {
            // 清除全局请求上下文，释放资源
            RequestContext.clearCurrentRequestNode();
        }
    }

    /**
     * 构建包含参数的原始 URL（暂不替换变量，暂不编码）。
     *
     * <p>此时变量尚未替换（如 {{baseUrl}}），所以不能做 URL 编码，
     * 编码推迟到 {@link #replaceVariablesAfterPreScript} 中变量替换完成后，
     * 由 {@link HttpRequestUtil#buildEncodedUrl} 统一完成去重 + 编码。
     */
    private static String buildRawUrlWithParams(HttpRequestItem item) {
        String url = item.getUrl();
        if (item.getParamsList() == null || item.getParamsList().isEmpty()) {
            return url;
        }
        // 仅追加 paramsList 中不与 URL 已有 key 重复的参数（不编码，保留 {{变量}} 原样）
        boolean hasQuery = url != null && url.contains("?");
        Set<String> existingKeys = url != null
                ? HttpRequestUtil.extractUrlParamKeys(url, hasQuery)
                : java.util.Collections.emptySet();
        StringBuilder sb = new StringBuilder(url != null ? url : "");
        for (HttpParam param : item.getParamsList()) {
            if (!param.isEnabled()) continue;
            String key = param.getKey();
            if (key == null || key.isEmpty()) continue;
            if (existingKeys.contains(key)) continue;
            sb.append(hasQuery ? "&" : "?");
            hasQuery = true;
            sb.append(key).append("=").append(param.getValue() != null ? param.getValue() : "");
        }
        return sb.toString();
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
        String username = VariableResolver.resolve(item.getAuthUsername());
        if (username == null || username.isEmpty()) {
            return null;
        }

        String password = VariableResolver.resolve(item.getAuthPassword());
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
        String token = VariableResolver.resolve(item.getAuthToken());
        if (token == null || token.isEmpty()) {
            return null;
        }

        HttpHeader authHeader = new HttpHeader();
        authHeader.setKey(HEADER_AUTHORIZATION);
        authHeader.setValue("Bearer " + token);
        authHeader.setEnabled(true);
        return authHeader;
    }

    private static void replaceVariablesInHeadersList(List<HttpHeader> list) {
        if (list == null) return;
        for (HttpHeader item : list) {
            if (item.isEnabled()) {
                item.setKey(VariableResolver.resolve(item.getKey()));
                item.setValue(VariableResolver.resolve(item.getValue()));
            }
        }
    }

    private static void replaceVariablesInFormDataList(List<HttpFormData> list) {
        if (list == null) return;
        for (HttpFormData item : list) {
            if (item.isEnabled()) {
                item.setKey(VariableResolver.resolve(item.getKey()));
                item.setValue(VariableResolver.resolve(item.getValue()));
            }
        }
    }

    private static void replaceVariablesInUrlencodedList(List<HttpFormUrlencoded> list) {
        if (list == null) return;
        for (HttpFormUrlencoded item : list) {
            if (item.isEnabled()) {
                item.setKey(VariableResolver.resolve(item.getKey()));
                item.setValue(VariableResolver.resolve(item.getValue()));
            }
        }
    }

    private static void replaceVariablesInParamsList(List<HttpParam> list) {
        if (list == null) return;
        for (HttpParam item : list) {
            if (item.isEnabled()) {
                item.setKey(VariableResolver.resolve(item.getKey()));
                item.setValue(VariableResolver.resolve(item.getValue()));
            }
        }
    }

    private static List<HttpHeader> cloneHeaders(List<HttpHeader> list) {
        if (list == null) {
            return null;
        }
        List<HttpHeader> cloned = new ArrayList<>(list.size());
        for (HttpHeader item : list) {
            if (item == null) {
                cloned.add(null);
            } else {
                cloned.add(new HttpHeader(item.isEnabled(), item.getKey(), item.getValue()));
            }
        }
        return cloned;
    }

    private static List<HttpFormData> cloneFormData(List<HttpFormData> list) {
        if (list == null) {
            return null;
        }
        List<HttpFormData> cloned = new ArrayList<>(list.size());
        for (HttpFormData item : list) {
            if (item == null) {
                cloned.add(null);
            } else {
                cloned.add(new HttpFormData(item.isEnabled(), item.getKey(), item.getType(), item.getValue()));
            }
        }
        return cloned;
    }

    private static List<HttpFormUrlencoded> cloneUrlencoded(List<HttpFormUrlencoded> list) {
        if (list == null) {
            return null;
        }
        List<HttpFormUrlencoded> cloned = new ArrayList<>(list.size());
        for (HttpFormUrlencoded item : list) {
            if (item == null) {
                cloned.add(null);
            } else {
                cloned.add(new HttpFormUrlencoded(item.isEnabled(), item.getKey(), item.getValue()));
            }
        }
        return cloned;
    }

    private static List<HttpParam> cloneParams(List<HttpParam> list) {
        if (list == null) {
            return null;
        }
        List<HttpParam> cloned = new ArrayList<>(list.size());
        for (HttpParam item : list) {
            if (item == null) {
                cloned.add(null);
            } else {
                cloned.add(new HttpParam(item.isEnabled(), item.getKey(), item.getValue()));
            }
        }
        return cloned;
    }
}
