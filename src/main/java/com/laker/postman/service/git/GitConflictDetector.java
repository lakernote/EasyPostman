package com.laker.postman.service.git;

import com.laker.postman.model.GitStatusCheck;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Git冲突检测工具类
 * 检测Git操作前的潜在冲突和问题
 */
@Slf4j
public class GitConflictDetector {

    private static final String REFS_HEADS_PREFIX = "refs/heads/";

    private GitConflictDetector() {
        // 工具类，隐藏构造函数
    }

    /**
     * 检查Git仓库状态，判断是否可以执行指定操作（完整版本，支持所有认证方式）
     */
    public static GitStatusCheck checkGitStatus(String workspacePath, String operationType,
                                                CredentialsProvider credentialsProvider,
                                                SshCredentialsProvider sshCredentialsProvider) {
        GitStatusCheck result = new GitStatusCheck();

        try (Git git = Git.open(new File(workspacePath))) {
            // 获取基本信息
            result.currentBranch = git.getRepository().getBranch();

            // 检查本地状态
            Status status = git.status().call();
            checkLocalStatus(status, result);

            // 检查远程状态
            checkRemoteStatus(git, result, credentialsProvider, sshCredentialsProvider);

            // 根据操作类型生成建议
            generateSuggestions(result, operationType);

        } catch (Exception e) {
            log.error("Failed to check git status", e);
            result.warnings.add("无法检查Git状态: " + e.getMessage());
            result.hasAuthenticationIssue = true;
        }

        return result;
    }

    private static void checkLocalStatus(Status status, GitStatusCheck result) {
        // 检查未提交的变更
        result.hasUncommittedChanges = !status.getModified().isEmpty() ||
                !status.getChanged().isEmpty() ||
                !status.getRemoved().isEmpty() ||
                !status.getMissing().isEmpty();

        if (result.hasUncommittedChanges) {
            // 计算未提交变更的数量和文件列表
            result.uncommittedCount = status.getModified().size() +
                    status.getChanged().size() +
                    status.getRemoved().size() +
                    status.getMissing().size();
            result.uncommittedFiles.addAll(status.getModified()); // 修改的文件
            result.uncommittedFiles.addAll(status.getChanged()); // 新增的文件
            result.uncommittedFiles.addAll(status.getRemoved()); // 删除的文件
            result.uncommittedFiles.addAll(status.getMissing()); // 丢失的文件
        }

        // 检查未跟踪的文件
        result.hasUntrackedFiles = !status.getUntracked().isEmpty();
        if (result.hasUntrackedFiles) {
            result.untrackedCount = status.getUntracked().size();
            result.untrackedFilesList.addAll(status.getUntracked()); // 未跟踪的文件
        }

        // 检查是否可以提交
        result.canCommit = result.hasUncommittedChanges || result.hasUntrackedFiles;
    }

