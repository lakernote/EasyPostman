package com.laker.postman.performance.core.plan;

import com.laker.postman.performance.core.config.CsvDataSetData;
import com.laker.postman.performance.core.controller.ConditionData;
import com.laker.postman.performance.core.controller.LoopData;
import com.laker.postman.performance.core.controller.WhileData;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PerformanceCorePlanDocumentCompiler {

    public PerformanceTestPlan compile(PerformanceCorePlanDocument document) {
        return compile(document == null ? null : document.getRoot());
    }

    public PerformanceTestPlan compile(PerformanceCorePlanNode root) {
        List<PerformanceThreadGroupPlan> threadGroups = new ArrayList<>();
        if (root == null || root.getType() == null) {
            return new PerformanceTestPlan(threadGroups);
        }

        if (root.getType() == NodeType.THREAD_GROUP) {
            if (root.isEnabled()) {
                threadGroups.add(compileThreadGroup(root));
            }
            return new PerformanceTestPlan(threadGroups);
        }

        if (root.getType() != NodeType.ROOT) {
            return new PerformanceTestPlan(threadGroups);
        }

        for (PerformanceCorePlanNode child : root.getChildren()) {
            if (child.getType() == NodeType.THREAD_GROUP && child.isEnabled()) {
                threadGroups.add(compileThreadGroup(child));
            }
        }
        return new PerformanceTestPlan(threadGroups);
    }

    public PerformanceCoreRequestSampler compileRequestSampler(PerformanceCorePlanNode requestNode) {
        if (requestNode == null || !requestNode.isEnabled() || requestNode.getType() != NodeType.REQUEST) {
            return null;
        }
        return compileRequest(requestNode);
    }

    private PerformanceThreadGroupPlan compileThreadGroup(PerformanceCorePlanNode node) {
        ThreadGroupData threadGroupData = PerformancePlanCoreDataCopies.copyThreadGroupData(node.getThreadGroupData());
        if (threadGroupData == null) {
            threadGroupData = new ThreadGroupData();
        }
        threadGroupData.normalize();
        return new PerformanceThreadGroupPlan(
                node.getName(),
                threadGroupData,
                compileCsvDataSet(node),
                compileElements(node)
        );
    }

    private CsvDataSetData compileCsvDataSet(PerformanceCorePlanNode threadGroupNode) {
        if (threadGroupNode == null) {
            return null;
        }
        for (PerformanceCorePlanNode child : threadGroupNode.getChildren()) {
            if (child.isEnabled() && child.getType() == NodeType.CSV_DATA_SET && child.getCsvDataSetData() != null) {
                return PerformancePlanCoreDataCopies.copyCsvDataSetData(child.getCsvDataSetData());
            }
        }
        return null;
    }

    private List<PerformancePlanElement> compileElements(PerformanceCorePlanNode parent) {
        List<PerformancePlanElement> elements = new ArrayList<>();
        if (parent == null) {
            return elements;
        }
        for (PerformanceCorePlanNode child : parent.getChildren()) {
            if (child == null || !child.isEnabled()) {
                continue;
            }
            PerformancePlanElement element = compileElement(child);
            if (element != null) {
                elements.add(element);
            }
        }
        return elements;
    }

    private PerformancePlanElement compileElement(PerformanceCorePlanNode node) {
        return switch (node.getType()) {
            case CSV_DATA_SET -> null;
            case SIMPLE -> compileSimple(node);
            case LOOP -> compileLoop(node);
            case CONDITION -> compileCondition(node);
            case WHILE -> compileWhile(node);
            case ONCE_ONLY -> compileOnceOnly(node);
            case TIMER -> new PerformanceTimerElement(node.getName(), node.getTimerData());
            case REQUEST -> compileRequest(node);
            case ASSERTION -> new PerformanceAssertionElement(node.getName(), node.getAssertionData());
            case EXTRACTOR -> new PerformanceExtractorElement(node.getName(), node.getExtractorData());
            case SSE_CONNECT, SSE_READ, WS_CONNECT, WS_SEND, WS_READ, WS_CLOSE -> compileProtocolStage(node);
            default -> null;
        };
    }

    private PerformanceSimpleController compileSimple(PerformanceCorePlanNode node) {
        return new PerformanceSimpleController(node.getName(), compileElements(node));
    }

    private PerformanceLoopController compileLoop(PerformanceCorePlanNode node) {
        LoopData loopData = PerformancePlanCoreDataCopies.copyLoopData(node.getLoopData());
        if (loopData == null) {
            loopData = new LoopData();
        }
        loopData.normalize();
        return new PerformanceLoopController(node.getName(), loopData, compileElements(node));
    }

    private PerformanceConditionController compileCondition(PerformanceCorePlanNode node) {
        ConditionData conditionData = PerformancePlanCoreDataCopies.copyConditionData(node.getConditionData());
        if (conditionData == null) {
            conditionData = new ConditionData();
        }
        conditionData.normalize();
        return new PerformanceConditionController(node.getName(), conditionData, compileElements(node));
    }

    private PerformanceWhileController compileWhile(PerformanceCorePlanNode node) {
        WhileData whileData = PerformancePlanCoreDataCopies.copyWhileData(node.getWhileData());
        if (whileData == null) {
            whileData = new WhileData();
        }
        whileData.normalize();
        return new PerformanceWhileController(node.getName(), whileData, compileElements(node));
    }

    private PerformanceOnceOnlyController compileOnceOnly(PerformanceCorePlanNode node) {
        return new PerformanceOnceOnlyController(node.getName(), compileElements(node));
    }

    private PerformanceCoreRequestSampler compileRequest(PerformanceCorePlanNode node) {
        return new PerformanceCoreRequestSampler(
                node.getName(),
                node.getRequestSnapshot(),
                node.getWebSocketPerformanceData(),
                compileElements(node)
        );
    }

    private PerformanceProtocolStageElement compileProtocolStage(PerformanceCorePlanNode node) {
        return new PerformanceProtocolStageElement(
                node.getName(),
                node.getType(),
                node.getSsePerformanceData(),
                node.getWebSocketPerformanceData(),
                compileElements(node)
        );
    }
}
