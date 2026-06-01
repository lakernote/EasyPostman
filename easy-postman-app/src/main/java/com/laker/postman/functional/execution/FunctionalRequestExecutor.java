package com.laker.postman.functional.execution;

import com.laker.postman.functional.model.AssertionResult;
import com.laker.postman.functional.model.RunnerRowData;
import com.laker.postman.http.request.PreparedRequestFactory;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.transport.HttpTransportRuntime;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.service.js.ScriptExecutionResult;
import com.laker.postman.service.variable.ExecutionVariableContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public final class FunctionalRequestExecutor {
    public static final String ERROR = "Error";

    private final Consumer<String> requestErrorConsumer;

    public FunctionalRequestExecutionResult execute(RunnerRowData row,
                                                    ExecutionVariableContext iterationContext,
                                                    BooleanSupplier executionActiveSupplier) {
        if (executionActiveSupplier != null && !executionActiveSupplier.getAsBoolean()) {
            return FunctionalRequestExecutionResult.skipped();
        }

        long start = System.currentTimeMillis();
        HttpRequestItem item = row.requestItem;
        PreparedRequest request = PreparedRequestFactory.build(item);
        request.collectBasicInfo = true;
        request.collectEventInfo = true;
        request.enableNetworkLog = false;

        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.forRequestExecution(
                item,
                request,
                iterationContext,
                true
        );

        ScriptExecutionResult preResult = pipeline.executePreScript();
        if (preResult.isSuccess()) {
            pipeline.finalizeRequest();
        }

        HttpResponse response = null;
        String status;
        AssertionResult assertion = AssertionResult.NO_TESTS;
        String errorMessage = null;
        ScriptExecutionResult postResult = null;

        if (!preResult.isSuccess()) {
            errorMessage = preResult.getErrorMessage();
            status = ERROR;
        } else {
            try {
                response = HttpTransportRuntime.executeHttp(request, null);
                status = String.valueOf(response.code);
                postResult = pipeline.executePostScript(response);
                if (postResult.hasTestResults()) {
                    assertion = postResult.allTestsPassed() ? AssertionResult.PASS : AssertionResult.FAIL;
                }
            } catch (Exception ex) {
                log.error("请求执行失败", ex);
                if (requestErrorConsumer != null) {
                    requestErrorConsumer.accept(ex.getMessage());
                }
                assertion = AssertionResult.FAIL;
                errorMessage = ex.getMessage();
                status = ERROR;
            }
        }

        long elapsedMs = System.currentTimeMillis() - start;
        return new FunctionalRequestExecutionResult(
                request,
                response,
                response == null ? elapsedMs : response.costMs,
                status,
                errorMessage,
                assertion,
                postResult == null ? null : postResult.getTestResults()
        );
    }
}
