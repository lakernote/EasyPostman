package com.laker.postman.functional.execution;

import com.laker.postman.functional.model.AssertionResult;
import com.laker.postman.functional.model.RunnerRowData;
import com.laker.postman.http.request.PreparedRequestFactory;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.transport.DefaultHttpTransport;
import com.laker.postman.http.runtime.transport.HttpExchangeOptions;
import com.laker.postman.http.runtime.transport.HttpTransport;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.service.js.ScriptExecutionResult;
import com.laker.postman.service.variable.ExecutionVariableContext;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public final class FunctionalRequestExecutor {
    public static final String ERROR = "Error";

    private final Consumer<String> requestErrorConsumer;
    private final HttpTransport httpTransport;

    public FunctionalRequestExecutor(Consumer<String> requestErrorConsumer) {
        this(requestErrorConsumer, new DefaultHttpTransport());
    }

    FunctionalRequestExecutor(Consumer<String> requestErrorConsumer, HttpTransport httpTransport) {
        this.requestErrorConsumer = requestErrorConsumer;
        this.httpTransport = httpTransport == null ? new DefaultHttpTransport() : httpTransport;
    }

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
                response = httpTransport.execute(request, HttpExchangeOptions.defaults());
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
