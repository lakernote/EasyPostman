package com.laker.postman.panel.performance.runtime;

import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.service.variable.ExecutionVariableContext;
import com.laker.postman.service.variable.IterationDataRuntimeSupport;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public final class PerformanceIterationContextFactory {

    private final CsvDataPanel csvDataPanel;
    private final PerformanceVirtualUserCoordinator virtualUsers;

    public ExecutionVariableContext create(int iterationCount) {
        int iterationIndex = virtualUsers.nextIterationIndex();
        ExecutionVariableContext iterationContext = new ExecutionVariableContext();
        iterationContext.setIterationInfo(iterationIndex, iterationCount);
        iterationContext.replaceIterationData(
                IterationDataRuntimeSupport.prepare(resolveCsvRowForCurrentThread())
        );
        return iterationContext;
    }

    private Map<String, String> resolveCsvRowForCurrentThread() {
        if (csvDataPanel == null || !csvDataPanel.hasData()) {
            return null;
        }
        int rowCount = csvDataPanel.getRowCount();
        if (rowCount <= 0) {
            return null;
        }
        Integer vuIndex = virtualUsers.currentVirtualUserIndex();
        int rowIdx = (vuIndex != null ? vuIndex : 0) % rowCount;
        return csvDataPanel.getRowData(rowIdx);
    }
}
