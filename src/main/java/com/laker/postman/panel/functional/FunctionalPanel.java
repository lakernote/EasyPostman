package com.laker.postman.panel.functional;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.common.component.StartButton;
import com.laker.postman.common.component.StopButton;
import com.laker.postman.model.*;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.functional.table.FunctionalRunnerTableModel;
import com.laker.postman.panel.functional.table.RunnerRowData;
import com.laker.postman.panel.functional.table.TableRowTransferHandler;
import com.laker.postman.panel.sidebar.ConsolePanel;
import com.laker.postman.panel.sidebar.SidebarTabPanel;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.collections.RequestCollectionsService;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.js.JsScriptExecutor;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.TimeDisplayUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Slf4j
public class FunctionalPanel extends SingletonBasePanel {
    private JTable table;
    private FunctionalRunnerTableModel tableModel;
    private StartButton runBtn;
    private StopButton stopBtn;    // 停止按钮
    private JLabel timeLabel;     // 执行时间标签
    private JLabel progressLabel; // 进度标签
    private long startTime;       // 记录开始时间
    private Timer executionTimer; // 执行时间计时器
    private volatile boolean isStopped = false; // 停止标志

    // CSV 数据管理面板
    private CsvDataPanel csvDataPanel;

    // 批量执行历史记录
    private transient BatchExecutionHistory executionHistory;
    private JTabbedPane mainTabbedPane;
    private ExecutionResultsPanel resultsPanel;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
        setPreferredSize(new Dimension(700, 400));

