package com.laker.postman.performance.cli;

import com.laker.postman.performance.master.PerformanceMasterRunCommand;
import com.laker.postman.performance.worker.PerformanceWorkerCommand;

import java.io.PrintStream;

public class PerformanceCliCommand {
    private final PerformanceRunCliCommand runCommand;
    private final PerformanceWorkerCommand workerCommand;
    private final PerformanceMasterRunCommand masterRunCommand;

    public PerformanceCliCommand() {
        this(new PerformanceRunCliCommand(), new PerformanceWorkerCommand(), new PerformanceMasterRunCommand());
    }

    PerformanceCliCommand(PerformanceRunCliCommand runCommand,
                          PerformanceWorkerCommand workerCommand,
                          PerformanceMasterRunCommand masterRunCommand) {
        this.runCommand = runCommand == null ? new PerformanceRunCliCommand() : runCommand;
        this.workerCommand = workerCommand == null ? new PerformanceWorkerCommand() : workerCommand;
        this.masterRunCommand = masterRunCommand == null ? new PerformanceMasterRunCommand() : masterRunCommand;
    }

    public static boolean matches(String[] args) {
        return args != null && args.length > 0 && "performance".equals(args[0]);
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        if (args == null || args.length < 2 || isHelp(args[1])) {
            printUsage(out);
            return 0;
        }
        return switch (args[1]) {
            case "run" -> runCommand.run(args, out, err);
            case "worker" -> workerCommand.run(args, out, err);
            case "master" -> runMaster(args, out, err);
            default -> {
                err.println("Unknown performance command: " + args[1]);
                printUsage(err);
                yield 2;
            }
        };
    }

    private int runMaster(String[] args, PrintStream out, PrintStream err) {
        if (args.length < 3 || isHelp(args[2])) {
            printMasterUsage(out);
            return 0;
        }
        if (!"run".equals(args[2])) {
            err.println("Unknown performance master command: " + args[2]);
            printMasterUsage(err);
            return 2;
        }
        return masterRunCommand.run(args, out, err);
    }

    private static boolean isHelp(String arg) {
        return "--help".equals(arg) || "-h".equals(arg) || "help".equals(arg);
    }

    private static void printUsage(PrintStream out) {
        out.println("Usage:");
        out.println("  performance run --plan <plan.json> [--out <result.json>]");
        out.println("  performance worker [--host <host>] [--port <port>]");
        out.println("  performance master run --plan <plan.json> --workers host:port[,host:port] [--out <result.json>] [--timeout-sec <seconds>] [--poll-interval-ms <ms>]");
    }

    private static void printMasterUsage(PrintStream out) {
        out.println("Usage: performance master run --plan <plan.json> --workers host:port[,host:port] [--out <result.json>] [--timeout-sec <seconds>] [--poll-interval-ms <ms>]");
    }
}
