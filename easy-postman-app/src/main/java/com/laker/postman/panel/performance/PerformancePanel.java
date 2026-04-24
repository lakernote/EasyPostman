package com.laker.postman.panel.performance;

import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.DebouncedSaveSupport;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.common.component.button.StartButton;
import com.laker.postman.common.component.button.StopButton;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.panel.performance.assertion.AssertionPropertyPanel;
import com.laker.postman.panel.performance.model.ApiMetadata;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.RequestResult;
import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import com.laker.postman.panel.performance.result.PerformanceTrendPanel;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupPropertyPanel;
import com.laker.postman.panel.performance.timer.TimerPropertyPanel;
import com.laker.postman.service.PerformancePersistenceService;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 左侧多层级树（用户组-请求-断言-定时器），右侧属性区，底部Tab结果区
 */
@Slf4j
public class PerformancePanel extends SingletonBasePanel {
    public static final String EMPTY = "empty";
    public static final String THREAD_GROUP = "threadGroup";
    public static final String REQUEST = "request";
    public static final String ASSERTION = "assertion";
    public static final String TIMER = "timer";
    public static final String SSE_CONNECT = "sseConnect";
    public static final String SSE_AWAIT = "sseAwait";
    public static final String WS_CONNECT = "wsConnect";
    public static final String WS_SEND = "wsSend";
    public static final String WS_AWAIT = "wsAwait";
    public static final String WS_CLOSE = "wsClose";
    private JTree jmeterTree;
    private DefaultTreeModel treeModel;
    private JPanel propertyPanel; // 右侧属性区（CardLayout）
    private CardLayout propertyCardLayout;
    private JTabbedPane resultTabbedPane; // 结果Tab
    private ThreadGroupPropertyPanel threadGroupPanel;
    private AssertionPropertyPanel assertionPanel;
    private TimerPropertyPanel timerPanel;
    private SseStagePropertyPanel sseConnectPanel;
    private SseStagePropertyPanel sseAwaitPanel;
    private WebSocketStagePropertyPanel wsConnectPanel;
    private WebSocketStagePropertyPanel wsSendPanel;
    private WebSocketStagePropertyPanel wsAwaitPanel;
    private WebSocketStagePropertyPanel wsClosePanel;
    private JPanel requestEditorHost;
    private volatile boolean running = false;
    private transient Thread runThread;
    private StartButton runBtn;
    private StopButton stopBtn;
    private RefreshButton refreshBtn;
    private JCheckBox efficientCheckBox; // 高效模式复选框
    private JLabel progressLabel; // 进度标签
    private JPanel topPanel; // 顶部工具栏面板，用于主题切换时更新边框
    private long startTime;
    // 记录所有请求的结果（包含开始时间、结束时间、APIID等）
    private final transient List<RequestResult> allRequestResults = Collections.synchronizedList(new ArrayList<>());
    // 按接口统计
    private final Map<String, List<Long>> apiCostMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> apiSuccessMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> apiFailMap = new ConcurrentHashMap<>();
    // 统计数据保护锁
    private final transient Object statsLock = new Object();

    // 定时器管理器 - 统一管理趋势图采样和报表刷新定时器
    private transient PerformanceTimerManager timerManager;

    // 高效模式
    private boolean efficientMode = true;

    private PerformanceReportPanel performanceReportPanel;
    private PerformanceResultTablePanel performanceResultTablePanel;
    private PerformanceTrendPanel performanceTrendPanel;


    // CSV 数据管理面板
    private CsvDataPanel csvDataPanel;

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
    private transient PerformanceRequestEditorSupport requestEditorSupport;
    private final transient PerformanceSaveShortcutSupport saveShortcutSupport = new PerformanceSaveShortcutSupport();
    private final transient DebouncedSaveSupport autoSaveSupport = new DebouncedSaveSupport(500, this::saveConfigAsync);

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        this.persistenceService = SingletonFactory.getInstance(PerformancePersistenceService.class);
        initTimerManager();
        efficientMode = persistenceService.loadEfficientMode();
        DefaultMutableTreeNode root = loadPersistedOrDefaultRoot();

