package com.laker.postman.workspace.cli;

import java.nio.file.Path;

public record WorkspaceRunWorkspace(String name, Path directory) {

    public Path collectionsFile() {
        return directory.resolve("collections.json");
    }

    public Path environmentsFile() {
        return directory.resolve("environments.json");
    }
}
