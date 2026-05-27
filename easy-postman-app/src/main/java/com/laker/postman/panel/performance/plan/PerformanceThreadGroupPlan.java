package com.laker.postman.panel.performance.plan;

import com.laker.postman.panel.performance.config.CsvDataSetData;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PerformanceThreadGroupPlan {
    private final String name;
    private final ThreadGroupData threadGroupData;
    private final CsvDataSetData csvDataSetData;
    private final List<PerformancePlanElement> elements;

    public PerformanceThreadGroupPlan(String name,
                                      ThreadGroupData threadGroupData,
                                      List<PerformancePlanElement> elements) {
        this(name, threadGroupData, null, elements);
    }

    public PerformanceThreadGroupPlan(String name,
                                      ThreadGroupData threadGroupData,
                                      CsvDataSetData csvDataSetData,
                                      List<PerformancePlanElement> elements) {
        this.name = name;
        this.threadGroupData = PerformancePlanNodeCopies.copyThreadGroupData(threadGroupData);
        if (this.threadGroupData != null) {
            this.threadGroupData.normalize();
        }
        this.csvDataSetData = PerformancePlanNodeCopies.copyCsvDataSetData(csvDataSetData);
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements == null ? List.of() : elements));
    }

    public String getName() {
        return name;
    }

    public ThreadGroupData getThreadGroupData() {
        return PerformancePlanNodeCopies.copyThreadGroupData(threadGroupData);
    }

    public CsvDataSetData getCsvDataSetData() {
        return PerformancePlanNodeCopies.copyCsvDataSetData(csvDataSetData);
    }

    public List<PerformancePlanElement> getElements() {
        return elements;
    }
}
