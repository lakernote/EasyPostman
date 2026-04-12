package com.laker.postman.common.component;

import cn.hutool.core.text.CharSequenceUtil;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.button.CSVButton;
import com.laker.postman.common.component.button.CloseButton;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * CSV 数据管理面板 - 独立的CSV功能组件
 */
@Slf4j
public class CsvDataPanel extends JPanel {

    private static final int CSV_TABLE_MIN_COLUMN_WIDTH = 140;
    private static final int CSV_MANUAL_DIALOG_MIN_WIDTH = 620;
    private static final int CSV_MANAGE_DIALOG_MIN_WIDTH = 820;
    private static final int CSV_MANAGE_DIALOG_MIN_HEIGHT = 580;
    private static final float CSV_STRIPE_ALPHA = 0.035f;
    private static final int CSV_TOOLBAR_BUTTON_HEIGHT = 32;
    private static final int CSV_FOOTER_BUTTON_HEIGHT = 34;

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
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(false);

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBorder(BorderFactory.createEmptyBorder(18, 22, 14, 22));
        dialog.add(rootPanel, BorderLayout.CENTER);

        // 顶部说明面板
        JPanel topPanel = new JPanel(new BorderLayout(0, 8));

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_DIALOG_TITLE));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +2));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        JLabel descLabel = new JLabel("<html><body style='width:520px'>"
                + I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_DESCRIPTION)
                + "</body></html>");
        descLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        topPanel.add(descLabel, BorderLayout.CENTER);

        rootPanel.add(topPanel, BorderLayout.NORTH);

        // 中间内容面板
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);

        // 列标题输入
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 0;
        JLabel headersLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_CREATE_MANUAL_COLUMN_HEADERS));
        headersLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        contentPanel.add(headersLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField headersField = new JTextField();
        headersField.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        headersField.setText("username,password,email");
        headersField.setColumns(36);
        contentPanel.add(headersField, gbc);

        // 占位符提示
        gbc.gridx = 1;
        gbc.gridy = 1;
        JLabel placeholderLabel = new JLabel("eg: username,password,email");
        placeholderLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.ITALIC, -2)); // 比标准字体小2号
        placeholderLabel.setForeground(ModernColors.getTextHint());
        contentPanel.add(placeholderLabel, gbc);

        rootPanel.add(contentPanel, BorderLayout.CENTER);

        // 底部按钮面板
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JButton createBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK), true);
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

        JButton cancelBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL), false);
        cancelBtn.addActionListener(e -> dialog.dispose());

        bottomPanel.add(createBtn);
        bottomPanel.add(cancelBtn);
        rootPanel.add(bottomPanel, BorderLayout.SOUTH);

        dialog.getRootPane().setDefaultButton(createBtn);
        dialog.pack();
        dialog.setSize(Math.max(dialog.getWidth(), CSV_MANUAL_DIALOG_MIN_WIDTH), dialog.getHeight());
        dialog.setLocationRelativeTo(SingletonFactory.getInstance(MainFrame.class));
        SwingUtilities.invokeLater(headersField::requestFocusInWindow);

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
        descArea.setFocusable(false);
        descArea.setRequestFocusEnabled(false);
        descArea.setCaretPosition(0);
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

        JButton selectFileBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_SELECT_FILE), false);
        selectFileBtn.setIcon(IconUtil.createThemed("icons/file.svg", 16, 16));
        selectFileBtn.setHorizontalAlignment(SwingConstants.LEFT);
        selectFileBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectFileBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JButton createManualBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.CSV_MENU_CREATE_MANUAL), false);
        createManualBtn.setIcon(new FlatSVGIcon("icons/plus.svg", 16, 16));
        createManualBtn.setHorizontalAlignment(SwingConstants.LEFT);
        createManualBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        createManualBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JButton manageDataBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_MANAGE_DATA), false);
        manageDataBtn.setIcon(IconUtil.createThemed("icons/code.svg", 16, 16));
        manageDataBtn.setHorizontalAlignment(SwingConstants.LEFT);
        manageDataBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        manageDataBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        manageDataBtn.setEnabled(csvData != null && !csvData.isEmpty());

        JButton clearBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_CLEAR_DATA), false);
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
        JButton closeBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLOSE), false);
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
        manageDialog.setSize(860, 620);
        manageDialog.setMinimumSize(new Dimension(CSV_MANAGE_DIALOG_MIN_WIDTH, CSV_MANAGE_DIALOG_MIN_HEIGHT));
        manageDialog.setLocationRelativeTo(SingletonFactory.getInstance(MainFrame.class));
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
        configureCsvTable(csvTable);

        // 创建带样式的滚动面板
        JScrollPane scrollPane = new JScrollPane(csvTable);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.getBorderLightColor()), // 主题适配的边框色
                BorderFactory.createEmptyBorder(8, 8, 8, 8))); // 参考 EasyTablePanel 的边框样式
        installResponsiveTableLayout(csvTable, scrollPane);

        // 创建表格容器面板，应用背景色
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 10));
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        manageDialog.add(tablePanel, BorderLayout.CENTER);

        // 底部面板
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // 工具栏
        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        JButton bulkEditBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.CSV_BULK_EDIT), false);
        bulkEditBtn.setIcon(IconUtil.createThemed("icons/edit.svg", 16, 16));
        styleToolbarButton(bulkEditBtn);
        List<String> finalHeaders = headers;
        bulkEditBtn.addActionListener(e -> showBulkEditDialog(manageDialog, editTableModel, csvTable, finalHeaders, () -> csvTable.repaint()));

        JButton addRowBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_ADD_ROW), false);
        addRowBtn.setIcon(IconUtil.createThemed("icons/plus.svg", 16, 16));
        styleToolbarButton(addRowBtn);
        addRowBtn.addActionListener(e -> editTableModel.addRow(new Object[finalHeaders.size()]));

        JButton deleteRowBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_DELETE_ROW), false);
        deleteRowBtn.setIcon(IconUtil.createThemed("icons/clear.svg", 16, 16));
        styleToolbarButton(deleteRowBtn);
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

        JButton addColumnBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_ADD_COLUMN), false);
        addColumnBtn.setIcon(IconUtil.createThemed("icons/plus.svg", 16, 16));
        styleToolbarButton(addColumnBtn);
        addColumnBtn.addActionListener(e -> {
            String columnName = JOptionPane.showInputDialog(manageDialog, I18nUtil.getMessage(MessageKeys.CSV_ENTER_COLUMN_NAME),
                    I18nUtil.getMessage(MessageKeys.CSV_ADD_COLUMN), JOptionPane.PLAIN_MESSAGE);
            if (columnName != null && !columnName.trim().isEmpty()) {
                columnName = columnName.trim();
                editTableModel.addColumn(columnName);
                csvTable.repaint();
                refreshResponsiveTableLayout(csvTable, scrollPane);
            }
        });

        JButton deleteColumnBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.CSV_BUTTON_DELETE_COLUMN), false);
        deleteColumnBtn.setIcon(IconUtil.createThemed("icons/clear.svg", 16, 16));
        styleToolbarButton(deleteColumnBtn);
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

                csvTable.repaint();
                refreshResponsiveTableLayout(csvTable, scrollPane);
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
        JScrollPane helpScrollPane = new JScrollPane(helpText,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        helpScrollPane.setBorder(BorderFactory.createEmptyBorder());
        helpScrollPane.setOpaque(false);
        helpScrollPane.getViewport().setOpaque(false);
        helpScrollPane.setPreferredSize(new Dimension(0, 112));
        helpPanel.add(helpScrollPane, BorderLayout.CENTER);
        bottomPanel.add(helpPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton saveBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.BUTTON_SAVE), true);
        saveBtn.setIcon(IconUtil.createThemed("icons/save.svg", 16, 16));
        styleFooterButton(saveBtn);
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

        JButton cancelBtn = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL), false);
        styleFooterButton(cancelBtn);
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

    private void installResponsiveTableLayout(JTable table, JScrollPane scrollPane) {
        refreshResponsiveTableLayout(table, scrollPane);
        scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> refreshResponsiveTableLayout(table, scrollPane));
            }
        });
    }

    private void refreshResponsiveTableLayout(JTable table, JScrollPane scrollPane) {
        int columnCount = table.getColumnCount();
        if (columnCount <= 0) {
            return;
        }

        int viewportWidth = scrollPane.getViewport().getExtentSize().width;
        if (viewportWidth <= 0) {
            viewportWidth = scrollPane.getViewport().getWidth();
        }
        if (viewportWidth <= 0) {
            viewportWidth = scrollPane.getPreferredSize().width;
        }

        if (viewportWidth <= 0) {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            for (int i = 0; i < columnCount; i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(CSV_TABLE_MIN_COLUMN_WIDTH);
            }
            return;
        }

        int usableViewportWidth = Math.max(viewportWidth - 16, 0);
        int minTotalWidth = CSV_TABLE_MIN_COLUMN_WIDTH * columnCount;

        if (usableViewportWidth >= minTotalWidth) {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            int calculatedWidth = usableViewportWidth / columnCount;
            for (int i = 0; i < columnCount; i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(calculatedWidth);
            }
        } else {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            for (int i = 0; i < columnCount; i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(CSV_TABLE_MIN_COLUMN_WIDTH);
            }
        }

        table.revalidate();
        table.repaint();
    }

    private void styleToolbarButton(JButton button) {
        button.setPreferredSize(null);
        button.setMinimumSize(new Dimension(0, CSV_TOOLBAR_BUTTON_HEIGHT));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, CSV_TOOLBAR_BUTTON_HEIGHT));
    }

    private void styleFooterButton(JButton button) {
        Dimension preferred = button.getPreferredSize();
        int width = Math.max(112, preferred.width + 6);
        button.setPreferredSize(new Dimension(width, CSV_FOOTER_BUTTON_HEIGHT));
    }

    private void configureCsvTable(JTable table) {
        Color tableBackground = UIManager.getColor("Table.background");
        if (tableBackground == null) {
            tableBackground = Color.WHITE;
        }
        Color softSelection = blendColor(tableBackground, ModernColors.PRIMARY, ModernColors.isDarkTheme() ? 0.28f : 0.14f);

        table.setRowHeight(30);
        table.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        table.setForeground(ModernColors.getTextPrimary());
        table.setBackground(tableBackground);
        table.setSelectionBackground(softSelection);
        table.setSelectionForeground(ModernColors.getTextPrimary());
        table.setGridColor(ModernColors.getBorderLightColor());
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setRowMargin(0);
        table.setOpaque(true);
        table.setFillsViewportHeight(true);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setSurrendersFocusOnKeystroke(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.putClientProperty("JTable.autoStartsEdit", Boolean.TRUE);
        table.putClientProperty("csv.hoveredRow", -1);

        JTableHeader header = table.getTableHeader();
        header.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        header.setBackground(UIManager.getColor("TableHeader.background"));
        header.setForeground(ModernColors.getTextPrimary());
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getBorderLightColor()));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 34));

        table.setDefaultRenderer(Object.class, new CsvTableCellRenderer());
        table.setDefaultEditor(Object.class, createCsvCellEditor());
        setupCsvTableKeyboardNavigation(table);

        table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                Object oldValue = table.getClientProperty("csv.hoveredRow");
                int oldRow = oldValue instanceof Integer ? (Integer) oldValue : -1;
                if (row != oldRow) {
                    table.putClientProperty("csv.hoveredRow", row);
                    table.repaint();
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0 || col < 0 || !table.isCellEditable(row, col)) {
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    if (table.editCellAt(row, col)) {
                        selectAllEditorText(table.getEditorComponent());
                    }
                });
            }

            @Override
            public void mouseExited(MouseEvent e) {
                table.putClientProperty("csv.hoveredRow", -1);
                table.repaint();
            }
        });
    }

    private void setupCsvTableKeyboardNavigation(JTable table) {
        InputMap inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = table.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("TAB"), "csv.nextCell");
        inputMap.put(KeyStroke.getKeyStroke("shift TAB"), "csv.prevCell");
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "csv.downCell");
        inputMap.put(KeyStroke.getKeyStroke("shift ENTER"), "csv.upCell");

        actionMap.put("csv.nextCell", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                navigateCsvTable(table, 0, 1);
            }
        });
        actionMap.put("csv.prevCell", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                navigateCsvTable(table, 0, -1);
            }
        });
        actionMap.put("csv.downCell", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                navigateCsvTable(table, 1, 0);
            }
        });
        actionMap.put("csv.upCell", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                navigateCsvTable(table, -1, 0);
            }
        });
    }

    private void navigateCsvTable(JTable table, int rowDelta, int columnDelta) {
        int row = table.getSelectedRow();
        int column = table.getSelectedColumn();
        if (row < 0 || column < 0) {
            row = 0;
            column = 0;
        }

        if (table.isEditing()) {
            int editingRow = table.getEditingRow();
            int editingColumn = table.getEditingColumn();
            if (!table.getCellEditor().stopCellEditing()) {
                return;
            }
            if (editingRow >= 0) {
                row = editingRow;
            }
            if (editingColumn >= 0) {
                column = editingColumn;
            }
        }

        int targetRow = row + rowDelta;
        int targetColumn = column + columnDelta;

        if (columnDelta != 0) {
            if (targetColumn >= table.getColumnCount()) {
                targetColumn = 0;
                targetRow = Math.min(row + 1, table.getRowCount() - 1);
            } else if (targetColumn < 0) {
                targetColumn = table.getColumnCount() - 1;
                targetRow = Math.max(row - 1, 0);
            }
        }

        targetRow = Math.max(0, Math.min(targetRow, table.getRowCount() - 1));
        targetColumn = Math.max(0, Math.min(targetColumn, table.getColumnCount() - 1));

        table.changeSelection(targetRow, targetColumn, false, false);
        final int nextRow = targetRow;
        final int nextColumn = targetColumn;
        SwingUtilities.invokeLater(() -> {
            if (table.editCellAt(nextRow, nextColumn)) {
                selectAllEditorText(table.getEditorComponent());
            } else {
                table.requestFocusInWindow();
            }
        });
    }

    private DefaultCellEditor createCsvCellEditor() {
        JTextField editorField = new JTextField();
        editorField.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        editorField.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        editorField.setBackground(ModernColors.getInputBackgroundColor());
        editorField.setForeground(ModernColors.getTextPrimary());
        editorField.setCaretColor(ModernColors.PRIMARY);
        editorField.setSelectionColor(blendColor(ModernColors.getInputBackgroundColor(), ModernColors.PRIMARY, 0.12f));
        editorField.setSelectedTextColor(ModernColors.getTextPrimary());

        return new DefaultCellEditor(editorField) {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                Component component = super.getTableCellEditorComponent(table, value, isSelected, row, column);
                if (component instanceof JTextField textField) {
                    textField.setText(value == null ? "" : String.valueOf(value));
                    textField.setBackground(ModernColors.getInputBackgroundColor());
                    textField.setForeground(ModernColors.getTextPrimary());
                    textField.setSelectionColor(blendColor(ModernColors.getInputBackgroundColor(), ModernColors.PRIMARY, 0.12f));
                    textField.setSelectedTextColor(ModernColors.getTextPrimary());
                    textField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(blendColor(ModernColors.getInputBackgroundColor(), ModernColors.PRIMARY, 0.35f)),
                            BorderFactory.createEmptyBorder(0, 8, 0, 8)));
                    SwingUtilities.invokeLater(textField::selectAll);
                }
                return component;
            }
        };
    }

    private void selectAllEditorText(Component editorComponent) {
        if (editorComponent instanceof JTextField textField) {
            textField.requestFocusInWindow();
            textField.selectAll();
        }
    }

    private static Color getStripeBackground(Color base) {
        Color alternate = UIManager.getColor("Table.alternateRowColor");
        if (alternate != null) {
            return alternate;
        }
        return blendColor(base, FlatLaf.isLafDark() ? Color.WHITE : Color.BLACK, CSV_STRIPE_ALPHA);
    }

    private static Color blendColor(Color base, Color blend, float alpha) {
        int r = Math.min(255, Math.max(0, Math.round(base.getRed() * (1 - alpha) + blend.getRed() * alpha)));
        int g = Math.min(255, Math.max(0, Math.round(base.getGreen() * (1 - alpha) + blend.getGreen() * alpha)));
        int b = Math.min(255, Math.max(0, Math.round(base.getBlue() * (1 - alpha) + blend.getBlue() * alpha)));
        return new Color(r, g, b);
    }

    private static final class CsvTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            setFont(FontsUtil.getDefaultFont(Font.PLAIN));

            String text = value == null ? "" : String.valueOf(value);
            boolean isEmpty = text.trim().isEmpty();
            setText(text);

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                Object hoveredValue = table.getClientProperty("csv.hoveredRow");
                int hoveredRow = hoveredValue instanceof Integer ? (Integer) hoveredValue : -1;
                Color base = row % 2 == 0 ? table.getBackground() : getStripeBackground(table.getBackground());
                if (row == hoveredRow) {
                    base = blendColor(base, table.getSelectionBackground(), 0.32f);
                }
                setBackground(isEmpty ? blendColor(base, ModernColors.getEmptyCellBackground(), 0.55f) : base);
                setForeground(ModernColors.getTextPrimary());
            }
            return this;
        }
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