    private static void checkRemoteStatus(Git git, GitStatusCheck result,
                                          CredentialsProvider credentialsProvider,
                                          SshCredentialsProvider sshCredentialsProvider) {
        try {
            String currentBranch = git.getRepository().getBranch();
            String tracking = git.getRepository().getConfig()
                    .getString("branch", currentBranch, "merge");

            // 检查是否有远程仓库
            var remotes = git.remoteList().call();
            result.hasRemoteRepository = !remotes.isEmpty();

            if (!result.hasRemoteRepository) {
                result.warnings.add("当前分支没有设置远程仓库");
                result.canPull = false;
                result.canPush = false;
                return;
            }

            // 设置远程仓库URL
            result.remoteUrl = remotes.get(0).getURIs().get(0).toString();

            result.hasUpstreamBranch = tracking != null;

            if (!result.hasUpstreamBranch) {
                // 有远程仓库但没有设置跟踪分支（典型的 init 类型工作区情况）
                result.isInitTypeWorkspace = true;
                result.warnings.add("当前分支没有设置远程跟踪分支");
                result.canPull = false;

                // 对于 init 类型，需要检查潜在的冲突
                checkInitTypeConflicts(git, result, currentBranch, credentialsProvider, sshCredentialsProvider);
                return;
            }

            // 获取远程仓库名称
            String remoteName = git.getRepository().getConfig()
                    .getString("branch", currentBranch, "remote");
            if (remoteName == null) {
                remoteName = "origin";
            }

            // 获取远程分支名称
            String remoteBranchName = tracking;
            if (remoteBranchName.startsWith(REFS_HEADS_PREFIX)) {
                remoteBranchName = remoteBranchName.substring(REFS_HEADS_PREFIX.length());
            }
            result.remoteBranch = remoteName + "/" + remoteBranchName;

            // 比较本地和远程分支
            String localRef = REFS_HEADS_PREFIX + currentBranch;
            String remoteRef = "refs/remotes/" + remoteName + "/" + remoteBranchName;

            ObjectId localId = git.getRepository().resolve(localRef);
            ObjectId remoteId = git.getRepository().resolve(remoteRef);

            result.isEmptyLocalRepository = localId == null;

            // 检查本地提交情况
            if (localId != null) {
                try {
                    // 检查仓库是否为空
                    Iterable<RevCommit> localCommits = git.log().setMaxCount(1).call();
                    result.hasLocalCommits = localCommits.iterator().hasNext();

                    if (result.hasLocalCommits && remoteId != null) {
                        // 计算本地领先于远程的提交数
                        Iterable<RevCommit> aheadCommits = git.log()
                                .addRange(remoteId, localId)
                                .call();
                        for (RevCommit ignored : aheadCommits) {
                            result.localCommitsAhead++;
                        }

                        // 计算远程领先于本地的提交数
                        Iterable<RevCommit> behindCommits = git.log()
                                .addRange(localId, remoteId)
                                .call();
                        for (RevCommit ignored : behindCommits) {
                            result.remoteCommitsBehind++;
                        }
                        result.hasRemoteCommits = result.remoteCommitsBehind > 0;

                        // 设置需要强制操作的标志
                        result.needsForcePush = result.hasRemoteCommits && result.localCommitsAhead > 0;
                        result.needsForcePull = result.hasUncommittedChanges && result.hasRemoteCommits;

                    } else if (result.hasLocalCommits) {
                        // 本地有提交但远程分支不存在（首次推送情况）
                        result.isFirstPush = true;
                        result.isRemoteRepositoryEmpty = true;
                        Iterable<RevCommit> allCommits = git.log().call();
                        for (RevCommit ignored : allCommits) {
                            result.localCommitsAhead++;
                        }
                        log.debug("Local branch exists but remote branch not found, commits to push: {}", result.localCommitsAhead);
                    }
                } catch (org.eclipse.jgit.api.errors.NoHeadException e) {
                    // 空仓库，没有提交
                    log.debug("Repository has no HEAD (empty repository): {}", e.getMessage());
                    result.hasLocalCommits = false;
                    result.localCommitsAhead = 0;
                    result.isEmptyLocalRepository = true;
                } catch (Exception e) {
                    log.warn("Failed to count commits", e);
                    result.warnings.add("无法统计提交信息: " + e.getMessage());
                }
            }

            // 尝试 fetch 最新的远程状态（用于更准确的检测）
            boolean fetchSuccess = false;
            try {
                var fetchCommand = git.fetch().setDryRun(false);
                // 设置认证信息 - 支持SSH和其他认证方式
                if (sshCredentialsProvider != null) {
                    fetchCommand.setTransportConfigCallback(sshCredentialsProvider);
                } else if (credentialsProvider != null) {
                    fetchCommand.setCredentialsProvider(credentialsProvider);
                }
                fetchCommand.call();
                log.debug("Fetched latest remote status for conflict detection");
                fetchSuccess = true;
                result.canConnectToRemote = true;

                // fetch 成功后重新解析远程分支ID
                remoteId = git.getRepository().resolve(remoteRef);
            } catch (RefNotAdvertisedException e) {
                log.debug("Remote branch does not exist: {}", e.getMessage());
                result.isRemoteRepositoryEmpty = true;
                result.isFirstPush = true;
                result.canConnectToRemote = true;
            } catch (Exception fetchEx) {
                log.debug("Failed to fetch remote status, using cached refs: {}", fetchEx.getMessage());
                result.canConnectToRemote = false;
                // 只有在真正需要远程状态时才添加警告
                if (credentialsProvider != null || sshCredentialsProvider != null) {
                    result.warnings.add("无法获取最新远程状态: " + fetchEx.getMessage());
                    result.hasAuthenticationIssue = true;
                } else {
                    log.debug("No credentials provided for fetch, skipping remote status update");
                }
            }

            // 设置操作可行性
            determineOperationCapabilities(result, localId, remoteId, fetchSuccess);

            // 执行冲突检测
            performIntelligentConflictDetection(git, result, localId, remoteId);

        } catch (Exception e) {
            log.warn("Failed to check remote status", e);
            result.warnings.add("无法检查远程状态: " + e.getMessage());
            result.hasAuthenticationIssue = true;
            // 发生错误时，保守设置操作能力
            result.canPull = false;
            result.canPush = false;
        }
    }

