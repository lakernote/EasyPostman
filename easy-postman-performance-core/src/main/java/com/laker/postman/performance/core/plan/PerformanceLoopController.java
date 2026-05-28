package com.laker.postman.performance.core.plan;

import com.laker.postman.performance.core.controller.LoopData;
import com.laker.postman.performance.core.model.NodeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PerformanceLoopController implements PerformanceController {
    private final String name;
    private final LoopData loopData;
    private final List<PerformancePlanElement> elements;

    public PerformanceLoopController(String name, LoopData loopData, List<PerformancePlanElement> elements) {
        this.name = name;
        this.loopData = PerformancePlanCoreDataCopies.copyLoopData(loopData);
        if (this.loopData != null) {
            this.loopData.normalize();
        }
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements == null ? List.of() : elements));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NodeType getType() {
        return NodeType.LOOP;
    }

    public LoopData getLoopData() {
        return PerformancePlanCoreDataCopies.copyLoopData(loopData);
    }

    @Override
    public int getIterationCount() {
        return loopData == null ? LoopData.MIN_ITERATIONS : loopData.iterations;
    }

    @Override
    public List<PerformancePlanElement> getElements() {
        return elements;
    }
}
