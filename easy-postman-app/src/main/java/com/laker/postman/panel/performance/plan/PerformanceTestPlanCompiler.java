package com.laker.postman.panel.performance.plan;

import com.laker.postman.panel.performance.config.CsvDataSetData;
import com.laker.postman.panel.performance.controller.LoopData;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;

public final class PerformanceTestPlanCompiler {
    private PerformanceTestPlanCompiler() {
    }

    public static PerformanceTestPlan compile(DefaultMutableTreeNode snapshotRoot) {
        List<PerformanceThreadGroupPlan> threadGroups = new ArrayList<>();
        JMeterTreeNode rootData = nodeData(snapshotRoot);
        if (rootData == null) {
            return new PerformanceTestPlan(threadGroups);
        }

        if (rootData.type == NodeType.THREAD_GROUP) {
            if (rootData.enabled) {
                threadGroups.add(compileThreadGroup(snapshotRoot, rootData));
            }
            return new PerformanceTestPlan(threadGroups);
        }

        if (rootData.type != NodeType.ROOT) {
            return new PerformanceTestPlan(threadGroups);
        }

        for (int i = 0; i < snapshotRoot.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) snapshotRoot.getChildAt(i);
            JMeterTreeNode childData = nodeData(child);
            if (childData != null && childData.type == NodeType.THREAD_GROUP && childData.enabled) {
                threadGroups.add(compileThreadGroup(child, childData));
            }
        }
        return new PerformanceTestPlan(threadGroups);
    }

    private static PerformanceThreadGroupPlan compileThreadGroup(DefaultMutableTreeNode node, JMeterTreeNode data) {
        ThreadGroupData threadGroupData = PerformancePlanNodeCopies.copyThreadGroupData(data.threadGroupData);
        if (threadGroupData == null) {
            threadGroupData = new ThreadGroupData();
        }
        threadGroupData.normalize();
        return new PerformanceThreadGroupPlan(data.name, threadGroupData, compileCsvDataSet(node), compileElements(node));
    }

    private static CsvDataSetData compileCsvDataSet(DefaultMutableTreeNode threadGroupNode) {
        if (threadGroupNode == null) {
            return null;
        }
        for (int i = 0; i < threadGroupNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) threadGroupNode.getChildAt(i);
            JMeterTreeNode data = nodeData(child);
            if (data != null && data.enabled && data.type == NodeType.CSV_DATA_SET && data.csvDataSetData != null) {
                return PerformancePlanNodeCopies.copyCsvDataSetData(data.csvDataSetData);
            }
        }
        return null;
    }

    private static List<PerformancePlanElement> compileElements(DefaultMutableTreeNode parent) {
        List<PerformancePlanElement> elements = new ArrayList<>();
        if (parent == null) {
            return elements;
        }
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            JMeterTreeNode data = nodeData(child);
            if (data == null || !data.enabled) {
                continue;
            }
            PerformancePlanElement element = compileElement(child, data);
            if (element != null) {
                elements.add(element);
            }
        }
        return elements;
    }

    private static PerformancePlanElement compileElement(DefaultMutableTreeNode node, JMeterTreeNode data) {
        return switch (data.type) {
            case CSV_DATA_SET -> null;
            case LOOP -> compileLoop(node, data);
            case TIMER -> new PerformanceTimerElement(data.name, data.timerData);
            case REQUEST -> compileRequest(node, data);
            case ASSERTION -> new PerformanceAssertionElement(data.name, data.assertionData);
            case EXTRACTOR -> new PerformanceExtractorElement(data.name, data.extractorData);
            case SSE_CONNECT, SSE_AWAIT, WS_CONNECT, WS_SEND, WS_AWAIT, WS_CLOSE -> compileProtocolStage(node, data);
            default -> null;
        };
    }

    public static PerformanceRequestSampler compileRequestSampler(DefaultMutableTreeNode requestNode) {
        JMeterTreeNode data = nodeData(requestNode);
        if (data == null || !data.enabled || data.type != NodeType.REQUEST) {
            return null;
        }
        return compileRequest(requestNode, data);
    }

    private static PerformanceLoopController compileLoop(DefaultMutableTreeNode node, JMeterTreeNode data) {
        LoopData loopData = PerformancePlanNodeCopies.copyLoopData(data.loopData);
        if (loopData == null) {
            loopData = new LoopData();
        }
        loopData.normalize();
        return new PerformanceLoopController(data.name, loopData, compileElements(node));
    }

    private static PerformanceRequestSampler compileRequest(DefaultMutableTreeNode node, JMeterTreeNode data) {
        return new PerformanceRequestSampler(
                data.name,
                data.httpRequestItem,
                data.ssePerformanceData,
                data.webSocketPerformanceData,
                compileElements(node)
        );
    }

    private static PerformanceProtocolStageElement compileProtocolStage(DefaultMutableTreeNode node, JMeterTreeNode data) {
        return new PerformanceProtocolStageElement(
                data.name,
                data.type,
                data.ssePerformanceData,
                data.webSocketPerformanceData,
                compileElements(node)
        );
    }

    private static JMeterTreeNode nodeData(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof JMeterTreeNode data)) {
            return null;
        }
        return data;
    }
}