    /**
     * 根据本地和远程状态确定操作能力
     */
    private static void determineOperationCapabilities(GitStatusCheck result, ObjectId localId, ObjectId remoteId, boolean fetchSuccess) {
        // Pull 操作判断：
        // 1. 远程分支必须存在
        // 2. 如果 fetch 失败，则不建议拉取
        result.canPull = remoteId != null && fetchSuccess;

        // 检查远程仓库状态并添加相应建议
        if (remoteId == null) {
            // 远程分支不存在，说明远程仓库为空
            result.isRemoteRepositoryEmpty = true;
            result.suggestions.add("远程仓库为空");
            result.suggestions.add("远程仓库没有同名分支");
            result.suggestions.add("等待首次推送内容");
        }

        // Push 操作判断：
        // 1. 必须有本地提交
        // 2. 不能有未提交的变更（除非是 init 类型的首次推送）
        if (!result.hasLocalCommits) {
            result.canPush = false;
        } else if (result.hasUncommittedChanges) {
            // 有未提交变更时，只有在特殊情况下才能推送
            result.canPush = result.isInitTypeWorkspace; // init 类型的首次推送可能允许
        } else {
            // 没有未提交变更，可以推送
            result.canPush = true;
        }

        // 设置需要强制操作的标志
        result.needsForcePush = result.hasRemoteCommits && result.localCommitsAhead > 0;
        result.needsForcePull = result.hasUncommittedChanges && result.hasRemoteCommits;

        // 如果远程有新提交，推送可能会失败
        if (result.hasRemoteCommits && result.canPush) {
            result.warnings.add("远程仓库有新提交，推送可能失败，建议先拉取");
        }

        // 如果本地仓库为空，则无法进行任何操作
        if (localId == null) {
            result.isEmptyLocalRepository = true;
            result.canPush = false;
            if (remoteId != null) {
                result.canPull = fetchSuccess; // 空本地仓库可以拉取远程内容
            }
        }
    }

