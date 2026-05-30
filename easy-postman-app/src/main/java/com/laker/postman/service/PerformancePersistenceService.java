package com.laker.postman.service;

import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.ioc.Component;
import com.laker.postman.ioc.PostConstruct;
import com.laker.postman.ioc.PreDestroy;
import com.laker.postman.model.Workspace;
import com.laker.postman.panel.performance.plan.PerformancePlanConfiguration;
import com.laker.postman.panel.performance.plan.PerformancePlanDocument;
import com.laker.postman.panel.performance.plan.PerformancePlanStorage;
import com.laker.postman.panel.performance.plan.PerformanceRemoteWorkerSettings;
import com.laker.postman.panel.performance.plan.PerformanceSwingTreePlanAdapter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 性能测试配置持久化服务
 * 用于保存和加载性能测试面板中的测试计划配置
 */
@Slf4j
@Component
public class PerformancePersistenceService {
    private final PerformancePlanStorage planStorage = new PerformancePlanStorage();
    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "performance-config-save");
        thread.setDaemon(true);
        return thread;
    });

    @PostConstruct
    public void init() {
        ensureDirExists();
    }

    private void ensureDirExists() {
        ensureDirExists(getConfigFilePath());
    }

    private void ensureDirExists(Path configPath) {
        try {
            Path configDir = configPath.getParent();
            if (configDir != null && !Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (IOException e) {
            log.error("Failed to create config directory: {}", e.getMessage());
        }
    }

    /**
     * 保存性能测试配置树结构
     * UI 树会先转换为纯计划文档，持久化层不直接依赖 Swing 节点。
     */
    public void save(DefaultMutableTreeNode rootNode) {
        save(rootNode, true, true, false);
    }

    /**
     * 保存性能测试配置树结构
     *
     * @param rootNode      树根节点
     * @param efficientMode 是否开启精简明细
     */
    public void save(DefaultMutableTreeNode rootNode, boolean efficientMode) {
        save(rootNode, efficientMode, true, false);
    }

    /**
     * 保存性能测试配置树结构
     *
     * @param rootNode      树根节点
     * @param efficientMode 是否开启精简明细
     * @param trendEnabled  是否开启趋势采样
     */
    public void save(DefaultMutableTreeNode rootNode,
                     boolean efficientMode,
                     boolean trendEnabled) {
        save(rootNode, efficientMode, trendEnabled, false);
    }

    public void save(DefaultMutableTreeNode rootNode,
                     boolean efficientMode,
                     boolean trendEnabled,
                     boolean reportRealtimeEnabled) {
        save(rootNode, efficientMode, trendEnabled, reportRealtimeEnabled, PerformanceRemoteWorkerSettings.disabled());
    }

    public void save(DefaultMutableTreeNode rootNode,
                     boolean efficientMode,
                     boolean trendEnabled,
                     boolean reportRealtimeEnabled,
                     PerformanceRemoteWorkerSettings remoteWorkerSettings) {
        saveDocument(
                PerformanceSwingTreePlanAdapter.toDocument(rootNode),
                efficientMode,
                trendEnabled,
                reportRealtimeEnabled,
                remoteWorkerSettings
        );
    }

    private void saveDocument(PerformancePlanDocument document,
                              boolean efficientMode,
                              boolean trendEnabled,
                              boolean reportRealtimeEnabled) {
        saveDocument(document, efficientMode, trendEnabled, reportRealtimeEnabled, PerformanceRemoteWorkerSettings.disabled());
    }

    private void saveDocument(PerformancePlanDocument document,
                              boolean efficientMode,
                              boolean trendEnabled,
                              boolean reportRealtimeEnabled,
                              PerformanceRemoteWorkerSettings remoteWorkerSettings) {
        saveConfiguration(PerformancePlanConfiguration.builder()
                .planDocument(document)
                .efficientMode(efficientMode)
                .trendEnabled(trendEnabled)
                .reportRealtimeEnabled(reportRealtimeEnabled)
                .remoteWorkerSettings(remoteWorkerSettings)
                .build());
    }

    private void saveDocument(Path configPath,
                              PerformancePlanDocument document,
                              boolean efficientMode,
                              boolean trendEnabled,
                              boolean reportRealtimeEnabled) {
        saveDocument(configPath, document, efficientMode, trendEnabled, reportRealtimeEnabled,
                PerformanceRemoteWorkerSettings.disabled());
    }

    private void saveDocument(Path configPath,
                              PerformancePlanDocument document,
                              boolean efficientMode,
                              boolean trendEnabled,
                              boolean reportRealtimeEnabled,
                              PerformanceRemoteWorkerSettings remoteWorkerSettings) {
        saveConfiguration(configPath, PerformancePlanConfiguration.builder()
                .planDocument(document)
                .efficientMode(efficientMode)
                .trendEnabled(trendEnabled)
                .reportRealtimeEnabled(reportRealtimeEnabled)
                .remoteWorkerSettings(remoteWorkerSettings)
                .build());
    }

    public void saveConfiguration(PerformancePlanConfiguration configuration) {
        saveConfiguration(getConfigFilePath(), configuration);
    }

    private void saveConfiguration(Path configPath, PerformancePlanConfiguration configuration) {
        planStorage.saveConfiguration(configPath, configuration);
    }

    /**
     * 异步保存配置
     */
    public void saveAsync(DefaultMutableTreeNode rootNode) {
        saveAsync(rootNode, true, true, false);
    }

    /**
     * 异步保存配置
     *
     * @param rootNode      树根节点
     * @param efficientMode 是否开启精简明细
     */
    public void saveAsync(DefaultMutableTreeNode rootNode, boolean efficientMode) {
        saveAsync(rootNode, efficientMode, true, false);
    }

    /**
     * 异步保存配置
     *
     * @param rootNode      树根节点
     * @param efficientMode 是否开启精简明细
     * @param trendEnabled  是否开启趋势采样
     */
    public void saveAsync(DefaultMutableTreeNode rootNode,
                          boolean efficientMode,
                          boolean trendEnabled) {
        saveAsync(rootNode, efficientMode, trendEnabled, false);
    }

    public void saveAsync(DefaultMutableTreeNode rootNode,
                          boolean efficientMode,
                          boolean trendEnabled,
                          boolean reportRealtimeEnabled) {
        saveAsync(rootNode, efficientMode, trendEnabled, reportRealtimeEnabled, PerformanceRemoteWorkerSettings.disabled());
    }

    public void saveAsync(DefaultMutableTreeNode rootNode,
                          boolean efficientMode,
                          boolean trendEnabled,
                          boolean reportRealtimeEnabled,
                          PerformanceRemoteWorkerSettings remoteWorkerSettings) {
        // 异步线程启动前先固定路径，防止用户切换 workspace 后把旧性能方案写到新 workspace。
        Path configPath = getConfigFilePath();
        PerformancePlanDocument document = PerformanceSwingTreePlanAdapter.toDocument(rootNode);
        saveExecutor.execute(() -> saveDocument(
                configPath,
                document,
                efficientMode,
                trendEnabled,
                reportRealtimeEnabled,
                remoteWorkerSettings
        ));
    }

    /**
     * 加载性能测试配置
     * UI 层需要树节点时由纯计划文档适配生成。
     */
    public DefaultMutableTreeNode load(String rootName) {
        PerformancePlanConfiguration configuration = loadConfiguration();
        PerformancePlanDocument document = configuration == null ? null : configuration.getPlanDocument();
        return PerformanceSwingTreePlanAdapter.toTree(document, rootName);
    }

    public PerformancePlanConfiguration loadConfiguration() {
        return planStorage.loadConfiguration(getConfigFilePath());
    }

    /**
     * 清空配置
     */
    public void clear() {
        planStorage.clear(getConfigFilePath());
    }

    protected Path getConfigFilePath() {
        Workspace workspace = getCurrentWorkspace();
        return Paths.get(ConfigPathConstants.getPerformanceConfigPath(workspace));
    }

    protected Workspace getCurrentWorkspace() {
        try {
            return WorkspaceService.getInstance().getCurrentWorkspace();
        } catch (Exception e) {
            log.debug("Failed to resolve current workspace for performance config path", e);
            return null;
        }
    }

    @PreDestroy
    public void shutdown() {
        saveExecutor.shutdown();
        try {
            if (!saveExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                saveExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            saveExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
