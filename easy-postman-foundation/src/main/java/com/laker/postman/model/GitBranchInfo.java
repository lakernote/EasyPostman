package com.laker.postman.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Git branch metadata for workspace branch management.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitBranchInfo {
    private String name;
    private String fullName;
    private boolean current;
    private boolean remote;
    private String remoteName;
    private String trackingBranch;
    private int aheadCount;
    private int behindCount;

    public GitBranchInfo(String name, String fullName, boolean current, boolean remote, String remoteName) {
        this(name, fullName, current, remote, remoteName, null);
    }

    public GitBranchInfo(String name, String fullName, boolean current, boolean remote, String remoteName, String trackingBranch) {
        this(name, fullName, current, remote, remoteName, trackingBranch, 0, 0);
    }
}
