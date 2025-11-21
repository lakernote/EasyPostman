package com.laker.postman.panel.functional;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.model.BatchExecutionHistory;
import com.laker.postman.model.IterationResult;
import com.laker.postman.model.RequestResult;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.TimeDisplayUtil;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 执行结果展示面板 - 类似 Postman 的 Runner Results
 */
public class ExecutionResultsPanel extends JPanel {
    public static final String TEXT_HTML = "text/html";
    private JTree resultsTree;
    private DefaultTreeModel treeModel;
    private JPanel detailPanel;
    private JTabbedPane detailTabs;
    private transient BatchExecutionHistory executionHistory; // 当前执行历史记录
    private TreePath lastSelectedPath; // 保存最后选中的路径
    private int lastSelectedRequestDetailTabIndex = 0; // 记住用户在RequestDetail中选择的tab索引

    public ExecutionResultsPanel() {
        initUI();
        registerListeners();
    }

    /**
     * 格式化时间戳为 yyyy-MM-dd HH:mm:ss 格式
     */
    private String formatTimestamp(long timestamp) {
        // 日期格式化器
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(new Date(timestamp));
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 创建分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(350);
        splitPane.setResizeWeight(0.35);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);

        // 左侧：结果树
        createResultsTree();
        splitPane.setLeftComponent(createTreePanel());

