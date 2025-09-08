package com.laker.postman.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Git状态检查结果
 */
public class GitStatusCheck {
    public boolean hasUncommittedChanges = false;
    public boolean hasUntrackedFiles = false;
    public boolean hasLocalCommits = false;
    public boolean hasRemoteCommits = false;
    public boolean canCommit = false;
    public boolean canPush = false;
    public boolean canPull = false;
    public final List<String> warnings = new ArrayList<>();
    public final List<String> suggestions = new ArrayList<>();

    // 详细信息
    public int uncommittedCount = 0;
    public int untrackedCount = 0;
    public int localCommitsAhead = 0;
    public int remoteCommitsBehind = 0;
    public final List<String> uncommittedFiles = new ArrayList<>();
    public final List<String> untrackedFilesList = new ArrayList<>();
}