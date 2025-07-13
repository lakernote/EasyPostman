package com.laker.postman.panel.jmeter;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.constants.Colors;
import com.laker.postman.common.panel.BasePanel;
import com.laker.postman.common.setting.SettingManager;
import com.laker.postman.model.*;
import com.laker.postman.panel.SidebarTabPanel;
import com.laker.postman.panel.collections.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.edit.RequestEditSubPanel;
import com.laker.postman.panel.history.HistoryHtmlBuilder;
import com.laker.postman.panel.runner.RunnerHtmlUtil;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.service.js.JsScriptExecutor;
import com.laker.postman.util.FontUtil;
import com.laker.postman.util.JsonPathUtil;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * 左侧多层级树（用户组-请求-断言-定时器），右侧属性区，底部Tab结果区
 */
@Slf4j
public class JMeterPanel extends BasePanel {
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
    private JButton runBtn;
    private JButton stopBtn;
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
    private TimeSeriesCollection trendDataset;
    private TimeSeries userCountSeries;
    private TimeSeries responseTimeSeries;
    private TimeSeries qpsSeries;
    private TimeSeries errorPercentSeries;


    private static final int JMETER_MAX_IDLE_CONNECTIONS = 200;
    private static final long JMETER_KEEP_ALIVE_DURATION = 60L;

    // 搜索框相关
    private JTextField searchField;
    private JLabel elapsedLabel; // 显示已用时

    // 活跃线程计数器
    private final AtomicInteger activeThreads = new AtomicInteger(0);

    // 定时采样线程
    private Timer trendTimer;

