package com.laker.postman.request.edit;

import com.laker.postman.request.model.AuthType;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.model.HttpRequestProxyPolicy;
import com.laker.postman.request.model.HttpRequestVersions;
import com.laker.postman.request.model.RequestBodyTypes;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import lombok.Value;

import java.util.List;

@Value
public class HttpRequestEditorContentSummary {
    boolean hasParams;
    boolean hasAuth;
    boolean hasHeaders;
    boolean hasBody;
    boolean hasSettings;
    boolean hasScripts;

    public static HttpRequestEditorContentSummary from(HttpRequestEditorDraft draft) {
        if (draft == null) {
            return empty();
        }
        return new HttpRequestEditorContentSummary(
                hasParams(draft.getPathVariables()) || hasParams(draft.getParams()),
                hasAuth(draft.getAuthType()),
                hasHeaders(draft.getHeaders()),
                hasBody(draft),
                hasSettings(draft),
                hasScripts(draft)
        );
    }

    public static HttpRequestEditorContentSummary empty() {
        return new HttpRequestEditorContentSummary(false, false, false, false, false, false);
    }

    private static boolean hasParams(List<HttpParam> params) {
        if (params == null) {
            return false;
        }
        return params.stream().anyMatch(param -> hasText(param.getKey()));
    }

    private static boolean hasAuth(String authType) {
        return hasText(authType)
                && !AuthType.INHERIT.getConstant().equals(authType)
                && !AuthType.NONE.getConstant().equals(authType);
    }

    private static boolean hasHeaders(List<HttpHeader> headers) {
        if (headers == null) {
            return false;
        }
        return headers.stream().anyMatch(header -> hasText(header.getKey()));
    }

    private static boolean hasBody(HttpRequestEditorDraft draft) {
        RequestItemProtocolEnum protocol = draft.getProtocol();
        if (protocol != null && !protocol.supportsRequestBodyContent()) {
            return false;
        }

        String bodyType = draft.getBodyType();
        if (bodyType == null) {
            return false;
        }

        return switch (bodyType) {
            case RequestBodyTypes.BODY_TYPE_RAW, RequestBodyTypes.BODY_TYPE_BINARY -> hasText(draft.getBody());
            case RequestBodyTypes.BODY_TYPE_FORM_DATA -> hasFormData(draft.getFormData());
            case RequestBodyTypes.BODY_TYPE_FORM_URLENCODED -> hasUrlencoded(draft.getUrlencoded());
            default -> false;
        };
    }

    private static boolean hasFormData(List<HttpFormData> formData) {
        if (formData == null) {
            return false;
        }
        return formData.stream().anyMatch(item -> hasText(item.getKey()));
    }

    private static boolean hasUrlencoded(List<HttpFormUrlencoded> urlencoded) {
        if (urlencoded == null) {
            return false;
        }
        return urlencoded.stream().anyMatch(item -> hasText(item.getKey()));
    }

    private static boolean hasSettings(HttpRequestEditorDraft draft) {
        return draft.getFollowRedirects() != null
                || Boolean.FALSE.equals(draft.getCookieJarEnabled())
                || HttpRequestProxyPolicy.DEFAULT != HttpRequestProxyPolicy.normalize(draft.getProxyPolicy())
                || !HttpRequestVersions.AUTO.equals(normalizeHttpVersion(draft.getHttpVersion()))
                || draft.getRequestTimeoutMs() != null;
    }

    private static boolean hasScripts(HttpRequestEditorDraft draft) {
        return hasText(draft.getPrescript()) || hasText(draft.getPostscript());
    }

    private static String normalizeHttpVersion(String httpVersion) {
        return httpVersion == null || httpVersion.trim().isEmpty()
                ? HttpRequestVersions.AUTO
                : httpVersion.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
