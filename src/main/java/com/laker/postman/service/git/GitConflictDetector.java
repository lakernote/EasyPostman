package com.laker.postman.service.git;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

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
     * Git状态检查结果
     */
    @Getter
    public static class GitStatusCheck {
        private boolean hasUncommittedChanges = false;
        private boolean hasUntrackedFiles = false;
        private boolean hasLocalCommits = false;
        private boolean hasRemoteCommits = false;
        private boolean canCommit = false;
        private boolean canPush = false;
        private boolean canPull = false;
        private final List<String> warnings = new ArrayList<>();
        private final List<String> suggestions = new ArrayList<>();

        // 详细信息
        private int uncommittedCount = 0;
        private int untrackedCount = 0;
        private int localCommitsAhead = 0;
        private int remoteCommitsBehind = 0;
        private final List<String> uncommittedFiles = new ArrayList<>();
        private final List<String> untrackedFilesList = new ArrayList<>();
    }

    /**
     * 检查Git仓库状态，判断是否可以执行指定操作
     */
    public static GitStatusCheck checkGitStatus(String workspacePath, String operationType) {
        GitStatusCheck result = new GitStatusCheck();

        try (Git git = Git.open(new File(workspacePath))) {
            // 检查本地状态
            Status status = git.status().call();
            checkLocalStatus(status, result);

            // 检查远程状态
            checkRemoteStatus(git, result);

            // 根据操作类型生成建议
            generateSuggestions(result, operationType);

        } catch (Exception e) {
            log.error("Failed to check git status", e);
            result.warnings.add("无法检查Git状态: " + e.getMessage());
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
            result.uncommittedCount = status.getModified().size() +
                                    status.getChanged().size() +
                                    status.getRemoved().size() +
                                    status.getMissing().size();
            result.uncommittedFiles.addAll(status.getModified());
            result.uncommittedFiles.addAll(status.getChanged());
            result.uncommittedFiles.addAll(status.getRemoved());
            result.uncommittedFiles.addAll(status.getMissing());
        }

        // 检查未跟踪的文件
        result.hasUntrackedFiles = !status.getUntracked().isEmpty();
        if (result.hasUntrackedFiles) {
            result.untrackedCount = status.getUntracked().size();
            result.untrackedFilesList.addAll(status.getUntracked());
        }

        // 检查是否可以提交
        result.canCommit = result.hasUncommittedChanges || result.hasUntrackedFiles;
    }

    private static void checkRemoteStatus(Git git, GitStatusCheck result) {
        try {
            String currentBranch = git.getRepository().getBranch();
            String tracking = git.getRepository().getConfig()
                    .getString("branch", currentBranch, "merge");

            // 检查是否有远程仓库
            var remotes = git.remoteList().call();
            boolean hasRemote = !remotes.isEmpty();

            if (tracking == null) {
                if (!hasRemote) {
                    // 没有远程仓库，也没有跟踪分支
                    result.warnings.add("当前分支没有设置远程仓库");
                    result.canPull = false;
                    result.canPush = false;
                } else {
                    // 有远程仓库但没有设置跟踪分支（典型的 init 类型工作区情况）
                    result.warnings.add("当前分支没有设置远程跟踪分支");
                    result.canPull = false;

                    // 对于 init 类型，如果有本地提交且有远程仓库，可以尝试推送
                    // 检查是否有本地提交
                    try {
                        Iterable<RevCommit> localCommits = git.log().setMaxCount(1).call();
                        boolean hasLocalCommits = localCommits.iterator().hasNext();
                        result.canPush = hasLocalCommits && !result.hasUncommittedChanges;

                        if (hasLocalCommits) {
                            result.hasLocalCommits = true;
                            // 计算本地提交数（最多检查前100个）
                            Iterable<RevCommit> allCommits = git.log().setMaxCount(100).call();
                            for (RevCommit ignored : allCommits) {
                                result.localCommitsAhead++;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to check local commits", e);
                        result.canPush = false;
                    }
                }
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

            // 比较本地和远程分支
            String localRef = REFS_HEADS_PREFIX + currentBranch;
            String remoteRef = "refs/remotes/" + remoteName + "/" + remoteBranchName;

            ObjectId localId = git.getRepository().resolve(localRef);
            ObjectId remoteId = git.getRepository().resolve(remoteRef);

            if (localId != null && remoteId != null) {
                // 计算本地领先多少提交
                Iterable<RevCommit> localCommits = git.log()
                        .addRange(remoteId, localId)
                        .call();
                for (RevCommit ignored : localCommits) {
                    result.localCommitsAhead++;
                }
                result.hasLocalCommits = result.localCommitsAhead > 0;

                // 计算远程领先多少提交
                Iterable<RevCommit> remoteCommits = git.log()
                        .addRange(localId, remoteId)
                        .call();
                for (RevCommit ignored : remoteCommits) {
                    result.remoteCommitsBehind++;
                }
                result.hasRemoteCommits = result.remoteCommitsBehind > 0;
            } else if (localId != null && remoteId == null) {
                // 本地分支存在但远程分支不存在（首次推送的情况）
                try {
                    Iterable<RevCommit> localCommits = git.log().call();
                    for (RevCommit ignored : localCommits) {
                        result.localCommitsAhead++;
                    }
                    result.hasLocalCommits = result.localCommitsAhead > 0;
                } catch (Exception e) {
                    log.warn("Failed to count local commits", e);
                }
            }

            // 设置操作可行性
            result.canPush = result.hasLocalCommits && !result.hasUncommittedChanges;
            result.canPull = remoteId != null; // 只有远程分支存在时才能拉取

        } catch (Exception e) {
            log.warn("Failed to check remote status", e);
            result.warnings.add("无法检查远程状态: " + e.getMessage());
        }
    }

    private static void generateSuggestions(GitStatusCheck result, String operationType) {
        switch (operationType.toUpperCase()) {
            case "COMMIT" -> generateCommitSuggestions(result);
            case "PUSH" -> generatePushSuggestions(result);
            case "PULL" -> generatePullSuggestions(result);
            default -> {
                // 默认情况不需要特殊处理
            }
        }
    }

    private static void generateCommitSuggestions(GitStatusCheck result) {
        if (!result.canCommit) {
            result.warnings.add("没有可提交的变更");
            result.suggestions.add("请先修改一些文件或添加新文件");
        } else {
            if (result.hasUntrackedFiles) {
                result.suggestions.add("检测到 " + result.untrackedCount + " 个未跟踪文件，将会被添加到提交中");
            }
            if (result.hasUncommittedChanges) {
                result.suggestions.add("检测到 " + result.uncommittedCount + " 个已修改文件");
            }
        }
    }

    private static void generatePushSuggestions(GitStatusCheck result) {
        if (result.hasUncommittedChanges) {
            result.warnings.add("有未提交的变更，无法推送");
            result.suggestions.add("请先提交所有变更，然后再推送");
        } else if (!result.hasLocalCommits) {
            result.warnings.add("没有本地提交需要推送");
            result.suggestions.add("本地仓库已与远程仓库同步");
        } else {
            // 检查是否为首次推送情况（init 类型工作区常见）
            boolean isFirstPush = result.warnings.stream()
                    .anyMatch(warning -> warning.contains("没有设置远程跟踪分支"));

            if (isFirstPush) {
                result.suggestions.add("检测到首次推送情况，将设置上游分支");
                result.suggestions.add("可以推送 " + result.localCommitsAhead + " 个本地提交到远程仓库");
            } else if (result.hasRemoteCommits) {
                result.warnings.add("远程仓库有新的提交，推送可能失败");
                result.suggestions.add("建议先拉取远程变更，然后再推送");
            } else {
                result.suggestions.add("可以安全推送 " + result.localCommitsAhead + " 个本地提交");
            }
        }
    }

    private static void generatePullSuggestions(GitStatusCheck result) {
        // 检查是否没有远程跟踪分支
        boolean noTracking = result.warnings.stream()
                .anyMatch(warning -> warning.contains("没有设置远程跟踪分支"));

        if (noTracking) {
            result.warnings.add("无法拉取：当前分支没有设置远程跟踪分支");
            result.suggestions.add("请先配置远程仓库并设置上游分支");
            result.suggestions.add("或者先进行首次推送以建立跟踪关系");
            return;
        }

        if (result.hasUncommittedChanges) {
            result.warnings.add("有未提交的变更，拉取可能导致冲突");
            result.suggestions.add("建议先提交或暂存本地变更");
            result.suggestions.add("或者选择强制拉取（将丢弃本地未提交变更）");
        } else if (!result.hasRemoteCommits) {
            result.suggestions.add("本地仓库已是最新状态");
        } else {
            result.suggestions.add("可以安全拉取 " + result.remoteCommitsBehind + " 个远程提交");
        }

        if (result.hasUntrackedFiles) {
            result.suggestions.add("注意：有 " + result.untrackedCount + " 个未跟踪文件可能与远程变更冲突");
        }
    }
}
