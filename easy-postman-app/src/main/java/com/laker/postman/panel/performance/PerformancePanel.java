package com.laker.postman.panel.performance;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.core.model.PerformanceStatsCollector;
import com.laker.postman.performance.core.model.PerformanceTrendWindowCollector;


import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.DebouncedSaveSupport;
import com.laker.postman.common.component.button.ExportButton;
import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.common.component.button.StartButton;
import com.laker.postman.common.component.button.StopButton;
import com.laker.postman.common.constants.AppConstants;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.panel.performance.assertion.AssertionPropertyPanel;
import com.laker.postman.panel.performance.control.PerformanceSaveShortcutSupport;
import com.laker.postman.panel.performance.control.PerformanceRunUiController;
import com.laker.postman.panel.performance.control.PerformanceRunUiEventBridge;
import com.laker.postman.panel.performance.control.PerformanceStatisticsCoordinator;
import com.laker.postman.panel.performance.control.PerformanceTimerManager;
import com.laker.postman.panel.performance.config.CsvDataSetPropertyPanel;
import com.laker.postman.panel.performance.controller.LoopPropertyPanel;
import com.laker.postman.panel.performance.extractor.ExtractorPropertyPanel;
import com.laker.postman.panel.performance.execution.DefaultPerformanceNetworkRuntime;
import com.laker.postman.panel.performance.execution.PerformanceExecutionConfig;
import com.laker.postman.panel.performance.model.PerformanceTreeNode;
import com.laker.postman.panel.performance.model.PerformanceResultListener;
import com.laker.postman.panel.performance.model.PerformanceStatsCollectorListener;
import com.laker.postman.panel.performance.model.PerformanceTrendWindowCollectorListener;
import com.laker.postman.panel.performance.plan.PerformancePlanConfiguration;
import com.laker.postman.panel.performance.plan.PerformancePlanDocument;
import com.laker.postman.panel.performance.plan.PerformanceRemoteWorkerSettings;
import com.laker.postman.panel.performance.plan.PerformanceRunPlanFactory;
import com.laker.postman.panel.performance.plan.PerformanceSwingTreePlanAdapter;
import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.panel.performance.result.PerformanceResultCollector;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import com.laker.postman.panel.performance.result.PerformanceResultTableVisualizer;
import com.laker.postman.panel.performance.result.PerformanceTrendView;
import com.laker.postman.panel.performance.runtime.PerformanceExecutionEngine;
import com.laker.postman.panel.performance.runtime.PerformanceRunSession;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupPropertyPanel;
import com.laker.postman.panel.performance.timer.TimerPropertyPanel;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.run.PerformanceRunPlanJsonStorage;
import com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint;
import com.laker.postman.performance.core.worker.PerformanceWorkerEndpointParser;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.GlobalVariablesService;
import com.laker.postman.service.PerformancePersistenceService;
import com.laker.postman.service.http.okhttp.HttpClientRuntimeConfig;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * 左侧多层级树（用户组-请求-断言-定时器），右侧属性区，底部Tab结果区
 */
@Slf4j
public class PerformancePanel extends UiSingletonPanel {
    public static final String EMPTY = "empty";
    public static final String THREAD_GROUP = "threadGroup";
    public static final String CSV_DATA_SET = "csvDataSet";
    public static final String LOOP = "loop";
    public static final String REQUEST = "request";
    public static final String ASSERTION = "assertion";
    public static final String EXTRACTOR = "extractor";
    public static final String TIMER = "timer";
    public static final String SSE_CONNECT = "sseConnect";
    public static final String SSE_READ = "sseRead";
    public static final String WS_CONNECT = "wsConnect";
    public static final String WS_SEND = "wsSend";
    public static final String WS_READ = "wsRead";
    public static final String WS_CLOSE = "wsClose";
    private JTree performanceTree;
    private DefaultTreeModel treeModel;
    private JPanel propertyPanel; // 右侧属性区（CardLayout）
    private CardLayout propertyCardLayout;
    private JTabbedPane resultTabbedPane; // 结果Tab
    private ThreadGroupPropertyPanel threadGroupPanel;
    private CsvDataSetPropertyPanel csvDataSetPanel;
    private LoopPropertyPanel loopPanel;
    private AssertionPropertyPanel assertionPanel;
    private ExtractorPropertyPanel extractorPanel;
    private TimerPropertyPanel timerPanel;
    private SseStagePropertyPanel sseConnectPanel;
    private SseStagePropertyPanel sseReadPanel;
    private WebSocketStagePropertyPanel wsConnectPanel;
    private WebSocketStagePropertyPanel wsSendPanel;
    private WebSocketStagePropertyPanel wsReadPanel;
    private WebSocketStagePropertyPanel wsClosePanel;
    private JPanel requestEditorHost;
    private volatile boolean running = false;
    private transient Thread runThread;
    private StartButton runBtn;
    private StopButton stopBtn;
    private ExportButton exportBtn;
    private RefreshButton refreshBtn;
    private JCheckBox remoteModeCheckBox;
    private JTextField workerEndpointsField;
    private JCheckBox efficientCheckBox; // 精简明细复选框
    private JCheckBox trendCheckBox; // 趋势采样开关
    private JComboBox<String> reportRefreshModeBox; // 报表刷新模式
    private JLabel progressLabel; // 进度标签
    private JPanel topPanel; // 顶部工具栏面板，用于主题切换时更新边框
    private long startTime;
    private final transient PerformanceStatsCollector statsCollector = new PerformanceStatsCollector();
    private final transient PerformanceTrendWindowCollector trendWindowCollector = new PerformanceTrendWindowCollector();

