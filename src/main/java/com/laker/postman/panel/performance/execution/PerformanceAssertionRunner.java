package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.performance.assertion.AssertionData;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
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

    public static void runAssertionNodes(List<DefaultMutableTreeNode> assertionNodes,
                                         HttpResponse resp,
                                         List<TestResult> testResults,
                                         AtomicReference<String> errorMsgRef) {
        for (DefaultMutableTreeNode sub : assertionNodes) {
            Object subObj = sub.getUserObject();
            if (!(subObj instanceof JMeterTreeNode subNode) || subNode.type != NodeType.ASSERTION || subNode.assertionData == null) {
                continue;
            }
            if (!subNode.enabled) {
                continue;
            }
            AssertionData assertion = subNode.assertionData;
            String type = assertion.type;
            boolean pass = false;
            if ("Response Code".equals(type)) {
                String op = assertion.operator;
                String valStr = assertion.value;
                try {
                    int expect = Integer.parseInt(valStr);
                    if ("=".equals(op)) pass = (resp.code == expect);
                    else if (">".equals(op)) pass = (resp.code > expect);
                    else if ("<".equals(op)) pass = (resp.code < expect);
                } catch (Exception ignored) {
                    log.warn("断言响应码格式错误: {}", valStr);
                }
            } else if ("Contains".equals(type)) {
                pass = resp.body.contains(assertion.content);
            } else if ("JSONPath".equals(type)) {
                String jsonPath = assertion.value;
                String expect = assertion.content;
                String actual = JsonPathUtil.extractJsonPath(resp.body, jsonPath);
                pass = Objects.equals(actual, expect);
            }
            if (!pass && CharSequenceUtil.isBlank(errorMsgRef.get())) {
                errorMsgRef.set(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_ASSERTION_FAILED, type, assertion.content));
            }
            testResults.add(new TestResult(type, pass, pass ? null : "断言失败"));
        }
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
