package com.laker.postman.panel.performance.plan;

import java.util.List;

public interface PerformanceSampler extends PerformancePlanElement {

    List<PerformancePlanElement> getChildren();

    boolean executesChildrenInSamplerOrder();
}
