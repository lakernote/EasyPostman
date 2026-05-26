package com.laker.postman.panel.performance.tree;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.performance.controller.LoopData;
import com.laker.postman.panel.performance.extractor.ExtractorData;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;

@UtilityClass
public class PerformanceTreeNodeFactory {

    public void addDefaultRequest(DefaultMutableTreeNode root) {
        DefaultMutableTreeNode group = new DefaultMutableTreeNode(
                new JMeterTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_THREAD_GROUP), NodeType.THREAD_GROUP)
        );
        HttpRequestItem defaultReq = new HttpRequestItem();
        defaultReq.setName(I18nUtil.getMessage(MessageKeys.PERFORMANCE_DEFAULT_REQUEST));
        defaultReq.setMethod("GET");
        defaultReq.setUrl("https://www.baidu.com");
        DefaultMutableTreeNode req = new DefaultMutableTreeNode(
                new JMeterTreeNode(defaultReq.getName(), NodeType.REQUEST, defaultReq)
        );
        group.add(req);
        root.add(group);
    }

    DefaultMutableTreeNode loopNode() {
        LoopData data = new LoopData();
        return new DefaultMutableTreeNode(
                new JMeterTreeNode(PerformanceTreeNodeTitleFormatter.loopTitle(data), NodeType.LOOP, data)
        );
    }

    DefaultMutableTreeNode timerNode() {
        return new DefaultMutableTreeNode(new JMeterTreeNode("Timer", NodeType.TIMER));
    }

    public DefaultMutableTreeNode extractorNode() {
        ExtractorData data = new ExtractorData();
        return new DefaultMutableTreeNode(
                new JMeterTreeNode(PerformanceTreeNodeTitleFormatter.extractorTitle(data), NodeType.EXTRACTOR, data)
        );
    }

    DefaultMutableTreeNode sseStageNode(NodeType type, SsePerformanceData requestData) {
        JMeterTreeNode nodeData;
        switch (type) {
            case SSE_CONNECT -> nodeData = new JMeterTreeNode(
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_NODE_CONNECT),
                    NodeType.SSE_CONNECT
            );
            case SSE_AWAIT -> nodeData = new JMeterTreeNode(
                    PerformanceTreeNodeTitleFormatter.sseAwaitTitle(requestData),
                    NodeType.SSE_AWAIT
            );
            default -> throw new IllegalArgumentException("Unsupported SSE stage type: " + type);
        }
        return new DefaultMutableTreeNode(nodeData);
    }

    DefaultMutableTreeNode webSocketStepNode(NodeType type, WebSocketPerformanceData requestDefaults) {
        JMeterTreeNode nodeData;
        switch (type) {
            case WS_CONNECT -> {
                nodeData = new JMeterTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_CONNECT), NodeType.WS_CONNECT);
                nodeData.webSocketPerformanceData = copyWebSocketData(requestDefaults);
            }
            case WS_SEND -> {
                nodeData = new JMeterTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_SEND), NodeType.WS_SEND);
                WebSocketPerformanceData stepData = copyWebSocketData(requestDefaults);
                stepData.sendMode = WebSocketPerformanceData.SendMode.REQUEST_BODY_ON_CONNECT;
                stepData.sendCount = 1;
                stepData.sendIntervalMs = 1000;
                nodeData.webSocketPerformanceData = stepData;
                nodeData.name = PerformanceTreeNodeTitleFormatter.webSocketSendTitle(stepData);
            }
            case WS_AWAIT -> {
                nodeData = new JMeterTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_AWAIT), NodeType.WS_AWAIT);
                WebSocketPerformanceData stepData = copyWebSocketData(requestDefaults);
                stepData.completionMode = WebSocketPerformanceData.CompletionMode.FIRST_MESSAGE;
                stepData.firstMessageTimeoutMs = Math.max(100, stepData.firstMessageTimeoutMs);
                stepData.targetMessageCount = 1;
                nodeData.webSocketPerformanceData = stepData;
                nodeData.name = PerformanceTreeNodeTitleFormatter.webSocketAwaitTitle(stepData);
            }
            case WS_CLOSE -> {
                nodeData = new JMeterTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_CLOSE), NodeType.WS_CLOSE);
                nodeData.webSocketPerformanceData = copyWebSocketData(requestDefaults);
            }
            default -> throw new IllegalArgumentException("Unsupported WebSocket step type: " + type);
        }
        return new DefaultMutableTreeNode(nodeData);
    }

    private WebSocketPerformanceData copyWebSocketData(WebSocketPerformanceData source) {
        WebSocketPerformanceData target = new WebSocketPerformanceData();
        if (source == null) {
            return target;
        }
        target.connectTimeoutMs = source.connectTimeoutMs;
        target.sendMode = source.sendMode;
        target.sendContentSource = source.sendContentSource;
        target.customSendBody = source.customSendBody;
        target.sendPreScript = source.sendPreScript;
        target.sendCount = source.sendCount;
        target.sendIntervalMs = source.sendIntervalMs;
        target.completionMode = source.completionMode;
        target.firstMessageTimeoutMs = source.firstMessageTimeoutMs;
        target.holdConnectionMs = source.holdConnectionMs;
        target.targetMessageCount = source.targetMessageCount;
        target.messageFilter = source.messageFilter;
        return target;
    }
}
