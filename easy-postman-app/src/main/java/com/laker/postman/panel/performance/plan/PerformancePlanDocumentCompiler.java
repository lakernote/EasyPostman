package com.laker.postman.panel.performance.plan;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocumentCompiler;
import com.laker.postman.performance.core.plan.PerformanceCoreRequestSampler;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;


import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformancePlanDocumentCompiler {

    public PerformanceTestPlan compile(PerformancePlanDocument document) {
        PerformanceTestPlan corePlan = PerformanceCorePlanDocumentCompiler.compile(
                PerformanceCorePlanAdapter.toCoreDocument(document)
        );
        return PerformanceCorePlanAdapter.toGuiExecutablePlan(corePlan);
    }

    public PerformanceTestPlan compile(PerformancePlanNode root) {
        PerformanceTestPlan corePlan = PerformanceCorePlanDocumentCompiler.compile(
                PerformanceCorePlanAdapter.toCoreNode(root)
        );
        return PerformanceCorePlanAdapter.toGuiExecutablePlan(corePlan);
    }

    public PerformanceRequestSampler compileRequestSampler(PerformancePlanNode requestNode) {
        if (requestNode == null || !requestNode.isEnabled() || requestNode.getType() != NodeType.REQUEST) {
            return null;
        }
        PerformanceCoreRequestSampler coreSampler = PerformanceCorePlanDocumentCompiler.compileRequestSampler(
                PerformanceCorePlanAdapter.toCoreNode(requestNode)
        );
        return PerformanceCorePlanAdapter.toAppRequestSampler(coreSampler);
    }
}