        // 创建主选项卡面板
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));

        JPanel executionPanel = new JPanel(new BorderLayout());
        executionPanel.add(createTopPanel(), BorderLayout.NORTH);
        executionPanel.add(createTablePanel(), BorderLayout.CENTER);
        mainTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TAB_REQUEST_CONFIG), new FlatSVGIcon("icons/functional.svg", 16, 16), executionPanel);

        resultsPanel = new ExecutionResultsPanel();
        mainTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TAB_EXECUTION_RESULTS), new FlatSVGIcon("icons/history.svg", 16, 16), resultsPanel);

        add(mainTabbedPane, BorderLayout.CENTER);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 12));

        // 初始化 CSV 数据面板
        csvDataPanel = new CsvDataPanel();

        // 左侧按钮面板
        topPanel.add(createButtonPanel(), BorderLayout.WEST);

        // 中间 CSV 状态面板
        topPanel.add(csvDataPanel, BorderLayout.CENTER);

        // 创建右侧信息面板，包含执行时间和进度显示
        JPanel rightPanel = new JPanel();
        // 使用更紧凑的布局
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        rightPanel.setOpaque(false);

        // 创建执行时间显示面板
        JPanel timePanel = new JPanel();
        timePanel.setLayout(new BoxLayout(timePanel, BoxLayout.X_AXIS));
        timePanel.setOpaque(false);
        JLabel timeIcon = new JLabel(new FlatSVGIcon("icons/time.svg", 20, 20));
        timeLabel = new JLabel("0 ms");
        timeLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 12));
        timePanel.add(timeIcon);
        timePanel.add(Box.createHorizontalStrut(3));
        timePanel.add(timeLabel);

        // 创建任务进度显示面板
        JPanel taskPanel = new JPanel();
        taskPanel.setLayout(new BoxLayout(taskPanel, BoxLayout.X_AXIS));
        taskPanel.setOpaque(false);
        JLabel taskIcon = new JLabel(new FlatSVGIcon("icons/functional.svg", 20, 20));

        // 创建进度文本标签
        progressLabel = new JLabel("0/0");
        progressLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 12));

        taskPanel.add(taskIcon);
        taskPanel.add(Box.createHorizontalStrut(3));
        taskPanel.add(progressLabel);

        // 添加到右侧面板，并设置间距
        rightPanel.add(timePanel);
        rightPanel.add(Box.createHorizontalStrut(10));
        rightPanel.add(taskPanel);

        topPanel.add(rightPanel, BorderLayout.EAST);

        // 固定顶部面板高度，避免挤压表格区域
        topPanel.setPreferredSize(new Dimension(700, 40));
        return topPanel;
    }

    private JPanel createButtonPanel() {
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnPanel.setOpaque(false);

        JButton loadBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_LOAD));
        loadBtn.setIcon(new FlatSVGIcon("icons/load.svg"));
        loadBtn.setPreferredSize(new Dimension(90, 28));
        loadBtn.addActionListener(e -> showLoadRequestsDialog());
        btnPanel.add(loadBtn);

        runBtn = new StartButton();
        runBtn.addActionListener(e -> {
            runSelectedRequests();
            stopBtn.setEnabled(true);
        });
        btnPanel.add(runBtn);

        stopBtn = new StopButton();
        stopBtn.addActionListener(e -> {
            isStopped = true;
            if (executionTimer != null && executionTimer.isRunning()) {
                executionTimer.stop();
            }
            stopBtn.setEnabled(false);
            runBtn.setEnabled(true);
        });
        btnPanel.add(stopBtn);

        JButton clearBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));
        clearBtn.setIcon(new FlatSVGIcon("icons/clear.svg"));
        clearBtn.setPreferredSize(new Dimension(95, 28));
        clearBtn.addActionListener(e -> {
            tableModel.clear();
            runBtn.setEnabled(false);
            stopBtn.setEnabled(false);
            resetProgress();
            resultsPanel.updateExecutionHistory(null);
        });
        btnPanel.add(clearBtn);

        return btnPanel;
    }

    // 批量运行
    private void runSelectedRequests() {
        isStopped = false; // 开始运行时重置停止标志
        int rowCount = tableModel.getRowCount();
        int selectedCount = (int) IntStream.range(0, rowCount).mapToObj(i -> tableModel.getRow(i)).filter(row -> row != null && row.selected).count();
        if (selectedCount == 0) {
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MSG_NO_RUNNABLE_REQUEST), I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 检查是否使用 CSV 数据
        int iterations = 1;
        if (csvDataPanel.hasData()) {
            iterations = csvDataPanel.getRowCount();
            int response = JOptionPane.showConfirmDialog(this,
                    I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MSG_CSV_DETECTED, iterations),
                    I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MSG_CSV_TITLE),
                    JOptionPane.YES_NO_OPTION);
            if (response != JOptionPane.YES_OPTION) {
                iterations = 1; // 用户选择不使用 CSV 数据
            }
        }

        // 创建新的执行历史记录
        final int totalExecutions = selectedCount * iterations;
        executionHistory = new BatchExecutionHistory();
        executionHistory.setTotalIterations(iterations);
        executionHistory.setTotalRequests(totalExecutions);

        clearRunResults(rowCount);
        runBtn.setEnabled(false);

        progressLabel.setText("0/" + totalExecutions);

        startTime = System.currentTimeMillis(); // 记录开始时间
        executionTimer = new Timer(100, e -> updateExecutionTime());
        executionTimer.start(); // 启动计时器

        final int finalIterations = iterations;
        new Thread(() -> executeBatchRequestsWithCsv(rowCount, selectedCount, finalIterations)).start();
    }

    private void clearRunResults(int rowCount) {
        for (int i = 0; i < rowCount; i++) {
            RunnerRowData row = tableModel.getRow(i);
            if (row != null) {
                row.response = null;
                row.cost = 0;
                row.status = null;
                row.assertion = null;
                row.testResults = null;
                tableModel.fireTableRowsUpdated(i, i);
            }
        }
    }

    private void executeBatchRequestsWithCsv(int rowCount, int selectedCount, int iterations) {
        int totalFinished = 0;

        for (int iteration = 0; iteration < iterations && !isStopped; iteration++) {
            // 获取当前迭代的 CSV 数据
            Map<String, String> currentCsvRow = getCsvDataForIteration(iteration);

            // 创建当前迭代的结果记录
            IterationResult iterationResult = new IterationResult(iteration, currentCsvRow);

            totalFinished = processIterationRequests(rowCount, selectedCount, iterations, totalFinished, iterationResult, currentCsvRow);

            if (isStopped) break;

            // 完成当前迭代并添加到历史记录
            iterationResult.complete();
            executionHistory.addIteration(iterationResult);

            // 实时更新结果面板
            SwingUtilities.invokeLater(() -> resultsPanel.updateExecutionHistory(executionHistory));
        }

        // 完成整个批量执行
        executionHistory.complete();
        finalizeExecution();
    }

    private Map<String, String> getCsvDataForIteration(int iteration) {
        if (csvDataPanel.hasData() && iteration < csvDataPanel.getRowCount()) {
            return csvDataPanel.getRowData(iteration);
        }
        return java.util.Collections.emptyMap();
    }

    private int processIterationRequests(int rowCount, int selectedCount, int iterations,
                                         int totalFinished, IterationResult iterationResult,
                                         Map<String, String> currentCsvRow) {
        int finished = totalFinished;

        for (int i = 0; i < rowCount && !isStopped; i++) {
            RunnerRowData row = tableModel.getRow(i);

            if (!isValidRow(row)) {
                continue;
            }

            if (row.selected) {
                finished = executeAndRecordRequest(row, currentCsvRow, iterationResult, finished, selectedCount, iterations);
            }
        }

        return finished;
    }

    private boolean isValidRow(RunnerRowData row) {
        if (row == null || row.requestItem == null || row.preparedRequest == null) {
            log.warn("Row is invalid, skipping execution");
            return false;
        }
        return true;
    }

    private int executeAndRecordRequest(RunnerRowData row, Map<String, String> currentCsvRow,
                                        IterationResult iterationResult, int totalFinished,
                                        int selectedCount, int iterations) {
        // 找到当前行的索引
        int rowIndex = -1;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getRow(i) == row) {
                rowIndex = i;
                break;
            }
        }

        // 高亮当前执行的行
        final int currentRowIndex = rowIndex;
        if (currentRowIndex >= 0) {
            SwingUtilities.invokeLater(() -> {
                table.setRowSelectionInterval(currentRowIndex, currentRowIndex);
                table.scrollRectToVisible(table.getCellRect(currentRowIndex, 0, true));
            });
        }

        BatchResult result = executeSingleRequestWithCsv(row, currentCsvRow);

        // 更新表格中的执行结果
        if (currentRowIndex >= 0) {
            row.status = result.status;
            row.cost = result.cost;
            row.assertion = result.assertion;
            row.response = result.resp;

            SwingUtilities.invokeLater(() -> {
                tableModel.fireTableRowsUpdated(currentRowIndex, currentRowIndex);
            });
        }

        // 记录请求结果到执行历史
        RequestResult requestResult = new RequestResult(
                row.requestItem.getName(),
                row.requestItem.getMethod(),
                row.preparedRequest.url,
                result.req,
                result.resp,
                result.cost,
                result.status,
                result.assertion,
                row.testResults
        );
        iterationResult.addRequestResult(requestResult);

        int newTotalFinished = totalFinished + 1;
        SwingUtilities.invokeLater(() -> progressLabel.setText(newTotalFinished + "/" + (selectedCount * iterations)));

        return newTotalFinished;
    }

    private void finalizeExecution() {
        SwingUtilities.invokeLater(() -> {
            runBtn.setEnabled(true);
            // 停止计时器
            stopExecutionTimer();

            // 最终更新结果面板
            resultsPanel.updateExecutionHistory(executionHistory);

            // 如果执行完成，切换到结果面板
            if (!isStopped) {
                mainTabbedPane.setSelectedIndex(1); // 切换到执行结果面板
            }
        });
    }

    private void stopExecutionTimer() {
        if (executionTimer != null && executionTimer.isRunning()) {
            executionTimer.stop();
        }
    }

    private static class BatchResult {
        PreparedRequest req;
        HttpResponse resp;
        long cost;
        String status;
        String assertion;
    }

    private BatchResult executeSingleRequestWithCsv(RunnerRowData row, Map<String, String> csvRowData) {
        if (isStopped) return new BatchResult(); // 检查停止标志，直接返回空结果
        BatchResult result = new BatchResult();
        long start = System.currentTimeMillis();
        HttpRequestItem item = row.requestItem;

        // 每次执行都重新构建PreparedRequest，避免变量污染
        PreparedRequest req = PreparedRequestBuilder.build(item);
        result.req = req;
        // 每次执行前清理临时变量
        EnvironmentService.clearTemporaryVariables();

        Map<String, Object> bindings = HttpUtil.prepareBindings(req);
        Postman pm = (Postman) bindings.get("pm");

        // 添加 CSV 数据到脚本执行环境 - 使用 Postman 标准方式
        if (csvRowData != null) {
            // Postman 的标准方式：CSV 数据通过 pm.variables.get() 访问
            for (Map.Entry<String, String> entry : csvRowData.entrySet()) {
                pm.setVariable(entry.getKey(), entry.getValue());
            }
        }

        boolean preOk = runPreScriptWithCsv(item, bindings);

        // 前置脚本执行完成后，进行变量替换
        if (preOk) {
            PreparedRequestBuilder.replaceVariablesAfterPreScript(req);
        }


        HttpResponse resp = null;
        String status;
        String assertion = I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATUS_NOT_EXECUTED);
        if (!preOk) {
            status = I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATUS_PRE_SCRIPT_FAILED);
        } else if (HttpUtil.isSSERequest(req)) {
            status = I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATUS_SSE_BATCH_NOT_SUPPORTED);
        } else if (item.getProtocol().isWebSocketProtocol()) {
            status = I18nUtil.getMessage(MessageKeys.FUNCTIONAL_STATUS_WS_BATCH_NOT_SUPPORTED);
        } else {
            try {
                req.logEvent = true; // 确保日志事件开启
                resp = HttpSingleRequestExecutor.executeHttp(req);
                status = String.valueOf(resp.code);
                assertion = runPostScriptAndAssertWithCsv(item, bindings, resp, row, pm);
            } catch (Exception ex) {
                log.error("请求执行失败", ex);
                ConsolePanel.appendLog("[Request Error]\n" + ex.getMessage(), ConsolePanel.LogType.ERROR);
                status = ex.getMessage();
                assertion = ex.getMessage();
            }
        }
        long cost = System.currentTimeMillis() - start;
        result.resp = resp;
        result.cost = resp == null ? cost : resp.costMs;
        result.status = status;
        result.assertion = assertion;
        return result;
    }

    private boolean runPreScriptWithCsv(HttpRequestItem item, Map<String, Object> bindings) {
        String prescript = item.getPrescript();
        if (prescript != null && !prescript.isBlank()) {
            try {
                JsScriptExecutor.executeScript(
                        prescript,
                        bindings,
                        output -> {
                            if (!output.isBlank()) {
                                ConsolePanel.appendLog("[PreScript Console]\n" + output);
                            }
                        }
                );
                return true;
            } catch (Exception ex) {
                log.error("前置脚本执行异常: {}", ex.getMessage(), ex);
                return false;
            }
        }
        return true;
    }

    private String runPostScriptAndAssertWithCsv(HttpRequestItem item, Map<String, Object> bindings, HttpResponse resp, RunnerRowData row, Postman pm) {
        String assertion = "Pass";
        String postscript = item.getPostscript();
        if (postscript != null && !postscript.isBlank()) {
            HttpUtil.postBindings(bindings, resp);
            try {
                JsScriptExecutor.executeScript(
                        postscript,
                        bindings,
                        output -> {
                            if (!output.isBlank()) {
                                ConsolePanel.appendLog("[PostScript Console]\n" + output);
                            }
                        }
                );
                row.testResults = new java.util.ArrayList<>();
                if (pm.testResults != null) {
                    row.testResults.addAll(pm.testResults);
                }
            } catch (Exception assertionEx) {
                assertion = assertionEx.getMessage();
                row.testResults = new java.util.ArrayList<>();
                if (pm.testResults != null) {
                    row.testResults.addAll(pm.testResults);
                }
            }
        }
        return assertion;
    }

    // 更新执行时间显示
    private void updateExecutionTime() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        timeLabel.setText(TimeDisplayUtil.formatElapsedTime(elapsedTime));
    }

    // 重置进度和时间显示
    private void resetProgress() {
        // 如果计时器在运行，停止它
        if (executionTimer != null && executionTimer.isRunning()) {
            executionTimer.stop();
        }

        // 重置标签文本
        timeLabel.setText("0 ms");
        progressLabel.setText("0/0");
    }

    private JScrollPane createTablePanel() {
        tableModel = new FunctionalRunnerTableModel();
        table = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // 只允许第一列（选中列）可编辑
            }
        };
        table.setRowHeight(28);
        table.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
        table.getTableHeader().setFont(FontsUtil.getDefaultFont(Font.BOLD, 13));

        // 添加表头点击监听器，点击"选择"列表头时全选/反选
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                if (column == 0) { // 点击选择列
                    boolean hasSelected = tableModel.hasSelectedRows();
                    tableModel.setAllSelected(!hasSelected);
                }
            }
        });

        setTableColumnWidths();
        setTableRenderers();
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(new Color(220, 235, 252));
        table.setSelectionForeground(Color.BLACK);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);
        table.setTransferHandler(new TableRowTransferHandler(table));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        return scrollPane;
    }

    private void setTableColumnWidths() {
        if (table.getColumnModel().getColumnCount() > 0) {
            // Select column
            table.getColumnModel().getColumn(0).setMinWidth(50);
            table.getColumnModel().getColumn(0).setMaxWidth(60);
            table.getColumnModel().getColumn(0).setPreferredWidth(55);
            // Method column
            table.getColumnModel().getColumn(3).setMinWidth(60);
            table.getColumnModel().getColumn(3).setMaxWidth(80);
            table.getColumnModel().getColumn(3).setPreferredWidth(70);
            // Status column
            table.getColumnModel().getColumn(4).setMinWidth(60);
            table.getColumnModel().getColumn(4).setMaxWidth(80);
            table.getColumnModel().getColumn(4).setPreferredWidth(70);
            // Time column
            table.getColumnModel().getColumn(5).setMinWidth(70);
            table.getColumnModel().getColumn(5).setMaxWidth(100);
            table.getColumnModel().getColumn(5).setPreferredWidth(80);
            // Result column
            table.getColumnModel().getColumn(6).setMinWidth(80);
            table.getColumnModel().getColumn(6).setMaxWidth(150);
            table.getColumnModel().getColumn(6).setPreferredWidth(100);
        }
    }

    private void setTableRenderers() {
        table.getColumnModel().getColumn(3).setCellRenderer(createMethodRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(createStatusRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(createResultRenderer());
    }

    private DefaultTableCellRenderer createMethodRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String color = HttpUtil.getMethodColor(value.toString());
                    c.setForeground(Color.decode(color));
                }
                return c;
            }
        };
    }

    private DefaultTableCellRenderer createStatusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (value != null && !"-".equals(value)) {
                    applyStatusColors(c, value.toString(), isSelected);
                }
                setHorizontalAlignment(CENTER);
                return c;
            }
        };
    }

    /**
     * 根据状态码应用颜色
     */
    private void applyStatusColors(Component c, String status, boolean isSelected) {
        try {
            int code = Integer.parseInt(status);
            if (code >= 200 && code < 300) {
                c.setForeground(new Color(34, 139, 34));
                if (!isSelected) c.setBackground(new Color(240, 255, 240));
            } else if (code >= 400 && code < 500) {
                c.setForeground(new Color(255, 140, 0));
                if (!isSelected) c.setBackground(new Color(255, 250, 230));
            } else if (code >= 500) {
                c.setForeground(new Color(220, 20, 60));
                if (!isSelected) c.setBackground(new Color(255, 240, 245));
            }
        } catch (NumberFormatException e) {
            c.setForeground(Color.RED);
            if (!isSelected) c.setBackground(new Color(255, 240, 245));
        }
    }

    private DefaultTableCellRenderer createResultRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null && !"-".equals(value)) {
                    String result = value.toString();
                    if ("Pass".equalsIgnoreCase(result) || result.isEmpty()) {
                        c.setForeground(new Color(34, 139, 34)); // Green for Pass
                        setText("✓ Pass");
                    } else {
                        c.setForeground(new Color(220, 20, 60)); // Red for Fail
                        setText("✗ Fail");
                    }
                }

                setHorizontalAlignment(CENTER);
                return c;
            }
        };
    }

    @Override
    protected void registerListeners() {
        // 添加表格鼠标监听器
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) { // 双击
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        showRequestDetail(row);
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) { // 右键
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        showTableContextMenu(e, row);
                    }
                }
            }
        });
    }

    /**
     * 显示请求详情
     */
    private void showRequestDetail(int rowIndex) {
        RunnerRowData row = tableModel.getRow(rowIndex);
        if (row != null && row.requestItem != null) {
            // 打开请求编辑面板
           RequestEditPanel editPanel =
                    SingletonFactory.getInstance(RequestEditPanel.class);
            editPanel.showOrCreateTab(row.requestItem);

            // 切换到Collections标签
           SidebarTabPanel sidebarPanel =
                    SingletonFactory.getInstance(SidebarTabPanel.class);
            sidebarPanel.getTabbedPane().setSelectedIndex(0);
        }
    }

    /**
     * 显示表格右键菜单
     */
    private void showTableContextMenu(java.awt.event.MouseEvent e, int rowIndex) {
        JPopupMenu menu = new JPopupMenu();

        // 查看详情
        JMenuItem viewItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MENU_VIEW_DETAIL));
        viewItem.setIcon(new FlatSVGIcon("icons/detail.svg", 16, 16));
        viewItem.addActionListener(evt -> showRequestDetail(rowIndex));
        menu.add(viewItem);

        menu.addSeparator();

        // 移除当前行
        JMenuItem deleteItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MENU_REMOVE));
        deleteItem.setIcon(new FlatSVGIcon("icons/close.svg", 16, 16));
        deleteItem.addActionListener(evt -> {
            tableModel.removeRow(rowIndex);
            if (tableModel.getRowCount() == 0) {
                runBtn.setEnabled(false);
            }
        });
        menu.add(deleteItem);

        // 移除选中项（如果有选中的行）
        if (tableModel.hasSelectedRows()) {
            JMenuItem deleteSelectedItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.FUNCTIONAL_MENU_REMOVE_SELECTED));
            deleteSelectedItem.setIcon(new FlatSVGIcon("icons/clear.svg", 16, 16));
            deleteSelectedItem.addActionListener(evt -> {
                int removed = tableModel.removeSelectedRows();
                if (removed > 0) {
                    if (tableModel.getRowCount() == 0) {
                        runBtn.setEnabled(false);
                    }
                }
            });
            menu.add(deleteSelectedItem);
        }

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    // 弹出选择请求/分组对话框
    private void showLoadRequestsDialog() {
        RequestCollectionsService.showMultiSelectRequestDialog(
                selected -> {
                    if (selected == null || selected.isEmpty()) return;
                    loadRequests(selected);
                }
        );
    }

    // 加载选中的请求到表格
    public void loadRequests(List<HttpRequestItem> requests) {
        for (HttpRequestItem item : requests) {
            // 不在这里进行变量替换，延迟到前置脚本执行后
            PreparedRequest req = PreparedRequestBuilder.build(item);
            tableModel.addRow(new RunnerRowData(item, req));
        }
        table.setEnabled(true);
        runBtn.setEnabled(true);
    }
}