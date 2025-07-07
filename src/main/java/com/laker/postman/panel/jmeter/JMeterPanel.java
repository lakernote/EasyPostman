package com.laker.postman.panel.jmeter;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.Colors;
import com.laker.postman.common.panel.BasePanel;
import com.laker.postman.model.*;
import com.laker.postman.panel.SidebarTabPanel;
import com.laker.postman.panel.collections.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.edit.RequestEditSubPanel;
import com.laker.postman.panel.history.HistoryHtmlBuilder;
import com.laker.postman.panel.runner.RunnerHtmlUtil;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.js.JsScriptExecutor;
import com.laker.postman.util.FontUtil;
import com.laker.postman.util.JsonPathUtil;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

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
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 左侧多层级树（用户组-请求-断言-定时器），右侧属性区，底部Tab结果区
 */
@Slf4j
public class JMeterPanel extends BasePanel {
    private JTree jmeterTree;
    private DefaultTreeModel treeModel;
    private JPanel propertyPanel; // 右侧属性区（CardLayout）
    private CardLayout propertyCardLayout;
    private JTabbedPane resultTabbedPane; // 底���/右侧结果Tab
    private ThreadGroupPropertyPanel threadGroupPanel;
    private AssertionPropertyPanel assertionPanel;
    private TimerPropertyPanel timerPanel;
    private RequestEditSubPanel requestEditSubPanel;
    private boolean running = false;
    private Thread runThread;
    private JButton runBtn;
    private JButton stopBtn;
    // 统计变量
    private int totalRequests = 0;
    private DefaultTableModel reportTableModel;
    // 按接口统计
    private final Map<String, List<Long>> apiCostMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> apiSuccessMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> apiFailMap = new ConcurrentHashMap<>();
    // 趋势图相关
    private DefaultCategoryDataset trendDataset;


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
        resultRootNode = new DefaultMutableTreeNode("结果树");
        resultTreeModel = new DefaultTreeModel(resultRootNode);
        resultTree = new JTree(resultTreeModel);
        resultTree.setRootVisible(true);
        resultTree.setShowsRootHandles(true);
        resultTree.setCellRenderer(new ResultTreeCellRenderer());
        JScrollPane resultTreeScroll = new JScrollPane(resultTree);
        resultTreeScroll.setPreferredSize(new Dimension(320, 400));
        // 详情tab
        resultDetailTabbedPane = new JTabbedPane();
        resultDetailTabbedPane.addTab("Request", new JScrollPane(new JEditorPane()));
        resultDetailTabbedPane.addTab("Response", new JScrollPane(new JEditorPane()));
        resultDetailTabbedPane.addTab("Tests", new JScrollPane(new JEditorPane()));
        resultDetailTabbedPane.addTab("Timing", new JScrollPane(new JEditorPane()));
        resultDetailTabbedPane.addTab("Event Info", new JScrollPane(new JEditorPane()));
        JSplitPane resultSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, resultTreeScroll, resultDetailTabbedPane);
        resultSplit.setDividerLocation(260);
        resultTabbedPane = new JTabbedPane();
        resultTabbedPane.addTab("结果树", resultSplit);
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
        // 失败列红色渲染器（0为黑色，大于0为红色，总计行为蓝色加粗）
        DefaultTableCellRenderer failRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                boolean isTotal = "总计".equals(reportTableModel.getValueAt(modelRow, 0));
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
        // 成功率列绿色渲染器（总计行为蓝色加粗）
        DefaultTableCellRenderer rateRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                boolean isTotal = "总计".equals(reportTableModel.getValueAt(modelRow, 0));
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
        // 通用居中渲染器（总计行美化）
        DefaultTableCellRenderer generalRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                boolean isTotal = "总计".equals(reportTableModel.getValueAt(modelRow, 0));
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
        resultTabbedPane.addTab("报表", reportPanel);
        // 趋势图面板
        trendDataset = new DefaultCategoryDataset();
        JFreeChart trendChart = ChartFactory.createLineChart(
                "接口响应耗时趋势图", // 图表标题
                "请求序号", // X轴标签
                "耗时(ms)", // Y轴标签
                trendDataset,
                PlotOrientation.VERTICAL,
                true, // 图例
                false,
                false
        );
        // 设置趋势图样式
        CategoryPlot plot = trendChart.getCategoryPlot();
        // 设置字体，防止中文乱码
        Font font = FontUtil.getDefaultFont(Font.PLAIN, 12);
        trendChart.getTitle().setFont(font.deriveFont(13f));
        if (trendChart.getLegend() != null) trendChart.getLegend().setItemFont(font);
        plot.getDomainAxis().setTickLabelFont(font);
        plot.getDomainAxis().setLabelFont(font);
        plot.getRangeAxis().setTickLabelFont(font);
        plot.getRangeAxis().setLabelFont(font);
        // Y轴自动调整
        plot.getRangeAxis().setAutoRange(true);
        // X轴标签旋转45度，防止重叠
        org.jfree.chart.axis.CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(org.jfree.chart.axis.CategoryLabelPositions.UP_45);
        // X轴标签间隔显示（如每隔10个显示1个）
        domainAxis.setMaximumCategoryLabelWidthRatio(0.8f);
        // 设置背景色和交互
        ChartPanel chartPanel = new ChartPanel(trendChart);
        chartPanel.setMouseWheelEnabled(true); // 支持鼠标滚轮缩放
        chartPanel.setBackground(Colors.PANEL_BACKGROUND);
        chartPanel.setDisplayToolTips(true); // 显示工具提示
        chartPanel.setPreferredSize(new Dimension(300, 300));
        // 趋势图面板
        JPanel trendPanel = new JPanel(new BorderLayout());
        trendPanel.add(chartPanel, BorderLayout.CENTER);
        resultTabbedPane.addTab("趋势图", trendPanel);

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
        topPanel.add(btnPanel, BorderLayout.WEST);
        // ========== 执行进度指示器 ==========
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        JLabel progressLabel = new JLabel();
        progressLabel.setText("0/0");
        progressLabel.setFont(progressLabel.getFont().deriveFont(Font.BOLD)); // 设置粗体
        progressLabel.setIcon(new FlatSVGIcon("icons/jmeter.svg", 24, 24)); // 使用FlatLaf SVG图标
        // ���置icon在文字右边
        progressLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        progressPanel.add(progressLabel);
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
        if (running) return;
        running = true;
        runBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        resultTabbedPane.setSelectedIndex(0);
        resultRootNode.removeAllChildren();
        totalRequests = 0;
        apiCostMap.clear();
        apiSuccessMap.clear();
        apiFailMap.clear();
        // 统计总请求数
        int total = countTotalRequests((DefaultMutableTreeNode) treeModel.getRoot());
        progressLabel.setText("0/" + total);
        runThread = new Thread(() -> {
            try {
                DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();
                runJMeterTreeWithProgress(rootNode, progressLabel, total);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    running = false;
                    runBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    updateReportPanel();
                });
            }
        });
        runThread.start();
    }

    // 统计总请求数（线程组之间相加，线程组内为loop*numThread*请求数）
    private int countTotalRequests(DefaultMutableTreeNode node) {
        Object userObj = node.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.ROOT) {
            int total = 0;
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode tgNode = (DefaultMutableTreeNode) node.getChildAt(i);
                total += countThreadGroupRequests(tgNode);
            }
            return total;
        }
        return 0;
    }

    // 统计单个线程组的总请求数
    private int countThreadGroupRequests(DefaultMutableTreeNode tgNode) {
        Object userObj = tgNode.getUserObject();
        if (!(userObj instanceof JMeterTreeNode jtNode) || jtNode.type != NodeType.THREAD_GROUP) return 0;
        ThreadGroupData tg = jtNode.threadGroupData != null ? jtNode.threadGroupData : new ThreadGroupData();
        int numThreads = tg.numThreads;
        int loops = tg.loops;
        int reqCount = 0;
        for (int i = 0; i < tgNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) tgNode.getChildAt(i);
            Object childObj = child.getUserObject();
            if (childObj instanceof JMeterTreeNode childJtNode && childJtNode.type == NodeType.REQUEST) {
                reqCount++;
            }
        }
        return numThreads * loops * reqCount;
    }

    // 带进度的执行
    private void runJMeterTreeWithProgress(DefaultMutableTreeNode rootNode, JLabel progressLabel, int total) {
        if (!running) return;
        Object userObj = rootNode.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode) {
            if (Objects.requireNonNull(jtNode.type) == NodeType.ROOT) {
                List<Thread> tgThreads = new ArrayList<>();
                for (int i = 0; i < rootNode.getChildCount(); i++) {
                    DefaultMutableTreeNode tgNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
                    Thread t = new Thread(() -> runJMeterTreeWithProgress(tgNode, progressLabel, total));
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
                    executor.submit(() -> runThreadGroupWithProgress(rootNode, loops, progressLabel, total));
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

    private void runThreadGroupWithProgress(DefaultMutableTreeNode groupNode, int loops, JLabel progressLabel, int total) {
        for (int l = 0; l < loops && running; l++) {
            for (int i = 0; i < groupNode.getChildCount() && running; i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) groupNode.getChildAt(i);
                Object userObj = child.getUserObject();
                if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST && jtNode.httpRequestItem != null) {
                    String apiName = jtNode.httpRequestItem.getName();
                    boolean success = true;
                    int finished;
                    synchronized (this) {
                        totalRequests++;
                        finished = totalRequests;
                    }
                    SwingUtilities.invokeLater(() -> progressLabel.setText(finished + "/" + total));
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
                            log.error("前置脚��执行失败: {}", ex.getMessage(), ex);
                            errorMsg = "前置脚本执行失败: " + ex.getMessage();
                            preOk = false;
                            success = false;
                        }
                    }
                    if (preOk) {
                        long startTime = System.currentTimeMillis();
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
                        // 统计接口耗时（统一用resp.costMs）
                        apiCostMap.computeIfAbsent(apiName, k -> Collections.synchronizedList(new ArrayList<>())).add(resp == null ? costMs : resp.costMs);

                    }

                    if (success) {
                        apiSuccessMap.merge(apiName, 1, Integer::sum);
                    } else {
                        apiFailMap.merge(apiName, 1, Integer::sum);
                    }
                    DefaultMutableTreeNode reqNode = new DefaultMutableTreeNode(new ResultNodeInfo(jtNode.httpRequestItem.getName(), success, errorMsg, req, resp, testResults));
                    SwingUtilities.invokeLater(() -> {
                        resultRootNode.add(reqNode);
                        resultTreeModel.reload(resultRootNode);
                    });
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
                    pane.setText("");
                }
            }
        });
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

    private String buildTestsHtml(List<? extends Object> testResults) {
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
    }

    private void updateReportPanel() {
        // 表格统计
        reportTableModel.setRowCount(0);
        trendDataset.clear();
        int totalApi = 0, totalSuccess = 0, totalFail = 0;
        long totalCost = 0, totalAvg = 0, totalMin = Long.MAX_VALUE, totalMax = 0, totalP99 = 0;
        double totalQps = 0, totalRate = 0;
        int apiCount = 0;
        for (String api : apiCostMap.keySet()) {
            List<Long> costs = apiCostMap.get(api);
            int apiTotal = costs.size();
            int apiSuccess = apiSuccessMap.getOrDefault(api, 0);
            int apiFail = apiFailMap.getOrDefault(api, 0);
            long apiAvg = apiSuccess > 0 ? costs.stream().mapToLong(Long::longValue).sum() / apiSuccess : 0;
            long apiMin = costs.stream().mapToLong(Long::longValue).min().orElse(0);
            long apiMax = costs.stream().mapToLong(Long::longValue).max().orElse(0);
            long apiP99 = getP99(costs);
            long apiTotalCost = costs.stream().mapToLong(Long::longValue).sum();
            double apiQps = (apiTotalCost > 0 && apiTotal > 0) ? (apiTotal * 1000.0 / apiTotalCost) : 0;
            double apiRate = apiTotal > 0 ? (apiSuccess * 100.0 / apiTotal) : 0;
            reportTableModel.addRow(new Object[]{api, apiTotal, apiSuccess, apiFail, String.format("%.2f", apiQps), apiAvg, apiMin, apiMax, apiP99, apiTotalCost, String.format("%.2f%%", apiRate)});
            // 累加total
            totalApi += apiTotal;
            totalSuccess += apiSuccess;
            totalFail += apiFail;
            totalCost += apiTotalCost;
            totalAvg += apiAvg;
            totalMin = Math.min(totalMin, apiMin);
            totalMax = Math.max(totalMax, apiMax);
            totalP99 += apiP99;
            totalQps += apiQps;
            totalRate += apiRate;
            apiCount++;
            // 趋势图数据
            for (int i = 0; i < costs.size(); i++) {
                trendDataset.addValue(costs.get(i), api, String.valueOf(i + 1));
            }
        }
        // 添加total行
        if (apiCount > 0) {
            long avgAvg = totalAvg / apiCount;
            long avgP99 = totalP99 / apiCount;
            double avgQps = totalQps / apiCount;
            double avgRate = totalRate / apiCount;
            reportTableModel.addRow(new Object[]{"总计", totalApi, totalSuccess, totalFail, String.format("%.2f", avgQps), avgAvg, totalMin == Long.MAX_VALUE ? 0 : totalMin, totalMax, avgP99, totalCost, String.format("%.2f%%", avgRate)});
        }
    }

    private long getP99(List<Long> costs) {
        if (costs == null || costs.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(costs);
        Collections.sort(sorted);
        int idx = (int) Math.ceil(sorted.size() * 0.99) - 1;
        return sorted.get(Math.max(idx, 0));
    }
}

