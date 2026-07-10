package com.laker.postman.util;

import com.laker.postman.model.Workspace;
import com.laker.postman.model.WorkspaceType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class WorkspaceStorageUtilTest {

    private String previousDataDir;
    private Path dataRoot;

    @BeforeMethod
    public void setUp() throws IOException {
        previousDataDir = System.getProperty("easyPostman.data.dir");
        dataRoot = Files.createTempDirectory("workspace-storage-data-root");
        System.setProperty("easyPostman.data.dir", dataRoot.toString());
        SystemUtil.resetForTests();
    }

    @AfterMethod
    public void tearDown() {
        if (previousDataDir == null) {
            System.clearProperty("easyPostman.data.dir");
        } else {
            System.setProperty("easyPostman.data.dir", previousDataDir);
        }
        SystemUtil.resetForTests();
    }

    @Test
    public void saveWorkspacesShouldPersistManagedWorkspacePathsRelativeToDataRoot() throws Exception {
        Workspace managedWorkspace = workspace("api", dataRoot.resolve("workspaces/api"));
        Workspace externalWorkspace = workspace("external", Files.createTempDirectory("external-workspace"));

        WorkspaceStorageUtil.saveWorkspaces(List.of(managedWorkspace, externalWorkspace));

        String json = Files.readString(dataRoot.resolve("workspaces.json"), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"path\": \"$EASY_POSTMAN_DATA$/workspaces/api/\""));
        assertTrue(json.contains(externalWorkspace.getPath()));
        assertEquals(managedWorkspace.getPath(), dataRoot.resolve("workspaces/api").toString() + File.separator);
    }

    @Test
    public void loadWorkspacesShouldResolveDataRootTokenAndPlainRelativePaths() throws Exception {
        writeWorkspacesJson("""
                [
                  {"id":"api","name":"API","type":"LOCAL","path":"$EASY_POSTMAN_DATA$/workspaces/api/"},
                  {"id":"manual","name":"Manual","type":"LOCAL","path":"workspaces/manual/"}
                ]
                """);

        List<Workspace> workspaces = WorkspaceStorageUtil.loadWorkspaces();

        assertEquals(pathOf(workspaces, "api"), dataRoot.resolve("workspaces/api").toString() + File.separator);
        assertEquals(pathOf(workspaces, "manual"), dataRoot.resolve("workspaces/manual").toString() + File.separator);
    }

    @Test
    public void loadWorkspacesShouldRebaseCopiedManagedWorkspacePathsToCurrentDataRoot() throws Exception {
        Path oldRoot = Files.createTempDirectory("workspace-storage-old-root").resolve("EasyPostman/app/data");
        Files.createDirectories(dataRoot.resolve("workspaces/default"));
        Files.createDirectories(dataRoot.resolve("workspaces/team"));
        writeWorkspacesJson("""
                [
                  {
                    "id":"default-workspace",
                    "name":"Default",
                    "type":"LOCAL",
                    "path":"%s"
                  },
                  {
                    "id":"team",
                    "name":"Team",
                    "type":"LOCAL",
                    "path":"%s"
                  }
                ]
                """.formatted(
                oldRoot.resolve("workspaces/default").toString(),
                oldRoot.resolve("workspaces/team").toString()
        ));

        List<Workspace> workspaces = WorkspaceStorageUtil.loadWorkspaces();

        assertEquals(pathOf(workspaces, "default-workspace"),
                dataRoot.resolve("workspaces/default").toString() + File.separator);
        assertEquals(pathOf(workspaces, "team"),
                dataRoot.resolve("workspaces/team").toString() + File.separator);
    }

    @Test
    public void loadWorkspacesShouldKeepExternalAbsolutePathsThatContainWorkspacesSegment() throws Exception {
        Path externalWorkspace = Files.createTempDirectory("external-projects")
                .resolve("workspaces/api");
        Files.createDirectories(externalWorkspace);
        Files.createDirectories(dataRoot.resolve("workspaces/api"));
        writeWorkspacesJson("""
                [
                  {
                    "id":"api",
                    "name":"API",
                    "type":"LOCAL",
                    "path":"%s"
                  }
                ]
                """.formatted(externalWorkspace.toString()));

        List<Workspace> workspaces = WorkspaceStorageUtil.loadWorkspaces();

        assertEquals(pathOf(workspaces, "api"), externalWorkspace.toString() + File.separator);
    }

    @Test
    public void loadWorkspacesShouldKeepExternalAppDataWorkspacesPaths() throws Exception {
        Path externalWorkspace = Files.createTempDirectory("external-projects")
                .resolve("project/app/data/workspaces/api");
        Files.createDirectories(externalWorkspace);
        Files.createDirectories(dataRoot.resolve("workspaces/api"));
        writeWorkspacesJson("""
                [
                  {
                    "id":"api",
                    "name":"API",
                    "type":"LOCAL",
                    "path":"%s"
                  }
                ]
                """.formatted(externalWorkspace.toString()));

        List<Workspace> workspaces = WorkspaceStorageUtil.loadWorkspaces();

        assertEquals(pathOf(workspaces, "api"), externalWorkspace.toString() + File.separator);
    }

    private static Workspace workspace(String id, Path path) {
        Workspace workspace = new Workspace();
        workspace.setId(id);
        workspace.setName(id);
        workspace.setType(WorkspaceType.LOCAL);
        workspace.setPath(path.toString());
        return workspace;
    }

    private void writeWorkspacesJson(String json) throws IOException {
        Files.writeString(dataRoot.resolve("workspaces.json"), json, StandardCharsets.UTF_8);
    }

    private static String pathOf(List<Workspace> workspaces, String id) {
        return workspaces.stream()
                .filter(workspace -> id.equals(workspace.getId()))
                .findFirst()
                .orElseThrow()
                .getPath();
    }
}
