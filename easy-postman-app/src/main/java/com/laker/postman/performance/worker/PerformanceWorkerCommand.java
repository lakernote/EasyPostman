package com.laker.postman.performance.worker;

import com.laker.postman.startup.HeadlessStartupBootstrap;

import java.io.PrintStream;

public class PerformanceWorkerCommand {

    public int run(String[] args, PrintStream out, PrintStream err) {
        try {
            PerformanceWorkerOptions options = PerformanceWorkerCommandLine.parse(args);
            if (options.isHelp()) {
                printUsage(out);
                return 0;
            }
            HeadlessStartupBootstrap.initRuntime();
            try (PerformanceWorkerServer server = new PerformanceWorkerServer(options)) {
                server.start();
                out.printf("Performance worker listening on %s:%d%n", options.getHost(), server.getPort());
                server.awaitShutdown();
            }
            return 0;
        } catch (IllegalArgumentException ex) {
            err.println(ex.getMessage());
            printUsage(err);
            return 2;
        } catch (Exception ex) {
            err.println("Performance worker failed: " + ex.getMessage());
            return 1;
        }
    }

    private static void printUsage(PrintStream out) {
        out.println("Usage: performance worker [--host <host>] [--port <port>]");
    }
}
