package com.laker.postman.panel.performance.config;

import com.laker.postman.common.component.CsvDataPanel;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public final class CsvPerformanceIterationDataProvider implements PerformanceIterationDataProvider {
    private final CsvDataPanel csvDataPanel;

    @Override
    public Map<String, String> dataForVirtualUser(int virtualUserIndex) {
        if (csvDataPanel == null || !csvDataPanel.hasData()) {
            return null;
        }
        int rowCount = csvDataPanel.getRowCount();
        if (rowCount <= 0) {
            return null;
        }
        int rowIndex = Math.max(0, virtualUserIndex) % rowCount;
        return csvDataPanel.getRowData(rowIndex);
    }
}
