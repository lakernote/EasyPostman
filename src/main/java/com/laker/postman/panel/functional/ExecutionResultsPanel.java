package com.laker.postman.panel.functional;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.model.BatchExecutionHistory;
import com.laker.postman.model.IterationResult;
import com.laker.postman.model.RequestResult;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.util.FontUtil;
import com.laker.postman.util.TimeDisplayUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 执行结果展示面板 - 类似 Postman 的 Runner Results
 */
public class ExecutionResultsPanel extends JPanel {
    private JTree resultsTree;
    private DefaultTreeModel treeModel;
    private JPanel detailPanel;
    private JTabbedPane detailTabs;
    private BatchExecutionHistory executionHistory;

    // 日期格式化器
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public ExecutionResultsPanel() {
        initUI();
        registerListeners();
    }

    /**
     * 格式化时间戳为 yyyy-MM-dd HH:mm:ss 格式
     */
    private static String formatTimestamp(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 创建分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(350);
        splitPane.setResizeWeight(0.3);

        // 左侧：结果树
        createResultsTree();
        splitPane.setLeftComponent(createTreePanel());

        // 右侧：详情面板
        createDetailPanel();
        splitPane.setRightComponent(detailPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private void createResultsTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("执行结果");
        treeModel = new DefaultTreeModel(root);
        resultsTree = new JTree(treeModel);
        resultsTree.setRootVisible(true);
        resultsTree.setShowsRootHandles(true);
        resultsTree.setCellRenderer(new ExecutionResultTreeCellRenderer());
        resultsTree.setFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
        resultsTree.setRowHeight(24);
    }

    private JPanel createTreePanel() {
        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.setBorder(BorderFactory.createTitledBorder("执行历史"));

        JScrollPane treeScrollPane = new JScrollPane(resultsTree);
        treePanel.add(treeScrollPane, BorderLayout.CENTER);

        // 添加工具栏
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        toolBar.setOpaque(false);

        JButton expandAllBtn = new JButton("展开全部");
        expandAllBtn.setFont(FontUtil.getDefaultFont(Font.PLAIN, 11));
        expandAllBtn.addActionListener(e -> expandAll());

        JButton collapseAllBtn = new JButton("收起全部");
        collapseAllBtn.setFont(FontUtil.getDefaultFont(Font.PLAIN, 11));
        collapseAllBtn.addActionListener(e -> collapseAll());

        toolBar.add(expandAllBtn);
        toolBar.add(collapseAllBtn);
        treePanel.add(toolBar, BorderLayout.SOUTH);

        return treePanel;
    }

    private void createDetailPanel() {
        detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBorder(BorderFactory.createTitledBorder("详细信息"));

        // 创建详情选项卡
        detailTabs = new JTabbedPane();
        detailTabs.setFont(FontUtil.getDefaultFont(Font.PLAIN, 12));

        // 默认显示欢迎页面
        JPanel welcomePanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("<html><div style='text-align: center;'>" +
                "<br><br><br>" +
                "<span style='font-size: 16px; color: #666;'>选择左侧的请求记录查看详细信息</span>" +
                "<br><br>" +
                "<span style='font-size: 12px; color: #999;'>支持查看请求、响应、测试结果等</span>" +
                "</div></html>");
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        welcomePanel.add(welcomeLabel, BorderLayout.CENTER);

        detailTabs.addTab("概览", welcomePanel);
        detailPanel.add(detailTabs, BorderLayout.CENTER);
    }

    private void registerListeners() {
        resultsTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) { // 单击显示详情
                    TreePath path = resultsTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        showNodeDetail(node);
                    }
                }
            }
        });
    }

    /**
     * 更新执行历史数据
     */
    public void updateExecutionHistory(BatchExecutionHistory history) {
        this.executionHistory = history;
        rebuildTree();
    }

    private void rebuildTree() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        root.removeAllChildren();

        if (executionHistory == null) {
            root.setUserObject("执行结果 (无数据)");
            treeModel.nodeStructureChanged(root);
            return;
        }

        // 设置根节点信息
        long totalTime = executionHistory.getExecutionTime();
        int totalIterations = executionHistory.getIterations().size();
        root.setUserObject(String.format("执行结果 (%d 轮迭代, %s)",
                totalIterations, TimeDisplayUtil.formatElapsedTime(totalTime)));

        // 添加迭代节点
        for (IterationResult iteration : executionHistory.getIterations()) {
            DefaultMutableTreeNode iterationNode = new DefaultMutableTreeNode(
                    new IterationNodeData(iteration));

            // 添加请求节点
            for (RequestResult request : iteration.getRequestResults()) {
                DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(
                        new RequestNodeData(request));
                iterationNode.add(requestNode);
            }

            root.add(iterationNode);
        }

        treeModel.nodeStructureChanged(root);
        expandAll(); // 默认展开所有节点
    }

    private void showNodeDetail(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();

        detailTabs.removeAll();

        if (userObject instanceof IterationNodeData) {
            showIterationDetail((IterationNodeData) userObject);
        } else if (userObject instanceof RequestNodeData) {
            showRequestDetail((RequestNodeData) userObject);
        } else {
            showOverviewDetail();
        }

        detailTabs.revalidate();
        detailTabs.repaint();
    }

    private void showOverviewDetail() {
        if (executionHistory == null) {
            detailTabs.addTab("概览", createWelcomePanel());
            return;
        }

        JPanel overviewPanel = new JPanel(new BorderLayout());
        overviewPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建统计信息
        JPanel statsPanel = createStatsPanel();
        overviewPanel.add(statsPanel, BorderLayout.NORTH);

        // 创建汇总表格
        JTable summaryTable = createSummaryTable();
        JScrollPane scrollPane = new JScrollPane(summaryTable);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        overviewPanel.add(scrollPane, BorderLayout.CENTER);

        detailTabs.addTab("执行概览", overviewPanel);
    }

    private void showIterationDetail(IterationNodeData iterationData) {
        IterationResult iteration = iterationData.iteration;

        // 迭代概览
        JPanel iterationPanel = new JPanel(new BorderLayout());
        iterationPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 迭代信息
        JPanel infoPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder("迭代信息"));

        infoPanel.add(new JLabel("迭代轮次:"));
        infoPanel.add(new JLabel("第 " + (iteration.getIterationIndex() + 1) + " 轮"));

        infoPanel.add(new JLabel("开始时间:"));
        infoPanel.add(new JLabel(formatTimestamp(iteration.getStartTime())));

        infoPanel.add(new JLabel("执行时长:"));
        infoPanel.add(new JLabel(TimeDisplayUtil.formatElapsedTime(iteration.getExecutionTime())));

        infoPanel.add(new JLabel("请求数量:"));
        infoPanel.add(new JLabel(String.valueOf(iteration.getRequestResults().size())));

        iterationPanel.add(infoPanel, BorderLayout.NORTH);

        // CSV 数据（如果有）
        if (iteration.getCsvData() != null && !iteration.getCsvData().isEmpty()) {
            JPanel csvPanel = createCsvDataPanel(iteration.getCsvData());
            iterationPanel.add(csvPanel, BorderLayout.CENTER);
        }

        detailTabs.addTab("迭代详情", iterationPanel);
    }

    private void showRequestDetail(RequestNodeData requestData) {
        RequestResult request = requestData.request;


        // 清空现有的选项卡
        detailTabs.removeAll();

        // 请求信息（HTML） - 使用 getReq() 方法获取 PreparedRequest
        JEditorPane reqPane = new JEditorPane();
        reqPane.setContentType("text/html");
        reqPane.setEditable(false);
        reqPane.setText(HttpHtmlRenderer.renderRequest(request.getReq()));
        reqPane.setCaretPosition(0);
        detailTabs.addTab("Request", new FlatSVGIcon("icons/http.svg", 16, 16), new JScrollPane(reqPane));

        // 响应信息（HTML） - 使用 HttpHtmlRenderer.renderResponse
        if (request.getResponse() != null) {
            JEditorPane respPane = new JEditorPane();
            respPane.setContentType("text/html");
            respPane.setEditable(false);
            respPane.setText(HttpHtmlRenderer.renderResponse(request.getResponse()));
            respPane.setCaretPosition(0);
            detailTabs.addTab("Response", new FlatSVGIcon("icons/check.svg", 16, 16), new JScrollPane(respPane));
        }

        // Tests 断言结果Tab - 使用 HttpHtmlRenderer.renderTestResults
        if (request.getTestResults() != null && !request.getTestResults().isEmpty()) {
            JEditorPane testsPane = new JEditorPane();
            testsPane.setContentType("text/html");
            testsPane.setEditable(false);
            testsPane.setText(HttpHtmlRenderer.renderTestResults(request.getTestResults()));
            testsPane.setCaretPosition(0);
            detailTabs.addTab("Tests", new FlatSVGIcon("icons/code.svg", 16, 16), new JScrollPane(testsPane));
        }

        // Timing & Event Info Tab - 使用 HttpHtmlRenderer 的方法
        if (request.getResponse() != null && request.getResponse().httpEventInfo != null) {
            JEditorPane timingPane = new JEditorPane();
            timingPane.setContentType("text/html");
            timingPane.setEditable(false);
            timingPane.setText(HttpHtmlRenderer.renderTimingInfo(request.getResponse()));
            timingPane.setCaretPosition(0);
            detailTabs.addTab("Timing", new FlatSVGIcon("icons/time.svg", 16, 16), new JScrollPane(timingPane));

            JEditorPane eventInfoPane = new JEditorPane();
            eventInfoPane.setContentType("text/html");
            eventInfoPane.setEditable(false);
            eventInfoPane.setText(HttpHtmlRenderer.renderEventInfo(request.getResponse()));
            eventInfoPane.setCaretPosition(0);
            detailTabs.addTab("Event Info", new FlatSVGIcon("icons/detail.svg", 16, 16), new JScrollPane(eventInfoPane));
        }
    }

    private void expandAll() {
        for (int i = 0; i < resultsTree.getRowCount(); i++) {
            resultsTree.expandRow(i);
        }
    }

    private void collapseAll() {
        for (int i = resultsTree.getRowCount() - 1; i >= 0; i--) {
            resultsTree.collapseRow(i);
        }
    }

    // 树节点数据类
    private static class IterationNodeData {
        final IterationResult iteration;

        IterationNodeData(IterationResult iteration) {
            this.iteration = iteration;
        }

        @Override
        public String toString() {
            long passedCount = iteration.getRequestResults().stream()
                    .filter(req -> "Pass".equals(req.getAssertion()))
                    .count();
            return String.format("第 %d 轮 (%d/%d 通过, %s)",
                    iteration.getIterationIndex() + 1,
                    passedCount,
                    iteration.getRequestResults().size(),
                    TimeDisplayUtil.formatElapsedTime(iteration.getExecutionTime()));
        }
    }

    private static class RequestNodeData {
        final RequestResult request;

        RequestNodeData(RequestResult request) {
            this.request = request;
        }

        @Override
        public String toString() {
            String status = "Pass".equals(request.getAssertion()) ? "✓" : "✗";
            return String.format("%s %s %s (%s, %dms)",
                    status,
                    request.getMethod(),
                    request.getRequestName(),
                    request.getStatus(),
                    request.getCost());
        }
    }

    // 自定义树渲染器
    private static class ExecutionResultTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();

            if (userObject instanceof IterationNodeData) {
                setIcon(new FlatSVGIcon("icons/functional.svg", 16, 16));
            } else if (userObject instanceof RequestNodeData requestData) {
                // 根据断言结果设置前景色和图标
                if ("Pass".equals(requestData.request.getAssertion())) {
                    // 成功：绿色文字和绿色勾选图标
                    if (!sel) { // 只在非选中状态下设置颜色，选中时保持选中色
                        setForeground(new Color(40, 167, 69)); // 绿色
                    }
                    setIcon(new FlatSVGIcon("icons/check.svg", 16, 16));
                } else if (requestData.request.getAssertion() != null &&
                        !requestData.request.getAssertion().isEmpty() &&
                        !"Pass".equals(requestData.request.getAssertion())) {
                    // 失败：红色文字和红色取消图标
                    if (!sel) { // 只在非选中状态下设置颜色，选中时保持选中色
                        setForeground(new Color(220, 53, 69)); // 红色
                    }
                    setIcon(new FlatSVGIcon("icons/cancel.svg", 16, 16));
                } else {
                    // 未执行或其他状态：默认图标
                    setIcon(new FlatSVGIcon("icons/http.svg", 16, 16));
                }
            } else {
                setIcon(new FlatSVGIcon("icons/history.svg", 16, 16));
            }

            return this;
        }
    }

    private JPanel createWelcomePanel() {
        JPanel welcomePanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("<html><div style='text-align: center;'>" +
                "<br><br><br>" +
                "<span style='font-size: 16px; color: #666;'>选择左侧的记录查看详细信息</span>" +
                "<br><br>" +
                "<span style='font-size: 12px; color: #999;'>支持查看请求、响应、测试结果等</span>" +
                "</div></html>");
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        welcomePanel.add(welcomeLabel, BorderLayout.CENTER);
        return welcomePanel;
    }

    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(2, 4, 15, 5));
        statsPanel.setBorder(BorderFactory.createTitledBorder("执行统计"));

        // 计算统计数据
        int totalIterations = executionHistory.getIterations().size();
        int totalRequests = executionHistory.getIterations().stream()
                .mapToInt(iter -> iter.getRequestResults().size())
                .sum();
        long totalTime = executionHistory.getExecutionTime();

        long passedTests = executionHistory.getIterations().stream()
                .flatMap(iter -> iter.getRequestResults().stream())
                .filter(req -> "Pass".equals(req.getAssertion()))
                .count();
        double successRate = totalRequests > 0 ? (double) passedTests / totalRequests * 100 : 0;

        statsPanel.add(new JLabel("总迭代数: " + totalIterations));
        statsPanel.add(new JLabel("总请求数: " + totalRequests));
        statsPanel.add(new JLabel("总耗时: " + TimeDisplayUtil.formatElapsedTime(totalTime)));
        statsPanel.add(new JLabel(String.format("成功率: %.1f%%", successRate)));

        statsPanel.add(new JLabel("开始时间: " + formatTimestamp(executionHistory.getStartTime())));
        statsPanel.add(new JLabel("结束时间: " + formatTimestamp(executionHistory.getEndTime())));
        statsPanel.add(new JLabel("平均耗时: " + (totalRequests > 0 ? totalTime / totalRequests : 0) + "ms"));
        statsPanel.add(new JLabel("状态: 已完成"));

        return statsPanel;
    }

    private JTable createSummaryTable() {
        String[] columnNames = {"迭代", "请求名称", "方法", "状态", "耗时", "断言", "时间戳"};

        java.util.List<Object[]> tableData = new java.util.ArrayList<>();
        for (IterationResult iteration : executionHistory.getIterations()) {
            for (RequestResult request : iteration.getRequestResults()) {
                Object[] row = {
                        "第" + (iteration.getIterationIndex() + 1) + "轮",
                        request.getRequestName(),
                        request.getMethod(),
                        request.getStatus(),
                        request.getCost() + "ms",
                        request.getAssertion(),
                        formatTimestamp(request.getTimestamp())
                };
                tableData.add(row);
            }
        }

        Object[][] data = tableData.toArray(new Object[0][]);
        JTable table = new JTable(data, columnNames);
        table.setFont(FontUtil.getDefaultFont(Font.PLAIN, 11));
        table.getTableHeader().setFont(FontUtil.getDefaultFont(Font.BOLD, 11));
        table.setRowHeight(22);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        return table;
    }

    private JPanel createCsvDataPanel(java.util.Map<String, String> csvData) {
        JPanel csvPanel = new JPanel(new BorderLayout());
        csvPanel.setBorder(BorderFactory.createTitledBorder("CSV 数据"));

        JTable csvTable = new JTable();
        String[] headers = csvData.keySet().toArray(new String[0]);
        Object[][] data = new Object[1][headers.length];
        for (int i = 0; i < headers.length; i++) {
            data[0][i] = csvData.get(headers[i]);
        }

        csvTable.setModel(new javax.swing.table.DefaultTableModel(data, headers));
        csvTable.setFont(FontUtil.getDefaultFont(Font.PLAIN, 11));
        csvTable.getTableHeader().setFont(FontUtil.getDefaultFont(Font.BOLD, 11));
        csvTable.setRowHeight(20);

        JScrollPane csvScrollPane = new JScrollPane(csvTable);
        csvScrollPane.setPreferredSize(new Dimension(0, 100));
        csvPanel.add(csvScrollPane, BorderLayout.CENTER);

        return csvPanel;
    }

}