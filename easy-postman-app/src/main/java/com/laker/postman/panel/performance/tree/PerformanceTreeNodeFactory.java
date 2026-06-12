package com.laker.postman.panel.performance.tree;

import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.performance.core.controller.ConditionData;
import com.laker.postman.performance.core.controller.LoopData;
import com.laker.postman.performance.core.controller.WhileData;
import com.laker.postman.performance.core.extractor.ExtractorData;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.SsePerformanceData;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;


import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;

@UtilityClass
public class PerformanceTreeNodeFactory {

    public void addDefaultRequest(DefaultMutableTreeNode root) {
        DefaultMutableTreeNode group = new DefaultMutableTreeNode(
                new PerformanceTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_THREAD_GROUP), NodeType.THREAD_GROUP)
        );
        HttpRequestItem defaultReq = new HttpRequestItem();
        defaultReq.setName(I18nUtil.getMessage(MessageKeys.PERFORMANCE_DEFAULT_REQUEST));
        defaultReq.setMethod("GET");
        defaultReq.setUrl("https://www.baidu.com");
        DefaultMutableTreeNode req = new DefaultMutableTreeNode(
                new PerformanceTreeNode(defaultReq.getName(), NodeType.REQUEST, defaultReq)
        );
        group.add(req);
        root.add(group);
    }

    DefaultMutableTreeNode loopNode() {
        LoopData data = new LoopData();
        return new DefaultMutableTreeNode(
                new PerformanceTreeNode(PerformanceTreeNodeTitleFormatter.loopTitle(data), NodeType.LOOP, data)
        );
    }

    DefaultMutableTreeNode simpleNode() {
        return new DefaultMutableTreeNode(
                new PerformanceTreeNode(PerformanceTreeNodeTitleFormatter.simpleTitle(), NodeType.SIMPLE)
        );
    }

    DefaultMutableTreeNode conditionNode() {
        ConditionData data = new ConditionData();
        return new DefaultMutableTreeNode(
                new PerformanceTreeNode(PerformanceTreeNodeTitleFormatter.conditionTitle(data), NodeType.CONDITION, data)
        );
    }

    DefaultMutableTreeNode whileNode() {
        WhileData data = new WhileData();
        return new DefaultMutableTreeNode(
                new PerformanceTreeNode(PerformanceTreeNodeTitleFormatter.whileTitle(data), NodeType.WHILE, data)
        );
    }

    DefaultMutableTreeNode onceOnlyNode() {
        return new DefaultMutableTreeNode(
                new PerformanceTreeNode(PerformanceTreeNodeTitleFormatter.onceOnlyTitle(), NodeType.ONCE_ONLY)
        );
    }

    public DefaultMutableTreeNode csvDataSetNode() {
        return new DefaultMutableTreeNode(new PerformanceTreeNode(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_CSV_DATA_SET_NODE),
                NodeType.CSV_DATA_SET
        ));
    }

    DefaultMutableTreeNode timerNode() {
        return new DefaultMutableTreeNode(new PerformanceTreeNode("Timer", NodeType.TIMER));
    }

    public DefaultMutableTreeNode extractorNode() {
        ExtractorData data = new ExtractorData();
        return new DefaultMutableTreeNode(
                new PerformanceTreeNode(PerformanceTreeNodeTitleFormatter.extractorTitle(data), NodeType.EXTRACTOR, data)
        );
    }

    DefaultMutableTreeNode sseStageNode(NodeType type) {
        PerformanceTreeNode nodeData;
        switch (type) {
            case SSE_CONNECT -> {
                nodeData = new PerformanceTreeNode(
                        I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_NODE_CONNECT),
                        NodeType.SSE_CONNECT
                );
                nodeData.ssePerformanceData = new SsePerformanceData();
            }
            case SSE_READ -> {
                nodeData = new PerformanceTreeNode(
                        I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_NODE_READ),
                        NodeType.SSE_READ
                );
                nodeData.ssePerformanceData = new SsePerformanceData();
                nodeData.name = PerformanceTreeNodeTitleFormatter.sseReadTitle(nodeData.ssePerformanceData);
            }
            default -> throw new IllegalArgumentException("Unsupported SSE stage type: " + type);
        }
        return new DefaultMutableTreeNode(nodeData);
    }

    DefaultMutableTreeNode webSocketStepNode(NodeType type, WebSocketPerformanceData requestDefaults) {
        PerformanceTreeNode nodeData;
        switch (type) {
            case WS_CONNECT -> {
                nodeData = new PerformanceTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_CONNECT), NodeType.WS_CONNECT);
                nodeData.webSocketPerformanceData = copyWebSocketData(requestDefaults);
            }
            case WS_SEND -> {
                nodeData = new PerformanceTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_SEND), NodeType.WS_SEND);
                WebSocketPerformanceData stepData = copyWebSocketData(requestDefaults);
                stepData.sendMode = WebSocketPerformanceData.SendMode.REQUEST_BODY_ON_CONNECT;
                stepData.sendCount = 1;
                stepData.sendIntervalMs = 1000;
                nodeData.webSocketPerformanceData = stepData;
                nodeData.name = PerformanceTreeNodeTitleFormatter.webSocketSendTitle(stepData);
            }
            case WS_READ -> {
                nodeData = new PerformanceTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_READ), NodeType.WS_READ);
                WebSocketPerformanceData stepData = copyWebSocketData(requestDefaults);
                stepData.completionMode = WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE;
                stepData.firstMessageTimeoutMs = Math.max(100, stepData.firstMessageTimeoutMs);
                stepData.targetMessageCount = 1;
                nodeData.webSocketPerformanceData = stepData;
                nodeData.name = PerformanceTreeNodeTitleFormatter.webSocketReadTitle(stepData);
            }
            case WS_CLOSE -> {
                nodeData = new PerformanceTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_CLOSE), NodeType.WS_CLOSE);
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
