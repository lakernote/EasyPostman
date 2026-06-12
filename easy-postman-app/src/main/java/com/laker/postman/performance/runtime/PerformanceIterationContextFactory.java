package com.laker.postman.performance.runtime;

import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.runtime.PerformanceVirtualUserCoordinator;


import com.laker.postman.service.variable.ExecutionVariableContext;
import com.laker.postman.service.variable.IterationDataRuntimeSupport;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class PerformanceIterationContextFactory {

    private final PerformanceIterationDataProvider iterationDataProvider;
    private final PerformanceVirtualUserCoordinator virtualUsers;
    private final AtomicLong controlStateGeneration = new AtomicLong();
    private final ThreadLocal<ScopedOnceOnlyState> onceOnlyState = new ThreadLocal<>();

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
        iterationContext.attachOnceOnlyState(resolveOnceOnlyState());
        return iterationContext;
    }

    public void resetControlState() {
        controlStateGeneration.incrementAndGet();
        onceOnlyState.remove();
    }

    private Map<String, String> resolveIterationDataForCurrentThread(PerformanceThreadGroupPlan groupPlan) {
        Integer virtualUserIndex = virtualUsers.currentVirtualUserIndex();
        return iterationDataProvider.dataForVirtualUser(groupPlan, virtualUserIndex == null ? 0 : virtualUserIndex);
    }

    private Set<String> resolveOnceOnlyState() {
        String scope = virtualUsers.currentVirtualUserScope();
        long generation = controlStateGeneration.get();
        ScopedOnceOnlyState current = onceOnlyState.get();
        if (current == null || current.generation != generation || !Objects.equals(current.scope, scope)) {
            current = new ScopedOnceOnlyState(scope, generation, ConcurrentHashMap.newKeySet());
            onceOnlyState.set(current);
        }
        return current.keys;
    }

    private record ScopedOnceOnlyState(String scope, long generation, Set<String> keys) {
    }
}
