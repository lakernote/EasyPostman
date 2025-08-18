package com.laker.postman.common.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.util.CsvDataUtil;
import com.laker.postman.util.FontUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * CSV 数据管理面板 - 独立的CSV功能组件
 */
@Slf4j
public class CsvDataPanel extends JPanel {

    private File csvFile;
    private List<Map<String, String>> csvData;
    private List<String> csvHeaders; // 保存CSV列标题的顺序
    private JPanel csvStatusPanel;  // CSV 状态显示面板
    private JLabel csvStatusLabel;  // CSV 状态标签

    public CsvDataPanel() {
        initUI();
    }


    private void initUI() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // 创建CSV状态面板和CSV按钮
        JPanel csvPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        csvPanel.setOpaque(false);

        // CSV按钮
        JButton csvBtn = createCsvMenuButton();
        csvPanel.add(csvBtn);

        // CSV状态显示面板
        csvStatusPanel = createCsvStatusPanel();
        csvPanel.add(csvStatusPanel);

        add(csvPanel, BorderLayout.CENTER);
    }

    /**
     * 创建 CSV 状态显示面板
     */
    private JPanel createCsvStatusPanel() {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        statusPanel.setOpaque(false);
        statusPanel.setVisible(false); // 初始隐藏

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

        statusPanel.add(csvIcon);
        statusPanel.add(csvStatusLabel);
        statusPanel.add(csvClearBtn);

        return statusPanel;
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
    public void clearCsvData() {
        csvFile = null;
        csvData = null;
        csvHeaders = null;
        updateCsvStatus();
        JOptionPane.showMessageDialog(SingletonFactory.getInstance(MainFrame.class),
                "CSV 数据已清除",
                "信息",
                JOptionPane.INFORMATION_MESSAGE);
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

    /**
     * 增强的 CSV 文件管理对话框
     */
    private void showEnhancedCsvManagementDialog() {
        JDialog dialog = new JDialog(SingletonFactory.getInstance(MainFrame.class), "CSV数据管理", true);
        dialog.setSize(600, 430);
        dialog.setLocationRelativeTo(SingletonFactory.getInstance(MainFrame.class));
        dialog.setLayout(new BorderLayout());

        // 顶部说明面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        JLabel titleLabel = new JLabel("CSV数据驱动测试");
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
     * CSV 数据管理对话框 - 集成预览和编辑功能
     */
    private void showCsvDataManageDialog() {
        if (csvData == null || csvData.isEmpty()) {
            JOptionPane.showMessageDialog(null, "没有可管理的 CSV 数据", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog manageDialog = new JDialog((Frame) null, "CSV数据管理", true);
        manageDialog.setSize(700, 550);
        manageDialog.setLocationRelativeTo(null);
        manageDialog.setLayout(new BorderLayout());

        // 顶部信息面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel infoLabel = new JLabel(String.format(
                "<html>数据来源: <b>%s</b> | 行数: <b>%d</b></html>",
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

        // 创建表格数据，确保至少有5行用于编辑
        Object[][] tableData = new Object[csvData.size()][headers.size()];
        for (int i = 0; i < csvData.size(); i++) {
            Map<String, String> row = csvData.get(i);
            for (int j = 0; j < headers.size(); j++) {
                tableData[i][j] = row.get(headers.get(j));
            }
        }

        // 创建可编辑的表格模型
        DefaultTableModel editTableModel = new DefaultTableModel(tableData, headers.toArray()) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true; // 所有单元格都可编辑
            }
        };

        JTable csvTable = new JTable(editTableModel);

        csvTable.setFillsViewportHeight(true);
        csvTable.setRowHeight(28); // 与 EasyTablePanel 一致的行高
        csvTable.setFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
        csvTable.getTableHeader().setFont(FontUtil.getDefaultFont(Font.BOLD, 12));
        csvTable.getTableHeader().setBackground(new Color(240, 242, 245));
        csvTable.getTableHeader().setForeground(new Color(33, 33, 33));
        csvTable.setGridColor(new Color(237, 237, 237)); // 使用更柔和的表格线颜色
        csvTable.setShowHorizontalLines(true);
        csvTable.setShowVerticalLines(true);
        csvTable.setIntercellSpacing(new Dimension(2, 2)); // 设置单元格间距
        csvTable.setRowMargin(2);
        csvTable.setSelectionBackground(new Color(220, 235, 252));
        csvTable.setSelectionForeground(Color.BLACK);
        csvTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        csvTable.setOpaque(false); // 设置表格透明

        // 设置空值渲染器
        DefaultTableCellRenderer emptyCellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                    if (isSelected) {
                        c.setBackground(table.getSelectionBackground());
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                } else {
                    if (isSelected) {
                        c.setBackground(table.getSelectionBackground());
                    } else {
                        c.setBackground(table.getBackground());
                    }
                }
                return c;
            }
        };

        // 为所有列设置渲染器
        for (int i = 0; i < headers.size(); i++) {
            csvTable.getColumnModel().getColumn(i).setCellRenderer(emptyCellRenderer);
            csvTable.getColumnModel().getColumn(i).setPreferredWidth(120);
        }

        // 创建带样式的滚动面板
        JScrollPane scrollPane = new JScrollPane(csvTable);
        scrollPane.getViewport().setBackground(new Color(248, 250, 252)); // 与 EasyTablePanel 一致的背景色
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(237, 237, 237)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8))); // 参考 EasyTablePanel 的边框样式

        // 创建表格容器面板，应用背景色
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(new Color(248, 250, 252));
        tablePanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        manageDialog.add(tablePanel, BorderLayout.CENTER);

        // 底部面板
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));

        // 工具栏
        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton addRowBtn = new JButton("Add Row");
        addRowBtn.setIcon(new FlatSVGIcon("icons/plus.svg", 16, 16));
        List<String> finalHeaders = headers;
        addRowBtn.addActionListener(e -> editTableModel.addRow(new Object[finalHeaders.size()]));

        JButton deleteRowBtn = new JButton("Delete Row");
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

        JButton addColumnBtn = new JButton("Add Column");
        addColumnBtn.setIcon(new FlatSVGIcon("icons/plus.svg", 16, 16));
        addColumnBtn.addActionListener(e -> {
            String columnName = JOptionPane.showInputDialog(manageDialog, "请输入新列名:", "添加列", JOptionPane.PLAIN_MESSAGE);
            if (columnName != null && !columnName.trim().isEmpty()) {
                columnName = columnName.trim();
                editTableModel.addColumn(columnName);
                csvTable.getColumnModel().getColumn(csvTable.getColumnCount() - 1).setPreferredWidth(120);
            }
        });

        JButton deleteColumnBtn = new JButton("Delete Column");
        deleteColumnBtn.setIcon(new FlatSVGIcon("icons/clear.svg", 16, 16));
        deleteColumnBtn.addActionListener(e -> {
            int[] selectedColumns = csvTable.getSelectedColumns();
            if (selectedColumns.length == 0) {
                JOptionPane.showMessageDialog(manageDialog, "请先选择要删除的列", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 检查是否要删除所有列
            if (selectedColumns.length >= editTableModel.getColumnCount()) {
                JOptionPane.showMessageDialog(manageDialog, "不能删除所有列，至少需要保留一列", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 显示要删除的列名
            StringBuilder columnNames = new StringBuilder();
            for (int i = 0; i < selectedColumns.length; i++) {
                if (i > 0) columnNames.append(", ");
                columnNames.append(editTableModel.getColumnName(selectedColumns[i]));
            }

            int confirm = JOptionPane.showConfirmDialog(manageDialog,
                    String.format("确定要删除选中的列吗？\n列名: %s", columnNames.toString()),
                    "确认删除", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                // 从后往前删除，避免索引变化问题
                for (int i = selectedColumns.length - 1; i >= 0; i--) {
                    int columnIndex = selectedColumns[i];

                    // 删除列数据
                    for (int row = 0; row < editTableModel.getRowCount(); row++) {
                        // 移动后面的列数据
                        for (int col = columnIndex; col < editTableModel.getColumnCount() - 1; col++) {
                            editTableModel.setValueAt(editTableModel.getValueAt(row, col + 1), row, col);
                        }
                    }

                    // 删除列
                    editTableModel.setColumnCount(editTableModel.getColumnCount() - 1);

                    // 更新列标识符
                    java.util.Vector<String> columnIdentifiers = new java.util.Vector<>();
                    for (int j = 0; j < editTableModel.getColumnCount(); j++) {
                        if (j < columnIndex) {
                            columnIdentifiers.add(editTableModel.getColumnName(j));
                        } else {
                            columnIdentifiers.add(editTableModel.getColumnName(j + 1));
                        }
                    }
                    editTableModel.setColumnIdentifiers(columnIdentifiers);
                }

                // 重新设置列宽
                for (int i = 0; i < csvTable.getColumnCount(); i++) {
                    csvTable.getColumnModel().getColumn(i).setPreferredWidth(120);
                }
            }
        });

        toolPanel.add(addRowBtn);
        toolPanel.add(deleteRowBtn);
        toolPanel.add(addColumnBtn);
        toolPanel.add(deleteColumnBtn);
        bottomPanel.add(toolPanel, BorderLayout.NORTH);

        // 使用说明
        JPanel helpPanel = new JPanel(new BorderLayout());
        helpPanel.setBorder(BorderFactory.createTitledBorder("使用说明"));
        JTextArea helpText = new JTextArea(
                """
                        • 双击单元格可直接编辑内容
                        • 支持两种使用方式：{{列名}} 占位符 或 pm.variables.get('列名')
                        • 例如：URL中使用 {{baseUrl}}/users 或脚本中使用 pm.variables.get('userId')""");
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

        int result = fileChooser.showOpenDialog(SingletonFactory.getInstance(MainFrame.class));
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

    /**
     * 获取当前是否有CSV数据
     */
    public boolean hasData() {
        return csvData != null && !csvData.isEmpty();
    }

    /**
     * 获取CSV数据行数
     */
    public int getRowCount() {
        return csvData != null ? csvData.size() : 0;
    }

    /**
     * 获取指定行的数据
     */
    public Map<String, String> getRowData(int index) {
        if (csvData != null && index >= 0 && index < csvData.size()) {
            return csvData.get(index);
        }
        return null;
    }
}

