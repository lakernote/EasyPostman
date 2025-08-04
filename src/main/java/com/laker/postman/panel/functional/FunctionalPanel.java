package com.laker.postman.panel.functional;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.component.StartButton;
import com.laker.postman.common.component.StopButton;
import com.laker.postman.common.panel.SingletonBasePanel;
import com.laker.postman.model.*;
import com.laker.postman.panel.SidebarTabPanel;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.functional.table.RunnerRowData;
import com.laker.postman.panel.functional.table.RunnerTableModel;
import com.laker.postman.panel.functional.table.TableRowTransferHandler;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.js.JsScriptExecutor;
import com.laker.postman.util.CsvDataUtil;
import com.laker.postman.util.FontUtil;
import com.laker.postman.util.TimeDisplayUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Slf4j
public class FunctionalPanel extends SingletonBasePanel {
    private JTable table;
    private RunnerTableModel tableModel;
    private StartButton runBtn;
    private StopButton stopBtn;    // 停止按钮
    private JLabel timeLabel;     // 执行时间标签
    private JLabel progressLabel; // 进度标签
    private long startTime;       // 记录开始时间
    private Timer executionTimer; // 执行时间计时器
    private volatile boolean isStopped = false; // 停止标志

    // CSV 相关字段
    private File csvFile;
    private List<Map<String, String>> csvData;
    private List<String> csvHeaders; // 保存CSV列标题的顺序
    private JPanel csvStatusPanel;  // CSV 状态显示面板
    private JLabel csvStatusLabel;  // CSV 状态标签

    // 批量执行历史记录
    private BatchExecutionHistory executionHistory;
    private JTabbedPane mainTabbedPane;
    private ExecutionResultsPanel resultsPanel;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
        setPreferredSize(new Dimension(700, 400));

        // 创建主选项卡面板
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(FontUtil.getDefaultFont(Font.PLAIN, 12));

        // 执行面板（原有功能）
        JPanel executionPanel = new JPanel(new BorderLayout());
        executionPanel.add(createTopPanel(), BorderLayout.NORTH);
        executionPanel.add(createTablePanel(), BorderLayout.CENTER);
        mainTabbedPane.addTab("请求配置", new FlatSVGIcon("icons/functional.svg", 16, 16), executionPanel);

        // 结果面板（新增）
        resultsPanel = new ExecutionResultsPanel();
        mainTabbedPane.addTab("执行结果", new FlatSVGIcon("icons/history.svg", 16, 16), resultsPanel);

