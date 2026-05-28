package com.laker.postman.performance.core.runtime;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerformanceRunError {
    String message;
    Throwable cause;
}
