package com.laker.postman.plugin.api.service;

import com.laker.postman.model.GitAuthType;
import com.laker.postman.model.GitOperation;
import com.laker.postman.model.GitOperationResult;
import com.laker.postman.model.GitStatusCheck;
import com.laker.postman.model.RemoteStatus;
import com.laker.postman.model.Workspace;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertThrows;

public class GitPluginServiceDefaultsTest {

    @Test
    public void optionalGitManagementCapabilitiesShouldFailExplicitlyWhenUnsupported() {
        GitPluginService service = new MinimalGitPluginService();
        Workspace workspace = new Workspace();

        assertThrows(UnsupportedOperationException.class, () -> service.listWorkingTreeChanges(workspace));
        assertThrows(UnsupportedOperationException.class, () -> service.getWorkingTreeDiff(workspace, "README.md"));
        assertThrows(UnsupportedOperationException.class, () -> service.listBranches(workspace));
    }

    @Test
    public void sshRemoteConfigShouldNotFallBackToLegacyMethodWhenUnsupported() {
        GitPluginService service = new MinimalGitPluginService();

        assertThrows(UnsupportedOperationException.class, () -> service.addRemoteRepository(
                new Workspace(),
                "git@example.com:team/repo.git",
                "main",
                GitAuthType.SSH_KEY,
                "",
                "",
                "",
                "/Users/test/.ssh/id_ed25519",
                ""
        ));
    }

    @Test
    public void forceDeleteShouldNotFallBackToNormalDeleteWhenUnsupported() throws Exception {
        LegacyDeleteOnlyGitPluginService service = new LegacyDeleteOnlyGitPluginService();
        Workspace workspace = new Workspace();

        service.deleteBranch(workspace, "feature", false);
        assertThrows(UnsupportedOperationException.class, () -> service.deleteBranch(workspace, "feature", true));
    }

    private static class MinimalGitPluginService implements GitPluginService {
        @Override
        public void prepareGitWorkspace(Workspace workspace) {
        }

        @Override
        public GitStatusCheck checkGitStatus(Workspace workspace, GitOperation operation) {
            return new GitStatusCheck();
        }

        @Override
        public void clearSshCache(String privateKeyPath) {
        }

        @Override
        public GitOperationResult pullUpdates(Workspace workspace) {
            return new GitOperationResult();
        }

        @Override
        public GitOperationResult pushChanges(Workspace workspace) {
            return new GitOperationResult();
        }

        @Override
        public GitOperationResult forcePushChanges(Workspace workspace) {
            return new GitOperationResult();
        }

        @Override
        public GitOperationResult stashChanges(Workspace workspace) {
            return new GitOperationResult();
        }

        @Override
        public GitOperationResult popStashChanges(Workspace workspace) {
            return new GitOperationResult();
        }

        @Override
        public GitOperationResult forcePullUpdates(Workspace workspace) {
            return new GitOperationResult();
        }

        @Override
        public GitOperationResult commitChanges(Workspace workspace, String message) {
            return new GitOperationResult();
        }

        @Override
        public List<String> getChangedFilesBetweenCommits(Workspace workspace, String oldCommitId, String newCommitId) {
            return List.of();
        }

        @Override
        public void addRemoteRepository(Workspace workspace, String remoteUrl, String remoteBranch,
                                        GitAuthType authType, String username, String password, String token) {
        }

        @Override
        public RemoteStatus getRemoteStatus(Workspace workspace) {
            return new RemoteStatus();
        }

        @Override
        public void convertLocalToGit(Workspace workspace, String localBranch) {
        }

        @Override
        public List<com.laker.postman.model.GitCommitInfo> getGitHistory(Workspace workspace, int maxCount) {
            return List.of();
        }

        @Override
        public GitOperationResult restoreToCommit(Workspace workspace, String commitId, boolean createBackup) {
            return new GitOperationResult();
        }

        @Override
        public String getCommitDetails(Workspace workspace, String commitId) {
            return "";
        }
    }

    private static class LegacyDeleteOnlyGitPluginService extends MinimalGitPluginService {
        @Override
        public GitOperationResult deleteBranch(Workspace workspace, String branchName) {
            GitOperationResult result = new GitOperationResult();
            result.success = true;
            return result;
        }
    }
}
