package com.laker.postman.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Git 提交信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitCommitInfo {
    /**
     * 提交ID（完整SHA）
     */
    private String commitId;

    /**
     * 短提交ID（前8位）
     */
    private String shortCommitId;

    /**
     * 提交消息
     */
    private String message;

    /**
     * 作者名称
     */
    private String authorName;

    /**
     * 作者邮箱
     */
    private String authorEmail;

    /**
     * 提交时间（毫秒时间戳）
     */
    private long commitTime;

    /**
     * 提交者名称
     */
    private String committerName;

    /**
     * 提交者邮箱
     */
    private String committerEmail;
}
