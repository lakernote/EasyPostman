package com.laker.postman.functional.execution;

import com.laker.postman.functional.model.AssertionResult;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.script.model.TestResult;
import lombok.Value;

import java.util.List;

@Value
public class FunctionalRequestExecutionResult {
    PreparedRequest request;
    HttpResponse response;
    long cost;
    String status;
    String errorMessage;
    AssertionResult assertion;
    List<TestResult> testResults;

    public static FunctionalRequestExecutionResult skipped() {
        return new FunctionalRequestExecutionResult(
                null,
                null,
                0L,
                "",
                null,
                AssertionResult.NO_TESTS,
                null
        );
    }
}
