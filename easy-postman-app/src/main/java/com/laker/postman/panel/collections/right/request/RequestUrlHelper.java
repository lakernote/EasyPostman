package com.laker.postman.panel.collections.right.request;

import com.laker.postman.model.HttpParam;
import com.laker.postman.service.http.HttpUtil;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@UtilityClass
class RequestUrlHelper {

    static List<HttpParam> mergeUrlParamsWithDisabledParams(String url, List<HttpParam> currentParams) {
        List<HttpParam> urlParams = HttpUtil.getParamsListFromUrl(url);
        List<HttpParam> enabledCurrentParams = currentParams.stream()
                .filter(HttpParam::isEnabled)
                .toList();

        if (paramsListEquals(urlParams, enabledCurrentParams)) {
            return currentParams;
        }

        List<HttpParam> disabledParams = currentParams.stream()
                .filter(p -> !p.isEnabled())
                .toList();

        List<HttpParam> mergedParams = new ArrayList<>(urlParams);
        mergedParams.addAll(disabledParams);
        return mergedParams;
    }

    static String rebuildUrlFromParams(String currentUrl, List<HttpParam> params) {
        String baseUrl = HttpUtil.getBaseUrlWithoutParams(currentUrl);
        if (baseUrl == null || baseUrl.isEmpty()) {
            return currentUrl;
        }
        return HttpUtil.buildUrlFromParamsList(baseUrl, params);
    }

    static String prependProtocolIfNeeded(String url, boolean webSocketProtocol, String defaultProtocol) {
        if (url == null || url.isBlank()) {
            return url;
        }
        if (url.startsWith("{{")) {
            return url;
        }

        String lower = url.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("ws://") || lower.startsWith("wss://")) {
            return url;
        }

        if (webSocketProtocol) {
            return ("https".equals(defaultProtocol) ? "wss://" : "ws://") + url;
        }
        return defaultProtocol + "://" + url;
    }

    private static boolean paramsListEquals(List<HttpParam> list1, List<HttpParam> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;

        for (int i = 0; i < list1.size(); i++) {
            HttpParam p1 = list1.get(i);
            HttpParam p2 = list2.get(i);
            if (!Objects.equals(p1.getKey(), p2.getKey())
                    || !Objects.equals(p1.getValue(), p2.getValue())
                    || p1.isEnabled() != p2.isEnabled()) {
                return false;
            }
        }
        return true;
    }
}
