package com.laker.postman.service;

import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.ioc.Component;
import com.laker.postman.ioc.PostConstruct;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.Workspace;
import com.laker.postman.panel.performance.plan.PerformanceCsvState;
import com.laker.postman.panel.performance.plan.PerformancePlanConfiguration;
import com.laker.postman.panel.performance.plan.PerformancePlanDocument;
import com.laker.postman.panel.performance.plan.PerformancePlanStorage;
import com.laker.postman.panel.performance.plan.PerformanceSwingTreePlanAdapter;
import com.laker.postman.service.collections.ActiveCollectionTreeNodeRepository;
import com.laker.postman.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 性能测试配置持久化服务
 * 用于保存和加载性能测试面板中的测试计划配置
 */
@Slf4j
@Component
public class PerformancePersistenceService {
    private static final String FILE_PATH = ConfigPathConstants.PERFORMANCE_CONFIG;
    private final PerformancePlanStorage planStorage = new PerformancePlanStorage();

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
        save(rootNode, true, true, false, null);
    }

    /**
     * 保存性能测试配置树结构
     *
     * @param rootNode      树根节点
     * @param efficientMode 是否开启精简明细
     */
    public void save(DefaultMutableTreeNode rootNode, boolean efficientMode) {
        save(rootNode, efficientMode, true, false, null);
    }

    /**
     * 保存性能测试配置树结构
     *
     * @param rootNode      树根节点
     * @param efficientMode 是否开启精简明细
     * @param csvState      CSV 快照状态
     */
    public void save(DefaultMutableTreeNode rootNode, boolean efficientMode, CsvDataPanel.CsvState csvState) {
        save(rootNode, efficientMode, true, false, csvState);
    }

    /**
     * 保存性能测试配置树结构
     *
     * @param rootNode      树根节点
     * @param efficientMode 是否开启精简明细
     * @param trendEnabled  是否开启趋势采样
     * @param csvState      CSV 快照状态
     */
    public void save(DefaultMutableTreeNode rootNode,
                     boolean efficientMode,
                     boolean trendEnabled,
                     CsvDataPanel.CsvState csvState) {
        save(rootNode, efficientMode, trendEnabled, false, csvState);
    }

    public void save(DefaultMutableTreeNode rootNode,
                     boolean efficientMode,
                     boolean trendEnabled,
                     boolean reportRealtimeEnabled) {
        save(rootNode, efficientMode, trendEnabled, reportRealtimeEnabled, null);
    }

    /**
     * 保存性能测试配置树结构
     *
     * @param rootNode              树根节点
     * @param efficientMode         是否开启精简明细
     * @param trendEnabled          是否开启趋势采样
     * @param reportRealtimeEnabled 是否运行中实时刷新报表
     * @param csvState              CSV 快照状态
     */
    public void save(DefaultMutableTreeNode rootNode,
                     boolean efficientMode,
                     boolean trendEnabled,
                     boolean reportRealtimeEnabled,
                     CsvDataPanel.CsvState csvState) {
        saveDocument(PerformanceSwingTreePlanAdapter.toDocument(rootNode), efficientMode, trendEnabled, reportRealtimeEnabled, csvState);
    }

    public void saveDocument(PerformancePlanDocument document) {
        saveDocument(document, true, true, false, null);
    }

    public void saveDocument(PerformancePlanDocument document,
                             boolean efficientMode,
                             boolean trendEnabled,
                             boolean reportRealtimeEnabled,
                             CsvDataPanel.CsvState csvState) {
        saveConfiguration(PerformancePlanConfiguration.builder()
                .planDocument(document)
                .efficientMode(efficientMode)
                .trendEnabled(trendEnabled)
                .reportRealtimeEnabled(reportRealtimeEnabled)
                .csvState(toPerformanceCsvState(csvState))
                .build());
    }

    private void saveDocument(Path configPath,
                              PerformancePlanDocument document,
                              boolean efficientMode,
                              boolean trendEnabled,
                              boolean reportRealtimeEnabled,
                              CsvDataPanel.CsvState csvState) {
        saveConfiguration(configPath, PerformancePlanConfiguration.builder()
                .planDocument(document)
                .efficientMode(efficientMode)
                .trendEnabled(trendEnabled)
                .reportRealtimeEnabled(reportRealtimeEnabled)
                .csvState(toPerformanceCsvState(csvState))
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
        saveAsync(rootNode, true, true, false, null);
    }

    /**
     * 异步保存配置
     *
     * @param rootNode      树根节点
     * @param efficientMode 是否开启精简明细
     */
    public void saveAsync(DefaultMutableTreeNode rootNode, boolean efficientMode) {
        saveAsync(rootNode, efficientMode, true, false, null);
    }

    /**
     * 异步保存配置
     *
     * @param rootNode      树根节点
     * @param efficientMode 是否开启精简明细
     * @param csvState      CSV 快照状态
     */
    public void saveAsync(DefaultMutableTreeNode rootNode, boolean efficientMode, CsvDataPanel.CsvState csvState) {
        saveAsync(rootNode, efficientMode, true, false, csvState);
    }

    /**
     * 异步保存配置
     *
     * @param rootNode      树根节点
     * @param efficientMode 是否开启精简明细
     * @param trendEnabled  是否开启趋势采样
     * @param csvState      CSV 快照状态
     */
    public void saveAsync(DefaultMutableTreeNode rootNode,
                          boolean efficientMode,
                          boolean trendEnabled,
                          CsvDataPanel.CsvState csvState) {
        saveAsync(rootNode, efficientMode, trendEnabled, false, csvState);
    }

    public void saveAsync(DefaultMutableTreeNode rootNode,
                          boolean efficientMode,
                          boolean trendEnabled,
                          boolean reportRealtimeEnabled) {
        saveAsync(rootNode, efficientMode, trendEnabled, reportRealtimeEnabled, null);
    }

    /**
     * 异步保存配置
     *
     * @param rootNode              树根节点
     * @param efficientMode         是否开启精简明细
     * @param trendEnabled          是否开启趋势采样
     * @param reportRealtimeEnabled 是否运行中实时刷新报表
     * @param csvState              CSV 快照状态
     */
    public void saveAsync(DefaultMutableTreeNode rootNode,
                          boolean efficientMode,
                          boolean trendEnabled,
                          boolean reportRealtimeEnabled,
                          CsvDataPanel.CsvState csvState) {
        // 异步线程启动前先固定路径，防止用户切换 workspace 后把旧性能方案写到新 workspace。
        Path configPath = getConfigFilePath();
        PerformancePlanDocument document = PerformanceSwingTreePlanAdapter.toDocument(rootNode);
        Thread saveThread = new Thread(
                () -> saveDocument(configPath, document, efficientMode, trendEnabled, reportRealtimeEnabled, csvState),
                "performance-config-save"
        );
        saveThread.setDaemon(true);
        saveThread.start();
    }

    private PerformanceCsvState toPerformanceCsvState(CsvDataPanel.CsvState csvState) {
        if (csvState == null) {
            return null;
        }
        return new PerformanceCsvState(csvState.getSourceName(), csvState.getHeaders(), csvState.getRows());
    }

    private CsvDataPanel.CsvState toCsvState(PerformanceCsvState csvState) {
        if (csvState == null) {
            return null;
        }
        return new CsvDataPanel.CsvState(csvState.getSourceName(), csvState.getHeaders(), csvState.getRows());
    }

    /**
     * 加载性能测试配置
     * UI 层需要树节点时由纯计划文档适配生成。
     */
    public DefaultMutableTreeNode load(String rootName) {
        PerformancePlanDocument document = loadDocument();
        return PerformanceSwingTreePlanAdapter.toTree(document, rootName);
    }

    public PerformancePlanDocument loadDocument() {
        PerformancePlanConfiguration configuration = loadConfiguration();
        return configuration == null ? null : configuration.getPlanDocument();
    }

    public PerformancePlanConfiguration loadConfiguration() {
        return planStorage.loadConfiguration(getConfigFilePath(), this::findRequestItemById);
    }

    /**
     * 加载精简明细设置
     *
     * @return 精简明细设置，如果配置文件不存在或读取失败则返回 true（默认值）
     */
    public boolean loadEfficientMode() {
        PerformancePlanConfiguration configuration = loadConfiguration();
        return configuration == null || configuration.isEfficientMode();
    }

    /**
     * 加载趋势采样开关。
     *
     * @return 趋势采样设置，如果配置文件不存在或读取失败则返回 true（默认开启）
     */
    public boolean loadTrendEnabled() {
        PerformancePlanConfiguration configuration = loadConfiguration();
        return configuration == null || configuration.isTrendEnabled();
    }

    /**
     * 加载报表实时刷新开关。
     *
     * @return 报表实时刷新设置，如果配置文件不存在或读取失败则返回 false（默认结束后生成）
     */
    public boolean loadReportRealtimeEnabled() {
        PerformancePlanConfiguration configuration = loadConfiguration();
        return configuration != null && configuration.isReportRealtimeEnabled();
    }

    public CsvDataPanel.CsvState loadCsvState() {
        PerformancePlanConfiguration configuration = loadConfiguration();
        return configuration == null ? null : toCsvState(configuration.getCsvState());
    }

    /**
     * 清空配置
     */
    public void clear() {
        planStorage.clear(getConfigFilePath());
    }

    /**
     * 通过ID从集合中查找请求项
     * 注意：返回的是深拷贝，避免性能测试面板中的修改影响集合中的原始数据
     */
    public HttpRequestItem findRequestItemById(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            return null;
        }

        try {
            DefaultMutableTreeNode requestNode = new ActiveCollectionTreeNodeRepository()
                    .findNodeByRequestId(requestId)
                    .orElse(null);

            if (requestNode != null) {
                Object userObj = requestNode.getUserObject();
                if (userObj instanceof Object[] obj) {
                    if (obj.length > 1 && obj[1] instanceof HttpRequestItem originalItem) {
                        // 使用 JSON 序列化/反序列化进行深拷贝
                        // 这样可以确保性能测试面板中的修改不会影响集合中的原始请求
                        return deepCopyRequestItem(originalItem);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to find request item by ID {}: {}", requestId, e.getMessage());
        }

        return null;
    }

    /**
     * 深拷贝 HttpRequestItem 对象
     * 使用 JSON 序列化/反序列化实现深拷贝
     */
    private HttpRequestItem deepCopyRequestItem(HttpRequestItem original) {
        try {
            return JsonUtil.deepCopy(original, HttpRequestItem.class);
        } catch (Exception e) {
            log.error("Failed to deep copy request item: {}", e.getMessage());
            return original;
        }
    }

    protected Path getConfigFilePath() {
        Workspace workspace = getCurrentWorkspace();
        Path workspaceConfigPath = Paths.get(ConfigPathConstants.getPerformanceConfigPath(workspace));
        return WorkspaceScopedConfigSupport.resolveConfigPath(
                workspace,
                workspaceConfigPath,
                getLegacyConfigFilePath(),
                "performance",
                log
        );
    }

    protected Workspace getCurrentWorkspace() {
        try {
            return WorkspaceService.getInstance().getCurrentWorkspace();
        } catch (Exception e) {
            log.debug("Failed to resolve current workspace for performance config path", e);
            return null;
        }
    }

    protected Path getLegacyConfigFilePath() {
        return Paths.get(FILE_PATH);
    }
}