        treeModel = new DefaultTreeModel(root);
        treeSupport = new PerformanceTreeSupport(treeModel);
        PerformancePanelViewFactory viewFactory = new PerformancePanelViewFactory();
        PerformancePanelViewFactory.TreeSection treeSection = viewFactory.createTreeSection(treeModel);
        jmeterTree = treeSection.tree();
        requestSyncSupport = new PerformanceRequestSyncSupport(
                treeModel,
                jmeterTree,
                persistenceService,
                this::syncRequestStructure
        );
        PerformancePanelViewFactory.PropertySection propertySection = viewFactory.createPropertySection(
                this::refreshCurrentRequest,
                EMPTY,
                THREAD_GROUP,
                REQUEST,
                ASSERTION,
                TIMER,
                SSE_CONNECT,
                SSE_AWAIT,
                WS_CONNECT,
                WS_SEND,
                WS_AWAIT,
                WS_CLOSE
        );
        propertyPanel = propertySection.propertyPanel();
        propertyCardLayout = propertySection.propertyCardLayout();
        threadGroupPanel = propertySection.threadGroupPanel();
        assertionPanel = propertySection.assertionPanel();
        timerPanel = propertySection.timerPanel();
        sseConnectPanel = propertySection.sseConnectPanel();
        sseAwaitPanel = propertySection.sseAwaitPanel();
        wsConnectPanel = propertySection.wsConnectPanel();
        wsSendPanel = propertySection.wsSendPanel();
        wsAwaitPanel = propertySection.wsAwaitPanel();
        wsClosePanel = propertySection.wsClosePanel();
        RequestEditSubPanel requestEditSubPanel = propertySection.requestEditSubPanel();
        requestEditorHost = propertySection.requestEditorHost();
        requestEditorSupport = new PerformanceRequestEditorSupport(
                requestEditSubPanel,
                requestEditorHost,
                this::resolveRequestProtocol
        );

        propertyPanelSupport = new PerformancePropertyPanelSupport(
                jmeterTree,
                threadGroupPanel,
                assertionPanel,
                timerPanel,
                sseConnectPanel,
                sseAwaitPanel,
                wsConnectPanel,
                wsSendPanel,
                wsAwaitPanel,
                wsClosePanel,
                requestEditorSupport::getRequestEditSubPanel,
                () -> currentRequestNode,
                this::saveRequestNodeData,
                treeSupport,
                this::syncRequestStructure
        );