    // 定时器管理器 - 统一管理趋势图采样和报表刷新定时器
    private transient PerformanceTimerManager timerManager;

    // 精简明细：报表统计完整保留，结果表只保留失败/慢请求详情
    private boolean efficientMode = true;
    private boolean trendEnabled = true;
    private boolean reportRealtimeEnabled = false;
    private boolean remoteExecutionEnabled = false;
    private String remoteWorkerEndpoints = "";

    private PerformanceReportPanel performanceReportPanel;
    private PerformanceResultTablePanel performanceResultTablePanel;
    private PerformanceTrendView performanceTrendPanel;

    // 持久化服务
    private transient PerformancePersistenceService persistenceService;

    // 当前选中的请求节点
    private DefaultMutableTreeNode currentRequestNode;
    private transient PerformanceTreeSupport treeSupport;
    private transient PerformanceRequestSyncSupport requestSyncSupport;
    private transient PerformancePropertyPanelSupport propertyPanelSupport;
    private transient PerformanceStatisticsCoordinator statisticsCoordinator;
    private transient PerformanceExecutionEngine executionEngine;
    private transient PerformanceRunControlSupport runControlSupport;
    private transient PerformanceRemoteRunControlSupport remoteRunControlSupport;
    private transient PerformanceRequestEditorSupport requestEditorSupport;
    private final transient PerformanceSaveShortcutSupport saveShortcutSupport = new PerformanceSaveShortcutSupport();
    private final transient DebouncedSaveSupport autoSaveSupport = new DebouncedSaveSupport(500, this::saveConfigAsync);

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        this.persistenceService = BeanFactory.getBean(PerformancePersistenceService.class);
        initTimerManager();
        PerformancePlanConfiguration persistedConfiguration = persistenceService.loadConfiguration();
        applyPersistedSettings(persistedConfiguration);
        DefaultMutableTreeNode root = loadPersistedOrDefaultRoot(persistedConfiguration);

        treeModel = new DefaultTreeModel(root);
        treeSupport = new PerformanceTreeSupport(treeModel);
        PerformancePanelViewFactory viewFactory = new PerformancePanelViewFactory();
        PerformancePanelViewFactory.TreeSection treeSection = viewFactory.createTreeSection(treeModel);
        performanceTree = treeSection.tree();
        requestSyncSupport = new PerformanceRequestSyncSupport(
                treeModel,
                performanceTree,
                new PerformanceCollectionRequestResolver(),
                this::syncRequestStructure
        );
        PerformancePanelViewFactory.PropertySection propertySection = viewFactory.createPropertySection(
                EMPTY,
                THREAD_GROUP,
                CSV_DATA_SET,
                LOOP,
                REQUEST,
                ASSERTION,
                EXTRACTOR,
                TIMER,
                SSE_CONNECT,
                SSE_READ,
                WS_CONNECT,
                WS_SEND,
                WS_READ,
                WS_CLOSE
        );
        propertyPanel = propertySection.propertyPanel();
        propertyCardLayout = propertySection.propertyCardLayout();
        threadGroupPanel = propertySection.threadGroupPanel();
        csvDataSetPanel = propertySection.csvDataSetPanel();
        loopPanel = propertySection.loopPanel();
        assertionPanel = propertySection.assertionPanel();
        extractorPanel = propertySection.extractorPanel();
        timerPanel = propertySection.timerPanel();
        sseConnectPanel = propertySection.sseConnectPanel();
        sseReadPanel = propertySection.sseReadPanel();
        wsConnectPanel = propertySection.wsConnectPanel();
        wsSendPanel = propertySection.wsSendPanel();
        wsReadPanel = propertySection.wsReadPanel();
        wsClosePanel = propertySection.wsClosePanel();
        RequestEditSubPanel requestEditSubPanel = propertySection.requestEditSubPanel();
        requestEditorHost = propertySection.requestEditorHost();
        requestEditorSupport = new PerformanceRequestEditorSupport(
                requestEditSubPanel,
                requestEditorHost,
                this::resolveRequestProtocol
        );

