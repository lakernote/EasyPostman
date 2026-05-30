package com.laker.postman.performance.worker;

import com.laker.postman.performance.core.model.PerformanceStatsSnapshot;
import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummary;
import com.laker.postman.performance.core.report.PerformanceJsonReportMapper;
import com.laker.postman.performance.core.report.PerformanceJsonReportMetadata;
import com.laker.postman.performance.core.run.PerformanceRunStatus;
import com.laker.postman.performance.core.runtime.PerformanceThreadFactory;
import com.laker.postman.performance.core.worker.PerformanceWorkerErrorResponse;
import com.laker.postman.performance.core.worker.PerformanceWorkerApiPaths;
import com.laker.postman.performance.core.worker.PerformanceWorkerAssignment;
import com.laker.postman.performance.core.worker.PerformanceWorkerProtocolJsonStorage;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunAcceptedResponse;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunRequest;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunResultResponse;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunStatusResponse;
import com.laker.postman.performance.runtime.PerformanceRunExecutionControl;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PerformanceWorkerServer implements AutoCloseable {
    private final PerformanceWorkerOptions options;
    private final PerformanceWorkerRunExecutor runExecutor;
    private final PerformanceWorkerServerListener listener;
    private final PerformanceWorkerProtocolJsonStorage jsonStorage = new PerformanceWorkerProtocolJsonStorage();
    private final Map<String, WorkerRunState> runs = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private final Object runLock = new Object();
    private ExecutorService requestExecutor;
    private ExecutorService runExecutorService;
    private ScheduledExecutorService progressExecutor;
    private ScheduledFuture<?> progressTask;
    private HttpServer httpServer;
    private volatile int port;

    public PerformanceWorkerServer(PerformanceWorkerOptions options) {
        this(options, new DefaultPerformanceWorkerRunExecutor(), PerformanceWorkerServerListener.NOOP);
    }

    public PerformanceWorkerServer(PerformanceWorkerOptions options, PerformanceWorkerRunExecutor runExecutor) {
        this(options, runExecutor, PerformanceWorkerServerListener.NOOP);
    }

    public PerformanceWorkerServer(PerformanceWorkerOptions options,
                                   PerformanceWorkerRunExecutor runExecutor,
                                   PerformanceWorkerServerListener listener) {
        this.options = options == null ? PerformanceWorkerOptions.builder().build() : options;
        this.runExecutor = runExecutor == null ? new DefaultPerformanceWorkerRunExecutor() : runExecutor;
        this.listener = listener == null ? PerformanceWorkerServerListener.NOOP : listener;
    }

    public void start() throws IOException {
        if (running.get()) {
            return;
        }
        requestExecutor = Executors.newCachedThreadPool(
                PerformanceThreadFactory.daemonFactory("PerformanceWorkerHttp")
        );
        runExecutorService = Executors.newCachedThreadPool(
                PerformanceThreadFactory.daemonFactory("PerformanceWorkerRun")
        );
        httpServer = HttpServer.create(new InetSocketAddress(options.getHost(), options.getPort()), 0);
        httpServer.createContext(PerformanceWorkerApiPaths.HEALTH, this::handleHealth);
        httpServer.createContext(PerformanceWorkerApiPaths.RUNS, this::handleRuns);
        httpServer.setExecutor(requestExecutor);
        httpServer.start();
        port = httpServer.getAddress().getPort();
        running.set(true);
        startProgressReporter();
    }

    public void stop() {
        HttpServer server = httpServer;
        httpServer = null;
        if (server != null) {
            server.stop(0);
        }
        if (runExecutorService != null) {
            runExecutorService.shutdownNow();
            runExecutorService = null;
        }
        stopProgressReporter();
        if (requestExecutor != null) {
            requestExecutor.shutdownNow();
            requestExecutor = null;
        }
        running.set(false);
        shutdownLatch.countDown();
    }

    public void awaitShutdown() throws InterruptedException {
        shutdownLatch.await();
    }

    public boolean isRunning() {
        return running.get();
    }

    public PerformanceWorkerOptions getOptions() {
        return options;
    }

    public int getPort() {
        return port == 0 ? options.getPort() : port;
    }

    @Override
    public void close() {
        stop();
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        pruneCompletedRuns();
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            write(exchange, 405, error("Method not allowed"));
            return;
        }
        write(exchange, 200, """
                {"status":"UP","workerId":"%s","host":"%s","port":%d}
                """.formatted(workerId(), options.getHost(), getPort()));
    }

    private void handleRuns(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (PerformanceWorkerApiPaths.RUNS.equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            handleRunSubmit(exchange);
            return;
        }
        String suffix = path.substring(PerformanceWorkerApiPaths.RUNS.length());
        String[] parts = suffix.split("/");
        if (parts.length >= 2 && !parts[1].isBlank()) {
            String runId = parts[1];
            if (parts.length == 2 && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleRunStatus(exchange, runId);
                return;
            }
            if (parts.length == 3 && "result".equals(parts[2]) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleRunResult(exchange, runId);
                return;
            }
            if (parts.length == 3 && "stop".equals(parts[2]) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleRunStop(exchange, runId);
                return;
            }
        }
        write(exchange, 404, error("Not found"));
    }

    private void handleRunSubmit(HttpExchange exchange) throws IOException {
        PerformanceWorkerRunRequest request;
        try {
            request = jsonStorage.runRequestFromJson(readBody(exchange));
        } catch (RuntimeException ex) {
            write(exchange, 400, error("Invalid run request: " + ex.getMessage()));
            return;
        }
        if (request == null || request.getPlan() == null) {
            write(exchange, 400, error("Run request requires plan"));
            return;
        }
        if (request.getAssignment() == null) {
            write(exchange, 400, error("Run request requires worker assignment"));
            return;
        }
        if (!request.getRunId().isBlank()
                && !request.getAssignment().getRunId().isBlank()
                && !request.getRunId().equals(request.getAssignment().getRunId())) {
            write(exchange, 400, error("Run request runId does not match assignment runId"));
            return;
        }

        // worker 只接收控制面 JSON；plan.assets 中的本地文件路径必须由用户提前放到本机同一路径。
        String runId;
        WorkerRunState state;
        boolean busy;
        synchronized (runLock) {
            pruneCompletedRuns();
            busy = hasActiveRun();
            if (busy) {
                runId = "";
                state = null;
            } else {
                runId = resolveRunId(request);
                state = new WorkerRunState(workerId());
                runs.put(runId, state);
            }
        }
        if (busy) {
            write(exchange, 409, error("Worker is already running"));
            return;
        }
        PerformanceWorkerRunRequest effectiveRequest = requestWithRunId(request, runId);
        notifyAccepted(runId, state.workerId, effectiveRequest.getAssignment());
        try {
            runExecutorService.submit(() -> executeRun(runId, effectiveRequest, state));
        } catch (RuntimeException ex) {
            runs.remove(runId);
            throw ex;
        }
        write(exchange, 202, jsonStorage.toJson(PerformanceWorkerRunAcceptedResponse.builder()
                .runId(runId)
                .workerId(workerId())
                .build()));
    }

    private PerformanceWorkerRunRequest requestWithRunId(PerformanceWorkerRunRequest request, String runId) {
        PerformanceWorkerAssignment assignment = request.getAssignment();
        if (assignment != null && !runId.equals(assignment.getRunId())) {
            assignment = PerformanceWorkerAssignment.builder()
                    .schemaVersion(assignment.getSchemaVersion())
                    .runId(runId)
                    .workerId(assignment.getWorkerId())
                    .assignmentId(assignment.getAssignmentId())
                    .endpoint(assignment.getEndpoint())
                    .threadGroups(assignment.getThreadGroups())
                    .build();
        }
        return PerformanceWorkerRunRequest.builder()
                .runId(runId)
                .plan(request.getPlan())
                .assignment(assignment)
                .build();
    }

    private String resolveRunId(PerformanceWorkerRunRequest request) {
        if (!request.getRunId().isBlank()) {
            return request.getRunId();
        }
        if (request.getAssignment() != null && !request.getAssignment().getRunId().isBlank()) {
            return request.getAssignment().getRunId();
        }
        return UUID.randomUUID().toString();
    }

    private boolean hasActiveRun() {
        return runs.values().stream().anyMatch(WorkerRunState::isActive);
    }

    private void executeRun(String runId, PerformanceWorkerRunRequest request, WorkerRunState state) {
        state.startedAtMs = System.currentTimeMillis();
        state.status = PerformanceRunStatus.RUNNING;
        notifyStarted(runId, state.workerId);
        try {
            PerformanceJsonReport report = runExecutor.execute(request, state.control);
            state.report = report;
            String reportStatus = report == null || report.getMetadata() == null
                    ? PerformanceRunStatus.SUCCESS
                    : report.getMetadata().getStatus();
            state.status = state.stopRequested ? PerformanceRunStatus.STOPPED : reportStatus;
        } catch (Exception ex) {
            state.status = PerformanceRunStatus.FAILED;
            state.error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        } finally {
            state.completedAtMs = System.currentTimeMillis();
            notifyCompleted(runId, state);
        }
    }

    private void handleRunStatus(HttpExchange exchange, String runId) throws IOException {
        pruneCompletedRuns();
        WorkerRunState state = runs.get(runId);
        if (state == null) {
            write(exchange, 404, error("Run not found: " + runId));
            return;
        }
        write(exchange, 200, jsonStorage.toJson(statusResponse(runId, state)));
    }

    private void handleRunResult(HttpExchange exchange, String runId) throws IOException {
        pruneCompletedRuns();
        WorkerRunState state = runs.get(runId);
        if (state == null) {
            write(exchange, 404, error("Run not found: " + runId));
            return;
        }
        write(exchange, 200, jsonStorage.toJson(PerformanceWorkerRunResultResponse.builder()
                .runId(runId)
                .workerId(state.workerId)
                .status(state.status)
                .report(state.report)
                .error(state.error)
                .build()));
    }

    private void handleRunStop(HttpExchange exchange, String runId) throws IOException {
        pruneCompletedRuns();
        WorkerRunState state = runs.get(runId);
        if (state == null) {
            write(exchange, 404, error("Run not found: " + runId));
            return;
        }
        state.stopRequested = true;
        state.control.stop();
        if (state.isActive()) {
            state.status = PerformanceRunStatus.STOPPING;
        }
        write(exchange, 200, jsonStorage.toJson(statusResponse(runId, state)));
    }

    private void pruneCompletedRuns() {
        long cutoff = System.currentTimeMillis() - options.getCompletedRunRetentionMs();
        runs.entrySet().removeIf(entry -> entry.getValue().isExpired(cutoff));
    }

    private String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private void write(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = (body == null ? "" : body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String error(String message) {
        return jsonStorage.toJson(PerformanceWorkerErrorResponse.builder().error(message).build());
    }

    private String workerId() {
        return options.getHost() + ":" + getPort();
    }

    private void startProgressReporter() {
        long intervalMs = options.getProgressIntervalMs();
        if (intervalMs <= 0) {
            return;
        }
        progressExecutor = Executors.newSingleThreadScheduledExecutor(
                PerformanceThreadFactory.daemonFactory("PerformanceWorkerProgress")
        );
        progressTask = progressExecutor.scheduleAtFixedRate(
                this::notifyActiveRunProgress,
                intervalMs,
                intervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    private void stopProgressReporter() {
        if (progressTask != null) {
            progressTask.cancel(false);
            progressTask = null;
        }
        if (progressExecutor != null) {
            progressExecutor.shutdownNow();
            progressExecutor = null;
        }
    }

    private void notifyActiveRunProgress() {
        for (Map.Entry<String, WorkerRunState> entry : runs.entrySet()) {
            WorkerRunState state = entry.getValue();
            if (state != null && state.isActive()) {
                notifyProgress(entry.getKey(), statusResponse(entry.getKey(), state));
            }
        }
    }

    private PerformanceWorkerRunStatusResponse statusResponse(String runId, WorkerRunState state) {
        PerformanceJsonReport report = currentReport(runId, state);
        PerformanceJsonReportSummary summary = report == null ? null : report.getSummary();
        return PerformanceWorkerRunStatusResponse.builder()
                .runId(runId)
                .workerId(state.workerId)
                .status(state.status)
                .activeUsers(state.control.getActiveUsers())
                .totalUsers(state.control.getTotalUsers())
                .totalRequests(summary == null ? 0L : summary.getTotalRequests())
                .successRequests(summary == null ? 0L : summary.getSuccessRequests())
                .failedRequests(summary == null ? 0L : summary.getFailedRequests())
                .qps(qps(report))
                .report(report)
                .error(state.error)
                .build();
    }

    private PerformanceJsonReport currentReport(String runId, WorkerRunState state) {
        if (state.report != null) {
            return state.report;
        }
        PerformanceStatsSnapshot snapshot = state.control.statsSnapshot();
        if (snapshot.totalRequests() == 0 && snapshot.summaries().isEmpty()) {
            return null;
        }
        long now = System.currentTimeMillis();
        return PerformanceJsonReportMapper.fromStatsSnapshot(
                PerformanceJsonReportMetadata.builder()
                        .runId(runId)
                        .source(state.workerId)
                        .status(state.status)
                        .startTimeMs(state.startedAtMs)
                        .endTimeMs(now)
                        .elapsedTimeMs(Math.max(0L, now - state.startedAtMs))
                        .stopped(state.stopRequested)
                        .error(state.error)
                        .build(),
                snapshot
        );
    }

    private double qps(PerformanceJsonReport report) {
        if (report == null || report.getProtocols() == null) {
            return 0.0;
        }
        return report.getProtocols().values().stream()
                .filter(protocol -> protocol != null && protocol.getTotal() != null)
                .mapToDouble(protocol -> protocol.getTotal().getSamplesPerSecond())
                .sum();
    }

    private void notifyAccepted(String runId, String workerId, PerformanceWorkerAssignment assignment) {
        try {
            listener.onRunAccepted(runId, workerId, assignment);
        } catch (RuntimeException ignored) {
            // listener 只负责控制台提示，不能影响 worker 控制面响应。
        }
    }

    private void notifyStarted(String runId, String workerId) {
        try {
            listener.onRunStarted(runId, workerId);
        } catch (RuntimeException ignored) {
            // listener 只负责控制台提示，不能影响压测执行。
        }
    }

    private void notifyProgress(String runId, PerformanceWorkerRunStatusResponse status) {
        try {
            listener.onRunProgress(
                    runId,
                    status.getWorkerId(),
                    status.getStatus(),
                    status.getActiveUsers(),
                    status.getTotalUsers(),
                    status.getTotalRequests(),
                    status.getSuccessRequests(),
                    status.getFailedRequests(),
                    status.getQps()
            );
        } catch (RuntimeException ignored) {
            // listener 只负责控制台提示，不能影响 worker 执行。
        }
    }

    private void notifyCompleted(String runId, WorkerRunState state) {
        PerformanceJsonReportSummary summary = state.report == null ? null : state.report.getSummary();
        try {
            listener.onRunCompleted(
                    runId,
                    state.workerId,
                    state.status,
                    summary,
                    Math.max(0L, state.completedAtMs - state.startedAtMs),
                    state.error
            );
        } catch (RuntimeException ignored) {
            // listener 只负责控制台提示，不能影响结果查询。
        }
    }

    @RequiredArgsConstructor
    private static final class WorkerRunState {
        private final String workerId;
        private final PerformanceRunExecutionControl control = new PerformanceRunExecutionControl();
        private volatile String status = PerformanceRunStatus.PENDING;
        private volatile String error = "";
        private volatile PerformanceJsonReport report;
        private volatile boolean stopRequested;
        private volatile long startedAtMs;
        private volatile long completedAtMs;

        private boolean isActive() {
            return PerformanceRunStatus.PENDING.equals(status)
                    || PerformanceRunStatus.RUNNING.equals(status)
                    || PerformanceRunStatus.STOPPING.equals(status);
        }

        private boolean isExpired(long cutoffMs) {
            return completedAtMs > 0 && completedAtMs < cutoffMs && !isActive();
        }
    }
}
