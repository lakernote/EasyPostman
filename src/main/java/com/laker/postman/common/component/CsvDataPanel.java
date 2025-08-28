package com.laker.postman.common.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.util.CsvDataUtil;
import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
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
        csvStatusLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_STATUS_NO_DATA));
        csvStatusLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
        csvStatusLabel.setForeground(new Color(100, 100, 100));

        // CSV 清除按钮
        JButton csvClearBtn = new JButton();
        csvClearBtn.setIcon(new FlatSVGIcon("icons/close.svg", 14, 14));
        csvClearBtn.setPreferredSize(new Dimension(20, 20));
        csvClearBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_CLEAR_TOOLTIP));
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

        JMenuItem loadCsvItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.CSV_MENU_IMPORT_FILE),
                new FlatSVGIcon("icons/import.svg", 16, 16));
        loadCsvItem.addActionListener(e -> showEnhancedCsvManagementDialog());
        csvMenu.add(loadCsvItem);

        JMenuItem manageCsvItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.CSV_MENU_MANAGE_DATA),
                new FlatSVGIcon("icons/code.svg", 16, 16));
        manageCsvItem.addActionListener(e -> showCsvDataManageDialog());
        manageCsvItem.setEnabled(false); // 默认禁用，有数据时启用
        csvMenu.add(manageCsvItem);

        csvMenu.addSeparator();

        JMenuItem clearCsvItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.CSV_MENU_CLEAR_DATA),
                new FlatSVGIcon("icons/clear.svg", 16, 16));
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
                I18nUtil.getMessage(MessageKeys.CSV_DATA_CLEARED),
                I18nUtil.getMessage(MessageKeys.GENERAL_INFO),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 更新 CSV 状态显示
     */
    private void updateCsvStatus() {
        if (csvData == null || csvData.isEmpty()) {
            csvStatusPanel.setVisible(false);
        } else {
            csvStatusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_LOADED,
                    csvFile != null ? csvFile.getName() : I18nUtil.getMessage(MessageKeys.CSV_MANUAL_CREATED),
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
        JDialog dialog = new JDialog(SingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.CSV_DIALOG_MANAGEMENT_TITLE), true);
        dialog.setSize(600, 430);
        dialog.setLocationRelativeTo(SingletonFactory.getInstance(MainFrame.class));
        dialog.setLayout(new BorderLayout());

        // 顶部说明面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_DATA_DRIVEN_TEST));
        titleLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 16));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        JTextArea descArea = new JTextArea(I18nUtil.getMessage(MessageKeys.CSV_DIALOG_DESCRIPTION));
        descArea.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
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
        statusPanel.setBorder(BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.CSV_CURRENT_STATUS)));

        JLabel currentStatusLabel = new JLabel();
        if (csvData == null || csvData.isEmpty()) {
            currentStatusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_NO_DATA));
            currentStatusLabel.setIcon(new FlatSVGIcon("icons/warning.svg", 16, 16));
            currentStatusLabel.setForeground(Color.GRAY);
        } else {
            currentStatusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_LOADED,
                    csvFile != null ? csvFile.getName() : I18nUtil.getMessage(MessageKeys.CSV_MANUAL_CREATED), csvData.size()));
            currentStatusLabel.setIcon(new FlatSVGIcon("icons/check.svg", 16, 16));
            currentStatusLabel.setForeground(new Color(0, 128, 0));
        }
        statusPanel.add(currentStatusLabel);
        contentPanel.add(statusPanel, BorderLayout.NORTH);

        // 操作按钮面板 - 改为3行
        JPanel actionPanel = new JPanel(new GridLayout(3, 1, 5, 10));
        actionPanel.setBorder(BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.CSV_OPERATIONS)));

        // 选择文件按钮
        JButton selectFileBtn = new JButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_SELECT_FILE));
        selectFileBtn.setIcon(new FlatSVGIcon("icons/file.svg", 16, 16));
        selectFileBtn.setPreferredSize(new Dimension(200, 35));

        // 管理数据按钮
        JButton manageDataBtn = new JButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_MANAGE_DATA));
        manageDataBtn.setIcon(new FlatSVGIcon("icons/code.svg", 16, 16));
        manageDataBtn.setPreferredSize(new Dimension(200, 35));
        manageDataBtn.setEnabled(csvData != null && !csvData.isEmpty());

        // 清除数据按钮
        JButton clearBtn = new JButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_CLEAR_DATA));
        clearBtn.setIcon(new FlatSVGIcon("icons/clear.svg", 16, 16));
        clearBtn.setPreferredSize(new Dimension(200, 35));
        clearBtn.setEnabled(csvData != null && !csvData.isEmpty());

        // 为按钮添加事件监听器，并确保状态更新
        selectFileBtn.addActionListener(e -> {
            if (selectCsvFile()) {
                currentStatusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_LOADED,
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
            currentStatusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_NO_DATA));
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
        JButton closeBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLOSE));
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
            JOptionPane.showMessageDialog(null, I18nUtil.getMessage(MessageKeys.CSV_NO_MANAGEABLE_DATA),
                    I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog manageDialog = new JDialog((Frame) null, I18nUtil.getMessage(MessageKeys.CSV_DATA_MANAGEMENT), true);
        manageDialog.setSize(700, 550);
        manageDialog.setLocationRelativeTo(null);
        manageDialog.setLayout(new BorderLayout());

        // 顶部信息面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel infoLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_DATA_SOURCE_INFO,
                csvFile != null ? csvFile.getName() : I18nUtil.getMessage(MessageKeys.CSV_MANUAL_CREATED),
                csvData.size()));
        infoLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        topPanel.add(infoLabel, BorderLayout.CENTER);

        manageDialog.add(topPanel, BorderLayout.NORTH);

        // 创建可编辑的表格
        List<String> headers;
        try {
            if (csvHeaders != null && !csvHeaders.isEmpty()) {
                // 优先使用保存的列标题顺序
                headers = new ArrayList<>(csvHeaders);
            } else if (csvFile != null) {
                headers = CsvDataUtil.getCsvHeaders(csvFile);
                csvHeaders = headers; // 保存列标题顺序
            } else {
                // 从现有数据中获取列名，使用LinkedHashMap保持顺序
                if (csvData.isEmpty()) {
                    headers = new ArrayList<>();
                } else {
                    // 如果数据使用的是LinkedHashMap，keySet()会保持插入顺序
                    headers = new ArrayList<>(csvData.get(0).keySet());
                    csvHeaders = headers; // 保存列标题顺序
                }
            }
        } catch (Exception e) {
            log.error("获取 CSV 列标题失败", e);
            headers = csvHeaders != null ? new ArrayList<>(csvHeaders) :
                    (csvData.isEmpty() ? new ArrayList<>() : new ArrayList<>(csvData.get(0).keySet()));
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
        csvTable.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        csvTable.getTableHeader().setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
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

        // 封装设置渲染器的方法
        Runnable applyEmptyCellRenderer = () -> {
            for (int i = 0; i < csvTable.getColumnCount(); i++) {
                csvTable.getColumnModel().getColumn(i).setCellRenderer(emptyCellRenderer);
            }
        };

        // 为所有列设置渲染器
        applyEmptyCellRenderer.run();
        for (int i = 0; i < headers.size(); i++) {
            csvTable.getColumnModel().getColumn(i).setPreferredWidth(120);
        }

        // 创建带样式的滚动面板
        JScrollPane scrollPane = new JScrollPane(csvTable);
        scrollPane.getViewport().setBackground(new Color(248, 250, 252)); // 与 EasyTablePanel 一致的背景色
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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

        JButton addRowBtn = new JButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_ADD_ROW));
        addRowBtn.setIcon(new FlatSVGIcon("icons/plus.svg", 16, 16));
        List<String> finalHeaders = headers;
        addRowBtn.addActionListener(e -> editTableModel.addRow(new Object[finalHeaders.size()]));

        JButton deleteRowBtn = new JButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_DELETE_ROW));
        deleteRowBtn.setIcon(new FlatSVGIcon("icons/clear.svg", 16, 16));
        deleteRowBtn.addActionListener(e -> {
            int[] selectedRows = csvTable.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(manageDialog, I18nUtil.getMessage(MessageKeys.CSV_SELECT_ROWS_TO_DELETE),
                        I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(manageDialog,
                    I18nUtil.getMessage(MessageKeys.CSV_CONFIRM_DELETE_ROWS, selectedRows.length),
                    I18nUtil.getMessage(MessageKeys.CSV_CONFIRM_DELETE), JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    editTableModel.removeRow(selectedRows[i]);
                }
            }
        });

        JButton addColumnBtn = new JButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_ADD_COLUMN));
        addColumnBtn.setIcon(new FlatSVGIcon("icons/plus.svg", 16, 16));
        addColumnBtn.addActionListener(e -> {
            String columnName = JOptionPane.showInputDialog(manageDialog, I18nUtil.getMessage(MessageKeys.CSV_ENTER_COLUMN_NAME),
                    I18nUtil.getMessage(MessageKeys.CSV_ADD_COLUMN), JOptionPane.PLAIN_MESSAGE);
            if (columnName != null && !columnName.trim().isEmpty()) {
                columnName = columnName.trim();
                editTableModel.addColumn(columnName);
                // 重新设置列宽
                for (int i = 0; i < csvTable.getColumnCount(); i++) {
                    csvTable.getColumnModel().getColumn(i).setPreferredWidth(120);
                }
                applyEmptyCellRenderer.run(); // 新增列后重新设置渲染器
            }
        });

        JButton deleteColumnBtn = new JButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_DELETE_COLUMN));
        deleteColumnBtn.setIcon(new FlatSVGIcon("icons/clear.svg", 16, 16));
        deleteColumnBtn.addActionListener(e -> {
            int[] selectedColumns = csvTable.getSelectedColumns();
            if (selectedColumns.length == 0) {
                JOptionPane.showMessageDialog(manageDialog, I18nUtil.getMessage(MessageKeys.CSV_SELECT_COLUMNS_TO_DELETE),
                        I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 检查是否要删除所有列
            if (selectedColumns.length >= editTableModel.getColumnCount()) {
                JOptionPane.showMessageDialog(manageDialog, I18nUtil.getMessage(MessageKeys.CSV_CANNOT_DELETE_ALL_COLUMNS),
                        I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 显示要删除的列名
            StringBuilder columnNames = new StringBuilder();
            for (int i = 0; i < selectedColumns.length; i++) {
                if (i > 0) columnNames.append(", ");
                columnNames.append(editTableModel.getColumnName(selectedColumns[i]));
            }

            int confirm = JOptionPane.showConfirmDialog(manageDialog,
                    I18nUtil.getMessage(MessageKeys.CSV_CONFIRM_DELETE_COLUMNS, columnNames.toString()),
                    I18nUtil.getMessage(MessageKeys.CSV_CONFIRM_DELETE), JOptionPane.YES_NO_OPTION);

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
                applyEmptyCellRenderer.run(); // 删除列后重新设置渲染器
            }
        });

        toolPanel.add(addRowBtn);
        toolPanel.add(deleteRowBtn);
        toolPanel.add(addColumnBtn);
        toolPanel.add(deleteColumnBtn);
        bottomPanel.add(toolPanel, BorderLayout.NORTH);

        // 使用说明
        JPanel helpPanel = new JPanel(new BorderLayout());
        helpPanel.setBorder(BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.CSV_USAGE_INSTRUCTIONS)));
        JTextArea helpText = new JTextArea(I18nUtil.getMessage(MessageKeys.CSV_USAGE_TEXT));
        helpText.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
        helpText.setEditable(false);
        helpText.setOpaque(false);
        helpText.setLineWrap(true);
        helpText.setWrapStyleWord(true);
        helpPanel.add(helpText, BorderLayout.CENTER);
        bottomPanel.add(helpPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton saveBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_SAVE));
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
                    JOptionPane.showMessageDialog(manageDialog, I18nUtil.getMessage(MessageKeys.CSV_NO_VALID_DATA_ROWS),
                            I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // 更新 CSV 数据和列标题顺序
                csvData = newCsvData;
                csvHeaders = currentHeaders; // 保存列标题顺序
                csvFile = null; // 清除原文件引用，表示这是手动编辑的数据

                updateCsvStatus();

                JOptionPane.showMessageDialog(manageDialog,
                        I18nUtil.getMessage(MessageKeys.CSV_DATA_SAVED, newCsvData.size(), currentHeaders.size()),
                        I18nUtil.getMessage(MessageKeys.CSV_SAVE_SUCCESS), JOptionPane.INFORMATION_MESSAGE);

                manageDialog.dispose();

            } catch (Exception ex) {
                log.error("保存 CSV 数据失败", ex);
                JOptionPane.showMessageDialog(manageDialog, I18nUtil.getMessage(MessageKeys.CSV_SAVE_FAILED, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));
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
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.CSV_SELECT_FILE));
        fileChooser.setFileFilter(new FileNameExtensionFilter(I18nUtil.getMessage(MessageKeys.CSV_FILE_FILTER), "csv"));

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
                JOptionPane.showMessageDialog(this, validation, I18nUtil.getMessage(MessageKeys.CSV_FILE_VALIDATION_FAILED), JOptionPane.ERROR_MESSAGE);
                return false;
            }

            try {
                List<Map<String, String>> newCsvData = CsvDataUtil.readCsvData(selectedFile);
                if (newCsvData.isEmpty()) {
                    JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.CSV_NO_VALID_DATA),
                            I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
                    return false;
                }

                csvFile = selectedFile;
                csvData = newCsvData;
                csvHeaders = CsvDataUtil.getCsvHeaders(selectedFile); // 获取列标题
                return true;

            } catch (Exception e) {
                log.error("加载 CSV 文件失败", e);
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.CSV_LOAD_FAILED, e.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
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