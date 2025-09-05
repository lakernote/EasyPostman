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

                    // 对于 init 类型，需要检查潜在的冲突
                    checkInitTypeConflicts(git, result, currentBranch);
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

            // 尝试 fetch 最新的远程状态（静默获取，不修改工作区）
            try {
                git.fetch().setDryRun(false).call();
                log.debug("Fetched latest remote status for conflict detection");
            } catch (Exception fetchEx) {
                log.debug("Failed to fetch remote status, using cached refs", fetchEx);
            }

            // 比较本地和远程分支
            String localRef = REFS_HEADS_PREFIX + currentBranch;
            String remoteRef = "refs/remotes/" + remoteName + "/" + remoteBranchName;

            ObjectId localId = git.getRepository().resolve(localRef);
            ObjectId remoteId = git.getRepository().resolve(remoteRef);

            // 首先检查是否有本地提交（无论远程状态如何）
            if (localId != null) {
                try {
                    Iterable<RevCommit> allLocalCommits = git.log().call();
                    for (RevCommit ignored : allLocalCommits) {
                        result.localCommitsAhead++;
                    }
                    result.hasLocalCommits = result.localCommitsAhead > 0;
                } catch (Exception e) {
                    log.warn("Failed to count all local commits", e);
                }
            }

            if (localId != null && remoteId != null) {
                // 重新计算本地领先于远程的提交数
                result.localCommitsAhead = 0; // 重置计数
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
                // localCommitsAhead 已经在上面计算过了，这里不需要重复计算
                log.debug("Local branch exists but remote branch not found, commits to push: {}", result.localCommitsAhead);
            }

            // 设置操作可行性
            result.canPush = result.hasLocalCommits && !result.hasUncommittedChanges;
            result.canPull = remoteId != null; // 只有远程分支存在时才能拉取

        } catch (Exception e) {
            log.warn("Failed to check remote status", e);
            result.warnings.add("无法检查远程状态: " + e.getMessage());
        }
    }

    /**
     * 检查 init 类型工作区的潜在冲突
     * 当本地仓库已有内容，但要绑定到已存在的远程分支时
     */
    private static void checkInitTypeConflicts(Git git, GitStatusCheck result, String currentBranch) {
        try {
            // 检查是否有本地提交
            Iterable<RevCommit> localCommits = git.log().setMaxCount(1).call();
            boolean hasLocalCommits = localCommits.iterator().hasNext();

            if (hasLocalCommits) {
                result.hasLocalCommits = true;
                // 计算本地提交数（最多检查前100个）
                Iterable<RevCommit> allCommits = git.log().setMaxCount(100).call();
                for (RevCommit ignored : allCommits) {
                    result.localCommitsAhead++;
                }

                // 尝试 fetch 远程分支信息来检查冲突
                try {
                    // 尝试获取远程分支信息（不会修改工作区）
                    var fetchCommand = git.fetch().setDryRun(true);
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

                } catch (Exception fetchEx) {
                    // fetch 失败可能是网络问题或认证问题
                    log.debug("Cannot fetch remote info for conflict check", fetchEx);
                    result.warnings.add("无法获取远程分支信息进行冲突检测");
                    result.suggestions.add("建议检查网络连接和认证信息");
                }

                // 设置推送能力
                result.canPush = !result.hasUncommittedChanges;

            } else {
                // 没有本地提交，无法推送
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

            int conflictingFiles = 0;
            List<String> conflictFiles = new ArrayList<>();

            for (var diff : diffs) {
                String fileName = diff.getNewPath();
                // 检查是否是同一文件的不同版本（潜在冲突）
                if (diff.getChangeType() == org.eclipse.jgit.diff.DiffEntry.ChangeType.MODIFY) {
                    conflictingFiles++;
                    conflictFiles.add(fileName);
                }
            }

            if (conflictingFiles > 0) {
                result.warnings.add("检测到 " + conflictingFiles + " 个文件可能存在内容冲突");
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
    private static org.eclipse.jgit.treewalk.AbstractTreeIterator prepareTreeParser(
            org.eclipse.jgit.lib.Repository repository, ObjectId objectId) throws Exception {
        try (org.eclipse.jgit.revwalk.RevWalk walk = new org.eclipse.jgit.revwalk.RevWalk(repository)) {
            org.eclipse.jgit.revwalk.RevCommit commit = walk.parseCommit(objectId);
            ObjectId treeId = commit.getTree().getId();
            org.eclipse.jgit.treewalk.CanonicalTreeParser treeParser = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
            try (var reader = repository.newObjectReader()) {
                treeParser.reset(reader, treeId);
            }
            return treeParser;
        }
    }

    private static void generateSuggestions(GitStatusCheck result, String operationType) {
        // 不要清空现有的警告和建议，因为它们包含了重要的状态检测信息
        // 只在必要时添加新的建议

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
                result.getSuggestions().add("未知的操作类型: " + operationType);
        }
    }

    private static void generateCommitSuggestions(GitStatusCheck result) {
        if (result.hasUncommittedChanges) {
            result.suggestions.add("可以提交 " + result.uncommittedCount + " 个变更");
        } else {
            result.suggestions.add("没有要提交的变更");
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

            // 检查是否有潜在的文件冲突
            boolean hasConflictWarning = result.warnings.stream()
                    .anyMatch(warning -> warning.contains("可能存在文件冲突") || warning.contains("已存在同名分支"));

            if (isFirstPush) {
                if (hasConflictWarning) {
                    result.warnings.add("⚠️ 首次推送可能覆盖远程分支已有内容");
                    result.suggestions.add("建议使用 --force-with-lease 进行安全的强制推送");
                    result.suggestions.add("或者先拉取远程分支内容进行手动合并");
                    result.suggestions.add("推送前请确认要覆盖的远程文件");
                } else {
                    result.suggestions.add("检测到首次推送情况，将设置上游分支");
                    result.suggestions.add("可以推送 " + result.localCommitsAhead + " 个本地提交到远程仓库");
                }
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
            // 检查是否是 init 类型且可能有冲突的情况
            boolean hasConflictWarning = result.warnings.stream()
                    .anyMatch(warning -> warning.contains("可能存在文件冲突") || warning.contains("已存在同名分支"));

            if (hasConflictWarning) {
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
