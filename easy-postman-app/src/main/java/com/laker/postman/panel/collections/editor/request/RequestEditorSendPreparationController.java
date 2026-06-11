package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.execution.RequestPreparationResult;
import com.laker.postman.http.execution.RequestPreparationService;
import com.laker.postman.panel.sidebar.ConsoleScriptOutputAdapter;
import com.laker.postman.request.model.HttpRequestItem;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

/**
 * 请求发送前准备控制器。
 * <p>
 * 发送前只做“收集表单 + 设置校验 + 构建请求 + 执行前置脚本”，真正协议执行由 runtime controller 分发。
 */
@RequiredArgsConstructor
final class RequestEditorSendPreparationController {
    private final RequestPreparationService requestPreparationService;
    private final Runnable editorInitializer;
    private final Runnable previewTabPromoter;
    private final Supplier<String> settingsValidator;
    private final Supplier<HttpRequestItem> currentRequestSupplier;

    static RequestEditorSendPreparationController createDefault(Runnable editorInitializer,
                                                                Runnable previewTabPromoter,
                                                                Supplier<String> settingsValidator,
                                                                Supplier<HttpRequestItem> currentRequestSupplier) {
        return new RequestEditorSendPreparationController(
                new RequestPreparationService(ConsoleScriptOutputAdapter.outputCallback()),
                editorInitializer,
                previewTabPromoter,
                settingsValidator,
                currentRequestSupplier
        );
    }

    RequestPreparationResult prepareForSending() {
        editorInitializer.run();
        previewTabPromoter.run();
        String settingsValidationError = validateCurrentSettings();
        if (settingsValidationError != null) {
            return RequestPreparationResult.error(settingsValidationError);
        }
        HttpRequestItem item = currentRequestSupplier.get();
        return requestPreparationService.prepare(item);
    }

    String validateRequestSettings() {
        editorInitializer.run();
        return validateCurrentSettings();
    }

    private String validateCurrentSettings() {
        return settingsValidator.get();
    }
}
