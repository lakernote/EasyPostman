package com.laker.postman.performance.plan;

import com.laker.postman.performance.core.plan.PerformanceTestPlan;

public final class PerformanceTestPlanCompiler {

    private PerformanceTestPlanCompiler() {
    }

    public static PerformanceTestPlan compile(PerformanceTestPlanNode root) {
        return PerformancePlanDocumentCompiler.compile(root == null ? null : root.toPlanNode());
    }

    public static PerformanceTestPlan compile(PerformancePlanNode root) {
        return PerformancePlanDocumentCompiler.compile(root);
    }

    public static PerformanceRequestSampler compileRequestSampler(PerformanceTestPlanNode requestNode) {
        return PerformancePlanDocumentCompiler.compileRequestSampler(requestNode == null ? null : requestNode.toPlanNode());
    }

    public static PerformanceRequestSampler compileRequestSampler(PerformancePlanNode requestNode) {
        return PerformancePlanDocumentCompiler.compileRequestSampler(requestNode);
    }
}
