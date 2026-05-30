package com.laker.postman.startup;

import com.laker.postman.performance.cli.PerformanceCliCommand;

import java.io.PrintStream;
import java.util.OptionalInt;

public class AppCommandRouter {
    private final PerformanceCliCommand performanceCliCommand;

    public AppCommandRouter() {
        this(new PerformanceCliCommand());
    }

    AppCommandRouter(PerformanceCliCommand performanceCliCommand) {
        this.performanceCliCommand = performanceCliCommand == null
                ? new PerformanceCliCommand()
                : performanceCliCommand;
    }

    public OptionalInt route(String[] args, PrintStream out, PrintStream err) {
        if (!PerformanceCliCommand.matches(args)) {
            return OptionalInt.empty();
        }
        System.setProperty("java.awt.headless", "true");
        return OptionalInt.of(performanceCliCommand.run(args, out, err));
    }
}
