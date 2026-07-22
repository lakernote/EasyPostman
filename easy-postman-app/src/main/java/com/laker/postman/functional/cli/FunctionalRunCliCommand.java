package com.laker.postman.functional.cli;

import com.laker.postman.startup.HeadlessStartupBootstrap;
import com.laker.postman.workspace.cli.WorkspaceRunCliSupport;
import com.laker.postman.workspace.cli.WorkspaceRunExecutor;
import com.laker.postman.workspace.cli.WorkspaceRunReport;

import java.io.PrintStream;

public class FunctionalRunCliCommand {
    private final RuntimeBootstrap runtimeBootstrap;
    private final WorkspaceRunExecutor executor;

    public FunctionalRunCliCommand() {
        this(HeadlessStartupBootstrap::initRuntime, new WorkspaceRunExecutor());
    }

    FunctionalRunCliCommand(RuntimeBootstrap runtimeBootstrap, WorkspaceRunExecutor executor) {
        this.runtimeBootstrap = runtimeBootstrap == null ? () -> {
        } : runtimeBootstrap;
        this.executor = executor == null ? new WorkspaceRunExecutor() : executor;
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        try {
            FunctionalRunCliOptions options = FunctionalRunCliOptions.parse(args);
            if (options.isHelp()) {
                printUsage(out);
                return 0;
            }

            runtimeBootstrap.init();
            WorkspaceRunReport report = executor.execute(
                    options.toRunOptions(),
                    new FunctionalRunPlanner(),
                    out
            );
            if (options.getOutPath() != null) {
                WorkspaceRunCliSupport.saveReport(options.getOutPath(), report);
            }
            WorkspaceRunCliSupport.printCompletion(out, "Functional run", report);
            return report.isSuccess() ? 0 : 1;
        } catch (IllegalArgumentException ex) {
            err.println(ex.getMessage());
            printUsage(err);
            return 2;
        } catch (Exception ex) {
            err.println("Functional run failed: " + WorkspaceRunCliSupport.describe(ex));
            return 1;
        }
    }

    static void printUsage(PrintStream out) {
        out.println("Usage: functional run [workspace-name|workspace-directory] [options]");
        out.println("Runs selected requests and embedded CSV rows from functional_config.json.");
        out.println("Uses the current directory when it contains collections.json;");
        out.println("otherwise uses the desktop current workspace.");
        out.println("Options:");
        out.println("  -w, --workspace <name|directory> Select a registered workspace or directory");
        out.println("  -e, --environment <name>         Defaults to the workspace's active environment");
        out.println("  -d, --iteration-data <data.csv|data.json>  Override embedded CSV rows");
        out.println("  -n, --iteration-count <count>");
        out.println("      --working-dir <directory>    Defaults to the workspace directory");
        out.println("      --out <result.json>");
        out.println("      --bail                       Stop after the first failed request/test");
        out.println("  -h, --help                       Show this help");
        out.println("Relative iteration-data and upload paths resolve from the workspace directory.");
    }

    @FunctionalInterface
    interface RuntimeBootstrap {
        void init() throws Exception;
    }
}
