package com.laker.postman.plugin.capture;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

record CaptureSourceInfo(String clientHost,
                         int clientPort,
                         String proxyHost,
                         int proxyPort,
                         String processId,
                         String processName,
                         String processPath) {

    static CaptureSourceInfo unknown() {
        return network("", 0, "", 0);
    }

    static CaptureSourceInfo network(String clientHost, int clientPort, String proxyHost, int proxyPort) {
        return new CaptureSourceInfo(
                clean(clientHost),
                clientPort,
                clean(proxyHost),
                proxyPort,
                "",
                "",
                ""
        );
    }

    CaptureSourceInfo withProcess(String processId, String processName, String processPath) {
        return new CaptureSourceInfo(
                clientHost,
                clientPort,
                proxyHost,
                proxyPort,
                clean(processId),
                clean(processName),
                clean(processPath)
        );
    }

    String clientEndpoint() {
        return endpoint(clientHost, clientPort);
    }

    String proxyEndpoint() {
        return endpoint(proxyHost, proxyPort);
    }

    String tableText() {
        if (!processName.isBlank() && !processId.isBlank()) {
            return processName + " · PID " + processId;
        }
        if (!processName.isBlank()) {
            return processName;
        }
        if (!processId.isBlank()) {
            return "PID " + processId;
        }
        String endpoint = clientEndpoint();
        return endpoint.isBlank() ? t(MessageKeys.TOOLBOX_CAPTURE_SOURCE_UNKNOWN) : endpoint;
    }

    String sourceTableText() {
        if (!processName.isBlank()) {
            return processName;
        }
        String endpoint = clientEndpoint();
        return endpoint.isBlank() ? t(MessageKeys.TOOLBOX_CAPTURE_SOURCE_UNKNOWN) : endpoint;
    }

    String pidTableText() {
        return processId.isBlank() ? "-" : processId;
    }

    String detailText() {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_SOURCE_PROCESS), processSummary());
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_SOURCE_PID), processId.isBlank() ? "-" : processId);
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_SOURCE_PATH), processPath.isBlank() ? "-" : processPath);
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_SOURCE_CLIENT_ENDPOINT), valueOrDash(clientEndpoint()));
        appendLine(builder, t(MessageKeys.TOOLBOX_CAPTURE_SOURCE_PROXY_ENDPOINT), valueOrDash(proxyEndpoint()));
        return builder.toString();
    }

    private String processSummary() {
        if (!processName.isBlank() && !processId.isBlank()) {
            return processName + " · PID " + processId;
        }
        if (!processName.isBlank()) {
            return processName;
        }
        if (!processId.isBlank()) {
            return "PID " + processId;
        }
        return t(MessageKeys.TOOLBOX_CAPTURE_SOURCE_UNRESOLVED);
    }

    private static String endpoint(String host, int port) {
        if (host == null || host.isBlank() || port <= 0) {
            return "";
        }
        return host + ":" + port;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static void appendLine(StringBuilder builder, String label, String value) {
        builder.append(label).append(": ").append(value == null ? "" : value).append('\n');
    }
}
