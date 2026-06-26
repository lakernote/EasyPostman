package com.laker.postman.plugin.capture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

final class CaptureSourceAppResolver {
    private static final Logger log = LoggerFactory.getLogger(CaptureSourceAppResolver.class);
    private static final Duration COMMAND_TIMEOUT = Duration.ofMillis(1_500);

    void resolveAsync(CaptureConnectionContext context, CaptureSessionStore sessionStore) {
        if (context == null) {
            return;
        }
        CompletableFuture
                .supplyAsync(() -> resolve(context.sourceInfo()))
                .thenAccept(resolved -> {
                    context.completeSourceResolution(resolved);
                    CaptureDiagnosticEvent event = CaptureDiagnosticEvent.info(
                            CaptureDiagnosticPhase.SOURCE_RESOLVE,
                            CaptureDiagnosticRole.SOURCE_APP,
                            t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_SOURCE_APP_RESOLVED),
                            resolved.detailText(),
                            ""
                    );
                    context.addDiagnostic(event);
                    if (sessionStore != null) {
                        sessionStore.updateSourceForConnection(context.connectionId(), resolved);
                        sessionStore.appendDiagnosticEventForConnection(context.connectionId(), event);
                    }
                })
                .exceptionally(ex -> {
                    context.completeSourceResolutionFailure();
                    log.debug("Failed to resolve capture source application", ex);
                    return null;
                });
    }

    CaptureSourceInfo resolve(CaptureSourceInfo sourceInfo) {
        if (sourceInfo == null || sourceInfo.clientEndpoint().isBlank()) {
            return sourceInfo == null ? CaptureSourceInfo.unknown() : sourceInfo;
        }
        try {
            String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (osName.contains("win")) {
                return resolveWindows(sourceInfo);
            }
            return resolveWithLsof(sourceInfo);
        } catch (Exception ex) {
            log.debug("Source application resolution failed for {}", sourceInfo.clientEndpoint(), ex);
            return sourceInfo;
        }
    }

    private CaptureSourceInfo resolveWithLsof(CaptureSourceInfo sourceInfo) throws Exception {
        String output = runCommand(List.of(
                "lsof",
                "-nP",
                "-iTCP:" + sourceInfo.clientPort(),
                "-sTCP:ESTABLISHED",
                "-F",
                "pcn"
        ));
        long currentPid = ProcessHandle.current().pid();
        Optional<ProcessMatch> match = parseLsofOutput(sourceInfo, output, currentPid);
        if (match.isEmpty()) {
            return sourceInfo;
        }
        String path = resolveUnixProcessPath(match.get().processId());
        return sourceInfo.withProcess(match.get().processId(), match.get().processName(), path);
    }

    private CaptureSourceInfo resolveWindows(CaptureSourceInfo sourceInfo) throws Exception {
        String output = runCommand(List.of("netstat", "-ano", "-p", "tcp"));
        long currentPid = ProcessHandle.current().pid();
        Optional<String> pid = parseWindowsNetstatPid(sourceInfo, output, currentPid);
        if (pid.isEmpty()) {
            return sourceInfo;
        }
        WindowsProcessInfo processInfo = resolveWindowsProcessInfo(pid.get());
        return sourceInfo.withProcess(pid.get(), processInfo.name(), processInfo.path());
    }

    static Optional<ProcessMatch> parseLsofOutput(CaptureSourceInfo sourceInfo, String output, long currentPid) {
        if (sourceInfo == null || output == null || output.isBlank()) {
            return Optional.empty();
        }
        List<ProcessMatch> records = new ArrayList<>();
        String pid = "";
        String command = "";
        String network = "";
        for (String rawLine : output.split("\\R")) {
            if (rawLine.isBlank()) {
                continue;
            }
            char prefix = rawLine.charAt(0);
            String value = rawLine.length() > 1 ? rawLine.substring(1).trim() : "";
            if (prefix == 'p') {
                if (!pid.isBlank()) {
                    records.add(new ProcessMatch(pid, command, network));
                }
                pid = value;
                command = "";
                network = "";
            } else if (prefix == 'c') {
                command = value;
            } else if (prefix == 'n') {
                network = value;
            }
        }
        if (!pid.isBlank()) {
            records.add(new ProcessMatch(pid, command, network));
        }
        String expected = sourceInfo.clientEndpoint() + "->" + sourceInfo.proxyEndpoint();
        for (ProcessMatch record : records) {
            if (String.valueOf(currentPid).equals(record.processId())) {
                continue;
            }
            if (record.network().contains(expected)) {
                return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    static Optional<String> parseWindowsNetstatPid(CaptureSourceInfo sourceInfo, String output, long currentPid) {
        if (sourceInfo == null || output == null || output.isBlank()) {
            return Optional.empty();
        }
        String local = sourceInfo.clientEndpoint();
        String foreign = sourceInfo.proxyEndpoint();
        for (String rawLine : output.split("\\R")) {
            String line = rawLine.trim();
            if (!line.toUpperCase(Locale.ROOT).startsWith("TCP")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 5) {
                continue;
            }
            String pid = parts[4];
            if (String.valueOf(currentPid).equals(pid)) {
                continue;
            }
            if (local.equals(parts[1]) && foreign.equals(parts[2])) {
                return Optional.of(pid);
            }
        }
        return Optional.empty();
    }

    private String resolveUnixProcessPath(String pid) {
        try {
            return runCommand(List.of("ps", "-p", pid, "-o", "comm=")).trim();
        } catch (Exception ex) {
            log.debug("Failed to resolve Unix process path for pid {}", pid, ex);
            return "";
        }
    }

    private WindowsProcessInfo resolveWindowsProcessInfo(String pid) {
        try {
            String command = "$p=Get-CimInstance Win32_Process -Filter \"ProcessId=" + pid + "\";"
                    + "if ($p) { Write-Output ($p.Name + \"`n\" + $p.ExecutablePath) }";
            String output = runCommand(List.of("powershell", "-NoProfile", "-Command", command));
            String[] lines = output.split("\\R", 2);
            String name = lines.length > 0 ? lines[0].trim() : "";
            String path = lines.length > 1 ? lines[1].trim() : "";
            return new WindowsProcessInfo(name, path);
        } catch (Exception ex) {
            log.debug("Failed to resolve Windows process info for pid {}", pid, ex);
            return new WindowsProcessInfo("", "");
        }
    }

    private static String runCommand(List<String> command) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        boolean finished = process.waitFor(COMMAND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        }
    }

    record ProcessMatch(String processId, String processName, String network) {
    }

    private record WindowsProcessInfo(String name, String path) {
    }
}
