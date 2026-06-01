package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.execution.RequestPreparationResult;
import com.laker.postman.panel.collections.editor.request.sub.RequestLinePanel;
import com.laker.postman.panel.collections.editor.request.sub.ResponsePanel;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
final class RequestSendCoordinator {
    private final RequestExecutionState executionState;
    private final Runnable cancelCurrentRequestAction;
    private final JTextField urlField;
    private final RequestPreparationFeedbackHelper requestPreparationFeedbackHelper;
    private final RequestLinePanel requestLinePanel;
    private final ActionListener sendAction;
    private final ResponsePanel responsePanel;
    private final Supplier<RequestPreparationResult> preparationSupplier;
    private final Runnable updateUiForRequestingAction;
    private final Consumer<RequestPreparationResult> dispatchAction;

    void sendRequest() {
        if (executionState.currentWorker() != null) {
            cancelCurrentRequestAction.run();
            return;
        }

        requestPreparationFeedbackHelper.clearUrlValidationFeedback(urlField);
        // 点击发送后立即切到“请求中”态，保持和实际网络请求一致的即时反馈。
        updateUiForRequestingAction.run();

        SwingWorker<Void, Void> preparationWorker = new SwingWorker<>() {
            RequestPreparationResult preparationResult;

            @Override
            protected Void doInBackground() {
                // 预处理阶段只负责准备请求数据，不直接触碰协议执行，便于后续独立演进校验逻辑。
                preparationResult = preparationSupplier.get();
                return null;
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    executionState.clearCurrentWorkerIf(this);
                    return;
                }

                // 预处理 worker 只负责准备数据；进入结果分发前，先把当前 worker 归还给主状态机。
                executionState.clearCurrentWorkerIf(this);

                if (requestPreparationFeedbackHelper.handlePreparationFailure(
                        preparationResult, urlField, requestLinePanel, sendAction, responsePanel)) {
                    return;
                }

                requestPreparationFeedbackHelper.showPreparationWarningIfNeeded(preparationResult);
                if (!requestPreparationFeedbackHelper.confirmContinuationIfNeeded(
                        preparationResult, requestLinePanel, sendAction, responsePanel)) {
                    return;
                }
                dispatchAction.accept(preparationResult);
            }
        };

        executionState.startWorker(preparationWorker);
        preparationWorker.execute();
    }
}
