package com.laker.postman.panel.performance;

import com.laker.postman.performance.core.assertion.AssertionData;
import com.laker.postman.performance.core.config.CsvDataSetData;
import com.laker.postman.performance.core.controller.LoopData;
import com.laker.postman.performance.core.extractor.ExtractorData;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.SsePerformanceData;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.performance.core.timer.TimerData;


import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.service.variable.RequestExecutionScope;
import com.laker.postman.util.JsonUtil;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.UUID;

@UtilityClass
public class PerformanceTreeSnapshot {

    public DefaultMutableTreeNode copy(DefaultMutableTreeNode source) {
        return copy(source, false);
    }

    public DefaultMutableTreeNode copyForPaste(DefaultMutableTreeNode source) {
        return copy(source, true);
    }

    private static DefaultMutableTreeNode copy(DefaultMutableTreeNode source, boolean regenerateRequestIds) {
        JMeterTreeNode sourceData = source != null && source.getUserObject() instanceof JMeterTreeNode jtNode
                ? jtNode
                : null;
        DefaultMutableTreeNode copy = new DefaultMutableTreeNode(copyNodeData(sourceData, regenerateRequestIds));
        if (source == null) {
            return copy;
        }
        for (int i = 0; i < source.getChildCount(); i++) {
            copy.add(copy((DefaultMutableTreeNode) source.getChildAt(i), regenerateRequestIds));
        }
        return copy;
    }

    private static JMeterTreeNode copyNodeData(JMeterTreeNode source, boolean regenerateRequestIds) {
        if (source == null) {
            return new JMeterTreeNode("", NodeType.ROOT);
        }
        JMeterTreeNode copy = new JMeterTreeNode(source.name, source.type);
        copy.enabled = source.enabled;
        copy.threadGroupData = JsonUtil.deepCopy(source.threadGroupData, ThreadGroupData.class);
        copy.csvDataSetData = JsonUtil.deepCopy(source.csvDataSetData, CsvDataSetData.class);
        copy.loopData = JsonUtil.deepCopy(source.loopData, LoopData.class);
        copy.httpRequestItem = JsonUtil.deepCopy(source.httpRequestItem, HttpRequestItem.class);
        if (regenerateRequestIds && copy.httpRequestItem != null) {
            copy.httpRequestItem.setId(UUID.randomUUID().toString());
        }
        copy.assertionData = JsonUtil.deepCopy(source.assertionData, AssertionData.class);
        copy.extractorData = JsonUtil.deepCopy(source.extractorData, ExtractorData.class);
        copy.timerData = JsonUtil.deepCopy(source.timerData, TimerData.class);
        copy.ssePerformanceData = JsonUtil.deepCopy(source.ssePerformanceData, SsePerformanceData.class);
        copy.webSocketPerformanceData = JsonUtil.deepCopy(source.webSocketPerformanceData, WebSocketPerformanceData.class);
        copy.requestExecutionScope = copyRequestExecutionScope(source.requestExecutionScope);
        copy.requestInheritanceSnapshot = source.requestInheritanceSnapshot;
        return copy;
    }

    private static RequestExecutionScope copyRequestExecutionScope(RequestExecutionScope source) {
        return source == null ? null : RequestExecutionScope.fromGroupVariables(source.getGroupVariables());
    }
}
