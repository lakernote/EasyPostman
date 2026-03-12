package com.laker.postman.panel.performance;

import cn.hutool.core.text.CharSequenceUtil;
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
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.panel.performance.assertion.AssertionPropertyPanel;
import com.laker.postman.panel.performance.component.JMeterTreeCellRenderer;
import com.laker.postman.panel.performance.component.TreeNodeTransferHandler;
import com.laker.postman.panel.performance.execution.PerformanceRequestExecutionResult;
import com.laker.postman.panel.performance.execution.PerformanceRequestExecutor;
import com.laker.postman.panel.performance.execution.PerformanceResultRecorder;
import com.laker.postman.panel.performance.model.*;
import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import com.laker.postman.panel.performance.result.PerformanceTrendPanel;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupPropertyPanel;
import com.laker.postman.panel.performance.timer.TimerPropertyPanel;
import com.laker.postman.service.PerformancePersistenceService;
import com.laker.postman.service.collections.RequestCollectionsService;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;
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
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;

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
    private RequestEditSubPanel requestEditSubPanel;
    private JPanel requestEditorHost;
    private RequestItemProtocolEnum currentRequestEditorProtocol = RequestItemProtocolEnum.HTTP;
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
    // 虚拟用户（线程）编号分配器，每个线程启动时分配一个唯一编号
    private final AtomicInteger virtualUserCounter = new AtomicInteger(0);
    // 每个线程持有自己的虚拟用户编号，用于绑定 CSV 行
    private final transient ThreadLocal<Integer> threadVirtualUserIndex = new ThreadLocal<>();

    // 持久化服务
    private transient PerformancePersistenceService persistenceService;

    // 当前选中的请求节点
    private DefaultMutableTreeNode currentRequestNode;
    private final transient Set<EventSource> activeSseSources = ConcurrentHashMap.newKeySet();
    private final transient Set<WebSocket> activeWebSockets = ConcurrentHashMap.newKeySet();
    private transient PerformanceRequestExecutor requestExecutor;
    private transient PerformanceResultRecorder resultRecorder;

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
        sseConnectPanel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.CONNECT);
        propertyPanel.add(sseConnectPanel, SSE_CONNECT);
        sseAwaitPanel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.AWAIT);
        propertyPanel.add(sseAwaitPanel, SSE_AWAIT);
        wsConnectPanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.CONNECT);
        propertyPanel.add(wsConnectPanel, WS_CONNECT);
        wsSendPanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.SEND);
        propertyPanel.add(wsSendPanel, WS_SEND);
        wsAwaitPanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.AWAIT);
        propertyPanel.add(wsAwaitPanel, WS_AWAIT);
        wsClosePanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.CLOSE);
        propertyPanel.add(wsClosePanel, WS_CLOSE);
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
        requestExecutor = new PerformanceRequestExecutor(
                () -> running,
                this::isCancelledOrInterrupted,
                activeSseSources,
                activeWebSockets
        );
        resultRecorder = new PerformanceResultRecorder(
                allRequestResults,
                apiCostMap,
                apiSuccessMap,
                apiFailMap,
                statsLock,
                performanceResultTablePanel,
                PerformancePanel::getJmeterSlowRequestThreshold
        );

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
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3)); // 按钮面板 宽度间距4，高度间距3
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
        csvDataPanel.setContextHelpText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_CSV_USAGE_NOTE));
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

        syncAllRequestStructures((DefaultMutableTreeNode) treeModel.getRoot());

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

        JLabel infoIcon = new JLabel(IconUtil.create("icons/info.svg", 14, 14));
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
        requestEditorHost = new JPanel(new BorderLayout());
        requestEditorHost.add(requestEditSubPanel, BorderLayout.CENTER);
        wrapper.add(requestEditorHost, BorderLayout.CENTER);

        return wrapper;
    }

    private RequestItemProtocolEnum resolveRequestProtocol(HttpRequestItem item) {
        return item != null && item.getProtocol() != null ? item.getProtocol() : RequestItemProtocolEnum.HTTP;
    }

    private boolean isSsePerfRequest(HttpRequestItem item) {
        RequestItemProtocolEnum protocol = resolveRequestProtocol(item);
        return protocol.isSseProtocol() || (protocol.isHttpProtocol() && HttpUtil.isSSERequest(item));
    }

    private boolean isSsePerfRequest(HttpRequestItem item, PreparedRequest req) {
        RequestItemProtocolEnum protocol = resolveRequestProtocol(item);
        return protocol.isSseProtocol() || (protocol.isHttpProtocol() && HttpUtil.isSSERequest(req));
    }

    private boolean isWebSocketPerfRequest(HttpRequestItem item) {
        RequestItemProtocolEnum protocol = resolveRequestProtocol(item);
        return protocol.isWebSocketProtocol();
    }

    private DefaultMutableTreeNode getParentRequestNode(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode current = node;
        while (current != null) {
            Object userObj = current.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST) {
                return current;
            }
            current = (DefaultMutableTreeNode) current.getParent();
        }
        return null;
    }

    private DefaultMutableTreeNode findChildNode(DefaultMutableTreeNode parent, NodeType type) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == type) {
                return child;
            }
        }
        return null;
    }

    private DefaultMutableTreeNode ensureFixedChildNode(DefaultMutableTreeNode parent, NodeType type, String name, int index) {
        DefaultMutableTreeNode existing = findChildNode(parent, type);
        if (existing != null) {
            Object userObj = existing.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode) {
                jtNode.name = name;
            }
            if (parent.getIndex(existing) != index) {
                treeModel.removeNodeFromParent(existing);
                treeModel.insertNodeInto(existing, parent, Math.min(index, parent.getChildCount()));
            } else {
                treeModel.nodeChanged(existing);
            }
            return existing;
        }
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(new JMeterTreeNode(name, type));
        treeModel.insertNodeInto(child, parent, Math.min(index, parent.getChildCount()));
        return child;
    }

    private void moveChildrenByType(DefaultMutableTreeNode from, DefaultMutableTreeNode to, NodeType type) {
        if (from == null || to == null) {
            return;
        }
        List<DefaultMutableTreeNode> toMove = new ArrayList<>();
        for (int i = 0; i < from.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) from.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == type) {
                toMove.add(child);
            }
        }
        for (DefaultMutableTreeNode child : toMove) {
            treeModel.removeNodeFromParent(child);
            treeModel.insertNodeInto(child, to, to.getChildCount());
        }
    }

    private void syncRequestStructure(DefaultMutableTreeNode requestNode, JMeterTreeNode requestData) {
        if (requestNode == null || requestData == null || requestData.httpRequestItem == null) {
            return;
        }

        boolean isSse = isSsePerfRequest(requestData.httpRequestItem);
        boolean isWebSocket = isWebSocketPerfRequest(requestData.httpRequestItem);

        cleanupSseRequestStructure(requestNode, !isSse);
        cleanupWebSocketRequestStructure(requestNode, !isWebSocket);

        if (isSse) {
            if (requestData.ssePerformanceData == null) {
                requestData.ssePerformanceData = new SsePerformanceData();
            }
            DefaultMutableTreeNode connectNode = ensureFixedChildNode(requestNode, NodeType.SSE_CONNECT,
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_NODE_CONNECT), 0);
            DefaultMutableTreeNode awaitNode = ensureFixedChildNode(requestNode, NodeType.SSE_AWAIT,
                    buildSseAwaitNodeTitle(requestData.ssePerformanceData), 1);
            moveChildrenByType(requestNode, awaitNode, NodeType.ASSERTION);
            treeModel.nodeChanged(connectNode);
            treeModel.nodeChanged(awaitNode);
        } else if (isWebSocket) {
            if (requestData.webSocketPerformanceData == null) {
                requestData.webSocketPerformanceData = new WebSocketPerformanceData();
            }
            DefaultMutableTreeNode connectNode = ensureFixedChildNode(requestNode, NodeType.WS_CONNECT,
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_CONNECT), 0);
            refreshWebSocketStepTitles(requestNode);
            treeModel.nodeChanged(connectNode);
        }
    }

    private void cleanupSseRequestStructure(DefaultMutableTreeNode requestNode, boolean removeNodes) {
        DefaultMutableTreeNode connectNode = findChildNode(requestNode, NodeType.SSE_CONNECT);
        DefaultMutableTreeNode awaitNode = findChildNode(requestNode, NodeType.SSE_AWAIT);
        if (removeNodes && awaitNode != null) {
            moveChildrenByType(awaitNode, requestNode, NodeType.ASSERTION);
        }
        if (removeNodes && connectNode != null) {
            treeModel.removeNodeFromParent(connectNode);
        }
        if (removeNodes && awaitNode != null) {
            treeModel.removeNodeFromParent(awaitNode);
        }
    }

    private void cleanupWebSocketRequestStructure(DefaultMutableTreeNode requestNode, boolean removeNodes) {
        DefaultMutableTreeNode connectNode = findChildNode(requestNode, NodeType.WS_CONNECT);
        List<DefaultMutableTreeNode> wsStepNodes = new ArrayList<>();
        for (int i = 0; i < requestNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) requestNode.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode) {
                if (jtNode.type == NodeType.WS_AWAIT) {
                    moveChildrenByType(child, requestNode, NodeType.ASSERTION);
                }
                if (jtNode.type == NodeType.WS_SEND || jtNode.type == NodeType.WS_AWAIT || jtNode.type == NodeType.WS_CLOSE) {
                    wsStepNodes.add(child);
                }
            }
        }
        if (removeNodes && connectNode != null) {
            treeModel.removeNodeFromParent(connectNode);
        }
        if (removeNodes) {
            for (DefaultMutableTreeNode wsStepNode : wsStepNodes) {
                treeModel.removeNodeFromParent(wsStepNode);
            }
        }
    }

    private void refreshWebSocketStepTitles(DefaultMutableTreeNode requestNode) {
        for (int i = 0; i < requestNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) requestNode.getChildAt(i);
            Object userObj = child.getUserObject();
            if (!(userObj instanceof JMeterTreeNode jtNode)) {
                continue;
            }
            switch (jtNode.type) {
                case WS_SEND -> jtNode.name = buildWebSocketSendNodeTitle(jtNode.webSocketPerformanceData);
                case WS_AWAIT -> jtNode.name = buildWebSocketAwaitNodeTitle(jtNode.webSocketPerformanceData);
                case WS_CLOSE -> jtNode.name = I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_CLOSE);
                default -> {
                }
            }
            treeModel.nodeChanged(child);
        }
    }

    private WebSocketPerformanceData copyWebSocketData(WebSocketPerformanceData source) {
        WebSocketPerformanceData target = new WebSocketPerformanceData();
        if (source == null) {
            return target;
        }
        target.connectTimeoutMs = source.connectTimeoutMs;
        target.sendMode = source.sendMode;
        target.sendContentSource = source.sendContentSource;
        target.customSendBody = source.customSendBody;
        target.sendCount = source.sendCount;
        target.sendIntervalMs = source.sendIntervalMs;
        target.completionMode = source.completionMode;
        target.firstMessageTimeoutMs = source.firstMessageTimeoutMs;
        target.holdConnectionMs = source.holdConnectionMs;
        target.targetMessageCount = source.targetMessageCount;
        target.messageFilter = source.messageFilter;
        return target;
    }

    private boolean isWebSocketStepNode(NodeType type) {
        return type == NodeType.WS_SEND || type == NodeType.WS_AWAIT || type == NodeType.WS_CLOSE;
    }

    private DefaultMutableTreeNode resolveWebSocketStepParent(DefaultMutableTreeNode selectedNode) {
        if (selectedNode == null || !(selectedNode.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return null;
        }
        if (jtNode.type == NodeType.REQUEST && isWebSocketPerfRequest(jtNode.httpRequestItem)) {
            return selectedNode;
        }
        if (jtNode.type == NodeType.WS_CONNECT || isWebSocketStepNode(jtNode.type)) {
            return getParentRequestNode(selectedNode);
        }
        return null;
    }

    private DefaultMutableTreeNode createWebSocketStepNode(NodeType type, WebSocketPerformanceData requestDefaults) {
        JMeterTreeNode nodeData;
        switch (type) {
            case WS_SEND -> {
                nodeData = new JMeterTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_SEND), NodeType.WS_SEND);
                WebSocketPerformanceData stepData = copyWebSocketData(requestDefaults);
                stepData.sendMode = WebSocketPerformanceData.SendMode.REQUEST_BODY_ON_CONNECT;
                stepData.sendCount = 1;
                stepData.sendIntervalMs = 1000;
                nodeData.webSocketPerformanceData = stepData;
                nodeData.name = buildWebSocketSendNodeTitle(stepData);
            }
            case WS_AWAIT -> {
                nodeData = new JMeterTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_AWAIT), NodeType.WS_AWAIT);
                WebSocketPerformanceData stepData = copyWebSocketData(requestDefaults);
                stepData.completionMode = WebSocketPerformanceData.CompletionMode.FIRST_MESSAGE;
                stepData.firstMessageTimeoutMs = Math.max(100, stepData.firstMessageTimeoutMs);
                stepData.targetMessageCount = 1;
                nodeData.webSocketPerformanceData = stepData;
                nodeData.name = buildWebSocketAwaitNodeTitle(stepData);
            }
            case WS_CLOSE -> {
                nodeData = new JMeterTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_CLOSE), NodeType.WS_CLOSE);
                nodeData.webSocketPerformanceData = copyWebSocketData(requestDefaults);
            }
            default -> throw new IllegalArgumentException("Unsupported WebSocket step type: " + type);
        }
        return new DefaultMutableTreeNode(nodeData);
    }

    private void addWebSocketStepNode(NodeType type) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode requestNode = resolveWebSocketStepParent(selectedNode);
        if (requestNode == null || !(requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode)) {
            return;
        }
        WebSocketPerformanceData defaults = requestJtNode.webSocketPerformanceData != null
                ? requestJtNode.webSocketPerformanceData
                : new WebSocketPerformanceData();
        DefaultMutableTreeNode newNode = createWebSocketStepNode(type, defaults);
        int insertIndex = requestNode.getChildCount();
        if (selectedNode != null && selectedNode.getParent() == requestNode) {
            insertIndex = requestNode.getIndex(selectedNode) + 1;
        }
        insertIndex = Math.max(1, insertIndex);
        treeModel.insertNodeInto(newNode, requestNode, Math.min(insertIndex, requestNode.getChildCount()));
        refreshWebSocketStepTitles(requestNode);
        jmeterTree.expandPath(new TreePath(requestNode.getPath()));
        jmeterTree.setSelectionPath(new TreePath(newNode.getPath()));
        saveConfig();
    }

    private void addTimerNode() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
        if (selectedNode == null) {
            return;
        }
        DefaultMutableTreeNode parentNode = selectedNode;
        int insertIndex = parentNode.getChildCount();
        DefaultMutableTreeNode wsParent = resolveWebSocketStepParent(selectedNode);
        if (wsParent != null) {
            parentNode = wsParent;
            insertIndex = selectedNode.getParent() == wsParent ? wsParent.getIndex(selectedNode) + 1 : wsParent.getChildCount();
            insertIndex = Math.max(1, insertIndex);
        }
        DefaultMutableTreeNode timer = new DefaultMutableTreeNode(new JMeterTreeNode("Timer", NodeType.TIMER));
        treeModel.insertNodeInto(timer, parentNode, Math.min(insertIndex, parentNode.getChildCount()));
        jmeterTree.expandPath(new TreePath(parentNode.getPath()));
        jmeterTree.setSelectionPath(new TreePath(timer.getPath()));
        saveConfig();
    }

    private String buildSseAwaitNodeTitle(SsePerformanceData data) {
        if (data == null) {
            return I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_NODE_AWAIT);
        }
        StringJoiner joiner = new StringJoiner(" | ",
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_NODE_AWAIT) + " [",
                "]");
        joiner.add(getSseCompletionModeLabel(data.completionMode));
        switch (data.completionMode) {
            case FIRST_MESSAGE -> joiner.add(formatSseDuration(data.firstMessageTimeoutMs));
            case MESSAGE_COUNT -> {
                joiner.add(String.valueOf(Math.max(1, data.targetMessageCount)));
                joiner.add(formatSseDuration(data.holdConnectionMs));
            }
            case FIXED_DURATION -> joiner.add(formatSseDuration(data.holdConnectionMs));
        }
        if (CharSequenceUtil.isNotBlank(data.eventNameFilter)) {
            joiner.add("event=" + data.eventNameFilter.trim());
        }
        return joiner.toString();
    }

    private String getSseCompletionModeLabel(SsePerformanceData.CompletionMode mode) {
        if (mode == null) {
            mode = SsePerformanceData.CompletionMode.FIRST_MESSAGE;
        }
        return switch (mode) {
            case FIRST_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_FIRST_MESSAGE);
            case FIXED_DURATION -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_FIXED_DURATION);
            case MESSAGE_COUNT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_MESSAGE_COUNT);
        };
    }

    private String formatSseDuration(int durationMs) {
        if (durationMs >= 1000 && durationMs % 1000 == 0) {
            return (durationMs / 1000) + "s";
        }
        return durationMs + "ms";
    }

    private String buildWebSocketSendNodeTitle(WebSocketPerformanceData data) {
        if (data == null) {
            return I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_SEND);
        }
        String modeLabel = switch (data.sendMode) {
            case NONE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_NONE);
            case REQUEST_BODY_ON_CONNECT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY);
            case REQUEST_BODY_REPEAT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY_REPEAT);
        };
        StringJoiner joiner = new StringJoiner(" | ",
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_SEND) + " [",
                "]");
        joiner.add(modeLabel);
        WebSocketPerformanceData.SendContentSource contentSource = data.sendContentSource != null
                ? data.sendContentSource
                : WebSocketPerformanceData.SendContentSource.REQUEST_BODY;
        if (data.sendMode != WebSocketPerformanceData.SendMode.NONE
                && contentSource == WebSocketPerformanceData.SendContentSource.CUSTOM_TEXT) {
            joiner.add(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_CONTENT_CUSTOM_TEXT));
        }
        if (data.sendMode == WebSocketPerformanceData.SendMode.REQUEST_BODY_REPEAT) {
            joiner.add(Math.max(1, data.sendCount) + "x");
            joiner.add(formatSseDuration(Math.max(0, data.sendIntervalMs)));
        }
        return joiner.toString();
    }

    private String buildWebSocketAwaitNodeTitle(WebSocketPerformanceData data) {
        if (data == null) {
            return I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_AWAIT);
        }
        StringJoiner joiner = new StringJoiner(" | ",
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_AWAIT) + " [",
                "]");
        joiner.add(getWebSocketCompletionModeLabel(data.completionMode));
        switch (data.completionMode) {
            case FIRST_MESSAGE -> joiner.add(formatSseDuration(data.firstMessageTimeoutMs));
            case MATCHED_MESSAGE -> joiner.add(formatSseDuration(data.firstMessageTimeoutMs));
            case MESSAGE_COUNT -> {
                joiner.add(String.valueOf(Math.max(1, data.targetMessageCount)));
                joiner.add(formatSseDuration(data.holdConnectionMs));
            }
            case FIXED_DURATION -> joiner.add(formatSseDuration(data.holdConnectionMs));
        }
        if (CharSequenceUtil.isNotBlank(data.messageFilter)) {
            joiner.add("contains=" + data.messageFilter.trim());
        }
        return joiner.toString();
    }

    private String getWebSocketCompletionModeLabel(WebSocketPerformanceData.CompletionMode mode) {
        if (mode == null) {
            mode = WebSocketPerformanceData.CompletionMode.FIRST_MESSAGE;
        }
        return switch (mode) {
            case FIRST_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_FIRST_MESSAGE);
            case MATCHED_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_MATCHED_MESSAGE);
            case FIXED_DURATION -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_FIXED_DURATION);
            case MESSAGE_COUNT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_MESSAGE_COUNT);
        };
    }

    private void saveSseStageNode(DefaultMutableTreeNode stageNode) {
        if (stageNode == null || !(stageNode.getUserObject() instanceof JMeterTreeNode stageJtNode)) {
            return;
        }
        DefaultMutableTreeNode requestNode = getParentRequestNode(stageNode);
        if (requestNode == null || !(requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode)) {
            return;
        }
        switch (stageJtNode.type) {
            case SSE_CONNECT -> sseConnectPanel.saveData();
            case SSE_AWAIT -> sseAwaitPanel.saveData();
            default -> {
                return;
            }
        }
        syncRequestStructure(requestNode, requestJtNode);
    }

    private void saveWebSocketStageNode(DefaultMutableTreeNode stageNode) {
        if (stageNode == null || !(stageNode.getUserObject() instanceof JMeterTreeNode stageJtNode)) {
            return;
        }
        DefaultMutableTreeNode requestNode = getParentRequestNode(stageNode);
        if (requestNode == null || !(requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode)) {
            return;
        }
        switch (stageJtNode.type) {
            case WS_CONNECT -> {
                wsConnectPanel.saveData();
                treeModel.nodeChanged(stageNode);
            }
            case WS_SEND -> wsSendPanel.saveData();
            case WS_AWAIT -> wsAwaitPanel.saveData();
            case WS_CLOSE -> wsClosePanel.saveData();
            default -> {
                return;
            }
        }
        syncRequestStructure(requestNode, requestJtNode);
    }

    private void syncAllRequestStructures(DefaultMutableTreeNode node) {
        Object userObj = node.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST) {
            syncRequestStructure(node, jtNode);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            syncAllRequestStructures((DefaultMutableTreeNode) node.getChildAt(i));
        }
    }

    private void switchRequestEditor(HttpRequestItem item) {
        RequestItemProtocolEnum protocol = resolveRequestProtocol(item);
        if (requestEditSubPanel == null || requestEditorHost == null || protocol != currentRequestEditorProtocol) {
            if (requestEditorHost != null && requestEditSubPanel != null) {
                requestEditorHost.remove(requestEditSubPanel);
            }
            requestEditSubPanel = new RequestEditSubPanel("", protocol);
            currentRequestEditorProtocol = protocol;
            if (requestEditorHost != null) {
                requestEditorHost.add(requestEditSubPanel, BorderLayout.CENTER);
                requestEditorHost.revalidate();
                requestEditorHost.repaint();
            }
        }

        if (item != null) {
            requestEditSubPanel.initPanelData(item);
        }
    }

    private void saveRequestNodeData(DefaultMutableTreeNode node) {
        if (requestEditSubPanel == null || node == null) {
            return;
        }
        Object userObj = node.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST) {
            jtNode.httpRequestItem = requestEditSubPanel.getCurrentRequest();
            syncRequestStructure(node, jtNode);
        }
    }

    /**
     * 刷新当前选中的请求节点
     */
    private void refreshCurrentRequest() {
        log.debug("[refreshCurrentRequest] 开始执行单节点刷新, currentRequestNode={}", currentRequestNode);
        if (currentRequestNode == null) {
            log.debug("[refreshCurrentRequest] currentRequestNode 为 null，退出");
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_NO_REQUEST_SELECTED));
            return;
        }

        Object userObj = currentRequestNode.getUserObject();
        if (!(userObj instanceof JMeterTreeNode jmNode) || jmNode.type != NodeType.REQUEST) {
            log.debug("[refreshCurrentRequest] 当前节点类型不是 REQUEST，退出");
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_NO_REQUEST_SELECTED));
            return;
        }

        // 优先从 requestEditSubPanel 获取当前展示的 id，确保与面板显示保持一致
        // 如果 editSub 中有有效 id 则使用，否则降级到节点存储的 id
        String editSubId = requestEditSubPanel.getId();
        String nodeId = jmNode.httpRequestItem != null ? jmNode.httpRequestItem.getId() : null;
        log.debug("[refreshCurrentRequest] editSub.id={}, node.httpRequestItem.id={}", editSubId, nodeId);

        String requestId = editSubId;
        if (requestId == null || requestId.trim().isEmpty()) {
            log.debug("[refreshCurrentRequest] editSub.id 为空，降级使用 node.id");
            if (jmNode.httpRequestItem == null) {
                log.debug("[refreshCurrentRequest] node.httpRequestItem 也为 null，退出");
                NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_NO_REQUEST_SELECTED));
                return;
            }
            requestId = nodeId;
        }

        if (requestId == null || requestId.trim().isEmpty()) {
            log.debug("[refreshCurrentRequest] requestId 最终为空，退出");
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REQUEST_NOT_FOUND_IN_COLLECTIONS));
            return;
        }

        log.debug("[refreshCurrentRequest] 准备从集合中查找请求, requestId={}", requestId);
        HttpRequestItem latestRequestItem = persistenceService.findRequestItemById(requestId);
        log.debug("[refreshCurrentRequest] 集合中找到的请求: name={}, url={}, id={}",
                latestRequestItem != null ? latestRequestItem.getName() : "null",
                latestRequestItem != null ? latestRequestItem.getUrl() : "null",
                latestRequestItem != null ? latestRequestItem.getId() : "null");

        if (latestRequestItem == null) {
            log.debug("[refreshCurrentRequest] 集合中未找到 requestId={} 对应的请求，退出", requestId);
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REQUEST_NOT_FOUND_IN_COLLECTIONS));
            return;
        }

        // 更新节点中的请求数据
        jmNode.httpRequestItem = latestRequestItem;
        syncRequestStructure(currentRequestNode, jmNode);
        jmNode.name = latestRequestItem.getName();
        treeModel.nodeChanged(currentRequestNode);
        // 清除 InheritanceCache，防止下次执行时 PreparedRequestBuilder.build() 拿到旧缓存
        PreparedRequestBuilder.invalidateCacheForRequest(requestId);
        log.debug("[refreshCurrentRequest] 已将节点数据更新并清除 InheritanceCache, name={}, requestId={}", latestRequestItem.getName(), requestId);

        // 用从集合取回的最新数据直接刷新右侧编辑面板（不依赖节点引用，避免被 listener 覆盖）
        log.debug("[refreshCurrentRequest] 调用 initPanelData, url={}", latestRequestItem.getUrl());
        switchRequestEditor(latestRequestItem);
        log.debug("[refreshCurrentRequest] initPanelData 执行完毕，editSub.id={}", requestEditSubPanel.getId());

        // 保存配置
        saveConfig();

        NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REQUEST_REFRESHED));
        log.debug("[refreshCurrentRequest] 单节点刷新完成");
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
        sseConnectPanel.forceCommitAllSpinners();
        sseAwaitPanel.forceCommitAllSpinners();
        wsConnectPanel.forceCommitAllSpinners();
        wsSendPanel.forceCommitAllSpinners();
        wsAwaitPanel.forceCommitAllSpinners();

        // 2. 保存所有属性面板数据
        saveAllPropertyPanelData();

        // 3. 持久化到文件
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        persistenceService.save(root, efficientMode);

        // 4. 显示成功提示
        NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_SAVE_SUCCESS));
    }

    private void saveAllPropertyPanelData() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
        if (selectedNode != null && selectedNode.getUserObject() instanceof JMeterTreeNode jtNode) {
            switch (jtNode.type) {
                case THREAD_GROUP -> threadGroupPanel.saveThreadGroupData();
                case REQUEST -> {
                    if (requestEditSubPanel != null && currentRequestNode != null) {
                        saveRequestNodeData(currentRequestNode);
                    }
                }
                case ASSERTION -> assertionPanel.saveAssertionData();
                case TIMER -> timerPanel.saveTimerData();
                case SSE_CONNECT, SSE_AWAIT -> saveSseStageNode(selectedNode);
                case WS_CONNECT, WS_SEND, WS_AWAIT, WS_CLOSE -> saveWebSocketStageNode(selectedNode);
                default -> {
                }
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
        // 虚拟用户编号重置（每次测试重新从0分配）
        virtualUserCounter.set(0);

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
            final int vuIndex = virtualUserCounter.getAndIncrement();
            executor.submit(() -> {
                threadVirtualUserIndex.set(vuIndex);
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
                    threadVirtualUserIndex.remove();
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

                // 取消所有正在执行的网络请求
                cancelAllNetworkCalls();

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

        // 当前活跃线程数（用于并发调度，不是累计启动数）
        AtomicInteger activeWorkerThreads = new AtomicInteger(0);

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
                while (running) {
                    int current = activeWorkerThreads.get();
                    if (current >= targetThreads) {
                        break;
                    }
                    if (!activeWorkerThreads.compareAndSet(current, current + 1)) {
                        continue;
                    }
                    final int vuIndex = virtualUserCounter.getAndIncrement();
                    executor.submit(() -> {
                        threadVirtualUserIndex.set(vuIndex);
                        activeThreads.incrementAndGet();
                        SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                        try {
                            // 循环执行直到结束
                            while (running && System.currentTimeMillis() - startTime < totalDuration * 1000L) {
                                runTaskIteration(groupNode);
                            }
                        } finally {
                            activeWorkerThreads.decrementAndGet();
                            activeThreads.decrementAndGet();
                            SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                            threadVirtualUserIndex.remove();
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
        AtomicInteger activeWorkerThreads = new AtomicInteger(0);

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
            activeWorkerThreads.incrementAndGet();
            final int vuIndex = virtualUserCounter.getAndIncrement();
            Thread thread = new Thread(() -> {
                threadVirtualUserIndex.set(vuIndex);
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
                    activeWorkerThreads.decrementAndGet();
                    activeThreads.decrementAndGet();
                    SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                    // 从跟踪Map中移除此线程
                    threadEndTimes.remove(Thread.currentThread());
                    threadVirtualUserIndex.remove();
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
                adjustSpikeThreadCount(groupNode, tg, activeWorkerThreads, targetThreads, totalTime, progressLabel, totalThreads, threadEndTimes);
            }
            // 保持阶段
            else if (elapsedSeconds < adjustedRampUpTime + adjustedHoldTime) {
                targetThreads = maxThreads;
                // 保持线程数
                adjustSpikeThreadCount(groupNode, tg, activeWorkerThreads, targetThreads, totalTime, progressLabel, totalThreads, threadEndTimes);
            }
            // 下降阶段
            else {
                double progress = (double) (elapsedSeconds - adjustedRampUpTime - adjustedHoldTime) / adjustedRampDownTime;
                targetThreads = maxThreads - (int) (progress * (maxThreads - minThreads));
                targetThreads = Math.max(targetThreads, minThreads); // 不低于最小线程数

                // 在下降阶段，通过设置线程结束时间来减少活跃线程数
                int threadsToRemove = activeWorkerThreads.get() - targetThreads;
                if (threadsToRemove > 0) {
                    // 找出可以终止的线程
                    threadEndTimes.keySet().stream()
                            .filter(t -> t.isAlive() && threadEndTimes.get(t) == Long.MAX_VALUE)
                            .limit(threadsToRemove)
                            .forEach(t -> threadEndTimes.put(t, now + 500)); // 设置一个短暂的结束时间
                }

                // 仍然需要增加线程的情况
                adjustSpikeThreadCount(groupNode, tg, activeWorkerThreads, targetThreads, totalTime, progressLabel, totalThreads, threadEndTimes);
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
        AtomicInteger activeWorkerThreads = new AtomicInteger(0);

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
            activeWorkerThreads.incrementAndGet();
            final int vuIndex = virtualUserCounter.getAndIncrement();
            Thread thread = new Thread(() -> {
                threadVirtualUserIndex.set(vuIndex);
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
                    activeWorkerThreads.decrementAndGet();
                    activeThreads.decrementAndGet();
                    SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                    // 从跟踪Map中移除此线程
                    threadEndTimes.remove(Thread.currentThread());
                    threadVirtualUserIndex.remove();
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
            adjustStairsThreadCount(groupNode, activeWorkerThreads, targetThreads, totalTime, progressLabel, totalThreads, threadEndTimes);

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
                                        ThreadGroupData tg,
                                        AtomicInteger activeWorkerThreads, int targetThreads,
                                        int totalTime, JLabel progressLabel, int totalThreads,
                                        ConcurrentHashMap<Thread, Long> threadEndTimes) {
        int current = activeWorkerThreads.get();

        // 需要增加线程
        if (current < targetThreads) {
            int threadsToAdd = targetThreads - current;
            for (int i = 0; i < threadsToAdd; i++) {
                if (!running) return;

                activeWorkerThreads.incrementAndGet();
                final int vuIndex = virtualUserCounter.getAndIncrement();
                Thread thread = new Thread(() -> {
                    threadVirtualUserIndex.set(vuIndex);
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
                        activeWorkerThreads.decrementAndGet();
                        activeThreads.decrementAndGet();
                        SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                        // 从跟踪Map中移除此线程
                        threadEndTimes.remove(Thread.currentThread());
                        threadVirtualUserIndex.remove();
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
                // 计算下降阶段的总时间
                int rampDownTime = tg.spikeRampDownTime;
                int totalSpikeTime = tg.spikeDuration;
                int rampUpTime = tg.spikeRampUpTime;
                int holdTime = tg.spikeHoldTime;

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
                                         AtomicInteger activeWorkerThreads, int targetThreads,
                                         int totalTime, JLabel progressLabel, int totalThreads,
                                         ConcurrentHashMap<Thread, Long> threadEndTimes) {
        int current = activeWorkerThreads.get();

        // 需要增加线程 阶梯模式下，不需要减少线程，只增加
        if (current < targetThreads) {
            int threadsToAdd = targetThreads - current;
            for (int i = 0; i < threadsToAdd; i++) {
                if (!running) return;

                activeWorkerThreads.incrementAndGet();
                final int vuIndex = virtualUserCounter.getAndIncrement();
                Thread thread = new Thread(() -> {
                    threadVirtualUserIndex.set(vuIndex);
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
                        activeWorkerThreads.decrementAndGet();
                        activeThreads.decrementAndGet();
                        SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                        // 从跟踪Map中移除此线程
                        threadEndTimes.remove(Thread.currentThread());
                        threadVirtualUserIndex.remove();
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
    private boolean isCancelledOrInterrupted(Throwable ex) {
        if (ex == null) {
            return false;
        }
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
        return isCancelledOrInterrupted(ex.getCause());
    }

    // 执行单个请求节点
    private void executeRequestNode(Object userObj, DefaultMutableTreeNode child) {
        // 如果测试已停止，立即返回，不执行请求
        if (!running) {
            return;
        }

        if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST && jtNode.httpRequestItem != null) {
            PerformanceRequestExecutionResult executionResult = requestExecutor.execute(child, jtNode, resolveCsvRowForCurrentThread());
            if (executionResult == null) {
                return;
            }
            resultRecorder.record(executionResult, efficientMode);
            if (executionResult.interrupted) {
                log.debug("跳过被中断请求的统计: {}", jtNode.httpRequestItem.getName());
            }

            // ====== 定时器延迟（sleep） ======
            // 如果测试已停止，跳过定时器延迟
            if (!running) {
                return;
            }

            for (int j = 0; j < child.getChildCount() && running && !executionResult.webSocketRequest; j++) {
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

    private Map<String, String> resolveCsvRowForCurrentThread() {
        if (csvDataPanel == null || !csvDataPanel.hasData()) {
            return null;
        }
        int rowCount = csvDataPanel.getRowCount();
        if (rowCount <= 0) {
            return null;
        }
        Integer vuIndex = threadVirtualUserIndex.get();
        int rowIdx = (vuIndex != null ? vuIndex : 0) % rowCount;
        return csvDataPanel.getRowData(rowIdx);
    }

    @Override
    protected void registerListeners() {
        jmeterTree.addTreeSelectionListener(new TreeSelectionListener() {
            private DefaultMutableTreeNode lastNode = null;

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                TreePath[] selectedPaths = jmeterTree.getSelectionPaths();
                if (selectedPaths != null && selectedPaths.length > 1) {
                    return;
                }

                if (lastNode != null && lastNode.getUserObject() instanceof JMeterTreeNode jtNode) {
                    switch (jtNode.type) {
                        case THREAD_GROUP -> threadGroupPanel.saveThreadGroupData();
                        case REQUEST -> saveRequestNodeData(lastNode);
                        case ASSERTION -> assertionPanel.saveAssertionData();
                        case TIMER -> timerPanel.saveTimerData();
                        case SSE_CONNECT, SSE_AWAIT -> saveSseStageNode(lastNode);
                        case WS_CONNECT, WS_SEND, WS_AWAIT, WS_CLOSE -> saveWebSocketStageNode(lastNode);
                        default -> {
                        }
                    }
                }

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
                if (node == null || !(node.getUserObject() instanceof JMeterTreeNode jtNode)) {
                    propertyCardLayout.show(propertyPanel, EMPTY);
                    currentRequestNode = null;
                    lastNode = node;
                    return;
                }

                switch (jtNode.type) {
                    case THREAD_GROUP -> {
                        propertyCardLayout.show(propertyPanel, THREAD_GROUP);
                        threadGroupPanel.setThreadGroupData(jtNode);
                        currentRequestNode = null;
                    }
                    case REQUEST -> {
                        propertyCardLayout.show(propertyPanel, REQUEST);
                        currentRequestNode = node;
                        if (jtNode.httpRequestItem != null) {
                            syncRequestStructure(node, jtNode);
                            switchRequestEditor(jtNode.httpRequestItem);
                        }
                    }
                    case ASSERTION -> {
                        propertyCardLayout.show(propertyPanel, ASSERTION);
                        assertionPanel.setAssertionData(jtNode);
                        currentRequestNode = null;
                    }
                    case TIMER -> {
                        propertyCardLayout.show(propertyPanel, TIMER);
                        timerPanel.setTimerData(jtNode);
                        currentRequestNode = null;
                    }
                    case SSE_CONNECT -> {
                        DefaultMutableTreeNode requestNode = getParentRequestNode(node);
                        if (requestNode != null && requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode) {
                            propertyCardLayout.show(propertyPanel, SSE_CONNECT);
                            sseConnectPanel.setRequestNode(requestJtNode);
                        } else {
                            propertyCardLayout.show(propertyPanel, EMPTY);
                        }
                        currentRequestNode = null;
                    }
                    case SSE_AWAIT -> {
                        DefaultMutableTreeNode requestNode = getParentRequestNode(node);
                        if (requestNode != null && requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode) {
                            propertyCardLayout.show(propertyPanel, SSE_AWAIT);
                            sseAwaitPanel.setRequestNode(requestJtNode);
                        } else {
                            propertyCardLayout.show(propertyPanel, EMPTY);
                        }
                        currentRequestNode = null;
                    }
                    case WS_CONNECT -> {
                        DefaultMutableTreeNode requestNode = getParentRequestNode(node);
                        if (requestNode != null && requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode) {
                            propertyCardLayout.show(propertyPanel, WS_CONNECT);
                            wsConnectPanel.setNode(requestJtNode);
                        } else {
                            propertyCardLayout.show(propertyPanel, EMPTY);
                        }
                        currentRequestNode = null;
                    }
                    case WS_SEND -> {
                        propertyCardLayout.show(propertyPanel, WS_SEND);
                        wsSendPanel.setNode(jtNode);
                        currentRequestNode = null;
                    }
                    case WS_AWAIT -> {
                        propertyCardLayout.show(propertyPanel, WS_AWAIT);
                        wsAwaitPanel.setNode(jtNode);
                        currentRequestNode = null;
                    }
                    case WS_CLOSE -> {
                        propertyCardLayout.show(propertyPanel, WS_CLOSE);
                        wsClosePanel.setNode(jtNode);
                        currentRequestNode = null;
                    }
                    default -> {
                        propertyCardLayout.show(propertyPanel, EMPTY);
                        currentRequestNode = null;
                    }
                }
                lastNode = node;
            }
        });

        JPopupMenu treeMenu = new JPopupMenu();
        JMenuItem addThreadGroup = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_THREAD_GROUP));
        JMenuItem addRequest = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_REQUEST));
        JMenuItem addWsSend = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_WS_SEND));
        JMenuItem addWsAwait = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_WS_AWAIT));
        JMenuItem addWsClose = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_WS_CLOSE));
        JMenuItem addAssertion = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_ASSERTION));
        JMenuItem addTimer = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ADD_TIMER));
        JMenuItem renameNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_RENAME));
        JMenuItem deleteNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_DELETE));
        JMenuItem enableNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_ENABLE));
        JMenuItem disableNode = new JMenuItem(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MENU_DISABLE));
        renameNode.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0));
        deleteNode.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));

        JSeparator separator1 = new JSeparator();
        JSeparator separator2 = new JSeparator();

        treeMenu.add(addThreadGroup);
        treeMenu.add(addRequest);
        treeMenu.add(addWsSend);
        treeMenu.add(addWsAwait);
        treeMenu.add(addWsClose);
        treeMenu.add(addAssertion);
        treeMenu.add(addTimer);
        treeMenu.add(separator1);
        treeMenu.add(enableNode);
        treeMenu.add(disableNode);
        treeMenu.add(separator2);
        treeMenu.add(renameNode);
        treeMenu.add(deleteNode);

        Runnable updateMenuSeparators = () -> {
            boolean hasAddGroup = addThreadGroup.isVisible() || addRequest.isVisible()
                    || addWsSend.isVisible() || addWsAwait.isVisible() || addWsClose.isVisible()
                    || addAssertion.isVisible() || addTimer.isVisible();
            boolean hasToggleGroup = enableNode.isVisible() || disableNode.isVisible();
            boolean hasEditGroup = renameNode.isVisible() || deleteNode.isVisible();
            separator1.setVisible(hasAddGroup && (hasToggleGroup || hasEditGroup));
            separator2.setVisible(hasToggleGroup && hasEditGroup);
        };

        addThreadGroup.addActionListener(e -> {
            DefaultMutableTreeNode root1 = (DefaultMutableTreeNode) treeModel.getRoot();
            DefaultMutableTreeNode group = new DefaultMutableTreeNode(new JMeterTreeNode("Thread Group", NodeType.THREAD_GROUP));
            treeModel.insertNodeInto(group, root1, root1.getChildCount());
            jmeterTree.expandPath(new TreePath(root1.getPath()));
            saveConfig();
        });
        addRequest.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
            if (node == null) return;
            Object userObj = node.getUserObject();
            if (!(userObj instanceof JMeterTreeNode jtNode) || jtNode.type != NodeType.THREAD_GROUP) {
                JOptionPane.showMessageDialog(PerformancePanel.this, I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_SELECT_THREAD_GROUP), I18nUtil.getMessage(MessageKeys.GENERAL_INFO), JOptionPane.WARNING_MESSAGE);
                return;
            }
            RequestCollectionsService.showMultiSelectRequestDialog(selectedList -> {
                if (selectedList == null || selectedList.isEmpty()) return;

                List<HttpRequestItem> supportedList = selectedList.stream()
                        .filter(reqItem -> {
                            RequestItemProtocolEnum protocol = resolveRequestProtocol(reqItem);
                            return protocol.isHttpProtocol() || protocol.isSseProtocol() || protocol.isWebSocketProtocol();
                        })
                        .toList();

                if (supportedList.isEmpty()) {
                    NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.MSG_ONLY_HTTP_SSE_WS_SUPPORTED));
                    return;
                }

                List<DefaultMutableTreeNode> newNodes = new ArrayList<>();
                for (HttpRequestItem reqItem : supportedList) {
                    DefaultMutableTreeNode req = new DefaultMutableTreeNode(new JMeterTreeNode(reqItem.getName(), NodeType.REQUEST, reqItem));
                    treeModel.insertNodeInto(req, node, node.getChildCount());
                    syncRequestStructure(req, (JMeterTreeNode) req.getUserObject());
                    newNodes.add(req);
                }
                jmeterTree.expandPath(new TreePath(node.getPath()));
                TreePath newPath = new TreePath(newNodes.get(0).getPath());
                jmeterTree.setSelectionPath(newPath);
                propertyCardLayout.show(propertyPanel, REQUEST);
                JMeterTreeNode newRequestNode = (JMeterTreeNode) newNodes.get(0).getUserObject();
                switchRequestEditor(newRequestNode.httpRequestItem);
                saveConfig();
            });
        });
        addWsSend.addActionListener(e -> addWebSocketStepNode(NodeType.WS_SEND));
        addWsAwait.addActionListener(e -> addWebSocketStepNode(NodeType.WS_AWAIT));
        addWsClose.addActionListener(e -> addWebSocketStepNode(NodeType.WS_CLOSE));
        addAssertion.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
            if (node == null) return;
            DefaultMutableTreeNode parentNode = node;
            Object userObj = node.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode
                    && (jtNode.type == NodeType.SSE_AWAIT || jtNode.type == NodeType.WS_AWAIT)) {
                parentNode = node;
            }
            DefaultMutableTreeNode assertion = new DefaultMutableTreeNode(new JMeterTreeNode("Assertion", NodeType.ASSERTION));
            treeModel.insertNodeInto(assertion, parentNode, parentNode.getChildCount());
            jmeterTree.expandPath(new TreePath(parentNode.getPath()));
            saveConfig();
        });
        addTimer.addActionListener(e -> {
            addTimerNode();
        });

        Action renameAction = new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
                if (node == null) return;
                Object userObj = node.getUserObject();
                if (!(userObj instanceof JMeterTreeNode jtNode)) return;
                if (jtNode.type == NodeType.ROOT
                        || jtNode.type == NodeType.SSE_CONNECT
                        || jtNode.type == NodeType.SSE_AWAIT
                        || jtNode.type == NodeType.WS_CONNECT
                        || isWebSocketStepNode(jtNode.type)) {
                    return;
                }
                String oldName = jtNode.name;
                String newName = JOptionPane.showInputDialog(PerformancePanel.this,
                        I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_RENAME_NODE), oldName);
                if (newName != null && !newName.trim().isEmpty()) {
                    jtNode.name = newName.trim();
                    if (jtNode.type == NodeType.REQUEST && jtNode.httpRequestItem != null) {
                        jtNode.httpRequestItem.setName(newName.trim());
                        switchRequestEditor(jtNode.httpRequestItem);
                    }
                    treeModel.nodeChanged(node);
                    saveConfig();
                }
            }
        };
        renameNode.addActionListener(renameAction);

        Action deleteAction = new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                TreePath[] selectedPaths = jmeterTree.getSelectionPaths();
                if (selectedPaths == null || selectedPaths.length == 0) return;

                List<DefaultMutableTreeNode> nodesToDelete = new ArrayList<>();
                for (TreePath path : selectedPaths) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    Object userObj = node.getUserObject();
                    if (userObj instanceof JMeterTreeNode jtNode
                            && jtNode.type != NodeType.ROOT
                            && jtNode.type != NodeType.SSE_CONNECT
                            && jtNode.type != NodeType.SSE_AWAIT
                            && jtNode.type != NodeType.WS_CONNECT) {
                        nodesToDelete.add(node);
                    }
                }

                if (nodesToDelete.isEmpty()) return;

                for (DefaultMutableTreeNode node : nodesToDelete) {
                    if (node == currentRequestNode) {
                        currentRequestNode = null;
                    }
                    treeModel.removeNodeFromParent(node);
                }
                saveConfig();
            }
        };
        deleteNode.addActionListener(deleteAction);

        InputMap treeInputMap = jmeterTree.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap treeActionMap = jmeterTree.getActionMap();
        treeInputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0), "renamePerformanceNode");
        treeActionMap.put("renamePerformanceNode", renameAction);
        treeInputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0), "deletePerformanceNode");
        treeActionMap.put("deletePerformanceNode", deleteAction);

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

        jmeterTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int row = jmeterTree.getClosestRowForLocation(e.getX(), e.getY());
                    if (row < 0) return;

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

                    if (currentRequestNode != null) {
                        saveRequestNodeData(currentRequestNode);
                    }

                    TreePath[] selectedPaths = jmeterTree.getSelectionPaths();
                    if (selectedPaths == null || selectedPaths.length == 0) return;
                    boolean isMultiSelection = selectedPaths.length > 1;

                    if (isMultiSelection) {
                        addThreadGroup.setVisible(false);
                        addRequest.setVisible(false);
                        addWsSend.setVisible(false);
                        addWsAwait.setVisible(false);
                        addWsClose.setVisible(false);
                        addAssertion.setVisible(false);
                        addTimer.setVisible(false);
                        renameNode.setVisible(false);
                        deleteNode.setVisible(true);

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
                        updateMenuSeparators.run();
                    } else {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
                        Object userObj = node.getUserObject();
                        if (!(userObj instanceof JMeterTreeNode jtNode)) return;

                        if (jtNode.type == NodeType.ROOT) {
                            addThreadGroup.setVisible(true);
                            addRequest.setVisible(false);
                            addWsSend.setVisible(false);
                            addWsAwait.setVisible(false);
                            addWsClose.setVisible(false);
                            addAssertion.setVisible(false);
                            addTimer.setVisible(false);
                            renameNode.setVisible(false);
                            deleteNode.setVisible(false);
                            enableNode.setVisible(false);
                            disableNode.setVisible(false);
                            updateMenuSeparators.run();
                            treeMenu.show(jmeterTree, e.getX(), e.getY());
                            return;
                        }
                        addThreadGroup.setVisible(false);
                        addRequest.setVisible(jtNode.type == NodeType.THREAD_GROUP);
                        boolean isSseRequestNode = jtNode.type == NodeType.REQUEST && isSsePerfRequest(jtNode.httpRequestItem);
                        boolean isWebSocketRequestNode = jtNode.type == NodeType.REQUEST && isWebSocketPerfRequest(jtNode.httpRequestItem);
                        boolean canManageWsSteps = resolveWebSocketStepParent(node) != null;
                        addWsSend.setVisible(canManageWsSteps);
                        addWsAwait.setVisible(canManageWsSteps);
                        addWsClose.setVisible(canManageWsSteps);
                        addAssertion.setVisible((jtNode.type == NodeType.REQUEST && !isSseRequestNode && !isWebSocketRequestNode)
                                || jtNode.type == NodeType.SSE_AWAIT
                                || jtNode.type == NodeType.WS_AWAIT);
                        addTimer.setVisible(jtNode.type == NodeType.REQUEST || canManageWsSteps);
                        boolean structuralNode = jtNode.type == NodeType.SSE_CONNECT
                                || jtNode.type == NodeType.SSE_AWAIT
                                || jtNode.type == NodeType.WS_CONNECT;
                        renameNode.setVisible(!structuralNode && !isWebSocketStepNode(jtNode.type));
                        deleteNode.setVisible(!structuralNode);
                        enableNode.setVisible(!structuralNode && !jtNode.enabled);
                        disableNode.setVisible(!structuralNode && jtNode.enabled);
                        updateMenuSeparators.run();
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
        cancelAllNetworkCalls();

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
     * 取消所有正在执行的 HTTP / SSE / WebSocket 请求
     * HTTP 通过 Dispatcher 中的 Call 中断，SSE 通过 EventSource.cancel() 中断，WebSocket 通过 close/cancel 中断。
     */
    private void cancelAllNetworkCalls() {
        OkHttpClientManager.cancelAllCalls();
        for (EventSource eventSource : new ArrayList<>(activeSseSources)) {
            try {
                eventSource.cancel();
            } catch (Exception e) {
                log.debug("取消 SSE EventSource 失败", e);
            }
        }
        activeSseSources.clear();
        for (WebSocket webSocket : new ArrayList<>(activeWebSockets)) {
            try {
                webSocket.close(1000, "Performance stopped");
            } catch (Exception ignored) {
            }
            try {
                webSocket.cancel();
            } catch (Exception e) {
                log.debug("取消 WebSocket 失败", e);
            }
        }
        activeWebSockets.clear();
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
     * 同步最新的 HttpRequestItem 到性能测试树中对应的节点（由 Collections 保存时调用）
     * 避免用户在 editSubPanel 修改并保存后，PerformancePanel 仍持有旧数据。
     *
     * @param item 已保存的最新请求数据
     */
    public void syncRequestItem(HttpRequestItem item) {
        if (item == null || item.getId() == null) return;
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        syncRequestItemInTree(root, item);
    }

    private void syncRequestItemInTree(DefaultMutableTreeNode node, HttpRequestItem item) {
        Object userObj = node.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode
                && jtNode.type == NodeType.REQUEST
                && jtNode.httpRequestItem != null
                && item.getId().equals(jtNode.httpRequestItem.getId())) {
            jtNode.httpRequestItem = item;
            syncRequestStructure(node, jtNode);
            jtNode.name = item.getName();
            treeModel.nodeChanged(node);
            // 如果当前正在编辑面板里展示的就是这个请求，也同步更新面板
            if (node == currentRequestNode) {
                switchRequestEditor(item);
            }
            log.debug("PerformancePanel syncRequestItem: id={}, name={}", item.getId(), item.getName());
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            syncRequestItemInTree((DefaultMutableTreeNode) node.getChildAt(i), item);
        }
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
        log.debug("[refreshFromCollections] 开始执行全局刷新");
        saveAllPropertyPanelData();
        log.debug("[refreshFromCollections] saveAllPropertyPanelData 执行完毕");

        // 清除缓存的测试结果数据，避免显示过时的结果
        performanceResultTablePanel.clearResults();
        performanceReportPanel.clearReport();
        performanceTrendPanel.clearTrendDataset();
        apiCostMap.clear();
        apiSuccessMap.clear();
        apiFailMap.clear();
        allRequestResults.clear();
        ApiMetadata.clear(); // 清理API元数据
        virtualUserCounter.set(0);

        // 主动触发GC，及时释放清除的缓存数据占用的内存
        System.gc();

        int updatedCount = 0;
        int removedCount = 0;
        List<DefaultMutableTreeNode> nodesToRemove = new ArrayList<>();

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        updatedCount = refreshTreeNode(root, nodesToRemove);
        removedCount = nodesToRemove.size();
        log.debug("[refreshFromCollections] refreshTreeNode 完毕: updatedCount={}, removedCount={}", updatedCount, removedCount);

        // 移除不存在的请求节点（从后往前删除，避免索引变化）
        for (int i = nodesToRemove.size() - 1; i >= 0; i--) {
            DefaultMutableTreeNode nodeToRemove = nodesToRemove.get(i);
            // 如果删除的是当前选中的请求节点，清空引用
            if (nodeToRemove == currentRequestNode) {
                currentRequestNode = null;
            }
            treeModel.removeNodeFromParent(nodeToRemove);
        }

        // 在 treeModel.reload() 之前，先把当前节点刷新后的数据保存到局部变量。
        // 原因：reload() 会触发 TreeSelectionListener（选中被清空），
        // listener 会把 editSub 里当前（旧）的表单数据写回节点，覆盖刚从集合取到的最新数据。
        // 使用局部变量可绕开这一覆盖，确保后续 initPanelData 拿到的是集合里的最新数据。
        HttpRequestItem refreshedCurrentItem = null;
        if (currentRequestNode != null) {
            Object curUserObj = currentRequestNode.getUserObject();
            if (curUserObj instanceof JMeterTreeNode curJmNode && curJmNode.type == NodeType.REQUEST
                    && curJmNode.httpRequestItem != null) {
                refreshedCurrentItem = curJmNode.httpRequestItem;
                log.debug("[refreshFromCollections] reload 前保存当前节点最新数据: name={}, url={}, id={}",
                        refreshedCurrentItem.getName(), refreshedCurrentItem.getUrl(), refreshedCurrentItem.getId());
            }
        } else {
            log.debug("[refreshFromCollections] currentRequestNode 为 null，无需保存当前节点数据");
        }
        log.debug("[refreshFromCollections] reload 前 editSub.id={}, editSub.url={}",
                requestEditSubPanel.getId(), requestEditSubPanel.getRequestLinePanel() != null ? "有requestLinePanel" : "null");

        // 保存当前选中节点的路径（用于重新定位）
        TreePath currentPath = null;
        if (currentRequestNode != null) {
            currentPath = new TreePath(currentRequestNode.getPath());
        }

        // 保存更新后的配置
        saveConfig();

        // 刷新树显示（注意：reload 会触发 TreeSelectionListener，可能覆盖节点数据）
        log.debug("[refreshFromCollections] 即将调用 treeModel.reload()");
        treeModel.reload();
        log.debug("[refreshFromCollections] treeModel.reload() 执行完毕，当前节点数据可能已被 listener 覆盖");
        if (currentRequestNode != null) {
            Object curObj = currentRequestNode.getUserObject();
            if (curObj instanceof JMeterTreeNode curJmNode) {
                log.debug("[refreshFromCollections] reload 后 currentRequestNode.httpRequestItem: name={}, url={}",
                        curJmNode.httpRequestItem != null ? curJmNode.httpRequestItem.getName() : "null",
                        curJmNode.httpRequestItem != null ? curJmNode.httpRequestItem.getUrl() : "null");
            }
        }

        // 展开所有节点
        for (int i = 0; i < jmeterTree.getRowCount(); i++) {
            jmeterTree.expandRow(i);
        }

        // 重新定位并刷新当前选中的请求节点
        if (currentPath != null) {
            // 尝试重新找到相同路径的节点
            TreePath newPath = findTreePathByPath(currentPath);
            log.debug("[refreshFromCollections] findTreePathByPath 结果: {}", newPath != null ? "找到" : "未找到");
            if (newPath != null) {
                jmeterTree.setSelectionPath(newPath);
                DefaultMutableTreeNode newNode = (DefaultMutableTreeNode) newPath.getLastPathComponent();
                Object userObj = newNode.getUserObject();
                if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST) {
                    currentRequestNode = newNode;
                    // 优先使用 reload() 前保存的最新集合数据刷新右侧编辑面板，
                    // 避免 listener 在 reload 过程中写回的旧表单数据污染 jtNode.httpRequestItem
                    HttpRequestItem itemToLoad = refreshedCurrentItem != null
                            ? refreshedCurrentItem : jtNode.httpRequestItem;
                    log.debug("[refreshFromCollections] 准备 initPanelData: 使用{}，name={}, url={}",
                            refreshedCurrentItem != null ? "reload前保存的数据" : "节点数据",
                            itemToLoad != null ? itemToLoad.getName() : "null",
                            itemToLoad != null ? itemToLoad.getUrl() : "null");
                    if (itemToLoad != null) {
                        // 同步到节点，保证节点与面板一致
                        jtNode.httpRequestItem = itemToLoad;
                        switchRequestEditor(itemToLoad);
                        log.debug("[refreshFromCollections] initPanelData 执行完毕，editSub.id={}", requestEditSubPanel.getId());
                    }
                } else {
                    currentRequestNode = null;
                    log.debug("[refreshFromCollections] newPath 最后节点不是 REQUEST 类型");
                }
            } else {
                // 路径找不到了（节点可能被删除），清空引用
                currentRequestNode = null;
                log.debug("[refreshFromCollections] 未找到匹配路径，currentRequestNode 已清空");
            }
        } else {
            log.debug("[refreshFromCollections] currentPath 为 null，跳过重新定位");
        }

        // 显示刷新结果
        if (removedCount > 0) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REFRESH_WARNING, removedCount));
        } else if (updatedCount > 0) {
            NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REFRESH_SUCCESS, updatedCount));
        } else {
            NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_NO_REQUEST_TO_REFRESH));
        }
        log.debug("[refreshFromCollections] 全局刷新完成");
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
                log.debug("[refreshTreeNode] 处理请求节点: name={}, requestId={}, url={}",
                        jmNode.name, requestId, jmNode.httpRequestItem.getUrl());
                // 检查 requestId 是否有效
                if (requestId == null || requestId.trim().isEmpty()) {
                    log.warn("[refreshTreeNode] 请求节点 id 为空，标记删除: name={}", jmNode.name);
                    nodesToRemove.add(treeNode);
                } else {
                    HttpRequestItem latestRequestItem = persistenceService.findRequestItemById(requestId);

                    if (latestRequestItem == null) {
                        // 请求在集合中已被删除，标记为待删除
                        log.warn("[refreshTreeNode] 集合中找不到 requestId={}，标记删除: name={}", requestId, jmNode.name);
                        nodesToRemove.add(treeNode);
                    } else {
                        log.debug("[refreshTreeNode] 集合中找到最新数据: name={}, url={} -> name={}, url={}",
                                jmNode.name, jmNode.httpRequestItem.getUrl(),
                                latestRequestItem.getName(), latestRequestItem.getUrl());
                        // 更新请求数据
                        jmNode.httpRequestItem = latestRequestItem;
                        syncRequestStructure(treeNode, jmNode);
                        jmNode.name = latestRequestItem.getName();
                        treeModel.nodeChanged(treeNode);
                        // 清除 InheritanceCache，防止执行时 PreparedRequestBuilder.build() 拿到旧缓存
                        PreparedRequestBuilder.invalidateCacheForRequest(requestId);
                        log.debug("[refreshTreeNode] 已清除 requestId={} 的 InheritanceCache", requestId);
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
