package com.laker.postman.functional.execution;

import com.laker.postman.functional.model.AssertionResult;
import com.laker.postman.functional.model.RunnerRowData;
import com.laker.postman.http.runtime.model.HttpCaptureProfile;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.transport.HttpExchangeOptions;
import com.laker.postman.http.runtime.transport.HttpTransport;
import com.laker.postman.http.runtime.transport.RealtimeConnectionHandle;
import com.laker.postman.http.runtime.transport.RealtimeConnectionOptions;
import com.laker.postman.http.runtime.transport.RealtimeWebSocketConnection;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.variable.ExecutionVariableContext;
import okhttp3.WebSocketListener;
import okhttp3.sse.EventSourceListener;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class FunctionalRequestExecutorTest {

    @Test
    public void shouldUseFunctionalDiagnosticCaptureProfileWithoutNetworkLog() {
        CapturingTransport transport = new CapturingTransport();
        HttpRequestItem item = new HttpRequestItem();
        item.setName("Functional Profile");
        item.setMethod("GET");
        item.setUrl("https://example.test/functional");

        FunctionalRequestExecutionResult result = new FunctionalRequestExecutor(null, transport).execute(
                new RunnerRowData(item),
                new ExecutionVariableContext(),
                () -> true
        );

        assertEquals(result.getStatus(), "204");
        assertSame(result.getRequest().captureProfile, HttpCaptureProfile.FUNCTIONAL_DIAGNOSTIC);
        assertSame(transport.request.captureProfile, HttpCaptureProfile.FUNCTIONAL_DIAGNOSTIC);
        assertTrue(result.getRequest().collectBasicInfo);
        assertTrue(result.getRequest().collectMetricsInfo);
        assertTrue(result.getRequest().collectEventInfo);
        assertFalse(result.getRequest().enableNetworkLog);
    }

    @Test
    public void shouldFailRequestWhenPostScriptExecutionFails() {
        HttpRequestItem item = new HttpRequestItem();
        item.setName("Broken post script");
        item.setMethod("GET");
        item.setUrl("https://example.test/failure");
        item.setPostscript("throw new Error('post script boom');");

        FunctionalRequestExecutionResult result = new FunctionalRequestExecutor(
                null,
                new CapturingTransport()
        ).execute(
                new RunnerRowData(item),
                new ExecutionVariableContext(),
                () -> true
        );

        assertSame(result.getAssertion(), AssertionResult.FAIL);
        assertTrue(result.getErrorMessage().contains("post script boom"), result.getErrorMessage());
    }

    private static final class CapturingTransport implements HttpTransport {
        private PreparedRequest request;

        @Override
        public HttpResponse execute(PreparedRequest request, HttpExchangeOptions options) {
            this.request = request;
            HttpResponse response = new HttpResponse();
            response.code = 204;
            response.costMs = 12L;
            response.headers = new LinkedHashMap<>();
            return response;
        }

        @Override
        public RealtimeConnectionHandle openSse(PreparedRequest request,
                                                EventSourceListener listener,
                                                RealtimeConnectionOptions options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RealtimeWebSocketConnection openWebSocket(PreparedRequest request,
                                                        WebSocketListener listener,
                                                        RealtimeConnectionOptions options) {
            throw new UnsupportedOperationException();
        }
    }
}
