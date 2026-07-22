package com.laker.postman.workspace.cli;

import com.laker.postman.util.JsonUtil;
import lombok.experimental.UtilityClass;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@UtilityClass
public class WorkspaceRunCliSupport {

    public void saveReport(Path path, WorkspaceRunReport report) throws Exception {
        Path normalized = path.toAbsolutePath().normalize();
        Path parent = normalized.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(normalized, JsonUtil.toJsonPrettyStr(report), StandardCharsets.UTF_8);
    }

    public void printCompletion(PrintStream out, String commandName, WorkspaceRunReport report) {
        out.printf(
                "%s completed: status=%s iterations=%d total=%d passed=%d failed=%d tests=%d/%d elapsedMs=%d%n",
                commandName,
                report.status(),
                report.iterations(),
                report.totalRequests(),
                report.passedRequests(),
                report.failedRequests(),
                report.passedTests(),
                report.totalTests(),
                report.elapsedTimeMs()
        );
    }

    public String describe(Throwable failure) {
        String message = failure == null ? null : failure.getMessage();
        return message == null || message.isBlank()
                ? failure == null ? "unknown error" : failure.getClass().getSimpleName()
                : message;
    }
}
