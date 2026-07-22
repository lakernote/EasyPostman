package com.laker.postman.workspace.cli;

import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.model.Workspace;
import com.laker.postman.util.WorkspaceStorageUtil;
import lombok.experimental.UtilityClass;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

@UtilityClass
class WorkspaceRunWorkspaceResolver {

    WorkspaceRunWorkspace resolve(String selector) {
        if (selector == null || selector.isBlank()) {
            Path currentDirectory = Path.of("").toAbsolutePath().normalize();
            if (Files.isRegularFile(currentDirectory.resolve("collections.json"))) {
                return fromDirectory(currentDirectory);
            }
            return resolveCurrent(WorkspaceStorageUtil.loadWorkspaces());
        }

        Path direct = toPath(selector);
        if (Files.isDirectory(direct)) {
            return fromDirectory(direct);
        }
        if (Files.isRegularFile(direct)) {
            throw new IllegalArgumentException(
                    "EasyPostman CLI expects a workspace directory, not a collection file: " + direct
            );
        }

        List<Workspace> registered = WorkspaceStorageUtil.loadWorkspaces();
        Workspace matched = registered.stream()
                .filter(workspace -> selector.equals(workspace.getName()) || selector.equals(workspace.getId()))
                .findFirst()
                .orElse(null);
        if (matched != null) {
            return fromRegistered(matched);
        }

        Path managed = Path.of(ConfigPathConstants.WORKSPACES_DIR).resolve(selector).toAbsolutePath().normalize();
        if (Files.isDirectory(managed)) {
            return fromDirectory(managed);
        }

        String available = registered.stream()
                .map(Workspace::getName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("<none>");
        throw new IllegalArgumentException(
                "EasyPostman workspace not found: " + selector + ". Available workspaces: " + available
        );
    }

    private WorkspaceRunWorkspace resolveCurrent(List<Workspace> registered) {
        String currentId = WorkspaceStorageUtil.getCurrentWorkspace();
        Workspace current = registered.stream()
                .filter(workspace -> currentId != null && currentId.equals(workspace.getId()))
                .findFirst()
                .orElseGet(() -> registered.stream()
                        .filter(WorkspaceStorageUtil::isDefaultWorkspace)
                        .findFirst()
                        .orElse(null));
        if (current == null) {
            throw new IllegalArgumentException(
                    "No current EasyPostman workspace is configured; specify a workspace directory"
            );
        }
        return fromRegistered(current);
    }

    private WorkspaceRunWorkspace fromRegistered(Workspace workspace) {
        if (workspace.getPath() == null || workspace.getPath().isBlank()) {
            throw new IllegalArgumentException("Workspace has no local path: " + workspace.getName());
        }
        Path directory = toPath(workspace.getPath());
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException(
                    "Workspace directory does not exist: " + workspace.getName() + " (" + directory + ")"
            );
        }
        return new WorkspaceRunWorkspace(workspace.getName(), directory);
    }

    private WorkspaceRunWorkspace fromDirectory(Path directory) {
        Path normalized = directory.toAbsolutePath().normalize();
        Path fileName = normalized.getFileName();
        return new WorkspaceRunWorkspace(
                fileName == null ? normalized.toString() : fileName.toString(),
                normalized
        );
    }

    private Path toPath(String value) {
        try {
            return Path.of(value).toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            throw new IllegalArgumentException("Invalid workspace path: " + value, ex);
        }
    }
}
