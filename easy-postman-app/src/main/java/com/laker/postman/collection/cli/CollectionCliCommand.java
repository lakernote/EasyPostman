package com.laker.postman.collection.cli;

import java.io.PrintStream;

public class CollectionCliCommand {
    private final CollectionRunCliCommand runCommand;

    public CollectionCliCommand() {
        this(new CollectionRunCliCommand());
    }

    CollectionCliCommand(CollectionRunCliCommand runCommand) {
        this.runCommand = runCommand == null ? new CollectionRunCliCommand() : runCommand;
    }

    public static boolean matches(String[] args) {
        return args != null && args.length > 0 && "collection".equals(args[0]);
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        if (args == null || args.length < 2 || isHelp(args[1])) {
            CollectionRunCliCommand.printUsage(out);
            return 0;
        }
        if (!"run".equals(args[1])) {
            err.println("Unknown collection command: " + args[1]);
            CollectionRunCliCommand.printUsage(err);
            return 2;
        }
        return runCommand.run(args, out, err);
    }

    private static boolean isHelp(String arg) {
        return "--help".equals(arg) || "-h".equals(arg) || "help".equals(arg);
    }
}
