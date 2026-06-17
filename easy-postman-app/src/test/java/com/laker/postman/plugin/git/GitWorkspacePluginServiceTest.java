package com.laker.postman.plugin.git;

import com.laker.postman.model.GitAuthType;
import com.laker.postman.model.GitBranchInfo;
import com.laker.postman.model.GitFileChange;
import com.laker.postman.model.GitOperationResult;
import com.laker.postman.model.GitRepoSource;
import com.laker.postman.model.Workspace;
import com.laker.postman.model.WorkspaceType;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.URIish;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GitWorkspacePluginServiceTest {

    private final GitWorkspacePluginService service = new GitWorkspacePluginService();
    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("easy-postman-git-service");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws IOException {
        if (tempDir == null || !Files.exists(tempDir)) {
            return;
        }
        try (var paths = Files.walk(tempDir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best-effort cleanup for temp test repositories.
                        }
                    });
        }
    }

    @Test
    public void pullUpdatesShouldPreserveNonConflictingUntrackedFiles() throws Exception {
        Path remote = createBareRemote();
        Path seed = seedRemote(remote);
        Path workspacePath = cloneRemote(remote, "main");
        Path scratch = workspacePath.resolve("scratch.txt");
        Files.writeString(scratch, "local scratch", StandardCharsets.UTF_8);

        try (Git git = Git.open(seed.toFile())) {
            writeAndCommit(git, seed.resolve("remote.txt"), "remote update", "Add remote file");
            git.push().setRemote("origin").add("refs/heads/main:refs/heads/main").call();
        }

        GitOperationResult result = service.pullUpdates(workspace(workspacePath, GitRepoSource.CLONED));

        assertTrue(result.success);
        assertTrue(Files.exists(scratch), "Plain pull must not delete non-conflicting untracked files");
        assertEquals(Files.readString(workspacePath.resolve("remote.txt")), "remote update");
    }

    @Test
    public void stashChangesShouldIncludeUntrackedFiles() throws Exception {
        Path workspacePath = initRepositoryWithInitialCommit("stash-workspace");
        Path tracked = workspacePath.resolve("README.md");
        Path untracked = workspacePath.resolve("local-only.txt");
        Files.writeString(tracked, "changed", StandardCharsets.UTF_8);
        Files.writeString(untracked, "untracked", StandardCharsets.UTF_8);

        GitOperationResult stashResult = service.stashChanges(workspace(workspacePath, GitRepoSource.INITIALIZED));

        assertTrue(stashResult.success);
        assertTrue(stashResult.affectedFiles.contains("local-only.txt"));
        assertFalse(Files.exists(untracked), "Stash should remove untracked files from the working tree");

        GitOperationResult popResult = service.popStashChanges(workspace(workspacePath, GitRepoSource.INITIALIZED));

        assertTrue(popResult.success);
        assertEquals(Files.readString(untracked), "untracked");
        assertEquals(Files.readString(tracked), "changed");
    }

    @Test
    public void addRemoteRepositoryShouldPersistSshAuthentication() throws Exception {
        Path workspacePath = initRepositoryWithInitialCommit("ssh-remote-workspace");
        Workspace workspace = workspace(workspacePath, GitRepoSource.INITIALIZED);

        service.addRemoteRepository(
                workspace,
                "git@example.com:team/repo.git",
                "main",
                GitAuthType.SSH_KEY,
                "",
                "",
                "",
                "/Users/test/.ssh/id_ed25519",
                "optional-passphrase"
        );

        assertEquals(workspace.getGitAuthType(), GitAuthType.SSH_KEY);
        assertEquals(workspace.getSshPrivateKeyPath(), "/Users/test/.ssh/id_ed25519");
        assertEquals(workspace.getSshPassphrase(), "optional-passphrase");
    }

    @Test
    public void forcePullUpdatesShouldResetFromConfiguredUpstreamBranch() throws Exception {
        Path remote = createBareRemote();
        Path seed = seedRemote(remote);
        Path workspacePath = cloneRemote(remote, "main");
        configureLocalBranchTracking(workspacePath, "feature", "origin", "main");

        try (Git git = Git.open(seed.toFile())) {
            writeAndCommit(git, seed.resolve("README.md"), "remote main update", "Update main");
            git.push().setRemote("origin").add("refs/heads/main:refs/heads/main").call();
        }

        GitOperationResult result = service.forcePullUpdates(workspace(workspacePath, GitRepoSource.CLONED));

        assertTrue(result.success);
        assertEquals(Files.readString(workspacePath.resolve("README.md")), "remote main update");
    }

    @Test
    public void forcePushChangesShouldPushCurrentBranchToConfiguredUpstreamBranch() throws Exception {
        Path remote = createBareRemote();
        seedRemote(remote);
        Path workspacePath = cloneRemote(remote, "main");
        configureLocalBranchTracking(workspacePath, "feature", "origin", "main");

        try (Git git = Git.open(workspacePath.toFile())) {
            writeAndCommit(git, workspacePath.resolve("README.md"), "local feature overwrite", "Overwrite main from feature");
        }

        GitOperationResult result = service.forcePushChanges(workspace(workspacePath, GitRepoSource.CLONED));

        assertTrue(result.success);
        Path verifyPath = cloneRemote(remote, "main", "verify-force-push");
        assertEquals(Files.readString(verifyPath.resolve("README.md")), "local feature overwrite");
    }

    @Test
    public void listBranchesShouldIncludeLocalAndRemoteBranches() throws Exception {
        Path remote = createBareRemote();
        Path seed = seedRemote(remote);
        createAndPushBranch(seed, "feature", "feature content");
        Path workspacePath = cloneRemote(remote, "main");

        List<GitBranchInfo> branches = service.listBranches(workspace(workspacePath, GitRepoSource.CLONED));

        assertTrue(branches.stream().anyMatch(branch -> branch.getName().equals("main")
                && !branch.isRemote()
                && branch.isCurrent()));
        assertTrue(branches.stream().anyMatch(branch -> branch.getName().equals("origin/feature")
                && branch.isRemote()
                && !branch.isCurrent()));
    }

    @Test
    public void listBranchesShouldFoldTrackedRemoteIntoLocalBranch() throws Exception {
        Path remote = createBareRemote();
        seedRemote(remote);
        Path workspacePath = cloneRemote(remote, "main");

        List<GitBranchInfo> branches = service.listBranches(workspace(workspacePath, GitRepoSource.CLONED));

        assertTrue(branches.stream().anyMatch(branch -> branch.getName().equals("main")
                && !branch.isRemote()
                && branch.isCurrent()
                && "origin/main".equals(branch.getTrackingBranch())));
        assertFalse(branches.stream().anyMatch(branch -> branch.getName().equals("origin/main")
                && branch.isRemote()));
    }

    @Test
    public void listBranchesShouldShowAheadAndBehindCounts() throws Exception {
        Path remote = createBareRemote();
        Path seed = seedRemote(remote);
        Path workspacePath = cloneRemote(remote, "main");
        Workspace workspace = workspace(workspacePath, GitRepoSource.CLONED);

        try (Git git = Git.open(workspacePath.toFile())) {
            writeAndCommit(git, workspacePath.resolve("local.txt"), "local", "Local change");
        }
        try (Git git = Git.open(seed.toFile())) {
            writeAndCommit(git, seed.resolve("remote.txt"), "remote", "Remote change");
            git.push().setRemote("origin").add("refs/heads/main:refs/heads/main").call();
        }
        service.fetchBranches(workspace);

        List<GitBranchInfo> branches = service.listBranches(workspace);

        assertTrue(branches.stream().anyMatch(branch -> branch.getName().equals("main")
                && branch.getAheadCount() == 1
                && branch.getBehindCount() == 1));
    }

    @Test
    public void publishBranchShouldPushCurrentBranchAndSetUpstream() throws Exception {
        Path remote = createBareRemote();
        seedRemote(remote);
        Path workspacePath = cloneRemote(remote, "main");
        Workspace workspace = workspace(workspacePath, GitRepoSource.CLONED);
        service.createBranch(workspace, "feature/new-api");
        try (Git git = Git.open(workspacePath.toFile())) {
            writeAndCommit(git, workspacePath.resolve("feature.txt"), "feature", "Feature change");
        }

        GitOperationResult result = service.publishBranch(workspace);
        List<GitBranchInfo> branches = service.listBranches(workspace);

        assertTrue(result.success);
        assertEquals(workspace.getCurrentBranch(), "feature/new-api");
        assertEquals(workspace.getRemoteBranch(), "origin/feature/new-api");
        assertTrue(branches.stream().anyMatch(branch -> branch.getName().equals("feature/new-api")
                && "origin/feature/new-api".equals(branch.getTrackingBranch())));
        Path verifyPath = cloneRemote(remote, "feature/new-api", "verify-publish");
        assertEquals(Files.readString(verifyPath.resolve("feature.txt")), "feature");
    }

    @Test
    public void publishBranchShouldKeepPublishSuccessWhenPostPublishFetchFails() throws IOException {
        String source = Files.readString(repositoryRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/plugin/git/GitWorkspacePluginService.java"));
        String method = source.substring(
                source.indexOf("public GitOperationResult publishBranch"),
                source.indexOf("\n    @Override\n    public GitOperationResult pullUpdates", source.indexOf("public GitOperationResult publishBranch"))
        );

        int metadataUpdate = method.indexOf("updateWorkspaceBranchMetadata(git, workspace);", method.indexOf("config.save();"));
        int fetch = method.indexOf("git.fetch()");
        int fetchWarning = method.indexOf("catch (Exception fetchException)", fetch);

        assertTrue(metadataUpdate > 0, "Published branch metadata should be updated immediately after upstream config is saved.");
        assertTrue(fetch > metadataUpdate, "Post-publish fetch should run after workspace metadata is already updated.");
        assertTrue(fetchWarning > fetch, "Post-publish fetch must be best-effort and must not fail the publish operation.");
    }

    @Test
    public void switchBranchShouldMapRemoteTrackingBranchToExistingLocalBranch() throws Exception {
        Path remote = createBareRemote();
        seedRemote(remote);
        Path workspacePath = cloneRemote(remote, "main");
        Workspace workspace = workspace(workspacePath, GitRepoSource.CLONED);

        GitOperationResult result = service.switchBranch(workspace, "origin/main");

        assertTrue(result.success);
        assertEquals(workspace.getCurrentBranch(), "main");
        assertEquals(workspace.getRemoteBranch(), "origin/main");
    }

    @Test
    public void switchBranchShouldCheckoutExistingLocalBranchAndUpdateWorkspaceMetadata() throws Exception {
        Path workspacePath = initRepositoryWithInitialCommit("switch-local-workspace");
        try (Git git = Git.open(workspacePath.toFile())) {
            git.branchCreate().setName("feature").call();
            git.checkout().setName("feature").call();
            writeAndCommit(git, workspacePath.resolve("README.md"), "feature local", "Feature change");
            git.checkout().setName("main").call();
        }
        Workspace workspace = workspace(workspacePath, GitRepoSource.INITIALIZED);

        GitOperationResult result = service.switchBranch(workspace, "feature");

        assertTrue(result.success);
        assertEquals(workspace.getCurrentBranch(), "feature");
        assertEquals(Files.readString(workspacePath.resolve("README.md")), "feature local");
    }

    @Test
    public void switchBranchShouldCreateTrackingLocalBranchFromRemoteBranch() throws Exception {
        Path remote = createBareRemote();
        Path seed = seedRemote(remote);
        createAndPushBranch(seed, "feature", "feature remote");
        Path workspacePath = cloneRemote(remote, "main");
        Workspace workspace = workspace(workspacePath, GitRepoSource.CLONED);

        GitOperationResult result = service.switchBranch(workspace, "origin/feature");

        assertTrue(result.success);
        assertEquals(workspace.getCurrentBranch(), "feature");
        assertEquals(workspace.getRemoteBranch(), "origin/feature");
        assertEquals(Files.readString(workspacePath.resolve("README.md")), "feature remote");
    }

    @Test
    public void fetchBranchesShouldUpdateRemoteBranchList() throws Exception {
        Path remote = createBareRemote();
        Path seed = seedRemote(remote);
        Path workspacePath = cloneRemote(remote, "main");
        createAndPushBranch(seed, "feature", "feature remote");
        Workspace workspace = workspace(workspacePath, GitRepoSource.CLONED);

        GitOperationResult result = service.fetchBranches(workspace);
        List<GitBranchInfo> branches = service.listBranches(workspace);

        assertTrue(result.success);
        assertTrue(branches.stream().anyMatch(branch -> branch.getName().equals("origin/feature")
                && branch.isRemote()));
    }

    @Test
    public void createBranchShouldCreateAndCheckoutLocalBranch() throws Exception {
        Path workspacePath = initRepositoryWithInitialCommit("create-branch-workspace");
        Workspace workspace = workspace(workspacePath, GitRepoSource.INITIALIZED);

        GitOperationResult result = service.createBranch(workspace, "feature/new-api");

        assertTrue(result.success);
        assertEquals(workspace.getCurrentBranch(), "feature/new-api");
        List<GitBranchInfo> branches = service.listBranches(workspace);
        assertTrue(branches.stream().anyMatch(branch -> branch.getName().equals("feature/new-api")
                && branch.isCurrent()
                && !branch.isRemote()));
    }

    @Test
    public void deleteBranchShouldRejectCurrentBranch() throws Exception {
        Path workspacePath = initRepositoryWithInitialCommit("delete-current-branch-workspace");
        Workspace workspace = workspace(workspacePath, GitRepoSource.INITIALIZED);

        try {
            service.deleteBranch(workspace, "main");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Cannot delete the current branch"));
            return;
        }

        throw new AssertionError("Deleting the current branch should fail");
    }

    @Test
    public void deleteBranchShouldDeleteLocalBranch() throws Exception {
        Path workspacePath = initRepositoryWithInitialCommit("delete-branch-workspace");
        try (Git git = Git.open(workspacePath.toFile())) {
            git.branchCreate().setName("feature/delete-me").call();
        }
        Workspace workspace = workspace(workspacePath, GitRepoSource.INITIALIZED);

        GitOperationResult result = service.deleteBranch(workspace, "feature/delete-me");
        List<GitBranchInfo> branches = service.listBranches(workspace);

        assertTrue(result.success);
        assertFalse(branches.stream().anyMatch(branch -> branch.getName().equals("feature/delete-me")));
    }

    @Test
    public void forceDeleteBranchShouldDeleteUnmergedLocalBranch() throws Exception {
        Path workspacePath = initRepositoryWithInitialCommit("force-delete-branch-workspace");
        try (Git git = Git.open(workspacePath.toFile())) {
            git.checkout().setCreateBranch(true).setName("feature/unmerged").call();
            writeAndCommit(git, workspacePath.resolve("feature.txt"), "unmerged", "Unmerged feature");
            git.checkout().setName("main").call();
        }
        Workspace workspace = workspace(workspacePath, GitRepoSource.INITIALIZED);

        GitOperationResult result = service.deleteBranch(workspace, "feature/unmerged", true);
        List<GitBranchInfo> branches = service.listBranches(workspace);

        assertTrue(result.success);
        assertFalse(branches.stream().anyMatch(branch -> branch.getName().equals("feature/unmerged")));
    }

    @Test
    public void listWorkingTreeChangesShouldIncludeTrackedAndUntrackedFiles() throws Exception {
        Path workspacePath = initRepositoryWithInitialCommit("diff-summary-workspace");
        Files.writeString(workspacePath.resolve("README.md"), "changed", StandardCharsets.UTF_8);
        Files.writeString(workspacePath.resolve("new-request.json"), "{}", StandardCharsets.UTF_8);

        List<GitFileChange> changes = service.listWorkingTreeChanges(workspace(workspacePath, GitRepoSource.INITIALIZED));

        assertTrue(changes.stream().anyMatch(change -> change.getPath().equals("README.md")
                && change.getType() == GitFileChange.Type.MODIFIED));
        assertTrue(changes.stream().anyMatch(change -> change.getPath().equals("new-request.json")
                && change.getType() == GitFileChange.Type.UNTRACKED));
    }

    @Test
    public void getWorkingTreeDiffShouldRenderTrackedAndUntrackedPatch() throws Exception {
        Path workspacePath = initRepositoryWithInitialCommit("diff-content-workspace");
        Files.writeString(workspacePath.resolve("README.md"), "changed", StandardCharsets.UTF_8);
        Files.writeString(workspacePath.resolve("new-request.json"), "{\n  \"name\": \"new\"\n}", StandardCharsets.UTF_8);
        Workspace workspace = workspace(workspacePath, GitRepoSource.INITIALIZED);

        String trackedDiff = service.getWorkingTreeDiff(workspace, "README.md");
        String untrackedDiff = service.getWorkingTreeDiff(workspace, "new-request.json");

        assertTrue(trackedDiff.contains("-initial"));
        assertTrue(trackedDiff.contains("+changed"));
        assertTrue(untrackedDiff.contains("+++ b/new-request.json"));
        assertTrue(untrackedDiff.contains("+  \"name\": \"new\""));
    }

    @Test
    public void getWorkingTreeDiffShouldSkipBinaryUntrackedFiles() throws Exception {
        Path workspacePath = initRepositoryWithInitialCommit("diff-binary-workspace");
        Files.write(workspacePath.resolve("binary.bin"), new byte[]{0, (byte) 0xC3, 0x28, 1});

        String diff = service.getWorkingTreeDiff(workspace(workspacePath, GitRepoSource.INITIALIZED), "binary.bin");

        assertEquals(diff, "");
    }

    @Test
    public void getWorkingTreeDiffShouldSkipLargeUntrackedFiles() throws Exception {
        Path workspacePath = initRepositoryWithInitialCommit("diff-large-workspace");
        Files.writeString(workspacePath.resolve("large.txt"), "x".repeat(1024 * 1024 + 1), StandardCharsets.UTF_8);

        String diff = service.getWorkingTreeDiff(workspace(workspacePath, GitRepoSource.INITIALIZED), "large.txt");

        assertEquals(diff, "");
    }

    private Path createBareRemote() throws Exception {
        Path remote = tempDir.resolve("remote.git");
        try (Git ignored = Git.init().setBare(true).setDirectory(remote.toFile()).call()) {
            return remote;
        }
    }

    private Path seedRemote(Path remote) throws Exception {
        Path seed = tempDir.resolve("seed");
        try (Git git = Git.init().setInitialBranch("main").setDirectory(seed.toFile()).call()) {
            writeAndCommit(git, seed.resolve("README.md"), "initial", "Initial commit");
            git.remoteAdd().setName("origin").setUri(new URIish(remote.toUri().toString())).call();
            git.push().setRemote("origin").add("refs/heads/main:refs/heads/main").call();
        }
        return seed;
    }

    private Path initRepositoryWithInitialCommit(String name) throws Exception {
        Path repo = tempDir.resolve(name);
        try (Git git = Git.init().setInitialBranch("main").setDirectory(repo.toFile()).call()) {
            writeAndCommit(git, repo.resolve("README.md"), "initial", "Initial commit");
        }
        return repo;
    }

    private Path cloneRemote(Path remote, String branch) throws Exception {
        return cloneRemote(remote, branch, "workspace");
    }

    private Path cloneRemote(Path remote, String branch, String directoryName) throws Exception {
        Path workspace = tempDir.resolve(directoryName);
        try (Git ignored = Git.cloneRepository()
                .setURI(remote.toUri().toString())
                .setDirectory(workspace.toFile())
                .setBranch(branch)
                .call()) {
            return workspace;
        }
    }

    private void configureLocalBranchTracking(Path repoPath, String localBranch, String remoteName, String remoteBranch) throws Exception {
        try (Git git = Git.open(repoPath.toFile())) {
            git.branchCreate().setName(localBranch).call();
            git.checkout().setName(localBranch).call();
            var config = git.getRepository().getConfig();
            config.setString("branch", localBranch, "remote", remoteName);
            config.setString("branch", localBranch, "merge", "refs/heads/" + remoteBranch);
            config.save();
        }
    }

    private void createAndPushBranch(Path repoPath, String branchName, String content) throws Exception {
        try (Git git = Git.open(repoPath.toFile())) {
            git.checkout().setCreateBranch(true).setName(branchName).call();
            writeAndCommit(git, repoPath.resolve("README.md"), content, "Update " + branchName);
            git.push().setRemote("origin").add("refs/heads/" + branchName + ":refs/heads/" + branchName).call();
            git.checkout().setName("main").call();
        }
    }

    private void writeAndCommit(Git git, Path file, String content, String message) throws Exception {
        Files.writeString(file, content, StandardCharsets.UTF_8);
        git.add().addFilepattern(".").call();
        git.commit()
                .setAuthor("EasyPostman Test", "test@example.com")
                .setCommitter("EasyPostman Test", "test@example.com")
                .setMessage(message)
                .call();
    }

    private Workspace workspace(Path path, GitRepoSource repoSource) {
        Workspace workspace = new Workspace();
        workspace.setType(WorkspaceType.GIT);
        workspace.setGitRepoSource(repoSource);
        workspace.setGitAuthType(GitAuthType.NONE);
        workspace.setPath(path.toString());
        return workspace;
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
