package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.plan.PerformanceAssertionElement;
import com.laker.postman.panel.performance.plan.PerformanceExtractorElement;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;

import java.util.List;
import java.util.regex.Pattern;

record PerformanceResponseCapturePlan(PreparedRequest.ResponseBodyMode httpResponseBodyMode,
                                      boolean runPostScript,
                                      boolean postScriptNeedsResponseBody,
                                      boolean retainStreamResponseBody,
                                      boolean retainWebSocketAwaitPayloads,
                                      boolean trackStreamResponseBodySize) {

    static PerformanceResponseCapturePlan resolve(boolean efficientMode,
                                                  PerformanceRequestSampler requestSampler,
                                                  boolean sseRequest,
                                                  boolean webSocketRequest,
                                                  String postscript) {
        List<PerformanceAssertionElement> assertionElements =
                PerformanceAssertionRunner.collectAssertionElements(requestSampler, sseRequest, webSocketRequest);
        List<PerformanceExtractorElement> extractorElements =
                PerformanceExtractorRunner.collectExtractorElements(requestSampler, sseRequest, webSocketRequest);
        boolean assertionNeedsResponseBody = PerformanceAssertionRunner.requiresResponseBodyElements(assertionElements);
        boolean extractorNeedsResponseBody = PerformanceExtractorRunner.requiresResponseBodyElements(extractorElements);
        boolean runPostScript = CharSequenceUtil.isNotBlank(postscript);
        boolean postScriptNeedsResponseBody = PerformancePostScriptResponseUsage.requiresResponseBody(postscript);
        boolean postScriptNeedsResponseSize = PerformancePostScriptResponseUsage.requiresResponseSize(postscript);
        boolean awaitStepNeedsResponseBody = webSocketRequest
                && WebSocketScenarioStepSupport.hasAwaitStepWithResponseBodyNode(requestSampler);
        boolean retainWebSocketAwaitPayloads = webSocketRequest
                && WebSocketScenarioStepSupport.hasAwaitStepRequiringPayload(requestSampler);
        boolean retainStreamResponseBody = assertionNeedsResponseBody
                || extractorNeedsResponseBody
                || postScriptNeedsResponseBody
                || awaitStepNeedsResponseBody;
        boolean trackStreamResponseBodySize = retainStreamResponseBody || postScriptNeedsResponseSize;

        return new PerformanceResponseCapturePlan(
                resolveHttpResponseBodyMode(efficientMode, assertionElements, extractorElements, postscript),
                runPostScript,
                postScriptNeedsResponseBody,
                retainStreamResponseBody,
                retainWebSocketAwaitPayloads,
                trackStreamResponseBodySize
        );
    }

    static PreparedRequest.ResponseBodyMode resolveHttpResponseBodyMode(boolean efficientMode,
                                                                        List<PerformanceAssertionElement> assertionElements,
                                                                        List<PerformanceExtractorElement> extractorElements,
                                                                        String postscript) {
        if (!efficientMode) {
            return PreparedRequest.ResponseBodyMode.FULL;
        }
        boolean requiresResponseBody = PerformanceAssertionRunner.requiresResponseBodyElements(assertionElements)
                || PerformanceExtractorRunner.requiresResponseBodyElements(extractorElements)
                || PerformancePostScriptResponseUsage.requiresResponseBody(postscript);
        if (requiresResponseBody) {
            return PreparedRequest.ResponseBodyMode.PREVIEW;
        }
        return PreparedRequest.ResponseBodyMode.METADATA_ONLY;
    }

    private static final class PerformancePostScriptResponseUsage {
        private static final List<Pattern> RESPONSE_BODY_ACCESS_PATTERNS = List.of(
                Pattern.compile("\\bresponseBody\\b"),
                Pattern.compile("\\bresponse\\s*\\.\\s*body\\b"),
                Pattern.compile("\\b(?:pm|postman)\\s*\\.\\s*response\\s*\\.\\s*(?:text|json)\\s*\\("),
                Pattern.compile("\\b(?:pm|postman)\\s*\\.\\s*response\\s*\\.\\s*body\\b"),
                Pattern.compile("\\b(?:pm|postman)\\s*\\.\\s*response\\s*\\.\\s*to\\s*\\.\\s*have\\s*\\.\\s*body\\b")
        );
        private static final List<Pattern> RESPONSE_SIZE_ACCESS_PATTERNS = List.of(
                Pattern.compile("\\bresponse\\s*\\.\\s*bodySize\\b"),
                Pattern.compile("\\b(?:pm|postman)\\s*\\.\\s*response\\s*\\.\\s*size\\s*\\(")
        );

        private PerformancePostScriptResponseUsage() {
        }

        private static boolean requiresResponseBody(String script) {
            if (CharSequenceUtil.isBlank(script)) {
                return false;
            }
            for (Pattern pattern : RESPONSE_BODY_ACCESS_PATTERNS) {
                if (pattern.matcher(script).find()) {
                    return true;
                }
            }
            return false;
        }

        private static boolean requiresResponseSize(String script) {
            if (CharSequenceUtil.isBlank(script)) {
                return false;
            }
            for (Pattern pattern : RESPONSE_SIZE_ACCESS_PATTERNS) {
                if (pattern.matcher(script).find()) {
                    return true;
                }
            }
            return false;
        }
    }
}
