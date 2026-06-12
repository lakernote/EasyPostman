package com.laker.postman.performance.core.plan;

import com.laker.postman.performance.core.model.NodeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PerformanceSimpleController implements PerformanceController {
    private final String name;
    private final List<PerformancePlanElement> elements;

    public PerformanceSimpleController(String name, List<PerformancePlanElement> elements) {
        this.name = name;
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements == null ? List.of() : elements));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NodeType getType() {
        return NodeType.SIMPLE;
    }

    @Override
    public int getIterationCount() {
        return 1;
    }

    @Override
    public List<PerformancePlanElement> getElements() {
        return elements;
    }
}
