package com.laker.postman.panel.performance.execution;

import com.laker.postman.performance.core.assertion.AssertionData;
import com.laker.postman.performance.core.assertion.AssertionType;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceAssertionElement;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceProtocolStageElement;


import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.service.variable.VariableResolver;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.JsonPathUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public final class PerformanceAssertionRunner {

    private PerformanceAssertionRunner() {
    }

    public static List<PerformanceAssertionElement> collectAssertionElements(PerformanceRequestSampler requestSampler,
                                                                            boolean sseRequest,
                                                                            boolean webSocketRequest) {
        if (requestSampler == null) {
            return List.of();
        }
        List<PerformancePlanElement> parentElements = requestSampler.getChildren();
        if (sseRequest) {
            List<PerformanceAssertionElement> assertions = collectDirectAssertionsFromStages(requestSampler, NodeType.SSE_READ);
            if (assertions != null) {
                return assertions;
            }
        }
        return collectDirectAssertionElements(parentElements);
    }

    private static List<PerformanceAssertionElement> collectDirectAssertionsFromStages(PerformanceRequestSampler requestSampler,
                                                                                      NodeType type) {
        List<PerformanceAssertionElement> assertions = new ArrayList<>();
        boolean foundStage = false;
        for (PerformancePlanElement element : requestSampler.getChildren()) {
            if (element instanceof PerformanceProtocolStageElement stage && stage.getType() == type) {
                foundStage = true;
                assertions.addAll(collectDirectAssertionElements(stage.getElements()));
            }
        }
        return foundStage ? assertions : null;
    }

    public static List<PerformanceAssertionElement> collectDirectAssertionElements(List<PerformancePlanElement> elements) {
        List<PerformanceAssertionElement> assertions = new ArrayList<>();
        if (elements == null || elements.isEmpty()) {
            return assertions;
        }
        for (PerformancePlanElement element : elements) {
            if (element instanceof PerformanceAssertionElement assertionElement) {
                assertions.add(assertionElement);
            }
        }
        return assertions;
    }

    public static boolean requiresResponseBodyElements(List<PerformanceAssertionElement> assertionElements) {
        if (assertionElements == null || assertionElements.isEmpty()) {
            return false;
        }
        for (PerformanceAssertionElement element : assertionElements) {
            AssertionData assertion = element.getAssertionData();
            if (assertion == null) {
                continue;
            }
            if (AssertionType.fromStorageValue(assertion.type).requiresResponseBody()) {
                return true;
            }
        }
        return false;
    }

    public static void runAssertionElements(List<PerformanceAssertionElement> assertionElements,
                                            HttpResponse resp,
                                            List<TestResult> testResults,
                                            AtomicReference<String> errorMsgRef) {
        String responseBody = resp != null && resp.body != null ? resp.body : "";
        for (PerformanceAssertionElement element : assertionElements) {
            AssertionData assertion = element.getAssertionData();
            if (assertion == null) {
                continue;
            }
            runAssertion(assertion, responseBodyForAssertion(assertion, resp, responseBody), resp, testResults, errorMsgRef);
        }
    }

    private static void runAssertion(AssertionData assertion,
                                     String responseBody,
                                     HttpResponse resp,
                                     List<TestResult> testResults,
                                     AtomicReference<String> errorMsgRef) {
        AssertionType type = AssertionType.fromStorageValue(assertion.type);
        String operator = VariableResolver.resolve(CharSequenceUtil.nullToEmpty(assertion.operator));
        String content = VariableResolver.resolve(CharSequenceUtil.nullToEmpty(assertion.content));
        String value = VariableResolver.resolve(CharSequenceUtil.nullToEmpty(assertion.value));
        boolean pass = false;
        switch (type) {
            case RESPONSE_CODE -> {
                try {
                    int expect = Integer.parseInt(value);
                    if (resp != null) {
                        pass = compareNumber(resp.code, expect, operator);
                    }
                } catch (Exception ignored) {
                    log.warn("断言响应码格式错误: {}", value);
                }
            }
            case RESPONSE_TIME -> pass = resp != null && compareLong(resp.costMs, parseLong(value, Long.MIN_VALUE), operator);
            case CONTAINS -> pass = CharSequenceUtil.isNotBlank(responseBody)
                    && CharSequenceUtil.isNotBlank(content)
                    && responseBody.contains(content);
            case JSON_PATH -> {
                String actual = JsonPathUtil.extractJsonPath(responseBody, value);
                String expect = content;
                pass = Objects.equals(actual, expect);
            }
            case REGEX -> {
                String pattern = CharSequenceUtil.isNotBlank(content) ? content : value;
                try {
                    pass = CharSequenceUtil.isNotBlank(responseBody)
                            && CharSequenceUtil.isNotBlank(pattern)
                            && PerformanceRegexPatternCache.compileDotAll(pattern).matcher(responseBody).find();
                } catch (Exception e) {
                    log.debug("断言正则格式错误: {}", pattern);
                }
            }
            case HEADER_EXISTS -> pass = findHeader(resp == null ? null : resp.headers, content) != null;
            case HEADER_EQUALS -> pass = Objects.equals(findHeader(resp == null ? null : resp.headers, content), value);
            case BODY_SIZE -> pass = resp != null && compareLong(resp.bodySize, parseLong(value, Long.MIN_VALUE), operator);
        }
        if (!pass && CharSequenceUtil.isBlank(errorMsgRef.get())) {
            errorMsgRef.set(I18nUtil.getMessage(
                    MessageKeys.PERFORMANCE_MSG_ASSERTION_FAILED,
                    I18nUtil.getMessage(type.getMessageKey()),
                    CharSequenceUtil.blankToDefault(content, value)
            ));
        }
        testResults.add(new TestResult(
                type.getStorageValue(),
                pass,
                pass ? null : I18nUtil.getMessage(MessageKeys.PERFORMANCE_ASSERTION_FAILED)
        ));
    }

    private static String responseBodyForAssertion(AssertionData assertion, HttpResponse resp, String responseBody) {
        AssertionType type = AssertionType.fromStorageValue(assertion.type);
        if (type == AssertionType.JSON_PATH && resp != null && resp.isSse) {
            return PerformanceResponseBodyViews.extractLastSseDataPayload(responseBody);
        }
        return responseBody;
    }

    private static boolean compareNumber(int actual, int expected, String operator) {
        return compareLong(actual, expected, operator);
    }

    private static boolean compareLong(long actual, long expected, String operator) {
        if (expected == Long.MIN_VALUE) {
            return false;
        }
        return switch (operator) {
            case ">" -> actual > expected;
            case "<" -> actual < expected;
            case ">=" -> actual >= expected;
            case "<=" -> actual <= expected;
            case "!=", "≠" -> actual != expected;
            default -> actual == expected;
        };
    }

    private static long parseLong(String value, long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
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
}
