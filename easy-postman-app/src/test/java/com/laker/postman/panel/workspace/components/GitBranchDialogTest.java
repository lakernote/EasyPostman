package com.laker.postman.panel.workspace.components;

import com.laker.postman.model.GitBranchInfo;
import com.laker.postman.model.Workspace;
import org.eclipse.jgit.api.errors.NotMergedException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GitBranchDialogTest {

    @Test
    public void branchTableModelShouldExposeReadableBranchRows() {
        GitBranchDialog.BranchTableModel model = new GitBranchDialog.BranchTableModel();
        model.setBranches(List.of(
                new GitBranchInfo("main", "refs/heads/main", true, false, null, "origin/main"),
                new GitBranchInfo("origin/feature", "refs/remotes/origin/feature", false, true, "origin")
        ));

        assertEquals(model.getRowCount(), 2);
        assertEquals(model.getValueAt(0, 0), "main");
        assertEquals(model.getValueAt(0, 1), "origin/main");
        assertEquals(model.getValueAt(0, 2), "Current");
        assertEquals(model.getValueAt(1, 0), "origin/feature");
        assertEquals(model.getValueAt(1, 1), "Remote only");
        assertEquals(model.getValueAt(1, 2), "");
    }

    @Test
    public void branchTableModelShouldShowNoTrackingForUntrackedLocalBranch() {
        GitBranchDialog.BranchTableModel model = new GitBranchDialog.BranchTableModel();
        model.setBranches(List.of(
                new GitBranchInfo("scratch", "refs/heads/scratch", false, false, null)
        ));

        assertEquals(model.getValueAt(0, 0), "scratch");
        assertEquals(model.getValueAt(0, 1), "-");
        assertEquals(model.getValueAt(0, 2), "");
    }

    @Test
    public void branchTableModelShouldShowSyncStatus() {
        GitBranchInfo branch = new GitBranchInfo("main", "refs/heads/main", true, false, null, "origin/main");
        branch.setAheadCount(1);
        branch.setBehindCount(2);
        GitBranchDialog.BranchTableModel model = new GitBranchDialog.BranchTableModel();
        model.setBranches(List.of(branch));

        assertEquals(model.getValueAt(0, 2), "Current · ↑1 ↓2");
    }

    @Test
    public void switchAvailabilityMessageShouldExplainDisabledCurrentBranch() {
        GitBranchInfo currentBranch = new GitBranchInfo("main", "refs/heads/main", true, false, null);
        GitBranchInfo targetBranch = new GitBranchInfo("feature", "refs/heads/feature", false, false, null);

        assertEquals(GitBranchDialog.switchAvailabilityMessage(null, "Select", "Current", "Ready"), "Select");
        assertEquals(GitBranchDialog.switchAvailabilityMessage(currentBranch, "Select", "Current", "Ready"), "Current");
        assertEquals(GitBranchDialog.switchAvailabilityMessage(targetBranch, "Select", "Current", "Ready"), "Ready");
    }

    @Test
    public void publishAndFetchActionsShouldRequireRemoteRepository() {
        Workspace noRemoteWorkspace = new Workspace();
        Workspace remoteWorkspace = new Workspace();
        remoteWorkspace.setGitRemoteUrl("https://example.com/team/repo.git");
        GitBranchInfo currentUntrackedBranch = new GitBranchInfo("main", "refs/heads/main", true, false, null);
        GitBranchInfo currentTrackedBranch = new GitBranchInfo("main", "refs/heads/main", true, false, null, "origin/main");
        GitBranchInfo remoteBranch = new GitBranchInfo("origin/main", "refs/remotes/origin/main", false, true, "origin");
        GitBranchInfo targetBranch = new GitBranchInfo("feature", "refs/heads/feature", false, false, null);

        assertFalse(GitBranchDialog.canFetchBranches(noRemoteWorkspace));
        assertTrue(GitBranchDialog.canFetchBranches(remoteWorkspace));
        assertFalse(GitBranchDialog.canPublishBranch(noRemoteWorkspace, currentUntrackedBranch));
        assertTrue(GitBranchDialog.canPublishBranch(remoteWorkspace, currentUntrackedBranch));
        assertFalse(GitBranchDialog.canPublishBranch(remoteWorkspace, currentTrackedBranch));
        assertFalse(GitBranchDialog.canPublishBranch(remoteWorkspace, remoteBranch));
        assertFalse(GitBranchDialog.canPublishBranch(remoteWorkspace, targetBranch));
    }

    @Test
    public void toolbarButtonWidthShouldFitLocalizedTextAndIcon() {
        assertEquals(GitBranchDialog.toolbarButtonWidth(64, 16, 6), 138);
        assertEquals(GitBranchDialog.toolbarButtonWidth(20, 16, 6), 100);
    }

    @Test
    public void notMergedDeleteFailureShouldBeRecognizedThroughSwingWorkerWrapper() {
        Exception wrapped = new ExecutionException(new NotMergedException());

        assertTrue(GitBranchDialog.isBranchNotMergedFailure(wrapped));
        assertFalse(GitBranchDialog.isBranchNotMergedFailure(new RuntimeException("other failure")));
    }

    @Test
    public void branchManagementActionsShouldNotCrowdDialogFooter() throws IOException {
        String source = Files.readString(gitBranchDialogSource());
        String footer = methodBody(source, "private JPanel createFooter()", "\n    private void loadBranches");

        assertFalse(footer.contains("GIT_BRANCH_FETCH"));
        assertFalse(footer.contains("GIT_BRANCH_CREATE"));
        assertFalse(footer.contains("GIT_BRANCH_PUBLISH"));
        assertFalse(footer.contains("GIT_BRANCH_DELETE"));
        assertTrue(footer.contains("GIT_BRANCH_CLOSE"));
        assertTrue(footer.contains("GIT_BRANCH_SWITCH"));
    }

    @Test
    public void branchToolbarShouldHaveBreathingRoomBelowHeader() throws IOException {
        String source = Files.readString(gitBranchDialogSource());
        String contentPanel = methodBody(source, "private JPanel createContentPanel()", "\n    private JScrollPane createTableScrollPane");
        String toolbar = methodBody(source, "private JPanel createBranchActionToolbar()", "\n    private static void fitToolbarButton");

        assertTrue(contentPanel.contains("new EmptyBorder(10, 18, 12, 18)"));
        assertTrue(toolbar.contains("new EmptyBorder(0, 0, 6, 0)"));
    }

    @Test
    public void publishSuccessShouldRefreshWorkspaceDetailAfterDialogCloses() throws IOException {
        String source = Files.readString(gitBranchDialogSource());
        String publish = methodBody(source, "private void publishSelectedBranch()", "\n    private void updateActionButtonState");

        assertTrue(publish.contains("needRefresh = true;"));
    }

    private static String methodBody(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());
        assertTrue(start >= 0, "Missing method start: " + startMarker);
        assertTrue(end > start, "Missing method end: " + endMarker);
        return source.substring(start, end);
    }

    private static Path gitBranchDialogSource() {
        return repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/components/GitBranchDialog.java");
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
