package com.laker.postman.panel.jmeter;

import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.Colors;
import com.laker.postman.common.panel.BasePanel;
import com.laker.postman.common.tree.RequestTreeCellRenderer;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.collections.RequestCollectionsSubPanel;
import com.laker.postman.panel.collections.edit.RequestEditSubPanel;
import com.laker.postman.util.FontUtil;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.util.JsonPathUtil;
import com.laker.postman.service.http.PreparedRequestBuilder;
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
 * JMeter风格完整UI骨架：左侧多层级树（用户组-请求-断言-定时器），右侧属性区，底部Tab结果区
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
    // 报表面板相关
    private JPanel reportPanel;
    private JTable reportTable;
    private DefaultTableModel reportTableModel;
    // 按接口统计
    private final Map<String, List<Long>> apiCostMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> apiSuccessMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> apiFailMap = new ConcurrentHashMap<>();
    // 趋势图面板
    private JPanel trendPanel;
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
        requestEditSubPanel = new RequestEditSubPanel();
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
        resultDetailArea = new JTextArea();
        resultDetailArea.setEditable(false);
        JScrollPane detailScroll = new JScrollPane(resultDetailArea);
        JSplitPane resultSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, resultTreeScroll, detailScroll);
        resultSplit.setDividerLocation(260);
        resultTabbedPane = new JTabbedPane();
        resultTabbedPane.addTab("结果树", resultSplit);
        // 报表面板
        reportPanel = new JPanel(new BorderLayout());
        String[] columns = {"接口名称", "总数", "成功", "失败", "QPS", "平均(ms)", "最小(ms)", "最大(ms)", "P99(ms)", "总耗时(ms)", "成功率"};
        reportTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        reportTable = new JTable(reportTableModel);
        reportTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
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
        // 设置背景色和交互
        ChartPanel chartPanel = new ChartPanel(trendChart);
        chartPanel.setMouseWheelEnabled(true); // 支持鼠标滚轮缩放
        chartPanel.setBackground(Colors.PANEL_BACKGROUND);
        chartPanel.setDisplayToolTips(true); // 显示工具提示
        chartPanel.setPreferredSize(new Dimension(300, 300));
        trendPanel = new JPanel(new BorderLayout());
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
        // 设置icon在文字右边
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
                    String responseBody = null;
                    int responseCode = -1;
                    StringBuilder detail = new StringBuilder();
                    PreparedRequest req = null;
                    HttpResponse resp = null;
                    long cost = 0;
                    try {
                        req = PreparedRequestBuilder.build(jtNode.httpRequestItem);
                        resp = HttpSingleRequestExecutor.execute(req);
                        responseBody = resp.body;
                        responseCode = resp.code;
                        cost = resp.costMs;
                        detail.append("请求URL: ").append(req.url).append("\n");
                        detail.append("请求方法: ").append(req.method).append("\n");
                        detail.append("请求耗时: ").append(cost).append(" ms\n");
                        detail.append("执行线程: ").append(resp.threadName).append("\n");
                        detail.append("连接信息: ").append(resp.httpEventInfo.getLocalAddress()).append("->").append(resp.httpEventInfo.getRemoteAddress()).append("\n");
                        detail.append("请求头: ").append(req.headers).append("\n");
                        detail.append("请求体: ").append(req.body).append("\n");
                        if (MapUtil.isNotEmpty(req.formData)) {
                            detail.append("请求表单数据: \n");
                            for (Map.Entry<String, String> entry : req.formData.entrySet()) {
                                detail.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                            }
                        }
                        if (MapUtil.isNotEmpty(req.formFiles)) {
                            detail.append("请求表单文件: \n");
                            for (Map.Entry<String, String> entry : req.formFiles.entrySet()) {
                                detail.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                            }
                        }
                        detail.append("响应码: ").append(responseCode).append("\n");
                        detail.append("响应体: ").append(responseBody).append("\n");
                        detail.append("响应体字节数: ").append(resp.bodySize).append("B\n");
                        detail.append("响应头字节数: ").append(resp.headersSize).append("B\n");
                    } catch (Exception ex) {
                        detail.append("请求异常: ").append(ex.getMessage());
                        success = false;
                    }
                    // 断言处理
                    for (int j = 0; j < child.getChildCount(); j++) {
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
                                    if ("=".equals(op)) pass = (responseCode == expect);
                                    else if (">".equals(op)) pass = (responseCode > expect);
                                    else if ("<".equals(op)) pass = (responseCode < expect);
                                } catch (Exception e) {
                                    pass = false;
                                }
                                detail.append("断言[Response Code] ").append(op).append(valStr).append(": ");
                            } else if ("Contains".equals(type)) {
                                pass = responseBody != null && responseBody.contains(assertion.content);
                                detail.append("断言[Contains] 包含: ").append(assertion.content).append(": ");
                            } else if ("JSONPath".equals(type)) {
                                String jsonPath = assertion.value;
                                String expect = assertion.content;
                                String actual = JsonPathUtil.extractJsonPath(responseBody, jsonPath);
                                pass = Objects.equals(actual, expect);
                                detail.append("断言[JSONPath] ").append(jsonPath).append(" = ").append(expect).append(", 实际:").append(actual).append(": ");
                            }
                            if (!pass) {
                                detail.append("失败\n");
                                success = false;
                            } else {
                                detail.append("通过\n");
                            }
                        }
                        if (subObj instanceof JMeterTreeNode subNode2 && subNode2.type == NodeType.TIMER && subNode2.timerData != null) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(subNode2.timerData.delayMs);
                            } catch (InterruptedException ignored) {
                                return;
                            }
                        }
                    }
                    // 统计接口耗时（统一用resp.costMs）
                    apiCostMap.computeIfAbsent(apiName, k -> Collections.synchronizedList(new ArrayList<>())).add(cost);
                    if (success) {
                        apiSuccessMap.merge(apiName, 1, Integer::sum);
                    } else {
                        apiFailMap.merge(apiName, 1, Integer::sum);
                    }
                    DefaultMutableTreeNode reqNode = new DefaultMutableTreeNode(new ResultNodeInfo(jtNode.httpRequestItem.getName(), success,
                            detail.toString(), req, resp));
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
    private JTextArea resultDetailArea;

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
            RequestCollectionsSubPanel collectionPanel = SingletonFactory.getInstance(RequestCollectionsSubPanel.class);
            DefaultTreeModel groupTreeModel = collectionPanel != null ? collectionPanel.getGroupTreeModel() : null;
            if (groupTreeModel == null) {
                JOptionPane.showMessageDialog(JMeterPanel.this, "未找到请求集合，无法选择", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JTree collectionTree = new JTree(groupTreeModel);
            collectionTree.setRootVisible(false);
            collectionTree.setShowsRootHandles(true);
            collectionTree.setCellRenderer(new RequestTreeCellRenderer());
            JScrollPane scrollPane = new JScrollPane(collectionTree);
            scrollPane.setPreferredSize(new Dimension(320, 320));
            int result = JOptionPane.showConfirmDialog(JMeterPanel.this, scrollPane, "选择请求", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                TreePath selPath = collectionTree.getSelectionPath();
                if (selPath != null) {
                    DefaultMutableTreeNode selNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                    Object userObj = selNode.getUserObject();
                    if (userObj instanceof Object[] arr && "request".equals(arr[0])) {
                        HttpRequestItem reqItem = (HttpRequestItem) arr[1];
                        DefaultMutableTreeNode req = new DefaultMutableTreeNode(new JMeterTreeNode(reqItem.getName(), NodeType.REQUEST, reqItem));
                        treeModel.insertNodeInto(req, node, node.getChildCount());
                        jmeterTree.expandPath(new TreePath(node.getPath()));
                        // 选中并填充右侧属性区
                        TreePath newPath = new TreePath(req.getPath());
                        jmeterTree.setSelectionPath(newPath);
                        propertyCardLayout.show(propertyPanel, "request");
                        requestEditSubPanel.updateRequestForm(reqItem);
                        return;
                    }
                }
                JOptionPane.showMessageDialog(JMeterPanel.this, "请选择一个请求节点", "提示", JOptionPane.WARNING_MESSAGE);
            }
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
                resultDetailArea.setText("");
                return;
            }
            Object userObj = node.getUserObject();
            if (userObj instanceof ResultNodeInfo info) {
                StringBuilder sb = new StringBuilder();
                sb.append("请求名称: ").append(info.name).append("\n");
                sb.append(info.detail).append("\n");
                resultDetailArea.setText(sb.toString());
            } else {
                resultDetailArea.setText("");
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
    }

    private void updateReportPanel() {
        // 表格统计
        reportTableModel.setRowCount(0);
        trendDataset.clear();
        for (String api : apiCostMap.keySet()) {
            List<Long> costs = apiCostMap.get(api);
            int apiTotal = costs.size();
            int apiSuccess = apiSuccessMap.getOrDefault(api, 0);
            int apiFail = apiFailMap.getOrDefault(api, 0);
            long apiAvg = apiSuccess > 0 ? costs.stream().mapToLong(Long::longValue).sum() / apiSuccess : 0;
            long apiMin = costs.stream().mapToLong(Long::longValue).min().orElse(0);
            long apiMax = costs.stream().mapToLong(Long::longValue).max().orElse(0);
            long apiP99 = getP99(costs);
            // QPS计算：用所有请求的总耗时costMs之和
            long totalCost = costs.stream().mapToLong(Long::longValue).sum();
            double apiQps = (totalCost > 0 && apiTotal > 0) ? (apiTotal * 1000.0 / totalCost) : 0;
            double apiRate = apiTotal > 0 ? (apiSuccess * 100.0 / apiTotal) : 0;
            reportTableModel.addRow(new Object[]{api, apiTotal, apiSuccess, apiFail, String.format("%.2f", apiQps), apiAvg, apiMin, apiMax, apiP99, totalCost, String.format("%.2f%%", apiRate)});
            // 趋势图数据
            for (int i = 0; i < costs.size(); i++) {
                trendDataset.addValue(costs.get(i), api, String.valueOf(i + 1));
            }
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

