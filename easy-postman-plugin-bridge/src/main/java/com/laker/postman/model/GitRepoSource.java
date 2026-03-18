package com.laker.postman.model;

// 枚举定义
public enum GitRepoSource {
    INITIALIZED, // 本地初始化后推送到远程
    CLONED       // 从远程仓库克隆
}