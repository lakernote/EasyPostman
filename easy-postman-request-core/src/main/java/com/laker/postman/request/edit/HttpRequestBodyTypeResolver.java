package com.laker.postman.request.edit;

import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestBodyTypes;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Locale;

@UtilityClass
public class HttpRequestBodyTypeResolver {
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final String CONTENT_TYPE_MULTIPART_FORM = "multipart/form-data";

    public static String resolveEditableBodyType(HttpRequestItem item) {
        if (item == null) {
            return RequestBodyTypes.BODY_TYPE_NONE;
        }

        String bodyType = item.getBodyType();
        if (bodyType != null && !bodyType.trim().isEmpty()) {
            return bodyType.trim();
        }

        if (!supportsBody(item.getMethod())) {
            return RequestBodyTypes.BODY_TYPE_NONE;
        }

        String contentType = findHeaderValue(item.getHeadersList(), HEADER_CONTENT_TYPE);
        if (contentType == null || contentType.trim().isEmpty()) {
            return RequestBodyTypes.BODY_TYPE_NONE;
        }

        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        if (normalizedContentType.contains(CONTENT_TYPE_FORM_URLENCODED)) {
            return RequestBodyTypes.BODY_TYPE_FORM_URLENCODED;
        }
        if (normalizedContentType.contains(CONTENT_TYPE_MULTIPART_FORM)) {
            return RequestBodyTypes.BODY_TYPE_FORM_DATA;
        }
        return RequestBodyTypes.BODY_TYPE_RAW;
    }

    private static boolean supportsBody(String method) {
        if (method == null) {
            return false;
        }
        String normalized = method.trim().toUpperCase(Locale.ROOT);
        return "POST".equals(normalized)
                || "PUT".equals(normalized)
                || "PATCH".equals(normalized)
                || "DELETE".equals(normalized);
    }

    private static String findHeaderValue(List<HttpHeader> headers, String key) {
        if (headers == null || key == null) {
            return null;
        }
        for (HttpHeader header : headers) {
            if (header != null && header.getKey() != null && key.equalsIgnoreCase(header.getKey().trim())) {
                return header.getValue();
            }
        }
        return null;
    }
}
