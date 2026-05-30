package com.laker.postman.performance.master;

import com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint;
import com.laker.postman.performance.core.worker.PerformanceWorkerApiPaths;
import com.laker.postman.performance.core.worker.PerformanceWorkerProtocolJsonStorage;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunDetailsResponse;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunRequest;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunResultResponse;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunStatusResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class PerformanceWorkerHttpClient {
    static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient client;
    private final PerformanceWorkerProtocolJsonStorage jsonStorage;
    private final Duration requestTimeout;

    public PerformanceWorkerHttpClient() {
        this(HttpClient.newBuilder()
                        .connectTimeout(DEFAULT_REQUEST_TIMEOUT)
                        .build(),
                new PerformanceWorkerProtocolJsonStorage(),
                DEFAULT_REQUEST_TIMEOUT);
    }

    PerformanceWorkerHttpClient(HttpClient client, PerformanceWorkerProtocolJsonStorage jsonStorage) {
        this(client, jsonStorage, DEFAULT_REQUEST_TIMEOUT);
    }

    PerformanceWorkerHttpClient(HttpClient client,
                                PerformanceWorkerProtocolJsonStorage jsonStorage,
                                Duration requestTimeout) {
        this.client = client == null ? HttpClient.newHttpClient() : client;
        this.jsonStorage = jsonStorage == null ? new PerformanceWorkerProtocolJsonStorage() : jsonStorage;
        this.requestTimeout = effectiveTimeout(requestTimeout);
    }

    public void submitRun(PerformanceWorkerEndpoint endpoint,
                          PerformanceWorkerRunRequest request) throws IOException, InterruptedException {
        submitRun(endpoint, request, requestTimeout);
    }

    public void submitRun(PerformanceWorkerEndpoint endpoint,
                          PerformanceWorkerRunRequest request,
                          Duration timeout) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .uri(uri(endpoint, PerformanceWorkerApiPaths.RUNS))
                        .timeout(effectiveTimeout(timeout))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonStorage.toJson(request)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Worker " + endpointLabel(endpoint) + " rejected run: " + response.body());
        }
    }

    public PerformanceWorkerRunStatusResponse status(PerformanceWorkerEndpoint endpoint,
                                                     String runId) throws IOException, InterruptedException {
        return status(endpoint, runId, requestTimeout);
    }

    public PerformanceWorkerRunStatusResponse status(PerformanceWorkerEndpoint endpoint,
                                                     String runId,
                                                     boolean includeReport) throws IOException, InterruptedException {
        return status(endpoint, runId, includeReport, requestTimeout);
    }

    public PerformanceWorkerRunStatusResponse status(PerformanceWorkerEndpoint endpoint,
                                                     String runId,
                                                     Duration timeout) throws IOException, InterruptedException {
        return status(endpoint, runId, true, timeout);
    }

    public PerformanceWorkerRunStatusResponse status(PerformanceWorkerEndpoint endpoint,
                                                     String runId,
                                                     boolean includeReport,
                                                     Duration timeout) throws IOException, InterruptedException {
        String path = PerformanceWorkerApiPaths.run(pathSegment(runId)) + (includeReport ? "" : "?report=false");
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .uri(uri(endpoint, path))
                        .timeout(effectiveTimeout(timeout))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Worker " + endpointLabel(endpoint) + " status failed: " + response.body());
        }
        return jsonStorage.statusResponseFromJson(response.body());
    }

    public PerformanceWorkerRunResultResponse result(PerformanceWorkerEndpoint endpoint,
                                                     String runId) throws IOException, InterruptedException {
        return result(endpoint, runId, requestTimeout);
    }

    public PerformanceWorkerRunResultResponse result(PerformanceWorkerEndpoint endpoint,
                                                     String runId,
                                                     Duration timeout) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .uri(uri(endpoint, PerformanceWorkerApiPaths.result(pathSegment(runId))))
                        .timeout(effectiveTimeout(timeout))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Worker " + endpointLabel(endpoint) + " result failed: " + response.body());
        }
        return jsonStorage.resultResponseFromJson(response.body());
    }

    public PerformanceWorkerRunDetailsResponse details(PerformanceWorkerEndpoint endpoint,
                                                       String runId) throws IOException, InterruptedException {
        return details(endpoint, runId, requestTimeout);
    }

    public PerformanceWorkerRunDetailsResponse details(PerformanceWorkerEndpoint endpoint,
                                                       String runId,
                                                       Duration timeout) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .uri(uri(endpoint, PerformanceWorkerApiPaths.details(pathSegment(runId))))
                        .timeout(effectiveTimeout(timeout))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Worker " + endpointLabel(endpoint) + " details failed: " + response.body());
        }
        return jsonStorage.detailsResponseFromJson(response.body());
    }

    public void stop(PerformanceWorkerEndpoint endpoint,
                     String runId) throws IOException, InterruptedException {
        stop(endpoint, runId, requestTimeout);
    }

    public void stop(PerformanceWorkerEndpoint endpoint,
                     String runId,
                     Duration timeout) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .uri(uri(endpoint, PerformanceWorkerApiPaths.stop(pathSegment(runId))))
                        .timeout(effectiveTimeout(timeout))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Worker " + endpointLabel(endpoint)
                    + " stop failed with status " + response.statusCode()
                    + ": " + response.body());
        }
    }

    private URI uri(PerformanceWorkerEndpoint endpoint, String path) {
        return URI.create("http://" + endpoint.getHost() + ":" + endpoint.getPort() + path);
    }

    private String pathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private String endpointLabel(PerformanceWorkerEndpoint endpoint) {
        return endpoint.getHost() + ":" + endpoint.getPort();
    }

    private static Duration effectiveTimeout(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return DEFAULT_REQUEST_TIMEOUT;
        }
        return timeout;
    }
}
