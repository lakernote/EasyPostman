package com.laker.postman.panel.performance.plan;

import com.laker.postman.panel.performance.assertion.AssertionData;
import com.laker.postman.panel.performance.model.NodeType;

public final class PerformanceAssertionElement implements PerformancePlanElement {
    private final String name;
    private final AssertionData assertionData;

    public PerformanceAssertionElement(String name, AssertionData assertionData) {
        this.name = name;
        this.assertionData = PerformancePlanNodeCopies.copyAssertionData(assertionData);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NodeType getType() {
        return NodeType.ASSERTION;
    }

    public AssertionData getAssertionData() {
        return PerformancePlanNodeCopies.copyAssertionData(assertionData);
    }
}
