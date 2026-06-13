package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.util.HttpUrlUtil;
import lombok.experimental.UtilityClass;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@UtilityClass
class RequestUrlEditorSupport {

    static List<HttpParam> mergeUrlParamsWithCurrentTableMetadata(String url, List<HttpParam> currentParams) {
        List<HttpParam> urlParams = HttpUrlUtil.parseQueryParams(url);
        List<HttpParam> safeCurrentParams = safeParams(currentParams);
        List<HttpParam> enabledCurrentParams = enabledParams(safeCurrentParams);

        if (paramsListEquals(urlParams, enabledCurrentParams)) {
            return currentParams;
        }

        QueryParamTableMetadata currentMetadata = QueryParamTableMetadata.from(safeCurrentParams);
        List<HttpParam> mergedParams = new ArrayList<>(urlParams.size() + currentMetadata.disabledParams().size());
        for (HttpParam urlParam : urlParams) {
            mergedParams.add(currentMetadata.copyUrlParamWithPreservedMetadata(urlParam));
        }
        mergedParams.addAll(currentMetadata.disabledParams());
        return mergedParams;
    }

    static String rebuildUrlFromParams(String currentUrl, List<HttpParam> params) {
        String baseUrl = HttpUrlUtil.baseUrlWithoutQuery(currentUrl);
        if (baseUrl == null || baseUrl.isEmpty()) {
            return currentUrl;
        }
        return HttpUrlUtil.buildUrl(baseUrl, params);
    }

    static List<HttpParam> mergePathVariablesFromUrl(String url, List<HttpParam> currentPathVariables) {
        List<HttpParam> urlPathVariables = HttpUrlUtil.extractPathVariables(url);
        if (urlPathVariables.isEmpty()) {
            return currentPathVariables == null || currentPathVariables.isEmpty()
                    ? currentPathVariables
                    : new ArrayList<>();
        }

        Map<String, HttpParam> currentByKey = new LinkedHashMap<>();
        if (currentPathVariables != null) {
            for (HttpParam current : currentPathVariables) {
                if (current != null && current.getKey() != null && !current.getKey().isBlank()) {
                    currentByKey.putIfAbsent(current.getKey(), current);
                }
            }
        }

        List<HttpParam> merged = new ArrayList<>();
        for (HttpParam urlPathVariable : urlPathVariables) {
            HttpParam current = currentByKey.get(urlPathVariable.getKey());
            if (current == null) {
                merged.add(urlPathVariable);
            } else {
                merged.add(new HttpParam(
                        current.isEnabled(),
                        current.getKey(),
                        current.getValue(),
                        current.getDescription()
                ));
            }
        }

        return paramsListEquals(merged, currentPathVariables) ? currentPathVariables : merged;
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

    private static List<HttpParam> safeParams(List<HttpParam> params) {
        return params == null ? List.of() : params;
    }

    private static List<HttpParam> enabledParams(List<HttpParam> params) {
        return params.stream()
                .filter(HttpParam::isEnabled)
                .toList();
    }

    private static final class QueryParamTableMetadata {
        private final Map<String, Deque<HttpParam>> enabledParamsByKey;
        private final List<HttpParam> disabledParams;

        private QueryParamTableMetadata(Map<String, Deque<HttpParam>> enabledParamsByKey,
                                        List<HttpParam> disabledParams) {
            this.enabledParamsByKey = enabledParamsByKey;
            this.disabledParams = disabledParams;
        }

        private static QueryParamTableMetadata from(List<HttpParam> params) {
            Map<String, Deque<HttpParam>> enabledParamsByKey = new LinkedHashMap<>();
            List<HttpParam> disabledParams = new ArrayList<>();
            for (HttpParam param : params) {
                if (param.isEnabled()) {
                    enabledParamsByKey
                            .computeIfAbsent(param.getKey(), key -> new ArrayDeque<>())
                            .addLast(param);
                } else {
                    disabledParams.add(param);
                }
            }
            return new QueryParamTableMetadata(enabledParamsByKey, disabledParams);
        }

        private HttpParam copyUrlParamWithPreservedMetadata(HttpParam urlParam) {
            HttpParam currentParam = takeNextEnabledParam(urlParam.getKey());
            if (currentParam == null) {
                return urlParam;
            }
            return new HttpParam(true, urlParam.getKey(), urlParam.getValue(), currentParam.getDescription());
        }

        private HttpParam takeNextEnabledParam(String key) {
            Deque<HttpParam> params = enabledParamsByKey.get(key);
            return params == null ? null : params.pollFirst();
        }

        private List<HttpParam> disabledParams() {
            return disabledParams;
        }
    }
}