        add(mainTabbedPane, BorderLayout.CENTER);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 12));

        // 左侧按钮面板
        topPanel.add(createButtonPanel(), BorderLayout.WEST);

        // 中间 CSV 状态面板
        topPanel.add(createCsvStatusPanel(), BorderLayout.CENTER);

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
        timeLabel.setFont(FontUtil.getDefaultFont(Font.BOLD, 12));
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
        progressLabel.setFont(FontUtil.getDefaultFont(Font.BOLD, 12));

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

    /**
     * 创建 CSV 状态显示面板
     */
    private JPanel createCsvStatusPanel() {
        csvStatusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        csvStatusPanel.setOpaque(false);
        csvStatusPanel.setVisible(false); // 初始隐藏

        // CSV 状态图标和文本
        JLabel csvIcon = new JLabel(new FlatSVGIcon("icons/csv.svg", 16, 16));
        csvStatusLabel = new JLabel("未加载 CSV 数据");
        csvStatusLabel.setFont(FontUtil.getDefaultFont(Font.PLAIN, 11));
        csvStatusLabel.setForeground(new Color(100, 100, 100));

        // CSV 清除按钮
        JButton csvClearBtn = new JButton();
        csvClearBtn.setIcon(new FlatSVGIcon("icons/close.svg", 14, 14));
        csvClearBtn.setPreferredSize(new Dimension(20, 20));
        csvClearBtn.setToolTipText("清除 CSV 数据");
        csvClearBtn.setBorderPainted(false);
        csvClearBtn.setContentAreaFilled(false);
        csvClearBtn.setFocusPainted(false);
        csvClearBtn.addActionListener(e -> clearCsvData());

        csvStatusPanel.add(csvIcon);
        csvStatusPanel.add(csvStatusLabel);
        csvStatusPanel.add(csvClearBtn);

        return csvStatusPanel;
    }

    private JPanel createButtonPanel() {
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnPanel.setOpaque(false);

        JButton loadBtn = new JButton("Load");
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

        JButton clearBtn = new JButton("Clear");
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

        JButton csvBtn = createCsvMenuButton();
        btnPanel.add(csvBtn);

        return btnPanel;
    }

    /**
     * 创建带下拉菜单的 CSV 按钮
     */
    private JButton createCsvMenuButton() {
        JButton csvBtn = new JButton("CSV");
        csvBtn.setIcon(new FlatSVGIcon("icons/csv.svg", 16, 16));
        csvBtn.setPreferredSize(new Dimension(90, 28));

        JPopupMenu csvMenu = new JPopupMenu();

        JMenuItem loadCsvItem = new JMenuItem("导入 CSV 文件", new FlatSVGIcon("icons/import.svg", 16, 16));
        loadCsvItem.addActionListener(e -> showEnhancedCsvManagementDialog());
        csvMenu.add(loadCsvItem);

        JMenuItem manageCsvItem = new JMenuItem("管理 CSV 数据", new FlatSVGIcon("icons/code.svg", 16, 16));
        manageCsvItem.addActionListener(e -> showCsvDataManageDialog());
        manageCsvItem.setEnabled(false); // 默认禁用，有数据时启用
        csvMenu.add(manageCsvItem);

        csvMenu.addSeparator();

        JMenuItem clearCsvItem = new JMenuItem("清除 CSV 数据", new FlatSVGIcon("icons/clear.svg", 16, 16));
        clearCsvItem.addActionListener(e -> clearCsvData());
        clearCsvItem.setEnabled(false); // 默认禁用，有数据时启用
        csvMenu.add(clearCsvItem);

        csvBtn.addActionListener(e -> {
            // 更新菜单项状态
            boolean hasCsvData = csvData != null && !csvData.isEmpty();
            manageCsvItem.setEnabled(hasCsvData);
            clearCsvItem.setEnabled(hasCsvData);

            // 显示菜单
            csvMenu.show(csvBtn, 0, csvBtn.getHeight());
        });

        return csvBtn;
    }

    /**
     * 清除 CSV 数据
     */
    private void clearCsvData() {
        csvFile = null;
        csvData = null;
        csvHeaders = null;
        updateCsvStatus();

        JOptionPane.showMessageDialog(this,
                "CSV 数据已清除",
                "信息",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * CSV 数据管理对话框 - 集成预览和编辑功能
     */
    private void showCsvDataManageDialog() {
        if (csvData == null || csvData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可管理的 CSV 数据", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog manageDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "CSV 数据管理", true);
        manageDialog.setSize(1000, 700);
        manageDialog.setLocationRelativeTo(this);
        manageDialog.setLayout(new BorderLayout());

        // 顶部信息面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel("CSV 数据管理");
        titleLabel.setFont(FontUtil.getDefaultFont(Font.BOLD, 16));
        topPanel.add(titleLabel, BorderLayout.WEST);

        JLabel infoLabel = new JLabel(String.format(
                "<html>数据来源: <b>%s</b> | 行数: <b>%d</b> | 支持直接编辑</html>",
                csvFile != null ? csvFile.getName() : "手动创建",
                csvData.size()));
        infoLabel.setFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
        topPanel.add(infoLabel, BorderLayout.CENTER);

        manageDialog.add(topPanel, BorderLayout.NORTH);

        // 创建可编辑的表格
        List<String> headers;
        try {
            if (csvHeaders != null && !csvHeaders.isEmpty()) {
                // 优先使用保存的列标题顺序
                headers = new java.util.ArrayList<>(csvHeaders);
            } else if (csvFile != null) {
                headers = CsvDataUtil.getCsvHeaders(csvFile);
                csvHeaders = headers; // 保存列标题顺序
            } else {
                // 从现有数据中获取列名，使用LinkedHashMap保持顺序
                if (csvData.isEmpty()) {
                    headers = new java.util.ArrayList<>();
                } else {
                    // 如果数据使用的是LinkedHashMap，keySet()会保持插入顺序
                    headers = new java.util.ArrayList<>(csvData.get(0).keySet());
                    csvHeaders = headers; // 保存列标题顺序
                }
            }
        } catch (Exception e) {
            headers = csvHeaders != null ? new java.util.ArrayList<>(csvHeaders) :
                    (csvData.isEmpty() ? new java.util.ArrayList<>() : new java.util.ArrayList<>(csvData.get(0).keySet()));
        }

        // 创建表格数据
        Object[][] tableData = new Object[Math.max(csvData.size(), 5)][headers.size()];
        for (int i = 0; i < csvData.size(); i++) {
            Map<String, String> row = csvData.get(i);
            for (int j = 0; j < headers.size(); j++) {
                tableData[i][j] = row.get(headers.get(j));
            }
        }

        // 创建可编辑的表格模型
        javax.swing.table.DefaultTableModel editTableModel = new javax.swing.table.DefaultTableModel(tableData, headers.toArray()) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true; // 所有单元格都可编辑
            }
        };

        JTable csvTable = new JTable(editTableModel);
        csvTable.setFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
        csvTable.getTableHeader().setFont(FontUtil.getDefaultFont(Font.BOLD, 12));
        csvTable.setRowHeight(28);
        csvTable.setGridColor(new Color(220, 220, 220));
        csvTable.setSelectionBackground(new Color(220, 235, 252));
        csvTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // 设置列宽
        for (int i = 0; i < csvTable.getColumnCount(); i++) {
            csvTable.getColumnModel().getColumn(i).setPreferredWidth(120);
        }

        JScrollPane scrollPane = new JScrollPane(csvTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));
        manageDialog.add(scrollPane, BorderLayout.CENTER);

        // 底部面板
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

        // 工具栏
        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addRowBtn = new JButton("添加行");
        addRowBtn.setIcon(new FlatSVGIcon("icons/plus.svg", 16, 16));
        List<String> finalHeaders = headers;
        addRowBtn.addActionListener(e -> editTableModel.addRow(new Object[finalHeaders.size()]));

        JButton deleteRowBtn = new JButton("删除行");
        deleteRowBtn.setIcon(new FlatSVGIcon("icons/clear.svg", 16, 16));
        deleteRowBtn.addActionListener(e -> {
            int[] selectedRows = csvTable.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(manageDialog, "请先选择要删除的行", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(manageDialog,
                    String.format("确定要删除选中的 %d 行数据吗？", selectedRows.length),
                    "确认删除", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    editTableModel.removeRow(selectedRows[i]);
                }
            }
        });

        JButton addColumnBtn = new JButton("添加列");
        addColumnBtn.setIcon(new FlatSVGIcon("icons/plus.svg", 16, 16));
        addColumnBtn.addActionListener(e -> {
            String columnName = JOptionPane.showInputDialog(manageDialog, "请输入新列名:", "添加列", JOptionPane.PLAIN_MESSAGE);
            if (columnName != null && !columnName.trim().isEmpty()) {
                columnName = columnName.trim();
                editTableModel.addColumn(columnName);
                csvTable.getColumnModel().getColumn(csvTable.getColumnCount() - 1).setPreferredWidth(120);
            }
        });

        toolPanel.add(addRowBtn);
        toolPanel.add(deleteRowBtn);
        toolPanel.add(addColumnBtn);

        bottomPanel.add(toolPanel, BorderLayout.NORTH);

        // 使用说明
        JPanel helpPanel = new JPanel(new BorderLayout());
        helpPanel.setBorder(BorderFactory.createTitledBorder("使用说明"));
        JTextArea helpText = new JTextArea(
                "• 双击单元格可直接编辑内容\n" +
                        "• 支持两种使用方式：{{列名}} 占位符 或 pm.variables.get('列名')\n" +
                        "• 例如：URL中使用 {{baseUrl}}/users 或脚本中使用 pm.variables.get('userId')");
        helpText.setFont(FontUtil.getDefaultFont(Font.PLAIN, 11));
        helpText.setEditable(false);
        helpText.setOpaque(false);
        helpText.setLineWrap(true);
        helpText.setWrapStyleWord(true);
        helpPanel.add(helpText, BorderLayout.CENTER);
        bottomPanel.add(helpPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton saveBtn = new JButton("保存");
        saveBtn.setIcon(new FlatSVGIcon("icons/save.svg", 16, 16));
        saveBtn.addActionListener(e -> {
            try {
                // 将表格数据转换为 CSV 数据格式
                List<Map<String, String>> newCsvData = new java.util.ArrayList<>();

                // 获取当前的列名
                List<String> currentHeaders = new java.util.ArrayList<>();
                for (int i = 0; i < editTableModel.getColumnCount(); i++) {
                    currentHeaders.add(editTableModel.getColumnName(i));
                }

                // 转换每一行数据
                for (int i = 0; i < editTableModel.getRowCount(); i++) {
                    Map<String, String> rowData = new java.util.LinkedHashMap<>();
                    boolean hasData = false;

                    for (int j = 0; j < currentHeaders.size(); j++) {
                        Object value = editTableModel.getValueAt(i, j);
                        String strValue = value != null ? value.toString().trim() : "";
                        rowData.put(currentHeaders.get(j), strValue);
                        if (!strValue.isEmpty()) {
                            hasData = true;
                        }
                    }

                    if (hasData) {
                        newCsvData.add(rowData);
                    }
                }

                if (newCsvData.isEmpty()) {
                    JOptionPane.showMessageDialog(manageDialog, "没有有效的数据行，请至少添加一行数据", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // 更新 CSV 数据和列标题顺序
                csvData = newCsvData;
                csvHeaders = currentHeaders; // 保存列标题顺序
                csvFile = null; // 清除原文件引用，表示这是手动编辑的数据

                updateCsvStatus();

                JOptionPane.showMessageDialog(manageDialog,
                        String.format("数据已保存！共 %d 行数据，%d 列", newCsvData.size(), currentHeaders.size()),
                        "保存成功", JOptionPane.INFORMATION_MESSAGE);

                manageDialog.dispose();

            } catch (Exception ex) {
                log.error("保存 CSV 数据失败", ex);
                JOptionPane.showMessageDialog(manageDialog, "保存数据失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelBtn = new JButton("取消");
        cancelBtn.addActionListener(e -> manageDialog.dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        manageDialog.add(bottomPanel, BorderLayout.SOUTH);
        manageDialog.setVisible(true);
    }

    /**
     * 更新 CSV 状态显示
     */
    private void updateCsvStatus() {
        if (csvData == null || csvData.isEmpty()) {
            csvStatusPanel.setVisible(false);
        } else {
            csvStatusLabel.setText(String.format("CSV: %s (%d 行数据)",
                    csvFile != null ? csvFile.getName() : "手动创建",
                    csvData.size()));
            csvStatusLabel.setForeground(new Color(0, 128, 0)); // 绿色表示已加载
            csvStatusPanel.setVisible(true);
        }
        revalidate();
        repaint();
    }

    private JScrollPane createTablePanel() {
        tableModel = new RunnerTableModel();
        table = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 7;
            }
        };
        table.setRowHeight(28);
        table.setFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
        table.getTableHeader().setFont(FontUtil.getDefaultFont(Font.BOLD, 13));
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
            table.getColumnModel().getColumn(0).setMinWidth(50);
            table.getColumnModel().getColumn(0).setMaxWidth(60);
            table.getColumnModel().getColumn(0).setPreferredWidth(55);
            table.getColumnModel().getColumn(3).setMinWidth(60);
            table.getColumnModel().getColumn(3).setMaxWidth(80);
            table.getColumnModel().getColumn(3).setPreferredWidth(70);
        }
    }

    private void setTableRenderers() {
        table.getColumnModel().getColumn(3).setCellRenderer(createMethodRenderer());
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

    @Override
    protected void registerListeners() {

    }

    // 弹出选择请求/分组对话框
    private void showLoadRequestsDialog() {
        RequestCollectionsLeftPanel.showMultiSelectRequestDialog(
                selected -> {
                    if (selected == null || selected.isEmpty()) return;
                    loadRequests(selected);
                }
        );
    }

    // 加载选中的请求到表格
    public void loadRequests(List<HttpRequestItem> requests) {
        tableModel.clear();
        for (HttpRequestItem item : requests) {
            // 不在这里进行变量替换，延迟到前置脚本执行后
            PreparedRequest req = PreparedRequestBuilder.build(item);
            tableModel.addRow(new RunnerRowData(item, req));
        }
        table.setEnabled(true);
        runBtn.setEnabled(true);
    }

    // 批量运行
    private void runSelectedRequests() {
        isStopped = false; // 开始运行时重置停止标志
        int rowCount = tableModel.getRowCount();
        int selectedCount = (int) IntStream.range(0, rowCount).mapToObj(i -> tableModel.getRow(i)).filter(row -> row != null && row.selected).count();
        if (selectedCount == 0) {
            JOptionPane.showMessageDialog(this, "没有可运行的请求", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 检查是否使用 CSV 数据
        int iterations = 1;
        if (csvData != null && !csvData.isEmpty()) {
            iterations = csvData.size();
            int response = JOptionPane.showConfirmDialog(this,
                    String.format("检测到 CSV 数据文件，包含 %d 行数据。\n是否使用 CSV 数据进行数据驱动测试？\n选择'是'将为每行数据执行一次所有选中的请求。", iterations),
                    "CSV 数据驱动测试",
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

        for (int iteration = 0; iteration < iterations; iteration++) {
            if (isStopped) break; // 检查停止标志

            // 获取当前迭代的 CSV 数据
            Map<String, String> currentCsvRow = null;
            if (csvData != null && !csvData.isEmpty() && iteration < csvData.size()) {
                currentCsvRow = csvData.get(iteration);
            }

            // 创建当前迭代的结果记录
            BatchExecutionHistory.IterationResult iterationResult =
                    new BatchExecutionHistory.IterationResult(iteration, currentCsvRow);

            for (int i = 0; i < rowCount; i++) {
                if (isStopped) break; // 检查停止标志

                RunnerRowData row = tableModel.getRow(i);
                if (row == null || row.requestItem == null || row.preparedRequest == null) {
                    log.warn("Row {} is invalid, skipping execution", i);
                    continue; // 跳过无效行
                }

                if (row.selected) {
                    BatchResult result = executeSingleRequestWithCsv(row, currentCsvRow);

                    // 记录请求结果到执行历史
                    BatchExecutionHistory.RequestResult requestResult = new BatchExecutionHistory.RequestResult(
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

                    totalFinished++;
                    int finalTotalFinished = totalFinished;
                    SwingUtilities.invokeLater(() -> {
                        // 更新进度标签显示
                        progressLabel.setText(finalTotalFinished + "/" + (selectedCount * iterations));
                    });
                }
            }

            // 完成当前迭代并添加到历史记录
            iterationResult.complete();
            executionHistory.addIteration(iterationResult);

            // 实时更新结果面板
            SwingUtilities.invokeLater(() -> {
                resultsPanel.updateExecutionHistory(executionHistory);
            });
        }

        // 完成整个批量执行
        executionHistory.complete();

        SwingUtilities.invokeLater(() -> {
            runBtn.setEnabled(true);
            // 停止计时器
            if (executionTimer != null && executionTimer.isRunning()) {
                executionTimer.stop();
            }

            // 最终更新结果面板
            resultsPanel.updateExecutionHistory(executionHistory);

            // 如果执行完成，切换到结果面板
            if (!isStopped) {
                mainTabbedPane.setSelectedIndex(1); // 切换到执行结果面板
            }
        });
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
        String assertion = "not executed";
        if (!preOk) {
            status = "前置脚本失败";
        } else if (HttpUtil.isSSERequest(req)) {
            status = "SSE请求，无法批量执行";
        } else if (HttpUtil.isWebSocketRequest(req)) {
            status = "WebSocket请求，无法批量执行";
        } else {
            try {
                req.logEvent = true; // 确保日志事件开启
                resp = HttpSingleRequestExecutor.execute(req);
                status = String.valueOf(resp.code);
                assertion = runPostScriptAndAssertWithCsv(item, bindings, resp, row, pm);
            } catch (Exception ex) {
                log.error("请求执行失败", ex);
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
                                SidebarTabPanel.appendConsoleLog("[PreScript Console]\n" + output);
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
                                SidebarTabPanel.appendConsoleLog("[PostScript Console]\n" + output);
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

    // 增强的 CSV 文件管理对话框
    private void showEnhancedCsvManagementDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "CSV 数据管理", true);
        dialog.setSize(600, 430);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // 顶部说明面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        JLabel titleLabel = new JLabel("CSV 数据驱动测试");
        titleLabel.setFont(FontUtil.getDefaultFont(Font.BOLD, 16));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        JTextArea descArea = new JTextArea(
                "CSV 数据驱动测试允许您使用外部数据文件为每行数据执行一次测试。\n" +
                        "• CSV 文件第一行应为列标题\n" +
                        "• 支持两种使用方式：\n" +
                        "  1. 在请求URL、Header、Body中直接使用 {{列名}} 占位符\n" +
                        "  2. 在脚本中使用 pm.variables.get('列名') 访问数据\n" +
                        "• 支持的编码：UTF-8");
        descArea.setFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
        descArea.setEditable(false);
        descArea.setOpaque(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        topPanel.add(descArea, BorderLayout.CENTER);

        dialog.add(topPanel, BorderLayout.NORTH);

        // 中间内容面板
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));

        // 当前状态显示
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(BorderFactory.createTitledBorder("当前状态"));

        JLabel currentStatusLabel = new JLabel();
        if (csvData == null || csvData.isEmpty()) {
            currentStatusLabel.setText("未加载 CSV 数据");
            currentStatusLabel.setIcon(new FlatSVGIcon("icons/warning.svg", 16, 16));
            currentStatusLabel.setForeground(Color.GRAY);
        } else {
            currentStatusLabel.setText(String.format("已加载: %s (%d 行数据)",
                    csvFile != null ? csvFile.getName() : "手动创建", csvData.size()));
            currentStatusLabel.setIcon(new FlatSVGIcon("icons/check.svg", 16, 16));
            currentStatusLabel.setForeground(new Color(0, 128, 0));
        }
        statusPanel.add(currentStatusLabel);
        contentPanel.add(statusPanel, BorderLayout.NORTH);

        // 操作按钮面板 - 改为3行
        JPanel actionPanel = new JPanel(new GridLayout(3, 1, 5, 10));
        actionPanel.setBorder(BorderFactory.createTitledBorder("操作"));

        // 选择文件按钮
        JButton selectFileBtn = new JButton("选择 CSV 文件");
        selectFileBtn.setIcon(new FlatSVGIcon("icons/file.svg", 16, 16));
        selectFileBtn.setPreferredSize(new Dimension(200, 35));

        // 管理数据按钮
        JButton manageDataBtn = new JButton("管理数据");
        manageDataBtn.setIcon(new FlatSVGIcon("icons/code.svg", 16, 16));
        manageDataBtn.setPreferredSize(new Dimension(200, 35));
        manageDataBtn.setEnabled(csvData != null && !csvData.isEmpty());

        // 清除数据按钮
        JButton clearBtn = new JButton("清除数据");
        clearBtn.setIcon(new FlatSVGIcon("icons/clear.svg", 16, 16));
        clearBtn.setPreferredSize(new Dimension(200, 35));
        clearBtn.setEnabled(csvData != null && !csvData.isEmpty());

        // 为按钮添加事件监听器，并确保状态更新
        selectFileBtn.addActionListener(e -> {
            if (selectCsvFile()) {
                currentStatusLabel.setText(String.format("已加载: %s (%d 行数据)",
                        csvFile.getName(), csvData.size()));
                currentStatusLabel.setIcon(new FlatSVGIcon("icons/check.svg", 16, 16));
                currentStatusLabel.setForeground(new Color(0, 128, 0));
                updateCsvStatus();

                // 立即更新按钮状态
                manageDataBtn.setEnabled(csvData != null && !csvData.isEmpty());
                clearBtn.setEnabled(csvData != null && !csvData.isEmpty());
            }
        });

        manageDataBtn.addActionListener(e -> showCsvDataManageDialog());

        clearBtn.addActionListener(e -> {
            clearCsvData();
            currentStatusLabel.setText("未加载 CSV 数据");
            currentStatusLabel.setIcon(new FlatSVGIcon("icons/warning.svg", 16, 16));
            currentStatusLabel.setForeground(Color.GRAY);
            manageDataBtn.setEnabled(false);
            clearBtn.setEnabled(false);
        });

        actionPanel.add(selectFileBtn);
        actionPanel.add(manageDataBtn);
        actionPanel.add(clearBtn);

        contentPanel.add(actionPanel, BorderLayout.CENTER);
        dialog.add(contentPanel, BorderLayout.CENTER);

        // 底部按钮
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeBtn = new JButton("关闭");
        closeBtn.addActionListener(e -> dialog.dispose());
        bottomPanel.add(closeBtn);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    /**
     * 选择 CSV 文件
     */
    private boolean selectCsvFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择 CSV 文件");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV 文件 (*.csv)", "csv"));

        // 设置默认目录
        if (csvFile != null && csvFile.getParentFile() != null) {
            fileChooser.setCurrentDirectory(csvFile.getParentFile());
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            // 验证文件
            String validation = CsvDataUtil.validateCsvFile(selectedFile);
            if (!validation.startsWith("文件格式正确")) {
                JOptionPane.showMessageDialog(this, validation, "文件验证失败", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            try {
                List<Map<String, String>> newCsvData = CsvDataUtil.readCsvData(selectedFile);
                if (newCsvData.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "CSV 文件没有有效数据", "提示", JOptionPane.WARNING_MESSAGE);
                    return false;
                }

                csvFile = selectedFile;
                csvData = newCsvData;
                csvHeaders = CsvDataUtil.getCsvHeaders(selectedFile); // 获取列标题
                return true;

            } catch (Exception e) {
                log.error("加载 CSV 文件失败", e);
                JOptionPane.showMessageDialog(this, "读取 CSV 文件失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return false;
    }
}