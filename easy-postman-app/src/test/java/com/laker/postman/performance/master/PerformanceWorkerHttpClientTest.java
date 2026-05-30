package com.laker.postman.performance.master;

import com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint;
import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class PerformanceWorkerHttpClientTest {

    @Test
    public void shouldPostStopToWorkerRunEndpoint() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        try (TestServer server = TestServer.start(200, "", method, path)) {
            PerformanceWorkerHttpClient client = new PerformanceWorkerHttpClient();

            client.stop(server.endpoint(), "run with space");

            assertEquals(method.get(), "POST");
            assertEquals(path.get(), "/api/performance/v1/runs/run%20with%20space/stop");
        }
    }

    @Test
    public void shouldThrowIOExceptionWhenStopFails() throws Exception {
        try (TestServer server = TestServer.start(409, "already finished", new AtomicReference<>(), new AtomicReference<>())) {
            PerformanceWorkerHttpClient client = new PerformanceWorkerHttpClient();

            try {
                client.stop(server.endpoint(), "run-1");
                fail("Expected IOException");
            } catch (IOException ex) {
                assertTrue(ex.getMessage().contains("409"), ex.getMessage());
                assertTrue(ex.getMessage().contains("already finished"), ex.getMessage());
            }
        }
    }

    @Test
    public void shouldApplyRequestTimeoutToStatusCalls() throws Exception {
        try (TestServer server = TestServer.start(
                200,
                "{\"status\":\"SUCCESS\"}",
                new AtomicReference<>(),
                new AtomicReference<>(),
                500L
        )) {
            PerformanceWorkerHttpClient client = new PerformanceWorkerHttpClient(
                    HttpClient.newHttpClient(),
                    new com.laker.postman.performance.core.worker.PerformanceWorkerProtocolJsonStorage(),
                    Duration.ofMillis(100)
            );

            try {
                client.status(server.endpoint(), "run-timeout");
                fail("Expected request timeout");
            } catch (HttpTimeoutException expected) {
                assertTrue(expected.getMessage() != null && !expected.getMessage().isBlank());
            }
        }
    }

    private record TestServer(HttpServer server,
                              AtomicReference<String> method,
                              AtomicReference<String> path) implements AutoCloseable {

        static TestServer start(int status,
                                String body,
                                AtomicReference<String> method,
                                AtomicReference<String> path) throws IOException {
            return start(status, body, method, path, 0L);
        }

        static TestServer start(int status,
                                String body,
                                AtomicReference<String> method,
                                AtomicReference<String> path,
                                long delayMs) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                method.set(exchange.getRequestMethod());
                path.set(exchange.getRequestURI().getRawPath());
                if (delayMs > 0) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
                byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, responseBody.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(responseBody);
                }
            });
            server.start();
            return new TestServer(server, method, path);
        }

        PerformanceWorkerEndpoint endpoint() {
            return new PerformanceWorkerEndpoint("127.0.0.1", server.getAddress().getPort());
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
