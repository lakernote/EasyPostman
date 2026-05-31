package com.laker.postman.performance.plan;

import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformanceTestPlanCompiler {

    public PerformanceTestPlan compile(PerformanceTestPlanNode root) {
        return PerformancePlanDocumentCompiler.compile(root == null ? null : root.toPlanNode());
    }

    public PerformanceTestPlan compile(PerformancePlanNode root) {
        return PerformancePlanDocumentCompiler.compile(root);
    }

    public PerformanceRequestSampler compileRequestSampler(PerformanceTestPlanNode requestNode) {
        return PerformancePlanDocumentCompiler.compileRequestSampler(requestNode == null ? null : requestNode.toPlanNode());
    }

    public PerformanceRequestSampler compileRequestSampler(PerformancePlanNode requestNode) {
        return PerformancePlanDocumentCompiler.compileRequestSampler(requestNode);
    }
}
