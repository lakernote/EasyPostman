package com.laker.postman.performance.core.runtime;

import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceSampler;
import com.laker.postman.performance.core.plan.PerformanceTimerElement;

import java.util.ArrayList;
import java.util.List;

final class PerformanceCoreTimerScope {
    private final List<PerformanceTimerElement> timers;

    private PerformanceCoreTimerScope(List<PerformanceTimerElement> timers) {
        this.timers = timers == null || timers.isEmpty() ? List.of() : List.copyOf(timers);
    }

    static PerformanceCoreTimerScope empty() {
        return new PerformanceCoreTimerScope(List.of());
    }

    PerformanceCoreTimerScope enter(List<PerformancePlanElement> elements) {
        return new PerformanceCoreTimerScope(mergeTimers(timers, collectDirectTimers(elements)));
    }

    List<PerformanceTimerElement> timersForSampler(PerformanceSampler sampler) {
        if (sampler == null || sampler.executesChildrenInSamplerOrder()) {
            return timers;
        }
        return mergeTimers(timers, collectDirectTimers(sampler.getChildren()));
    }

    private static List<PerformanceTimerElement> collectDirectTimers(List<PerformancePlanElement> elements) {
        if (elements == null || elements.isEmpty()) {
            return List.of();
        }
        List<PerformanceTimerElement> scopedTimers = new ArrayList<>();
        for (PerformancePlanElement element : elements) {
            if (element instanceof PerformanceTimerElement timerElement) {
                scopedTimers.add(timerElement);
            }
        }
        return scopedTimers;
    }

    private static List<PerformanceTimerElement> mergeTimers(List<PerformanceTimerElement> first,
                                                             List<PerformanceTimerElement> second) {
        if ((first == null || first.isEmpty()) && (second == null || second.isEmpty())) {
            return List.of();
        }
        List<PerformanceTimerElement> merged = new ArrayList<>();
        if (first != null) {
            merged.addAll(first);
        }
        if (second != null) {
            merged.addAll(second);
        }
        return List.copyOf(merged);
    }
}
