package com.laker.postman.performance.plan;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceAssertionElement;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.performance.core.plan.PerformanceCoreRequestSampler;
import com.laker.postman.performance.core.plan.PerformanceExtractorElement;
import com.laker.postman.performance.core.plan.PerformanceLoopController;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceProtocolStageElement;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.plan.PerformanceTimerElement;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PerformanceCorePlanAdapter {

    public PerformanceCorePlanDocument toCoreDocument(PerformancePlanDocument document) {
        return new PerformanceCorePlanDocument(toCoreNode(document == null ? null : document.getRoot()));
    }

    public PerformancePlanDocument toAppDocument(PerformanceCorePlanDocument document) {
        PerformancePlanNode root = toAppNode(document == null ? null : document.getRoot());
        return root == null ? null : new PerformancePlanDocument(root);
    }

    public PerformanceCorePlanNode toCoreNode(PerformancePlanNode node) {
        if (node == null) {
            return null;
        }
        return PerformanceCorePlanNode.builder()
                .name(node.getName())
                .type(node.getType())
                .enabled(node.isEnabled())
                .threadGroupData(node.getThreadGroupData())
                .csvDataSetData(node.getCsvDataSetData())
                .loopData(node.getLoopData())
                .requestSnapshot(node.getRequestSnapshot())
                .assertionData(node.getAssertionData())
                .extractorData(node.getExtractorData())
                .timerData(node.getTimerData())
                .ssePerformanceData(node.getSsePerformanceData())
                .webSocketPerformanceData(node.getWebSocketPerformanceData())
                .requestInheritanceSnapshot(node.isRequestInheritanceSnapshot())
                .children(toCoreNodes(node.getChildren()))
                .build();
    }

    public PerformancePlanNode toAppNode(PerformanceCorePlanNode node) {
        if (node == null) {
            return null;
        }
        return PerformancePlanNode.builder()
                .name(node.getName())
                .type(node.getType())
                .enabled(node.isEnabled())
                .threadGroupData(node.getThreadGroupData())
                .csvDataSetData(node.getCsvDataSetData())
                .loopData(node.getLoopData())
                .requestSnapshot(node.getRequestSnapshot())
                .assertionData(node.getAssertionData())
                .extractorData(node.getExtractorData())
                .timerData(node.getTimerData())
                .ssePerformanceData(node.getSsePerformanceData())
                .webSocketPerformanceData(node.getWebSocketPerformanceData())
                .requestInheritanceSnapshot(node.isRequestInheritanceSnapshot())
                .children(toAppNodes(node.getChildren()))
                .build();
    }

    public PerformanceTestPlan toGuiExecutablePlan(PerformanceTestPlan corePlan) {
        if (corePlan == null) {
            return new PerformanceTestPlan(List.of());
        }
        List<PerformanceThreadGroupPlan> threadGroups = new ArrayList<>();
        for (PerformanceThreadGroupPlan group : corePlan.getThreadGroups()) {
            // worker 分片后的虚拟用户 offset 必须保留下来，否则 CSV 行会从第 0 行重新分配。
            threadGroups.add(new PerformanceThreadGroupPlan(
                    group.getName(),
                    group.getThreadGroupData(),
                    group.getCsvDataSetData(),
                    toAppElements(group.getElements()),
                    group.getVirtualUserIndexOffset()
            ));
        }
        return new PerformanceTestPlan(threadGroups);
    }

    public PerformanceRequestSampler toAppRequestSampler(PerformanceCoreRequestSampler sampler) {
        if (sampler == null) {
            return null;
        }
        return new PerformanceRequestSampler(
                sampler.getName(),
                null,
                sampler.getRequestSnapshot(),
                sampler.getWebSocketPerformanceData(),
                toAppElements(sampler.getChildren()),
                null
        );
    }

    private List<PerformanceCorePlanNode> toCoreNodes(List<PerformancePlanNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        List<PerformanceCorePlanNode> coreNodes = new ArrayList<>();
        for (PerformancePlanNode node : nodes) {
            PerformanceCorePlanNode coreNode = toCoreNode(node);
            if (coreNode != null) {
                coreNodes.add(coreNode);
            }
        }
        return coreNodes;
    }

    private List<PerformancePlanNode> toAppNodes(List<PerformanceCorePlanNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        List<PerformancePlanNode> appNodes = new ArrayList<>();
        for (PerformanceCorePlanNode node : nodes) {
            PerformancePlanNode appNode = toAppNode(node);
            if (appNode != null) {
                appNodes.add(appNode);
            }
        }
        return appNodes;
    }

    private List<PerformancePlanElement> toAppElements(List<PerformancePlanElement> elements) {
        if (elements == null || elements.isEmpty()) {
            return List.of();
        }
        List<PerformancePlanElement> appElements = new ArrayList<>();
        for (PerformancePlanElement element : elements) {
            PerformancePlanElement appElement = toAppElement(element);
            if (appElement != null) {
                appElements.add(appElement);
            }
        }
        return appElements;
    }

    private PerformancePlanElement toAppElement(PerformancePlanElement element) {
        if (element instanceof PerformanceCoreRequestSampler sampler) {
            return toAppRequestSampler(sampler);
        }
        if (element instanceof PerformanceLoopController loop) {
            return new PerformanceLoopController(
                    loop.getName(),
                    loop.getLoopData(),
                    toAppElements(loop.getElements())
            );
        }
        if (element instanceof PerformanceProtocolStageElement stage) {
            return new PerformanceProtocolStageElement(
                    stage.getName(),
                    stage.getType(),
                    stage.getSsePerformanceData(),
                    stage.getWebSocketPerformanceData(),
                    toAppElements(stage.getElements())
            );
        }
        if (element instanceof PerformanceTimerElement timer) {
            return new PerformanceTimerElement(timer.getName(), timer.getTimerData());
        }
        if (element instanceof PerformanceAssertionElement assertion) {
            return new PerformanceAssertionElement(assertion.getName(), assertion.getAssertionData());
        }
        if (element instanceof PerformanceExtractorElement extractor) {
            return new PerformanceExtractorElement(extractor.getName(), extractor.getExtractorData());
        }
        if (element != null && element.getType() == NodeType.CSV_DATA_SET) {
            return null;
        }
        return element;
    }
}
