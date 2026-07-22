package com.laker.postman.collection.cli;

import com.laker.postman.startup.HeadlessStartupBootstrap;
import com.laker.postman.workspace.cli.WorkspaceRunCliSupport;
import com.laker.postman.workspace.cli.WorkspaceRunExecutor;
import com.laker.postman.workspace.cli.WorkspaceRunReport;

import java.io.PrintStream;

public class CollectionRunCliCommand {
    private final RuntimeBootstrap runtimeBootstrap;
    private final WorkspaceRunExecutor executor;

    public CollectionRunCliCommand() {
        this(HeadlessStartupBootstrap::initRuntime, new WorkspaceRunExecutor());
    }

    CollectionRunCliCommand(RuntimeBootstrap runtimeBootstrap, WorkspaceRunExecutor executor) {
        this.runtimeBootstrap = runtimeBootstrap == null ? () -> {
        } : runtimeBootstrap;
        this.executor = executor == null ? new WorkspaceRunExecutor() : executor;
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        try {
            CollectionRunCliOptions options = CollectionRunCliOptions.parse(args);
            if (options.isHelp()) {
                printUsage(out);
                return 0;
            }

            runtimeBootstrap.init();
            WorkspaceRunReport report = executor.execute(
                    options.toRunOptions(),
                    new CollectionRunPlanner(options.getCollections(), options.getFolders()),
                    out
            );
            if (options.getOutPath() != null) {
                WorkspaceRunCliSupport.saveReport(options.getOutPath(), report);
            }
            WorkspaceRunCliSupport.printCompletion(out, "Collection run", report);
            return report.isSuccess() ? 0 : 1;
        } catch (IllegalArgumentException ex) {
            err.println(ex.getMessage());
            printUsage(err);
            return 2;
        } catch (Exception ex) {
            err.println("Collection run failed: " + WorkspaceRunCliSupport.describe(ex));
            return 1;
        }
    }

    static void printUsage(PrintStream out) {
        out.println("Usage: collection run <workspace-directory> [options]");
        out.println("Pass the EasyPostman workspace directory; use . at a checked-out Git workspace root.");
        out.println("Options:");
        out.println("  -w, --workspace <directory>      Alternative to the positional workspace directory");
        out.println("  -c, --collection <name>          Repeat to select collections; defaults to all");
        out.println("  -e, --environment <name>         Defaults to the workspace's active environment");
        out.println("  -d, --iteration-data <data.csv|data.json>");
        out.println("  -n, --iteration-count <count>");
        out.println("      --folder <folder-name>       Repeat to select multiple folders");
        out.println("      --working-dir <directory>    Defaults to the workspace directory");
        out.println("      --out <result.json>");
        out.println("      --bail                       Stop after the first failed request/test");
        out.println("  -h, --help                       Show this help");
        out.println("Collections, environments, and globals use EasyPostman's native workspace data.");
        out.println("Relative iteration-data and upload paths resolve from the workspace directory.");
    }

    @FunctionalInterface
    interface RuntimeBootstrap {
        void init() throws Exception;
    }
}
