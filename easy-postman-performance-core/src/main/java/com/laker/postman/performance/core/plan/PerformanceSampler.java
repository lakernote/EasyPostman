package com.laker.postman.performance.core.plan;

import java.util.List;

public interface PerformanceSampler extends PerformancePlanElement {

    List<PerformancePlanElement> getChildren();

    boolean executesChildrenInSamplerOrder();
}
