package com.laker.postman.performance.core.plan;

import com.laker.postman.performance.core.model.NodeType;

public interface PerformancePlanElement {
    String getName();

    NodeType getType();
}
