package com.laker.postman.service;

import com.laker.postman.model.Workspace;
import com.laker.postman.util.WorkspaceStorageUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 工作区级配置文件路径支持。
 * <p>
 * Function/Performance 配置都需要跟随当前 workspace，旧版应用级文件只作为默认工作区的迁移来源。
 */
final class WorkspaceScopedConfigSupport {

    private WorkspaceScopedConfigSupport() {
    }

    static Path resolveConfigPath(Workspace workspace,
                                  Path workspaceConfigPath,
                                  Path legacyConfigPath,
                                  String configName,
                                  Logger log) {
        migrateLegacyDefaultWorkspaceConfigIfNeeded(workspace, workspaceConfigPath, legacyConfigPath, configName, log);
        return workspaceConfigPath;
    }

    private static void migrateLegacyDefaultWorkspaceConfigIfNeeded(Workspace workspace,
                                                                    Path workspaceConfigPath,
                                                                    Path legacyConfigPath,
                                                                    String configName,
                                                                    Logger log) {
        // 只有默认工作区需要兼容旧版应用级文件；普通 workspace 必须保持目录隔离，避免误导入其它数据。
        if (!WorkspaceStorageUtil.isDefaultWorkspace(workspace)) {
            return;
        }

        if (legacyConfigPath.equals(workspaceConfigPath) || !Files.exists(legacyConfigPath)) {
            return;
        }

        try {
            Path parent = workspaceConfigPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            if (!Files.exists(workspaceConfigPath)) {
                // 首次升级时把旧文件移动到默认 workspace，后续 Git 同步只关注 workspace 目录。
                Files.move(legacyConfigPath, workspaceConfigPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Moved legacy {} config to workspace: {}", configName, workspaceConfigPath);
            } else {
                // workspace 内已有新文件时，旧路径文件只能算历史残留，直接清理避免 UI 再看到两份配置。
                Files.deleteIfExists(legacyConfigPath);
                log.info("Removed legacy {} config outside workspace: {}", configName, legacyConfigPath);
            }
        } catch (IOException e) {
            log.warn("Failed to migrate legacy {} config to workspace: {}", configName, workspaceConfigPath, e);
        }
    }
}
