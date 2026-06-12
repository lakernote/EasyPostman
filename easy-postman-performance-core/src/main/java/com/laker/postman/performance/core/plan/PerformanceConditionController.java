package com.laker.postman.performance.core.plan;

import com.laker.postman.performance.core.controller.ConditionData;
import com.laker.postman.performance.core.model.NodeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PerformanceConditionController implements PerformanceElementContainer {
    private final String name;
    private final ConditionData conditionData;
    private final List<PerformancePlanElement> elements;

    public PerformanceConditionController(String name, ConditionData conditionData, List<PerformancePlanElement> elements) {
        this.name = name;
        this.conditionData = PerformancePlanCoreDataCopies.copyConditionData(conditionData);
        if (this.conditionData != null) {
            this.conditionData.normalize();
        }
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements == null ? List.of() : elements));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NodeType getType() {
        return NodeType.CONDITION;
    }

    public ConditionData getConditionData() {
        return PerformancePlanCoreDataCopies.copyConditionData(conditionData);
    }

    @Override
    public List<PerformancePlanElement> getElements() {
        return elements;
    }
}
