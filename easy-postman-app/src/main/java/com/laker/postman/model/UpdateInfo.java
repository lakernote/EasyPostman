package com.laker.postman.model;

import cn.hutool.json.JSONObject;
import lombok.Data;

/**
 * 更新信息封装类
 */
@Data
public class UpdateInfo {

    public enum Status {
        UPDATE_AVAILABLE,           // 有新版本可用（且有对应平台的安装包）
        UPDATE_AVAILABLE_NO_ASSET,  // 有新版本可用，但无对应平台的安装包（资源尚未上传）
        NO_UPDATE,                  // 没有更新
        CHECK_FAILED                // 检查失败
    }

    private final Status status;
    private final String currentVersion;
    private final String latestVersion;
    private final String message;
    private final JSONObject releaseInfo;

    private UpdateInfo(Status status, String currentVersion, String latestVersion, String message, JSONObject releaseInfo) {
        this.status = status;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.message = message;
        this.releaseInfo = releaseInfo;
    }

    /**
     * 创建有更新可用的信息
     */
    public static UpdateInfo updateAvailable(String currentVersion, String latestVersion, JSONObject releaseInfo) {
        return new UpdateInfo(Status.UPDATE_AVAILABLE, currentVersion, latestVersion,
                "New version available: " + latestVersion, releaseInfo);
    }

    /**
     * 创建有更新可用但无平台安装包的信息（资源尚未上传或不支持当前平台）
     */
    public static UpdateInfo updateAvailableNoAsset(String currentVersion, String latestVersion, JSONObject releaseInfo) {
        return new UpdateInfo(Status.UPDATE_AVAILABLE_NO_ASSET, currentVersion, latestVersion,
                "New version available (no asset yet): " + latestVersion, releaseInfo);
    }

    /**
     * 创建无更新的信息
     */
    public static UpdateInfo noUpdateAvailable(String message) {
        return new UpdateInfo(Status.NO_UPDATE, null, null, message, null);
    }

    /**
     * 创建检查失败的信息
     */
    public static UpdateInfo checkFailed(String errorMessage) {
        return new UpdateInfo(Status.CHECK_FAILED, null, null, errorMessage, null);
    }

    /**
     * 是否有更新可用
     */
    public boolean isUpdateAvailable() {
        return status == Status.UPDATE_AVAILABLE;
    }

    /**
     * 是否有更新可用但无安装包（资源尚未上传）
     */
    public boolean isUpdateAvailableNoAsset() {
        return status == Status.UPDATE_AVAILABLE_NO_ASSET;
    }

    /**
     * 检查是否失败
     */
    public boolean isCheckFailed() {
        return status == Status.CHECK_FAILED;
    }
}
