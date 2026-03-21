package com.laker.postman.common.component;

import cn.hutool.core.text.CharSequenceUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.button.CSVButton;
import com.laker.postman.common.component.button.CloseButton;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * CSV 数据管理面板 - 独立的CSV功能组件
 */
@Slf4j
public class CsvDataPanel extends JPanel {

    public static final class CsvState {
        private final String sourceName;
        private final List<String> headers;
        private final List<Map<String, String>> rows;

        public CsvState(String sourceName, List<String> headers, List<Map<String, String>> rows) {
            this.sourceName = sourceName;
            this.headers = copyHeaders(headers);
            this.rows = copyRows(rows);
        }

        public String getSourceName() {
            return sourceName;
        }

        public List<String> getHeaders() {
            return copyHeaders(headers);
        }

        public List<Map<String, String>> getRows() {
            return copyRows(rows);
        }
    }

    private File csvFile;
    private String csvSourceName;
    private List<Map<String, String>> csvData;
    private List<String> csvHeaders; // 保存CSV列标题的顺序
    private JPanel csvStatusPanel;  // CSV 状态显示面板
    private JLabel csvStatusLabel;  // CSV 状态标签
    private String contextHelpText; // 调用方注入的场景说明
    private Runnable changeListener;


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
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        statusPanel.setOpaque(false);
        statusPanel.setVisible(false); // 初始隐藏

