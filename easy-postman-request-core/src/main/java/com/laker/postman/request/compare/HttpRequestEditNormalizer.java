package com.laker.postman.request.compare;

import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.util.HttpUrlUtil;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
class HttpRequestEditNormalizer {

    static NormalizedRequest original(HttpRequestItem item) {
        return new NormalizedRequest(
                item,
                normalizeHeaders(item.getHeadersList()),
                normalizeParams(item.getParamsList())
        );
    }

    static NormalizedRequest currentComparedToOriginal(HttpRequestItem current,
                                                       NormalizedRequest original,
                                                       List<HttpHeader> generatedDefaultHeaders) {
        List<HttpHeader> currentHeaders = dropGeneratedDefaultHeadersAbsentFromOriginal(
                normalizeHeaders(current.getHeadersList()),
                original.headers(),
                normalizeHeaders(generatedDefaultHeaders)
        );
        List<HttpParam> currentParams = dropGeneratedUrlQueryParamsAbsentFromOriginal(
                normalizeParams(current.getParamsList()),
                original.params(),
                current.getUrl()
        );
        return new NormalizedRequest(current, currentHeaders, currentParams);
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

    private static List<HttpParam> normalizeParams(List<HttpParam> params) {
        List<HttpParam> normalized = new ArrayList<>();
        if (params == null) {
            return normalized;
        }
        for (HttpParam param : params) {
            if (param == null) {
                continue;
            }
            normalized.add(new HttpParam(
                    param.isEnabled(),
                    normalizeParamValue(param.getKey()),
                    normalizeParamValue(param.getValue()),
                    normalizeParamValue(param.getDescription())
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

    private static List<HttpParam> dropGeneratedUrlQueryParamsAbsentFromOriginal(List<HttpParam> currentParams,
                                                                                List<HttpParam> originalParams,
                                                                                String currentUrl) {
        List<HttpParam> generatedQueryParams = normalizeParams(HttpUrlUtil.parseQueryParams(currentUrl));
        if (generatedQueryParams.isEmpty()) {
            return currentParams;
        }

        List<HttpParam> normalized = new ArrayList<>();
        List<HttpParam> remainingGeneratedParams = new ArrayList<>(generatedQueryParams);
        for (HttpParam param : currentParams) {
            if (removeEquivalentParam(remainingGeneratedParams, param)
                    && !containsEquivalentParam(originalParams, param)) {
                continue;
            }
            normalized.add(param);
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

    private static boolean containsEquivalentParam(List<HttpParam> params, HttpParam candidate) {
        if (params == null || candidate == null) {
            return false;
        }
        for (HttpParam param : params) {
            if (isEquivalentParam(param, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean removeEquivalentParam(List<HttpParam> params, HttpParam candidate) {
        for (int i = 0; i < params.size(); i++) {
            if (isEquivalentParam(params.get(i), candidate)) {
                params.remove(i);
                return true;
            }
        }
        return false;
    }

    private static boolean isEquivalentParam(HttpParam left, HttpParam right) {
        return left != null
                && right != null
                && left.isEnabled() == right.isEnabled()
                && equalsParamValue(left.getKey(), right.getKey())
                && equalsParamValue(left.getValue(), right.getValue())
                && equalsParamValue(left.getDescription(), right.getDescription());
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

    private static boolean equalsParamValue(String left, String right) {
        return normalizeParamValue(left).equals(normalizeParamValue(right));
    }

    private static String normalizeParamValue(String value) {
        return value == null ? "" : value;
    }

    record NormalizedRequest(HttpRequestItem item, List<HttpHeader> headers, List<HttpParam> params) {
    }
}
