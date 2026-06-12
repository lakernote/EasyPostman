package com.laker.postman.performance.core.plan;

import java.util.List;

public interface PerformanceElementContainer extends PerformancePlanElement {

    List<PerformancePlanElement> getElements();
}
