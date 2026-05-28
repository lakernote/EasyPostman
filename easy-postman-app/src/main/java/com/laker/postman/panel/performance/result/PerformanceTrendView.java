package com.laker.postman.panel.performance.result;

import com.laker.postman.performance.core.model.PerformanceTrendSnapshot;
import org.jfree.data.time.RegularTimePeriod;

public interface PerformanceTrendView {
    void clearTrendDataset();

    void addOrUpdate(RegularTimePeriod period, PerformanceTrendSnapshot snapshot);
}