    /**
     * 检查 init 类型工作区的潜在冲突
     * 当本地仓库已有内容，但要绑定到已存在的远程分支时
     */
    private static void checkInitTypeConflicts(Git git, GitStatusCheck result, String currentBranch,
                                               CredentialsProvider credentialsProvider,
                                               SshCredentialsProvider sshCredentialsProvider) {
        try {
            // 检查是否有本地提交
            boolean hasLocalCommits = false;
            try {
                Iterable<RevCommit> localCommits = git.log().setMaxCount(1).call();
                hasLocalCommits = localCommits.iterator().hasNext();
            } catch (NoHeadException e) {
                // 空仓库，没有提交
                log.debug("Repository has no HEAD (empty repository): {}", e.getMessage());
            }

            if (hasLocalCommits) {
                result.hasLocalCommits = true;
                // 计算本地提交数（最多检查前100个）
                try {
                    Iterable<RevCommit> allCommits = git.log().setMaxCount(100).call();
                    for (RevCommit ignored : allCommits) {
                        result.localCommitsAhead++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to count local commits", e);
                    result.localCommitsAhead = 1; // 保守估计
                }

                // 尝试 fetch 远程分支信息来检查冲突
                try {
                    // 尝试获取远程分支信息（不会修改工作区）
                    var fetchCommand = git.fetch().setDryRun(true);

                    // 设置认证信息 - 支持SSH和其他认证方式
                    if (sshCredentialsProvider != null) {
                        fetchCommand.setTransportConfigCallback(sshCredentialsProvider);
                    } else if (credentialsProvider != null) {
                        fetchCommand.setCredentialsProvider(credentialsProvider);
                    }

                    fetchCommand.call();

                    // 检查远程是否有同名分支
                    String remoteRef = "refs/remotes/origin/" + currentBranch;
                    ObjectId remoteId = git.getRepository().resolve(remoteRef);

                    if (remoteId != null) {
                        // 远程已有同名分支，可能存在冲突
                        result.warnings.add("检测到远程仓库已存在同名分支，可能存在文件冲突");
                        result.suggestions.add("建议先备份本地文件，然后谨慎处理合并");

                        // 检查具体的文件冲突
                        checkFileConflicts(git, result, remoteId);
                    } else {
                        // 远程没有同名分支，相对安全
                        result.suggestions.add("远程仓库没有同名分支，首次推送相对安全");
                    }

                } catch (org.eclipse.jgit.api.errors.RefNotAdvertisedException e) {
                    // 远程分支不存在，这是正常情况
                    log.debug("Remote branch does not exist: {}", e.getMessage());
                    result.suggestions.add("远程仓库没有同名分支，首次推送相对安全");
                } catch (Exception fetchEx) {
                    // fetch 失败可能是网络问题或认证问题
                    log.debug("Cannot fetch remote info for conflict check", fetchEx);
                    result.warnings.add("无法获取远程分支信息进行冲突检测");
                    result.suggestions.add("建议检查网络连接和认证信息");
                }

                // 设置推送能力：有本地提交且没有未提交变更时可以推送
                result.canPush = !result.hasUncommittedChanges;

            } else {
                // 没有本地提交，无法推送
                result.hasLocalCommits = false;
                result.localCommitsAhead = 0;
                result.canPush = false;
                result.suggestions.add("请先创建本地提交，然后进行首次推送");
            }

        } catch (Exception e) {
            log.warn("Failed to check init type conflicts", e);
            result.warnings.add("检查 init 类型冲突失败: " + e.getMessage());
            result.canPush = false;
        }
    }

    /**
     * 检查文件级别的冲突
     */
    private static void checkFileConflicts(Git git, GitStatusCheck result, ObjectId remoteCommitId) {
        try {
            // 获取本地文件列表
            ObjectId localCommitId = git.getRepository().resolve("HEAD");
            if (localCommitId == null) return;

            // 比较本地和远程的文件差异
            var diffs = git.diff()
                    .setOldTree(prepareTreeParser(git.getRepository(), remoteCommitId))
                    .setNewTree(prepareTreeParser(git.getRepository(), localCommitId))
                    .call();

            List<String> conflictFiles = new ArrayList<>();

            for (var diff : diffs) {
                String fileName = diff.getNewPath();
                // 检查是否是同一文件的不同版本（潜在冲突）
                if (diff.getChangeType() == org.eclipse.jgit.diff.DiffEntry.ChangeType.MODIFY) {
                    conflictFiles.add(fileName);
                }
            }

            // 设置冲突状态
            result.hasFileConflicts = !conflictFiles.isEmpty();
            result.conflictingFilesCount = conflictFiles.size();
            result.conflictingFiles.addAll(conflictFiles);

            if (result.hasFileConflicts) {
                result.warnings.add("检测到 " + result.conflictingFilesCount + " 个文件可能存在内容冲突");
                result.suggestions.add("冲突文件: " + String.join(", ", conflictFiles.subList(0, Math.min(5, conflictFiles.size()))));
                if (conflictFiles.size() > 5) {
                    result.suggestions.add("还有 " + (conflictFiles.size() - 5) + " 个文件可能冲突");
                }
                result.suggestions.add("建议使用 'git merge' 或手动解决冲突");
            }

        } catch (Exception e) {
            log.debug("Failed to check file conflicts", e);
            // 文件冲突检查失败不影响主要功能
        }
    }

    /**
     * 辅助方法：准备树解析器
     */
    private static AbstractTreeIterator prepareTreeParser(
            Repository repository, ObjectId objectId) throws Exception {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(objectId);
            ObjectId treeId = commit.getTree().getId();
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (var reader = repository.newObjectReader()) {
                treeParser.reset(reader, treeId);
            }
            return treeParser;
        } catch (Exception ex) {
            log.warn("Failed to prepare tree parser", ex);
            throw ex;
        }
    }

