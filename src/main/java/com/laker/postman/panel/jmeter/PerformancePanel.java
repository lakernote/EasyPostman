package com.laker.postman.panel.jmeter;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.component.MemoryLabel;
import com.laker.postman.common.component.StartButton;
import com.laker.postman.common.component.StopButton;
import com.laker.postman.common.panel.BasePanel;
import com.laker.postman.common.setting.SettingManager;
import com.laker.postman.model.*;
import com.laker.postman.panel.SidebarTabPanel;
import com.laker.postman.panel.collections.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.edit.RequestEditSubPanel;
import com.laker.postman.panel.jmeter.assertion.AssertionData;
import com.laker.postman.panel.jmeter.assertion.AssertionPropertyPanel;
import com.laker.postman.panel.jmeter.component.JMeterTreeCellRenderer;
import com.laker.postman.panel.jmeter.component.TreeNodeTransferHandler;
import com.laker.postman.panel.jmeter.model.JMeterTreeNode;
import com.laker.postman.panel.jmeter.model.NodeType;
import com.laker.postman.panel.jmeter.model.ResultNodeInfo;
import com.laker.postman.panel.jmeter.result.PerformanceReportPanel;
import com.laker.postman.panel.jmeter.result.PerformanceTrendPanel;
import com.laker.postman.panel.jmeter.result.ResultTreePanel;
import com.laker.postman.panel.jmeter.threadgroup.ThreadGroupData;
import com.laker.postman.panel.jmeter.threadgroup.ThreadGroupPropertyPanel;
import com.laker.postman.panel.jmeter.timer.TimerPropertyPanel;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.service.js.JsScriptExecutor;
import com.laker.postman.util.JsonPathUtil;
import lombok.extern.slf4j.Slf4j;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 左侧多层级树（用户组-请求-断言-定时器），右侧属性区，底部Tab结果区
 */
@Slf4j
public class PerformancePanel extends BasePanel {
    private JTree jmeterTree;
    private DefaultTreeModel treeModel;
    private JPanel propertyPanel; // 右侧属性区（CardLayout）
    private CardLayout propertyCardLayout;
    private JTabbedPane resultTabbedPane; // 结果Tab
    private ThreadGroupPropertyPanel threadGroupPanel;
    private AssertionPropertyPanel assertionPanel;
    private TimerPropertyPanel timerPanel;
    private RequestEditSubPanel requestEditSubPanel;
    private boolean running = false;
    private Thread runThread;
    private StartButton runBtn;
    private StopButton stopBtn;
    // 测试执行开始时间
    private long startTime;
    // 记录所有请求的开始和结束时间
    private final List<Long> allRequestStartTimes = Collections.synchronizedList(new ArrayList<>());

    // 用于统计每个请求的结束时间和成功状态
    private static class RequestResult {
        long endTime;
        boolean success;

        public RequestResult(long endTime, boolean success) {
            this.endTime = endTime;
            this.success = success;
        }
    }

    private final List<RequestResult> allRequestResults = Collections.synchronizedList(new ArrayList<>());
    private DefaultTableModel reportTableModel;
    // 按接口统计
    private final Map<String, List<Long>> apiCostMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> apiSuccessMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> apiFailMap = new ConcurrentHashMap<>();
    // 趋势图相关
    private TimeSeries userCountSeries;
    private TimeSeries responseTimeSeries;
    private TimeSeries qpsSeries;
    private TimeSeries errorPercentSeries;
    // 活跃线程计数器
    private final AtomicInteger activeThreads = new AtomicInteger(0);

    // 定时采样线程
    private Timer trendTimer;

    // 高效模式
    private boolean efficientMode = true;

