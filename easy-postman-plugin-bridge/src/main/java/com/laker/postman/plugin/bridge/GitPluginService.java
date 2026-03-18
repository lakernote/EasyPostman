package com.laker.postman.plugin.bridge;

import com.laker.postman.model.GitAuthType;
import com.laker.postman.model.GitCommitInfo;
import com.laker.postman.model.GitOperation;
import com.laker.postman.model.GitOperationResult;
import com.laker.postman.model.GitStatusCheck;
import com.laker.postman.model.RemoteStatus;
import com.laker.postman.model.Workspace;

import java.util.List;

public interface GitPluginService {

    void prepareGitWorkspace(Workspace workspace) throws Exception;

    void migrateDefaultWorkspaceGit(Workspace workspace, String commitMessage) throws Exception;

    GitStatusCheck checkGitStatus(Workspace workspace, GitOperation operation);

    void clearSshCache(String privateKeyPath);

    GitOperationResult pullUpdates(Workspace workspace) throws Exception;

    GitOperationResult pushChanges(Workspace workspace) throws Exception;

    GitOperationResult forcePushChanges(Workspace workspace) throws Exception;

    GitOperationResult stashChanges(Workspace workspace) throws Exception;

    GitOperationResult popStashChanges(Workspace workspace) throws Exception;

    GitOperationResult forcePullUpdates(Workspace workspace) throws Exception;

    GitOperationResult commitChanges(Workspace workspace, String message) throws Exception;

    List<String> getChangedFilesBetweenCommits(Workspace workspace, String oldCommitId, String newCommitId) throws Exception;

    void addRemoteRepository(Workspace workspace, String remoteUrl, String remoteBranch,
                             GitAuthType authType, String username, String password, String token) throws Exception;

    RemoteStatus getRemoteStatus(Workspace workspace) throws Exception;

    void convertLocalToGit(Workspace workspace, String localBranch) throws Exception;

    List<GitCommitInfo> getGitHistory(Workspace workspace, int maxCount) throws Exception;

    GitOperationResult restoreToCommit(Workspace workspace, String commitId, boolean createBackup) throws Exception;

    String getCommitDetails(Workspace workspace, String commitId) throws Exception;
}
