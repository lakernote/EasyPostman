package com.laker.postman.performance.core.report;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.run.PerformanceRunStatus;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class PerformanceJsonReportSummaryMapper {

    public Map<String, PerformanceJsonReportProtocol> emptyProtocols() {
        Map<String, PerformanceJsonReportProtocol> protocols = new LinkedHashMap<>();
        for (PerformanceProtocol protocol : PerformanceProtocol.values()) {
            protocols.put(protocol.name(), PerformanceJsonReportProtocol.builder()
                    .protocol(protocol.name())
                    .total(PerformanceJsonReportApi.builder().protocol(protocol.name()).build())
                    .apis(List.of())
                    .build());
        }
        return protocols;
    }

    public PerformanceJsonReport merge(String runId,
                                       String source,
                                       String status,
                                       String planPath,
                                       List<PerformanceJsonReport> reports) {
        long total = 0;
        long success = 0;
        long failed = 0;
        long start = 0;
        long end = 0;
        boolean stopped = false;
        StringBuilder errors = new StringBuilder();
        if (reports != null) {
            for (PerformanceJsonReport report : reports) {
                if (report == null) {
                    continue;
                }
                PerformanceJsonReportSummary summary = report.getSummary();
                if (summary != null) {
                    total += summary.getTotalRequests();
                    success += summary.getSuccessRequests();
                    failed += summary.getFailedRequests();
                }
                PerformanceJsonReportMetadata metadata = report.getMetadata();
                if (metadata != null) {
                    if (metadata.getStartTimeMs() > 0 && (start == 0 || metadata.getStartTimeMs() < start)) {
                        start = metadata.getStartTimeMs();
                    }
                    end = Math.max(end, metadata.getEndTimeMs());
                    stopped = stopped || metadata.isStopped();
                    if (metadata.getError() != null && !metadata.getError().isBlank()) {
                        appendError(errors, metadata.getSource(), metadata.getError());
                    }
                    String failureSummary = PerformanceJsonReportStatusResolver.withFailureSummary(null, summary);
                    if (failureSummary != null && !failureSummary.isBlank()) {
                        appendError(errors, metadata.getSource(), failureSummary);
                    }
                }
            }
        }
        String resolvedStatus = PerformanceJsonReportStatusResolver.resolve(
                status,
                stopped,
                errors.toString(),
                PerformanceJsonReportSummary.builder()
                        .totalRequests(total)
                        .successRequests(success)
                        .failedRequests(failed)
                        .build()
        );
        return PerformanceJsonReport.builder()
                .metadata(PerformanceJsonReportMetadata.builder()
                        .runId(runId)
                        .source(source)
                        .status(resolvedStatus)
                        .planPath(planPath)
                        .startTimeMs(start)
                        .endTimeMs(end)
                        .stopped(stopped)
                        .error(errors.toString())
                        .build())
                .summary(PerformanceJsonReportSummary.builder()
                        .totalRequests(total)
                        .successRequests(success)
                        .build())
                .protocols(mergeProtocols(reports))
                .build();
    }

    private void appendError(StringBuilder errors, String source, String error) {
        if (error == null || error.isBlank()) {
            return;
        }
        if (!errors.isEmpty()) {
            errors.append("; ");
        }
        if (source != null && !source.isBlank()) {
            errors.append(source).append(": ");
        }
        errors.append(error);
    }

    private Map<String, PerformanceJsonReportProtocol> mergeProtocols(List<PerformanceJsonReport> reports) {
        Map<String, PerformanceJsonReportProtocol> result = emptyProtocols();
        Map<String, MutableProtocol> protocols = new LinkedHashMap<>();
        for (PerformanceProtocol protocol : PerformanceProtocol.values()) {
            protocols.put(protocol.name(), new MutableProtocol(protocol.name()));
        }
        if (reports != null) {
            for (PerformanceJsonReport report : reports) {
                if (report == null || report.getProtocols() == null) {
                    continue;
                }
                for (Map.Entry<String, PerformanceJsonReportProtocol> entry : report.getProtocols().entrySet()) {
                    MutableProtocol protocol = protocols.computeIfAbsent(entry.getKey(), MutableProtocol::new);
                    protocol.add(entry.getValue());
                }
            }
        }
        for (Map.Entry<String, MutableProtocol> entry : protocols.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toProtocol());
        }
        return result;
    }

    private static final class MutableProtocol {
        private final String protocol;
        private final MutableApi total;
        private final Map<String, MutableApi> apis = new LinkedHashMap<>();

        private MutableProtocol(String protocol) {
            this.protocol = protocol == null ? "" : protocol;
            this.total = new MutableApi(this.protocol, "", this.protocol + " Total");
        }

        private void add(PerformanceJsonReportProtocol source) {
            if (source == null) {
                return;
            }
            total.add(source.getTotal());
            if (source.getApis() == null) {
                return;
            }
            for (PerformanceJsonReportApi api : source.getApis()) {
                if (api == null) {
                    continue;
                }
                String key = api.getApiId().isBlank() ? api.getName() : api.getApiId();
                apis.computeIfAbsent(key, ignored -> new MutableApi(protocol, api.getApiId(), api.getName()))
                        .add(api);
            }
        }

        private PerformanceJsonReportProtocol toProtocol() {
            List<PerformanceJsonReportApi> apiRows = new ArrayList<>();
            for (MutableApi api : apis.values()) {
                apiRows.add(api.toApi());
            }
            return PerformanceJsonReportProtocol.builder()
                    .protocol(protocol)
                    .total(total.toApi())
                    .apis(apiRows)
                    .build();
        }
    }

    private static final class MutableApi {
        private final String protocol;
        private String apiId;
        private String name;
        private long total;
        private long success;
        private double legacySamplesPerSecond;
        private long firstSampleStartTimeMs;
        private long lastSampleEndTimeMs;
        private long sentMessages;
        private long receivedMessages;
        private long matchedMessages;
        private double legacySendRate;
        private double legacyReceiveRate;
        private double legacyMatchedRate;
        private long sentBytes;
        private long receivedBytes;
        private double legacySentBytesPerSecond;
        private double legacyReceivedBytesPerSecond;
        private final MutableDuration duration = new MutableDuration();
        private final MutableDuration firstLatency = new MutableDuration();

        private MutableApi(String protocol, String apiId, String name) {
            this.protocol = protocol == null ? "" : protocol;
            this.apiId = apiId == null ? "" : apiId;
            this.name = name == null ? "" : name;
        }

        private void add(PerformanceJsonReportApi api) {
            if (api == null || api.getTotal() == 0 && api.getSuccess() == 0 && api.getFailed() == 0) {
                return;
            }
            if (apiId.isBlank() && api.getApiId() != null && !api.getApiId().isBlank()) {
                apiId = api.getApiId();
            }
            if (name.isBlank() && api.getName() != null && !api.getName().isBlank()) {
                name = api.getName();
            }
            total += api.getTotal();
            success += api.getSuccess();
            legacySamplesPerSecond += api.getSamplesPerSecond();
            mergeSampleWindow(api.getFirstSampleStartTimeMs(), api.getLastSampleEndTimeMs());
            if (api.getStream() != null) {
                sentMessages += api.getStream().getSentMessages();
                receivedMessages += api.getStream().getReceivedMessages();
                matchedMessages += api.getStream().getMatchedMessages();
                legacySendRate += api.getStream().getSendRate();
                legacyReceiveRate += api.getStream().getReceiveRate();
                legacyMatchedRate += api.getStream().getMatchedRate();
            }
            if (api.getBytes() != null) {
                sentBytes += api.getBytes().getSentBytes();
                receivedBytes += api.getBytes().getReceivedBytes();
                legacySentBytesPerSecond += api.getBytes().getSentBytesPerSecond();
                legacyReceivedBytesPerSecond += api.getBytes().getReceivedBytesPerSecond();
            }
            duration.add(api.getDurationMs(), api.getTotal());
            firstLatency.add(api.getFirstMessageLatencyMs(), positiveWeight(api.getFirstMessageLatencyMs(), api.getTotal()));
        }

        private void mergeSampleWindow(long first, long last) {
            if (first <= 0 || last <= 0) {
                return;
            }
            firstSampleStartTimeMs = firstSampleStartTimeMs == 0 ? first : Math.min(firstSampleStartTimeMs, first);
            lastSampleEndTimeMs = Math.max(lastSampleEndTimeMs, last);
        }

        private PerformanceJsonReportApi toApi() {
            double samplesPerSecond = rate(total, legacySamplesPerSecond);
            double sendRate = rate(sentMessages, legacySendRate);
            double receiveRate = rate(receivedMessages, legacyReceiveRate);
            double matchedRate = rate(matchedMessages, legacyMatchedRate);
            double sentBytesPerSecond = rate(sentBytes, legacySentBytesPerSecond);
            double receivedBytesPerSecond = rate(receivedBytes, legacyReceivedBytesPerSecond);
            return PerformanceJsonReportApi.builder()
                    .apiId(apiId)
                    .name(name)
                    .protocol(protocol)
                    .total(total)
                    .success(success)
                    .failed(Math.max(0L, total - success))
                    .samplesPerSecond(samplesPerSecond)
                    .firstSampleStartTimeMs(firstSampleStartTimeMs)
                    .lastSampleEndTimeMs(lastSampleEndTimeMs)
                    .durationMs(duration.toDuration())
                    .bytes(PerformanceJsonReportBytes.builder()
                            .sentBytes(sentBytes)
                            .receivedBytes(receivedBytes)
                            .sentBytesPerSecond(sentBytesPerSecond)
                            .receivedBytesPerSecond(receivedBytesPerSecond)
                            .avgReceivedBytes(total == 0 ? 0 : receivedBytes / total)
                            .build())
                    .stream(PerformanceJsonReportStream.builder()
                            .sentMessages(sentMessages)
                            .receivedMessages(receivedMessages)
                            .matchedMessages(matchedMessages)
                            .sendRate(sendRate)
                            .receiveRate(receiveRate)
                            .matchedRate(matchedRate)
                            .build())
                    .firstMessageLatencyMs(firstLatency.toDuration())
                    .build();
        }

        private double rate(long count, double legacyRate) {
            if (count <= 0) {
                return 0D;
            }
            if (firstSampleStartTimeMs <= 0 || lastSampleEndTimeMs <= 0) {
                return legacyRate;
            }
            long spanMs = Math.max(1L, lastSampleEndTimeMs - firstSampleStartTimeMs);
            return count * 1000.0 / spanMs;
        }

        private static long positiveWeight(PerformanceJsonReportDuration duration, long fallback) {
            return duration != null && duration.getAvg() > 0 ? fallback : 0L;
        }
    }

    private static final class MutableDuration {
        private long avg;
        private long avgWeight;
        private long min;
        private long max;
        private long p90;
        private long p95;
        private long p99;

        private void add(PerformanceJsonReportDuration duration, long weight) {
            if (duration == null) {
                return;
            }
            long safeWeight = Math.max(0L, weight);
            if (safeWeight > 0 && duration.getAvg() > 0) {
                avg = weightedAverage(avg, avgWeight, duration.getAvg(), safeWeight);
                avgWeight += safeWeight;
            }
            if (duration.getMin() > 0) {
                min = min == 0 ? duration.getMin() : Math.min(min, duration.getMin());
            }
            max = Math.max(max, duration.getMax());
            // 目前 report JSON 未携带直方图 bucket，master 只能用各 worker 分位值的最大值做保守近似。
            p90 = Math.max(p90, duration.getP90());
            p95 = Math.max(p95, duration.getP95());
            p99 = Math.max(p99, duration.getP99());
        }

        private PerformanceJsonReportDuration toDuration() {
            return PerformanceJsonReportDuration.builder()
                    .avg(avg)
                    .min(min)
                    .max(max)
                    .p90(p90)
                    .p95(p95)
                    .p99(p99)
                    .build();
        }

        private static long weightedAverage(long leftValue, long leftWeight, long rightValue, long rightWeight) {
            long totalWeight = leftWeight + rightWeight;
            if (totalWeight <= 0) {
                return 0;
            }
            return (leftValue * leftWeight + rightValue * rightWeight) / totalWeight;
        }
    }

}