        // 状态文本
        csvStatusLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_STATUS_NO_DATA));
        csvStatusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        csvStatusLabel.setForeground(ModernColors.getTextSecondary()); // 使用次要文本颜色
        csvStatusLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // 设置手型光标
        csvStatusLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.CSV_MENU_MANAGE_DATA)); // 添加提示文本

        // 添加点击事件监听器，点击文字打开数据管理对话框
        csvStatusLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            private Color originalColor;

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (csvData != null && !csvData.isEmpty()) {
                    showCsvDataManageDialog();
                }
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (csvData != null && !csvData.isEmpty()) {
                    // 鼠标悬停时改变颜色（使用更明亮的颜色）
                    originalColor = csvStatusLabel.getForeground();
                    csvStatusLabel.setForeground(ModernColors.PRIMARY);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (csvData != null && !csvData.isEmpty() && originalColor != null) {
                    // 鼠标离开时恢复原始颜色
                    csvStatusLabel.setForeground(originalColor);
                }
            }
        });

        // CSV 清除按钮
        CloseButton csvClearBtn = new CloseButton();
        csvClearBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_CLEAR_TOOLTIP));
        csvClearBtn.addActionListener(e -> clearCsvData());

        statusPanel.add(csvStatusLabel);
        statusPanel.add(csvClearBtn);

        return statusPanel;
    }

    /**
     * 创建带下拉菜单的 CSV 按钮
     */
    private JButton createCsvMenuButton() {
        JButton csvBtn = new CSVButton();

        JPopupMenu csvMenu = new JPopupMenu();

        JMenuItem loadCsvItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.CSV_MENU_IMPORT_FILE),
                IconUtil.createThemed("icons/import.svg", 16, 16));
        loadCsvItem.addActionListener(e -> showEnhancedCsvManagementDialog());
        csvMenu.add(loadCsvItem);

        JMenuItem createManualItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.CSV_MENU_CREATE_MANUAL),
                IconUtil.createThemed("icons/plus.svg", 16, 16));
        createManualItem.addActionListener(e -> showManualCreateDialog());
        csvMenu.add(createManualItem);

        JMenuItem manageCsvItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.CSV_MENU_MANAGE_DATA),
                IconUtil.createThemed("icons/code.svg", 16, 16));
        manageCsvItem.addActionListener(e -> showCsvDataManageDialog());
        manageCsvItem.setEnabled(false); // 默认禁用，有数据时启用
        csvMenu.add(manageCsvItem);

        csvMenu.addSeparator();

        JMenuItem clearCsvItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.CSV_MENU_CLEAR_DATA),
                IconUtil.createThemed("icons/clear.svg", 16, 16));
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
     * 设置调用场景专属说明，仅在说明面板中追加展示。
     */
    public void setContextHelpText(String contextHelpText) {
        this.contextHelpText = contextHelpText;
        if (csvStatusLabel != null) {
            csvStatusLabel.setToolTipText(buildStatusTooltip());
        }
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener;
    }

    public CsvState exportState() {
        if (!hasData()) {
            return null;
        }
        return new CsvState(getCurrentSourceName(), csvHeaders, csvData);
    }

    public void restoreState(CsvState state) {
        applyState(state, false);
    }

    /**
     * 清除 CSV 数据
     */
    public void clearCsvData() {
        csvFile = null;
        csvSourceName = null;
        csvData = null;
        csvHeaders = null;
        updateCsvStatus();
        notifyChangeListener();
        NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.CSV_DATA_CLEARED));
    }

    /**
     * 更新 CSV 状态显示
     */
    private void updateCsvStatus() {
        if (csvData == null || csvData.isEmpty()) {
            csvStatusPanel.setVisible(false);
        } else {
            csvStatusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_LOADED, csvData.size()));
            csvStatusLabel.setForeground(ModernColors.getTextSecondary()); // 使用次要文本颜色
            csvStatusLabel.setToolTipText(buildStatusTooltip());
            csvStatusPanel.setVisible(true);
        }
        revalidate();
        repaint();
    }

    private String buildStatusTooltip() {
        if (CharSequenceUtil.isBlank(contextHelpText)) {
            return I18nUtil.getMessage(MessageKeys.CSV_MENU_MANAGE_DATA);
        }
        return "<html>" + I18nUtil.getMessage(MessageKeys.CSV_MENU_MANAGE_DATA)
                + "<br><br>"
                + contextHelpText.replace("\n", "<br>")
                + "</html>";
    }

    private String buildHelpText(String baseText) {
        if (CharSequenceUtil.isBlank(contextHelpText)) {
            return baseText;
        }
        return baseText + "\n\n" + contextHelpText;
    }

    /**
     * 手动创建 CSV 数据对话框
     */
    private void showManualCreateDialog() {
        JDialog dialog = new JDialog(SingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_DIALOG_TITLE), true);
        dialog.setSize(550, 220);
        dialog.setLocationRelativeTo(SingletonFactory.getInstance(MainFrame.class));
        dialog.setLayout(new BorderLayout());

        // 顶部说明面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_DIALOG_TITLE));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +4));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        JLabel descLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_DESCRIPTION));
        descLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN)); // 使用标准字体大小
        descLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        topPanel.add(descLabel, BorderLayout.CENTER);

        dialog.add(topPanel, BorderLayout.NORTH);

        // 中间内容面板
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 列标题输入
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        JLabel headersLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_COLUMN_HEADERS));
        headersLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        contentPanel.add(headersLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        JTextField headersField = new JTextField();
        headersField.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        headersField.setText("username,password,email");
        contentPanel.add(headersField, gbc);

        // 占位符提示
        gbc.gridx = 1;
        gbc.gridy = 1;
        JLabel placeholderLabel = new JLabel("eg: username,password,email");
        placeholderLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.ITALIC, -2)); // 比标准字体小2号
        placeholderLabel.setForeground(ModernColors.getTextHint());
        contentPanel.add(placeholderLabel, gbc);


        dialog.add(contentPanel, BorderLayout.CENTER);

        // 底部按钮面板
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JButton createBtn = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        createBtn.addActionListener(e -> {
            try {
                // 默认创建1行数据
                int rowCount = 1;

                // 处理列标题
                List<String> headers = new ArrayList<>();
                String headersText = headersField.getText().trim();
                if (CharSequenceUtil.isBlank(headersText)) {
                    NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_HEADERS_REQUIRED));
                    return;
                }

                // 解析列标题
                String[] headerArray = headersText.split(",");
                for (String header : headerArray) {
                    String trimmedHeader = header.trim();
                    if (CharSequenceUtil.isNotBlank(trimmedHeader)) {
                        headers.add(trimmedHeader);
                    }
                }

                if (headers.isEmpty()) {
                    NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_HEADERS_REQUIRED));
                    return;
                }

                // 验证列数范围（从列标题自动计算）
                int columnCount = headers.size();
                if (columnCount > 50) {
                    NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_TOO_MANY_COLUMNS, columnCount));
                    return;
                }

                // 创建空数据
                List<Map<String, String>> newData = new ArrayList<>();
                for (int i = 0; i < rowCount; i++) {
                    Map<String, String> row = new java.util.LinkedHashMap<>();
                    for (String header : headers) {
                        row.put(header, "");
                    }
                    newData.add(row);
                }

                // 设置数据
                csvData = newData;
                csvHeaders = headers;
                csvFile = null; // 清除文件引用，标记为手动创建
                csvSourceName = null;
                updateCsvStatus();
                notifyChangeListener();

                dialog.dispose();

                // 自动打开数据管理对话框
                showCsvDataManageDialog();

            } catch (Exception ex) {
                log.error("创建 CSV 数据失败", ex);
                JOptionPane.showMessageDialog(dialog,
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR) + ": " + ex.getMessage(),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));
        cancelBtn.addActionListener(e -> dialog.dispose());

        bottomPanel.add(createBtn);
        bottomPanel.add(cancelBtn);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    /**
     * 增强的 CSV 文件管理对话框
     * 布局：标题固定 → 说明文字可滚动 → 当前状态+操作按钮固定 → 关闭按钮固定
     */
    private void showEnhancedCsvManagementDialog() {
        JDialog dialog = new JDialog(SingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.CSV_DIALOG_MANAGEMENT_TITLE), true);
        dialog.setSize(480, 520);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(SingletonFactory.getInstance(MainFrame.class));
        dialog.setLayout(new BorderLayout());

        // ── NORTH：标题 + 可滚动说明文字 ──
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(14, 16, 0, 16));

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_DATA_DRIVEN_TEST));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +2));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        // 说明文字只读区域，超出高度出现滚动条
        JTextArea descArea = new JTextArea(buildHelpText(I18nUtil.getMessage(MessageKeys.CSV_DIALOG_DESCRIPTION)));
        descArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        descArea.setEditable(false);
        descArea.setOpaque(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        descScroll.setBorder(BorderFactory.createEmptyBorder());
        descScroll.setPreferredSize(new Dimension(0, 180));
        descScroll.getVerticalScrollBar().setUnitIncrement(10);
        topPanel.add(descScroll, BorderLayout.CENTER);

        dialog.add(topPanel, BorderLayout.NORTH);

        // ── CENTER：当前状态 + 操作按钮（固定，始终可见）──
        JPanel fixedPanel = new JPanel();
        fixedPanel.setLayout(new BoxLayout(fixedPanel, BoxLayout.Y_AXIS));
        fixedPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 4, 16));

        // 当前状态
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        statusPanel.setBorder(BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.CSV_CURRENT_STATUS)));
        statusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel currentStatusLabel = new JLabel();
        if (csvData == null || csvData.isEmpty()) {
            currentStatusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_NO_DATA));
            currentStatusLabel.setIcon(IconUtil.createThemed("icons/warning.svg", 16, 16));
            currentStatusLabel.setForeground(ModernColors.getTextHint());
        } else {
            currentStatusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_LOADED, csvData.size()));
            currentStatusLabel.setIcon(IconUtil.createThemed("icons/check.svg", 16, 16));
            currentStatusLabel.setForeground(ModernColors.getTextSecondary());
        }
        statusPanel.add(currentStatusLabel);
        fixedPanel.add(statusPanel);
        fixedPanel.add(Box.createVerticalStrut(6));

        // 操作按钮区（BoxLayout，按钮全宽自适应，不受语言影响）
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.setBorder(BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.CSV_OPERATIONS)));
        actionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton selectFileBtn = new JButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_SELECT_FILE));
        selectFileBtn.setIcon(IconUtil.createThemed("icons/file.svg", 16, 16));
        selectFileBtn.setHorizontalAlignment(SwingConstants.LEFT);
        selectFileBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectFileBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JButton createManualBtn = new JButton(I18nUtil.getMessage(MessageKeys.CSV_MENU_CREATE_MANUAL));
        createManualBtn.setIcon(new FlatSVGIcon("icons/plus.svg", 16, 16));
        createManualBtn.setHorizontalAlignment(SwingConstants.LEFT);
        createManualBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        createManualBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JButton manageDataBtn = new JButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_MANAGE_DATA));
        manageDataBtn.setIcon(IconUtil.createThemed("icons/code.svg", 16, 16));
        manageDataBtn.setHorizontalAlignment(SwingConstants.LEFT);
        manageDataBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        manageDataBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        manageDataBtn.setEnabled(csvData != null && !csvData.isEmpty());

        JButton clearBtn = new JButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_CLEAR_DATA));
        clearBtn.setIcon(IconUtil.createThemed("icons/clear.svg", 16, 16));
        clearBtn.setHorizontalAlignment(SwingConstants.LEFT);
        clearBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        clearBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        clearBtn.setEnabled(csvData != null && !csvData.isEmpty());

        // 事件
        selectFileBtn.addActionListener(e -> {
            if (selectCsvFile()) {
                currentStatusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_LOADED, csvData.size()));
                currentStatusLabel.setIcon(IconUtil.createThemed("icons/check.svg", 16, 16));
                currentStatusLabel.setForeground(ModernColors.getTextSecondary());
                updateCsvStatus();
                manageDataBtn.setEnabled(true);
                clearBtn.setEnabled(true);
            }
        });
        createManualBtn.addActionListener(e -> {
            dialog.dispose();
            showManualCreateDialog();
        });
        manageDataBtn.addActionListener(e -> showCsvDataManageDialog());
        clearBtn.addActionListener(e -> {
            clearCsvData();
            currentStatusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_NO_DATA));
            currentStatusLabel.setIcon(IconUtil.createThemed("icons/warning.svg", 16, 16));
            currentStatusLabel.setForeground(ModernColors.getTextHint());
            manageDataBtn.setEnabled(false);
            clearBtn.setEnabled(false);
        });

        actionPanel.add(Box.createVerticalStrut(4));
        actionPanel.add(selectFileBtn);
        actionPanel.add(Box.createVerticalStrut(6));
        actionPanel.add(createManualBtn);
        actionPanel.add(Box.createVerticalStrut(6));
        actionPanel.add(manageDataBtn);
        actionPanel.add(Box.createVerticalStrut(6));
        actionPanel.add(clearBtn);
        actionPanel.add(Box.createVerticalStrut(4));

        fixedPanel.add(actionPanel);
        dialog.add(fixedPanel, BorderLayout.CENTER);

        // ── SOUTH：关闭按钮（固定，始终可见）──
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ModernColors.getDividerBorderColor()));
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
                getCurrentSourceDisplayName(),
                csvData.size()));
        infoLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
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
        csvTable.setRowHeight(28); // 与 EasyTablePanel 一致的行高
        csvTable.setFont(FontsUtil.getDefaultFont(Font.PLAIN)); // 使用标准字体大小
        csvTable.getTableHeader().setFont(FontsUtil.getDefaultFont(Font.BOLD)); // 使用标准字体大小（粗体）
        csvTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // 设置空值渲染器 - 优化后的主题适配版本
        DefaultTableCellRenderer emptyCellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // 检查单元格是否为空
                boolean isEmpty = (value == null || value.toString().trim().isEmpty());

                if (isEmpty) {
                    // 空单元格：使用主题适配的空单元格背景色（有区分度）
                    setBackground(isSelected ? table.getSelectionBackground() : ModernColors.getEmptyCellBackground());
                } else {
                    // 有值单元格：使用选中背景或表格背景
                    setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
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
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.getBorderLightColor()), // 主题适配的边框色
                BorderFactory.createEmptyBorder(8, 8, 8, 8))); // 参考 EasyTablePanel 的边框样式

        // 创建表格容器面板，应用背景色
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        manageDialog.add(tablePanel, BorderLayout.CENTER);

        // 底部面板
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));

        // 工具栏
        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton bulkEditBtn = new JButton(I18nUtil.getMessage(MessageKeys.CSV_BULK_EDIT));
        bulkEditBtn.setIcon(IconUtil.createThemed("icons/edit.svg", 16, 16));
        List<String> finalHeaders = headers;
        bulkEditBtn.addActionListener(e -> showBulkEditDialog(manageDialog, editTableModel, csvTable, finalHeaders, applyEmptyCellRenderer));

        JButton addRowBtn = new JButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_ADD_ROW));
        addRowBtn.setIcon(IconUtil.createThemed("icons/plus.svg", 16, 16));
        addRowBtn.addActionListener(e -> editTableModel.addRow(new Object[finalHeaders.size()]));

        JButton deleteRowBtn = new JButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_DELETE_ROW));
        deleteRowBtn.setIcon(IconUtil.createThemed("icons/clear.svg", 16, 16));
        deleteRowBtn.addActionListener(e -> {
            // 先停止单元格编辑，避免编辑状态下删除行导致问题
            if (csvTable.isEditing()) {
                csvTable.getCellEditor().stopCellEditing();
            }

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
        addColumnBtn.setIcon(IconUtil.createThemed("icons/plus.svg", 16, 16));
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
        deleteColumnBtn.setIcon(IconUtil.createThemed("icons/clear.svg", 16, 16));
        deleteColumnBtn.addActionListener(e -> {
            // 先停止单元格编辑，避免编辑状态下删除列导致问题
            if (csvTable.isEditing()) {
                csvTable.getCellEditor().stopCellEditing();
            }

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

        toolPanel.add(bulkEditBtn);
        toolPanel.add(addRowBtn);
        toolPanel.add(deleteRowBtn);
        toolPanel.add(addColumnBtn);
        toolPanel.add(deleteColumnBtn);
        bottomPanel.add(toolPanel, BorderLayout.NORTH);

        // 使用说明
        JPanel helpPanel = new JPanel(new BorderLayout());
        helpPanel.setBorder(BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.CSV_USAGE_INSTRUCTIONS)));
        JTextArea helpText = new JTextArea(buildHelpText(I18nUtil.getMessage(MessageKeys.CSV_USAGE_TEXT)));
        helpText.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        helpText.setEditable(false);
        helpText.setOpaque(false);
        helpText.setLineWrap(true);
        helpText.setWrapStyleWord(true);
        helpPanel.add(helpText, BorderLayout.CENTER);
        bottomPanel.add(helpPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton saveBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_SAVE));
        saveBtn.setIcon(IconUtil.createThemed("icons/save.svg", 16, 16));
        saveBtn.addActionListener(e -> {
            try {
                // 先停止单元格编辑，确保正在编辑的内容被保存
                if (csvTable.isEditing()) {
                    csvTable.getCellEditor().stopCellEditing();
                }

                // 将表格数据转换为 CSV 数据格式
                List<Map<String, String>> newCsvData = new ArrayList<>();

                // 获取当前的列名
                List<String> currentHeaders = new ArrayList<>();
                for (int i = 0; i < editTableModel.getColumnCount(); i++) {
                    currentHeaders.add(editTableModel.getColumnName(i));
                }

                // 转换每一行数据
                for (int i = 0; i < editTableModel.getRowCount(); i++) {
                    Map<String, String> rowData = new LinkedHashMap<>();
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
                csvFile = null; // 清除原文件引用，避免后续误以为仍需重新读文件
                if (CharSequenceUtil.isBlank(csvSourceName)) {
                    csvSourceName = null;
                }

                updateCsvStatus();
                notifyChangeListener();

                NotificationUtil.showSuccess(
                        I18nUtil.getMessage(MessageKeys.CSV_DATA_SAVED, newCsvData.size(), currentHeaders.size()));

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
     * 显示批量编辑对话框
     * 支持以 CSV 格式批量粘贴和编辑数据
     */
    private void showBulkEditDialog(Window parentWindow, DefaultTableModel tableModel, JTable csvTable, List<String> headers, Runnable applyEmptyCellRenderer) {
        // 1. 将当前表格数据转换为文本格式（用逗号分隔）
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean hasData = false;
            StringBuilder rowText = new StringBuilder();
            for (int j = 0; j < headers.size(); j++) {
                Object value = tableModel.getValueAt(i, j);
                String strValue = value != null ? value.toString() : "";
                if (!strValue.trim().isEmpty()) {
                    hasData = true;
                }
                if (j > 0) {
                    rowText.append(",");
                }
                // 如果值包含逗号、换行符或引号，用引号包裹并转义引号
                if (strValue.contains(",") || strValue.contains("\n") || strValue.contains("\"")) {
                    rowText.append("\"").append(strValue.replace("\"", "\"\"")).append("\"");
                } else {
                    rowText.append(strValue);
                }
            }
            if (hasData) {
                text.append(rowText).append("\n");
            }
        }

        // 2. 创建文本编辑区域
        JTextArea textArea = new JTextArea(text.toString());
        textArea.setLineWrap(false);
        textArea.setTabSize(4);
        textArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        textArea.setBackground(ModernColors.getInputBackgroundColor());
        textArea.setForeground(ModernColors.getTextPrimary());
        textArea.setCaretColor(ModernColors.PRIMARY);

        // 将光标定位到文本末尾
        textArea.setCaretPosition(textArea.getDocument().getLength());

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(700, 450));

        // 3. 创建提示标签 - 使用国际化，垂直排列
        JPanel hintPanel = new JPanel();
        hintPanel.setLayout(new BoxLayout(hintPanel, BoxLayout.Y_AXIS));
        hintPanel.setOpaque(false);

        // 主提示文本
        JLabel hintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_BULK_EDIT_HINT));
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintLabel.setForeground(ModernColors.getTextPrimary());

        // 支持格式说明
        JLabel formatLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_BULK_EDIT_SUPPORTED_FORMATS));
        formatLabel.setForeground(ModernColors.getTextSecondary());
        formatLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        formatLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        hintPanel.add(hintLabel);
        hintPanel.add(Box.createVerticalStrut(6));
        hintPanel.add(formatLabel);
        hintPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 4. 组装内容面板
        JPanel contentPanel = new JPanel(new BorderLayout(0, 5));
        contentPanel.add(hintPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // 5. 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton okButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // 6. 创建对话框
        JDialog dialog = new JDialog(parentWindow, I18nUtil.getMessage(MessageKeys.CSV_BULK_EDIT), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setSize(750, 550);
        dialog.setMinimumSize(new Dimension(600, 400));
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(parentWindow); // 相对于父窗口（CSV数据管理对话框）居中

        // 7. 按钮事件处理
        okButton.addActionListener(e -> {
            parseBulkCsvText(textArea.getText(), tableModel, headers, csvTable, applyEmptyCellRenderer);
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        // 8. 支持 ESC 键关闭对话框
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // 9. 设置默认按钮
        dialog.getRootPane().setDefaultButton(okButton);

        // 10. 显示对话框
        dialog.setVisible(true);
    }

    /**
     * 解析批量编辑的 CSV 文本内容
     * 支持标准 CSV 格式（逗号分隔）
     * 空行会被忽略
     */
    private void parseBulkCsvText(String text, DefaultTableModel tableModel, List<String> headers, JTable csvTable, Runnable applyEmptyCellRenderer) {
        if (text == null || text.trim().isEmpty()) {
            // 如果文本为空，清空所有数据行
            tableModel.setRowCount(0);
            return;
        }

        // 先停止单元格编辑
        if (csvTable.isEditing()) {
            csvTable.getCellEditor().stopCellEditing();
        }

        // 清空现有数据
        tableModel.setRowCount(0);

        for (List<String> values : CsvDataUtil.parseCsvText(buildBulkEditCsvText(headers, text)).rows()) {
            if (values.isEmpty()) {
                continue;
            }

            // 创建新行
            Object[] rowData = new Object[headers.size()];
            for (int i = 0; i < headers.size() && i < values.size(); i++) {
                rowData[i] = values.get(i);
            }

            // 填充剩余列为空字符串
            for (int i = values.size(); i < headers.size(); i++) {
                rowData[i] = "";
            }

            tableModel.addRow(rowData);
        }

        // 重新应用空单元格渲染器
        if (applyEmptyCellRenderer != null) {
            applyEmptyCellRenderer.run();
        }
    }

    private String buildBulkEditCsvText(List<String> headers, String text) {
        StringBuilder csvText = new StringBuilder();
        csvText.append(buildCsvHeaderLine(headers)).append('\n');
        csvText.append(text);
        return csvText.toString();
    }

    private String buildCsvHeaderLine(List<String> headers) {
        StringBuilder headerLine = new StringBuilder();
        for (int i = 0; i < headers.size(); i++) {
            if (i > 0) {
                headerLine.append(',');
            }
            headerLine.append(escapeCsvValue(headers.get(i)));
        }
        return headerLine.toString();
    }

    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\n") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
            String validationMsg = CsvDataUtil.validateCsvFile(selectedFile);
            if (CharSequenceUtil.isNotBlank(validationMsg)) {
                JOptionPane.showMessageDialog(this, validationMsg,
                        I18nUtil.getMessage(MessageKeys.CSV_FILE_VALIDATION_FAILED), JOptionPane.ERROR_MESSAGE);
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
                csvSourceName = selectedFile.getName();
                csvData = newCsvData;
                csvHeaders = CsvDataUtil.getCsvHeaders(selectedFile); // 获取列标题
                notifyChangeListener();
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
        return Collections.emptyMap();
    }

    private void applyState(CsvState state, boolean notifyChange) {
        if (state == null || state.getRows().isEmpty()) {
            csvFile = null;
            csvSourceName = null;
            csvData = null;
            csvHeaders = null;
        } else {
            csvFile = null;
            csvSourceName = CharSequenceUtil.isBlank(state.getSourceName()) ? null : state.getSourceName();
            csvHeaders = copyHeaders(state.getHeaders());
            csvData = copyRows(state.getRows());
        }
        updateCsvStatus();
        if (notifyChange) {
            notifyChangeListener();
        }
    }

    private String getCurrentSourceName() {
        if (csvFile != null) {
            return csvFile.getName();
        }
        return csvSourceName;
    }

    private String getCurrentSourceDisplayName() {
        String sourceName = getCurrentSourceName();
        return CharSequenceUtil.isNotBlank(sourceName)
                ? sourceName
                : I18nUtil.getMessage(MessageKeys.CSV_MANUAL_CREATED);
    }

    private void notifyChangeListener() {
        if (changeListener != null) {
            changeListener.run();
        }
    }

    private static List<String> copyHeaders(List<String> headers) {
        if (headers == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(headers);
    }

    private static List<Map<String, String>> copyRows(List<Map<String, String>> rows) {
        List<Map<String, String>> copiedRows = new ArrayList<>();
        if (rows == null) {
            return copiedRows;
        }
        for (Map<String, String> row : rows) {
            copiedRows.add(row == null ? new LinkedHashMap<>() : new LinkedHashMap<>(row));
        }
        return copiedRows;
    }
}
