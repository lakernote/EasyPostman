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
    public void defaultDetailDividerShouldPreserveToolAreaWhenWindowIsShort() {
        assertEquals(WorkspacePanel.defaultWorkspaceDetailDividerLocation(
                520,
                AppToolWindowChrome.DIVIDER_SIZE
        ), 295);
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
    public void gitWorkspaceContextMenuShouldNotDuplicateEmbeddedBranchManagement() throws IOException {
        Path sourcePath = repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java");
        String source = Files.readString(sourcePath);
        String method = source.substring(
                source.indexOf("private void addStandardGitMenuItems"),
                source.indexOf("\n    private void addManagementMenuItems", source.indexOf("private void addStandardGitMenuItems"))
        );

        assertFalse(method.contains("MessageKeys.WORKSPACE_GIT_BRANCHES"));
        assertFalse(method.contains("showGitBranches(workspace)"));
    }

    @Test
    public void gitWorkspaceContextMenuShouldNotDuplicateEmbeddedDiffManagement() throws IOException {
        Path sourcePath = repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java");
        String source = Files.readString(sourcePath);
        String method = source.substring(
                source.indexOf("private void addStandardGitMenuItems"),
                source.indexOf("\n    private void addManagementMenuItems", source.indexOf("private void addStandardGitMenuItems"))
        );

        assertFalse(method.contains("MessageKeys.WORKSPACE_GIT_DIFF"));
        assertFalse(method.contains("showGitDiff(workspace)"));
    }

    @Test
    public void workspaceDetailShouldExposeGitManagementActions() throws IOException {
        Path sourcePath = repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java");
        String source = Files.readString(sourcePath);
        String method = source.substring(
                source.indexOf("private void updateInfoPanel"),
                source.indexOf("\n    private void showError", source.indexOf("private void updateInfoPanel") + 1)
        );

        assertTrue(method.contains("new WorkspaceDetailPanel("));
        assertTrue(method.contains("() -> showGitBranches(selected)"));
        assertTrue(method.contains("() -> showGitDiff(selected)"));
    }

    @Test
    public void workspacePanelShouldEmbedGitDiffPanel() throws IOException {
        Path sourcePath = repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java");
        String source = Files.readString(sourcePath);
        String method = source.substring(
                source.indexOf("private void showGitDiff"),
                source.indexOf("\n    /**", source.indexOf("private void showGitDiff") + 1)
        );

        assertTrue(method.contains("saveCurrentWorkspaceScopedPanels();"));
        assertTrue(method.contains("new GitDiffPanel("));
        assertFalse(method.contains("new GitDiffDialog("));
    }

    @Test
    public void workspacePanelShouldEmbedGitBranchPanel() throws IOException {
        Path sourcePath = repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java");
        String source = Files.readString(sourcePath);
        String method = source.substring(
                source.indexOf("private void showGitBranches"),
                source.indexOf("\n    private void showGitDiff", source.indexOf("private void showGitBranches") + 1)
        );

        assertTrue(method.contains("saveCurrentWorkspaceScopedPanels();"));
        assertTrue(method.contains("new GitBranchPanel("));
        assertFalse(method.contains("new GitBranchDialog("));
    }

    @Test
    public void obsoleteGitManagementDialogsShouldBeRemoved() {
        Path componentDir = repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/components");

        assertFalse(Files.exists(componentDir.resolve("GitDiffDialog.java")));
        assertFalse(Files.exists(componentDir.resolve("GitBranchDialog.java")));
    }

    @Test
    public void workspacePanelShouldKeepEmbeddedGitToolWhenSameWorkspaceRefreshes() throws IOException {
        Path sourcePath = repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java");
        String source = Files.readString(sourcePath);
        String method = source.substring(
                source.indexOf("private void updateInfoPanel"),
                source.indexOf("\n    private void showError", source.indexOf("private void updateInfoPanel") + 1)
        );

        assertTrue(source.contains("displayedWorkspaceId"));
        assertTrue(method.contains("Objects.equals"));
        assertTrue(method.contains("if (workspaceChanged)"));
        assertTrue(method.contains("showDefaultWorkspaceTool(selected);"));
    }

    @Test
    public void workspacePanelShouldDefaultGitWorkspacesToDiffPanelInsteadOfLog() throws IOException {
        Path sourcePath = repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java");
        String source = Files.readString(sourcePath);
        String method = source.substring(
                source.indexOf("private void showDefaultWorkspaceTool"),
                source.indexOf("\n    private void showWorkspaceTool", source.indexOf("private void showDefaultWorkspaceTool") + 1)
        );

        assertTrue(method.contains("WorkspaceType.GIT"));
        assertTrue(method.contains("showGitDiff(workspace, false);"));
        assertTrue(method.contains("hideWorkspaceTool();"));
        assertFalse(source.contains("createEmptyWorkspaceToolPanel"));
        assertFalse(source.contains("logArea"));
        assertFalse(source.contains("createWorkspaceLogPanel"));
    }

    @Test
    public void workspacePanelShouldCollapseToolAreaWhenNoEmbeddedToolIsShown() throws IOException {
        Path sourcePath = repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java");
        String source = Files.readString(sourcePath);
        String showTool = source.substring(
                source.indexOf("private void showWorkspaceTool"),
                source.indexOf("\n    private void hideWorkspaceTool", source.indexOf("private void showWorkspaceTool") + 1)
        );
        String visibility = source.substring(
                source.indexOf("private void setWorkspaceToolVisible"),
                source.indexOf("\n    private static void installInitialWorkspaceDetailDivider",
                        source.indexOf("private void setWorkspaceToolVisible") + 1)
        );

        assertTrue(showTool.contains("setWorkspaceToolVisible(true);"));
        assertTrue(visibility.contains("workspaceToolPanel.setVisible(visible);"));
        assertTrue(visibility.contains("workspaceContentSplitPane.setDividerSize(visible ? AppToolWindowChrome.DIVIDER_SIZE : 0);"));
        assertTrue(visibility.contains("workspaceContentSplitPane.setResizeWeight(visible ? WORKSPACE_DETAIL_RESIZE_WEIGHT : 1.0);"));
    }

    @Test
    public void embeddedGitPanelsShouldUseWorkspaceSurfaceInsteadOfDialogSurface() throws IOException {
        Path diffPanelPath = repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/components/GitDiffPanel.java");
        Path branchPanelPath = repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/components/GitBranchPanel.java");
        String diffPanel = Files.readString(diffPanelPath);
        String branchPanel = Files.readString(branchPanelPath);

        assertFalse(diffPanel.contains("applyDialogHeader"));
        assertFalse(diffPanel.contains("applyDialogSurface"));
        assertFalse(diffPanel.contains("applyDialogFrame"));
        assertFalse(branchPanel.contains("applyDialogHeader"));
        assertFalse(branchPanel.contains("applyDialogSurface"));
        assertFalse(branchPanel.contains("applyDialogFrame"));
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
