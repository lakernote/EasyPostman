package com.laker.postman.panel.collections.right.request;

import com.laker.postman.panel.collections.right.request.sub.RequestLinePanel;
import com.laker.postman.panel.collections.right.request.sub.ResponsePanel;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
final class RequestSendCoordinator {
    private final Supplier<SwingWorker<Void, Void>> currentWorkerSupplier;
    private final Consumer<SwingWorker<Void, Void>> currentWorkerSetter;
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
        if (currentWorkerSupplier.get() != null) {
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
                    if (currentWorkerSupplier.get() == this) {
                        currentWorkerSetter.accept(null);
                    }
                    return;
                }

                // 预处理 worker 只负责准备数据；进入结果分发前，先把当前 worker 归还给主状态机。
                if (currentWorkerSupplier.get() == this) {
                    currentWorkerSetter.accept(null);
                }

                if (requestPreparationFeedbackHelper.handlePreparationFailure(
                        preparationResult, urlField, requestLinePanel, sendAction, responsePanel)) {
                    return;
                }

                requestPreparationFeedbackHelper.showPreparationWarningIfNeeded(preparationResult);
                dispatchAction.accept(preparationResult);
            }
        };

        currentWorkerSetter.accept(preparationWorker);
        preparationWorker.execute();
    }
}
