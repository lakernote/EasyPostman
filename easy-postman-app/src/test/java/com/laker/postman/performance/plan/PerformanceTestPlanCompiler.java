package com.laker.postman.performance.plan;

import com.laker.postman.panel.performance.tree.PerformanceSwingTreePlanAdapter;

import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;

@UtilityClass
public class PerformanceTestPlanCompiler {

    public PerformanceTestPlan compile(DefaultMutableTreeNode snapshotRoot) {
        return PerformancePlanDocumentCompiler.compile(PerformanceSwingTreePlanAdapter.toDocument(snapshotRoot));
    }

    public PerformanceRequestSampler compileRequestSampler(DefaultMutableTreeNode requestNode) {
        return PerformancePlanDocumentCompiler.compileRequestSampler(
                PerformanceSwingTreePlanAdapter.toDocumentNode(requestNode)
        );
    }
}
