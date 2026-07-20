package com.laker.postman.startup;

import com.laker.postman.collection.cli.CollectionCliCommand;
import com.laker.postman.performance.cli.PerformanceCliCommand;

import java.io.PrintStream;
import java.util.OptionalInt;

public class AppCommandRouter {
    private final PerformanceCliCommand performanceCliCommand;
    private final CollectionCliCommand collectionCliCommand;

    public AppCommandRouter() {
        this(new PerformanceCliCommand(), new CollectionCliCommand());
    }

    AppCommandRouter(PerformanceCliCommand performanceCliCommand) {
        this(performanceCliCommand, new CollectionCliCommand());
    }

    AppCommandRouter(PerformanceCliCommand performanceCliCommand,
                     CollectionCliCommand collectionCliCommand) {
        this.performanceCliCommand = performanceCliCommand == null
                ? new PerformanceCliCommand()
                : performanceCliCommand;
        this.collectionCliCommand = collectionCliCommand == null
                ? new CollectionCliCommand()
                : collectionCliCommand;
    }

    public OptionalInt route(String[] args, PrintStream out, PrintStream err) {
        if (PerformanceCliCommand.matches(args)) {
            System.setProperty("java.awt.headless", "true");
            return OptionalInt.of(performanceCliCommand.run(args, out, err));
        }
        if (CollectionCliCommand.matches(args)) {
            System.setProperty("java.awt.headless", "true");
            return OptionalInt.of(collectionCliCommand.run(args, out, err));
        }
        return OptionalInt.empty();
    }
}
