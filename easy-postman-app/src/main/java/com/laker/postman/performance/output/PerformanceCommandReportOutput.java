package com.laker.postman.performance.output;

import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportJsonStorage;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class PerformanceCommandReportOutput {
    private final Path path;
    private final PrintStream err;
    private final PerformanceJsonReportJsonStorage storage = new PerformanceJsonReportJsonStorage();
    private final AtomicBoolean progressWarningPrinted = new AtomicBoolean(false);

    public PerformanceCommandReportOutput(Path path, PrintStream err) {
        this.path = path;
        this.err = err;
    }

    public synchronized void write(PerformanceJsonReport report) throws IOException {
        if (path != null) {
            storage.save(path, report);
        }
    }

    public void writeProgress(PerformanceJsonReport report) {
        try {
            write(report);
        } catch (IOException | RuntimeException ex) {
            if (err != null && progressWarningPrinted.compareAndSet(false, true)) {
                err.println("Unable to update performance result JSON: " + describe(ex));
            }
        }
    }

    public void writeFailure(PerformanceJsonReport report, Throwable originalFailure) {
        try {
            write(report);
        } catch (IOException | RuntimeException writeFailure) {
            if (originalFailure != null) {
                originalFailure.addSuppressed(writeFailure);
            }
            if (err != null) {
                err.println("Unable to write failed performance result JSON: " + describe(writeFailure));
            }
        }
    }

    private static String describe(Throwable failure) {
        String message = failure == null ? null : failure.getMessage();
        return message == null || message.isBlank()
                ? failure == null ? "unknown error" : failure.getClass().getSimpleName()
                : message;
    }
}
