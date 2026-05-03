package com.laker.postman.service.http;

import com.laker.postman.model.*;
import com.laker.postman.service.collections.InheritanceService;
import com.laker.postman.service.variable.VariableResolver;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.laker.postman.common.constants.HttpConstants.HEADER_AUTHORIZATION;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BASIC;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BEARER;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_DIGEST;

/**
 * 负责构建 PreparedRequest
 */
@Slf4j
@UtilityClass
public class PreparedRequestBuilder {

    public record DeferredAuthorization(String authType,
                                        String authUsername,
                                        String authPassword,
                                        String authToken,
                                        String previewAuthorizationHeaderValue) {
    }


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
        HttpRequestItem effectiveItem = resolveEffectiveItem(item, useCache);

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
        req.transportAuth = createTransportAuth(effectiveItem);

        // 填充 List 数据，支持相同 key
        // 每次执行都使用独立副本，避免并发压测时线程间互相污染变量替换结果
        req.headersList = cloneHeaders(buildHeadersListWithResolvedAuth(effectiveItem));
        req.formDataList = cloneFormData(effectiveItem.getFormDataList());
        req.urlencodedList = cloneUrlencoded(effectiveItem.getUrlencodedList());
        req.paramsList = cloneParams(effectiveItem.getParamsList());

        // 3. 存储合并后的脚本（供 ScriptExecutionPipeline 使用）
        req.prescript = effectiveItem.getPrescript();
        req.postscript = effectiveItem.getPostscript();

        return req;
    }

    private static TransportAuth createTransportAuth(HttpRequestItem item) {
        if (item == null || !AUTH_TYPE_DIGEST.equals(item.getAuthType())) {
            return null;
        }
        return new TransportAuth(item.getAuthType(), item.getAuthUsername(), item.getAuthPassword());
    }

    public static DeferredAuthorization resolveDeferredAuthorization(HttpRequestItem item, boolean useCache) {
        HttpRequestItem effectiveItem = resolveEffectiveItem(item, useCache);
        return new DeferredAuthorization(
                effectiveItem.getAuthType(),
                effectiveItem.getAuthUsername(),
                effectiveItem.getAuthPassword(),
                effectiveItem.getAuthToken(),
                resolvePreviewAuthorizationHeaderValue(effectiveItem)
        );
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

    private static HttpRequestItem resolveEffectiveItem(HttpRequestItem item, boolean useCache) {
        return inheritanceService.applyInheritance(item, useCache);
    }

    /**
     * 保持 pre-script 阶段对 auth tab 自动请求头的可见性。
     * <p>
     * 这里只在当前上下文已经能解析出凭据时才预生成 Authorization，
     * 对于依赖 shared execution context / pre-script 新写入变量的场景，
     * 仍交给 RequestFinalizer 在发送前兜底补齐。
     */
    private static List<HttpHeader> buildHeadersListWithResolvedAuth(HttpRequestItem item) {
        List<HttpHeader> headers = cloneHeaders(item.getHeadersList());
        if (headers == null) {
            headers = new ArrayList<>();
        }

        if (hasEnabledAuthorizationHeader(headers)) {
            return headers;
        }

        HttpHeader authHeader = tryCreateResolvableAuthHeader(item);
        if (authHeader != null) {
            headers.add(authHeader);
        }
        return headers;
    }

    private static String resolvePreviewAuthorizationHeaderValue(HttpRequestItem item) {
        if (item == null || hasEnabledAuthorizationHeader(item.getHeadersList())) {
            return null;
        }
        HttpHeader authHeader = tryCreateResolvableAuthHeader(item);
        return authHeader != null ? authHeader.getValue() : null;
    }

    private static boolean hasEnabledAuthorizationHeader(List<HttpHeader> headers) {
        if (headers == null || headers.isEmpty()) {
            return false;
        }
        return headers.stream()
                .anyMatch(h -> h != null && h.isEnabled() && HEADER_AUTHORIZATION.equalsIgnoreCase(h.getKey()));
    }

    private static HttpHeader tryCreateResolvableAuthHeader(HttpRequestItem item) {
        if (item == null || item.getAuthType() == null) {
            return null;
        }
        if (AUTH_TYPE_BASIC.equals(item.getAuthType())) {
            return createResolvableBasicAuthHeader(item.getAuthUsername(), item.getAuthPassword());
        }
        if (AUTH_TYPE_BEARER.equals(item.getAuthType())) {
            return createResolvableBearerAuthHeader(item.getAuthToken());
        }
        if (AUTH_TYPE_DIGEST.equals(item.getAuthType())) {
            return null;
        }
        return null;
    }

    private static HttpHeader createResolvableBasicAuthHeader(String rawUsername, String rawPassword) {
        String username = VariableResolver.resolve(rawUsername);
        if (username == null || username.isEmpty() || containsUnresolvedPlaceholder(username)) {
            return null;
        }

        String password = VariableResolver.resolve(rawPassword);
        if (containsUnresolvedPlaceholder(password)) {
            return null;
        }

        String credentials = username + ":" + (password == null ? "" : password);
        String token = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());

        HttpHeader authHeader = new HttpHeader();
        authHeader.setKey(HEADER_AUTHORIZATION);
        authHeader.setValue("Basic " + token);
        authHeader.setEnabled(true);
        return authHeader;
    }

    private static HttpHeader createResolvableBearerAuthHeader(String rawToken) {
        String token = VariableResolver.resolve(rawToken);
        if (token == null || token.isEmpty() || containsUnresolvedPlaceholder(token)) {
            return null;
        }

        HttpHeader authHeader = new HttpHeader();
        authHeader.setKey(HEADER_AUTHORIZATION);
        authHeader.setValue("Bearer " + token);
        authHeader.setEnabled(true);
        return authHeader;
    }

    private static boolean containsUnresolvedPlaceholder(String value) {
        return value != null && value.contains("{{") && value.contains("}}");
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
