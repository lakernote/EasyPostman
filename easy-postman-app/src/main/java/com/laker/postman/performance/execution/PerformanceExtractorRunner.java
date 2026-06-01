package com.laker.postman.performance.execution;

import com.laker.postman.performance.core.extractor.ExtractorData;
import com.laker.postman.performance.core.extractor.ExtractorType;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceExtractorElement;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceProtocolStageElement;


import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.performance.plan.PerformanceRequestSampler;
import com.laker.postman.service.variable.VariableResolver;
import com.laker.postman.service.variable.VariablesService;
import com.laker.postman.util.JsonPathUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

@Slf4j
public final class PerformanceExtractorRunner {

    private PerformanceExtractorRunner() {
    }

    public static List<PerformanceExtractorElement> collectExtractorElements(PerformanceRequestSampler requestSampler,
                                                                             boolean sseRequest,
                                                                             boolean webSocketRequest) {
        if (requestSampler == null) {
            return List.of();
        }
        List<PerformancePlanElement> parentElements = requestSampler.getChildren();
        if (sseRequest) {
            List<PerformanceExtractorElement> extractors = collectDirectExtractorsFromStages(requestSampler, NodeType.SSE_READ);
            if (extractors != null) {
                return extractors;
            }
        }
        return collectDirectExtractorElements(parentElements);
    }

    private static List<PerformanceExtractorElement> collectDirectExtractorsFromStages(PerformanceRequestSampler requestSampler,
                                                                                      NodeType type) {
        List<PerformanceExtractorElement> extractors = new ArrayList<>();
        boolean foundStage = false;
        for (PerformancePlanElement element : requestSampler.getChildren()) {
            if (element instanceof PerformanceProtocolStageElement stage && stage.getType() == type) {
                foundStage = true;
                extractors.addAll(collectDirectExtractorElements(stage.getElements()));
            }
        }
        return foundStage ? extractors : null;
    }

    public static List<PerformanceExtractorElement> collectDirectExtractorElements(List<PerformancePlanElement> elements) {
        List<PerformanceExtractorElement> extractors = new ArrayList<>();
        if (elements == null || elements.isEmpty()) {
            return extractors;
        }
        for (PerformancePlanElement element : elements) {
            if (element instanceof PerformanceExtractorElement extractorElement) {
                extractors.add(extractorElement);
            }
        }
        return extractors;
    }

    public static boolean requiresResponseBodyElements(List<PerformanceExtractorElement> extractorElements) {
        if (extractorElements == null || extractorElements.isEmpty()) {
            return false;
        }
        for (PerformanceExtractorElement element : extractorElements) {
            ExtractorData extractor = element.getExtractorData();
            if (extractor == null) {
                continue;
            }
            if (ExtractorType.fromStorageValue(extractor.type).requiresResponseBody()) {
                return true;
            }
        }
        return false;
    }

    public static void runExtractorElements(List<PerformanceExtractorElement> extractorElements,
                                            HttpResponse response) {
        if (extractorElements == null || extractorElements.isEmpty()) {
            return;
        }
        for (PerformanceExtractorElement element : extractorElements) {
            ExtractorData extractor = element.getExtractorData();
            if (extractor == null) {
                continue;
            }
            runExtractor(extractor, response);
        }
    }

    private static void runExtractor(ExtractorData extractor, HttpResponse response) {
        String variableName = VariableResolver.resolve(CharSequenceUtil.nullToEmpty(extractor.variableName)).trim();
        if (CharSequenceUtil.isBlank(variableName)) {
            return;
        }
        String expression = VariableResolver.resolve(CharSequenceUtil.nullToEmpty(extractor.expression)).trim();
        String defaultValue = VariableResolver.resolve(CharSequenceUtil.nullToEmpty(extractor.defaultValue));
        String extractedValue = extractValue(extractor, expression, response);
        VariablesService.getInstance().set(variableName, extractedValue == null ? defaultValue : extractedValue);
    }

    private static String extractValue(ExtractorData extractor, String expression, HttpResponse response) {
        ExtractorType type = ExtractorType.fromStorageValue(extractor.type);
        try {
            return switch (type) {
                case JSON_PATH -> JsonPathUtil.extractJsonPath(
                        PerformanceResponseBodyViews.bodyForBodyBasedNode(response),
                        expression
                );
                case REGEX -> extractRegex(
                        PerformanceResponseBodyViews.bodyForBodyBasedNode(response),
                        expression,
                        extractor.matchIndex,
                        extractor.groupIndex
                );
                case HEADER -> findHeader(response == null ? null : response.headers, expression);
                case COOKIE -> findCookie(response == null ? null : response.headers, expression);
            };
        } catch (Exception e) {
            log.debug("Performance extractor failed: type={}, expression={}, message={}",
                    type.getStorageValue(), expression, e.getMessage());
            return null;
        }
    }

    private static String extractRegex(String body, String expression, int matchIndex, int groupIndex) {
        if (CharSequenceUtil.isBlank(body) || CharSequenceUtil.isBlank(expression)) {
            return null;
        }
        int normalizedMatchIndex = Math.max(1, matchIndex);
        Matcher matcher = PerformanceRegexPatternCache.compileDotAll(expression).matcher(body);
        int current = 0;
        while (matcher.find()) {
            current++;
            if (current != normalizedMatchIndex) {
                continue;
            }
            int normalizedGroupIndex = Math.max(0, groupIndex);
            return normalizedGroupIndex <= matcher.groupCount() ? matcher.group(normalizedGroupIndex) : null;
        }
        return null;
    }

    private static String findHeader(Map<String, List<String>> headers, String name) {
        if (headers == null || headers.isEmpty() || CharSequenceUtil.isBlank(name)) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                List<String> values = entry.getValue();
                return values == null || values.isEmpty() ? "" : values.get(0);
            }
        }
        return null;
    }

    private static String findCookie(Map<String, List<String>> headers, String cookieName) {
        if (headers == null || headers.isEmpty() || CharSequenceUtil.isBlank(cookieName)) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() == null || !"Set-Cookie".equalsIgnoreCase(entry.getKey()) || entry.getValue() == null) {
                continue;
            }
            for (String setCookie : entry.getValue()) {
                String value = parseSetCookie(setCookie, cookieName);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private static String parseSetCookie(String setCookie, String cookieName) {
        if (CharSequenceUtil.isBlank(setCookie)) {
            return null;
        }
        int pairEnd = setCookie.indexOf(';');
        String pair = pairEnd >= 0 ? setCookie.substring(0, pairEnd) : setCookie;
        int equals = pair.indexOf('=');
        if (equals <= 0) {
            return null;
        }
        String name = pair.substring(0, equals).trim();
        if (!name.equals(cookieName)) {
            return null;
        }
        return pair.substring(equals + 1).trim();
    }

}