        propertyPanelSupport = new PerformancePropertyPanelSupport(
                performanceTree,
                threadGroupPanel,
                csvDataSetPanel,
                loopPanel,
                assertionPanel,
                extractorPanel,
                timerPanel,
                sseConnectPanel,
                sseReadPanel,
                wsConnectPanel,
                wsSendPanel,
                wsReadPanel,
                wsClosePanel,
                requestEditorSupport::getRequestEditSubPanel,
                () -> currentRequestNode,
                this::saveRequestNodeData,
                treeSupport,
                this::syncRequestStructure
        );
        csvDataSetPanel.setChangeListener(this::handleCsvDataSetChanged);

        PerformancePanelViewFactory.ResultSection resultSection = viewFactory.createResultSection(
                trendEnabled,
                reportRealtimeEnabled,
                efficientMode,
                this,
                value -> efficientMode = value,
                this::applyTrendEnabled,
                this::applyReportRealtimeEnabled,
                this::refreshReportSnapshot,
                this::saveAllPropertyPanelData,
                this::saveConfig
        );
        resultTabbedPane = resultSection.resultTabbedPane();
        performanceResultTablePanel = resultSection.performanceResultTablePanel();
        performanceTrendPanel = resultSection.performanceTrendPanel();
        performanceReportPanel = resultSection.performanceReportPanel();
        efficientCheckBox = resultSection.efficientCheckBox();
        trendCheckBox = resultSection.trendCheckBox();
        reportRefreshModeBox = resultSection.reportRefreshModeBox();
        statisticsCoordinator = new PerformanceStatisticsCoordinator(
                statsCollector,
                trendWindowCollector,
                performanceReportPanel,
                performanceTrendPanel,
                resultTabbedPane,
                () -> executionEngine != null ? executionEngine.getActiveThreads() : 0,
                () -> executionEngine != null ? executionEngine.getActiveWebSockets() : 0,
                () -> executionEngine != null ? executionEngine.getActiveSseStreams() : 0,
                () -> timerManager != null ? timerManager.getSamplingIntervalMs() : 1000L,
                () -> trendEnabled,
                nowMs -> executionEngine != null
                        ? executionEngine.sampleRealtimeMetrics(nowMs)
                        : PerformanceRealtimeMetrics.Sample.empty(),
                nowMs -> executionEngine != null
                        ? executionEngine.liveRealtimeMetrics(nowMs)
                        : PerformanceRealtimeMetrics.LiveSnapshot.empty()
        );
        timerManager.setTrendSamplingCallback(statisticsCoordinator::sampleTrendData);
        timerManager.setReportRefreshCallback(statisticsCoordinator::refreshReport);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeSection.scrollPane(), propertyPanel);
        mainSplit.setDividerLocation(360);
        mainSplit.setDividerSize(3);
        mainSplit.setContinuousLayout(true);

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainSplit, resultSection.resultPanel());
        verticalSplit.setDividerSize(3);
        verticalSplit.setContinuousLayout(true);
        verticalSplit.setDividerLocation(260);
        mainSplit.setMinimumSize(new Dimension(400, 150));
        resultSection.resultPanel().setMinimumSize(new Dimension(400, 150));
        add(verticalSplit, BorderLayout.CENTER);

        PerformancePanelViewFactory.ToolbarSection toolbarSection = viewFactory.createToolbarSection(
                this::exportRunPlan,
                this::showUsageGuide,
                this::refreshRequestsFromCollections,
                remoteExecutionEnabled,
                remoteWorkerEndpoints,
                this::setRemoteExecutionEnabled,
                this::setRemoteWorkerEndpoints
        );
        topPanel = toolbarSection.topPanel();
        runBtn = toolbarSection.runBtn();
        stopBtn = toolbarSection.stopBtn();
        exportBtn = toolbarSection.exportBtn();
        refreshBtn = toolbarSection.refreshBtn();
        remoteModeCheckBox = toolbarSection.remoteModeCheckBox();
        workerEndpointsField = toolbarSection.workerEndpointsField();
        progressLabel = toolbarSection.progressLabel();
        add(topPanel, BorderLayout.NORTH);

        List<PerformanceResultListener> resultListeners = List.of(
                new PerformanceStatsCollectorListener(statsCollector),
                new PerformanceTrendWindowCollectorListener(trendWindowCollector),
                new PerformanceResultTableVisualizer(
                        performanceResultTablePanel,
                        SettingManager::getPerformanceSlowRequestThreshold
                )
        );
        PerformanceRunUiController runUiController = new PerformanceRunUiController(
                runBtn,
                stopBtn,
                refreshBtn,
                List.of(remoteModeCheckBox, workerEndpointsField)
        );
        DefaultPerformanceNetworkRuntime networkRuntime = new DefaultPerformanceNetworkRuntime(
                () -> new HttpClientRuntimeConfig(
                        SettingManager.getPerformanceMaxIdleConnections(),
                        SettingManager.getPerformanceKeepAliveSeconds(),
                        SettingManager.getPerformanceMaxRequests(),
                        SettingManager.getPerformanceMaxRequestsPerHost()
                )
        );
        executionEngine = new PerformanceExecutionEngine(
                () -> running,
                PerformanceExecutionConfig.guiSupplying(
                        () -> efficientMode,
                        SettingManager::getPerformanceResponseBodyPreviewLimitKb,
                        SettingManager::isPerformanceEventLoggingEnabled
                ),
                new PerformanceResultCollector(resultListeners),
                new PerformanceRunUiEventBridge(this, runUiController, progressLabel),
                networkRuntime
        );
        PerformanceRunSession runSession = new PerformanceRunSession(
                () -> running,
                value -> running = value,
                executionEngine
        );
        runControlSupport = new PerformanceRunControlSupport(
                this,
                () -> running,
                value -> running = value,
                () -> startTime,
                value -> startTime = value,
                propertyPanelSupport,
                executionEngine,
                runSession,
                statisticsCoordinator,
                timerManager,
                runUiController,
                efficientCheckBox,
                performanceResultTablePanel,
                statsCollector,
                this::clearCachedPerformanceResults
        );
        remoteRunControlSupport = new PerformanceRemoteRunControlSupport(
                () -> running,
                value -> running = value,
                runUiController,
                performanceReportPanel,
                this::clearCachedPerformanceResults
        );

        treeSupport.syncAllRequestStructures((DefaultMutableTreeNode) treeModel.getRoot());
        runBtn.addActionListener(e -> startRun(progressLabel));
        stopBtn.addActionListener(e -> stopRun());
        for (int i = 0; i < performanceTree.getRowCount(); i++) {
            performanceTree.expandRow(i);
        }
        selectFirstThreadGroup();
        setupSaveShortcut();
    }

    public void switchWorkspaceAndRefreshUI() {
        if (!isReadyForWorkspaceSwitch()) {
            return;
        }

        // 切换 workspace 前取消待保存任务，避免旧性能方案或 CSV 快照延迟写入新 workspace。
        autoSaveSupport.cancel();
        if (running) {
            stopRun();
        }

        PerformancePlanConfiguration persistedConfiguration = persistenceService.loadConfiguration();
        applyPersistedSettings(persistedConfiguration);
        DefaultMutableTreeNode root = loadPersistedOrDefaultRoot(persistedConfiguration);
        treeModel.setRoot(root);
        treeSupport.syncAllRequestStructures(root);
        currentRequestNode = null;
        if (efficientCheckBox != null) {
            efficientCheckBox.setSelected(efficientMode);
        }
        if (trendCheckBox != null) {
            trendCheckBox.setSelected(trendEnabled);
        }
        if (reportRefreshModeBox != null) {
            reportRefreshModeBox.setSelectedIndex(reportRealtimeEnabled ? 1 : 0);
        }
        if (remoteModeCheckBox != null) {
            remoteModeCheckBox.setSelected(remoteExecutionEnabled);
        }
        if (workerEndpointsField != null) {
            workerEndpointsField.setText(remoteWorkerEndpoints);
        }
        clearCachedPerformanceResults();
        propertyCardLayout.show(propertyPanel, EMPTY);
        for (int i = 0; i < performanceTree.getRowCount(); i++) {
            performanceTree.expandRow(i);
        }
        selectFirstThreadGroup();
    }

    private boolean isReadyForWorkspaceSwitch() {
        return treeModel != null
                && persistenceService != null
                && treeSupport != null
                && performanceTree != null
                && propertyPanel != null
                && propertyCardLayout != null
                && threadGroupPanel != null
                && csvDataSetPanel != null
                && extractorPanel != null
                && performanceResultTablePanel != null
                && performanceReportPanel != null
                && performanceTrendPanel != null
                && executionEngine != null
                && remoteRunControlSupport != null;
    }

    private void applyPersistedSettings(PerformancePlanConfiguration configuration) {
        efficientMode = configuration == null || configuration.isEfficientMode();
        trendEnabled = configuration == null || configuration.isTrendEnabled();
        reportRealtimeEnabled = configuration != null && configuration.isReportRealtimeEnabled();
        PerformanceRemoteWorkerSettings remoteSettings = configuration == null
                ? PerformanceRemoteWorkerSettings.disabled()
                : configuration.getRemoteWorkerSettings();
        remoteExecutionEnabled = remoteSettings.isEnabled();
        remoteWorkerEndpoints = remoteSettings.getWorkerEndpoints();
        applyTrendEnabled(trendEnabled);
        applyReportRealtimeEnabled(reportRealtimeEnabled);
    }

    private void setRemoteExecutionEnabled(boolean enabled) {
        remoteExecutionEnabled = enabled;
        saveConfig();
    }

    private void setRemoteWorkerEndpoints(String workerEndpoints) {
        remoteWorkerEndpoints = workerEndpoints == null ? "" : workerEndpoints.trim();
        saveConfig();
    }

    private PerformanceRemoteWorkerSettings currentRemoteWorkerSettings() {
        return PerformanceRemoteWorkerSettings.builder()
                .enabled(remoteModeCheckBox == null ? remoteExecutionEnabled : remoteModeCheckBox.isSelected())
                .workerEndpoints(workerEndpointsField == null ? remoteWorkerEndpoints : workerEndpointsField.getText())
                .build();
    }

    private boolean isRemoteExecutionSelected() {
        return currentRemoteWorkerSettings().isEnabled();
    }

    private DefaultMutableTreeNode loadPersistedOrDefaultRoot(PerformancePlanConfiguration configuration) {
        PerformancePlanDocument document = configuration == null ? null : configuration.getPlanDocument();
        DefaultMutableTreeNode savedRoot = PerformanceSwingTreePlanAdapter.toTree(
                document,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TEST_PLAN)
        );
        if (savedRoot != null) {
            return savedRoot;
        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new PerformanceTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TEST_PLAN), NodeType.ROOT));
        PerformanceTreeSupport.createDefaultRequest(root);
        return root;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // 主题切换时重新设置边框，确保分隔线颜色更新
        if (topPanel != null) {
            topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()));
        }
    }

    /**
     * 选择并定位到第一个Thread Group节点，并触发对应的点击事件
     */
    private void selectFirstThreadGroup() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        if (root.getChildCount() > 0) {
            DefaultMutableTreeNode firstGroup = (DefaultMutableTreeNode) root.getChildAt(0);
            TreePath path = new TreePath(firstGroup.getPath());
            performanceTree.setSelectionPath(path);
            performanceTree.scrollPathToVisible(path);

            // 触发节点点击事件，确保属性面板显示正确
            Object userObj = firstGroup.getUserObject();
            if (userObj instanceof PerformanceTreeNode nodeData && nodeData.type == NodeType.THREAD_GROUP) {
                // 显示线程组属性面板
                propertyCardLayout.show(propertyPanel, THREAD_GROUP);
                threadGroupPanel.setThreadGroupData(nodeData);
            }
        }
    }

    private RequestItemProtocolEnum resolveRequestProtocol(HttpRequestItem item) {
        return treeSupport.resolveRequestProtocol(item);
    }

    private void syncRequestStructure(DefaultMutableTreeNode requestNode, PerformanceTreeNode requestData) {
        treeSupport.syncRequestStructure(requestNode, requestData);
    }

    private void ensureRequestStructure(DefaultMutableTreeNode requestNode, PerformanceTreeNode requestData) {
        treeSupport.ensureRequestStructure(requestNode, requestData);
    }

    private void saveSseStageNode(DefaultMutableTreeNode stageNode) {
        propertyPanelSupport.saveSseStageNode(stageNode);
    }

    private void saveWebSocketStageNode(DefaultMutableTreeNode stageNode) {
        propertyPanelSupport.saveWebSocketStageNode(stageNode);
        if (stageNode != null && stageNode.getUserObject() instanceof PerformanceTreeNode stageNodeData
                && stageNodeData.type == NodeType.WS_CONNECT) {
            treeModel.nodeChanged(stageNode);
        }
    }

    private void switchRequestEditor(HttpRequestItem item) {
        requestEditorSupport.switchRequestEditor(item);
    }

    private void saveRequestNodeData(DefaultMutableTreeNode node) {
        PerformanceProtocol oldProtocol = PerformanceProtocol.HTTP;
        if (node != null && node.getUserObject() instanceof PerformanceTreeNode nodeData) {
            oldProtocol = treeSupport.resolvePerformanceProtocol(nodeData.httpRequestItem);
        }
        requestEditorSupport.saveRequestNodeData(node, this::syncRequestStructure);
        if (node != null && node.getUserObject() instanceof PerformanceTreeNode nodeData) {
            PerformanceProtocol newProtocol = treeSupport.resolvePerformanceProtocol(nodeData.httpRequestItem);
            if (oldProtocol != newProtocol) {
                ensureRequestStructure(node, nodeData);
            }
        }
    }

    /**
     * 设置 Ctrl/Cmd+S 快捷键保存所有配置
     */
    private void setupSaveShortcut() {
        saveShortcutSupport.install(this, this::handleSaveShortcut);
    }

    /**
     * 处理 Ctrl/Cmd+S 快捷键
     * 1. 强制提交所有 EasyJSpinner 的值
     * 2. 保存所有属性面板数据到树节点
     * 3. 持久化到文件
     * 4. 显示成功提示
     */
    private void handleSaveShortcut() {
        autoSaveSupport.cancel();
        propertyPanelSupport.forceCommitAllSpinners();

        saveAllPropertyPanelData();

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        persistenceService.save(root, efficientMode, trendEnabled, reportRealtimeEnabled, currentRemoteWorkerSettings());
        NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_SAVE_SUCCESS));
    }

    private void saveAllPropertyPanelData() {
        propertyPanelSupport.saveAllPropertyPanelData();
    }

    private void showUsageGuide() {
        PerformanceUsageGuideDialog.show(this);
    }

    private void exportRunPlan() {
        autoSaveSupport.cancel();
        propertyPanelSupport.forceCommitAllSpinners();
        saveAllPropertyPanelData();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.PERFORMANCE_RUN_PLAN_EXPORT_TITLE));
        fileChooser.setSelectedFile(new File("plan.json"));
        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = ensureJsonExtension(fileChooser.getSelectedFile());
        try {
            DefaultMutableTreeNode root = PerformanceTreeSnapshot.copy((DefaultMutableTreeNode) treeModel.getRoot());
            PerformancePlanDocument document = PerformanceSwingTreePlanAdapter.toDocument(root);
            PerformanceRunPlan runPlan = PerformanceRunPlanFactory.create(
                    PerformancePlanConfiguration.builder()
                            .planDocument(document)
                            .efficientMode(efficientMode)
                            .trendEnabled(trendEnabled)
                            .reportRealtimeEnabled(reportRealtimeEnabled)
                            .remoteWorkerSettings(currentRemoteWorkerSettings())
                            .build(),
                    EnvironmentService.getActiveEnvironment(),
                    GlobalVariablesService.getInstance().getGlobalVariables(),
                    AppConstants.APP_NAME + " " + SystemUtil.getCurrentVersion()
            );
            new PerformanceRunPlanJsonStorage().save(selectedFile.toPath(), runPlan);
            NotificationUtil.showSuccess(I18nUtil.getMessage(
                    MessageKeys.PERFORMANCE_RUN_PLAN_EXPORT_SUCCESS,
                    selectedFile.getAbsolutePath()
            ));
        } catch (Exception ex) {
            log.error("Failed to export performance run plan", ex);
            NotificationUtil.showError(I18nUtil.getMessage(
                    MessageKeys.PERFORMANCE_RUN_PLAN_EXPORT_FAIL,
                    ex.getMessage()
            ));
        }
    }

    private File ensureJsonExtension(File selectedFile) {
        if (selectedFile == null) {
            return new File("plan.json");
        }
        String path = selectedFile.getAbsolutePath();
        if (path.toLowerCase().endsWith(".json")) {
            return selectedFile;
        }
        return new File(path + ".json");
    }

    private void handleCsvDataSetChanged() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) performanceTree.getLastSelectedPathComponent();
        if (selectedNode != null && selectedNode.getUserObject() instanceof PerformanceTreeNode nodeData
                && nodeData.type == NodeType.CSV_DATA_SET) {
            treeModel.nodeChanged(selectedNode);
        }
        saveConfig();
    }

    // ========== 定时器管理 ==========

    /**
     * 初始化定时器管理器
     */
    private void initTimerManager() {
        timerManager = new PerformanceTimerManager(() -> running);
    }

    // ========== 执行与停止核心逻辑 ==========
    private void startRun(JLabel progressLabel) {
        if (isRemoteExecutionSelected()) {
            startRemoteRun(progressLabel);
            return;
        }
        runThread = runControlSupport.startRun(
                (DefaultMutableTreeNode) treeModel.getRoot(),
                progressLabel,
                efficientMode,
                value -> efficientMode = value
        );
    }

    private void startRemoteRun(JLabel progressLabel) {
        PerformanceRemoteWorkerSettings remoteSettings = currentRemoteWorkerSettings();
        List<PerformanceWorkerEndpoint> workers;
        try {
            workers = PerformanceWorkerEndpointParser.parse(remoteSettings.getWorkerEndpoints());
        } catch (IllegalArgumentException ex) {
            NotificationUtil.showError(ex.getMessage());
            return;
        }
        if (workers.isEmpty()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_WORKERS_REQUIRED));
            return;
        }

        propertyPanelSupport.forceCommitAllSpinners();
        saveAllPropertyPanelData();

        try {
            DefaultMutableTreeNode root = PerformanceTreeSnapshot.copy((DefaultMutableTreeNode) treeModel.getRoot());
            PerformancePlanDocument document = PerformanceSwingTreePlanAdapter.toDocument(root);
            PerformanceRunPlan runPlan = PerformanceRunPlanFactory.create(
                    PerformancePlanConfiguration.builder()
                            .planDocument(document)
                            .efficientMode(efficientMode)
                            .trendEnabled(trendEnabled)
                            .reportRealtimeEnabled(reportRealtimeEnabled)
                            .remoteWorkerSettings(remoteSettings)
                            .build(),
                    EnvironmentService.getActiveEnvironment(),
                    GlobalVariablesService.getInstance().getGlobalVariables(),
                    AppConstants.APP_NAME + " " + SystemUtil.getCurrentVersion()
            );
            runThread = remoteRunControlSupport.startRun(runPlan, workers, progressLabel);
        } catch (Exception ex) {
            log.error("Failed to start remote performance run", ex);
            NotificationUtil.showError(I18nUtil.getMessage(
                    MessageKeys.PERFORMANCE_REMOTE_MSG_FAILED,
                    ex.getMessage()
            ));
        }
    }

    @Override
    protected void registerListeners() {
        new PerformanceTreeInteractionSupport(
                this,
                performanceTree,
                treeModel,
                propertyCardLayout,
                propertyPanel,
                threadGroupPanel,
                csvDataSetPanel,
                loopPanel,
                assertionPanel,
                extractorPanel,
                timerPanel,
                sseConnectPanel,
                sseReadPanel,
                wsConnectPanel,
                wsSendPanel,
                wsReadPanel,
                wsClosePanel,
                treeSupport,
                this::saveRequestNodeData,
                this::saveSseStageNode,
                this::saveWebSocketStageNode,
                this::switchRequestEditor,
                this::syncRequestStructure,
                this::saveConfig,
                () -> currentRequestNode,
                node -> currentRequestNode = node,
                EMPTY,
                THREAD_GROUP,
                CSV_DATA_SET,
                LOOP,
                REQUEST,
                ASSERTION,
                EXTRACTOR,
                TIMER,
                SSE_CONNECT,
                SSE_READ,
                WS_CONNECT,
                WS_SEND,
                WS_READ,
                WS_CLOSE
        ).install();
    }

    private void stopRun() {
        if (isRemoteExecutionSelected() && remoteRunControlSupport != null) {
            remoteRunControlSupport.stopRun();
            return;
        }
        runControlSupport.stopRun();
    }

    /**
     * 保存当前配置
     */
    private void saveConfig() {
        // 树节点编辑和属性面板变化都会触发保存，统一防抖减少频繁写盘。
        autoSaveSupport.requestSave();
    }

    private void saveConfigAsync() {
        try {
            // 保存所有属性面板数据到树节点
            saveAllPropertyPanelData();
            // 获取根节点
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
            // 异步保存配置
            persistenceService.saveAsync(root, efficientMode, trendEnabled, reportRealtimeEnabled, currentRemoteWorkerSettings());
        } catch (Exception e) {
            log.error("Failed to save performance config", e);
        }
    }

    /**
     * 保存性能测试配置（供外部调用，如退出时）
     */
    public void save() {
        try {
            autoSaveSupport.cancel();
            if (treeModel == null || persistenceService == null) {
                return;
            }
            propertyPanelSupport.forceCommitAllSpinners();
            saveAllPropertyPanelData();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
            persistenceService.save(root, efficientMode, trendEnabled, reportRealtimeEnabled, currentRemoteWorkerSettings());
        } catch (Exception e) {
            log.error("Failed to save performance config", e);
        }
    }

    /**
     * 同步最新的 HttpRequestItem 到性能测试树中对应的节点（由 Collections 保存时调用）
     * 避免用户在 editSubPanel 修改并保存后，PerformancePanel 仍持有旧数据。
     *
     * @param item 已保存的最新请求数据
     */
    public void syncRequestItem(HttpRequestItem item) {
        requestSyncSupport.syncRequestItem(
                (DefaultMutableTreeNode) treeModel.getRoot(),
                item,
                currentRequestNode,
                this::switchRequestEditor
        );
    }

    /**
     * 清理资源（应用退出时调用）
     * 确保定时器被正确停止，避免资源泄漏
     */
    public void cleanup() {
        // 1. 停止运行中的测试
        if (running) {
            stopRun();
        }

        // 2. 停止定时器
        if (timerManager != null) {
            timerManager.stopAll();
            timerManager.dispose();
        }
        if (statisticsCoordinator != null) {
            statisticsCoordinator.dispose();
        }
        if (performanceResultTablePanel != null) {
            performanceResultTablePanel.dispose();
        }
    }

    private void clearCachedPerformanceResults() {
        if (statisticsCoordinator != null) {
            statisticsCoordinator.resetForNewRun();
        }
        performanceResultTablePanel.clearResults();
        performanceReportPanel.clearReport();
        performanceTrendPanel.clearTrendDataset();
        statsCollector.clear();
        trendWindowCollector.clear();
        trendWindowCollector.setEnabled(trendEnabled);
        executionEngine.resetVirtualUsers();
    }

    private void applyTrendEnabled(boolean enabled) {
        trendEnabled = enabled;
        trendWindowCollector.setEnabled(enabled);
        if (timerManager != null) {
            timerManager.setTrendSamplingEnabled(enabled);
        }
        syncTrendResultTabState();
    }

    private void applyReportRealtimeEnabled(boolean enabled) {
        reportRealtimeEnabled = enabled;
        if (timerManager != null) {
            timerManager.setReportRefreshEnabled(enabled);
        }
    }

    private void refreshReportSnapshot() {
        if (statisticsCoordinator != null) {
            statisticsCoordinator.updateReportWithLatestDataSync();
        }
    }

    private void syncTrendResultTabState() {
        if (trendCheckBox != null) {
            trendCheckBox.setSelected(trendEnabled);
        }
    }

    /**
     * 从集合中刷新请求数据
     * 重新加载所有请求的最新配置
     */
    private void refreshRequestsFromCollections() {
        currentRequestNode = requestSyncSupport.refreshRequestsFromCollections(
                currentRequestNode,
                this::switchRequestEditor,
                this::saveAllPropertyPanelData,
                this::clearCachedPerformanceResults,
                this::saveConfig
        );
    }
}