        PerformancePanelViewFactory.ResultSection resultSection = viewFactory.createResultSection();
        resultTabbedPane = resultSection.resultTabbedPane();
        performanceResultTablePanel = resultSection.performanceResultTablePanel();
        performanceTrendPanel = resultSection.performanceTrendPanel();
        performanceReportPanel = resultSection.performanceReportPanel();
        statisticsCoordinator = new PerformanceStatisticsCoordinator(
                statsLock,
                allRequestResults,
                apiCostMap,
                apiSuccessMap,
                apiFailMap,
                performanceReportPanel,
                performanceTrendPanel,
                resultTabbedPane,
                () -> executionEngine != null ? executionEngine.getActiveThreads() : 0,
                () -> timerManager != null ? timerManager.getSamplingIntervalMs() : 1000L
        );
        timerManager.setTrendSamplingCallback(statisticsCoordinator::sampleTrendData);
        timerManager.setReportRefreshCallback(statisticsCoordinator::refreshReport);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeSection.scrollPane(), propertyPanel);
        mainSplit.setDividerLocation(360);
        mainSplit.setDividerSize(3);
        mainSplit.setContinuousLayout(true);

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainSplit, resultTabbedPane);
        verticalSplit.setDividerSize(3);
        verticalSplit.setContinuousLayout(true);
        verticalSplit.setDividerLocation(260);
        mainSplit.setMinimumSize(new Dimension(400, 150));
        resultTabbedPane.setMinimumSize(new Dimension(400, 150));
        add(verticalSplit, BorderLayout.CENTER);

        PerformancePanelViewFactory.ToolbarSection toolbarSection = viewFactory.createToolbarSection(
                this,
                efficientMode,
                persistenceService,
                this::refreshRequestsFromCollections,
                value -> efficientMode = value,
                this::saveAllPropertyPanelData,
                this::saveConfig
        );
        topPanel = toolbarSection.topPanel();
        runBtn = toolbarSection.runBtn();
        stopBtn = toolbarSection.stopBtn();
        refreshBtn = toolbarSection.refreshBtn();
        efficientCheckBox = toolbarSection.efficientCheckBox();
        csvDataPanel = toolbarSection.csvDataPanel();
        progressLabel = toolbarSection.progressLabel();
        add(topPanel, BorderLayout.NORTH);

        executionEngine = new PerformanceExecutionEngine(
                this,
                () -> running,
                () -> efficientMode,
                csvDataPanel,
                allRequestResults,
                apiCostMap,
                apiSuccessMap,
                apiFailMap,
                statsLock,
                performanceResultTablePanel
        );
        runControlSupport = new PerformanceRunControlSupport(
                this,
                () -> running,
                value -> running = value,
                () -> startTime,
                value -> startTime = value,
                propertyPanelSupport,
                executionEngine,
                statisticsCoordinator,
                timerManager,
                runBtn,
                stopBtn,
                refreshBtn,
                efficientCheckBox,
                resultTabbedPane,
                performanceResultTablePanel,
                performanceReportPanel,
                performanceTrendPanel,
                allRequestResults,
                statsLock,
                apiCostMap,
                apiSuccessMap,
                apiFailMap,
                this::clearCachedPerformanceResults
        );

        treeSupport.syncAllRequestStructures((DefaultMutableTreeNode) treeModel.getRoot());
        runBtn.addActionListener(e -> startRun(progressLabel));
        stopBtn.addActionListener(e -> stopRun());
        for (int i = 0; i < jmeterTree.getRowCount(); i++) {
            jmeterTree.expandRow(i);
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

        efficientMode = persistenceService.loadEfficientMode();
        DefaultMutableTreeNode root = loadPersistedOrDefaultRoot();
        treeModel.setRoot(root);
        treeSupport.syncAllRequestStructures(root);
        currentRequestNode = null;
        if (efficientCheckBox != null) {
            efficientCheckBox.setSelected(efficientMode);
        }
        if (csvDataPanel != null) {
            csvDataPanel.restoreState(persistenceService.loadCsvState());
        }
        clearCachedPerformanceResults();
        propertyCardLayout.show(propertyPanel, EMPTY);
        for (int i = 0; i < jmeterTree.getRowCount(); i++) {
            jmeterTree.expandRow(i);
        }
        selectFirstThreadGroup();
    }

    private boolean isReadyForWorkspaceSwitch() {
        return treeModel != null
                && persistenceService != null
                && treeSupport != null
                && jmeterTree != null
                && propertyPanel != null
                && propertyCardLayout != null
                && threadGroupPanel != null
                && performanceResultTablePanel != null
                && performanceReportPanel != null
                && performanceTrendPanel != null
                && executionEngine != null;
    }

    private DefaultMutableTreeNode loadPersistedOrDefaultRoot() {
        DefaultMutableTreeNode savedRoot = persistenceService.load(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TEST_PLAN));
        if (savedRoot != null) {
            return savedRoot;
        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TEST_PLAN), NodeType.ROOT));
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
            jmeterTree.setSelectionPath(path);
            jmeterTree.scrollPathToVisible(path);

            // 触发节点点击事件，确保属性面板显示正确
            Object userObj = firstGroup.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.THREAD_GROUP) {
                // 显示线程组属性面板
                propertyCardLayout.show(propertyPanel, THREAD_GROUP);
                threadGroupPanel.setThreadGroupData(jtNode);
            }
        }
    }

    private RequestItemProtocolEnum resolveRequestProtocol(HttpRequestItem item) {
        return treeSupport.resolveRequestProtocol(item);
    }

    private void syncRequestStructure(DefaultMutableTreeNode requestNode, JMeterTreeNode requestData) {
        treeSupport.syncRequestStructure(requestNode, requestData);
    }

    private void saveSseStageNode(DefaultMutableTreeNode stageNode) {
        propertyPanelSupport.saveSseStageNode(stageNode);
    }

    private void saveWebSocketStageNode(DefaultMutableTreeNode stageNode) {
        propertyPanelSupport.saveWebSocketStageNode(stageNode);
        if (stageNode != null && stageNode.getUserObject() instanceof JMeterTreeNode stageJtNode
                && stageJtNode.type == NodeType.WS_CONNECT) {
            treeModel.nodeChanged(stageNode);
        }
    }

    private void switchRequestEditor(HttpRequestItem item) {
        requestEditorSupport.switchRequestEditor(item);
    }

    private void saveRequestNodeData(DefaultMutableTreeNode node) {
        requestEditorSupport.saveRequestNodeData(node, this::syncRequestStructure);
    }

    /**
     * 刷新当前选中的请求节点
     */
    private void refreshCurrentRequest() {
        currentRequestNode = requestSyncSupport.refreshCurrentRequest(
                currentRequestNode,
                requestEditorSupport.getRequestEditSubPanel(),
                this::switchRequestEditor,
                this::saveConfig
        );
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
        persistenceService.save(root, efficientMode, csvDataPanel != null ? csvDataPanel.exportState() : null);
        NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_SAVE_SUCCESS));
    }

    private void saveAllPropertyPanelData() {
        propertyPanelSupport.saveAllPropertyPanelData();
    }

    // ========== 定时器管理 ==========

    /**
     * 初始化定时器管理器
     */
    private void initTimerManager() {
        timerManager = new PerformanceTimerManager(() -> running);
        log.debug("定时器管理器初始化完成");
    }

    // ========== 执行与停止核心逻辑 ==========
    private void startRun(JLabel progressLabel) {
        runThread = runControlSupport.startRun(
                (DefaultMutableTreeNode) treeModel.getRoot(),
                progressLabel,
                efficientMode,
                value -> efficientMode = value
        );
    }

    @Override
    protected void registerListeners() {
        new PerformanceTreeInteractionSupport(
                this,
                jmeterTree,
                treeModel,
                propertyCardLayout,
                propertyPanel,
                threadGroupPanel,
                assertionPanel,
                timerPanel,
                sseConnectPanel,
                sseAwaitPanel,
                wsConnectPanel,
                wsSendPanel,
                wsAwaitPanel,
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
                REQUEST,
                ASSERTION,
                TIMER,
                SSE_CONNECT,
                SSE_AWAIT,
                WS_CONNECT,
                WS_SEND,
                WS_AWAIT,
                WS_CLOSE
        ).install();
    }

    private void stopRun() {
        runControlSupport.stopRun(runThread);
    }

    /**
     * 保存当前配置
     */
    private void saveConfig() {
        // 树节点编辑、属性面板和 CSV 变化都会触发保存，统一防抖减少频繁写盘。
        autoSaveSupport.requestSave();
    }

    private void saveConfigAsync() {
        try {
            // 保存所有属性面板数据到树节点
            saveAllPropertyPanelData();
            // 获取根节点
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
            // 异步保存配置
            persistenceService.saveAsync(root, efficientMode, csvDataPanel != null ? csvDataPanel.exportState() : null);
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
            persistenceService.save(root, efficientMode, csvDataPanel != null ? csvDataPanel.exportState() : null);
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
        log.info("清理 PerformancePanel 资源");

        // 1. 停止定时器
        if (timerManager != null) {
            timerManager.stopAll();
            timerManager.dispose();
        }

        // 2. 停止运行中的测试
        if (running) {
            stopRun();
        }

        log.debug("PerformancePanel 资源清理完成");
    }

    private void clearCachedPerformanceResults() {
        performanceResultTablePanel.clearResults();
        performanceReportPanel.clearReport();
        performanceTrendPanel.clearTrendDataset();
        apiCostMap.clear();
        apiSuccessMap.clear();
        apiFailMap.clear();
        allRequestResults.clear();
        ApiMetadata.clear();
        executionEngine.resetVirtualUsers();
    }

    /**
     * 从集合中刷新请求数据
     * 重新加载所有请求的最新配置
     */
    private void refreshRequestsFromCollections() {
        currentRequestNode = requestSyncSupport.refreshRequestsFromCollections(
                currentRequestNode,
                requestEditorSupport.getRequestEditSubPanel(),
                this::switchRequestEditor,
                this::saveAllPropertyPanelData,
                this::clearCachedPerformanceResults,
                this::saveConfig
        );
    }
}
