package com.laker.postman.model;

import lombok.Data;

import java.io.File;

// Workspace模型类
@Data
public class Workspace {
    private String id; // UUID
    private String name; // 工作空间名称
    private String description; // 工作空间描述
    private WorkspaceType type; // LOCAL/GIT
    private GitRepoSource gitRepoSource; // 仓库来源：INITIALIZED（本地初始化）/ CLONED（远程克隆）

    /**
     * 本地路径，末尾始终带 {@link File#separator}。
     * getter/setter 均做规范化，调用方无需手动处理末尾斜杠。
     */
    private String path; // 本地路径

    private String gitRemoteUrl; // Git远程仓库地址
    private String currentBranch; // 当前分支名称
    private String remoteBranch; // 当前跟踪的远程分支名称
    private String lastCommitId; // 最后提交ID
    private long createdAt;
    private long updatedAt;
    private String gitUsername;      // Git 用户名
    private String gitPassword;      // Git 密码（加密存储）
    private String gitToken;         // Git 令牌（替代密码，优先使用）
    private GitAuthType gitAuthType; // 认证类型
    private String sshPrivateKeyPath; // SSH 私钥文件路径
    private String sshPassphrase;     // SSH 私钥密码（可选）

    /**
     * 返回末尾带 {@link File#separator} 的规范路径。
     * 无论 path 存储时末尾是否有分隔符，此方法总是返回统一格式。
     */
    public String getPath() {
        if (path == null || path.isBlank()) {
            return path;
        }
        return path.endsWith(File.separator) ? path : path + File.separator;
    }

    /**
     * 存储前自动规范化末尾分隔符，保持内部一致。
     */
    public void setPath(String path) {
        if (path == null || path.isBlank()) {
            this.path = path;
            return;
        }
        String trimmed = path.trim();
        this.path = trimmed.endsWith(File.separator) ? trimmed : trimmed + File.separator;
    }
}