    private ResultTreePanel resultTreePanel;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));

        // 1. 左侧树结构
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("Test Plan", NodeType.ROOT));
        createDefaultRequest(root);
        treeModel = new DefaultTreeModel(root);
        jmeterTree = new JTree(treeModel);
        jmeterTree.setRootVisible(true);
        jmeterTree.setShowsRootHandles(true);
        jmeterTree.setCellRenderer(new JMeterTreeCellRenderer());
        // 只允许单节点拖拽
        jmeterTree.setDragEnabled(true);
        jmeterTree.setDropMode(DropMode.ON_OR_INSERT);
        jmeterTree.setTransferHandler(new TreeNodeTransferHandler(jmeterTree, treeModel));
        JScrollPane treeScroll = new JScrollPane(jmeterTree);
        treeScroll.setPreferredSize(new Dimension(260, 500));

        // 2. 右侧属性区（CardLayout）
        propertyCardLayout = new CardLayout();
        propertyPanel = new JPanel(propertyCardLayout);
        propertyPanel.add(new JLabel("Please select a node on the left to edit"), "empty");
        threadGroupPanel = new ThreadGroupPropertyPanel();
        propertyPanel.add(threadGroupPanel, "threadGroup");
        requestEditSubPanel = new RequestEditSubPanel("");
        propertyPanel.add(requestEditSubPanel, "request");
        assertionPanel = new AssertionPropertyPanel();
        propertyPanel.add(assertionPanel, "assertion");
        timerPanel = new TimerPropertyPanel();
        propertyPanel.add(timerPanel, "timer");
        propertyCardLayout.show(propertyPanel, "empty");

        // 3. 结果树面板
        resultTreePanel = new ResultTreePanel();

        // 趋势图面板
        PerformanceTrendPanel performanceTrendPanel = new PerformanceTrendPanel();
        userCountSeries = performanceTrendPanel.getUserCountSeries();
        responseTimeSeries = performanceTrendPanel.getResponseTimeSeries();
        qpsSeries = performanceTrendPanel.getQpsSeries();
        errorPercentSeries = performanceTrendPanel.getErrorPercentSeries();
        // 报告面板
        PerformanceReportPanel performanceReportPanel = new PerformanceReportPanel();
        reportTableModel = performanceReportPanel.getReportTableModel();

        resultTabbedPane = new JTabbedPane();
        resultTabbedPane.addTab("Trend", performanceTrendPanel);
        resultTabbedPane.addTab("Report", performanceReportPanel);
        resultTabbedPane.addTab("Result Tree", resultTreePanel);

        // 主分割（左树-右属性）
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, propertyPanel);
        mainSplit.setDividerLocation(260);
        mainSplit.setDividerSize(6);
        mainSplit.setContinuousLayout(true);

        // 下部分（主分割+结果Tab）
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainSplit, resultTabbedPane);
        verticalSplit.setDividerLocation(0.62);
        verticalSplit.setDividerSize(6);
        verticalSplit.setContinuousLayout(true);

        add(verticalSplit, BorderLayout.CENTER);

        // 只允许单节点选择和拖拽
        jmeterTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // 保存/加载用例按钮 ==========
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        runBtn = new StartButton();
        stopBtn = new StopButton();
        stopBtn.setEnabled(false);
        btnPanel.add(runBtn);
        btnPanel.add(stopBtn);
        // 高效模式checkbox和问号提示
        JCheckBox efficientCheckBox = new JCheckBox("Efficient Mode");
        efficientCheckBox.setSelected(true); // 默认开启高效模式
        efficientCheckBox.setToolTipText("Only record error results to reduce memory usage");
        efficientCheckBox.addActionListener(e -> efficientMode = efficientCheckBox.isSelected());
        btnPanel.add(efficientCheckBox);
        JLabel efficientHelp = new JLabel(new FlatSVGIcon("icons/help.svg", 16, 16));
        efficientHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        efficientHelp.setToolTipText("Efficient Mode Help");
        efficientHelp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(PerformancePanel.this,
                        "高效模式：\n只记录断言失败或请求异常的结果，极大减少内存占用。适合高并发/大循环压测。\n可扩展更多性能相关配置。",
                        "高效模式说明", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        btnPanel.add(efficientHelp);
        topPanel.add(btnPanel, BorderLayout.WEST);
        // ========== 执行进度指示器 ==========
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        JLabel progressLabel = new JLabel();
        progressLabel.setText("0/0");
        progressLabel.setFont(progressLabel.getFont().deriveFont(Font.BOLD)); // 设置粗体
        progressLabel.setIcon(new FlatSVGIcon("icons/users.svg", 20, 20)); // 使用FlatLaf SVG图标
        progressLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        progressPanel.setToolTipText("active threads / total threads");
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
                propertyCardLayout.show(propertyPanel, "threadGroup");
                threadGroupPanel.setThreadGroupData(jtNode);
            }
        }
    }

    private static void createDefaultRequest(DefaultMutableTreeNode root) {
        // 默认添加一个用户组和一个请求（www.baidu.com）
        DefaultMutableTreeNode group = new DefaultMutableTreeNode(new JMeterTreeNode("Thread Group", NodeType.THREAD_GROUP));
        HttpRequestItem defaultReq = new HttpRequestItem();
        defaultReq.setName("Baidu Home Page");
        defaultReq.setMethod("GET");
        defaultReq.setUrl("https://www.baidu.com");
        DefaultMutableTreeNode req = new DefaultMutableTreeNode(new JMeterTreeNode(defaultReq.getName(), NodeType.REQUEST, defaultReq));

        // 添加默认断言
        DefaultMutableTreeNode assertionNode = new DefaultMutableTreeNode(new JMeterTreeNode("Assertion", NodeType.ASSERTION));
        req.add(assertionNode);

        // 添加默认定时器
        DefaultMutableTreeNode timerNode = new DefaultMutableTreeNode(new JMeterTreeNode("Timer", NodeType.TIMER));
        req.add(timerNode);

        group.add(req);
        root.add(group);
    }

    private void saveAllPropertyPanelData() {
        // 保存所有属性区数据到树节点
        threadGroupPanel.saveThreadGroupData();
        assertionPanel.saveAssertionData();
        timerPanel.saveTimerData();
        if (requestEditSubPanel != null) {
            // 保存RequestEditSubPanel表单到当前选中节点
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
            if (node != null) {
                Object userObj = node.getUserObject();
                if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST) {
                    jtNode.httpRequestItem = requestEditSubPanel.getCurrentRequest();
                }
            }
        }
    }

    // ========== 执行与停止核心逻辑 ==========
    private void startRun(JLabel progressLabel) {
        saveAllPropertyPanelData();
        OkHttpClientManager.setConnectionPoolConfig(getJmeterMaxIdleConnections(), getJmeterKeepAliveSeconds());
        if (running) return;
        running = true;
        runBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        resultTabbedPane.setSelectedIndex(0); // 切换到趋势图Tab
        resultTreePanel.clearResults(); // 清空结果树
        reportTableModel.setRowCount(0); // 清空报表数据
        apiCostMap.clear();
        apiSuccessMap.clear();
        apiFailMap.clear();
        allRequestStartTimes.clear();
        allRequestResults.clear();
        // 清理趋势图历史数据
        if (userCountSeries != null) userCountSeries.clear();
        if (responseTimeSeries != null) responseTimeSeries.clear();
        if (qpsSeries != null) qpsSeries.clear();
        if (errorPercentSeries != null) errorPercentSeries.clear();
        // 启动趋势图定时采样
        if (trendTimer != null) {
            trendTimer.cancel();
        }
        trendTimer = new Timer();
        trendTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> sampleTrendData());
            }
        }, 0, 1000);

        // 重要：更新开始时间，确保递增线程等模式正常工作
        startTime = System.currentTimeMillis();

        // 统计总用户数
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();
        int totalThreads = getTotalThreads(rootNode);
        // 当前已启动线程数 = 0，启动后动态刷新
        progressLabel.setText(0 + "/" + totalThreads);
        runThread = new Thread(() -> {
            try {
                runJMeterTreeWithProgress(rootNode, progressLabel, totalThreads);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    running = false;
                    runBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    stopTrendTimer();
                    OkHttpClientManager.setDefaultConnectionPoolConfig();
                    updateReportPanel();
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

    // 停止定时采样方法
    private void stopTrendTimer() {
        if (trendTimer != null) {
            trendTimer.cancel();
            trendTimer = null;
        }
    }

    // 每秒采样统计方法
    private void sampleTrendData() {
        int users = activeThreads.get();
        long now = System.currentTimeMillis();
        Second second = new Second(new Date(now));
        // 统计本秒内的请求
        int totalReq = 0, errorReq = 0;
        long totalRespTime = 0;
        synchronized (allRequestResults) {
            for (int i = allRequestResults.size() - 1; i >= 0; i--) {
                RequestResult result = allRequestResults.get(i);
                if (result.endTime >= now - 1000 && result.endTime <= now) {
                    totalReq++;
                    if (!result.success) errorReq++;
                } else if (result.endTime < now - 1000) {
                    break;
                }
            }
        }
        // 统计平均响应时间
        synchronized (allRequestStartTimes) {
            for (int i = allRequestStartTimes.size() - 1; i >= 0; i--) {
                long start = allRequestStartTimes.get(i);
                if (start >= now - 1000 && start <= now) {
                    totalRespTime += (now - start);
                } else if (start < now - 1000) {
                    break;
                }
            }
        }
        double avgRespTime = totalReq > 0 ? (double) totalRespTime / totalReq : 0;
        double qps = totalReq;
        double errorPercent = totalReq > 0 ? (double) errorReq / totalReq * 100 : 0;
        // 更新趋势图数据
        log.info("采样数据 {} - 用户数: {}, 平均响应时间: {} ms, QPS: {}, 错误率: {}%", second, users, avgRespTime, qps, errorPercent);
        userCountSeries.addOrUpdate(second, users);
        responseTimeSeries.addOrUpdate(second, avgRespTime);
        qpsSeries.addOrUpdate(second, qps);
        errorPercentSeries.addOrUpdate(second, errorPercent);
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
                    Thread t = new Thread(() -> runJMeterTreeWithProgress(tgNode, progressLabel, totalThreads));
                    tgThreads.add(t);
                    t.start();
                }
                for (Thread t : tgThreads) {
                    try {
                        t.join();
                    } catch (InterruptedException ignored) {
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
        long startTime = System.currentTimeMillis();
        long endTime = useTime ? (startTime + (durationSeconds * 1000L)) : Long.MAX_VALUE;

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
            if (useTime) {
                executor.awaitTermination(durationSeconds + 10, TimeUnit.SECONDS);
            } else {
                executor.awaitTermination(1, TimeUnit.HOURS);
            }
        } catch (InterruptedException exception) {
            JOptionPane.showMessageDialog(this, "执行被中断: " + exception.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
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
            scheduler.awaitTermination(totalDuration + 10, TimeUnit.SECONDS);
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
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
            scheduler.awaitTermination(totalTime + 10, TimeUnit.SECONDS);
            // 确保等待所有threadEndTimes中的线程完成
            for (Thread t : threadEndTimes.keySet()) {
                try {
                    if (t.isAlive()) {
                        t.join(10000); // 设置超时时间避免永久阻塞
                    }
                } catch (InterruptedException ie) {
                    log.error("等待线程完成时中断", ie);
                }
            }
        } catch (InterruptedException e) {
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
            scheduler.awaitTermination(totalTime + 10, TimeUnit.SECONDS);

            // 确保等待所有threadEndTimes中的线程完成
            for (Thread t : threadEndTimes.keySet()) {
                try {
                    if (t.isAlive()) {
                        t.join(10000); // 设置超时时间避免永久阻塞
                    }
                } catch (InterruptedException ie) {
                    log.error("等待线程完成时中断", ie);
                }
            }
        } catch (InterruptedException e) {
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
                long rampDownStartTime = (rampUpTime + holdTime) * totalSpikeTime / phaseSum;
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
            executeRequestNode(userObj, child);
        }
    }

    // 执行指定次数的请求
    private void runTask(DefaultMutableTreeNode groupNode, int loops) {
        for (int l = 0; l < loops && running; l++) {
            runTaskIteration(groupNode);
        }
    }

    // 执行单个请求节点
    private void executeRequestNode(Object userObj, DefaultMutableTreeNode child) {
        if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST && jtNode.httpRequestItem != null) {
            String apiName = jtNode.httpRequestItem.getName();
            boolean success = true;
            PreparedRequest req;
            HttpResponse resp = null;
            String errorMsg = "";
            List<TestResult> testResults = new ArrayList<>();
            // ====== 前置脚本 ======
            req = PreparedRequestBuilder.build(jtNode.httpRequestItem);
            Map<String, Object> bindings = HttpUtil.prepareBindings(req);
            Postman pm = (Postman) bindings.get("pm");
            boolean preOk = true;
            String prescript = jtNode.httpRequestItem.getPrescript();
            if (prescript != null && !prescript.isBlank()) {
                try {
                    JsScriptExecutor.executeScript(
                            prescript,
                            bindings,
                            output -> {
                                if (!output.isBlank()) {
                                    SidebarTabPanel.appendConsoleLog("[PreScript Console]\n" + output);
                                }
                            }
                    );
                } catch (Exception ex) {
                    log.error("前置脚本: {}", ex.getMessage(), ex);
                    errorMsg = "前置脚本执行失败: " + ex.getMessage();
                    preOk = false;
                    success = false;
                }
            }

            long startTime = System.currentTimeMillis();
            allRequestStartTimes.add(startTime); // 记录开始时间
            long costMs = 0;

            if (preOk) {
                try {
                    req.logEvent = true; // 记录事件日志
                    resp = HttpSingleRequestExecutor.execute(req);
                } catch (Exception ex) {
                    log.error("请求执行失败: {}", ex.getMessage(), ex);
                    errorMsg = "请求执行失败: " + ex.getMessage();
                    success = false;
                } finally {
                    costMs = System.currentTimeMillis() - startTime;
                }
                // 断言处理（JMeter树断言）
                for (int j = 0; j < child.getChildCount() && resp != null; j++) {
                    DefaultMutableTreeNode sub = (DefaultMutableTreeNode) child.getChildAt(j);
                    Object subObj = sub.getUserObject();
                    if (subObj instanceof JMeterTreeNode subNode && subNode.type == NodeType.ASSERTION && subNode.assertionData != null) {
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
                            errorMsg = "断言失败: " + type + " - " + assertion.content;
                        }
                        testResults.add(new TestResult(type, pass, pass ? null : "断言失败"));
                    }
                    if (subObj instanceof JMeterTreeNode subNode2 && subNode2.type == NodeType.TIMER && subNode2.timerData != null) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(subNode2.timerData.delayMs);
                        } catch (InterruptedException ignored) {
                            return;
                        }
                    }
                }
                // ====== 后置脚本 ======
                String postscript = jtNode.httpRequestItem.getPostscript();
                if (resp != null && postscript != null && !postscript.isBlank()) {
                    HttpUtil.postBindings(bindings, resp);
                    try {
                        JsScriptExecutor.executeScript(
                                postscript,
                                bindings,
                                output -> {
                                    if (!output.isBlank()) {
                                        SidebarTabPanel.appendConsoleLog("[PostScript Console]\n" + output);
                                    }
                                }
                        );
                        if (pm.testResults != null) {
                            testResults.addAll(pm.testResults);
                        }
                    } catch (Exception assertionEx) {
                        log.error("后置脚本执行失败: {}", assertionEx.getMessage(), assertionEx);
                        if (pm.testResults != null) {
                            testResults.addAll(pm.testResults);
                        }
                        errorMsg = assertionEx.getMessage();
                        success = false;
                    }
                }
            } else {
                // 前置脚本失败的情况，也需要记录costMs
                costMs = System.currentTimeMillis() - startTime;
            }

            long cost = resp == null ? costMs : resp.costMs;
            allRequestResults.add(new RequestResult(startTime + cost, success)); // 记录结束时间
            // 统计接口耗时（统一用resp.costMs）
            apiCostMap.computeIfAbsent(apiName, k -> Collections.synchronizedList(new ArrayList<>())).add(cost);

            if (success) {
                apiSuccessMap.merge(apiName, 1, Integer::sum);
            } else {
                apiFailMap.merge(apiName, 1, Integer::sum);
            }

            // 高效模式下只保存失败或异常结果
            resultTreePanel.addResult(new ResultNodeInfo(jtNode.httpRequestItem.getName(), success, errorMsg, req, resp, testResults), efficientMode);
        }
    }

    @Override
    protected void registerListeners() {
        // 节点选中切换属性区
        jmeterTree.addTreeSelectionListener(new TreeSelectionListener() {
            private DefaultMutableTreeNode lastNode = null;

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                // 保存上一个节点的数据
                if (lastNode != null) {
                    Object userObj = lastNode.getUserObject();
                    if (userObj instanceof JMeterTreeNode jtNode) {
                        switch (jtNode.type) {
                            case THREAD_GROUP -> threadGroupPanel.saveThreadGroupData();
                            case REQUEST -> {
                                // 保存RequestEditSubPanel表单到jtNode
                                jtNode.httpRequestItem = requestEditSubPanel.getCurrentRequest();
                            }
                            case ASSERTION -> assertionPanel.saveAssertionData();
                            case TIMER -> timerPanel.saveTimerData();
                        }
                    }
                }
                // 回填当前节点数据
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
                if (node == null) {
                    propertyCardLayout.show(propertyPanel, "empty");
                    lastNode = null;
                    return;
                }
                Object userObj = node.getUserObject();
                if (!(userObj instanceof JMeterTreeNode jtNode)) {
                    propertyCardLayout.show(propertyPanel, "empty");
                    lastNode = node;
                    return;
                }
                switch (jtNode.type) {
                    case THREAD_GROUP -> {
                        propertyCardLayout.show(propertyPanel, "threadGroup");
                        threadGroupPanel.setThreadGroupData(jtNode);
                    }
                    case REQUEST -> {
                        propertyCardLayout.show(propertyPanel, "request");
                        if (jtNode.httpRequestItem != null) {
                            requestEditSubPanel.updateRequestForm(jtNode.httpRequestItem);
                        }
                    }
                    case ASSERTION -> {
                        propertyCardLayout.show(propertyPanel, "assertion");
                        assertionPanel.setAssertionData(jtNode);
                    }
                    case TIMER -> {
                        propertyCardLayout.show(propertyPanel, "timer");
                        timerPanel.setTimerData(jtNode);
                    }
                    default -> propertyCardLayout.show(propertyPanel, "empty");
                }
                lastNode = node;
            }
        });

        // 右键菜单
        JPopupMenu treeMenu = new JPopupMenu();
        JMenuItem addThreadGroup = new JMenuItem("Add Thread Group");
        JMenuItem addRequest = new JMenuItem("Add Request");
        JMenuItem addAssertion = new JMenuItem("Add Assertion");
        JMenuItem addTimer = new JMenuItem("Add Timer");
        JMenuItem renameNode = new JMenuItem("Rename");
        JMenuItem deleteNode = new JMenuItem("Delete");
        treeMenu.add(addThreadGroup);
        treeMenu.add(addRequest);
        treeMenu.add(addAssertion);
        treeMenu.add(addTimer);
        treeMenu.addSeparator();
        treeMenu.add(renameNode);
        treeMenu.add(deleteNode);

        // 添加用户组（仅根节点可添加）
        addThreadGroup.addActionListener(e -> {
            DefaultMutableTreeNode root1 = (DefaultMutableTreeNode) treeModel.getRoot();
            DefaultMutableTreeNode group = new DefaultMutableTreeNode(new JMeterTreeNode("Thread Group", NodeType.THREAD_GROUP));
            treeModel.insertNodeInto(group, root1, root1.getChildCount());
            jmeterTree.expandPath(new TreePath(root1.getPath()));
        });
        // 添加请求
        addRequest.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
            if (node == null) return;
            Object userObj = node.getUserObject();
            if (!(userObj instanceof JMeterTreeNode jtNode) || jtNode.type != NodeType.THREAD_GROUP) {
                JOptionPane.showMessageDialog(PerformancePanel.this, "Please select a thread group node to add", "Info", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // 多选请求弹窗
            RequestCollectionsLeftPanel.showMultiSelectRequestDialog(selectedList -> {
                if (selectedList == null || selectedList.isEmpty()) return;
                List<DefaultMutableTreeNode> newNodes = new ArrayList<>();
                for (com.laker.postman.model.HttpRequestItem reqItem : selectedList) {
                    DefaultMutableTreeNode req = new DefaultMutableTreeNode(new JMeterTreeNode(reqItem.getName(), NodeType.REQUEST, reqItem));
                    treeModel.insertNodeInto(req, node, node.getChildCount());
                    newNodes.add(req);
                }
                jmeterTree.expandPath(new TreePath(node.getPath()));
                // 选中第一个新加的请求节点
                TreePath newPath = new TreePath(newNodes.get(0).getPath());
                jmeterTree.setSelectionPath(newPath);
                propertyCardLayout.show(propertyPanel, "request");
                requestEditSubPanel.updateRequestForm(((JMeterTreeNode) newNodes.get(0).getUserObject()).httpRequestItem);
            });
        });
        // 添加断言
        addAssertion.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
            if (node == null) return;
            DefaultMutableTreeNode assertion = new DefaultMutableTreeNode(new JMeterTreeNode("Assertion", NodeType.ASSERTION));
            treeModel.insertNodeInto(assertion, node, node.getChildCount());
            jmeterTree.expandPath(new TreePath(node.getPath()));
        });
        // 添加定时器
        addTimer.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
            if (node == null) return;
            DefaultMutableTreeNode timer = new DefaultMutableTreeNode(new JMeterTreeNode("Timer", NodeType.TIMER));
            treeModel.insertNodeInto(timer, node, node.getChildCount());
            jmeterTree.expandPath(new TreePath(node.getPath()));
        });
        // 重命名
        renameNode.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
            if (node == null) return;
            Object userObj = node.getUserObject();
            if (!(userObj instanceof JMeterTreeNode jtNode)) return;
            if (jtNode.type == NodeType.ROOT) return;
            String oldName = jtNode.name;
            String newName = JOptionPane.showInputDialog(PerformancePanel.this, "Rename node:", oldName);
            if (newName != null && !newName.trim().isEmpty()) {
                jtNode.name = newName.trim();
                // 同步更新 request 类型的 httpRequestItem name 字段
                if (jtNode.type == NodeType.REQUEST && jtNode.httpRequestItem != null) {
                    jtNode.httpRequestItem.setName(newName.trim());
                    requestEditSubPanel.updateRequestForm(jtNode.httpRequestItem);
                }
                treeModel.nodeChanged(node);
            }
        });
        // 删除
        deleteNode.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
            if (node == null) return;
            Object userObj = node.getUserObject();
            if (!(userObj instanceof JMeterTreeNode jtNode)) return;
            if (jtNode.type == NodeType.ROOT) return;
            treeModel.removeNodeFromParent(node);
        });

        // 右键弹出逻辑
        jmeterTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int row = jmeterTree.getClosestRowForLocation(e.getX(), e.getY());
                    if (row < 0) return;
                    jmeterTree.setSelectionRow(row);
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
                    if (node == null) return;
                    Object userObj = node.getUserObject();
                    if (!(userObj instanceof JMeterTreeNode jtNode)) return;
                    if (jtNode.type == NodeType.ROOT) {
                        addThreadGroup.setVisible(true);
                        addRequest.setVisible(false);
                        addAssertion.setVisible(false);
                        addTimer.setVisible(false);
                        renameNode.setVisible(false);
                        deleteNode.setVisible(false);
                        treeMenu.show(jmeterTree, e.getX(), e.getY());
                        return;
                    }
                    addThreadGroup.setVisible(false);
                    addRequest.setVisible(jtNode.type == NodeType.THREAD_GROUP);
                    addAssertion.setVisible(jtNode.type == NodeType.REQUEST);
                    addTimer.setVisible(jtNode.type == NodeType.REQUEST);
                    renameNode.setVisible(jtNode.type != NodeType.ROOT);
                    deleteNode.setVisible(jtNode.type != NodeType.ROOT);
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
        runBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        OkHttpClientManager.setDefaultConnectionPoolConfig();
        // 停止趋势图定时采样
        stopTrendTimer();
    }

    private void updateReportPanel() {
        // 表格统计
        reportTableModel.setRowCount(0); // 清空表格
        int totalApi = 0, totalSuccess = 0, totalFail = 0;
        long totalCost = 0, totalMin = Long.MAX_VALUE, totalMax = 0, totalP99 = 0;
        double totalRate = 0;
        int apiCount = 0;
        for (String api : apiCostMap.keySet()) {
            List<Long> costs = apiCostMap.get(api);
            int apiTotal = costs.size();
            int apiSuccess = apiSuccessMap.getOrDefault(api, 0);
            int apiFail = apiFailMap.getOrDefault(api, 0);
            long apiAvg = apiTotal > 0 ? costs.stream().mapToLong(Long::longValue).sum() / apiTotal : 0;
            long apiMin = costs.stream().mapToLong(Long::longValue).min().orElse(0);
            long apiMax = costs.stream().mapToLong(Long::longValue).max().orElse(0);
            long apiP99 = getP99(costs);
            long apiTotalCost = costs.stream().mapToLong(Long::longValue).sum();
            double apiQps = 0;
            if (!allRequestStartTimes.isEmpty() && !allRequestResults.isEmpty()) {
                long minStart = Collections.min(allRequestStartTimes);
                long maxEnd = Collections.max(allRequestResults.stream().map(result -> result.endTime).toList());
                long spanMs = Math.max(1, maxEnd - minStart);
                apiQps = apiTotal * 1000.0 / spanMs;
            }
            double apiRate = apiTotal > 0 ? (apiSuccess * 100.0 / apiTotal) : 0;
            reportTableModel.addRow(new Object[]{api, apiTotal, apiSuccess, apiFail, String.format("%.2f", apiQps), apiAvg, apiMin, apiMax, apiP99, apiTotalCost, String.format("%.2f%%", apiRate)});
            // 累加total
            totalApi += apiTotal;
            totalSuccess += apiSuccess;
            totalFail += apiFail;
            totalCost += apiTotalCost;
            totalMin = Math.min(totalMin, apiMin);
            totalMax = Math.max(totalMax, apiMax);
            totalP99 += apiP99;
            totalRate += apiRate;
            apiCount++;
        }
        // 添加total行
        if (apiCount > 0) {
            long avgP99 = totalP99 / apiCount;
            double avgRate = totalRate / apiCount;
            double totalQps = 0;
            if (!allRequestStartTimes.isEmpty() && !allRequestResults.isEmpty()) {
                long minStart = Collections.min(allRequestStartTimes);
                long maxEnd = Collections.max(allRequestResults.stream().map(result -> result.endTime).toList());
                long spanMs = Math.max(1, maxEnd - minStart); // 防止除0
                totalQps = totalApi * 1000.0 / spanMs;
            }
            long avg = totalApi > 0 ? totalCost / totalApi : 0;
            reportTableModel.addRow(new Object[]{"Total", totalApi, totalSuccess, totalFail, String.format("%.2f", totalQps), avg, totalMin == Long.MAX_VALUE ? 0 : totalMin, totalMax, avgP99, totalCost, String.format("%.2f%%", avgRate)});
        }
    }

    private long getP99(List<Long> costs) {
        if (costs == null || costs.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(costs);
        Collections.sort(sorted);
        int idx = (int) Math.ceil(sorted.size() * 0.99) - 1;
        return sorted.get(Math.max(idx, 0));
    }

    private static int getJmeterMaxIdleConnections() {
        return SettingManager.getJmeterMaxIdleConnections();
    }

    private static long getJmeterKeepAliveSeconds() {
        return SettingManager.getJmeterKeepAliveSeconds();
    }
}
