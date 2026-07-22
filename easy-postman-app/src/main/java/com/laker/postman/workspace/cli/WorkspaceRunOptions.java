package com.laker.postman.workspace.cli;

import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;

@Value
@Builder
public class WorkspaceRunOptions {
    String workspace;
    String environment;
    Path iterationDataPath;
    Integer iterationCount;
    Path workingDirectory;
    boolean bail;

    public WorkspaceRunOptions(String workspace,
                               String environment,
                               Path iterationDataPath,
                               Integer iterationCount,
                               Path workingDirectory,
                               Boolean bail) {
        this.workspace = workspace;
        this.environment = environment;
        this.iterationDataPath = iterationDataPath;
        this.iterationCount = iterationCount;
        this.workingDirectory = workingDirectory;
        this.bail = bail != null && bail;
    }
}
