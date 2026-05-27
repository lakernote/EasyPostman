package com.laker.postman.panel.performance.runtime;

import com.laker.postman.panel.performance.config.CsvDataSetPerformanceIterationDataProvider;
import com.laker.postman.panel.performance.config.PerformanceIterationDataProvider;
import com.laker.postman.panel.performance.plan.PerformanceThreadGroupPlan;
import com.laker.postman.service.variable.ExecutionVariableContext;
import com.laker.postman.service.variable.IterationDataRuntimeSupport;

import java.util.Map;

public final class PerformanceIterationContextFactory {

    private final PerformanceIterationDataProvider iterationDataProvider;
    private final PerformanceVirtualUserCoordinator virtualUsers;

    public PerformanceIterationContextFactory(PerformanceVirtualUserCoordinator virtualUsers) {
        this(new CsvDataSetPerformanceIterationDataProvider(), virtualUsers);
    }

    public PerformanceIterationContextFactory(PerformanceIterationDataProvider iterationDataProvider,
                                              PerformanceVirtualUserCoordinator virtualUsers) {
        this.iterationDataProvider = iterationDataProvider == null
                ? PerformanceIterationDataProvider.empty()
                : iterationDataProvider;
        this.virtualUsers = virtualUsers;
    }

    public ExecutionVariableContext create(int iterationCount) {
        return create(null, iterationCount);
    }

    public ExecutionVariableContext create(PerformanceThreadGroupPlan groupPlan, int iterationCount) {
        int iterationIndex = virtualUsers.nextIterationIndex();
        ExecutionVariableContext iterationContext = new ExecutionVariableContext();
        iterationContext.setIterationInfo(iterationIndex, iterationCount);
        iterationContext.replaceIterationData(
                IterationDataRuntimeSupport.prepare(resolveIterationDataForCurrentThread(groupPlan))
        );
        return iterationContext;
    }

    private Map<String, String> resolveIterationDataForCurrentThread(PerformanceThreadGroupPlan groupPlan) {
        Integer virtualUserIndex = virtualUsers.currentVirtualUserIndex();
        return iterationDataProvider.dataForVirtualUser(groupPlan, virtualUserIndex == null ? 0 : virtualUserIndex);
    }
}
