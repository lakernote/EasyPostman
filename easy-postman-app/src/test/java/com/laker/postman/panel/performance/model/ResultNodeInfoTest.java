package com.laker.postman.panel.performance.model;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.script.TestResult;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ResultNodeInfoTest {

    @Test
    public void shouldApplyUnifiedSuccessRules() {
        HttpResponse switchingProtocols = new HttpResponse();
        switchingProtocols.code = 101;
        assertTrue(ResultNodeInfo.isActuallySuccessful(false, switchingProtocols, null));

        HttpResponse serverError = new HttpResponse();
        serverError.code = 500;
        assertFalse(ResultNodeInfo.isActuallySuccessful(false, serverError, null));

        TestResult failedAssertion = new TestResult("failed", false, "boom");
        assertFalse(ResultNodeInfo.isActuallySuccessful(false, switchingProtocols, List.of(failedAssertion)));

        TestResult passedAssertion = new TestResult("passed", true, "");
        assertTrue(ResultNodeInfo.isActuallySuccessful(false, serverError, List.of(passedAssertion)));

        assertFalse(ResultNodeInfo.isActuallySuccessful(true, switchingProtocols, null));
    }
}
