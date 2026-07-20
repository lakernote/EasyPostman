package com.laker.postman.collection.cli;

import com.laker.postman.startup.HeadlessStartupBootstrap;
import com.laker.postman.util.JsonUtil;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CollectionRunCliCommand {
    private final RuntimeBootstrap runtimeBootstrap;
    private final CollectionRunExecutor executor;

    public CollectionRunCliCommand() {
        this(HeadlessStartupBootstrap::initRuntime, new CollectionRunExecutor());
    }

    CollectionRunCliCommand(RuntimeBootstrap runtimeBootstrap, CollectionRunExecutor executor) {
        this.runtimeBootstrap = runtimeBootstrap == null ? () -> {
        } : runtimeBootstrap;
        this.executor = executor == null ? new CollectionRunExecutor() : executor;
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        try {
            CollectionRunCliOptions options = CollectionRunCliOptions.parse(args);
            if (options.isHelp()) {
                printUsage(out);
                return 0;
            }
            if (options.getCollectionPath() == null) {
                throw new IllegalArgumentException("Collection file is required");
            }

            runtimeBootstrap.init();
            CollectionRunReport report = executor.execute(options, out);
            if (options.getOutPath() != null) {
                saveReport(options.getOutPath(), report);
            }
            out.printf(
                    "Collection run completed: status=%s iterations=%d total=%d passed=%d failed=%d tests=%d/%d elapsedMs=%d%n",
                    report.status(),
                    report.iterations(),
                    report.totalRequests(),
                    report.passedRequests(),
                    report.failedRequests(),
                    report.passedTests(),
                    report.totalTests(),
                    report.elapsedTimeMs()
            );
            return report.isSuccess() ? 0 : 1;
        } catch (IllegalArgumentException ex) {
            err.println(ex.getMessage());
            printUsage(err);
            return 2;
        } catch (Exception ex) {
            err.println("Collection run failed: " + describe(ex));
            return 1;
        }
    }

    static void printUsage(PrintStream out) {
        out.println("Usage: collection run <collection.json> [options]");
        out.println("Options:");
        out.println("  -e, --environment <environment.json>");
        out.println("  -g, --globals <globals.json>");
        out.println("  -d, --iteration-data <data.csv|data.json>");
        out.println("  -n, --iteration-count <count>");
        out.println("      --folder <folder-name>       Repeat to select multiple folders");
        out.println("      --working-dir <directory>    Defaults to the collection directory");
        out.println("      --out <result.json>");
        out.println("      --bail                       Stop after the first failed request/test");
        out.println("  -h, --help                       Show this help");
    }

    private static void saveReport(Path path, CollectionRunReport report) throws Exception {
        Path normalized = path.toAbsolutePath().normalize();
        Path parent = normalized.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(
                normalized,
                JsonUtil.toJsonPrettyStr(report),
                StandardCharsets.UTF_8
        );
    }

    private static String describe(Throwable failure) {
        String message = failure == null ? null : failure.getMessage();
        return message == null || message.isBlank()
                ? failure == null ? "unknown error" : failure.getClass().getSimpleName()
                : message;
    }

    @FunctionalInterface
    interface RuntimeBootstrap {
        void init() throws Exception;
    }
}
