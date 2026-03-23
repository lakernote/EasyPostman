package com.laker.postman.plugin.capture;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SystemProxyService {
    private static final Logger log = LoggerFactory.getLogger(SystemProxyService.class);
    private static final String NETWORKSETUP = "/usr/sbin/networksetup";
    private static final List<String> REQUIRED_BYPASS_DOMAINS = List.of("localhost", "127.0.0.1", "::1");
    private static final String EMPTY = "Empty";

    private final Thread shutdownHook;

    private volatile Map<String, ProxyServiceSnapshot> snapshots = Map.of();
    private volatile boolean active;
    private volatile String activeHost = "";
    private volatile int activePort;

    SystemProxyService() {
        shutdownHook = new Thread(this::restoreQuietly, "capture-system-proxy-restore");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    boolean isActive() {
        return active;
    }

    synchronized void enable(String host, int port) throws Exception {
        ensureSupported();
        String proxyHost = normalizeProxyHost(host);
        if (active) {
            if (proxyHost.equals(activeHost) && port == activePort) {
                return;
            }
            restoreSnapshots();
        }

        List<String> services = listEnabledNetworkServices();
        if (services.isEmpty()) {
            throw new IllegalStateException("No enabled macOS network services found");
        }

        Map<String, ProxyServiceSnapshot> captured = new LinkedHashMap<>();
        try {
            for (String service : services) {
                ProxyServiceSnapshot snapshot = readSnapshot(service);
                applyProxy(service, proxyHost, port, snapshot.bypassDomains());
                captured.put(service, snapshot);
            }
            snapshots = Map.copyOf(captured);
            activeHost = proxyHost;
            activePort = port;
            active = true;
        } catch (Exception ex) {
            snapshots = captured;
            restoreQuietly();
            throw ex;
        }
    }

    synchronized void disable() throws Exception {
        if (!active) {
            return;
        }
        restoreSnapshots();
    }

    String statusSummary() {
        if (!isSupported()) {
            return "System proxy: unsupported";
        }
        if (!active) {
            return "System proxy: manual";
        }
        return "System proxy: synced to " + activeHost + ":" + activePort;
    }

    private void ensureSupported() {
        if (!isSupported()) {
            throw new IllegalStateException("System proxy sync is only supported on macOS");
        }
    }

    private synchronized void restoreSnapshots() throws Exception {
        Map<String, ProxyServiceSnapshot> currentSnapshots = snapshots;
        try {
            for (Map.Entry<String, ProxyServiceSnapshot> entry : currentSnapshots.entrySet()) {
                restoreSnapshot(entry.getKey(), entry.getValue());
            }
        } finally {
            active = false;
            activeHost = "";
            activePort = 0;
            snapshots = Map.of();
        }
    }

    private void restoreQuietly() {
        try {
            restoreSnapshots();
        } catch (Exception ex) {
            log.warn("Failed to restore macOS proxy settings", ex);
        }
    }

    private void applyProxy(String service, String host, int port, List<String> originalBypassDomains) throws Exception {
        runCommand(NETWORKSETUP, "-setproxyautodiscovery", service, "off");
        runCommand(NETWORKSETUP, "-setautoproxystate", service, "off");
        runCommand(NETWORKSETUP, "-setwebproxy", service, host, String.valueOf(port));
        runCommand(NETWORKSETUP, "-setsecurewebproxy", service, host, String.valueOf(port));

        LinkedHashSet<String> bypassDomains = new LinkedHashSet<>(originalBypassDomains);
        bypassDomains.addAll(REQUIRED_BYPASS_DOMAINS);
        setBypassDomains(service, new ArrayList<>(bypassDomains));

        runCommand(NETWORKSETUP, "-setwebproxystate", service, "on");
        runCommand(NETWORKSETUP, "-setsecurewebproxystate", service, "on");
    }

    private void restoreSnapshot(String service, ProxyServiceSnapshot snapshot) throws Exception {
        restoreProxy(service, false, snapshot.webProxy());
        restoreProxy(service, true, snapshot.secureWebProxy());
        setBypassDomains(service, snapshot.bypassDomains());

        if (snapshot.autoProxyUrl() != null && !snapshot.autoProxyUrl().isBlank()) {
            runCommand(NETWORKSETUP, "-setautoproxyurl", service, snapshot.autoProxyUrl());
        }
        runCommand(NETWORKSETUP, "-setautoproxystate", service, snapshot.autoProxyEnabled() ? "on" : "off");
        runCommand(NETWORKSETUP, "-setproxyautodiscovery", service, snapshot.autoDiscoveryEnabled() ? "on" : "off");
    }

    private void restoreProxy(String service, boolean secure, ProxyEndpoint endpoint) throws Exception {
        if (endpoint.server() != null && !endpoint.server().isBlank() && endpoint.port() > 0) {
            runCommand(
                    NETWORKSETUP,
                    secure ? "-setsecurewebproxy" : "-setwebproxy",
                    service,
                    endpoint.server(),
                    String.valueOf(endpoint.port())
            );
        }
        runCommand(
                NETWORKSETUP,
                secure ? "-setsecurewebproxystate" : "-setwebproxystate",
                service,
                endpoint.enabled() ? "on" : "off"
        );
    }

    private ProxyServiceSnapshot readSnapshot(String service) throws Exception {
        ProxyEndpoint webProxy = readProxyEndpoint(service, false);
        ProxyEndpoint secureWebProxy = readProxyEndpoint(service, true);
        boolean autoDiscoveryEnabled = parseEnabled(readLines(NETWORKSETUP, "-getproxyautodiscovery", service));
        AutoProxyConfig autoProxyConfig = readAutoProxyConfig(service);
        List<String> bypassDomains = readBypassDomains(service);
        return new ProxyServiceSnapshot(
                webProxy,
                secureWebProxy,
                bypassDomains,
                autoDiscoveryEnabled,
                autoProxyConfig.enabled(),
                autoProxyConfig.url()
        );
    }

    private ProxyEndpoint readProxyEndpoint(String service, boolean secure) throws Exception {
        List<String> lines = readLines(NETWORKSETUP, secure ? "-getsecurewebproxy" : "-getwebproxy", service);
        Map<String, String> values = parseKeyValueLines(lines);
        boolean enabled = "yes".equalsIgnoreCase(values.getOrDefault("Enabled", "No"));
        String server = values.getOrDefault("Server", "");
        int port = parsePort(values.get("Port"));
        return new ProxyEndpoint(enabled, server, port);
    }

    private AutoProxyConfig readAutoProxyConfig(String service) throws Exception {
        Map<String, String> values = parseKeyValueLines(readLines(NETWORKSETUP, "-getautoproxyurl", service));
        boolean enabled = "yes".equalsIgnoreCase(values.getOrDefault("Enabled", "No"));
        return new AutoProxyConfig(enabled, values.getOrDefault("URL", ""));
    }

    private List<String> readBypassDomains(String service) throws Exception {
        List<String> lines = readLines(NETWORKSETUP, "-getproxybypassdomains", service);
        List<String> domains = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.toLowerCase(Locale.ROOT).contains("there aren't any bypass domains")) {
                return List.of();
            }
            domains.add(trimmed);
        }
        return domains;
    }

    private List<String> listEnabledNetworkServices() throws Exception {
        List<String> lines = readLines(NETWORKSETUP, "-listallnetworkservices");
        List<String> services = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("An asterisk")) {
                continue;
            }
            if (trimmed.startsWith("*")) {
                continue;
            }
            services.add(trimmed);
        }
        return services;
    }

    private void setBypassDomains(String service, List<String> domains) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(NETWORKSETUP);
        command.add("-setproxybypassdomains");
        command.add(service);
        if (domains == null || domains.isEmpty()) {
            command.add(EMPTY);
        } else {
            command.addAll(domains);
        }
        runCommand(command);
    }

    private boolean parseEnabled(List<String> lines) {
        Map<String, String> values = parseKeyValueLines(lines);
        return values.values().stream().findFirst()
                .map(value -> "yes".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value))
                .orElse(false);
    }

    private Map<String, String> parseKeyValueLines(List<String> lines) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : lines) {
            int separator = line.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            values.put(key, value);
        }
        return values;
    }

    private int parsePort(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private List<String> readLines(String... command) throws Exception {
        return readLines(List.of(command));
    }

    private List<String> readLines(List<String> command) throws Exception {
        return runCommand(command).lines();
    }

    private CommandResult runCommand(String... command) throws Exception {
        return runCommand(List.of(command));
    }

    private CommandResult runCommand(List<String> command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException(String.join(System.lineSeparator(), lines));
        }
        return new CommandResult(lines);
    }

    private String normalizeProxyHost(String host) {
        if (host == null || host.isBlank()) {
            return "127.0.0.1";
        }
        String trimmed = host.trim();
        if ("0.0.0.0".equals(trimmed) || "::".equals(trimmed) || "::0".equals(trimmed)) {
            return "127.0.0.1";
        }
        return trimmed;
    }

    private record CommandResult(List<String> lines) {
    }

    private record ProxyServiceSnapshot(
            ProxyEndpoint webProxy,
            ProxyEndpoint secureWebProxy,
            List<String> bypassDomains,
            boolean autoDiscoveryEnabled,
            boolean autoProxyEnabled,
            String autoProxyUrl
    ) {
    }

    private record ProxyEndpoint(boolean enabled, String server, int port) {
    }

    private record AutoProxyConfig(boolean enabled, String url) {
    }
}
