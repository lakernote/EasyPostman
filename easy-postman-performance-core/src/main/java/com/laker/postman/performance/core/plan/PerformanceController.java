package com.laker.postman.performance.core.plan;

import java.util.List;

public interface PerformanceController extends PerformancePlanElement {

    int getIterationCount();

    List<PerformancePlanElement> getElements();
}
