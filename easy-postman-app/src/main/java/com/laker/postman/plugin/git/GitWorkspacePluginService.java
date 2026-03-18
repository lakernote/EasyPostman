package com.laker.postman.plugin.git;

import com.laker.postman.model.GitAuthType;
import com.laker.postman.model.GitCommitInfo;
import com.laker.postman.model.GitOperation;
import com.laker.postman.model.GitOperationResult;
import com.laker.postman.model.GitStatusCheck;
import com.laker.postman.model.GitRepoSource;
import com.laker.postman.model.RemoteStatus;
import com.laker.postman.model.Workspace;
import com.laker.postman.model.WorkspaceType;
import com.laker.postman.plugin.bridge.GitPluginService;
import com.laker.postman.plugin.git.internal.GitConflictDetector;
import com.laker.postman.plugin.git.internal.SshCredentialsProvider;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GitWorkspacePluginService implements GitPluginService {

    private static final int GIT_OPERATION_TIMEOUT = 10;

    @Override
    public void prepareGitWorkspace(Workspace workspace) throws Exception {
        ensureGitWorkspace(workspace);
        if (workspace.getGitRepoSource() == GitRepoSource.CLONED) {
            cloneRepository(workspace);
        } else if (workspace.getGitRepoSource() == GitRepoSource.INITIALIZED) {
            initializeGitRepository(workspace);
        }
    }

    @Override
    public void migrateDefaultWorkspaceGit(Workspace workspace, String commitMessage) throws Exception {
        try (Git git = Git.init().setDirectory(new File(workspace.getPath())).call()) {
            createGitignore(workspace);
            git.add().addFilepattern(".").call();
            git.commit().setMessage(commitMessage).call();

            String oldBranch = workspace.getCurrentBranch();
            String actualBranch = git.getRepository().getBranch();
            if (oldBranch != null && !oldBranch.isBlank() && !oldBranch.equals(actualBranch)) {
                git.branchCreate().setName(oldBranch).call();
                git.checkout().setName(oldBranch).call();
            } else {
                workspace.setCurrentBranch(actualBranch);
            }

            String remoteUrl = workspace.getGitRemoteUrl();
            if (remoteUrl != null && !remoteUrl.isBlank()) {
                git.remoteAdd().setName("origin").setUri(new URIish(remoteUrl)).call();
            }

            workspace.setLastCommitId(getLastCommitId(git));
            workspace.setUpdatedAt(System.currentTimeMillis());
        }
    }

    @Override
    public GitStatusCheck checkGitStatus(Workspace workspace, GitOperation operation) {
        return GitConflictDetector.checkGitStatus(
                workspace.getPath(),
                operation.name(),
                getCredentialsProvider(workspace),
                getSshCredentialsProvider(workspace)
        );
    }

    @Override
    public void clearSshCache(String privateKeyPath) {
        if (privateKeyPath != null && !privateKeyPath.isBlank()) {
            SshCredentialsProvider.clearCache(privateKeyPath);
        }
    }

    @Override
    public GitOperationResult pullUpdates(Workspace workspace) throws Exception {
        ensureGitWorkspace(workspace);
        GitOperationResult result = new GitOperationResult();
        result.operationType = "Pull";

        try (Git git = Git.open(new File(workspace.getPath()))) {
            String branch = git.getRepository().getBranch();
            String tracking = git.getRepository().getConfig().getString("branch", branch, "merge");
            if (tracking == null) {
                throw new IllegalStateException(
                        "Local branch is not tracking a remote branch, unable to pull. Please set up tracking branch first, e.g.: git branch --set-upstream-to=origin/" + branch);
            }

            var statusBefore = git.status().call();
            String commitIdBefore = getLastCommitId(git);

            if (commitIdBefore == null) {
                result.details += "Detected empty local repository, attempting to pull initial content from remote\n";
                try {
                    var fetchCommand = git.fetch();
                    fetchCommand.setTimeout(GIT_OPERATION_TIMEOUT);
                    applyAuth(fetchCommand, workspace);
                    fetchCommand.call();

                    String remoteName = git.getRepository().getConfig().getString("branch", branch, "remote");
                    if (remoteName == null) {
                        remoteName = "origin";
                    }
                    String remoteBranchName = tracking.startsWith("refs/heads/")
                            ? tracking.substring("refs/heads/".length())
                            : tracking;
                    String remoteRef = "refs/remotes/" + remoteName + "/" + remoteBranchName;
                    ObjectId remoteId = git.getRepository().resolve(remoteRef);
                    if (remoteId == null) {
                        result.success = true;
                        result.message = "Remote repository is empty, nothing to pull";
                        result.details += "Remote repository is currently empty, waiting for initial push\n";
                        return result;
                    }
                } catch (RefNotAdvertisedException e) {
                    result.success = true;
                    result.message = "Remote repository is empty or remote branch does not exist, nothing to pull";
                    result.details += "Remote repository is empty or branch '" + tracking + "' does not exist\n";
                    return result;
                } catch (Exception e) {
                    throw new RuntimeException("Unable to connect to remote repository: " + e.getMessage(), e);
                }
            }

            if (!statusBefore.isClean()) {
                result.details += "Detected local uncommitted content, automatically cleaning:\n";
                if (!statusBefore.getModified().isEmpty()) {
                    result.details += "  Reset modified files: " + String.join(", ", statusBefore.getModified()) + "\n";
                }
                if (!statusBefore.getUntracked().isEmpty()) {
                    result.details += "  Clean untracked files: " + String.join(", ", statusBefore.getUntracked()) + "\n";
                }
                git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).call();
                git.clean().setCleanDirectories(true).setForce(true).call();
                if (!git.status().call().isClean()) {
                    throw new IllegalStateException("Still have uncommitted content or conflicts after automatic cleanup, please handle manually.");
                }
            }

            try {
                var pullCommand = git.pull();
                pullCommand.setTimeout(GIT_OPERATION_TIMEOUT);
                pullCommand.setStrategy(MergeStrategy.RECURSIVE);
                applyAuth(pullCommand, workspace);
                var pullResult = pullCommand.call();
                if (!pullResult.isSuccessful()) {
                    throw new RuntimeException("Git pull failed: " + pullResult);
                }

                String commitIdAfter = getLastCommitId(git);
                if (commitIdBefore == null || !commitIdBefore.equals(commitIdAfter)) {
                    try {
                        if (commitIdBefore != null && commitIdAfter != null) {
                            List<String> changedFiles = getChangedFilesBetweenCommits(workspace, commitIdBefore, commitIdAfter);
                            result.affectedFiles.addAll(changedFiles);
                            result.details += "Pulled new commits, affected files:\n";
                            for (String file : changedFiles) {
                                result.details += "  " + file + "\n";
                            }
                        } else if (commitIdAfter != null) {
                            result.details += "Pulled initial content from remote repository\n";
                        }
                    } catch (Exception e) {
                        log.warn("Failed to get changed files", e);
                        result.details += "Unable to get detailed file change information\n";
                    }
                    result.message = "Successfully pulled updates, " + result.affectedFiles.size() + " files affected";
                } else {
                    result.message = "Already up to date, no updates needed";
                    result.details += "Local repository is already up to date\n";
                }

                workspace.setLastCommitId(commitIdAfter);
                workspace.setUpdatedAt(System.currentTimeMillis());
                result.success = true;
            } catch (RefNotAdvertisedException e) {
                result.success = true;
                result.message = "Remote branch does not exist, nothing to pull";
                result.details += "Remote branch '" + tracking + "' does not exist or is empty\n";
            }
        }

        return result;
    }

    @Override
    public GitOperationResult pushChanges(Workspace workspace) throws Exception {
        ensureGitWorkspace(workspace);
        GitOperationResult result = new GitOperationResult();
        result.operationType = "Push";

        try (Git git = Git.open(new File(workspace.getPath()))) {
            var remotes = git.remoteList().call();
            if (remotes.isEmpty()) {
                throw new IllegalStateException("No remote repository configured, unable to push");
            }

            String currentBranch = git.getRepository().getBranch();
            String tracking = git.getRepository().getConfig().getString("branch", currentBranch, "merge");
            if (tracking == null) {
                throw new IllegalStateException("Current branch has no upstream branch set, please perform initial push or set upstream branch first");
            }

            String remoteBranchName = tracking.startsWith("refs/heads/")
                    ? tracking.substring("refs/heads/".length())
                    : tracking;
            String remoteName = git.getRepository().getConfig().getString("branch", currentBranch, "remote");
            if (remoteName == null) {
                remoteName = "origin";
            }

            int unpushedCommitsCount = 0;
            try {
                var fetchCommand = git.fetch();
                fetchCommand.setTimeout(GIT_OPERATION_TIMEOUT);
                applyAuth(fetchCommand, workspace);
                fetchCommand.call();

                String localRef = "refs/heads/" + currentBranch;
                String remoteRef = "refs/remotes/" + remoteName + "/" + remoteBranchName;
                ObjectId localId = git.getRepository().resolve(localRef);
                ObjectId remoteId = git.getRepository().resolve(remoteRef);
                if (localId == null) {
                    throw new IllegalStateException("Unable to find local branch: " + currentBranch);
                }

                Iterable<RevCommit> commits = remoteId != null
                        ? git.log().addRange(remoteId, localId).call()
                        : git.log().call();
                for (RevCommit commit : commits) {
                    unpushedCommitsCount++;
                    if (commit.getParentCount() > 0 && (remoteId != null || unpushedCommitsCount <= 10)) {
                        try {
                            var parent = commit.getParent(0);
                            List<DiffEntry> diffs = git.diff()
                                    .setOldTree(prepareTreeParser(git.getRepository(), parent.toObjectId()))
                                    .setNewTree(prepareTreeParser(git.getRepository(), commit.toObjectId()))
                                    .call();
                            for (DiffEntry diff : diffs) {
                                String fileName = diff.getNewPath();
                                if (!result.affectedFiles.contains(fileName)) {
                                    result.affectedFiles.add(fileName);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Failed to get commit files for {}", commit.getName(), e);
                        }
                    }
                }

                if (unpushedCommitsCount == 0) {
                    result.success = true;
                    result.message = "No commits to push, local branch is up to date";
                    result.details = "Local branch " + currentBranch + " is in sync with remote branch "
                            + remoteName + "/" + remoteBranchName + "\n";
                    return result;
                }
            } catch (Exception e) {
                log.warn("Failed to accurately count unpushed commits, proceeding with push", e);
            }

            var pushCommand = git.push().setRemote(remoteName).add(currentBranch + ":" + remoteBranchName);
            pushCommand.setTimeout(GIT_OPERATION_TIMEOUT);
            applyAuth(pushCommand, workspace);
            var pushResults = pushCommand.call();

            result.details += "Push details:\n";
            result.details += "  Local branch: " + currentBranch + "\n";
            result.details += "  Remote branch: " + remoteName + "/" + remoteBranchName + "\n";
            result.details += "  Pushed commits: " + unpushedCommitsCount + "\n";

            boolean pushSuccess = false;
            for (var pushResult : pushResults) {
                String msg = pushResult.getMessages();
                if (msg != null && !msg.isEmpty() && (msg.toLowerCase().contains("error") || msg.toLowerCase().contains("fail"))) {
                    result.details += "  Push message: " + msg + "\n";
                }
                for (var remoteRefUpdate : pushResult.getRemoteUpdates()) {
                    result.details += "  Branch update: " + remoteRefUpdate.getSrcRef() + " -> "
                            + remoteRefUpdate.getRemoteName() + " (" + remoteRefUpdate.getStatus() + ")\n";
                    if (remoteRefUpdate.getMessage() != null && !remoteRefUpdate.getMessage().isEmpty()) {
                        result.details += "    Details: " + remoteRefUpdate.getMessage() + "\n";
                    }
                    if (remoteRefUpdate.getStatus() == org.eclipse.jgit.transport.RemoteRefUpdate.Status.OK
                            || remoteRefUpdate.getStatus() == org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE) {
                        pushSuccess = true;
                    }
                }
            }

            if (!pushSuccess) {
                throw new RuntimeException("Push failed, please check error details in push result");
            }

            result.success = true;
            result.message = "Successfully pushed " + unpushedCommitsCount + " commits, " + result.affectedFiles.size() + " files affected";
        }

        return result;
    }

    @Override
    public GitOperationResult forcePushChanges(Workspace workspace) throws Exception {
        ensureGitWorkspace(workspace);
        GitOperationResult result = new GitOperationResult();
        result.operationType = "Force Push";

        try (Git git = Git.open(new File(workspace.getPath()))) {
            String currentBranch = git.getRepository().getBranch();
            var pushCommand = git.push().setForce(true);
            pushCommand.setTimeout(GIT_OPERATION_TIMEOUT);
            applyAuth(pushCommand, workspace);
            var pushResults = pushCommand.call();

            result.details += "Force push details:\n";
            result.details += "  Local branch: " + currentBranch + "\n";
            result.details += "  Warning: Force push has overwritten remote changes\n";

            boolean pushSuccess = false;
            for (var pushResult : pushResults) {
                for (var remoteRefUpdate : pushResult.getRemoteUpdates()) {
                    result.details += "  Branch update: " + remoteRefUpdate.getSrcRef() + " -> "
                            + remoteRefUpdate.getRemoteName() + " (" + remoteRefUpdate.getStatus() + ")\n";
                    if (remoteRefUpdate.getStatus() == org.eclipse.jgit.transport.RemoteRefUpdate.Status.OK
                            || remoteRefUpdate.getStatus() == org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE) {
                        pushSuccess = true;
                    }
                }
            }

            if (!pushSuccess) {
                throw new RuntimeException("Force push failed");
            }

            result.success = true;
            result.message = "Force push successful, remote changes have been overwritten";
        }

        return result;
    }

    @Override
    public GitOperationResult stashChanges(Workspace workspace) throws Exception {
        ensureGitWorkspace(workspace);
        GitOperationResult result = new GitOperationResult();
        result.operationType = "Stash";

        try (Git git = Git.open(new File(workspace.getPath()))) {
            var status = git.status().call();
            result.affectedFiles.addAll(status.getModified());
            result.affectedFiles.addAll(status.getChanged());
            result.affectedFiles.addAll(status.getAdded());
            result.affectedFiles.addAll(status.getRemoved());

            var stashResult = git.stashCreate().call();
            if (stashResult == null) {
                throw new RuntimeException("Stash failed, no changes to stash");
            }

            result.success = true;
            result.message = "Successfully stashed " + result.affectedFiles.size() + " file changes";
            result.details = "Stash ID: " + stashResult.getName().substring(0, 8) + "\n";
        }

        return result;
    }

    @Override
    public GitOperationResult popStashChanges(Workspace workspace) throws Exception {
        ensureGitWorkspace(workspace);
        GitOperationResult result = new GitOperationResult();
        result.operationType = "Pop Stash";

        try (Git git = Git.open(new File(workspace.getPath()))) {
            if (!git.stashList().call().iterator().hasNext()) {
                throw new RuntimeException("No stashed changes found");
            }

            git.stashApply().call();
            git.stashDrop().call();
            var status = git.status().call();
            result.affectedFiles.addAll(status.getModified());
            result.affectedFiles.addAll(status.getChanged());
            result.affectedFiles.addAll(status.getAdded());

            result.success = true;
            result.message = "Successfully restored stashed changes";
            result.details = "Restored files:\n";
            for (String file : result.affectedFiles) {
                result.details += "  " + file + "\n";
            }
        }

        return result;
    }

    @Override
    public GitOperationResult forcePullUpdates(Workspace workspace) throws Exception {
        ensureGitWorkspace(workspace);
        GitOperationResult result = new GitOperationResult();
        result.operationType = "Force Pull";

        try (Git git = Git.open(new File(workspace.getPath()))) {
            var statusBefore = git.status().call();
            result.affectedFiles.addAll(statusBefore.getModified());
            result.affectedFiles.addAll(statusBefore.getChanged());
            result.affectedFiles.addAll(statusBefore.getAdded());
            result.affectedFiles.addAll(statusBefore.getUntracked());

            String commitIdBefore = getLastCommitId(git);
            String branchName = git.getRepository().getBranch();

            var fetchCmd = git.fetch();
            fetchCmd.setTimeout(GIT_OPERATION_TIMEOUT);
            applyAuth(fetchCmd, workspace);
            fetchCmd.call();

            git.reset().setRef("origin/" + branchName).setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).call();
            git.clean().setCleanDirectories(true).setForce(true).call();

            String commitIdAfter = getLastCommitId(git);
            workspace.setLastCommitId(commitIdAfter);
            workspace.setUpdatedAt(System.currentTimeMillis());

            result.success = true;
            result.message = "Force pull successful, local changes have been discarded";
            result.details = !java.util.Objects.equals(commitIdBefore, commitIdAfter)
                    ? "Pulled new commits\n"
                    : "Already up to date\n";
        }

        return result;
    }

    @Override
    public GitOperationResult commitChanges(Workspace workspace, String message) throws Exception {
        ensureGitWorkspace(workspace);
        GitOperationResult result = new GitOperationResult();
        result.operationType = "Commit";

        try (Git git = Git.open(new File(workspace.getPath()))) {
            var status = git.status().call();
            boolean hasChanges = !status.getAdded().isEmpty()
                    || !status.getModified().isEmpty()
                    || !status.getRemoved().isEmpty()
                    || !status.getUntracked().isEmpty()
                    || !status.getMissing().isEmpty();
            if (!hasChanges) {
                throw new IllegalStateException("No file changes to commit");
            }

            result.affectedFiles.addAll(status.getAdded());
            result.affectedFiles.addAll(status.getModified());
            result.affectedFiles.addAll(status.getRemoved());
            result.affectedFiles.addAll(status.getUntracked());

            git.add().addFilepattern(".").call();
            var commitResult = git.commit().setMessage(message).call();

            workspace.setLastCommitId(getLastCommitId(git));
            workspace.setUpdatedAt(System.currentTimeMillis());

            result.success = true;
            result.message = "Successfully committed " + result.affectedFiles.size() + " file changes";
            result.details = "Commit ID: " + commitResult.getName().substring(0, 8) + "\n";
        }

        return result;
    }

    @Override
    public List<String> getChangedFilesBetweenCommits(Workspace workspace, String oldCommitId, String newCommitId) throws Exception {
        ensureGitWorkspace(workspace);
        try (Git git = Git.open(new File(workspace.getPath()))) {
            ObjectId oldId = git.getRepository().resolve(oldCommitId);
            ObjectId newId = git.getRepository().resolve(newCommitId);
            List<DiffEntry> diffs = git.diff()
                    .setOldTree(prepareTreeParser(git.getRepository(), oldId))
                    .setNewTree(prepareTreeParser(git.getRepository(), newId))
                    .call();
            List<String> files = new ArrayList<>();
            for (DiffEntry entry : diffs) {
                files.add(entry.getNewPath());
            }
            return files;
        }
    }

    @Override
    public void addRemoteRepository(Workspace workspace, String remoteUrl, String remoteBranch,
                                    GitAuthType authType, String username, String password, String token) throws Exception {
        ensureGitWorkspace(workspace);
        if (workspace.getGitRepoSource() != GitRepoSource.INITIALIZED) {
            throw new IllegalStateException("Only Git workspaces of type INITIALIZED can add a remote repository");
        }

        try (Git git = Git.open(new File(workspace.getPath()))) {
            git.remoteAdd().setName("origin").setUri(new URIish(remoteUrl)).call();

            String currentBranch = git.getRepository().getBranch();
            String targetRemoteBranch = remoteBranch;
            if (targetRemoteBranch != null && targetRemoteBranch.startsWith("origin/")) {
                targetRemoteBranch = targetRemoteBranch.substring("origin/".length());
            }
            if (targetRemoteBranch == null || targetRemoteBranch.trim().isEmpty()) {
                targetRemoteBranch = currentBranch;
            }

            var config = git.getRepository().getConfig();
            config.setString("branch", currentBranch, "remote", "origin");
            config.setString("branch", currentBranch, "merge", "refs/heads/" + targetRemoteBranch);
            config.save();

            workspace.setGitRemoteUrl(remoteUrl);
            workspace.setRemoteBranch("origin/" + targetRemoteBranch);
            workspace.setGitAuthType(authType);
            workspace.setGitUsername(username);
            if (authType == GitAuthType.PASSWORD) {
                workspace.setGitPassword(password);
                workspace.setGitToken(null);
            } else if (authType == GitAuthType.TOKEN) {
                workspace.setGitToken(token);
                workspace.setGitPassword(null);
            }
            workspace.setUpdatedAt(System.currentTimeMillis());
        }
    }

    @Override
    public RemoteStatus getRemoteStatus(Workspace workspace) throws Exception {
        ensureGitWorkspace(workspace);
        try (Git git = Git.open(new File(workspace.getPath()))) {
            RemoteStatus status = new RemoteStatus();
            var remotes = git.remoteList().call();
            status.hasRemote = !remotes.isEmpty();
            if (status.hasRemote) {
                status.remoteUrl = remotes.get(0).getURIs().get(0).toString();
                String currentBranch = git.getRepository().getBranch();
                String tracking = git.getRepository().getConfig().getString("branch", currentBranch, "merge");
                status.hasUpstream = tracking != null;
                status.currentBranch = currentBranch;
                status.upstreamBranch = tracking;
            }
            return status;
        }
    }

    @Override
    public void convertLocalToGit(Workspace workspace, String localBranch) throws Exception {
        if (workspace.getType() != WorkspaceType.LOCAL) {
            throw new IllegalStateException("Only LOCAL workspaces can be converted to GIT");
        }
        workspace.setType(WorkspaceType.GIT);
        workspace.setGitRepoSource(GitRepoSource.INITIALIZED);
        workspace.setCurrentBranch(localBranch != null && !localBranch.isEmpty() ? localBranch : "master");
        initializeGitRepository(workspace);
        workspace.setUpdatedAt(System.currentTimeMillis());
    }

    @Override
    public List<GitCommitInfo> getGitHistory(Workspace workspace, int maxCount) throws Exception {
        ensureGitWorkspace(workspace);
        List<GitCommitInfo> commits = new ArrayList<>();
        try (Git git = Git.open(new File(workspace.getPath()))) {
            Iterable<RevCommit> logs = maxCount > 0 ? git.log().setMaxCount(maxCount).call() : git.log().call();
            for (RevCommit revCommit : logs) {
                GitCommitInfo commitInfo = new GitCommitInfo();
                commitInfo.setCommitId(revCommit.getName());
                commitInfo.setShortCommitId(revCommit.getName().substring(0, 8));
                commitInfo.setMessage(revCommit.getFullMessage());
                PersonIdent author = revCommit.getAuthorIdent();
                commitInfo.setAuthorName(author.getName());
                commitInfo.setAuthorEmail(author.getEmailAddress());
                commitInfo.setCommitTime(revCommit.getCommitTime() * 1000L);
                PersonIdent committer = revCommit.getCommitterIdent();
                commitInfo.setCommitterName(committer.getName());
                commitInfo.setCommitterEmail(committer.getEmailAddress());
                commits.add(commitInfo);
            }
        } catch (NoHeadException e) {
            log.warn("No commits found in workspace: {}", workspace.getName());
        }
        return commits;
    }

    @Override
    public GitOperationResult restoreToCommit(Workspace workspace, String commitId, boolean createBackup) throws Exception {
        ensureGitWorkspace(workspace);
        GitOperationResult result = new GitOperationResult();
        String backupCommitId = null;

        try (Git git = Git.open(new File(workspace.getPath()))) {
            Repository repository = git.getRepository();
            var status = git.status().call();
            boolean hasChanges = !status.getAdded().isEmpty()
                    || !status.getModified().isEmpty()
                    || !status.getRemoved().isEmpty()
                    || !status.getUntracked().isEmpty()
                    || !status.getMissing().isEmpty();

            if (hasChanges) {
                if (createBackup) {
                    git.add().addFilepattern(".").call();
                    String backupMessage = "Backup before restore to " + commitId.substring(0, 8);
                    backupCommitId = git.commit().setMessage(backupMessage).call().getName();
                    result.details += "Created backup commit: " + backupCommitId.substring(0, 8) + "\n\n";
                } else {
                    git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).call();
                    result.details += "Discarded uncommitted changes\n\n";
                }
            }

            ObjectId currentHead = repository.resolve("HEAD");
            git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.SOFT).setRef(commitId).call();
            git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.MIXED).setRef(commitId).call();
            git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.SOFT).setRef(currentHead.getName()).call();
            git.checkout().setAllPaths(true).setForced(true).call();

            git.add().addFilepattern(".").setUpdate(true).call();
            git.add().addFilepattern(".").call();

            var statusAfterRestore = git.status().call();
            boolean hasChangesToCommit = !statusAfterRestore.getAdded().isEmpty()
                    || !statusAfterRestore.getModified().isEmpty()
                    || !statusAfterRestore.getRemoved().isEmpty();

            if (hasChangesToCommit) {
                String restoreMessage = "Restore to commit " + commitId.substring(0, 8);
                if (backupCommitId != null) {
                    restoreMessage += "\n\nBackup: " + backupCommitId.substring(0, 8);
                }
                var restoreCommit = git.commit().setMessage(restoreMessage).call();
                result.details += "Created restore commit: " + restoreCommit.getName().substring(0, 8) + "\n";
            } else {
                result.details += "No changes detected, already at target state\n";
            }

            workspace.setLastCommitId(getLastCommitId(git));
            workspace.setUpdatedAt(System.currentTimeMillis());
            result.success = true;
            result.message = "Successfully restored to commit " + commitId.substring(0, 8);
        }

        return result;
    }

    @Override
    public String getCommitDetails(Workspace workspace, String commitId) throws Exception {
        ensureGitWorkspace(workspace);
        StringBuilder details = new StringBuilder();
        try (Git git = Git.open(new File(workspace.getPath()))) {
            Repository repository = git.getRepository();
            try (RevWalk revWalk = new RevWalk(repository)) {
                ObjectId objectId = repository.resolve(commitId);
                RevCommit commit = revWalk.parseCommit(objectId);

                details.append("Commit: ").append(commit.getName()).append("\n");
                details.append("Author: ").append(commit.getAuthorIdent().getName())
                        .append(" <").append(commit.getAuthorIdent().getEmailAddress()).append(">\n");
                details.append("Date: ").append(new java.util.Date(commit.getCommitTime() * 1000L)).append("\n");
                details.append("\n").append(commit.getFullMessage()).append("\n\n");

                if (commit.getParentCount() > 0) {
                    RevCommit parent = revWalk.parseCommit(commit.getParent(0).getId());
                    CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
                    CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
                    try (var reader = repository.newObjectReader()) {
                        oldTreeParser.reset(reader, parent.getTree().getId());
                        newTreeParser.reset(reader, commit.getTree().getId());
                    }
                    List<DiffEntry> diffs = git.diff().setOldTree(oldTreeParser).setNewTree(newTreeParser).call();
                    if (!diffs.isEmpty()) {
                        details.append("Changed files:\n");
                        for (DiffEntry entry : diffs) {
                            String changeType = switch (entry.getChangeType()) {
                                case ADD -> "A";
                                case MODIFY -> "M";
                                case DELETE -> "D";
                                case RENAME -> "R";
                                case COPY -> "C";
                            };
                            details.append("  ").append(changeType).append(" ").append(entry.getNewPath()).append("\n");
                        }
                    }
                } else {
                    details.append("(Initial commit)\n");
                }
            }
        }
        return details.toString();
    }

    private void ensureGitWorkspace(Workspace workspace) {
        if (workspace.getType() != WorkspaceType.GIT) {
            throw new IllegalStateException("Not a Git workspace");
        }
    }

    private UsernamePasswordCredentialsProvider getCredentialsProvider(Workspace workspace) {
        if (workspace.getGitAuthType() == GitAuthType.PASSWORD
                && workspace.getGitUsername() != null
                && workspace.getGitPassword() != null) {
            return new UsernamePasswordCredentialsProvider(workspace.getGitUsername(), workspace.getGitPassword());
        }
        if (workspace.getGitAuthType() == GitAuthType.TOKEN
                && workspace.getGitUsername() != null
                && workspace.getGitToken() != null) {
            return new UsernamePasswordCredentialsProvider(workspace.getGitUsername(), workspace.getGitToken());
        }
        return null;
    }

    private SshCredentialsProvider getSshCredentialsProvider(Workspace workspace) {
        if (workspace.getGitAuthType() == GitAuthType.SSH_KEY && workspace.getSshPrivateKeyPath() != null) {
            return new SshCredentialsProvider(workspace.getSshPrivateKeyPath(), workspace.getSshPassphrase());
        }
        return null;
    }

    private void cloneRepository(Workspace workspace) throws Exception {
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(workspace.getGitRemoteUrl())
                .setDirectory(new File(workspace.getPath()));
        cloneCommand.setTimeout(GIT_OPERATION_TIMEOUT);

        if (workspace.getCurrentBranch() != null && !workspace.getCurrentBranch().isEmpty()) {
            cloneCommand.setBranch(workspace.getCurrentBranch());
        }
        applyAuth(cloneCommand, workspace);

        try (Git git = cloneCommand.call()) {
            workspace.setLastCommitId(getLastCommitId(git));
            String actualCurrentBranch = git.getRepository().getBranch();
            workspace.setCurrentBranch(actualCurrentBranch);

            String remoteBranch = git.getRepository().getConfig().getString("branch", actualCurrentBranch, "merge");
            if (remoteBranch != null) {
                if (remoteBranch.startsWith("refs/heads/")) {
                    String branchName = remoteBranch.substring("refs/heads/".length());
                    workspace.setRemoteBranch("origin/" + branchName);
                } else {
                    workspace.setRemoteBranch(remoteBranch);
                }
            } else {
                try {
                    var config = git.getRepository().getConfig();
                    config.setString("branch", actualCurrentBranch, "remote", "origin");
                    config.setString("branch", actualCurrentBranch, "merge", "refs/heads/" + actualCurrentBranch);
                    config.save();
                    workspace.setRemoteBranch("origin/" + actualCurrentBranch);
                } catch (Exception e) {
                    log.warn("Failed to auto-bind remote branch", e);
                    workspace.setRemoteBranch(null);
                }
            }
        }
    }

    private void initializeGitRepository(Workspace workspace) throws Exception {
        try (Git git = Git.init().setDirectory(new File(workspace.getPath())).call()) {
            createInitialCommit(git, workspace);
            String currentBranch = git.getRepository().getBranch();
            if (workspace.getCurrentBranch() != null
                    && !workspace.getCurrentBranch().isEmpty()
                    && !workspace.getCurrentBranch().equals(currentBranch)) {
                git.branchCreate().setName(workspace.getCurrentBranch()).call();
                git.checkout().setName(workspace.getCurrentBranch()).call();
                workspace.setCurrentBranch(workspace.getCurrentBranch());
            } else {
                workspace.setCurrentBranch(currentBranch);
            }
            workspace.setRemoteBranch(null);
        }
    }

    private void createInitialCommit(Git git, Workspace workspace) throws Exception {
        createGitignore(workspace);
        Path readmePath = Paths.get(workspace.getPath(), "README.md");
        Files.write(readmePath, String.format("# %s\n\n%s",
                workspace.getName(),
                workspace.getDescription() != null ? workspace.getDescription() : "EasyPostman Workspace").getBytes());
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Initial commit").call();
        workspace.setLastCommitId(getLastCommitId(git));
    }

    private void createGitignore(Workspace workspace) throws IOException {
        Path gitignorePath = Paths.get(workspace.getPath(), ".gitignore");
        List<String> ignorePatterns = new ArrayList<>();
        ignorePatterns.add("# EasyPostman Workspace");
        ignorePatterns.add("");
        ignorePatterns.add("# Temporary files");
        ignorePatterns.add("*.tmp");
        ignorePatterns.add("*.bak");
        ignorePatterns.add("*~");
        ignorePatterns.add("");
        ignorePatterns.add("# OS generated files");
        ignorePatterns.add(".DS_Store");
        ignorePatterns.add("Thumbs.db");
        Files.write(gitignorePath, ignorePatterns, StandardCharsets.UTF_8);
    }

    private String getLastCommitId(Git git) {
        try {
            var iterator = git.log().setMaxCount(1).call().iterator();
            return iterator.hasNext() ? iterator.next().getName() : null;
        } catch (NoHeadException e) {
            return null;
        } catch (Exception e) {
            log.error("Failed to get the last commit id from git log", e);
            return null;
        }
    }

    private AbstractTreeIterator prepareTreeParser(Repository repository, ObjectId objectId) throws Exception {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(objectId);
            ObjectId treeId = commit.getTree().getId();
            CanonicalTreeParser treeWalk = new CanonicalTreeParser();
            try (var reader = repository.newObjectReader()) {
                treeWalk.reset(reader, treeId);
            }
            return treeWalk;
        }
    }

    private void applyAuth(Object command, Workspace workspace) {
        UsernamePasswordCredentialsProvider credentialsProvider = getCredentialsProvider(workspace);
        SshCredentialsProvider sshCredentialsProvider = getSshCredentialsProvider(workspace);
        if (command instanceof org.eclipse.jgit.api.TransportCommand<?, ?> transportCommand) {
            if (credentialsProvider != null) {
                transportCommand.setCredentialsProvider(credentialsProvider);
            } else if (sshCredentialsProvider != null) {
                transportCommand.setTransportConfigCallback(sshCredentialsProvider);
            }
        }
    }
}
