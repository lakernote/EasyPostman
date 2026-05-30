package com.laker.postman.performance.core.plan;

import com.laker.postman.performance.core.config.CsvDataSetData;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class PerformanceThreadGroupPlan {
    private final String name;
    private final ThreadGroupData threadGroupData;
    private final CsvDataSetData csvDataSetData;
    private final List<PerformancePlanElement> elements;
    private final int virtualUserIndexOffset;

    public PerformanceThreadGroupPlan(String name,
                                      ThreadGroupData threadGroupData,
                                      List<PerformancePlanElement> elements) {
        this(name, threadGroupData, null, elements);
    }

    public PerformanceThreadGroupPlan(String name,
                                      ThreadGroupData threadGroupData,
                                      CsvDataSetData csvDataSetData,
                                      List<PerformancePlanElement> elements) {
        this(name, threadGroupData, csvDataSetData, elements, 0);
    }

    public PerformanceThreadGroupPlan(String name,
                                      ThreadGroupData threadGroupData,
                                      CsvDataSetData csvDataSetData,
                                      List<PerformancePlanElement> elements,
                                      int virtualUserIndexOffset) {
        this.name = name;
        this.threadGroupData = PerformancePlanCoreDataCopies.copyThreadGroupData(threadGroupData);
        if (this.threadGroupData != null) {
            this.threadGroupData.normalize();
        }
        this.csvDataSetData = PerformancePlanCoreDataCopies.copyCsvDataSetData(csvDataSetData);
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements == null ? List.of() : elements));
        this.virtualUserIndexOffset = Math.max(0, virtualUserIndexOffset);
    }

    public String getName() {
        return name;
    }

    public ThreadGroupData getThreadGroupData() {
        return PerformancePlanCoreDataCopies.copyThreadGroupData(threadGroupData);
    }

    public CsvDataSetData getCsvDataSetData() {
        return PerformancePlanCoreDataCopies.copyCsvDataSetData(csvDataSetData);
    }

    public Map<String, String> csvRowForVirtualUser(int virtualUserIndex) {
        return csvDataSetData == null ? null : csvDataSetData.rowForVirtualUser(virtualUserIndexOffset + virtualUserIndex);
    }

    public List<PerformancePlanElement> getElements() {
        return elements;
    }

    public int getVirtualUserIndexOffset() {
        return virtualUserIndexOffset;
    }
}
