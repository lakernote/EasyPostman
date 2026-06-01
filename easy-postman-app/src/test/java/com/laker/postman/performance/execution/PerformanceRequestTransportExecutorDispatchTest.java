package com.laker.postman.performance.execution;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.plan.PerformanceRequestSampler;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import com.laker.postman.http.runtime.transport.RealtimeConnectionHandle;
import com.laker.postman.http.runtime.transport.RealtimeWebSocketConnection;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

public class PerformanceRequestTransportExecutorDispatchTest {

    @Test
    public void shouldDispatchToProtocolSpecificSamplerExecutor() throws Exception {
        List<String> calls = new ArrayList<>();
        PreparedRequest request = new PreparedRequest();
        HttpRequestItem requestItem = requestItem(RequestItemProtocolEnum.HTTP);
        PerformanceRequestSampler sampler = sampler(requestItem);
        PerformanceRequestSnapshot snapshot = sampler.getRequestSnapshot();

        PerformanceRequestTransportExecutor transportExecutor = new PerformanceRequestTransportExecutor(
                () -> true,
                throwable -> false,
                ConcurrentHashMap.<RealtimeConnectionHandle>newKeySet(),
                ConcurrentHashMap.<RealtimeWebSocketConnection>newKeySet(),
                new PerformanceRealtimeMetrics(),
                () -> 64,
                context -> {
                    calls.add("http");
                    assertSame(context.getRequest(), request);
                    assertSame(context.getRequestSampler(), sampler);
                    assertEquals(context.getRequestSnapshot().getId(), "request-id");
                    assertEquals(context.getRequestName(), "request");
                    return result("http");
                },
                context -> {
                    calls.add("sse");
                    return result("sse");
                },
                context -> {
                    calls.add("websocket");
                    return result("websocket");
                }
        );

        assertEquals(
                transportExecutor.execute(request, sampler, snapshot, false, false, "", null).errorMsg(),
                "http"
        );
        assertEquals(
                transportExecutor.execute(request, sampler, snapshot, true, false, "", null).errorMsg(),
                "sse"
        );
        assertEquals(
                transportExecutor.execute(request, sampler, snapshot, false, true, "body", null).errorMsg(),
                "websocket"
        );
        assertEquals(calls, List.of("http", "sse", "websocket"));
    }

    private static ProtocolExecutionResult result(String marker) {
        return new ProtocolExecutionResult(new HttpResponse(), marker, false, false, List.of());
    }

    private static PerformanceRequestSampler sampler(HttpRequestItem requestItem) {
        return new PerformanceRequestSampler("request", requestItem, null, List.of());
    }

    private static HttpRequestItem requestItem(RequestItemProtocolEnum protocol) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("request-id");
        item.setName("request");
        item.setProtocol(protocol);
        return item;
    }
}
