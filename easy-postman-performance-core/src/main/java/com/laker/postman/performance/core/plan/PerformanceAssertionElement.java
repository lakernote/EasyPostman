package com.laker.postman.performance.core.plan;

import com.laker.postman.performance.core.assertion.AssertionData;
import com.laker.postman.performance.core.model.NodeType;

public final class PerformanceAssertionElement implements PerformancePlanElement {
    private final String name;
    private final AssertionData assertionData;

    public PerformanceAssertionElement(String name, AssertionData assertionData) {
        this.name = name;
        this.assertionData = PerformancePlanCoreDataCopies.copyAssertionData(assertionData);
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
        return PerformancePlanCoreDataCopies.copyAssertionData(assertionData);
    }
}