    private static void generateSuggestions(GitStatusCheck result, String operationType) {
        // 根据操作类型生成建议
        switch (operationType.toLowerCase()) {
            case "commit":
                generateCommitSuggestions(result);
                break;
            case "push":
                generatePushSuggestions(result);
                break;
            case "pull":
                generatePullSuggestions(result);
                break;
            default:
                result.suggestions.add("未知的操作类型: " + operationType);
        }
    }

    private static void generateCommitSuggestions(GitStatusCheck result) {
        if (result.canCommit) {
            StringBuilder suggestion = new StringBuilder();
            suggestion.append("可以提交变更");

            int totalChanges = 0;
            List<String> changeTypes = new ArrayList<>();

            if (result.hasUncommittedChanges && result.uncommittedCount > 0) {
                totalChanges += result.uncommittedCount;
                changeTypes.add(result.uncommittedCount + " 个文件变更");
            }

            if (result.hasUntrackedFiles && result.untrackedCount > 0) {
                totalChanges += result.untrackedCount;
                changeTypes.add(result.untrackedCount + " 个未跟踪文件");
            }

            if (totalChanges > 0) {
                suggestion.append("：").append(String.join("、", changeTypes));
                suggestion.append("（共 ").append(totalChanges).append(" 个文件）");
            }

            result.suggestions.add(suggestion.toString());

            // 添加具体的操作建议
            if (result.hasUntrackedFiles) {
                result.suggestions.add("未跟踪文件将被添加到版本控制中");
            }
            if (result.hasUncommittedChanges) {
                result.suggestions.add("已修改的文件将被提交");
            }
        } else {
            result.suggestions.add("没有要提交的变更");
            result.suggestions.add("所有文件都已是最新状态且已提交");
        }
    }

    private static void generatePushSuggestions(GitStatusCheck result) {
        if (result.hasUncommittedChanges) {
            result.warnings.add("有未提交的变更，无法推送");
            result.suggestions.add("请先提交所有变更，然后再推送");
            return;
        }

        if (!result.hasLocalCommits) {
            if (result.hasUntrackedFiles) {
                result.warnings.add("有未跟踪文件但没有提交");
                result.suggestions.add("请先提交未跟踪文件，然后再推送");
            } else {
                result.warnings.add("没有本地提交需要推送");
                result.suggestions.add("本地仓库已与远程仓库同步");
            }
            return;
        }

        // 分析推送场景
        if (result.isInitTypeWorkspace || result.isFirstPush || result.isRemoteRepositoryEmpty) {
            // 首次推送或初始化类型工作区
            handleFirstPushSuggestions(result);
        } else if (result.needsForcePush) {
            // 需要强制推送的情况（有分歧历史）
            result.warnings.add("⚠️ 本地和远程有分歧的提交历史");
            result.suggestions.add("本地领先 " + result.localCommitsAhead + " 个提交");
            result.suggestions.add("远程领先 " + result.remoteCommitsBehind + " 个提交");
            result.suggestions.add("建议先拉取远程变更进行合并，或使用强制推送");
            result.suggestions.add("强制推送将覆盖远程的 " + result.remoteCommitsBehind + " 个提交");
        } else if (result.hasRemoteCommits) {
            // 远程有新提交，但可以快进合并
            result.warnings.add("远程仓库有新的提交");
            result.suggestions.add("远程领先 " + result.remoteCommitsBehind + " 个提交");
            result.suggestions.add("建议先拉取远程变更，然后再推送");
            result.suggestions.add("这样可以避免推送冲突");
        } else {
            // 正常推送情况
            result.suggestions.add("可以安全推送 " + result.localCommitsAhead + " 个本地提交");
            result.suggestions.add("推送后远程仓库将与本地同步");
        }
    }

    /**
     * 处理首次推送的建议
     */
    private static void handleFirstPushSuggestions(GitStatusCheck result) {
        if (result.hasFileConflicts) {
            result.warnings.add("⚠️ 首次推送可能覆盖远程分支已有内容");
            result.suggestions.add("检测到 " + result.conflictingFilesCount + " 个文件可能冲突");
            result.suggestions.add("建议使用 --force-with-lease 进行安全的强制推送");
            result.suggestions.add("或者先拉取远程分支内容进行手动合并");
            result.suggestions.add("推送前请确认要覆盖的远程文件");
        } else if (result.isRemoteRepositoryEmpty) {
            result.suggestions.add("✅ 远程仓库为空，首次推送安全");
            result.suggestions.add("将推送 " + result.localCommitsAhead + " 个本地提交到远程仓库");
            result.suggestions.add("推送后将自动设置上游分支跟踪");
        } else {
            result.suggestions.add("检测到首次推送情况");
            result.suggestions.add("将推送 " + result.localCommitsAhead + " 个本地提交");
            result.suggestions.add("推送后将设置上游分支跟踪");
        }
    }

