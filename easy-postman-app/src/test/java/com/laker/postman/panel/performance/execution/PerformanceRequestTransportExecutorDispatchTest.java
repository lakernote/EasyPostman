package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;
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

        PerformanceRequestTransportExecutor transportExecutor = new PerformanceRequestTransportExecutor(
                () -> true,
                throwable -> false,
                ConcurrentHashMap.<EventSource>newKeySet(),
                ConcurrentHashMap.<WebSocket>newKeySet(),
                new PerformanceRealtimeMetrics(),
                () -> 64,
                context -> {
                    calls.add("http");
                    assertSame(context.getRequest(), request);
                    assertSame(context.getRequestSampler(), sampler);
                    assertSame(context.getRequestItem(), requestItem);
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
                transportExecutor.execute(request, sampler, requestItem, false, false, "", null).errorMsg(),
                "http"
        );
        assertEquals(
                transportExecutor.execute(request, sampler, requestItem, true, false, "", null).errorMsg(),
                "sse"
        );
        assertEquals(
                transportExecutor.execute(request, sampler, requestItem, false, true, "body", null).errorMsg(),
                "websocket"
        );
        assertEquals(calls, List.of("http", "sse", "websocket"));
    }

    private static ProtocolExecutionResult result(String marker) {
        return new ProtocolExecutionResult(new HttpResponse(), marker, false, false, List.of());
    }

    private static PerformanceRequestSampler sampler(HttpRequestItem requestItem) {
        return new PerformanceRequestSampler("request", requestItem, null, null, List.of());
    }

    private static HttpRequestItem requestItem(RequestItemProtocolEnum protocol) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("request-id");
        item.setName("request");
        item.setProtocol(protocol);
        return item;
    }
}
