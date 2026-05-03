package com.laker.postman.service.http;

import com.laker.postman.model.HttpFormData;
import com.laker.postman.model.HttpFormUrlencoded;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpParam;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.TransportAuth;
import com.laker.postman.service.variable.RequestContext;
import com.laker.postman.service.variable.VariableResolver;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

import static com.laker.postman.common.constants.HttpConstants.HEADER_AUTHORIZATION;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BASIC;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BEARER;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_DIGEST;

/**
 * 发送前的最终收尾逻辑：
 * 1. 替换请求里的变量占位符
 * 2. 如果脚本/请求头里还没有 Authorization，再按 auth tab 自动补齐
 * <p>
 * 认证优先级约定：
 * <ol>
 *   <li>pre-script 中显式写入的 Authorization</li>
 *   <li>请求头列表里已有的 Authorization</li>
 *   <li>auth tab 自动生成的 Authorization</li>
 * </ol>
 * <p>
 * 为了保持规则简单稳定，pm.request.headers.remove("Authorization")
 * 不再表示“禁用 auth tab 认证”，它只表示删除当前 header。
 * 如果最终阶段请求头里仍然没有 Authorization，而 auth tab 又配置了认证，
 * finalizer 会按配置重新补齐认证头。
 */
@UtilityClass
public class RequestFinalizer {

    /**
     * 在请求真正发出前统一完成变量替换和认证决策。
     * 所有发送路径（普通发送、Functional、Performance、复制 cURL）
     * 都应尽量复用这里，避免不同入口出现不一致行为。
     */
    public void finalizeForSend(PreparedRequest request,
                                PreparedRequestBuilder.DeferredAuthorization deferredAuthorization) {
        try {
            replaceVariablesInHeadersList(request.headersList);
            replaceVariablesInFormDataList(request.formDataList);
            replaceVariablesInUrlencodedList(request.urlencodedList);

            request.url = VariableResolver.resolve(request.url);
            replaceVariablesInParamsList(request.paramsList);
            request.url = HttpRequestUtil.buildEncodedUrl(request.url, request.paramsList);
            request.body = VariableResolver.resolve(request.body);

            applyDeferredAuthorization(request, deferredAuthorization);
        } finally {
            RequestContext.clearCurrentRequestNode();
        }
    }

    public void finalizeForSend(PreparedRequest request,
                                HttpRequestItem item,
                                boolean useCache) {
        finalizeForSend(request, PreparedRequestBuilder.resolveDeferredAuthorization(item, useCache));
    }

    private void applyDeferredAuthorization(PreparedRequest request,
                                            PreparedRequestBuilder.DeferredAuthorization deferredAuthorization) {
        if (request == null) {
            return;
        }
        request.transportAuth = null;

        if (deferredAuthorization == null) {
            return;
        }

        HttpHeader authHeader = createAuthHeader(deferredAuthorization);
        List<HttpHeader> authorizationHeaders = findEnabledAuthorizationHeaders(request.headersList);
        List<HttpHeader> previewHeaders = findPreviewAuthorizationHeaders(authorizationHeaders, deferredAuthorization);
        if (previewHeaders.size() != authorizationHeaders.size()) {
            removeAuthorizationHeaders(request.headersList, previewHeaders);
            return;
        }

        applyTransportAuthorization(request, deferredAuthorization);

        if (authorizationHeaders.isEmpty()) {
            if (authHeader == null) {
                return;
            }
            if (request.headersList == null) {
                request.headersList = new ArrayList<>();
            }
            request.headersList.add(authHeader);
            return;
        }

        if (authHeader == null) {
            removeAuthorizationHeaders(request.headersList, previewHeaders);
            return;
        }

        HttpHeader primaryPreviewHeader = previewHeaders.get(0);
        primaryPreviewHeader.setValue(authHeader.getValue());
        primaryPreviewHeader.setEnabled(true);
        removeAuthorizationHeaders(request.headersList, previewHeaders.subList(1, previewHeaders.size()));
    }