    private static void generatePullSuggestions(GitStatusCheck result) {
        // 直接使用布尔属性而不是文本判断
        if (!result.hasUpstreamBranch) {
            // 检查是否是 init 类型且可能有冲突的情况
            if (result.hasFileConflicts) {
                result.warnings.add("⚠️ 无法直接拉取：检测到潜在的文件冲突");
                result.suggestions.add("建议先手动处理文件冲突：");
                result.suggestions.add("1. 备份当前本地文件");
                result.suggestions.add("2. 使用 git fetch origin 获取远程分支");
                result.suggestions.add("3. 手动合并冲突文件");
                result.suggestions.add("4. 创建合并提交");
            } else {
                result.warnings.add("无法拉取：当前分支没有设置远程跟踪分支");
                result.suggestions.add("请先配置远程仓库并设置上游分支");
                result.suggestions.add("或者先进行首次推送以建立跟踪关系");
            }
            return;
        }

        // 直接使用布尔属性检查空仓库状态
        if (result.isRemoteRepositoryEmpty) {
            result.suggestions.add("📍 远程仓库状态：远程仓库当前为空");
            result.suggestions.add("虽然可以尝试拉取，但远程仓库没有内容可拉取");
            result.suggestions.add("建议先向远程仓库推送本地内容");
        }

        if (result.hasUncommittedChanges) {
            result.warnings.add("有未提交的变更，拉取可能导致冲突");
            result.suggestions.add("建议先提交或暂存本地变更");
            result.suggestions.add("或者选择强制拉取（将丢弃本地未提交变更）");
        } else if (!result.hasRemoteCommits && !result.isRemoteRepositoryEmpty) {
            result.suggestions.add("本地仓库已是最新状态");
        } else if (result.hasRemoteCommits) {
            result.suggestions.add("可以安全拉取 " + result.remoteCommitsBehind + " 个远程提交");
        }

        if (result.hasUntrackedFiles) {
            result.suggestions.add("注意：有 " + result.untrackedCount + " 个未跟踪文件可能与远程变更冲突");
        }
    }

    /**
     * 执行冲突检测
     * 通过分析本地和远程的变更，判断是否存在实际冲突以及是否可以自动合并
     */
    private static void performIntelligentConflictDetection(Git git, GitStatusCheck result, ObjectId localId, ObjectId remoteId) {
        try {
            // 如果本地或远程仓库为空，则无法进行智能冲突检测
            if (localId == null || remoteId == null) {
                result.hasActualConflicts = false;
                result.canAutoMerge = false;
                return;
            }

            // 查找共同的基础提交（merge base）
            ObjectId mergeBase = findMergeBase(git, localId, remoteId);

            if (mergeBase == null) {
                // 没有共同基础，可能是完全不同的历史
                result.hasActualConflicts = true;
                result.canAutoMerge = false;
                result.warnings.add("本地和远程分支没有共同的提交历史");
                return;
            }

            // 如果merge base等于远程ID，说明远程是本地的子集，可以快进推送
            if (mergeBase.equals(remoteId)) {
                result.hasActualConflicts = false;
                result.canAutoMerge = true;
                result.suggestions.add("✅ 可以安全推送（快进合并）");
                return;
            }

            // 如果merge base等于本地ID，说明本地是远程的子集，可以快进拉取
            if (mergeBase.equals(localId)) {
                result.hasActualConflicts = false;
                result.canAutoMerge = true;
                result.suggestions.add("✅ 可以安全拉取（快进合并）");
                return;
            }

            // 分析文件级别的冲突
            analyzeFileConflicts(git, result, mergeBase, localId, remoteId);

        } catch (Exception e) {
            log.debug("智能冲突检测失败", e);
            result.hasActualConflicts = false;
            result.canAutoMerge = false;
        }
    }

