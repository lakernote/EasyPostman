package com.laker.postman.panel.collections.right.request;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.collections.right.request.sub.ResponsePanel;
import com.laker.postman.panel.history.HistoryPanel;
import com.laker.postman.panel.sidebar.ConsolePanel;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.service.js.ScriptExecutionResult;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
final class RequestResponseHelper {
    private final Component owner;
    private final ResponsePanel responsePanel;
    private final Consumer<List<TestResult>> testResultsConsumer;
    private final BiConsumer<PreparedRequest, HttpResponse> exchangeRecorder;

    RequestResponseHelper(Component owner,
                          ResponsePanel responsePanel,
                          Consumer<List<TestResult>> testResultsConsumer,
                          BiConsumer<PreparedRequest, HttpResponse> exchangeRecorder) {
        this.owner = owner;
        this.responsePanel = responsePanel;
        this.testResultsConsumer = testResultsConsumer;
        this.exchangeRecorder = exchangeRecorder;
    }

    void handleResponse(ScriptExecutionPipeline pipeline, PreparedRequest request, HttpResponse response) {
        if (response == null) {
            log.error("Response is null, cannot handle response.");
            return;
        }

        recordExchange(request, response);
        responsePanel.setRequestDetails(request);
        responsePanel.setResponseDetails(response);

        try {
            ScriptExecutionResult postResult = pipeline.executePostScript(response);
            if (!postResult.isSuccess()) {
                String errorMessage = I18nUtil.getMessage(MessageKeys.SCRIPT_POSTSCRIPT_EXECUTION_FAILED,
                        postResult.getErrorMessage());
                String errorTitle = I18nUtil.getMessage(MessageKeys.SCRIPT_POSTSCRIPT_ERROR_TITLE);
                JOptionPane.showMessageDialog(owner, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
            }
            testResultsConsumer.accept(postResult.getTestResults());
        } catch (Exception ex) {
            log.error("Error executing post-script: {}", ex.getMessage(), ex);
            ConsolePanel.appendLog("[Error] Post-script execution failed: " + ex.getMessage(), ConsolePanel.LogType.ERROR);
        }

        saveHistory(request, response, "request");
    }

    List<TestResult> handleStreamMessage(ScriptExecutionPipeline pipeline, String message) {
        HttpResponse response = new HttpResponse();
        response.body = message;
        response.bodySize = message != null ? message.length() : 0;

        ScriptExecutionResult postResult = pipeline.executePostScript(response);
        if (!postResult.isSuccess()) {
            log.warn("Post-script execution failed for stream message: {}", postResult.getErrorMessage());
        }
        return postResult.getTestResults();
    }

    void recordExchange(PreparedRequest request, HttpResponse response) {
        exchangeRecorder.accept(request, response);
    }

    void saveHistory(PreparedRequest request, HttpResponse response, String label) {
        try {
            SingletonFactory.getInstance(HistoryPanel.class).addRequestHistory(request, response);
        } catch (Exception ex) {
            log.error("Error saving {} to history: {}", label, ex.getMessage(), ex);
            ConsolePanel.appendLog("[Warning] Failed to save " + label + " to history: " + ex.getMessage(), ConsolePanel.LogType.WARN);
        }
    }
}
