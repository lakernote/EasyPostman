package com.laker.postman.performance.core.worker;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class PerformanceWorkerEndpointParser {

    private static final String ENDPOINT_DELIMITER_REGEX = "[,\\s]+";

    public List<PerformanceWorkerEndpoint> parse(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<PerformanceWorkerEndpoint> endpoints = new ArrayList<>();
        for (String fragment : text.split(ENDPOINT_DELIMITER_REGEX)) {
            String trimmed = fragment.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            endpoints.add(parseEndpoint(trimmed));
        }
        return endpoints;
    }

    public String formatList(List<PerformanceWorkerEndpoint> endpoints) {
        if (endpoints == null || endpoints.isEmpty()) {
            return "";
        }
        return endpoints.stream()
                .map(endpoint -> endpoint.getHost() + ":" + endpoint.getPort())
                .collect(Collectors.joining(","));
    }

    private PerformanceWorkerEndpoint parseEndpoint(String fragment) {
        int colon = fragment.indexOf(':');
        if (colon <= 0 || colon != fragment.lastIndexOf(':') || colon == fragment.length() - 1) {
            throw invalidEndpoint(fragment, "must use host:port");
        }

        String host = fragment.substring(0, colon).trim();
        String portText = fragment.substring(colon + 1).trim();
        if (host.isEmpty() || portText.isEmpty()) {
            throw invalidEndpoint(fragment, "must use host:port");
        }

        int port = parsePort(fragment, portText);
        return new PerformanceWorkerEndpoint(host, port);
    }

    private int parsePort(String fragment, String portText) {
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException ex) {
            throw invalidEndpoint(fragment, "port must be a number");
        }
        if (port < 1 || port > 65_535) {
            throw invalidEndpoint(fragment, "port must be between 1 and 65535");
        }
        return port;
    }

    private IllegalArgumentException invalidEndpoint(String fragment, String reason) {
        return new IllegalArgumentException("Invalid worker endpoint '" + fragment + "': " + reason);
    }
}
