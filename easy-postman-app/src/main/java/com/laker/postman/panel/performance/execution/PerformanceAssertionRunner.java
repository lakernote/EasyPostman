package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.performance.assertion.AssertionData;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.plan.PerformanceAssertionElement;
import com.laker.postman.panel.performance.plan.PerformancePlanElement;
import com.laker.postman.panel.performance.plan.PerformanceProtocolStageElement;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.JsonPathUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public final class PerformanceAssertionRunner {

    private static final String ASSERTION_TYPE_CONTAINS = "Contains";
    private static final String ASSERTION_TYPE_JSON_PATH = "JSONPath";

    private PerformanceAssertionRunner() {
    }

    public static List<DefaultMutableTreeNode> collectAssertionNodes(DefaultMutableTreeNode requestNode,
                                                                     boolean sseRequest,
                                                                     boolean webSocketRequest) {
        DefaultMutableTreeNode parent = requestNode;
        if (sseRequest) {
            parent = findChildNode(requestNode, NodeType.SSE_AWAIT);
        }
        return collectDirectAssertionNodes(parent != null ? parent : requestNode);
    }

    public static List<DefaultMutableTreeNode> collectDirectAssertionNodes(DefaultMutableTreeNode parentNode) {
        List<DefaultMutableTreeNode> nodes = new ArrayList<>();
        if (parentNode == null) {
            return nodes;
        }
        for (int i = 0; i < parentNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parentNode.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.ASSERTION) {
                nodes.add(child);
            }
        }
        return nodes;
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

    public static boolean requiresResponseBody(List<DefaultMutableTreeNode> assertionNodes) {
        if (assertionNodes == null || assertionNodes.isEmpty()) {
            return false;
        }
        for (DefaultMutableTreeNode node : assertionNodes) {
            Object userObj = node.getUserObject();
            if (!(userObj instanceof JMeterTreeNode jtNode) || jtNode.type != NodeType.ASSERTION
                    || !jtNode.enabled || jtNode.assertionData == null) {
                continue;
            }
            String type = jtNode.assertionData.type;
            if (ASSERTION_TYPE_CONTAINS.equals(type) || ASSERTION_TYPE_JSON_PATH.equals(type)) {
                return true;
            }
        }
        return false;
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
            String type = assertion.type;
            if (ASSERTION_TYPE_CONTAINS.equals(type) || ASSERTION_TYPE_JSON_PATH.equals(type)) {
                return true;
            }
        }
        return false;
    }

    public static void runAssertionNodes(List<DefaultMutableTreeNode> assertionNodes,
                                         HttpResponse resp,
                                         List<TestResult> testResults,
                                         AtomicReference<String> errorMsgRef) {
        // 压测线程不能因为空响应体导致断言阶段抛异常；空 body 应按断言失败处理并继续记录样本。
        String responseBody = resp != null && resp.body != null ? resp.body : "";
        for (DefaultMutableTreeNode sub : assertionNodes) {
            Object subObj = sub.getUserObject();
            if (!(subObj instanceof JMeterTreeNode subNode) || subNode.type != NodeType.ASSERTION || subNode.assertionData == null) {
                continue;
            }
            if (!subNode.enabled) {
                continue;
            }
            runAssertion(subNode.assertionData, responseBody, resp, testResults, errorMsgRef);
        }
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
            runAssertion(assertion, responseBody, resp, testResults, errorMsgRef);
        }
    }

    private static void runAssertion(AssertionData assertion,
                                     String responseBody,
                                     HttpResponse resp,
                                     List<TestResult> testResults,
                                     AtomicReference<String> errorMsgRef) {
        String type = assertion.type;
        boolean pass = false;
        if ("Response Code".equals(type)) {
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
        } else if (ASSERTION_TYPE_CONTAINS.equals(type)) {
            pass = CharSequenceUtil.isNotBlank(responseBody)
                    && CharSequenceUtil.isNotBlank(assertion.content)
                    && responseBody.contains(assertion.content);
        } else if (ASSERTION_TYPE_JSON_PATH.equals(type)) {
            String jsonPath = assertion.value;
            String expect = assertion.content;
            String actual = JsonPathUtil.extractJsonPath(responseBody, jsonPath);
            pass = Objects.equals(actual, expect);
        }
        if (!pass && CharSequenceUtil.isBlank(errorMsgRef.get())) {
            errorMsgRef.set(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_ASSERTION_FAILED, type, assertion.content));
        }
        testResults.add(new TestResult(type, pass, pass ? null : "断言失败"));
    }

    private static PerformanceProtocolStageElement findDirectStage(PerformanceRequestSampler requestSampler, NodeType type) {
        for (PerformancePlanElement element : requestSampler.getChildren()) {
            if (element instanceof PerformanceProtocolStageElement stage && stage.getType() == type) {
                return stage;
            }
        }
        return null;
    }

    private static DefaultMutableTreeNode findChildNode(DefaultMutableTreeNode parent, NodeType type) {
        if (parent == null) {
            return null;
        }
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == type) {
                return child;
            }
        }
        return null;
    }
}
