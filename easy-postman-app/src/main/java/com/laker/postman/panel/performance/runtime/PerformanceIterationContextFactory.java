package com.laker.postman.panel.performance.runtime;

import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.panel.performance.config.CsvPerformanceIterationDataProvider;
import com.laker.postman.panel.performance.config.PerformanceIterationDataProvider;
import com.laker.postman.service.variable.ExecutionVariableContext;
import com.laker.postman.service.variable.IterationDataRuntimeSupport;

import java.util.Map;

public final class PerformanceIterationContextFactory {

    private final PerformanceIterationDataProvider iterationDataProvider;
    private final PerformanceVirtualUserCoordinator virtualUsers;

    public PerformanceIterationContextFactory(CsvDataPanel csvDataPanel,
                                              PerformanceVirtualUserCoordinator virtualUsers) {
        this(new CsvPerformanceIterationDataProvider(csvDataPanel), virtualUsers);
    }

    public PerformanceIterationContextFactory(PerformanceIterationDataProvider iterationDataProvider,
                                              PerformanceVirtualUserCoordinator virtualUsers) {
        this.iterationDataProvider = iterationDataProvider == null
                ? PerformanceIterationDataProvider.empty()
                : iterationDataProvider;
        this.virtualUsers = virtualUsers;
    }

    public ExecutionVariableContext create(int iterationCount) {
        int iterationIndex = virtualUsers.nextIterationIndex();
        ExecutionVariableContext iterationContext = new ExecutionVariableContext();
        iterationContext.setIterationInfo(iterationIndex, iterationCount);
        iterationContext.replaceIterationData(
                IterationDataRuntimeSupport.prepare(resolveIterationDataForCurrentThread())
        );
        return iterationContext;
    }

    private Map<String, String> resolveIterationDataForCurrentThread() {
        Integer virtualUserIndex = virtualUsers.currentVirtualUserIndex();
        return iterationDataProvider.dataForVirtualUser(virtualUserIndex == null ? 0 : virtualUserIndex);
    }
}