    // 高效模式
    private boolean efficientMode = false;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));

        // 1. 左侧树结构
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("测试计划", NodeType.ROOT));
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
        propertyPanel.add(new JLabel("请选择左侧节点进行编辑"), "empty");
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
        // ========== 结果树搜索框 ==========
        JPanel resultTreePanel = new JPanel(new BorderLayout());
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchField = new SearchTextField();
        searchPanel.add(searchField, BorderLayout.CENTER);
        resultTreePanel.add(searchPanel, BorderLayout.NORTH);
        // 结果树
        resultRootNode = new DefaultMutableTreeNode("结果树");
        resultTreeModel = new DefaultTreeModel(resultRootNode);
        resultTree = new JTree(resultTreeModel);
        resultTree.setRootVisible(true);
        resultTree.setShowsRootHandles(true);
        resultTree.setCellRenderer(new ResultTreeCellRenderer());
        JScrollPane resultTreeScroll = new JScrollPane(resultTree);
        resultTreeScroll.setPreferredSize(new Dimension(320, 400));
        resultTreePanel.add(resultTreeScroll, BorderLayout.CENTER);
        // 详情tab
        resultDetailTabbedPane = new JTabbedPane();
        resultDetailTabbedPane.addTab("Request", new JScrollPane(new JEditorPane()));
        resultDetailTabbedPane.addTab("Response", new JScrollPane(new JEditorPane()));
        resultDetailTabbedPane.addTab("Tests", new JScrollPane(new JEditorPane()));
        resultDetailTabbedPane.addTab("Timing", new JScrollPane(new JEditorPane()));
        resultDetailTabbedPane.addTab("Event Info", new JScrollPane(new JEditorPane()));
        JSplitPane resultSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, resultTreePanel, resultDetailTabbedPane);
        resultSplit.setDividerLocation(260);
        resultTabbedPane = new JTabbedPane();

        // 报表面板
        // 报表面板相关
        JPanel reportPanel = new JPanel(new BorderLayout());
        String[] columns = {"接口名称", "总数", "成功", "失败", "QPS", "平均(ms)", "最小(ms)", "最大(ms)", "P99(ms)", "总耗时(ms)", "成功率"};
        reportTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable reportTable = new JTable(reportTableModel);
        reportTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        // 设置数据列居中
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        // 失败列红色渲染器（0为黑色，大于0为红色，Total行为蓝色加粗）
        DefaultTableCellRenderer failRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                boolean isTotal = "Total".equals(reportTableModel.getValueAt(modelRow, 0));
                if (isTotal) {
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                    c.setForeground(new Color(0, 102, 204));
                    c.setBackground(new Color(230, 240, 255));
                } else {
                    try {
                        int failCount = Integer.parseInt(value == null ? "0" : value.toString());
                        c.setForeground(failCount > 0 ? Color.RED : Color.BLACK);
                        c.setBackground(Color.WHITE);
                    } catch (Exception e) {
                        c.setForeground(Color.BLACK);
                        c.setBackground(Color.WHITE);
                    }
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
        // 成功率列绿色渲染器（Total行为蓝色加粗）
        DefaultTableCellRenderer rateRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                boolean isTotal = "Total".equals(reportTableModel.getValueAt(modelRow, 0));
                if (isTotal) {
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                    c.setForeground(new Color(0, 102, 204));
                    c.setBackground(new Color(230, 240, 255));
                } else {
                    String rateStr = value != null ? value.toString() : "";
                    if (rateStr.endsWith("%")) {
                        try {
                            double rate = Double.parseDouble(rateStr.replace("%", ""));
                            if (rate >= 99) {
                                c.setForeground(new Color(0, 153, 0)); // 深绿色
                            } else if (rate >= 90) {
                                c.setForeground(new Color(51, 153, 255)); // 蓝色
                            } else {
                                c.setForeground(Color.RED);
                            }
                        } catch (Exception e) {
                            c.setForeground(Color.BLACK);
                        }
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                    c.setBackground(Color.WHITE);
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
        // 通用居中渲染器（Total行美化）
        DefaultTableCellRenderer generalRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                boolean isTotal = "Total".equals(reportTableModel.getValueAt(modelRow, 0));
                if (isTotal) {
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                    c.setForeground(new Color(0, 102, 204));
                    c.setBackground(new Color(230, 240, 255));
                } else {
                    c.setForeground(Color.BLACK);
                    c.setBackground(Color.WHITE);
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
        // 需要居中的列索引
        int[] centerColumns = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        for (int col : centerColumns) {
            if (col == 3) { // 失败列
                reportTable.getColumnModel().getColumn(col).setCellRenderer(failRenderer);
            } else if (col == 10) { // 成功率列
                reportTable.getColumnModel().getColumn(col).setCellRenderer(rateRenderer);
            } else {
                reportTable.getColumnModel().getColumn(col).setCellRenderer(generalRenderer);
            }
        }
        // 设置表头加粗
        reportTable.getTableHeader().setFont(reportTable.getTableHeader().getFont().deriveFont(Font.BOLD));
        JScrollPane tableScroll = new JScrollPane(reportTable);
        reportPanel.add(tableScroll, BorderLayout.CENTER);
        // 趋势图面板
        trendDataset = new TimeSeriesCollection();
        userCountSeries = new TimeSeries("用户数");
        responseTimeSeries = new TimeSeries("响应时间(ms)");
        qpsSeries = new TimeSeries("QPS");
        errorPercentSeries = new TimeSeries("错误率(%)");
        trendDataset.addSeries(userCountSeries);
        trendDataset.addSeries(responseTimeSeries);
        trendDataset.addSeries(qpsSeries);
        trendDataset.addSeries(errorPercentSeries);
        // 趋势类型下拉框
        String[] trendOptions = {"全部", "用户数", "响应时间", "QPS", "错误率"};
        JComboBox<String> trendTypeCombo = new JComboBox<>(trendOptions);
        JFreeChart trendChart = ChartFactory.createTimeSeriesChart(
                "接口性能趋势", // 图表标题
                "时间", // X轴标签
                "指标值", // Y轴标签，后续动态切换
                trendDataset,
                true, // 图例
                false,
                false
        );
        // 设置趋势图样式
        XYPlot plot = trendChart.getXYPlot();
        // 设置series颜色
        Color qps = new Color(222, 156, 1);
        Color responseTime = new Color(7, 123, 237);
        Color errorPercent = new Color(236, 38, 26);
        Color userCount = new Color(230, 230, 230);
        org.jfree.chart.renderer.xy.XYLineAndShapeRenderer renderer = (org.jfree.chart.renderer.xy.XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, userCount); // 用户数-亮蓝色
        renderer.setSeriesPaint(1, responseTime); // 响应时间-琥珀黄
        renderer.setSeriesPaint(2, qps); // QPS-草绿色
        renderer.setSeriesPaint(3, errorPercent); // 错误率-柔和红
        // 设置字体，防止中文乱码
        Font font = FontUtil.getDefaultFont(Font.PLAIN, 12);
        trendChart.getTitle().setFont(font.deriveFont(13f));
        if (trendChart.getLegend() != null) trendChart.getLegend().setItemFont(font);
        plot.getDomainAxis().setTickLabelFont(font);
        plot.getDomainAxis().setLabelFont(font);
        plot.getRangeAxis().setTickLabelFont(font);
        plot.getRangeAxis().setLabelFont(font);
        plot.setBackgroundPaint(Color.WHITE);
        trendChart.setBackgroundPaint(Color.WHITE);
        // Y轴自动调整
        plot.getRangeAxis().setAutoRange(true);
        // 下拉框切换显示series
        trendTypeCombo.addActionListener(e -> {
            String selected = (String) trendTypeCombo.getSelectedItem();
            trendDataset.removeAllSeries();
            // 动态设置Y轴格式
            NumberFormat numberFormat = null;
            switch (selected) {
                case "全部" -> {
                    trendDataset.addSeries(userCountSeries);
                    trendDataset.addSeries(responseTimeSeries);
                    trendDataset.addSeries(qpsSeries);
                    trendDataset.addSeries(errorPercentSeries);
                    plot.getRangeAxis().setLabel("指标值");
                    // 恢复所有series颜色
                    renderer.setSeriesPaint(0, userCount); // 用户数-亮蓝色
                    renderer.setSeriesPaint(1, responseTime); // 响应时间-琥珀黄
                    renderer.setSeriesPaint(2, qps); // QPS-草绿色
                    renderer.setSeriesPaint(3, errorPercent); // 错误率-柔和红
                }
                case "用户数" -> {
                    trendDataset.addSeries(userCountSeries);
                    plot.getRangeAxis().setLabel("用户数");
                    numberFormat = java.text.NumberFormat.getIntegerInstance();
                    renderer.setSeriesPaint(0, userCount); // 用户数-亮蓝色
                }
                case "响应时间" -> {
                    trendDataset.addSeries(responseTimeSeries);
                    plot.getRangeAxis().setLabel("响应时间(ms)");
                    renderer.setSeriesPaint(0, responseTime);
                }
                case "QPS" -> {
                    trendDataset.addSeries(qpsSeries);
                    plot.getRangeAxis().setLabel("QPS");
                    renderer.setSeriesPaint(0, qps); // QPS-草绿色
                }
                case "错误率" -> {
                    trendDataset.addSeries(errorPercentSeries);
                    plot.getRangeAxis().setLabel("错误率(%)");
                    numberFormat = java.text.NumberFormat.getNumberInstance();
                    numberFormat.setMaximumFractionDigits(2);
                    renderer.setSeriesPaint(0, errorPercent); // 错误率-柔和红
                }
            }
            if (plot.getRangeAxis() instanceof NumberAxis numberAxis) {
                numberAxis.setNumberFormatOverride(numberFormat);
            }
        });
        ChartPanel chartPanel = new ChartPanel(trendChart);
        chartPanel.setMouseWheelEnabled(true); // 支持鼠标滚轮缩放
        chartPanel.setBackground(Colors.PANEL_BACKGROUND);
        chartPanel.setDisplayToolTips(true); // 显示工具提示
        chartPanel.setPreferredSize(new Dimension(300, 300));
        // 趋势图面板
        JPanel trendPanel = new JPanel(new BorderLayout());
        JPanel trendTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        trendTopPanel.add(new JLabel("趋势类型:"));
        trendTopPanel.add(trendTypeCombo);
        trendPanel.add(trendTopPanel, BorderLayout.NORTH);
        trendPanel.add(chartPanel, BorderLayout.CENTER);

        resultTabbedPane.addTab("趋势图", trendPanel);
        resultTabbedPane.addTab("报表", reportPanel);
        resultTabbedPane.addTab("结果树", resultSplit);

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
        runBtn = new JButton("启动");
        stopBtn = new JButton("停止");
        stopBtn.setEnabled(false);
        JButton saveCaseBtn = new JButton("保存计划");
        JButton loadCaseBtn = new JButton("加载计划");
        btnPanel.add(runBtn);
        btnPanel.add(stopBtn);
        btnPanel.add(saveCaseBtn);
        btnPanel.add(loadCaseBtn);
        // 高效模式checkbox和问号提示
        JCheckBox efficientCheckBox = new JCheckBox("高效模式");
        efficientCheckBox.setSelected(false);
        efficientCheckBox.setToolTipText("开启后只记录错误结果，减少内存占用");
        efficientCheckBox.addActionListener(e -> efficientMode = efficientCheckBox.isSelected());
        btnPanel.add(efficientCheckBox);
        JLabel efficientHelp = new JLabel(new FlatSVGIcon("icons/help.svg", 16, 16));
        efficientHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        efficientHelp.setToolTipText("高效模式说明");
        efficientHelp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(JMeterPanel.this,
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
        progressPanel.setToolTipText("当前活跃用户数/总用户数");
        progressPanel.add(progressLabel);
        // ========== 实时耗时显示 ==========
        elapsedLabel = new JLabel("0 ms");
        elapsedLabel.setIcon(new FlatSVGIcon("icons/time.svg", 20, 20));
        elapsedLabel.setFont(progressLabel.getFont().deriveFont(Font.BOLD));
        progressPanel.add(elapsedLabel);
        // ========== 内存占用显示 ==========
        JLabel memoryLabel = new JLabel();
        memoryLabel.setFont(progressLabel.getFont().deriveFont(Font.BOLD));
        memoryLabel.setIcon(new FlatSVGIcon("icons/computer.svg", 20, 20));
        memoryLabel.setToolTipText("当前JVM内存占用，双击手动GC");
        updateMemoryLabel(memoryLabel);
        // 定时刷新内存占用
        Timer memTimer = new Timer();
        memTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> updateMemoryLabel(memoryLabel));
            }
        }, 0, 2000);
        memoryLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    System.gc();
                    updateMemoryLabel(memoryLabel);
                    JOptionPane.showMessageDialog(JMeterPanel.this, "已手动触发GC！", "提示", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        progressPanel.add(memoryLabel);

        topPanel.add(progressPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        runBtn.addActionListener(e -> startRun(progressLabel));
        stopBtn.addActionListener(e -> stopRun());
        saveCaseBtn.addActionListener(e -> saveJMeterTreeToFile());
        loadCaseBtn.addActionListener(e -> loadJMeterTreeFromFile());

        // 展开所有节点
        for (int i = 0; i < jmeterTree.getRowCount(); i++) {
            jmeterTree.expandRow(i);
        }
    }

    private static void createDefaultRequest(DefaultMutableTreeNode root) {
        // 默认添加一个用户组和一个请求（www.baidu.com）
        DefaultMutableTreeNode group = new DefaultMutableTreeNode(new JMeterTreeNode("用户组", NodeType.THREAD_GROUP));
        HttpRequestItem defaultReq = new HttpRequestItem();
        defaultReq.setName("百度首页");
        defaultReq.setMethod("GET");
        defaultReq.setUrl("https://www.baidu.com");
        DefaultMutableTreeNode req = new DefaultMutableTreeNode(new JMeterTreeNode(defaultReq.getName(), NodeType.REQUEST, defaultReq));

        // 添加默认断言
        DefaultMutableTreeNode assertionNode = new DefaultMutableTreeNode(new JMeterTreeNode("断言", NodeType.ASSERTION));
        req.add(assertionNode);

        // 添加默认定时器
        DefaultMutableTreeNode timerNode = new DefaultMutableTreeNode(new JMeterTreeNode("定时器", NodeType.TIMER));
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
        resultTabbedPane.setSelectedIndex(0);
        resultRootNode.removeAllChildren();
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
        // 统计总用户数
        int totalThreads;
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();
        totalThreads = IntStream.range(0, rootNode.getChildCount()).mapToObj(i -> (DefaultMutableTreeNode) rootNode.getChildAt(i)).map(DefaultMutableTreeNode::getUserObject).filter(userObj -> userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.THREAD_GROUP).map(userObj -> (JMeterTreeNode) userObj).map(jtNode -> jtNode.threadGroupData != null ? jtNode.threadGroupData : new ThreadGroupData()).mapToInt(tg -> tg.numThreads).sum();
        // 当前已启动线程数 = 0，启动后动态刷新
        progressLabel.setText(0 + "/" + totalThreads);
        elapsedLabel.setText("0 ms");
        runThread = new Thread(() -> {
            try {
                runJMeterTreeWithProgress(rootNode, progressLabel, totalThreads);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    running = false;
                    runBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    updateReportPanel();
                    OkHttpClientManager.setDefaultConnectionPoolConfig();
                    // 正常执行完成时也停止定时采样
                    stopTrendTimer();
                });
            }
        });
        runThread.start();
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
                int numThreads = tg.numThreads;
                int loops = tg.loops;
                ExecutorService executor = Executors.newFixedThreadPool(numThreads);
                for (int i = 0; i < numThreads; i++) {
                    if (!running) {
                        executor.shutdownNow();
                        return;
                    }
                    executor.submit(() -> {
                        activeThreads.incrementAndGet();
                        SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                        try {
                            runTask(rootNode, loops);
                        } finally {
                            activeThreads.decrementAndGet();
                            SwingUtilities.invokeLater(() -> progressLabel.setText(activeThreads.get() + "/" + totalThreads));
                        }
                    });
                }
                executor.shutdown();
                try {
                    executor.awaitTermination(1, TimeUnit.HOURS);
                } catch (InterruptedException exception) {
                    JOptionPane.showMessageDialog(this, "执行被中断: " + exception.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    log.error(exception.getMessage(), exception);
                }
            } else {
                log.warn("不支持的节点类型: {}", jtNode.type);
            }
        }
    }

    private void runTask(DefaultMutableTreeNode groupNode, int loops) {
        for (int l = 0; l < loops && running; l++) {
            for (int i = 0; i < groupNode.getChildCount() && running; i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) groupNode.getChildAt(i);
                Object userObj = child.getUserObject();
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
                    if (preOk) {
                        long startTime = System.currentTimeMillis();
                        allRequestStartTimes.add(startTime); // 记录开始时间
                        long costMs = 0;
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
                        long cost = resp == null ? costMs : resp.costMs;
                        allRequestResults.add(new RequestResult(startTime + cost, success)); // 记录结束时间
                        // 统计接口耗时（统一用resp.costMs）
                        apiCostMap.computeIfAbsent(apiName, k -> Collections.synchronizedList(new ArrayList<>())).add(cost);
                    }
                    if (success) {
                        apiSuccessMap.merge(apiName, 1, Integer::sum);
                    } else {
                        apiFailMap.merge(apiName, 1, Integer::sum);
                    }
                    // 高效模式下只保存失败或异常结果
                    if (!efficientMode || !success) {
                        DefaultMutableTreeNode reqNode = new DefaultMutableTreeNode(new ResultNodeInfo(jtNode.httpRequestItem.getName(), success, errorMsg, req, resp, testResults));
                        SwingUtilities.invokeLater(() -> {
                            resultRootNode.add(reqNode);
                            resultTreeModel.reload(resultRootNode);
                            // ====== 刷新耗时UI ======
                            if (!allRequestStartTimes.isEmpty()) {
                                List<Long> snapshot = new ArrayList<>(allRequestStartTimes);
                                long minStart = Collections.min(snapshot);
                                long now = System.currentTimeMillis();
                                long elapsed = now - minStart;
                                elapsedLabel.setText(elapsed + " ms");
                            } else {
                                elapsedLabel.setText("0 ms");
                            }
                        });
                    }
                }
            }
        }
    }

    // 结果树相关
    private JTree resultTree;
    private DefaultTreeModel resultTreeModel;
    private DefaultMutableTreeNode resultRootNode;
    // 替换为tabbedPane
    private JTabbedPane resultDetailTabbedPane;

    private void saveJMeterTreeToFile() {
        saveAllPropertyPanelData(); // 确保保存所有属性区数据
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存JMeter用例树");
        fileChooser.setSelectedFile(new File("EasyPostman-Jmeter.json"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                Object treeData = buildTreeData((DefaultMutableTreeNode) treeModel.getRoot());
                String json = JSONUtil.toJsonPrettyStr(treeData);
                Files.writeString(fileToSave.toPath(), json, StandardCharsets.UTF_8);
                JOptionPane.showMessageDialog(this, "保存成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadJMeterTreeFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("加载JMeter用例树");
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                String json = Files.readString(fileToOpen.toPath(), StandardCharsets.UTF_8);
                JSONObject treeData = JSONUtil.parseObj(json);
                DefaultMutableTreeNode root = parseTreeData(treeData);
                treeModel.setRoot(root);
                jmeterTree.setModel(treeModel);
                jmeterTree.updateUI();
                JOptionPane.showMessageDialog(this, "加载成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "加载失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 递归导出树结构为Map
    private Object buildTreeData(DefaultMutableTreeNode node) {
        JMeterTreeNode jtNode = (JMeterTreeNode) node.getUserObject();
        Map<String, Object> map = new HashMap<>();
        map.put("name", jtNode.name);
        map.put("type", jtNode.type.name());
        map.put("httpRequestItem", jtNode.httpRequestItem);
        map.put("threadGroupData", jtNode.threadGroupData);
        map.put("assertionData", jtNode.assertionData);
        map.put("timerData", jtNode.timerData);
        List<Object> children = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            children.add(buildTreeData((DefaultMutableTreeNode) node.getChildAt(i)));
        }
        map.put("children", children);
        return map;
    }

    // 递归还原树结构
    private DefaultMutableTreeNode parseTreeData(Object data) {
        JSONObject map = (data instanceof JSONObject) ? (JSONObject) data : new JSONObject(data);
        String name = map.getStr("name");
        NodeType type = NodeType.valueOf(map.getStr("type"));
        JMeterTreeNode jtNode = new JMeterTreeNode(name, type);
        jtNode.httpRequestItem = map.get("httpRequestItem", HttpRequestItem.class);
        jtNode.threadGroupData = map.get("threadGroupData", ThreadGroupData.class);
        jtNode.assertionData = map.get("assertionData", AssertionData.class);
        jtNode.timerData = map.get("timerData", TimerData.class);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(jtNode);
        JSONArray children = map.getJSONArray("children");
        if (children != null) {
            for (Object child : children) {
                node.add(parseTreeData(child));
            }
        }
        return node;
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
        JMenuItem addThreadGroup = new JMenuItem("添加用户组");
        JMenuItem addRequest = new JMenuItem("添加请求");
        JMenuItem addAssertion = new JMenuItem("添加断言");
        JMenuItem addTimer = new JMenuItem("添加定时器");
        JMenuItem renameNode = new JMenuItem("重命名");
        JMenuItem deleteNode = new JMenuItem("删除");
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
            DefaultMutableTreeNode group = new DefaultMutableTreeNode(new JMeterTreeNode("用户组", NodeType.THREAD_GROUP));
            treeModel.insertNodeInto(group, root1, root1.getChildCount());
            jmeterTree.expandPath(new TreePath(root1.getPath()));
        });
        // 添加请求
        addRequest.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
            if (node == null) return;
            Object userObj = node.getUserObject();
            if (!(userObj instanceof JMeterTreeNode jtNode) || jtNode.type != NodeType.THREAD_GROUP) {
                JOptionPane.showMessageDialog(JMeterPanel.this, "请选择一个用户组节点进行添加", "提示", JOptionPane.WARNING_MESSAGE);
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
            String newName = JOptionPane.showInputDialog(JMeterPanel.this, "重命名节点:", oldName);
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

        // 结果树节点点击，展示请求响应详情
        resultTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) resultTree.getLastSelectedPathComponent();
            if (node == null) {
                for (int i = 0; i < resultDetailTabbedPane.getTabCount(); i++) {
                    JScrollPane scroll = (JScrollPane) resultDetailTabbedPane.getComponentAt(i);
                    JEditorPane pane = (JEditorPane) scroll.getViewport().getView();
                    pane.setText("");
                }
                return;
            }
            Object userObj = node.getUserObject();
            if (userObj instanceof ResultNodeInfo info) {
                // 构建HTML内容
                PreparedRequest req = info.req;
                HttpResponse resp = info.resp;
                // Request
                setTabHtml(0, buildRequestHtml(req));
                // Response
                setTabHtml(1, buildResponseHtml(resp));
                // Tests
                if (info.testResults != null && !info.testResults.isEmpty()) {
                    setTabHtml(2, buildTestsHtml(info.testResults));
                } else {
                    setTabHtml(2, "<html><body><i>无断言结果</i></body></html>");
                }
                // Timing
                if (resp != null && resp.httpEventInfo != null) {
                    setTabHtml(3, buildTimingHtml(req, resp));
                    setTabHtml(4, buildEventInfoHtml(req, resp));
                } else {
                    setTabHtml(3, "<html><body><i>无Timing信息</i></body></html>");
                    setTabHtml(4, "<html><body><i>无Event信息</i></body></html>");
                }
            } else {
                for (int i = 0; i < resultDetailTabbedPane.getTabCount(); i++) {
                    JScrollPane scroll = (JScrollPane) resultDetailTabbedPane.getComponentAt(i);
                    JEditorPane pane = (JEditorPane) scroll.getViewport().getView();
                    pane.setContentType("text/html");
                    pane.setEditable(false);
                    pane.setText("<html><body><i>无数据</i></body></html>");
                    pane.setCaretPosition(0);
                }
            }
        });

        searchField.addActionListener(e -> searchResultTree(searchField.getText()));
    }

    // 设置tab页内容
    private void setTabHtml(int tabIdx, String html) {
        JScrollPane scroll = (JScrollPane) resultDetailTabbedPane.getComponentAt(tabIdx);
        JEditorPane pane = (JEditorPane) scroll.getViewport().getView();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setText(html);
        pane.setCaretPosition(0);
    }

    // 复用RunnerPanel的HTML构建方法
    private String buildRequestHtml(PreparedRequest req) {
        return RunnerHtmlUtil.buildRequestHtml(req);
    }

    private String buildResponseHtml(HttpResponse resp) {
        return RunnerHtmlUtil.buildResponseHtml(resp);
    }

    private String buildTestsHtml(List<TestResult> testResults) {
        return RunnerHtmlUtil.buildTestsHtml(testResults);
    }

    private String buildTimingHtml(PreparedRequest req, HttpResponse resp) {
        RequestHistoryItem item = new RequestHistoryItem(req, resp);
        return HistoryHtmlBuilder.formatHistoryDetailPrettyHtml_Timing(item);
    }

    private String buildEventInfoHtml(PreparedRequest req, HttpResponse resp) {
        RequestHistoryItem item = new RequestHistoryItem(req, resp);
        item.response = resp;
        return HistoryHtmlBuilder.formatHistoryDetailPrettyHtml_EventInfo(item);
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
        reportTableModel.setRowCount(0);
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
            long avgAvg = totalApi > 0 ? totalCost / totalApi : 0;
            reportTableModel.addRow(new Object[]{"Total", totalApi, totalSuccess, totalFail, String.format("%.2f", totalQps), avgAvg, totalMin == Long.MAX_VALUE ? 0 : totalMin, totalMax, avgP99, totalCost, String.format("%.2f%%", avgRate)});
        }
    }

    private long getP99(List<Long> costs) {
        if (costs == null || costs.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(costs);
        Collections.sort(sorted);
        int idx = (int) Math.ceil(sorted.size() * 0.99) - 1;
        return sorted.get(Math.max(idx, 0));
    }

    // ========== 结果树搜索方法 ==========
    private void searchResultTree(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            // 关键字为空，显示全部
            resultTreeModel.setRoot(resultRootNode);
            resultTree.updateUI();
            return;
        }
        // 过滤结果树，仅显示匹配的节点及其父节点
        DefaultMutableTreeNode filteredRoot = filterResultTree(resultRootNode, keyword);
        resultTreeModel.setRoot(filteredRoot);
        resultTree.updateUI();
        // 展开所有节点
        for (int i = 0; i < resultTree.getRowCount(); i++) {
            resultTree.expandRow(i);
        }
    }

    private DefaultMutableTreeNode filterResultTree(DefaultMutableTreeNode node, String keyword) {
        boolean matched = node.toString().contains(keyword);
        DefaultMutableTreeNode filteredNode = new DefaultMutableTreeNode(node.getUserObject());
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            DefaultMutableTreeNode filteredChild = filterResultTree(child, keyword);
            if (filteredChild != null) {
                filteredNode.add(filteredChild);
                matched = true;
            }
        }
        return matched ? filteredNode : null;
    }

    private static int getJmeterMaxIdleConnections() {
        return SettingManager.getJmeterMaxIdleConnections();
    }

    private static long getJmeterKeepAliveSeconds() {
        return SettingManager.getJmeterKeepAliveSeconds();
    }

    /**
     * 刷新内存占用标签
     */
    private void updateMemoryLabel(JLabel memoryLabel) {
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long max = rt.maxMemory();
        String usedStr = String.format("%.1fMB", used / 1024.0 / 1024);
        String maxStr = String.format("%.1fMB", max / 1024.0 / 1024);
        memoryLabel.setText("内存: " + usedStr + " / " + maxStr);
    }
}