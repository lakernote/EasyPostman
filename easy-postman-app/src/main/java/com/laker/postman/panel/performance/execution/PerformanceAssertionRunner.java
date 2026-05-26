package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.performance.assertion.AssertionData;
import com.laker.postman.panel.performance.assertion.AssertionType;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.plan.PerformanceAssertionElement;
import com.laker.postman.panel.performance.plan.PerformancePlanElement;
import com.laker.postman.panel.performance.plan.PerformanceProtocolStageElement;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.JsonPathUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
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
            PerformanceProtocolStageElement awaitStage = findDirectStage(requestSampler, NodeType.SSE_AWAIT);
            parentElements = awaitStage == null ? parentElements : awaitStage.getElements();
        }
        return collectDirectAssertionElements(parentElements);
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
        boolean pass = false;
        switch (type) {
            case RESPONSE_CODE -> {
                String op = assertion.operator;
                String valStr = assertion.value;
                try {
                    int expect = Integer.parseInt(valStr);
                    if (resp != null) {
                        if ("=".equals(op)) pass = (resp.code == expect);
                        else if (">".equals(op)) pass = (resp.code > expect);
                        else if ("<".equals(op)) pass = (resp.code < expect);
                    }
                } catch (Exception ignored) {
                    log.warn("断言响应码格式错误: {}", valStr);
                }
            }
            case CONTAINS -> pass = CharSequenceUtil.isNotBlank(responseBody)
                    && CharSequenceUtil.isNotBlank(assertion.content)
                    && responseBody.contains(assertion.content);
            case JSON_PATH -> {
                String jsonPath = assertion.value;
                String expect = assertion.content;
                String actual = JsonPathUtil.extractJsonPath(responseBody, jsonPath);
                pass = Objects.equals(actual, expect);
            }
        }
        if (!pass && CharSequenceUtil.isBlank(errorMsgRef.get())) {
            errorMsgRef.set(I18nUtil.getMessage(
                    MessageKeys.PERFORMANCE_MSG_ASSERTION_FAILED,
                    type.displayName(),
                    assertion.content
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
            return extractLastSseDataPayload(responseBody);
        }
        return responseBody;
    }

    private static String extractLastSseDataPayload(String responseBody) {
        if (CharSequenceUtil.isBlank(responseBody)) {
            return responseBody;
        }
        StringBuilder currentEventData = null;
        String lastEventData = null;
        String[] lines = responseBody.split("\\R", -1);
        for (String line : lines) {
            if (line.isBlank()) {
                if (currentEventData != null) {
                    lastEventData = currentEventData.toString();
                    currentEventData = null;
                }
                continue;
            }
            if (!line.startsWith("data:")) {
                continue;
            }
            String dataLine = line.length() > 5 && line.charAt(5) == ' '
                    ? line.substring(6)
                    : line.substring(5);
            if (currentEventData == null) {
                currentEventData = new StringBuilder();
            } else {
                currentEventData.append('\n');
            }
            currentEventData.append(dataLine);
        }
        if (currentEventData != null) {
            lastEventData = currentEventData.toString();
        }
        return lastEventData == null ? responseBody : lastEventData;
    }

    private static PerformanceProtocolStageElement findDirectStage(PerformanceRequestSampler requestSampler, NodeType type) {
        for (PerformancePlanElement element : requestSampler.getChildren()) {
            if (element instanceof PerformanceProtocolStageElement stage && stage.getType() == type) {
                return stage;
            }
        }
        return null;
    }
}
