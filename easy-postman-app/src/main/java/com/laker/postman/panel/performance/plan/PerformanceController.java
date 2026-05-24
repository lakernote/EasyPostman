package com.laker.postman.panel.performance.plan;

import java.util.List;

public interface PerformanceController extends PerformancePlanElement {

    int getIterationCount();

    List<PerformancePlanElement> getElements();
}