        // 右侧：详情面板
        createDetailPanel();
        splitPane.setRightComponent(detailPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private void createResultsTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_EXECUTION_RESULTS));
        treeModel = new DefaultTreeModel(root);
        resultsTree = new JTree(treeModel);
        resultsTree.setRootVisible(true);
        resultsTree.setShowsRootHandles(true);
        resultsTree.setCellRenderer(new ExecutionResultTreeCellRenderer());
        resultsTree.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
        resultsTree.setRowHeight(26);
        resultsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // 启用工具提示
        ToolTipManager.sharedInstance().registerComponent(resultsTree);
    }

    private JPanel createTreePanel() {
        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));

        // 添加标题栏（包含标题和工具按钮）
        JPanel headerPanel = createTreeHeaderPanel();
        treePanel.add(headerPanel, BorderLayout.NORTH);

        JScrollPane treeScrollPane = new JScrollPane(resultsTree);
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        treeScrollPane.setBorder(BorderFactory.createEmptyBorder());
        treePanel.add(treeScrollPane, BorderLayout.CENTER);

        return treePanel;
    }

    private JPanel createTreeHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        // 左侧标题
        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_EXECUTION_HISTORY));
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 13));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // 右侧工具按钮
        JPanel toolBar = createToolBar();
        headerPanel.add(toolBar, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createToolBar() {
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        toolBar.setOpaque(false);

        // 只使用图标按钮，更简洁
        JButton expandAllBtn = createIconButton("icons/expand.svg",
            I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TOOLTIP_EXPAND_ALL));
        expandAllBtn.addActionListener(e -> expandAll());

        JButton collapseAllBtn = createIconButton("icons/collapse.svg",
            I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TOOLTIP_COLLAPSE_ALL));
        collapseAllBtn.addActionListener(e -> collapseAll());

        JButton refreshBtn = createIconButton("icons/refresh.svg",
            I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TOOLTIP_REFRESH));
        refreshBtn.addActionListener(e -> refreshData());

        toolBar.add(expandAllBtn);
        toolBar.add(collapseAllBtn);
        toolBar.add(refreshBtn);

        return toolBar;
    }

    private JButton createIconButton(String iconPath, String tooltip) {
        JButton button = new JButton(new FlatSVGIcon(iconPath, 14, 14));
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(24, 24));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        return button;
    }

    private void createDetailPanel() {
        detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        // 创建详情选项卡
        detailTabs = new JTabbedPane();
        detailTabs.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
        detailTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // 默认显示欢迎页面
        showWelcomePanel();
        detailPanel.add(detailTabs, BorderLayout.CENTER);


        detailTabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 根据鼠标点击位置确定点击的标签页索引，而不是使用getSelectedIndex() 因为切换后可能不准了
                int clickedTabIndex = detailTabs.indexAtLocation(e.getX(), e.getY());
                if (clickedTabIndex >= 0 && clickedTabIndex < detailTabs.getTabCount()) {
                    lastSelectedRequestDetailTabIndex = clickedTabIndex;
                }
            }

        });
    }


    private void registerListeners() {
        // 鼠标事件 - 优化灵敏度
        resultsTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // 鼠标按下时立即处理选择，提高响应速度
                TreePath path = resultsTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    resultsTree.setSelectionPath(path);
                    resultsTree.scrollPathToVisible(path);
                }
            }
        });

        // 键盘事件 - 改善键盘导航
        resultsTree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });

        // 树选择事件 - 主要的内容显示逻辑
        resultsTree.addTreeSelectionListener(e -> {
            TreePath newPath = e.getNewLeadSelectionPath();
            if (newPath != null) {
                lastSelectedPath = newPath;
                updateStatus();
                // 延迟显示详情，避免频繁刷新
                SwingUtilities.invokeLater(() -> {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) newPath.getLastPathComponent();
                    showNodeDetail(node);
                });
            }
        });
    }

    private void handleMouseClick(MouseEvent e) {
        TreePath path = resultsTree.getPathForLocation(e.getX(), e.getY());
        if (path == null) {
            return;
        }

        // 确保路径被选中
        if (!resultsTree.isPathSelected(path)) {
            resultsTree.setSelectionPath(path);
        }

    }

    private void handleKeyPress(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE:
                handleTreeSelection();
                break;
            case KeyEvent.VK_LEFT:
                TreePath selectedPath = resultsTree.getSelectionPath();
                if (selectedPath != null && resultsTree.isExpanded(selectedPath)) {
                    resultsTree.collapsePath(selectedPath);
                }
                break;
            case KeyEvent.VK_RIGHT:
                TreePath selected = resultsTree.getSelectionPath();
                if (selected != null && !resultsTree.isExpanded(selected)) {
                    resultsTree.expandPath(selected);
                }
                break;
            default:
                // 不需要处理其他按键
                break;
        }
    }

    private void handleTreeSelection() {
        TreePath path = resultsTree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            showNodeDetail(node);
        }
    }

    private void updateStatus() {
        // 状态栏已移除，此方法保留用于未来扩展
    }

    /**
     * 更新执行历史数据
     */
    public void updateExecutionHistory(BatchExecutionHistory history) {
        SwingUtilities.invokeLater(() -> {
            this.executionHistory = history;
            // 重置tab索引为第一个，因为这是新的执行历史数据
            lastSelectedRequestDetailTabIndex = 0;
            rebuildTree();
        });
    }

    private void rebuildTree() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        root.removeAllChildren();

        if (executionHistory == null) {
            root.setUserObject(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_EXECUTION_RESULTS_NO_DATA));
            treeModel.nodeStructureChanged(root);
            showWelcomePanel();
            return;
        }

        // 设置根节点信息
        long totalTime = executionHistory.getExecutionTime();
        int totalIterations = executionHistory.getIterations().size();
        root.setUserObject(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_EXECUTION_RESULTS_SUMMARY,
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

        // 尝试恢复之前的选中状态
        restoreSelection();
    }

    /**
     * 选择根节点（执行结果摘要）并展开详细信息
     * 用于执行完成后自动显示结果
     */
    public void selectFirstIteration() {
        SwingUtilities.invokeLater(() -> {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
            if (root.getChildCount() > 0) {
                // 选中根节点（显示总耗时、迭代数等摘要信息）
                TreePath rootPath = new TreePath(root);

                // 选中并展开根节点
                resultsTree.setSelectionPath(rootPath);
                resultsTree.scrollPathToVisible(rootPath);
                resultsTree.expandPath(rootPath);
            }
        });
    }

    private void restoreSelection() {
        if (lastSelectedPath != null) {
            // 尝试找到相同的节点路径并选中
            SwingUtilities.invokeLater(() -> {
                TreePath newPath = findSimilarPath();
                resultsTree.setSelectionPath(newPath);
                resultsTree.scrollPathToVisible(newPath);
            });
        }
    }

    private TreePath findSimilarPath() {
        // 简单实现：尝试选中根节点下的第一个子节点
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        if (root.getChildCount() > 0) {
            DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) root.getChildAt(0);
            return new TreePath(new Object[]{root, firstChild});
        }
        return new TreePath(root);
    }

    private void refreshData() {
        if (executionHistory != null) {
            rebuildTree();
        }
    }

    private void showWelcomePanel() {
        detailTabs.removeAll();
        JPanel welcomePanel = createWelcomePanel();
        detailTabs.addTab(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TAB_OVERVIEW),
                new FlatSVGIcon("icons/info.svg", 16, 16), welcomePanel);
        detailTabs.revalidate();
        detailTabs.repaint();
    }

    private void showNodeDetail(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();

        detailTabs.removeAll();

        if (userObject instanceof IterationNodeData iterationNodeData) {
            showIterationDetail(iterationNodeData);
        } else if (userObject instanceof RequestNodeData requestNodeData) {
            showRequestDetail(requestNodeData);
        } else {
            showOverviewDetail();
        }

        detailTabs.revalidate(); // 重新验证布局
        detailTabs.repaint(); // 重绘选项卡面板
    }

    private void showOverviewDetail() {
        if (executionHistory == null) {
            detailTabs.addTab(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TAB_OVERVIEW), createWelcomePanel());
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

        detailTabs.addTab(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_DETAIL_OVERVIEW), overviewPanel);
    }

    private void showIterationDetail(IterationNodeData iterationData) {
        IterationResult iteration = iterationData.iteration;

        // 迭代概览
        JPanel iterationPanel = new JPanel(new BorderLayout());
        iterationPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 迭代信息
        JPanel infoPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_DETAIL_ITERATION_INFO)));

        infoPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_ITERATION_ROUND) + ":"));
        infoPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_ITERATION_ROUND_FORMAT, iteration.getIterationIndex() + 1)));

        infoPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_ITERATION_START_TIME) + ":"));
        infoPanel.add(new JLabel(formatTimestamp(iteration.getStartTime())));

        infoPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_ITERATION_EXECUTION_TIME) + ":"));
        infoPanel.add(new JLabel(TimeDisplayUtil.formatElapsedTime(iteration.getExecutionTime())));

        infoPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_ITERATION_REQUEST_COUNT) + ":"));
        infoPanel.add(new JLabel(String.valueOf(iteration.getRequestResults().size())));

        iterationPanel.add(infoPanel, BorderLayout.NORTH);

        // CSV 数据（如果有）
        if (iteration.getCsvData() != null && !iteration.getCsvData().isEmpty()) {
            JPanel csvPanel = createCsvDataPanel(iteration.getCsvData());
            iterationPanel.add(csvPanel, BorderLayout.CENTER);
        }

        detailTabs.addTab(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_DETAIL_ITERATION), iterationPanel);
    }

    private void showRequestDetail(RequestNodeData requestData) {
        RequestResult request = requestData.request;
        // 请求信息
        JEditorPane reqPane = new JEditorPane();
        reqPane.setContentType(TEXT_HTML);
        reqPane.setEditable(false);
        reqPane.setText(HttpHtmlRenderer.renderRequest(request.getReq()));
        reqPane.setCaretPosition(0);
        detailTabs.addTab("Request", new FlatSVGIcon("icons/http.svg", 16, 16), new JScrollPane(reqPane));

        // 响应信息
        if (request.getResponse() != null) {
            JEditorPane respPane = new JEditorPane();
            respPane.setContentType(TEXT_HTML);
            respPane.setEditable(false);
            respPane.setText(HttpHtmlRenderer.renderResponse(request.getResponse()));
            respPane.setCaretPosition(0);
            detailTabs.addTab("Response", new FlatSVGIcon("icons/check.svg", 16, 16), new JScrollPane(respPane));
        }

        // Timing & Event Info
        if (request.getResponse() != null && request.getResponse().httpEventInfo != null) {
            JEditorPane timingPane = new JEditorPane();
            timingPane.setContentType(TEXT_HTML);
            timingPane.setEditable(false);
            timingPane.setText(HttpHtmlRenderer.renderTimingInfo(request.getResponse()));
            timingPane.setCaretPosition(0);
            detailTabs.addTab("Timing", new FlatSVGIcon("icons/time.svg", 16, 16), new JScrollPane(timingPane));

            JEditorPane eventInfoPane = new JEditorPane();
            eventInfoPane.setContentType(TEXT_HTML);
            eventInfoPane.setEditable(false);
            eventInfoPane.setText(HttpHtmlRenderer.renderEventInfo(request.getResponse()));
            eventInfoPane.setCaretPosition(0);
            detailTabs.addTab("Event Info", new FlatSVGIcon("icons/detail.svg", 16, 16), new JScrollPane(eventInfoPane));
        }

        // Tests
        if (request.getTestResults() != null && !request.getTestResults().isEmpty()) {
            JEditorPane testsPane = new JEditorPane();
            testsPane.setContentType(TEXT_HTML);
            testsPane.setEditable(false);
            testsPane.setText(HttpHtmlRenderer.renderTestResults(request.getTestResults()));
            testsPane.setCaretPosition(0);
            detailTabs.addTab("Tests", new FlatSVGIcon("icons/code.svg", 16, 16), new JScrollPane(testsPane));
        }

        // 恢复上次选中的 tab
        if (lastSelectedRequestDetailTabIndex >= detailTabs.getTabCount()) {
            lastSelectedRequestDetailTabIndex = 0;
        }
        detailTabs.setSelectedIndex(lastSelectedRequestDetailTabIndex);
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
            return I18nUtil.getMessage(MessageKeys.FUNCTIONAL_ITERATION_PASSED_FORMAT,
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
                } else if (requestData.request.getAssertion() != null && !requestData.request.getAssertion().isEmpty()) {
                    // 失败：红色文字和红色取消图标
                    if (!sel) { // 只在非选中状态下设置颜色，选中时保持选中色
                        setForeground(new Color(220, 53, 69)); // 红色
                    }
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
                "<span style='font-size: 16px; color: #666;'>" + I18nUtil.getMessage(MessageKeys.FUNCTIONAL_DETAIL_WELCOME_MESSAGE) + "</span>" +
                "<br><br>" +
                "<span style='font-size: 12px; color: #999;'>" + I18nUtil.getMessage(MessageKeys.FUNCTIONAL_DETAIL_WELCOME_SUBTITLE) + "</span>" +
                "</div></html>");
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        welcomePanel.add(welcomeLabel, BorderLayout.CENTER);
        return welcomePanel;
    }

    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(2, 4, 15, 5));
        statsPanel.setBorder(BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_DETAIL_EXECUTION_STATS)));

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

        statsPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_TOTAL_ITERATIONS) + ": " + totalIterations));
        statsPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_TOTAL_REQUESTS) + ": " + totalRequests));
        statsPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_TOTAL_TIME) + ": " + TimeDisplayUtil.formatElapsedTime(totalTime)));
        statsPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_SUCCESS_RATE) + ": " + String.format("%.1f%%", successRate)));

        statsPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_START_TIME) + ": " + formatTimestamp(executionHistory.getStartTime())));
        statsPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_END_TIME) + ": " + formatTimestamp(executionHistory.getEndTime())));
        statsPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_AVERAGE_TIME) + ": " + (totalRequests > 0 ? totalTime / totalRequests : 0) + "ms"));
        statsPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_STATUS) + ": " + I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATS_STATUS_COMPLETED)));

        return statsPanel;
    }

    private JTable createSummaryTable() {
        String[] columnNames = {
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_ITERATION),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_REQUEST_NAME),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_METHOD),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_STATUS),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_TIME),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_ASSERTION),
                I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_TIMESTAMP)
        };

        java.util.List<Object[]> tableData = new java.util.ArrayList<>();
        for (IterationResult iteration : executionHistory.getIterations()) {
            for (RequestResult request : iteration.getRequestResults()) {
                Object[] row = {
                        I18nUtil.getMessage("functional.iteration.round.prefix") + (iteration.getIterationIndex() + 1) + I18nUtil.getMessage("functional.iteration.round.suffix"),
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
        table.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 11));
        table.getTableHeader().setFont(FontsUtil.getDefaultFont(Font.BOLD, 11));
        table.setRowHeight(22);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        return table;
    }

    private JPanel createCsvDataPanel(java.util.Map<String, String> csvData) {
        JPanel csvPanel = new JPanel(new BorderLayout());
        csvPanel.setBorder(BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_DETAIL_CSV_DATA)));

        JTable csvTable = new JTable();
        String[] headers = csvData.keySet().toArray(new String[0]);
        Object[][] data = new Object[1][headers.length];
        for (int i = 0; i < headers.length; i++) {
            data[0][i] = csvData.get(headers[i]);
        }

        csvTable.setModel(new javax.swing.table.DefaultTableModel(data, headers));
        csvTable.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 11));
        csvTable.getTableHeader().setFont(FontsUtil.getDefaultFont(Font.BOLD, 11));
        csvTable.setRowHeight(20);

        JScrollPane csvScrollPane = new JScrollPane(csvTable);
        csvScrollPane.setPreferredSize(new Dimension(0, 100));
        csvPanel.add(csvScrollPane, BorderLayout.CENTER);

        return csvPanel;
    }

}
