package com.laker.postman.panel.performance.result;

import com.laker.postman.performance.core.model.PerformanceTrendSnapshot;
import com.laker.postman.performance.core.model.PerformanceProtocol;
import org.jfree.data.time.RegularTimePeriod;

import java.util.Set;

public interface PerformanceTrendView {
    default void setAvailableProtocols(Set<PerformanceProtocol> protocols) {
    }

    void clearTrendDataset();

    void addOrUpdate(RegularTimePeriod period, PerformanceTrendSnapshot snapshot);
}
