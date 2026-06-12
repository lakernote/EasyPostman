package com.laker.postman.performance.core.plan;

import com.laker.postman.performance.core.controller.WhileData;
import com.laker.postman.performance.core.model.NodeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PerformanceWhileController implements PerformanceElementContainer {
    private final String name;
    private final WhileData whileData;
    private final List<PerformancePlanElement> elements;

    public PerformanceWhileController(String name, WhileData whileData, List<PerformancePlanElement> elements) {
        this.name = name;
        WhileData copiedData = PerformancePlanCoreDataCopies.copyWhileData(whileData);
        if (copiedData == null) {
            copiedData = new WhileData();
        }
        copiedData.normalize();
        this.whileData = copiedData;
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements == null ? List.of() : elements));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NodeType getType() {
        return NodeType.WHILE;
    }

    public WhileData getWhileData() {
        return PerformancePlanCoreDataCopies.copyWhileData(whileData);
    }

    @Override
    public List<PerformancePlanElement> getElements() {
        return elements;
    }
}
