package com.laker.postman.service.variable;

import lombok.experimental.UtilityClass;

/**
 * Per-thread request execution context used by variable resolution.
 */
@UtilityClass
public class RequestExecutionContext {

    private static final ThreadLocal<RequestExecutionScope> CURRENT_SCOPE = new ThreadLocal<>();

    public static RequestExecutionScope getCurrentScope() {
        return CURRENT_SCOPE.get();
    }

    public static RequestExecutionScope captureCurrentScope() {
        return CURRENT_SCOPE.get();
    }

    public static void setCurrentScope(RequestExecutionScope executionScope) {
        CURRENT_SCOPE.set(executionScope == null ? RequestExecutionScope.empty() : executionScope);
    }

    public static void clearCurrentScope() {
        CURRENT_SCOPE.remove();
    }
}
