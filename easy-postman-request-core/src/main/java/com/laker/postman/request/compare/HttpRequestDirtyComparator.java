package com.laker.postman.request.compare;

import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpRequestItem;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class HttpRequestDirtyComparator {

    public static boolean isDirty(HttpRequestItem original,
                                  HttpRequestItem current,
                                  List<HttpHeader> generatedDefaultHeaders) {
        if (original == current) {
            return false;
        }
        if (original == null || current == null) {
            return true;
        }

        List<HttpHeader> originalHeaders = normalizeHeaders(original.getHeadersList());
        List<HttpHeader> currentHeaders = dropGeneratedDefaultHeadersAbsentFromOriginal(
                normalizeHeaders(current.getHeadersList()),
                originalHeaders,
                normalizeHeaders(generatedDefaultHeaders)
        );

        HttpRequestEditSnapshot originalSnapshot = HttpRequestEditSnapshot.from(original, originalHeaders);
        HttpRequestEditSnapshot currentSnapshot = HttpRequestEditSnapshot.from(current, currentHeaders);
        return !originalSnapshot.equals(currentSnapshot);
    }

    private static List<HttpHeader> normalizeHeaders(List<HttpHeader> headers) {
        List<HttpHeader> normalized = new ArrayList<>();
        if (headers == null) {
            return normalized;
        }
        for (HttpHeader header : headers) {
            if (header == null) {
                continue;
            }
            String key = normalizeHeaderKey(header.getKey());
            if (key.isEmpty()) {
                continue;
            }
            normalized.add(new HttpHeader(
                    header.isEnabled(),
                    key,
                    normalizeHeaderValue(header.getValue()),
                    normalizeHeaderValue(header.getDescription())
            ));
        }
        return normalized;
    }

    private static List<HttpHeader> dropGeneratedDefaultHeadersAbsentFromOriginal(List<HttpHeader> currentHeaders,
                                                                                 List<HttpHeader> originalHeaders,
                                                                                 List<HttpHeader> generatedDefaultHeaders) {
        List<HttpHeader> normalized = new ArrayList<>();
        for (HttpHeader header : currentHeaders) {
            if (containsEquivalentHeader(generatedDefaultHeaders, header)
                    && !containsEquivalentHeader(originalHeaders, header)) {
                continue;
            }
            normalized.add(header);
        }
        return normalized;
    }

    private static boolean containsEquivalentHeader(List<HttpHeader> headers, HttpHeader candidate) {
        if (headers == null || candidate == null) {
            return false;
        }
        for (HttpHeader header : headers) {
            if (header != null
                    && header.isEnabled() == candidate.isEnabled()
                    && equalsHeaderKey(header.getKey(), candidate.getKey())
                    && equalsHeaderValue(header.getValue(), candidate.getValue())
                    && equalsHeaderValue(header.getDescription(), candidate.getDescription())) {
                return true;
            }
        }
        return false;
    }

    private static boolean equalsHeaderKey(String left, String right) {
        return normalizeHeaderKey(left).equalsIgnoreCase(normalizeHeaderKey(right));
    }

    private static boolean equalsHeaderValue(String left, String right) {
        return normalizeHeaderValue(left).equals(normalizeHeaderValue(right));
    }

    private static String normalizeHeaderKey(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeHeaderValue(String value) {
        return value == null ? "" : value.trim();
    }
}