    private void applyTransportAuthorization(PreparedRequest request,
                                             PreparedRequestBuilder.DeferredAuthorization deferredAuthorization) {
        if (!AUTH_TYPE_DIGEST.equals(deferredAuthorization.authType())) {
            return;
        }

        String username = VariableResolver.resolve(deferredAuthorization.authUsername());
        String password = VariableResolver.resolve(deferredAuthorization.authPassword());
        if (username == null || username.isEmpty()
                || containsUnresolvedPlaceholder(username)
                || containsUnresolvedPlaceholder(password)) {
            return;
        }

        request.transportAuth = new TransportAuth(
                AUTH_TYPE_DIGEST,
                username,
                password == null ? "" : password
        );
    }

    private List<HttpHeader> findEnabledAuthorizationHeaders(List<HttpHeader> headersList) {
        if (headersList == null || headersList.isEmpty()) {
            return List.of();
        }
        return headersList.stream()
                .filter(h -> h != null && h.isEnabled() && HEADER_AUTHORIZATION.equalsIgnoreCase(h.getKey()))
                .toList();
    }

    private List<HttpHeader> findPreviewAuthorizationHeaders(List<HttpHeader> authorizationHeaders,
                                                             PreparedRequestBuilder.DeferredAuthorization deferredAuthorization) {
        String previewValue = deferredAuthorization.previewAuthorizationHeaderValue();
        if (previewValue == null || authorizationHeaders == null || authorizationHeaders.isEmpty()) {
            return List.of();
        }
        return authorizationHeaders.stream()
                .filter(header -> previewValue.equals(header.getValue()))
                .toList();
    }

    private void removeAuthorizationHeaders(List<HttpHeader> headersList, List<HttpHeader> headersToRemove) {
        if (headersList == null || headersList.isEmpty() || headersToRemove == null || headersToRemove.isEmpty()) {
            return;
        }
        headersList.removeIf(headersToRemove::contains);
    }

    private HttpHeader createAuthHeader(PreparedRequestBuilder.DeferredAuthorization deferredAuthorization) {
        if (deferredAuthorization == null || deferredAuthorization.authType() == null) {
            return null;
        }
        if (AUTH_TYPE_BASIC.equals(deferredAuthorization.authType())) {
            return createBasicAuthHeader(
                    deferredAuthorization.authUsername(),
                    deferredAuthorization.authPassword()
            );
        } else if (AUTH_TYPE_BEARER.equals(deferredAuthorization.authType())) {
            return createBearerAuthHeader(deferredAuthorization.authToken());
        }
        return null;
    }

    private HttpHeader createBasicAuthHeader(String rawUsername, String rawPassword) {
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

    private HttpHeader createBearerAuthHeader(String rawToken) {
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

    private boolean containsUnresolvedPlaceholder(String value) {
        return value != null && value.contains("{{") && value.contains("}}");
    }

    private void replaceVariablesInHeadersList(List<HttpHeader> list) {
        if (list == null) return;
        for (HttpHeader item : list) {
            if (item.isEnabled()) {
                item.setKey(VariableResolver.resolve(item.getKey()));
                item.setValue(VariableResolver.resolve(item.getValue()));
            }
        }
    }

    private void replaceVariablesInFormDataList(List<HttpFormData> list) {
        if (list == null) return;
        for (HttpFormData item : list) {
            if (item.isEnabled()) {
                item.setKey(VariableResolver.resolve(item.getKey()));
                item.setValue(VariableResolver.resolve(item.getValue()));
            }
        }
    }

    private void replaceVariablesInUrlencodedList(List<HttpFormUrlencoded> list) {
        if (list == null) return;
        for (HttpFormUrlencoded item : list) {
            if (item.isEnabled()) {
                item.setKey(VariableResolver.resolve(item.getKey()));
                item.setValue(VariableResolver.resolve(item.getValue()));
            }
        }
    }

    private void replaceVariablesInParamsList(List<HttpParam> list) {
        if (list == null) return;
        for (HttpParam item : list) {
            if (item.isEnabled()) {
                item.setKey(VariableResolver.resolve(item.getKey()));
                item.setValue(VariableResolver.resolve(item.getValue()));
            }
        }
    }
}
