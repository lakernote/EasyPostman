package com.laker.postman.performance.execution;


import com.laker.postman.model.Environment;
import com.laker.postman.service.js.JsScriptExecutor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class PerformanceExecutionConfig {
    public static final int DEFAULT_RESPONSE_BODY_PREVIEW_LIMIT_KB = 64;
    public static final int MIN_RESPONSE_BODY_PREVIEW_LIMIT_KB = 1;
    public static final int MAX_RESPONSE_BODY_PREVIEW_LIMIT_KB = 1024;
    public static final int DEFAULT_RESPONSE_BODY_PREVIEW_LIMIT_BYTES =
            DEFAULT_RESPONSE_BODY_PREVIEW_LIMIT_KB * 1024;
    private static final JsScriptExecutor.OutputCallback NOOP_SCRIPT_OUTPUT_CALLBACK = output -> {
    };
    private static final Supplier<Environment> NO_ENVIRONMENT_SUPPLIER = () -> null;
    public static final PerformanceExecutionConfig DEFAULT = fixed(
            true,
            DEFAULT_RESPONSE_BODY_PREVIEW_LIMIT_KB,
            false,
            NOOP_SCRIPT_OUTPUT_CALLBACK,
            NO_ENVIRONMENT_SUPPLIER
    );

    private final BooleanSupplier efficientModeSupplier;
    private final IntSupplier responseBodyPreviewLimitKbSupplier;
    private final BooleanSupplier eventLoggingEnabledSupplier;
    private final Supplier<JsScriptExecutor.OutputCallback> scriptOutputCallbackSupplier;
    private final Supplier<Environment> environmentSupplier;
    private final Supplier<JsScriptExecutor.ScriptExecutor> scriptExecutorSupplier;

    public static PerformanceExecutionConfig fixed(boolean efficientMode,
                                                   int responseBodyPreviewLimitKb,
                                                   boolean eventLoggingEnabled) {
        return fixed(efficientMode, responseBodyPreviewLimitKb, eventLoggingEnabled, null);
    }

    public static PerformanceExecutionConfig fixed(boolean efficientMode,
                                                   int responseBodyPreviewLimitKb,
                                                   boolean eventLoggingEnabled,
                                                   JsScriptExecutor.OutputCallback scriptOutputCallback) {
        return fixed(efficientMode, responseBodyPreviewLimitKb, eventLoggingEnabled, scriptOutputCallback, null);
    }

    public static PerformanceExecutionConfig fixed(boolean efficientMode,
                                                   int responseBodyPreviewLimitKb,
                                                   boolean eventLoggingEnabled,
                                                   JsScriptExecutor.OutputCallback scriptOutputCallback,
                                                   Supplier<Environment> environmentSupplier) {
        Supplier<JsScriptExecutor.OutputCallback> callbackSupplier =
                scriptOutputCallback == null ? null : () -> scriptOutputCallback;
        return supplying(
                () -> efficientMode,
                () -> responseBodyPreviewLimitKb,
                () -> eventLoggingEnabled,
                callbackSupplier,
                environmentSupplier
        );
    }

    public static PerformanceExecutionConfig supplying(BooleanSupplier efficientModeSupplier,
                                                       IntSupplier responseBodyPreviewLimitKbSupplier,
                                                       BooleanSupplier eventLoggingEnabledSupplier) {
        return supplying(efficientModeSupplier, responseBodyPreviewLimitKbSupplier, eventLoggingEnabledSupplier, null);
    }

    public static PerformanceExecutionConfig supplying(BooleanSupplier efficientModeSupplier,
                                                       IntSupplier responseBodyPreviewLimitKbSupplier,
                                                       BooleanSupplier eventLoggingEnabledSupplier,
                                                       Supplier<JsScriptExecutor.OutputCallback> scriptOutputCallbackSupplier) {
        return supplying(
                efficientModeSupplier,
                responseBodyPreviewLimitKbSupplier,
                eventLoggingEnabledSupplier,
                scriptOutputCallbackSupplier,
                null
        );
    }

    public static PerformanceExecutionConfig supplying(BooleanSupplier efficientModeSupplier,
                                                       IntSupplier responseBodyPreviewLimitKbSupplier,
                                                       BooleanSupplier eventLoggingEnabledSupplier,
                                                       Supplier<JsScriptExecutor.OutputCallback> scriptOutputCallbackSupplier,
                                                       Supplier<Environment> environmentSupplier) {
        return create(
                efficientModeSupplier,
                responseBodyPreviewLimitKbSupplier,
                eventLoggingEnabledSupplier,
                scriptOutputCallbackSupplier == null ? () -> NOOP_SCRIPT_OUTPUT_CALLBACK : scriptOutputCallbackSupplier,
                environmentSupplier == null ? NO_ENVIRONMENT_SUPPLIER : environmentSupplier,
                () -> null
        );
    }

    public static PerformanceExecutionConfig uiSupplying(BooleanSupplier efficientModeSupplier,
                                                         IntSupplier responseBodyPreviewLimitKbSupplier,
                                                         BooleanSupplier eventLoggingEnabledSupplier) {
        return create(
                efficientModeSupplier,
                responseBodyPreviewLimitKbSupplier,
                eventLoggingEnabledSupplier,
                () -> null,
                null,
                () -> null
        );
    }

    private static PerformanceExecutionConfig create(BooleanSupplier efficientModeSupplier,
                                                     IntSupplier responseBodyPreviewLimitKbSupplier,
                                                     BooleanSupplier eventLoggingEnabledSupplier,
                                                     Supplier<JsScriptExecutor.OutputCallback> scriptOutputCallbackSupplier,
                                                     Supplier<Environment> environmentSupplier) {
        return create(
                efficientModeSupplier,
                responseBodyPreviewLimitKbSupplier,
                eventLoggingEnabledSupplier,
                scriptOutputCallbackSupplier,
                environmentSupplier,
                () -> null
        );
    }

    private static PerformanceExecutionConfig create(BooleanSupplier efficientModeSupplier,
                                                     IntSupplier responseBodyPreviewLimitKbSupplier,
                                                     BooleanSupplier eventLoggingEnabledSupplier,
                                                     Supplier<JsScriptExecutor.OutputCallback> scriptOutputCallbackSupplier,
                                                     Supplier<Environment> environmentSupplier,
                                                     Supplier<JsScriptExecutor.ScriptExecutor> scriptExecutorSupplier) {
        return new PerformanceExecutionConfig(
                efficientModeSupplier == null ? () -> true : efficientModeSupplier,
                responseBodyPreviewLimitKbSupplier == null ? () -> DEFAULT_RESPONSE_BODY_PREVIEW_LIMIT_KB
                        : responseBodyPreviewLimitKbSupplier,
                eventLoggingEnabledSupplier == null ? () -> false : eventLoggingEnabledSupplier,
                scriptOutputCallbackSupplier,
                environmentSupplier,
                scriptExecutorSupplier == null ? () -> null : scriptExecutorSupplier
        );
    }

    public PerformanceExecutionConfig withScriptExecutorSupplier(
            Supplier<JsScriptExecutor.ScriptExecutor> scriptExecutorSupplier) {
        return create(
                efficientModeSupplier,
                responseBodyPreviewLimitKbSupplier,
                eventLoggingEnabledSupplier,
                scriptOutputCallbackSupplier,
                environmentSupplier,
                scriptExecutorSupplier
        );
    }

    public boolean isEfficientMode() {
        return efficientModeSupplier.getAsBoolean();
    }

    public int responseBodyPreviewLimitKb() {
        return sanitizeResponseBodyPreviewLimitKb(responseBodyPreviewLimitKbSupplier.getAsInt());
    }

    public int responseBodyPreviewLimitBytes() {
        return responseBodyPreviewLimitBytes(responseBodyPreviewLimitKb());
    }

    public boolean isEventLoggingEnabled() {
        return eventLoggingEnabledSupplier.getAsBoolean();
    }

    public JsScriptExecutor.OutputCallback scriptOutputCallback() {
        return scriptOutputCallbackSupplier.get();
    }

    public Supplier<Environment> environmentSupplier() {
        return environmentSupplier;
    }

    public JsScriptExecutor.ScriptExecutor scriptExecutor() {
        return scriptExecutorSupplier.get();
    }

    public static int sanitizeResponseBodyPreviewLimitKb(Integer limitKb) {
        if (limitKb == null
                || limitKb < MIN_RESPONSE_BODY_PREVIEW_LIMIT_KB
                || limitKb > MAX_RESPONSE_BODY_PREVIEW_LIMIT_KB) {
            return DEFAULT_RESPONSE_BODY_PREVIEW_LIMIT_KB;
        }
        return limitKb;
    }

    public static int responseBodyPreviewLimitBytes(int limitKb) {
        return sanitizeResponseBodyPreviewLimitKb(limitKb) * 1024;
    }
}
