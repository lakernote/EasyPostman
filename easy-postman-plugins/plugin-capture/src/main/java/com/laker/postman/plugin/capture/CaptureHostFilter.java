package com.laker.postman.plugin.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CaptureHostFilter {
    private final String rawValue;
    private final List<String> rules;

    private CaptureHostFilter(String rawValue, List<String> rules) {
        this.rawValue = rawValue;
        this.rules = rules;
    }

    static CaptureHostFilter parse(String rawValue) {
        String normalizedRaw = rawValue == null ? "" : rawValue.trim();
        List<String> rules = new ArrayList<>();
        if (!normalizedRaw.isEmpty()) {
            for (String token : normalizedRaw.split("[,\\s]+")) {
                String rule = normalizeHost(token);
                if (!rule.isEmpty() && !rules.contains(rule)) {
                    rules.add(rule);
                }
            }
        }
        return new CaptureHostFilter(normalizedRaw, rules);
    }

    boolean matches(String host) {
        if (rules.isEmpty()) {
            return true;
        }
        String normalizedHost = normalizeHost(host);
        for (String rule : rules) {
            if (normalizedHost.equals(rule) || normalizedHost.endsWith("." + rule)) {
                return true;
            }
        }
        return false;
    }

    boolean isMatchAll() {
        return rules.isEmpty();
    }

    String rawValue() {
        return rawValue;
    }

    String summary() {
        if (rules.isEmpty()) {
            return "Capture filter: all hosts";
        }
        return "Capture filter: " + String.join(", ", rules);
    }

    private static String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
