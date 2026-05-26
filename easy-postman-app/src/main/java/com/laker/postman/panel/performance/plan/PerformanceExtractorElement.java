package com.laker.postman.panel.performance.plan;

import com.laker.postman.panel.performance.extractor.ExtractorData;
import com.laker.postman.panel.performance.model.NodeType;

public final class PerformanceExtractorElement implements PerformancePlanElement {
    private final String name;
    private final ExtractorData extractorData;

    public PerformanceExtractorElement(String name, ExtractorData extractorData) {
        this.name = name;
        this.extractorData = PerformancePlanNodeCopies.copyExtractorData(extractorData);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NodeType getType() {
        return NodeType.EXTRACTOR;
    }

    public ExtractorData getExtractorData() {
        return PerformancePlanNodeCopies.copyExtractorData(extractorData);
    }
}
