package com.laker.postman.model;

/**
 * 更新类型枚举
 */
public enum UpdateType {
    /**
     * 增量更新 - 仅更新 JAR 文件（快速但可能有兼容性问题）
     */
    INCREMENTAL,

    /**
     * 全量更新 - 更新完整安装包 MSI/DMG（推荐，更稳定）
     */
    FULL;

    /**
     * 判断是否为增量更新
     */
    public boolean isIncremental() {
        return this == INCREMENTAL;
    }

    /**
     * 判断是否为全量更新
     */
    public boolean isFull() {
        return this == FULL;
    }
}

