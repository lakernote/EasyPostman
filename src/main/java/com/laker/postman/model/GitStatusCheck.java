package com.laker.postman.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Git状态检查结果
 */
public class GitStatusCheck {
    // 基本状态
    public boolean hasUncommittedChanges = false; // 是否有未提交的更改
    public boolean hasUntrackedFiles = false; // 是否有未跟踪的文件
    public boolean hasLocalCommits = false; // 是否有本地提交
    public boolean hasRemoteCommits = false; // 是否有远程提交

    // 操作能力
    public boolean canCommit = false; // 是否可以提交
    public boolean canPush = false; // 是否可以推送
    public boolean canPull = false; // 是否可以拉取

    // 远程仓库状态
    public boolean hasRemoteRepository = false; // 是否存在远程仓库
    public boolean hasUpstreamBranch = false; // 是否有上游分支
    public boolean isRemoteRepositoryEmpty = false; // 远程仓库是否为空
    public boolean isFirstPush = false; // 是否是第一次推送

    // 冲突和问题检测
    public boolean hasConflicts = false; // 是否存在冲突
    public boolean hasFileConflicts = false; // 是否有文件冲突
    public boolean needsForcePush = false; // 是否需要强制推送
    public boolean needsForcePull = false; // 是否需要强制拉取

    // 冲突检测
    public boolean hasActualConflicts = false; // 是否存在实际内容冲突
    public boolean canAutoMerge = false; // 是否可以自动合并
    public boolean hasOnlyNewFiles = false; // 是否只是新文件（无冲突）
    public boolean hasNonOverlappingChanges = false; // 是否为非重叠变更

    // 网络和认证状态
    public boolean canConnectToRemote = false; // 是否能连接到远程仓库
    public boolean hasAuthenticationIssue = false; // 是否存在认证问题

    // 操作类型判断
    public boolean isInitTypeWorkspace = false; // 是否是初始化类型的工作区
    public boolean isEmptyLocalRepository = false; // 本地仓库是否为空

    // 详细数量信息
    public int uncommittedCount = 0;
    public int untrackedCount = 0;
    public int localCommitsAhead = 0;
    public int remoteCommitsBehind = 0;
    public int conflictingFilesCount = 0;

    // 文件列表
    public final List<String> uncommittedFiles = new ArrayList<>();
    public final List<String> untrackedFilesList = new ArrayList<>();
    public final List<String> conflictingFiles = new ArrayList<>();

    // 消息列表（保留用于详细说明）
    public final List<String> warnings = new ArrayList<>();
    public final List<String> suggestions = new ArrayList<>();

    // 当前分支信息
    public String currentBranch;
    public String remoteBranch;
    public String remoteUrl;
}