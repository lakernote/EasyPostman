package com.laker.postman.performance.core.runtime;

import com.laker.postman.performance.core.model.PerformanceSampleRecord;
import com.laker.postman.performance.core.request.PerformanceOutboundRequest;

public interface PerformanceTransportRuntime extends PerformanceNetworkControl {

    PerformanceSampleRecord execute(PerformanceOutboundRequest request);
}
