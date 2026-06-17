package com.laker.postman.panel.workspace;

import com.laker.postman.common.component.AppToolWindowChrome;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class WorkspacePanelTest {

    @Test
    public void defaultDetailDividerShouldNotOverstretchDetailAreaInTallWindows() {
        assertEquals(WorkspacePanel.defaultWorkspaceDetailDividerLocation(
                1400,
                AppToolWindowChrome.DIVIDER_SIZE
        ), 400);
    }

    @Test
    public void defaultDetailDividerShouldGiveDetailAreaReadableSpaceInLargeWindows() {
        assertEquals(WorkspacePanel.defaultWorkspaceDetailDividerLocation(
                1000,
                AppToolWindowChrome.DIVIDER_SIZE
        ), 360);
    }

    @Test
    public void defaultDetailDividerShouldKeepDetailAreaReadableInMediumWindows() {
        assertEquals(WorkspacePanel.defaultWorkspaceDetailDividerLocation(
                700,
                AppToolWindowChrome.DIVIDER_SIZE
        ), 360);
    }

    @Test
    public void defaultDetailDividerShouldPreserveLogAreaWhenWindowIsShort() {
        assertEquals(WorkspacePanel.defaultWorkspaceDetailDividerLocation(
                520,
                AppToolWindowChrome.DIVIDER_SIZE
        ), 335);
    }

    @Test
    public void workspacePanelShouldNotConfigureRemoteRepositoryAfterDialogAlreadyConfiguredIt() throws IOException {
        Path sourcePath = repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java");
        String source = Files.readString(sourcePath);
        String method = source.substring(
                source.indexOf("private void configureRemoteRepository"),
                source.indexOf("\n    /**", source.indexOf("private void configureRemoteRepository") + 1)
        );

        assertFalse(method.contains("workspaceService.addRemoteRepository("),
                "RemoteConfigDialog performs the remote configuration; WorkspacePanel should only refresh after confirmation.");
        assertTrue(method.contains("refreshWorkspaceList();"));
    }

    @Test
    public void gitWorkspaceContextMenuShouldExposeBranchManagement() throws IOException {
        Path sourcePath = repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java");
        String source = Files.readString(sourcePath);
        String method = source.substring(
                source.indexOf("private void addStandardGitMenuItems"),
                source.indexOf("\n    private void addManagementMenuItems", source.indexOf("private void addStandardGitMenuItems"))
        );

        assertTrue(method.contains("MessageKeys.WORKSPACE_GIT_BRANCHES"));
        assertTrue(method.contains("showGitBranches(workspace)"));
    }

    @Test
    public void gitWorkspaceContextMenuShouldExposeDiffManagement() throws IOException {
        Path sourcePath = repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java");
        String source = Files.readString(sourcePath);
        String method = source.substring(
                source.indexOf("private void addStandardGitMenuItems"),
                source.indexOf("\n    private void addManagementMenuItems", source.indexOf("private void addStandardGitMenuItems"))
        );

        assertTrue(method.contains("MessageKeys.WORKSPACE_GIT_DIFF"));
        assertTrue(method.contains("showGitDiff(workspace)"));
    }

    @Test
    public void workspaceDetailShouldExposeGitManagementActions() throws IOException {
        Path sourcePath = repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java");
        String source = Files.readString(sourcePath);
        String method = source.substring(
                source.indexOf("private void updateInfoPanel"),
                source.indexOf("\n    /**", source.indexOf("private void updateInfoPanel") + 1)
        );

        assertTrue(method.contains("new WorkspaceDetailPanel("));
        assertTrue(method.contains("() -> showGitBranches(selected)"));
        assertTrue(method.contains("() -> showGitDiff(selected)"));
    }

    @Test
    public void workspacePanelShouldOpenGitDiffDialog() throws IOException {
        Path sourcePath = repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java");
        String source = Files.readString(sourcePath);
        String method = source.substring(
                source.indexOf("private void showGitDiff"),
                source.indexOf("\n    /**", source.indexOf("private void showGitDiff") + 1)
        );

        assertTrue(method.contains("saveCurrentWorkspaceScopedPanels();"));
        assertTrue(method.contains("new GitDiffDialog("));
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("easy-postman-app"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate repository root");
    }
}