    /**
     * 查找两个提交的合并基础
     */
    private static ObjectId findMergeBase(Git git, ObjectId commit1, ObjectId commit2) {
        try {
            try (RevWalk walk = new RevWalk(git.getRepository())) {
                RevCommit c1 = walk.parseCommit(commit1);
                RevCommit c2 = walk.parseCommit(commit2);

                walk.setRevFilter(org.eclipse.jgit.revwalk.filter.RevFilter.MERGE_BASE);
                walk.markStart(c1);
                walk.markStart(c2);

                RevCommit mergeBase = walk.next();
                return mergeBase != null ? mergeBase.getId() : null;
            }
        } catch (Exception e) {
            log.debug("Failed to find merge base", e);
            return null;
        }
    }

    /**
     * 分析文件级别的冲突
     */
    private static void analyzeFileConflicts(Git git, GitStatusCheck result, ObjectId mergeBase,
                                             ObjectId localId, ObjectId remoteId) {
        try {
            // 获取从merge base到本地的变更
            var localDiffs = git.diff()
                    .setOldTree(prepareTreeParser(git.getRepository(), mergeBase))
                    .setNewTree(prepareTreeParser(git.getRepository(), localId))
                    .call();

            // 获取从merge base到远程的变更
            var remoteDiffs = git.diff()
                    .setOldTree(prepareTreeParser(git.getRepository(), mergeBase))
                    .setNewTree(prepareTreeParser(git.getRepository(), remoteId))
                    .call();

            // 分析冲突情况
            List<String> localChangedFiles = new ArrayList<>();
            List<String> remoteChangedFiles = new ArrayList<>();
            List<String> conflictFiles = new ArrayList<>();
            List<String> newFiles = new ArrayList<>();

            // 收集本地变更的文件
            for (var diff : localDiffs) {
                String filePath = diff.getNewPath();
                localChangedFiles.add(filePath);
                if (diff.getChangeType() == org.eclipse.jgit.diff.DiffEntry.ChangeType.ADD) {
                    newFiles.add(filePath);
                }
            }

            // 收集远程变更的文件并检查冲突
            for (var diff : remoteDiffs) {
                String filePath = diff.getNewPath();
                remoteChangedFiles.add(filePath);

                // 如果本地也修改了同一个文件，可能存在冲突
                if (localChangedFiles.contains(filePath)) {
                    conflictFiles.add(filePath);
                }

                if (diff.getChangeType() == org.eclipse.jgit.diff.DiffEntry.ChangeType.ADD) {
                    newFiles.add(filePath);
                }
            }

            // 设置检测结果
            result.hasActualConflicts = !conflictFiles.isEmpty();
            result.conflictingFilesCount = conflictFiles.size();
            result.conflictingFiles.addAll(conflictFiles);

            // 判断是否只有新文件
            result.hasOnlyNewFiles = conflictFiles.isEmpty() && !newFiles.isEmpty();

            // 判断是否为非重叠变更
            result.hasNonOverlappingChanges = conflictFiles.isEmpty() &&
                    (!localChangedFiles.isEmpty() || !remoteChangedFiles.isEmpty());

            // 判断是否可以自动合并
            result.canAutoMerge = conflictFiles.isEmpty() || result.hasOnlyNewFiles;

            // 添加详细建议
            if (result.hasActualConflicts) {
                result.warnings.add("检测到 " + conflictFiles.size() + " 个文件存在实际冲突");
                result.suggestions.add("冲突文件: " + String.join(", ",
                        conflictFiles.subList(0, Math.min(3, conflictFiles.size()))));
                if (conflictFiles.size() > 3) {
                    result.suggestions.add("还有 " + (conflictFiles.size() - 3) + " 个文件存在冲突");
                }
                result.canAutoMerge = false;
            } else if (result.hasNonOverlappingChanges) {
                result.suggestions.add("✅ 检测到非重叠变更，可以安全自动合并");
            } else if (result.hasOnlyNewFiles) {
                result.suggestions.add("✅ 只包含新文件，可以安全合并");
            }

        } catch (Exception e) {
            log.debug("文件冲突分析失败", e);
            result.hasActualConflicts = true;
            result.canAutoMerge = false;
        }
    }
}
