package com.laker.postman.panel.performance;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.common.component.MemoryLabel;
import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.common.component.button.StartButton;
import com.laker.postman.common.component.button.StopButton;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.panel.performance.assertion.AssertionData;
import com.laker.postman.panel.performance.assertion.AssertionPropertyPanel;
import com.laker.postman.panel.performance.component.JMeterTreeCellRenderer;
import com.laker.postman.panel.performance.component.TreeNodeTransferHandler;
import com.laker.postman.panel.performance.model.*;
import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import com.laker.postman.panel.performance.result.PerformanceTrendPanel;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupPropertyPanel;
import com.laker.postman.panel.performance.timer.TimerPropertyPanel;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.PerformancePersistenceService;
import com.laker.postman.service.collections.RequestCollectionsService;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.service.js.ScriptExecutionResult;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;
import org.jfree.data.time.Second;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
    private JTree jmeterTree;
    private DefaultTreeModel treeModel;
    private JPanel propertyPanel; // 右侧属性区（CardLayout）
    private CardLayout propertyCardLayout;
    private JTabbedPane resultTabbedPane; // 结果Tab
    private ThreadGroupPropertyPanel threadGroupPanel;
    private AssertionPropertyPanel assertionPanel;
    private TimerPropertyPanel timerPanel;
    private RequestEditSubPanel requestEditSubPanel;
    private volatile boolean running = false;
    private transient Thread runThread;
    private StartButton runBtn;
    private StopButton stopBtn;
    private JButton refreshBtn;
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
    // 活跃线程计数器
    private final AtomicInteger activeThreads = new AtomicInteger(0);


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
    // CSV行索引分配器
    private final AtomicInteger csvRowIndex = new AtomicInteger(0);

    // 持久化服务
    private transient PerformancePersistenceService persistenceService;

    // 当前选中的请求节点
    private DefaultMutableTreeNode currentRequestNode;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());

        // 初始化持久化服务
        this.persistenceService = SingletonFactory.getInstance(PerformancePersistenceService.class);

        // 初始化定时器管理器
        initTimerManager();

        // 加载高效模式设置
        efficientMode = persistenceService.loadEfficientMode();

        // 1. 左侧树结构
        DefaultMutableTreeNode root;
        // 尝试加载保存的配置
        DefaultMutableTreeNode savedRoot = persistenceService.load(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TEST_PLAN));
        if (savedRoot != null) {
            root = savedRoot;
        } else {
            // 如果没有保存的配置，创建默认树结构
            root = new DefaultMutableTreeNode(new JMeterTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TEST_PLAN), NodeType.ROOT));
            createDefaultRequest(root);
        }
        treeModel = new DefaultTreeModel(root);
        jmeterTree = new JTree(treeModel);
        jmeterTree.setRootVisible(true);
        jmeterTree.setShowsRootHandles(true);
        jmeterTree.setCellRenderer(new JMeterTreeCellRenderer());
        // 允许多选，支持批量操作
        jmeterTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        // 只允许单节点拖拽
        jmeterTree.setDragEnabled(true);
        jmeterTree.setDropMode(DropMode.ON_OR_INSERT);
        jmeterTree.setTransferHandler(new TreeNodeTransferHandler(jmeterTree, treeModel));
        JScrollPane treeScroll = new JScrollPane(jmeterTree);
        treeScroll.setPreferredSize(new Dimension(260, 300));

        // 2. 右侧属性区（CardLayout）
        propertyCardLayout = new CardLayout();
        propertyPanel = new JPanel(propertyCardLayout);
        propertyPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_PROPERTY_SELECT_NODE)), EMPTY);
        threadGroupPanel = new ThreadGroupPropertyPanel();
        propertyPanel.add(threadGroupPanel, THREAD_GROUP);

        // 创建请求编辑面板的包装器，添加提示信息
        requestEditSubPanel = new RequestEditSubPanel("", RequestItemProtocolEnum.HTTP);
        JPanel requestWrapperPanel = createRequestEditPanelWithInfoBar();
        propertyPanel.add(requestWrapperPanel, REQUEST);

        assertionPanel = new AssertionPropertyPanel();
        propertyPanel.add(assertionPanel, ASSERTION);
        timerPanel = new TimerPropertyPanel();
        propertyPanel.add(timerPanel, TIMER);
        propertyCardLayout.show(propertyPanel, EMPTY);

        // 3. 结果区
        resultTabbedPane = new JTabbedPane();
        // 结果树面板
        performanceResultTablePanel = new PerformanceResultTablePanel();
        // 趋势图面板
        performanceTrendPanel = SingletonFactory.getInstance(PerformanceTrendPanel.class);
        // 报告面板
        performanceReportPanel = new PerformanceReportPanel();

        resultTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_TREND), performanceTrendPanel);
        resultTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_REPORT), performanceReportPanel);
        resultTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_RESULT_TREE), performanceResultTablePanel);

        // 主分割（左树-右属性）
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, propertyPanel);
        mainSplit.setDividerLocation(360);
        mainSplit.setDividerSize(3);
        mainSplit.setContinuousLayout(true);

        // 下部分（主分割+结果Tab）
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainSplit, resultTabbedPane);
        verticalSplit.setDividerSize(3); // 分割条宽度
        verticalSplit.setContinuousLayout(true); // 连续布局
        verticalSplit.setDividerLocation(260); // 初始位置
        // 设置最小尺寸，确保拖动范围足够大
        mainSplit.setMinimumSize(new Dimension(400, 150));
        resultTabbedPane.setMinimumSize(new Dimension(400, 150));

        add(verticalSplit, BorderLayout.CENTER);


        // 保存/加载用例按钮 ==========
        topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()));
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        runBtn = new StartButton();
        stopBtn = new StopButton();
        stopBtn.setEnabled(false);
        btnPanel.add(runBtn);
        btnPanel.add(stopBtn);

        // 刷新按钮
        refreshBtn = new RefreshButton();
        refreshBtn.addActionListener(e -> refreshRequestsFromCollections());
        btnPanel.add(refreshBtn);

        // 高效模式checkbox - 更醒目的样式和更好的交互
        efficientCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.PERFORMANCE_EFFICIENT_MODE));
        efficientCheckBox.setSelected(efficientMode); // 使用加载的高效模式设置
        // 设置为粗体并增大字体
        efficientCheckBox.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        // 设置醒目的前景色
        efficientCheckBox.setForeground(new Color(0, 128, 0)); // 深绿色表示推荐

        // 创建多行HTML格式的详细提示
        String htmlTooltip = "<html><body style='width: 400px; padding: 10px;'>" +
                "<b style='color: #008000; font-size: 13px;'>" + I18nUtil.getMessage(MessageKeys.PERFORMANCE_EFFICIENT_MODE) + "</b><br><br>" +
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_EFFICIENT_MODE_TOOLTIP_HTML) +
                "</body></html>";
        efficientCheckBox.setToolTipText(htmlTooltip);


        efficientCheckBox.addActionListener(e -> {
            // 如果用户尝试关闭高效模式，给出强烈警告
            if (!efficientCheckBox.isSelected()) {
                // 显示警告对话框，让用户确认是否真的要关闭
                int result = JOptionPane.showConfirmDialog(PerformancePanel.this,
                        I18nUtil.getMessage(MessageKeys.PERFORMANCE_EFFICIENT_MODE_DISABLE_WARNING),
                        I18nUtil.getMessage(MessageKeys.PERFORMANCE_EFFICIENT_MODE_WARNING_TITLE),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                // 如果用户选择NO（不关闭），则保持开启状态
                if (result != JOptionPane.YES_OPTION) {
                    efficientCheckBox.setSelected(true);
                    return;
                }
                // 如果用户坚持要关闭，则允许但更新状态
            }
            efficientMode = efficientCheckBox.isSelected();
            // 保存高效模式设置
            saveAllPropertyPanelData();
        });
        btnPanel.add(efficientCheckBox);
        csvDataPanel = new CsvDataPanel();
        btnPanel.add(csvDataPanel);
        topPanel.add(btnPanel, BorderLayout.WEST);
        // ========== 执行进度指示器 ==========
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        progressLabel = new JLabel();
        progressLabel.setText("0/0");
        progressLabel.setFont(progressLabel.getFont().deriveFont(Font.BOLD)); // 设置粗体
        progressLabel.setIcon(new FlatSVGIcon("icons/users.svg", 20, 20)
                .setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")))); // 使用FlatLaf SVG图标
        progressLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        progressPanel.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_PROGRESS_TOOLTIP));
        progressPanel.add(progressLabel);
        // ========== 内存占用显示 ==========
        MemoryLabel memoryLabel = new MemoryLabel();
        progressPanel.add(memoryLabel);

        topPanel.add(progressPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        runBtn.addActionListener(e -> startRun(progressLabel));
        stopBtn.addActionListener(e -> stopRun());

        // 展开所有节点
        for (int i = 0; i < jmeterTree.getRowCount(); i++) {
            jmeterTree.expandRow(i);
        }

        // 初始化定位到第一个Thread Group节点
        selectFirstThreadGroup();

        // 设置 Ctrl/Cmd+S 快捷键保存
        setupSaveShortcut();
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

    /**
     * 创建包含提示信息栏的请求编辑面板
     */
    private JPanel createRequestEditPanelWithInfoBar() {
        JPanel wrapper = new JPanel(new BorderLayout());

        // 创建顶部信息提示栏
        JPanel infoBar = new JPanel(new BorderLayout());
        infoBar.setBackground(new Color(255, 250, 205)); // 淡黄色背景
        infoBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 220, 170)),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));

        // 左侧：信息图标和文本
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);

        JLabel infoIcon = new JLabel(IconUtil.createThemed("icons/info.svg", 14, 14));
        leftPanel.add(infoIcon);

        JLabel infoText = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REQUEST_COPY_INFO));
        infoText.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1)); // 比标准字体小1号
        infoText.setForeground(new Color(102, 85, 0)); // 深黄色文字
        leftPanel.add(infoText);


        JButton refreshCurrentBtn = new JButton();
        refreshCurrentBtn.setIcon(IconUtil.createThemed("icons/refresh.svg", 14, 14));
        refreshCurrentBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // 手型光标
        refreshCurrentBtn.setFocusable(false); // 去掉聚焦边框
        refreshCurrentBtn.addActionListener(e -> refreshCurrentRequest());
        leftPanel.add(refreshCurrentBtn);
        infoBar.add(leftPanel, BorderLayout.CENTER);

        // 组装
        wrapper.add(infoBar, BorderLayout.NORTH);
        wrapper.add(requestEditSubPanel, BorderLayout.CENTER);

        return wrapper;
    }

    /**
     * 刷新当前选中的请求节点
     */
    private void refreshCurrentRequest() {
        if (currentRequestNode == null) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_NO_REQUEST_SELECTED));
            return;
        }

        Object userObj = currentRequestNode.getUserObject();
        if (!(userObj instanceof JMeterTreeNode jmNode) || jmNode.type != NodeType.REQUEST) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_NO_REQUEST_SELECTED));
            return;
        }

        if (jmNode.httpRequestItem == null) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_NO_REQUEST_SELECTED));
            return;
        }

        // 从集合中查找最新的请求
        String requestId = jmNode.httpRequestItem.getId();
        if (requestId == null || requestId.trim().isEmpty()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REQUEST_NOT_FOUND_IN_COLLECTIONS));
            return;
        }

        HttpRequestItem latestRequestItem = persistenceService.findRequestItemById(requestId);

        if (latestRequestItem == null) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REQUEST_NOT_FOUND_IN_COLLECTIONS));
            return;
        }

        // 更新节点中的请求数据
        jmNode.httpRequestItem = latestRequestItem;
        jmNode.name = latestRequestItem.getName();
        treeModel.nodeChanged(currentRequestNode);

        // 刷新右侧编辑面板
        requestEditSubPanel.initPanelData(latestRequestItem);

        // 保存配置
        saveConfig();

        NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REQUEST_REFRESHED));
    }

    private static void createDefaultRequest(DefaultMutableTreeNode root) {
        // 默认添加一个用户组和一个请求（www.baidu.com）
        DefaultMutableTreeNode group = new DefaultMutableTreeNode(new JMeterTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_THREAD_GROUP), NodeType.THREAD_GROUP));
        HttpRequestItem defaultReq = new HttpRequestItem();
        defaultReq.setName(I18nUtil.getMessage(MessageKeys.PERFORMANCE_DEFAULT_REQUEST));
        defaultReq.setMethod("GET");
        defaultReq.setUrl("https://www.baidu.com");
        DefaultMutableTreeNode req = new DefaultMutableTreeNode(new JMeterTreeNode(defaultReq.getName(), NodeType.REQUEST, defaultReq));
        group.add(req);
        root.add(group);
    }

    /**
     * 设置 Ctrl/Cmd+S 快捷键保存所有配置
     */
    private void setupSaveShortcut() {
        // 获取 InputMap 和 ActionMap
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        // 根据操作系统选择快捷键（macOS 用 Cmd，其他用 Ctrl）
        int modifierKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        KeyStroke saveKeyStroke = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, modifierKey);

        // 绑定快捷键
        inputMap.put(saveKeyStroke, "savePerformanceConfig");
        actionMap.put("savePerformanceConfig", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                handleSaveShortcut();
            }
        });
    }

    /**
     * 处理 Ctrl/Cmd+S 快捷键
     * 1. 强制提交所有 EasyJSpinner 的值
     * 2. 保存所有属性面板数据到树节点
     * 3. 持久化到文件
     * 4. 显示成功提示
     */
    private void handleSaveShortcut() {
        // 1. 强制提交所有 EasyJSpinner 的值
        threadGroupPanel.forceCommitAllSpinners();

        // 2. 保存所有属性面板数据
        saveAllPropertyPanelData();

        // 3. 持久化到文件
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        persistenceService.save(root, efficientMode);

        // 4. 显示成功提示
        NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_SAVE_SUCCESS));
    }

    private void saveAllPropertyPanelData() {
        // 保存所有属性区数据到树节点
        threadGroupPanel.saveThreadGroupData();
        assertionPanel.saveAssertionData();
        timerPanel.saveTimerData();
        if (requestEditSubPanel != null && currentRequestNode != null) {
            // 保存RequestEditSubPanel表单到当前选中的请求节点
            Object userObj = currentRequestNode.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST) {
                jtNode.httpRequestItem = requestEditSubPanel.getCurrentRequest();
            }
        }
    }

    // ========== 定时器管理 ==========

    /**
     * 初始化定时器管理器
     */
    private void initTimerManager() {
        // 创建定时器管理器，传入运行状态检查器
        timerManager = new PerformanceTimerManager(() -> running);

        // 设置趋势图采样回调
        timerManager.setTrendSamplingCallback(this::sampleTrendData);

        // 设置报表刷新回调
        timerManager.setReportRefreshCallback(this::refreshReport);

        log.debug("定时器管理器初始化完成");
    }

    // ========== 执行与停止核心逻辑 ==========
    private void startRun(JLabel progressLabel) {
        saveAllPropertyPanelData();

        // 检查高并发场景下是否开启高效模式
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();
        long estimatedRequests = estimateTotalRequests(rootNode);

        // 如果预计请求数超过阈值（例如5000）且未开启高效模式，给出警告
        final int HIGH_CONCURRENCY_THRESHOLD = 5000;
        if (estimatedRequests >= HIGH_CONCURRENCY_THRESHOLD && !efficientMode) {
            String message = I18nUtil.getMessage(MessageKeys.PERFORMANCE_EFFICIENT_MODE_WARNING_MSG,
                    String.format("%,d", estimatedRequests));
            int result = JOptionPane.showConfirmDialog(this,
                    message,
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_EFFICIENT_MODE_WARNING_TITLE),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                // 用户选择开启高效模式
                efficientMode = true;
                efficientCheckBox.setSelected(true); // 更新UI状态
                saveAllPropertyPanelData();
                NotificationUtil.showInfo("✅ " + I18nUtil.getMessage(MessageKeys.PERFORMANCE_EFFICIENT_MODE) + " enabled");
            }
            // 如果用户选择NO，继续执行但可能面临内存问题
        }

        // 配置 OkHttp 连接池和 Dispatcher 并发参数
        OkHttpClientManager.setConnectionPoolConfig(getJmeterMaxIdleConnections(), getJmeterKeepAliveSeconds());
        OkHttpClientManager.setDispatcherConfig(getJmeterMaxRequests(), getJmeterMaxRequestsPerHost());

        if (running) return;
        running = true;
        runBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        refreshBtn.setEnabled(false); // 运行时禁用刷新按钮
        resultTabbedPane.setSelectedIndex(0); // 切换到趋势图Tab
        performanceResultTablePanel.clearResults(); // 清空结果树
        performanceReportPanel.clearReport(); // 清空报表数据
        performanceTrendPanel.clearTrendDataset(); // 清理趋势图历史数据
        apiCostMap.clear();
        apiSuccessMap.clear();
        apiFailMap.clear();
        allRequestResults.clear();
        ApiMetadata.clear(); // 清理API元数据
        // CSV行索引重置
        csvRowIndex.set(0);

        // 启动所有定时器（趋势图采样 + 报表刷新）
        timerManager.startAll();

        // 重要：更新开始时间，确保递增线程等模式正常工作
        startTime = System.currentTimeMillis();

        // 统计总用户数
        int totalThreads = getTotalThreads(rootNode);
        // 当前已启动线程数 = 0，启动后动态刷新
        progressLabel.setText(0 + "/" + totalThreads);
        runThread = new Thread(() -> {
            try {
                runJMeterTreeWithProgress(rootNode, progressLabel, totalThreads);
            } finally {
                // 等待所有请求完成统计（避免数据竞争）
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                SwingUtilities.invokeLater(() -> {
                    running = false;
                    runBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    refreshBtn.setEnabled(true); // 测试完成时重新启用刷新按钮
                    timerManager.stopAll(); // 停止所有定时器

                    // 强制刷新 ResultTable 的 pendingQueue（确保所有待处理的结果都显示）
                    try {
                        performanceResultTablePanel.flushPendingResults();
                    } catch (Exception e) {
                        log.warn("完成时刷新结果树失败", e);
                    }

                    // 执行最后一次趋势图采样，避免漏掉完成前最后时刻的数据
                    try {
                        // 使用同步版本采样，确保数据立即更新
                        sampleTrendDataSync();
                    } catch (Exception e) {
                        log.warn("完成时最后一次趋势图采样失败", e);
                    }

                    OkHttpClientManager.setDefaultConnectionPoolConfig();

                    // 使用同步版本刷新报表，确保数据立即更新
                    updateReportWithLatestDataSync();

                    // 显示执行完成提示
                    long totalTime = System.currentTimeMillis() - startTime;
                    int totalRequests;
                    long successCount;

                    // 使用 statsLock 保护对 allRequestResults 的访问，避免 ConcurrentModificationException
                    synchronized (statsLock) {
                        totalRequests = allRequestResults.size();
                        successCount = allRequestResults.stream().filter(r -> r.success).count();
                    }

                    String message = I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_EXECUTION_COMPLETED,
                            totalRequests, successCount, totalTime / 1000.0);
                    NotificationUtil.showSuccess(message);
                });
            }
        });
        runThread.start();
    }

    /**
     * 遍历所有线程组，按各自模式计算总线程数
     */
    private int getTotalThreads(DefaultMutableTreeNode rootNode) {
        int total = 0;
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.THREAD_GROUP) {
                // 只计算已启用的线程组
                if (!jtNode.enabled) {
                    continue;
                }
                ThreadGroupData tg = jtNode.threadGroupData != null ? jtNode.threadGroupData : new ThreadGroupData();
                switch (tg.threadMode) {
                    case FIXED -> total += tg.numThreads;
                    case RAMP_UP -> total += tg.rampUpEndThreads;
                    case SPIKE -> total += tg.spikeMaxThreads;
                    case STAIRS -> total += tg.stairsEndThreads;
                }
            }
        }
        return total;
    }

    /**
     * 估算总请求数，用于判断是否需要提示用户开启高效模式
     * 计算公式：所有线程组的 (线程数 × 循环次数 × 请求数) 之和
     */
    private long estimateTotalRequests(DefaultMutableTreeNode rootNode) {
        // 平均每个请求耗时（秒），实际大部分请求在 300-500ms，这里取 0.3 秒
        final double AVG_REQUEST_DURATION = 0.3;

        long total = 0;
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            Object userObj = groupNode.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.THREAD_GROUP) {
                // 只计算已启用的线程组
                if (!jtNode.enabled) {
                    continue;
                }

                ThreadGroupData tg = jtNode.threadGroupData != null ? jtNode.threadGroupData : new ThreadGroupData();

                // 统计该线程组下的请求数（只计算已启用的）
                int enabledRequests = 0;
                for (int j = 0; j < groupNode.getChildCount(); j++) {
                    DefaultMutableTreeNode requestNode = (DefaultMutableTreeNode) groupNode.getChildAt(j);
                    Object reqObj = requestNode.getUserObject();
                    if (reqObj instanceof JMeterTreeNode reqJtNode
                            && reqJtNode.type == NodeType.REQUEST
                            && reqJtNode.enabled) {
                        enabledRequests++;
                    }
                }

                if (enabledRequests == 0) {
                    continue; // 没有启用的请求，跳过
                }

                // 根据不同模式估算请求数
                switch (tg.threadMode) {
                    case FIXED -> {
                        // 固定模式：线程数 × 循环次数 × 请求数
                        if (tg.useTime) {
                            // 使用时间模式：每秒可执行 (1 / AVG_REQUEST_DURATION) 个请求
                            // 总请求数 = 线程数 × 持续时间(秒) × 每秒请求数 × 请求种类数
                            double requestsPerSecondPerThread = 1.0 / AVG_REQUEST_DURATION;
                            total += (long) (tg.numThreads * tg.duration * requestsPerSecondPerThread * enabledRequests);
                        } else {
                            // 使用循环次数
                            total += (long) tg.numThreads * tg.loops * enabledRequests;
                        }
                    }
                    case RAMP_UP -> {
                        // 递增模式：平均线程数 × 持续时间 × 每秒请求数 × 请求种类数
                        int avgThreads = (tg.rampUpStartThreads + tg.rampUpEndThreads) / 2;
                        double requestsPerSecondPerThread = 1.0 / AVG_REQUEST_DURATION;
                        total += (long) (avgThreads * tg.rampUpDuration * requestsPerSecondPerThread * enabledRequests);
                    }
                    case SPIKE -> {
                        // 尖刺模式：按各阶段时间加权估算
                        int totalTime = tg.spikeRampUpTime + tg.spikeHoldTime + tg.spikeRampDownTime;
                        int avgThreads = (tg.spikeMinThreads + tg.spikeMaxThreads) / 2;
                        double requestsPerSecondPerThread = 1.0 / AVG_REQUEST_DURATION;
                        total += (long) (avgThreads * totalTime * requestsPerSecondPerThread * enabledRequests);
                    }
                    case STAIRS -> {
                        // 阶梯模式：使用总持续时间和平均线程数估算
                        int avgThreads = (tg.stairsStartThreads + tg.stairsEndThreads) / 2;
                        double requestsPerSecondPerThread = 1.0 / AVG_REQUEST_DURATION;
                        total += (long) (avgThreads * tg.stairsDuration * requestsPerSecondPerThread * enabledRequests);
                    }
                }
            }
        }
        return total;
    }

    /**
     * 定期刷新报表（由定时器定期调用）
     * 优化：仅在用户当前查看报表Tab时才执行刷新，避免无效计算
     */
    private void refreshReport() {
        try {
            // 优化：仅在用户查看报表Tab时才刷新（tab索引: 0=趋势图, 1=报表, 2=结果树）
            if (resultTabbedPane.getSelectedIndex() != 1) {
                log.debug("当前未查看报表Tab，跳过刷新");
                return;
            }

            updateReportWithLatestData();
        } catch (Exception ex) {
            // 不要让 Timer 因异常中断
            log.warn("实时刷新报表失败: {}", ex.getMessage(), ex);
        }
    }

    /**
     * 使用最新数据更新报表
     * 统一的数据复制和报表更新方法，确保线程安全和数据一致性
     * 采用异步计算，避免阻塞 EDT 线程
     */
    private void updateReportWithLatestData() {
        // 异步复制和计算数据，避免阻塞 EDT 线程
        CompletableFuture.runAsync(() -> {
            List<RequestResult> resultsCopy;
            Map<String, List<Long>> apiCostMapCopy;
            Map<String, Integer> apiSuccessMapCopy;
            Map<String, Integer> apiFailMapCopy;

            // 使用statsLock统一保护所有统计数据的复制，确保数据一致性
            synchronized (statsLock) {
                resultsCopy = new ArrayList<>(allRequestResults);

                apiCostMapCopy = new HashMap<>();
                for (Map.Entry<String, List<Long>> entry : apiCostMap.entrySet()) {
                    apiCostMapCopy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }

                apiSuccessMapCopy = new HashMap<>(apiSuccessMap);
                apiFailMapCopy = new HashMap<>(apiFailMap);
            }

            // UI 更新必须在 EDT 线程执行
            SwingUtilities.invokeLater(() -> performanceReportPanel.updateReport(
                    apiCostMapCopy,
                    apiSuccessMapCopy,
                    apiFailMapCopy,
                    resultsCopy
            ));
        }).exceptionally(ex -> {
            log.error("报表更新失败", ex);
            return null;
        });
    }

    /**
     * 同步刷新报表（用于停止/完成时）
     * 确保报表立即更新，不使用异步，避免时序问题
     */
    private void updateReportWithLatestDataSync() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updateReportWithLatestDataSync);
            return;
        }

        List<RequestResult> resultsCopy;
        Map<String, List<Long>> apiCostMapCopy;
        Map<String, Integer> apiSuccessMapCopy;
        Map<String, Integer> apiFailMapCopy;

        // 使用statsLock统一保护所有统计数据的复制，确保数据一致性
        synchronized (statsLock) {
            resultsCopy = new ArrayList<>(allRequestResults);

            apiCostMapCopy = new HashMap<>();
            for (Map.Entry<String, List<Long>> entry : apiCostMap.entrySet()) {
                apiCostMapCopy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }

            apiSuccessMapCopy = new HashMap<>(apiSuccessMap);
            apiFailMapCopy = new HashMap<>(apiFailMap);
        }

        // 直接在 EDT 线程中更新（因为已经在 EDT 中了）
        performanceReportPanel.updateReport(
                apiCostMapCopy,
                apiSuccessMapCopy,
                apiFailMapCopy,
                resultsCopy
        );
    }

    private void sampleTrendData() {
        int users = activeThreads.get();
        long now = System.currentTimeMillis();
        Second second = new Second(new Date(now));
        long samplingIntervalMs = timerManager.getSamplingIntervalMs();
        long windowStart = now - samplingIntervalMs;

        // 异步计算统计数据，避免阻塞 EDT 线程
        CompletableFuture.runAsync(() -> {
            int totalReq = 0;
            int errorReq = 0;
            long totalRespTime = 0;
            long actualMinTime = Long.MAX_VALUE;
            long actualMaxTime = 0;

            // 使用statsLock保护，确保数据一致性
            synchronized (statsLock) {
                // 遍历所有结果（不能假设顺序，因为多线程并发添加）
                for (RequestResult result : allRequestResults) {
                    // 只统计在时间窗口内的请求
                    if (result.endTime >= windowStart && result.endTime <= now) {
                        totalReq++;
                        if (!result.success) errorReq++;
                        totalRespTime += result.getResponseTime();
                        actualMinTime = Math.min(actualMinTime, result.endTime);
                        actualMaxTime = Math.max(actualMaxTime, result.endTime);
                    }
                }
            }

            double avgRespTime = totalReq > 0 ?
                    BigDecimal.valueOf((double) totalRespTime / totalReq)
                            .setScale(2, RoundingMode.HALF_UP)
                            .doubleValue()
                    : 0;

            // 修复QPS计算：使用实际时间跨度，而不是采样间隔
            double qps = 0;
            if (totalReq > 0 && actualMaxTime > actualMinTime) {
                long actualSpanMs = actualMaxTime - actualMinTime;
                qps = totalReq * 1000.0 / actualSpanMs;
            } else if (totalReq > 0) {
                // 如果只有一个请求，使用采样间隔（秒）作为分母
                qps = totalReq / (samplingIntervalMs / 1000.0);
            }

            double errorPercent = totalReq > 0 ? (double) errorReq / totalReq * 100 : 0;

            // 最终变量，供 lambda 使用
            final double finalAvgRespTime = avgRespTime;
            final double finalQps = qps;
            final double finalErrorPercent = errorPercent;
            final int finalTotalReq = totalReq;

            // UI 更新必须在 EDT 线程执行
            SwingUtilities.invokeLater(() -> {
                log.debug("采样数据 {} - 用户数: {}, 平均响应时间: {} ms, QPS: {}, 错误率: {}%, 样本数: {}",
                        second, users, finalAvgRespTime, finalQps, finalErrorPercent, finalTotalReq);
                performanceTrendPanel.addOrUpdate(second, users, finalAvgRespTime, finalQps, finalErrorPercent);
            });
        }).exceptionally(ex -> {
            log.error("趋势图采样失败", ex);
            return null;
        });
    }

    /**
     * 同步采样趋势图数据（用于停止/完成时）
     * 确保趋势图立即更新，不使用异步，避免时序问题
     */
    private void sampleTrendDataSync() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::sampleTrendDataSync);
            return;
        }

        int users = activeThreads.get();
        long now = System.currentTimeMillis();
        Second second = new Second(new Date(now));
        long samplingIntervalMs = timerManager.getSamplingIntervalMs();
        long windowStart = now - samplingIntervalMs;

        int totalReq = 0;
        int errorReq = 0;
        long totalRespTime = 0;
        long actualMinTime = Long.MAX_VALUE;
        long actualMaxTime = 0;

        // 使用statsLock保护，确保数据一致性
        synchronized (statsLock) {
            // 遍历所有结果（不能假设顺序，因为多线程并发添加）
            for (RequestResult result : allRequestResults) {
                // 只统计在时间窗口内的请求
                if (result.endTime >= windowStart && result.endTime <= now) {
                    totalReq++;
                    if (!result.success) errorReq++;
                    totalRespTime += result.getResponseTime();
                    actualMinTime = Math.min(actualMinTime, result.endTime);
                    actualMaxTime = Math.max(actualMaxTime, result.endTime);
                }
            }
        }

        double avgRespTime = totalReq > 0 ?
                BigDecimal.valueOf((double) totalRespTime / totalReq)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue()
                : 0;

        // 修复QPS计算：使用实际时间跨度，而不是采样间隔
        double qps = 0;
        if (totalReq > 0 && actualMaxTime > actualMinTime) {
            long actualSpanMs = actualMaxTime - actualMinTime;
            qps = totalReq * 1000.0 / actualSpanMs;
        } else if (totalReq > 0) {
            // 如果只有一个请求，使用采样间隔（秒）作为分母
            qps = totalReq / (samplingIntervalMs / 1000.0);
        }

        double errorPercent = totalReq > 0 ? (double) errorReq / totalReq * 100 : 0;

        // 直接在 EDT 线程中更新（因为已经在 EDT 中了）
        log.debug("同步采样数据 {} - 用户数: {}, 平均响应时间: {} ms, QPS: {}, 错误率: {}%, 样本数: {}",
                second, users, avgRespTime, qps, errorPercent, totalReq);
        performanceTrendPanel.addOrUpdate(second, users, avgRespTime, qps, errorPercent);
    }

    // 带进度的执行
    private void runJMeterTreeWithProgress(DefaultMutableTreeNode rootNode, JLabel progressLabel, int totalThreads) {
        if (!running) return;
        Object userObj = rootNode.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode) {
            if (Objects.requireNonNull(jtNode.type) == NodeType.ROOT) {
                List<Thread> tgThreads = new ArrayList<>();
                for (int i = 0; i < rootNode.getChildCount(); i++) {
                    DefaultMutableTreeNode tgNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
                    Object tgUserObj = tgNode.getUserObject();
                    // 跳过已停用的线程组
                    if (tgUserObj instanceof JMeterTreeNode tgJtNode && !tgJtNode.enabled) {
                        continue;
                    }
                    Thread t = new Thread(() -> runJMeterTreeWithProgress(tgNode, progressLabel, totalThreads));
                    tgThreads.add(t);
                    t.start();
                }
                for (Thread t : tgThreads) {
                    try {
                        t.join();
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else if (Objects.requireNonNull(jtNode.type) == NodeType.THREAD_GROUP) {
                ThreadGroupData tg = jtNode.threadGroupData != null ? jtNode.threadGroupData : new ThreadGroupData();

                // 根据线程模式选择对应的执行策略
                switch (tg.threadMode) {
                    case FIXED -> runFixedThreads(rootNode, tg, progressLabel, totalThreads);
                    case RAMP_UP -> runRampUpThreads(rootNode, tg, progressLabel, totalThreads);
                    case SPIKE -> runSpikeThreads(rootNode, tg, progressLabel, totalThreads);
                    case STAIRS -> runStairsThreads(rootNode, tg, progressLabel, totalThreads);
                }
            } else {
                log.warn("不支持的节点类型: {}", jtNode.type);
            }
        }
    }

    // 固定线程模式执行
    private void runFixedThreads(DefaultMutableTreeNode groupNode, ThreadGroupData tg, JLabel progressLabel, int totalThreads) {
        int numThreads = tg.numThreads;
        int loops = tg.loops;
        boolean useTime = tg.useTime;
        int durationSeconds = tg.duration;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long threadGroupStartTime = System.currentTimeMillis();
        long endTime = useTime ? (threadGroupStartTime + (durationSeconds * 1000L)) : Long.MAX_VALUE;

        for (int i = 0; i < numThreads; i++) {
            if (!running) {
                executor.shutdownNow();
                return;
            }
            executor.submit(() -> {
                activeThreads.incrementAndGet();
                SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                try {
                    // 按时间执行或按循环次数执行
                    if (useTime) {
                        // 按时间执行
                        while (System.currentTimeMillis() < endTime && running) {
                            runTaskIteration(groupNode);
                        }
                    } else {
                        // 按循环次数执行
                        runTask(groupNode, loops);
                    }
                } finally {
                    activeThreads.decrementAndGet();
                    SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                }
            });
        }
        executor.shutdown();
        try {
            // 等待所有线程完成，或者超时
            boolean terminated;
            int waitTimeSeconds;
            if (useTime) {
                // 对于定时测试，额外等待时间 5 秒
                waitTimeSeconds = durationSeconds + 5;
                terminated = executor.awaitTermination(waitTimeSeconds, TimeUnit.SECONDS);
            } else {
                // 对于循环次数测试，最多等待 1 小时
                terminated = executor.awaitTermination(1, TimeUnit.HOURS);
            }

            // 如果线程池未能正常终止（例如用户点击了停止按钮），则强制关闭
            if (!terminated || !running) {
                log.warn("线程池未能在预期时间内完成，强制关闭剩余线程");

                // 取消所有正在执行的 HTTP 请求
                cancelAllHttpCalls();

                // 强制关闭线程池
                List<Runnable> pendingTasks = executor.shutdownNow();
                log.debug("已取消 {} 个待执行任务", pendingTasks.size());

                // 再等待 3 秒确保强制关闭完成（缩短等待时间）
                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    log.warn("部分线程在强制关闭后仍未终止，这是正常的，线程会在网络操作完成后自动退出");
                } else {
                    log.info("所有线程已成功终止");
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executor.shutdownNow(); // 中断时也强制关闭
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_EXECUTION_INTERRUPTED, exception.getMessage()), I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            log.error(exception.getMessage(), exception);
        }
    }

    // 递增线程模式执行
    private void runRampUpThreads(DefaultMutableTreeNode groupNode, ThreadGroupData tg, JLabel progressLabel, int totalThreads) {
        int startThreads = tg.rampUpStartThreads;
        int endThreads = tg.rampUpEndThreads;
        int rampUpTime = tg.rampUpTime;
        int totalDuration = tg.rampUpDuration;  // 使用总持续时间参数

        // 计算每秒增加的线程数
        double threadsPerSecond = (double) (endThreads - startThreads) / rampUpTime;

        // 创建调度线程池
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ExecutorService executor = Executors.newCachedThreadPool();

        // 已启动的线程数
        AtomicInteger startedThreads = new AtomicInteger(0);

        // 每秒检查并启动新线程
        scheduler.scheduleAtFixedRate(() -> {
            if (!running) {
                scheduler.shutdownNow();
                executor.shutdownNow();
                return;
            }

            int currentSecond = (int) (System.currentTimeMillis() - startTime) / 1000;
            if (currentSecond > totalDuration) {
                scheduler.shutdown(); // 达到总时间，停止调度
                return;
            }

            // 在斜坡上升期间逐步增加线程
            if (currentSecond <= rampUpTime) {
                // 计算当前应有的线程数
                int targetThreads = startThreads + (int) (threadsPerSecond * currentSecond);
                targetThreads = Math.min(targetThreads, endThreads); // 不超过最大线程数

                // 启动新线程
                while (startedThreads.get() < targetThreads && running) {
                    executor.submit(() -> {
                        startedThreads.incrementAndGet();
                        activeThreads.incrementAndGet();
                        SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                        try {
                            // 循环执行直到结束
                            while (running && System.currentTimeMillis() - startTime < totalDuration * 1000L) {
                                runTaskIteration(groupNode);
                            }
                        } finally {
                            activeThreads.decrementAndGet();
                            SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                        }
                    });
                }
            }
        }, 0, 1, TimeUnit.SECONDS);

        try {
            // 等待执行完成
            boolean schedulerTerminated = scheduler.awaitTermination(totalDuration + 10L, TimeUnit.SECONDS);
            if (!schedulerTerminated || !running) {
                log.warn("递增模式调度器未能正常终止，强制关闭");
                scheduler.shutdownNow();
            }

            executor.shutdown();
            boolean executorTerminated = executor.awaitTermination(10, TimeUnit.SECONDS);
            if (!executorTerminated || !running) {
                log.warn("递增模式执行器未能正常终止，强制关闭");
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("递增模式部分线程在强制关闭后仍未终止");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
            executor.shutdownNow();
            log.error("递增线程执行中断", e);
        }
    }


    // 尖刺模式执行
    private void runSpikeThreads(DefaultMutableTreeNode groupNode, ThreadGroupData tg, JLabel progressLabel, int totalThreads) {
        int minThreads = tg.spikeMinThreads;
        int maxThreads = tg.spikeMaxThreads;
        int rampUpTime = tg.spikeRampUpTime;
        int holdTime = tg.spikeHoldTime;
        int rampDownTime = tg.spikeRampDownTime;
        int totalTime = tg.spikeDuration;  // 使用ThreadGroupData中定义的总持续时间

        // 创建线程池
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger startedThreads = new AtomicInteger(minThreads);

        // 使用ConcurrentHashMap跟踪线程及其预期结束时间
        ConcurrentHashMap<Thread, Long> threadEndTimes = new ConcurrentHashMap<>();

        // 计算各阶段所占总时间的比例
        int phaseSum = rampUpTime + holdTime + rampDownTime;
        int adjustedRampUpTime = totalTime * rampUpTime / phaseSum;
        int adjustedHoldTime = totalTime * holdTime / phaseSum;
        int adjustedRampDownTime = totalTime - adjustedRampUpTime - adjustedHoldTime;

        // 初始阶段: 启动最小线程数
        for (int i = 0; i < minThreads; i++) {
            if (!running) {
                return;
            }
            Thread thread = new Thread(() -> {
                activeThreads.incrementAndGet();
                SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                try {
                    // 持续运行直到测试结束或线程被标记为应该结束
                    Thread currentThread = Thread.currentThread();
                    while (running && System.currentTimeMillis() - startTime < totalTime * 1000L
                            && (System.currentTimeMillis() < threadEndTimes.getOrDefault(currentThread, Long.MAX_VALUE))) {
                        runTaskIteration(groupNode);
                    }
                } finally {
                    activeThreads.decrementAndGet();
                    SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                    // 从跟踪Map中移除此线程
                    threadEndTimes.remove(Thread.currentThread());
                }
            });
            // 将线程添加到跟踪Map
            threadEndTimes.put(thread, Long.MAX_VALUE); // 初始无限期运行
            thread.start();
        }

        // 阶段性调度：上升、保持、下降
        scheduler.scheduleAtFixedRate(() -> {
            if (!running) {
                scheduler.shutdownNow();
                return;
            }

            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            if (elapsedSeconds >= totalTime) {
                scheduler.shutdown();
                return;
            }

            long now = System.currentTimeMillis();
            int targetThreads;

            // 上升阶段
            if (elapsedSeconds < adjustedRampUpTime) {
                double progress = (double) elapsedSeconds / adjustedRampUpTime;
                targetThreads = minThreads + (int) (progress * (maxThreads - minThreads));
                // 增加线程
                adjustSpikeThreadCount(groupNode, startedThreads, targetThreads, totalTime, progressLabel, totalThreads, threadEndTimes);
            }
            // 保持阶段
            else if (elapsedSeconds < adjustedRampUpTime + adjustedHoldTime) {
                targetThreads = maxThreads;
                // 保持线程数
                adjustSpikeThreadCount(groupNode, startedThreads, targetThreads, totalTime, progressLabel, totalThreads, threadEndTimes);
            }
            // 下降阶段
            else {
                double progress = (double) (elapsedSeconds - adjustedRampUpTime - adjustedHoldTime) / adjustedRampDownTime;
                targetThreads = maxThreads - (int) (progress * (maxThreads - minThreads));
                targetThreads = Math.max(targetThreads, minThreads); // 不低于最小线程数

                // 在下降阶段，通过设置线程结束时间来减少活跃线程数
                int threadsToRemove = startedThreads.get() - targetThreads;
                if (threadsToRemove > 0) {
                    // 找出可以终止的线程
                    threadEndTimes.keySet().stream()
                            .filter(t -> t.isAlive() && threadEndTimes.get(t) == Long.MAX_VALUE)
                            .limit(threadsToRemove)
                            .forEach(t -> threadEndTimes.put(t, now + 500)); // 设置一个短暂的结束时间
                }

                // 仍然需要增加线程的情况
                adjustSpikeThreadCount(groupNode, startedThreads, targetThreads, totalTime, progressLabel, totalThreads, threadEndTimes);
            }

            // 更新UI显示实际活跃线程数而不是理论目标数
            SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));

        }, 1, 1, TimeUnit.SECONDS);

        try {
            // 等待执行完成
            boolean schedulerTerminated = scheduler.awaitTermination(totalTime + 10L, TimeUnit.SECONDS);
            if (!schedulerTerminated || !running) {
                log.warn("尖刺模式调度器未能正常终止，强制关闭");
                scheduler.shutdownNow();
            }

            // 确保等待所有threadEndTimes中的线程完成
            for (Thread t : threadEndTimes.keySet()) {
                try {
                    if (t.isAlive()) {
                        t.join(5000); // 减少超时时间，避免等待过久
                        if (t.isAlive() && !running) {
                            // 如果用户停止测试且线程仍在运行，则中断它
                            t.interrupt();
                        }
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("等待线程完成时中断", ie);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
            // 中断所有活跃线程
            for (Thread t : threadEndTimes.keySet()) {
                if (t.isAlive()) {
                    t.interrupt();
                }
            }
            log.error("尖刺模式执行中断", e);
        }
    }

    // 阶梯模式执行
    private void runStairsThreads(DefaultMutableTreeNode groupNode, ThreadGroupData tg, JLabel progressLabel, int totalThreads) {
        int startThreads = tg.stairsStartThreads;
        int endThreads = tg.stairsEndThreads;
        int step = tg.stairsStep;
        int holdTime = tg.stairsHoldTime;
        int totalTime = tg.stairsDuration;

        // 创建线程池
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger startedThreads = new AtomicInteger(startThreads);

        // 使用ConcurrentHashMap跟踪线程及其预期结束时间
        ConcurrentHashMap<Thread, Long> threadEndTimes = new ConcurrentHashMap<>();

        // 计算阶梯数量
        int totalSteps = Math.max(1, (endThreads - startThreads) / step);

        // 记录当前阶梯和上次阶梯变化的时间
        AtomicInteger currentStair = new AtomicInteger(0);
        AtomicLong lastStairChangeTime = new AtomicLong(System.currentTimeMillis());

        // 初始阶段: 启动起始线程数
        for (int i = 0; i < startThreads; i++) {
            if (!running) {
                return;
            }
            Thread thread = new Thread(() -> {
                activeThreads.incrementAndGet();
                SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                try {
                    // 持续运行直到测试结束或线程被标记为应该结束
                    Thread currentThread = Thread.currentThread();
                    while (running && System.currentTimeMillis() - startTime < totalTime * 1000L
                            && (System.currentTimeMillis() < threadEndTimes.getOrDefault(currentThread, Long.MAX_VALUE))) {
                        runTaskIteration(groupNode);
                    }
                } finally {
                    activeThreads.decrementAndGet();
                    SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                    // 从跟踪Map中移除此线程
                    threadEndTimes.remove(Thread.currentThread());
                }
            });
            // 将线程添加到跟踪Map
            threadEndTimes.put(thread, Long.MAX_VALUE); // 初始无限期运行
            thread.start();
        }

        // 阶梯式调度
        scheduler.scheduleAtFixedRate(() -> {
            if (!running) {
                scheduler.shutdownNow();
                return;
            }

            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            if (elapsedSeconds >= totalTime) {
                scheduler.shutdown();
                return;
            }

            long now = System.currentTimeMillis();

            // 计算当前应该处于哪个阶梯
            // 检查是否需要进入下一个阶梯（考虑保持时间）
            long timeSinceLastChange = now - lastStairChangeTime.get();
            int stair = currentStair.get();

            // 如果已经过了当前阶梯的保持时间，并且还没有达到最大阶梯数，则进入下一个阶梯
            if (timeSinceLastChange >= holdTime * 1000L && stair < totalSteps) {
                stair = currentStair.incrementAndGet();
                lastStairChangeTime.set(now);
            }

            // 计算当前阶梯应有的线程数
            int targetThreads = startThreads;
            if (stair > 0 && stair <= totalSteps) {
                targetThreads = startThreads + stair * step;
                targetThreads = Math.min(targetThreads, endThreads); // 不超过最大线程数
            }

            // 调整线程数
            adjustStairsThreadCount(groupNode, startedThreads, targetThreads, totalTime, progressLabel, totalThreads, threadEndTimes);

            // 更新UI显示实际活跃线程数
            SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));

        }, 1, 1, TimeUnit.SECONDS);

        try {
            // 等待执行完成
            boolean schedulerTerminated = scheduler.awaitTermination(totalTime + 10L, TimeUnit.SECONDS);
            if (!schedulerTerminated || !running) {
                log.warn("阶梯模式调度器未能正常终止，强制关闭");
                scheduler.shutdownNow();
            }

            // 确保等待所有threadEndTimes中的线程完成
            for (Thread t : threadEndTimes.keySet()) {
                try {
                    if (t.isAlive()) {
                        t.join(5000); // 减少超时时间，避免等待过久
                        if (t.isAlive() && !running) {
                            // 如果用户停止测试且线程仍在运行，则中断它
                            t.interrupt();
                        }
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("等待线程完成时中断", ie);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
            // 中断所有活跃线程
            for (Thread t : threadEndTimes.keySet()) {
                if (t.isAlive()) {
                    t.interrupt();
                }
            }
            log.error("阶梯模式执行中断", e);
        }
    }

    // 专用于尖刺模式的线程数调整方法
    private void adjustSpikeThreadCount(DefaultMutableTreeNode groupNode,
                                        AtomicInteger startedThreads, int targetThreads,
                                        int totalTime, JLabel progressLabel, int totalThreads,
                                        ConcurrentHashMap<Thread, Long> threadEndTimes) {
        int current = startedThreads.get();

        // 需要增加线程
        if (current < targetThreads) {
            int threadsToAdd = targetThreads - current;
            for (int i = 0; i < threadsToAdd; i++) {
                if (!running) return;

                Thread thread = new Thread(() -> {
                    startedThreads.incrementAndGet();
                    activeThreads.incrementAndGet();
                    SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                    try {
                        // 持续运行直到测试结束或线程被标记为应该结束
                        Thread currentThread = Thread.currentThread();
                        while (running && System.currentTimeMillis() - startTime < totalTime * 1000L
                                && (System.currentTimeMillis() < threadEndTimes.getOrDefault(currentThread, Long.MAX_VALUE))) {
                            runTaskIteration(groupNode);
                        }
                    } finally {
                        activeThreads.decrementAndGet();
                        SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                        // 从跟踪Map中移除此线程
                        threadEndTimes.remove(Thread.currentThread());
                    }
                });
                // 将线程添加到跟踪Map
                threadEndTimes.put(thread, Long.MAX_VALUE); // 初始无限期运行
                thread.start();
            }
        }
        // 需要减少线程 - 缓慢减少，而不是一次性全部标记为结束
        else if (current > targetThreads) {
            int threadsToRemove = current - targetThreads;
            long now = System.currentTimeMillis();

            // 找出所有可以终止的线程
            List<Thread> availableThreads = threadEndTimes.keySet().stream()
                    .filter(t -> t.isAlive() && threadEndTimes.get(t) == Long.MAX_VALUE)
                    .limit(threadsToRemove)
                    .toList();

            // 如果有可终止的线程，则设置它们分散结束
            if (!availableThreads.isEmpty()) {
                // 获取当前所处阶段信息 - 从runSpikeThreads方法推算
                ThreadGroupData tg = null;
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
                if (node != null) {
                    Object userObj = node.getUserObject();
                    if (userObj instanceof JMeterTreeNode jtNode && jtNode.threadGroupData != null) {
                        tg = jtNode.threadGroupData;
                    }
                }

                // 计算下降阶段的总时间
                int rampDownTime = (tg != null) ? tg.spikeRampDownTime : 10; // 默认10秒
                int totalSpikeTime = (tg != null) ? tg.spikeDuration : 60;   // 默认60秒
                int rampUpTime = (tg != null) ? tg.spikeRampUpTime : 10;     // 默认10秒
                int holdTime = (tg != null) ? tg.spikeHoldTime : 20;         // 默认20秒

                // 计算实际的下降时间（按比例）
                int phaseSum = rampUpTime + holdTime + rampDownTime;
                int adjustedRampDownTime = totalSpikeTime * rampDownTime / phaseSum;
                adjustedRampDownTime = Math.max(adjustedRampDownTime, 1); // 至少1秒

                // 计算从现在到下降结束还剩多少时间
                long elapsedSeconds = (now - startTime) / 1000;
                long rampDownStartTime = (long) (rampUpTime + holdTime) * totalSpikeTime / phaseSum;
                long timeLeftInRampDown = Math.max(1, adjustedRampDownTime - (elapsedSeconds - rampDownStartTime));

                // 计算每个线程应该在多久后结束，使其分散在剩余的下降时间内
                for (int i = 0; i < availableThreads.size(); i++) {
                    Thread t = availableThreads.get(i);
                    // 为线程设置不同的结束时间，均匀分布在剩余下降时间内
                    long delayMs = now + (i + 1) * timeLeftInRampDown * 1000 / (availableThreads.size() + 1);
                    threadEndTimes.put(t, delayMs);
                }
            }
        }
    }

    // 专用于阶梯模式的线程数调整方法
    private void adjustStairsThreadCount(DefaultMutableTreeNode groupNode,
                                         AtomicInteger startedThreads, int targetThreads,
                                         int totalTime, JLabel progressLabel, int totalThreads,
                                         ConcurrentHashMap<Thread, Long> threadEndTimes) {
        int current = startedThreads.get();

        // 需要增加线程 阶梯模式下，不需要减少线程，只增加
        if (current < targetThreads) {
            int threadsToAdd = targetThreads - current;
            for (int i = 0; i < threadsToAdd; i++) {
                if (!running) return;

                Thread thread = new Thread(() -> {
                    startedThreads.incrementAndGet();
                    activeThreads.incrementAndGet();
                    SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                    try {
                        // 持续运行直到测试结束或线程被标记为应该结束
                        Thread currentThread = Thread.currentThread();
                        while (running && System.currentTimeMillis() - startTime < totalTime * 1000L
                                && (System.currentTimeMillis() < threadEndTimes.getOrDefault(currentThread, Long.MAX_VALUE))) {
                            runTaskIteration(groupNode);
                        }
                    } finally {
                        activeThreads.decrementAndGet();
                        SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                        // 从跟踪Map中移除此线程
                        threadEndTimes.remove(Thread.currentThread());
                    }
                });
                // 将线程添加到跟踪Map
                threadEndTimes.put(thread, Long.MAX_VALUE); // 初始无限期运行
                thread.start();
            }
        }
    }

    // 执行单次请求
    private void runTaskIteration(DefaultMutableTreeNode groupNode) {
        for (int i = 0; i < groupNode.getChildCount() && running; i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) groupNode.getChildAt(i);
            Object userObj = child.getUserObject();
            // 跳过已停用的请求节点
            if (userObj instanceof JMeterTreeNode jtNode && !jtNode.enabled) {
                continue;
            }
            executeRequestNode(userObj, child);
        }
    }

    // 执行指定次数的请求
    private void runTask(DefaultMutableTreeNode groupNode, int loops) {
        for (int l = 0; l < loops && running; l++) {
            runTaskIteration(groupNode);
        }
    }

    /**
     * 检查异常是否是被取消或中断的请求
     * 包括：InterruptedIOException、IOException: Canceled 等
     */
    private boolean isCancelledOrInterrupted(Exception ex) {
        // 1. InterruptedIOException（线程中断）
        if (ex instanceof java.io.InterruptedIOException) {
            return true;
        }

        // 2. IOException with "Canceled" message（OkHttp 取消请求）
        if (ex instanceof java.io.IOException) {
            String message = ex.getMessage();
            if (message != null && message.contains("Canceled")) {
                return true;
            }
        }

        // 3. InterruptedException
        if (ex instanceof InterruptedException) {
            return true;
        }

        // 4. 检查异常链中是否包含上述异常
        Throwable cause = ex.getCause();
        if (cause instanceof Exception exception) {
            return isCancelledOrInterrupted(exception);
        }

        return false;
    }

    // 执行单个请求节点
    private void executeRequestNode(Object userObj, DefaultMutableTreeNode child) {
        // 如果测试已停止，立即返回，不执行请求
        if (!running) {
            return;
        }

        if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST && jtNode.httpRequestItem != null) {
            // 使用 ID 而不是 Name，避免重名问题
            String apiId = jtNode.httpRequestItem.getId();
            String apiName = jtNode.httpRequestItem.getName();

            // 注册API元数据（集中管理，避免重复存储）
            ApiMetadata.register(apiId, apiName);

            boolean success = true;
            PreparedRequest req;
            HttpResponse resp = null;
            String errorMsg = "";
            List<TestResult> testResults = new ArrayList<>();

            // 清理上次的临时变量
            EnvironmentService.clearTemporaryVariables();

            // ====== CSV变量注入 ======
            Map<String, String> csvRow = null;
            if (csvDataPanel != null && csvDataPanel.hasData()) {
                int rowCount = csvDataPanel.getRowCount();
                if (rowCount > 0) {
                    int rowIdx = csvRowIndex.getAndIncrement() % rowCount;
                    csvRow = csvDataPanel.getRowData(rowIdx);
                }
            }
            // ====== 前置脚本 ======
            req = PreparedRequestBuilder.build(jtNode.httpRequestItem);

            // 创建脚本执行流水线
            ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                    .request(req)
                    .preScript(jtNode.httpRequestItem.getPrescript())
                    .postScript(jtNode.httpRequestItem.getPostscript())
                    .build();

            // 注入 CSV 变量
            if (csvRow != null) {
                pipeline.addCsvDataBindings(csvRow);
            }

            // 执行前置脚本
            ScriptExecutionResult preResult = pipeline.executePreScript();
            boolean preOk = preResult.isSuccess();
            if (!preOk) {
                log.error("前置脚本: {}", preResult.getErrorMessage());
                errorMsg = I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_PRE_SCRIPT_FAILED, preResult.getErrorMessage());
                success = false;
            }

            // 前置脚本执行完后再次检查是否已停止
            if (!running) {
                return;
            }

            // 前置脚本执行完成后，进行变量替换
            if (preOk) {
                PreparedRequestBuilder.replaceVariablesAfterPreScript(req);
            }

            long requestStartTime = System.currentTimeMillis();
            long costMs = 0;
            boolean interrupted = false; // 标记是否被中断

            if (preOk && running) {  // 执行HTTP请求前再次检查running状态
                try {
                    req.logEvent = !efficientMode; // 记录事件日志
                    resp = HttpSingleRequestExecutor.executeHttp(req);
                } catch (Exception ex) {
                    // 检查是否是被取消/中断的请求
                    // 注意：cancelAllHttpCalls() 只在停止时调用，所以 Canceled 异常就是停止导致的
                    boolean isCancelled = isCancelledOrInterrupted(ex);

                    if (isCancelled) {
                        // 被取消/中断的请求，不算作失败
                        log.debug("请求被取消/中断（压测已停止）: {}", ex.getMessage());
                        interrupted = true; // 标记为中断
                    } else {
                        // 真正的错误
                        log.error("请求执行失败: {}", ex.getMessage(), ex);
                        errorMsg = I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REQUEST_FAILED, ex.getMessage());
                        success = false;
                    }
                } finally {
                    costMs = System.currentTimeMillis() - requestStartTime;
                }
                // 断言处理（JMeter树断言）
                for (int j = 0; j < child.getChildCount() && resp != null && running; j++) {
                    DefaultMutableTreeNode sub = (DefaultMutableTreeNode) child.getChildAt(j);
                    Object subObj = sub.getUserObject();
                    if (subObj instanceof JMeterTreeNode subNode && subNode.type == NodeType.ASSERTION && subNode.assertionData != null) {
                        // 跳过已停用的断言
                        if (!subNode.enabled) {
                            continue;
                        }
                        AssertionData assertion = subNode.assertionData;
                        String type = assertion.type;
                        boolean pass = false;
                        if ("Response Code".equals(type)) {
                            String op = assertion.operator;
                            String valStr = assertion.value;
                            try {
                                int expect = Integer.parseInt(valStr);
                                if ("=".equals(op)) pass = (resp.code == expect);
                                else if (">".equals(op)) pass = (resp.code > expect);
                                else if ("<".equals(op)) pass = (resp.code < expect);
                            } catch (Exception ignored) {
                                log.warn("断言响应码格式错误: {}", valStr);
                            }
                        } else if ("Contains".equals(type)) {
                            pass = resp.body.contains(assertion.content);
                        } else if ("JSONPath".equals(type)) {
                            String jsonPath = assertion.value;
                            String expect = assertion.content;
                            String actual = JsonPathUtil.extractJsonPath(resp.body, jsonPath);
                            pass = Objects.equals(actual, expect);
                        }
                        if (!pass) {
                            success = false;
                            errorMsg = I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_ASSERTION_FAILED, type, assertion.content);
                        }
                        testResults.add(new TestResult(type, pass, pass ? null : "断言失败"));
                    }
                }
                // ====== 后置脚本 ======
                if (resp != null && running) {  // 执行后置脚本前检查是否已停止
                    // 执行后置脚本（自动处理响应绑定和测试结果收集）
                    ScriptExecutionResult postResult = pipeline.executePostScript(resp);
                    if (postResult.hasTestResults()) {
                        testResults.addAll(postResult.getTestResults());
                    }
                    if (!postResult.isSuccess()) {
                        log.error("后置脚本执行失败: {}", postResult.getErrorMessage());
                        errorMsg = postResult.getErrorMessage();
                        success = false;
                    }
                }
            } else {
                // 前置脚本失败的情况，也需要记录costMs
                costMs = System.currentTimeMillis() - requestStartTime;
            }

            // ====== 统计请求结果（断言和后置脚本后，sleep前） ======
            long cost = resp == null ? costMs : resp.costMs;
            long endTime = requestStartTime + cost; // 如果响应时间有记录，则使用，否则使用计算的cost
            if (resp != null) {
                endTime = resp.endTime > 0 ? resp.endTime : requestStartTime + cost;
            }

            // 如果请求被中断（压测停止），跳过统计，不计入成功或失败
            if (!interrupted) {
                // 使用statsLock保护统计数据写入，确保与读取的一致性
                synchronized (statsLock) {
                    allRequestResults.add(new RequestResult(requestStartTime, endTime, success, apiId));

                    apiCostMap.computeIfAbsent(apiId, k -> Collections.synchronizedList(new ArrayList<>())).add(cost);
                    if (success) {
                        apiSuccessMap.merge(apiId, 1, Integer::sum);
                    } else {
                        apiFailMap.merge(apiId, 1, Integer::sum);
                    }
                }

                performanceResultTablePanel.addResult(
                        new ResultNodeInfo(apiName, errorMsg, req, resp, testResults),
                        efficientMode,
                        getJmeterSlowRequestThreshold()
                );
            } else {
                // 被中断的请求，记录日志但不计入统计
                log.debug("跳过被中断请求的统计: {}", jtNode.httpRequestItem.getName());
            }

            // ====== 定时器延迟（sleep） ======
            // 如果测试已停止，跳过定时器延迟
            if (!running) {
                return;
            }

            for (int j = 0; j < child.getChildCount() && running; j++) {
                DefaultMutableTreeNode sub = (DefaultMutableTreeNode) child.getChildAt(j);
                Object subObj = sub.getUserObject();
                if (subObj instanceof JMeterTreeNode subNode2 && subNode2.type == NodeType.TIMER && subNode2.timerData != null) {
                    // 跳过已停用的定时器
                    if (!subNode2.enabled) {
                        continue;
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(subNode2.timerData.delayMs);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    @Override
    protected void registerListeners() {
        // 节点选中切换属性区
        jmeterTree.addTreeSelectionListener(new TreeSelectionListener() {
            private DefaultMutableTreeNode lastNode = null;

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                // 检查是否多选
                TreePath[] selectedPaths = jmeterTree.getSelectionPaths();
                if (selectedPaths != null && selectedPaths.length > 1) {
                    // 多选模式：不切换属性面板，保持当前显示
                    return;
                }

                // 保存上一个节点的数据
                if (lastNode != null) {
                    Object userObj = lastNode.getUserObject();
                    if (userObj instanceof JMeterTreeNode jtNode) {
                        switch (jtNode.type) {
                            case THREAD_GROUP -> threadGroupPanel.saveThreadGroupData();
                            case REQUEST -> jtNode.httpRequestItem = requestEditSubPanel.getCurrentRequest();
                            case ASSERTION -> assertionPanel.saveAssertionData();
                            case TIMER -> timerPanel.saveTimerData();
                            default -> {
                                // 其他类型节点不需要保存数据
                            }
                        }
                    }
                }
                // 回填当前节点数据
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
                if (node == null) {
                    propertyCardLayout.show(propertyPanel, EMPTY);
                    currentRequestNode = null; // 清空当前请求节点引用
                    lastNode = null;
                    return;
                }
                Object userObj = node.getUserObject();
                if (!(userObj instanceof JMeterTreeNode jtNode)) {
                    propertyCardLayout.show(propertyPanel, EMPTY);
                    currentRequestNode = null; // 清空当前请求节点引用
                    lastNode = node;
                    return;
                }
                switch (jtNode.type) {
                    case THREAD_GROUP -> {
                        propertyCardLayout.show(propertyPanel, THREAD_GROUP);
                        threadGroupPanel.setThreadGroupData(jtNode);
                        currentRequestNode = null; // 清空当前请求节点引用
                    }
                    case REQUEST -> {
                        propertyCardLayout.show(propertyPanel, REQUEST);
                        currentRequestNode = node; // 记录当前请求节点
                        if (jtNode.httpRequestItem != null) {
                            requestEditSubPanel.initPanelData(jtNode.httpRequestItem);
                        }
                    }
                    case ASSERTION -> {
                        propertyCardLayout.show(propertyPanel, ASSERTION);
                        assertionPanel.setAssertionData(jtNode);
                        currentRequestNode = null; // 清空当前请求节点引用
                    }
                    case TIMER -> {
                        propertyCardLayout.show(propertyPanel, TIMER);
                        timerPanel.setTimerData(jtNode);
                        currentRequestNode = null; // 清空当前请求节点引用
                    }
                    default -> {
                        propertyCardLayout.show(propertyPanel, EMPTY);
                        currentRequestNode = null; // 清空当前请求节点引用
                    }
                }
                lastNode = node;
            }
        });

        // 右键菜单
        JPopupMenu treeMenu = new JPopupMenu();
        JMenuItem addThreadGroup = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_THREAD_GROUP));
        JMenuItem addRequest = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_REQUEST));
        JMenuItem addAssertion = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_ASSERTION));
        JMenuItem addTimer = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_TIMER));
        JMenuItem renameNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_RENAME));
        JMenuItem deleteNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_DELETE));
        JMenuItem enableNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ENABLE));
        JMenuItem disableNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_DISABLE));

        // 创建两个分割线，以便可以单独控制显示
        JSeparator separator1 = new JSeparator();
        JSeparator separator2 = new JSeparator();

        treeMenu.add(addThreadGroup);
        treeMenu.add(addRequest);
        treeMenu.add(addAssertion);
        treeMenu.add(addTimer);
        treeMenu.add(separator1);
        treeMenu.add(enableNode);
        treeMenu.add(disableNode);
        treeMenu.add(separator2);
        treeMenu.add(renameNode);
        treeMenu.add(deleteNode);

        // 添加用户组（仅根节点可添加）
        addThreadGroup.addActionListener(e -> {
            DefaultMutableTreeNode root1 = (DefaultMutableTreeNode) treeModel.getRoot();
            DefaultMutableTreeNode group = new DefaultMutableTreeNode(new JMeterTreeNode("Thread Group", NodeType.THREAD_GROUP));
            treeModel.insertNodeInto(group, root1, root1.getChildCount());
            jmeterTree.expandPath(new TreePath(root1.getPath()));
            saveConfig();
        });
        // 添加请求
        addRequest.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
            if (node == null) return;
            Object userObj = node.getUserObject();
            if (!(userObj instanceof JMeterTreeNode jtNode) || jtNode.type != NodeType.THREAD_GROUP) {
                JOptionPane.showMessageDialog(PerformancePanel.this, I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_SELECT_THREAD_GROUP), I18nUtil.getMessage(MessageKeys.GENERAL_INFO), JOptionPane.WARNING_MESSAGE);
                return;
            }
            // 多选请求弹窗
            RequestCollectionsService.showMultiSelectRequestDialog(selectedList -> {
                if (selectedList == null || selectedList.isEmpty()) return;

                // 过滤只保留HTTP类型的请求
                List<HttpRequestItem> httpOnlyList = selectedList.stream()
                        .filter(reqItem -> reqItem.getProtocol() != null && reqItem.getProtocol().isHttpProtocol())
                        .toList();

                if (httpOnlyList.isEmpty()) {
                    JOptionPane.showMessageDialog(PerformancePanel.this,
                            I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_ONLY_HTTP_SUPPORTED),
                            I18nUtil.getMessage(MessageKeys.GENERAL_INFO),
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                List<DefaultMutableTreeNode> newNodes = new ArrayList<>();
                for (HttpRequestItem reqItem : httpOnlyList) {
                    DefaultMutableTreeNode req = new DefaultMutableTreeNode(new JMeterTreeNode(reqItem.getName(), NodeType.REQUEST, reqItem));
                    treeModel.insertNodeInto(req, node, node.getChildCount());
                    newNodes.add(req);
                }
                jmeterTree.expandPath(new TreePath(node.getPath()));
                // 选中第一个新加的请求节点
                TreePath newPath = new TreePath(newNodes.get(0).getPath());
                jmeterTree.setSelectionPath(newPath);
                propertyCardLayout.show(propertyPanel, REQUEST);
                requestEditSubPanel.initPanelData(((JMeterTreeNode) newNodes.get(0).getUserObject()).httpRequestItem);
                saveConfig();
            });
        });
        // 添加断言
        addAssertion.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
            if (node == null) return;
            DefaultMutableTreeNode assertion = new DefaultMutableTreeNode(new JMeterTreeNode("Assertion", NodeType.ASSERTION));
            treeModel.insertNodeInto(assertion, node, node.getChildCount());
            jmeterTree.expandPath(new TreePath(node.getPath()));
            saveConfig();
        });
        // 添加定时器
        addTimer.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
            if (node == null) return;
            DefaultMutableTreeNode timer = new DefaultMutableTreeNode(new JMeterTreeNode("Timer", NodeType.TIMER));
            treeModel.insertNodeInto(timer, node, node.getChildCount());
            jmeterTree.expandPath(new TreePath(node.getPath()));
            saveConfig();
        });
        // 重命名
        renameNode.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
            if (node == null) return;
            Object userObj = node.getUserObject();
            if (!(userObj instanceof JMeterTreeNode jtNode)) return;
            if (jtNode.type == NodeType.ROOT) return;
            String oldName = jtNode.name;
            String newName = JOptionPane.showInputDialog(PerformancePanel.this,
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_RENAME_NODE), oldName);
            if (newName != null && !newName.trim().isEmpty()) {
                jtNode.name = newName.trim();
                // 同步更新 request 类型的 httpRequestItem name 字段
                if (jtNode.type == NodeType.REQUEST && jtNode.httpRequestItem != null) {
                    jtNode.httpRequestItem.setName(newName.trim());
                    requestEditSubPanel.initPanelData(jtNode.httpRequestItem);
                }
                treeModel.nodeChanged(node);
                saveConfig();
            }
        });
        // 删除（自动支持单个和批量操作）
        deleteNode.addActionListener(e -> {
            TreePath[] selectedPaths = jmeterTree.getSelectionPaths();
            if (selectedPaths == null || selectedPaths.length == 0) return;

            // 收集要删除的节点（过滤掉ROOT节点）
            List<DefaultMutableTreeNode> nodesToDelete = new ArrayList<>();
            for (TreePath path : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObj = node.getUserObject();
                if (userObj instanceof JMeterTreeNode jtNode && jtNode.type != NodeType.ROOT) {
                    nodesToDelete.add(node);
                }
            }

            if (nodesToDelete.isEmpty()) return;

            // 批量删除节点
            for (DefaultMutableTreeNode node : nodesToDelete) {
                // 如果删除的是当前选中的请求节点，清空引用
                if (node == currentRequestNode) {
                    currentRequestNode = null;
                }
                treeModel.removeNodeFromParent(node);
            }
            saveConfig();
        });
        // 启用（自动支持单个和批量操作）
        enableNode.addActionListener(e -> {
            TreePath[] selectedPaths = jmeterTree.getSelectionPaths();
            if (selectedPaths == null || selectedPaths.length == 0) return;
            for (TreePath path : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObj = node.getUserObject();
                if (userObj instanceof JMeterTreeNode jtNode && jtNode.type != NodeType.ROOT) {
                    jtNode.enabled = true;
                    treeModel.nodeChanged(node);
                }
            }
            saveConfig();
        });
        // 停用（自动支持单个和批量操作）
        disableNode.addActionListener(e -> {
            TreePath[] selectedPaths = jmeterTree.getSelectionPaths();
            if (selectedPaths == null || selectedPaths.length == 0) return;
            for (TreePath path : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObj = node.getUserObject();
                if (userObj instanceof JMeterTreeNode jtNode && jtNode.type != NodeType.ROOT) {
                    jtNode.enabled = false;
                    treeModel.nodeChanged(node);
                }
            }
            saveConfig();
        });

        // 右键弹出逻辑
        jmeterTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int row = jmeterTree.getClosestRowForLocation(e.getX(), e.getY());
                    if (row < 0) return;

                    // 如果右键点击的节点不在选中列表中，则只选中该节点
                    TreePath clickedPath = jmeterTree.getPathForLocation(e.getX(), e.getY());
                    if (clickedPath != null) {
                        TreePath[] selectedPaths = jmeterTree.getSelectionPaths();
                        boolean isInSelection = false;
                        if (selectedPaths != null) {
                            for (TreePath path : selectedPaths) {
                                if (path.equals(clickedPath)) {
                                    isInSelection = true;
                                    break;
                                }
                            }
                        }
                        if (!isInSelection) {
                            jmeterTree.setSelectionPath(clickedPath);
                        }
                    }

                    TreePath[] selectedPaths = jmeterTree.getSelectionPaths();
                    if (selectedPaths == null || selectedPaths.length == 0) return;

                    // 判断是单选还是多选
                    boolean isMultiSelection = selectedPaths.length > 1;

                    if (isMultiSelection) {
                        // 多选模式：只显示批量操作相关菜单
                        addThreadGroup.setVisible(false);
                        addRequest.setVisible(false);
                        addAssertion.setVisible(false);
                        addTimer.setVisible(false);
                        renameNode.setVisible(false);
                        deleteNode.setVisible(true); // 允许批量删除

                        // 检查选中节点的启用状态，智能显示启用/停用
                        boolean hasDisabled = false;
                        boolean hasEnabled = false;
                        for (TreePath path : selectedPaths) {
                            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                            Object userObj = node.getUserObject();
                            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type != NodeType.ROOT) {
                                if (jtNode.enabled) {
                                    hasEnabled = true;
                                } else {
                                    hasDisabled = true;
                                }
                            }
                        }
                        enableNode.setVisible(hasDisabled);
                        disableNode.setVisible(hasEnabled);
                        separator1.setVisible(true);
                        separator2.setVisible(true);
                    } else {
                        // 单选模式：显示常规菜单
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
                        Object userObj = node.getUserObject();
                        if (!(userObj instanceof JMeterTreeNode jtNode)) return;

                        if (jtNode.type == NodeType.ROOT) {
                            addThreadGroup.setVisible(true);
                            addRequest.setVisible(false);
                            addAssertion.setVisible(false);
                            addTimer.setVisible(false);
                            renameNode.setVisible(false);
                            deleteNode.setVisible(false);
                            enableNode.setVisible(false);
                            disableNode.setVisible(false);
                            separator1.setVisible(false);
                            separator2.setVisible(false);
                            treeMenu.show(jmeterTree, e.getX(), e.getY());
                            return;
                        }
                        addThreadGroup.setVisible(false);
                        addRequest.setVisible(jtNode.type == NodeType.THREAD_GROUP);
                        addAssertion.setVisible(jtNode.type == NodeType.REQUEST);
                        addTimer.setVisible(jtNode.type == NodeType.REQUEST);
                        renameNode.setVisible(true);
                        deleteNode.setVisible(true);
                        // 根据当前启用状态显示启用或停用菜单
                        enableNode.setVisible(!jtNode.enabled);
                        disableNode.setVisible(jtNode.enabled);
                        // 显示分割线
                        separator1.setVisible(true);
                        separator2.setVisible(true);
                    }
                    treeMenu.show(jmeterTree, e.getX(), e.getY());
                }
            }
        });

    }

    private void stopRun() {
        running = false;
        if (runThread != null && runThread.isAlive()) {
            runThread.interrupt();
        }

        // 立即取消所有正在执行的 OkHttp 请求
        // 这会中断所有网络 I/O，让线程快速退出
        cancelAllHttpCalls();

        runBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        refreshBtn.setEnabled(true); // 停止时重新启用刷新按钮

        // 停止所有定时器
        timerManager.stopAll();

        // 给正在执行的请求一点时间完成统计（避免数据丢失）
        // 然后执行最后一次趋势图采样和报表刷新
        CompletableFuture.runAsync(() -> {
            try {
                // 等待100ms，让被中断的请求有机会更新统计数据
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).thenRun(() -> SwingUtilities.invokeLater(() -> {
            // 强制刷新 ResultTable 的 pendingQueue（确保所有待处理的结果都显示）
            try {
                performanceResultTablePanel.flushPendingResults();
            } catch (Exception e) {
                log.warn("停止时刷新结果树失败", e);
            }

            try {
                // 使用同步版本采样，确保数据立即更新
                sampleTrendDataSync();
            } catch (Exception e) {
                log.warn("停止时最后一次趋势图采样失败", e);
            }

            try {
                // 使用同步版本刷新报表，确保数据立即更新
                updateReportWithLatestDataSync();
            } catch (Exception e) {
                log.warn("停止时最后一次报表刷新失败", e);
            }
        }));

        OkHttpClientManager.setDefaultConnectionPoolConfig();
    }

    /**
     * 取消所有正在执行的 HTTP 请求
     * 通过取消 OkHttpClient 的 Dispatcher 中的所有 Call 来实现快速停止
     */
    private void cancelAllHttpCalls() {
        OkHttpClientManager.cancelAllCalls();
    }

    private static int getJmeterMaxIdleConnections() {
        return SettingManager.getJmeterMaxIdleConnections();
    }

    private static long getJmeterKeepAliveSeconds() {
        return SettingManager.getJmeterKeepAliveSeconds();
    }

    private static int getJmeterMaxRequests() {
        return SettingManager.getJmeterMaxRequests();
    }

    private static int getJmeterMaxRequestsPerHost() {
        return SettingManager.getJmeterMaxRequestsPerHost();
    }

    private static int getJmeterSlowRequestThreshold() {
        return SettingManager.getJmeterSlowRequestThreshold();
    }

    /**
     * 保存当前配置
     */
    private void saveConfig() {
        try {
            // 保存所有属性面板数据到树节点
            saveAllPropertyPanelData();
            // 获取根节点
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
            // 异步保存配置
            persistenceService.saveAsync(root, efficientMode);
        } catch (Exception e) {
            log.error("Failed to save performance config", e);
        }
    }

    /**
     * 保存性能测试配置（供外部调用，如退出时）
     */
    public void save() {
        saveConfig();
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

    /**
     * 从集合中刷新请求数据
     * 重新加载所有请求的最新配置
     */
    private void refreshRequestsFromCollections() {
        saveAllPropertyPanelData();

        // 清除缓存的测试结果数据，避免显示过时的结果
        performanceResultTablePanel.clearResults();
        performanceReportPanel.clearReport();
        performanceTrendPanel.clearTrendDataset();
        apiCostMap.clear();
        apiSuccessMap.clear();
        apiFailMap.clear();
        allRequestResults.clear();
        ApiMetadata.clear(); // 清理API元数据
        csvRowIndex.set(0);

        // 主动触发GC，及时释放清除的缓存数据占用的内存
        System.gc();

        int updatedCount = 0;
        int removedCount = 0;
        List<DefaultMutableTreeNode> nodesToRemove = new ArrayList<>();

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        updatedCount = refreshTreeNode(root, nodesToRemove);
        removedCount = nodesToRemove.size();

        // 移除不存在的请求节点（从后往前删除，避免索引变化）
        for (int i = nodesToRemove.size() - 1; i >= 0; i--) {
            DefaultMutableTreeNode nodeToRemove = nodesToRemove.get(i);
            // 如果删除的是当前选中的请求节点，清空引用
            if (nodeToRemove == currentRequestNode) {
                currentRequestNode = null;
            }
            treeModel.removeNodeFromParent(nodeToRemove);
        }

        // 保存当前选中节点的路径（用于重新定位）
        TreePath currentPath = null;
        if (currentRequestNode != null) {
            currentPath = new TreePath(currentRequestNode.getPath());
        }

        // 保存更新后的配置
        saveConfig();

        // 刷新树显示
        treeModel.reload();

        // 展开所有节点
        for (int i = 0; i < jmeterTree.getRowCount(); i++) {
            jmeterTree.expandRow(i);
        }

        // 重新定位并刷新当前选中的请求节点
        if (currentPath != null) {
            // 尝试重新找到相同路径的节点
            TreePath newPath = findTreePathByPath(currentPath);
            if (newPath != null) {
                jmeterTree.setSelectionPath(newPath);
                DefaultMutableTreeNode newNode = (DefaultMutableTreeNode) newPath.getLastPathComponent();
                Object userObj = newNode.getUserObject();
                if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST) {
                    currentRequestNode = newNode;
                    // 刷新右侧编辑面板
                    if (jtNode.httpRequestItem != null) {
                        requestEditSubPanel.initPanelData(jtNode.httpRequestItem);
                    }
                } else {
                    currentRequestNode = null;
                }
            } else {
                // 路径找不到了（节点可能被删除），清空引用
                currentRequestNode = null;
            }
        }

        // 显示刷新结果
        if (removedCount > 0) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REFRESH_WARNING, removedCount));
        } else if (updatedCount > 0) {
            NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REFRESH_SUCCESS, updatedCount));
        } else {
            NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_NO_REQUEST_TO_REFRESH));
        }
    }

    /**
     * 递归刷新树节点
     */
    private int refreshTreeNode(DefaultMutableTreeNode treeNode, List<DefaultMutableTreeNode> nodesToRemove) {
        int updatedCount = 0;

        Object userObj = treeNode.getUserObject();
        if (userObj instanceof JMeterTreeNode jmNode) {
            // 如果是请求节点，刷新请求数据
            if (jmNode.type == NodeType.REQUEST && jmNode.httpRequestItem != null) {
                String requestId = jmNode.httpRequestItem.getId();
                // 检查 requestId 是否有效
                if (requestId == null || requestId.trim().isEmpty()) {
                    log.warn("Request node has null or empty ID, marking for removal");
                    nodesToRemove.add(treeNode);
                } else {
                    HttpRequestItem latestRequestItem = persistenceService.findRequestItemById(requestId);

                    if (latestRequestItem == null) {
                        // 请求在集合中已被删除，标记为待删除
                        log.warn("Request with ID {} not found in collections", requestId);
                        nodesToRemove.add(treeNode);
                    } else {
                        // 更新请求数据
                        jmNode.httpRequestItem = latestRequestItem;
                        jmNode.name = latestRequestItem.getName();
                        treeModel.nodeChanged(treeNode);
                        updatedCount++;
                    }
                }
            }
        }

        // 递归处理子节点
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) treeNode.getChildAt(i);
            updatedCount += refreshTreeNode(childNode, nodesToRemove);
        }

        return updatedCount;
    }

    /**
     * 在树重建后，根据旧路径查找新节点
     * 通过比较节点的名称和类型来定位
     */
    private TreePath findTreePathByPath(TreePath oldPath) {
        if (oldPath == null) {
            return null;
        }

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        Object[] oldPathObjects = oldPath.getPath();

        // 从根节点开始，逐层查找匹配的节点
        DefaultMutableTreeNode currentNode = root;
        List<DefaultMutableTreeNode> newPath = new ArrayList<>();
        newPath.add(root);

        // 跳过第一个根节点，从第二层开始匹配
        for (int i = 1; i < oldPathObjects.length; i++) {
            Object oldNodeUserObj = ((DefaultMutableTreeNode) oldPathObjects[i]).getUserObject();
            if (!(oldNodeUserObj instanceof JMeterTreeNode oldJmNode)) {
                return null;
            }

            // 在当前节点的子节点中查找匹配的节点
            DefaultMutableTreeNode matchedChild = null;
            for (int j = 0; j < currentNode.getChildCount(); j++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) currentNode.getChildAt(j);
                Object childUserObj = child.getUserObject();
                if (childUserObj instanceof JMeterTreeNode childJmNode) {
                    // 通过名称和类型匹配节点
                    if (childJmNode.type == oldJmNode.type &&
                            childJmNode.name.equals(oldJmNode.name)) {
                        matchedChild = child;
                        break;
                    }
                }
            }

            if (matchedChild == null) {
                // 找不到匹配的子节点，路径断裂
                return null;
            }

            newPath.add(matchedChild);
            currentNode = matchedChild;
        }

        return new TreePath(newPath.toArray());
    }
}

