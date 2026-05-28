package com.laker.postman.performance.runtime.okhttp;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceSampleRecord;
import com.laker.postman.performance.core.request.PerformanceAuthConfig;
import com.laker.postman.performance.core.request.PerformanceOutboundRequest;
import com.laker.postman.performance.core.request.PerformanceRequestFormDataPart;
import com.laker.postman.performance.core.request.PerformanceRequestKeyValue;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class OkHttpPerformanceTransportRuntimeTest {

    @Test
    public void shouldExecuteHttpGetRequestFromCoreOutboundRequest() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
            server.start();
            OkHttpPerformanceTransportRuntime runtime = new OkHttpPerformanceTransportRuntime();
            PerformanceOutboundRequest request = PerformanceOutboundRequest.builder()
                    .id("api")
                    .name("API")
                    .protocol(PerformanceProtocol.HTTP)
                    .method("GET")
                    .url(server.url("/hello").toString())
                    .headers(List.of(new PerformanceRequestKeyValue(true, "X-Test", "1")))
                    .build();

            PerformanceSampleRecord record = runtime.execute(request);

            assertEquals(server.takeRequest().getHeader("X-Test"), "1");
            assertEquals(record.getApiId(), "api");
            assertEquals(record.getApiName(), "API");
            assertEquals(record.getProtocol(), PerformanceProtocol.HTTP);
            assertEquals(record.getResponseCode(), 200);
            assertEquals(record.getBodySize(), 2L);
            assertFalse(record.isExecutionFailed());
            assertTrue(record.isSuccessful());
            assertTrue(record.getElapsedTimeMs() >= 0);
        }
    }

    @Test
    public void shouldExecuteUrlencodedPostRequest() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(201).setBody("created"));
            server.start();
            OkHttpPerformanceTransportRuntime runtime = new OkHttpPerformanceTransportRuntime();
            PerformanceOutboundRequest request = PerformanceOutboundRequest.builder()
                    .id("create-api")
                    .name("Create API")
                    .protocol(PerformanceProtocol.HTTP)
                    .method("POST")
                    .url(server.url("/submit").toString())
                    .urlencoded(List.of(new PerformanceRequestKeyValue(true, "name", "alice")))
                    .build();

            PerformanceSampleRecord record = runtime.execute(request);

            okhttp3.mockwebserver.RecordedRequest recordedRequest = server.takeRequest();
            assertEquals(recordedRequest.getMethod(), "POST");
            assertEquals(recordedRequest.getBody().readUtf8(), "name=alice");
            assertEquals(record.getResponseCode(), 201);
            assertEquals(record.getBodySize(), 7L);
            assertTrue(record.isSuccessful());
        }
    }

    @Test
    public void shouldExecuteMultipartFormDataRequest() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
            server.start();
            OkHttpPerformanceTransportRuntime runtime = new OkHttpPerformanceTransportRuntime();
            PerformanceOutboundRequest request = PerformanceOutboundRequest.builder()
                    .id("multipart-api")
                    .name("Multipart API")
                    .protocol(PerformanceProtocol.HTTP)
                    .method("POST")
                    .url(server.url("/upload").toString())
                    .formData(List.of(new PerformanceRequestFormDataPart(true, "note", "Text", "hello")))
                    .build();

            PerformanceSampleRecord record = runtime.execute(request);

            okhttp3.mockwebserver.RecordedRequest recordedRequest = server.takeRequest();
            assertEquals(recordedRequest.getMethod(), "POST");
            assertTrue(recordedRequest.getHeader("Content-Type").startsWith("multipart/form-data; boundary="));
            String multipartBody = recordedRequest.getBody().readUtf8();
            assertTrue(multipartBody.contains("name=\"note\""));
            assertTrue(multipartBody.contains("hello"));
            assertEquals(record.getResponseCode(), 200);
            assertTrue(record.isSuccessful());
        }
    }

    @Test
    public void shouldApplyBasicAuthFromCoreOutboundRequest() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
            server.start();
            OkHttpPerformanceTransportRuntime runtime = new OkHttpPerformanceTransportRuntime();
            PerformanceOutboundRequest request = PerformanceOutboundRequest.builder()
                    .id("auth-api")
                    .name("Auth API")
                    .protocol(PerformanceProtocol.HTTP)
                    .method("GET")
                    .url(server.url("/secure").toString())
                    .authConfig(PerformanceAuthConfig.basic("alice", "secret"))
                    .build();

            runtime.execute(request);

            String expected = "Basic " + Base64.getEncoder()
                    .encodeToString("alice:secret".getBytes(StandardCharsets.UTF_8));
            assertEquals(server.takeRequest().getHeader("Authorization"), expected);
        }
    }

    @Test
    public void shouldDisableInheritedCookieJarWhenRequested() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
            server.start();
            OkHttpClient clientWithCookies = new OkHttpClient.Builder()
                    .cookieJar(new CookieJar() {
                        @Override
                        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        }

                        @Override
                        public List<Cookie> loadForRequest(HttpUrl url) {
                            return List.of(new Cookie.Builder()
                                    .hostOnlyDomain(url.host())
                                    .path("/")
                                    .name("session")
                                    .value("abc")
                                    .build());
                        }
                    })
                    .build();
            OkHttpPerformanceTransportRuntime runtime = new OkHttpPerformanceTransportRuntime(clientWithCookies);
            PerformanceOutboundRequest request = PerformanceOutboundRequest.builder()
                    .id("cookie-api")
                    .name("Cookie API")
                    .protocol(PerformanceProtocol.HTTP)
                    .method("GET")
                    .url(server.url("/cookie").toString())
                    .cookieJarEnabled(false)
                    .build();

            runtime.execute(request);

            assertNull(server.takeRequest().getHeader("Cookie"));
        }
    }

    @Test
    public void shouldExecuteSseRequestAndCountEvents() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("data: one\n\nid: 2\nevent: message\ndata: two\n\n"));
            server.start();
            OkHttpPerformanceTransportRuntime runtime = new OkHttpPerformanceTransportRuntime();
            PerformanceOutboundRequest request = PerformanceOutboundRequest.builder()
                    .id("sse-api")
                    .name("SSE API")
                    .protocol(PerformanceProtocol.SSE)
                    .method("GET")
                    .url(server.url("/events").toString())
                    .requestTimeoutMs(1000)
                    .build();

            PerformanceSampleRecord record = runtime.execute(request);

            assertEquals(server.getRequestCount(), 1);
            assertEquals(record.getApiId(), "sse-api");
            assertEquals(record.getProtocol(), PerformanceProtocol.SSE);
            assertEquals(record.getResponseCode(), 200);
            assertEquals(record.getReceivedMessages(), 2);
            assertTrue(record.getFirstMessageLatencyMs() >= 0);
            assertFalse(record.isExecutionFailed());
            assertTrue(record.isSuccessful());
        }
    }

    @Test
    public void shouldExecuteWebSocketRequestAndCountReceivedMessages() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                    webSocket.send("hello");
                    webSocket.close(1000, "done");
                }
            }));
            server.start();
            OkHttpPerformanceTransportRuntime runtime = new OkHttpPerformanceTransportRuntime();
            PerformanceOutboundRequest request = PerformanceOutboundRequest.builder()
                    .id("ws-api")
                    .name("WS API")
                    .protocol(PerformanceProtocol.WEBSOCKET)
                    .method("GET")
                    .url(server.url("/socket").toString().replaceFirst("^http", "ws"))
                    .requestTimeoutMs(1000)
                    .build();

            PerformanceSampleRecord record = runtime.execute(request);

            assertEquals(server.getRequestCount(), 1);
            assertEquals(record.getApiId(), "ws-api");
            assertEquals(record.getProtocol(), PerformanceProtocol.WEBSOCKET);
            assertEquals(record.getResponseCode(), 101);
            assertEquals(record.getReceivedMessages(), 1);
            assertTrue(record.getFirstMessageLatencyMs() >= 0);
            assertFalse(record.isExecutionFailed());
            assertTrue(record.isSuccessful());
        }
    }

    @Test(timeOut = 3000)
    public void shouldCancelActiveSseRequest() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setSocketPolicy(SocketPolicy.NO_RESPONSE));
            server.start();
            OkHttpPerformanceTransportRuntime runtime = new OkHttpPerformanceTransportRuntime();
            PerformanceOutboundRequest request = PerformanceOutboundRequest.builder()
                    .id("sse-api")
                    .name("SSE API")
                    .protocol(PerformanceProtocol.SSE)
                    .method("GET")
                    .url(server.url("/events").toString())
                    .requestTimeoutMs(5000)
                    .build();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<PerformanceSampleRecord> future = executor.submit(() -> runtime.execute(request));
                waitUntil(() -> runtime.activeSseCount() == 1);

                runtime.cancelAll();
                PerformanceSampleRecord record = future.get(2, TimeUnit.SECONDS);

                assertEquals(runtime.activeSseCount(), 0);
                assertTrue(record.isExecutionFailed());
                assertFalse(record.isSuccessful());
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test(timeOut = 3000)
    public void shouldCancelActiveHttpRequest() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setSocketPolicy(SocketPolicy.NO_RESPONSE));
            server.start();
            OkHttpPerformanceTransportRuntime runtime = new OkHttpPerformanceTransportRuntime();
            PerformanceOutboundRequest request = PerformanceOutboundRequest.builder()
                    .id("http-api")
                    .name("HTTP API")
                    .protocol(PerformanceProtocol.HTTP)
                    .method("GET")
                    .url(server.url("/slow").toString())
                    .requestTimeoutMs(5000)
                    .build();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<PerformanceSampleRecord> future = executor.submit(() -> runtime.execute(request));
                waitUntil(() -> runtime.activeHttpCallCount() == 1);

                runtime.cancelAll();
                PerformanceSampleRecord record = future.get(2, TimeUnit.SECONDS);

                assertEquals(runtime.activeHttpCallCount(), 0);
                assertTrue(record.isExecutionFailed());
                assertFalse(record.isSuccessful());
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    public void shouldReturnFailedRecordForInvalidUrl() {
        OkHttpPerformanceTransportRuntime runtime = new OkHttpPerformanceTransportRuntime();
        PerformanceOutboundRequest request = PerformanceOutboundRequest.builder()
                .id("bad-api")
                .name("Bad API")
                .method("GET")
                .url("not a url")
                .build();

        PerformanceSampleRecord record = runtime.execute(request);

        assertEquals(record.getApiId(), "bad-api");
        assertTrue(record.isExecutionFailed());
        assertFalse(record.isSuccessful());
        assertTrue(record.getErrorMsg().contains("Invalid request URL"));
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 1000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean());
    }
}
