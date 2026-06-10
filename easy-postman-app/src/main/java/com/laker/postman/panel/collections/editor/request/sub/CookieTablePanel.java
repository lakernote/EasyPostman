package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.request.model.CookieInfo;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.*;
import com.laker.postman.common.component.setting.SettingsInputStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.panel.http.runtime.SwingHttpRuntimeInteractionAdapter;
import com.laker.postman.http.runtime.cookie.HttpCookieStore;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * CookieTablePanel: 高仿Postman，展示和管理所有Cookie（含属性/删除/清空）
 * 支持搜索、排序、双击编辑、图标按钮、空状态提示
 */
public class CookieTablePanel extends JPanel {
    private final JTable table;
    private final DefaultTableModel model;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final SearchTextField searchField = new SearchTextField();
    private final JPanel emptyStatePanel;
    private final transient Runnable cookieListener = this::loadCookies;

    public CookieTablePanel() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        ToolWindowSurfaceStyle.applyDialogSurface(this);

        // 顶部搜索和操作栏
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // 中间表格区域（包含空状态提示）
        JPanel centerPanel = new JPanel(new CardLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(centerPanel);

        // 创建表格
        String[] columns = {
                I18nUtil.getMessage(MessageKeys.COOKIE_COLUMN_NAME),
                I18nUtil.getMessage(MessageKeys.COOKIE_COLUMN_VALUE),
                I18nUtil.getMessage(MessageKeys.COOKIE_COLUMN_DOMAIN),
                I18nUtil.getMessage(MessageKeys.COOKIE_COLUMN_PATH),
                I18nUtil.getMessage(MessageKeys.COOKIE_COLUMN_EXPIRES),
                I18nUtil.getMessage(MessageKeys.COOKIE_COLUMN_SECURE),
                I18nUtil.getMessage(MessageKeys.COOKIE_COLUMN_HTTPONLY)
        };

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                // Secure 和 HttpOnly 列使用 Boolean 类型以显示复选框
                if (column == 5 || column == 6) {
                    return Boolean.class;
                }
                return String.class;
            }
        };

        table = new JTable(model);
        setupTable();

        // 添加排序和过滤支持
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        ToolWindowSurfaceStyle.applyTableScrollPaneCard(scrollPane, table);
        // 空状态提示
        emptyStatePanel = createEmptyStatePanel();

        centerPanel.add(scrollPane, "table");
        centerPanel.add(emptyStatePanel, "empty");
        add(centerPanel, BorderLayout.CENTER);

        // 双击编辑
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    editSelectedCookie();
                }
            }
        });

        // 注册监听器并加载数据
        HttpCookieStore.setNotificationDispatcher(SwingHttpRuntimeInteractionAdapter.callbackDispatcher());
        HttpCookieStore.registerCookieChangeListener(cookieListener);
        loadCookies();

        // 禁用搜索框的自动聚焦
        searchField.setFocusable(false);
        SwingUtilities.invokeLater(() -> {
            searchField.setFocusable(true);
            table.requestFocusInWindow(); // 让表格获得焦点
        });
    }

    /**
     * 创建顶部面板（搜索框和按钮）
     */
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(8, 0));
        ToolWindowSurfaceStyle.applyDialogSurface(topPanel);

        // 左侧搜索框（SearchTextField 已自带图标、占位符和清除按钮）
        searchField.setPlaceholderText(I18nUtil.getMessage(MessageKeys.COOKIE_SEARCH_PLACEHOLDER));
        searchField.setPreferredSize(new Dimension(250, 28));
        searchField.setMaximumSize(new Dimension(250, 28));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterTable();
            }

            public void removeUpdate(DocumentEvent e) {
                filterTable();
            }

            public void changedUpdate(DocumentEvent e) {
                filterTable();
            }
        });

        topPanel.add(searchField, BorderLayout.WEST);

        // 右侧按钮
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnPanel.setOpaque(false);

        PlusButton btnAdd = new PlusButton();

        EditButton btnEdit = new EditButton();

        CloseButton btnDelete = new CloseButton();
        btnDelete.setToolTipText("Delete");

        ClearButton btnClear = new ClearButton();

        RefreshButton btnRefresh = new RefreshButton();

        btnAdd.addActionListener(e -> addCookieDialog());
        btnEdit.addActionListener(e -> editSelectedCookie());
        btnDelete.addActionListener(e -> deleteSelectedCookie());
        btnClear.addActionListener(e -> clearAllCookies());
        btnRefresh.addActionListener(e -> HttpCookieStore.refreshCookies());

        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);
        btnPanel.add(btnRefresh);

        topPanel.add(btnPanel, BorderLayout.EAST);

        return topPanel;
    }


    /**
     * 配置表格样式
     */
    private void setupTable() {
        // 设置表格样式
        table.setRowHeight(28);
        table.setShowGrid(true);
        table.setFocusable(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        table.getTableHeader().setFont(FontsUtil.getDefaultFont(Font.BOLD));

        // 设置列宽
        setColumnWidths();

        // 设置单元格渲染器
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        // Value 列左对齐，可能很长
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(SwingConstants.LEFT);

        table.getColumnModel().getColumn(0).setCellRenderer(leftRenderer);  // Name
        table.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);  // Value
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Domain
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Path
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Expires
    }

    /**
     * 设置列宽
     */
    private void setColumnWidths() {
        TableColumn column;

        // Name - 150px
        column = table.getColumnModel().getColumn(0);
        column.setPreferredWidth(150);
        column.setMinWidth(100);

        // Value - 200px
        column = table.getColumnModel().getColumn(1);
        column.setPreferredWidth(200);
        column.setMinWidth(100);

        // Domain - 180px
        column = table.getColumnModel().getColumn(2);
        column.setPreferredWidth(180);
        column.setMinWidth(100);

        // Path - 100px
        column = table.getColumnModel().getColumn(3);
        column.setPreferredWidth(100);
        column.setMinWidth(60);

        // Expires - 150px
        column = table.getColumnModel().getColumn(4);
        column.setPreferredWidth(150);
        column.setMinWidth(100);

        // Secure - 80px
        column = table.getColumnModel().getColumn(5);
        column.setPreferredWidth(80);
        column.setMinWidth(60);

        // HttpOnly - 80px
        column = table.getColumnModel().getColumn(6);
        column.setPreferredWidth(90);
        column.setMinWidth(60);
    }

    /**
     * 创建空状态提示标签
     */
    private JPanel createEmptyStatePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(panel);
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel iconLabel = new JLabel();
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        try {
            iconLabel.setIcon(IconUtil.createThemed("icons/cookie.svg", 48, 48));
        } catch (Exception e) {
            // 图标加载失败也不影响
        }

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.COOKIE_EMPTY_STATE), SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 2));
        titleLabel.setForeground(ModernColors.getTextSecondary());

        JLabel hintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.COOKIE_EMPTY_STATE_HINT), SwingConstants.CENTER);
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        hintLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        hintLabel.setForeground(ModernColors.getTextHint());

        content.add(iconLabel);
        content.add(Box.createVerticalStrut(12));
        content.add(titleLabel);
        content.add(Box.createVerticalStrut(4));
        content.add(hintLabel);
        panel.add(content);
        return panel;
    }

    /**
     * 过滤表格
     */
    private void filterTable() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
            searchField.setNoResult(false);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            searchField.setNoResult(table.getRowCount() == 0);
        }
    }

    private void loadCookies() {
        model.setRowCount(0);
        List<CookieInfo> cookies = HttpCookieStore.getAllCookieInfos();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (CookieInfo c : cookies) {
            String expires = c.expires > 0 ? sdf.format(c.expires) : "Session";
            model.addRow(new Object[]{
                    c.name,
                    c.value,
                    c.domain,
                    c.path,
                    expires,
                    c.secure,   // Boolean 类型，自动显示为复选框
                    c.httpOnly  // Boolean 类型，自动显示为复选框
            });
        }

        // 切换显示状态（表格 or 空状态）
        Container parent = table.getParent().getParent().getParent();
        if (parent instanceof JPanel && parent.getLayout() instanceof CardLayout) {
            CardLayout layout = (CardLayout) parent.getLayout();
            if (cookies.isEmpty()) {
                layout.show(parent, "empty");
            } else {
                layout.show(parent, "table");
            }
        }
    }

    /**
     * 编辑选中的 Cookie
     */
    private void editSelectedCookie() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.COOKIE_ERROR_NO_SELECTION),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 获取实际行索引（考虑排序）
        int modelRow = table.convertRowIndexToModel(row);

        String name = (String) model.getValueAt(modelRow, 0);
        String value = (String) model.getValueAt(modelRow, 1);
        String domain = (String) model.getValueAt(modelRow, 2);
        String path = (String) model.getValueAt(modelRow, 3);
        boolean secure = (Boolean) model.getValueAt(modelRow, 5);
        boolean httpOnly = (Boolean) model.getValueAt(modelRow, 6);

        // 显示编辑对话框（预填充当前值）
        showCookieDialog(name, value, domain, path, secure, httpOnly, true);
    }

    private void deleteSelectedCookie() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.COOKIE_ERROR_NO_SELECTION),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 获取实际行索引（考虑排序）
        int modelRow = table.convertRowIndexToModel(row);

        String name = (String) model.getValueAt(modelRow, 0);
        String domain = (String) model.getValueAt(modelRow, 2);
        String path = (String) model.getValueAt(modelRow, 3);

        // 确认删除
        int confirm = JOptionPane.showConfirmDialog(this,
                String.format(I18nUtil.getMessage(MessageKeys.COOKIE_DELETE_CONFIRM), name, domain),
                I18nUtil.getMessage(MessageKeys.COOKIE_DELETE_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            HttpCookieStore.removeCookie(name, domain, path);
        }
    }

    private void clearAllCookies() {
        int confirm = JOptionPane.showConfirmDialog(this,
                I18nUtil.getMessage(MessageKeys.COOKIE_DIALOG_CLEAR_CONFIRM),
                I18nUtil.getMessage(MessageKeys.COOKIE_DIALOG_CLEAR_CONFIRM_TITLE), JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            HttpCookieStore.clearAllCookies();
        }
    }

    private void addCookieDialog() {
        showCookieDialog("", "", "", "/", false, false, false);
    }

    /**
     * 显示 Cookie 对话框（新增或编辑）
     */
    private void showCookieDialog(String defaultName, String defaultValue, String defaultDomain,
                                  String defaultPath, boolean defaultSecure, boolean defaultHttpOnly,
                                  boolean isEdit) {
        JPanel panel = new JPanel(new MigLayout(
                "insets 14 16 14 16, fillx, wrap 2, novisualpadding",
                "[right]12[grow,fill]",
                "[]8[]8[]8[]12[]4[]"
        ));
        ToolWindowSurfaceStyle.applyDialogSurface(panel);

        JTextField nameField = new JTextField(defaultName, 20);
        JTextField valueField = new JTextField(defaultValue, 20);
        JTextField domainField = new JTextField(defaultDomain, 20);
        JTextField pathField = new JTextField(defaultPath, 20);
        for (JTextField field : List.of(nameField, valueField, domainField, pathField)) {
            SettingsInputStyle.apply(field);
        }
        nameField.setEditable(!isEdit); // 编辑模式下名称不可修改
        domainField.setEditable(!isEdit); // 编辑模式下域名不可修改
        pathField.setEditable(!isEdit); // 编辑模式下路径不可修改

        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.COOKIE_FIELD_NAME) + ":"));
        panel.add(nameField, "growx");
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.COOKIE_FIELD_VALUE) + ":"));
        panel.add(valueField, "growx");
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.COOKIE_FIELD_DOMAIN) + ":"));
        panel.add(domainField, "growx");
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.COOKIE_FIELD_PATH) + ":"));
        panel.add(pathField, "growx");

        // Secure 和 HttpOnly 复选框
        JCheckBox secureBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.COOKIE_FIELD_SECURE), defaultSecure);
        JCheckBox httpOnlyBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.COOKIE_FIELD_HTTPONLY), defaultHttpOnly);
        secureBox.setOpaque(false);
        httpOnlyBox.setOpaque(false);
        panel.add(secureBox, "span 2, growx");
        panel.add(httpOnlyBox, "span 2, growx");

        // 添加说明
        if (isEdit) {
            JLabel hint = new JLabel(I18nUtil.getMessage(MessageKeys.COOKIE_EDIT_HINT));
            hint.setFont(FontsUtil.getDefaultFontWithOffset(Font.ITALIC, -1));
            hint.setForeground(ModernColors.getTextHint());
            panel.add(hint, "span 2, growx, gaptop 6");
        }

        String title = isEdit ?
                I18nUtil.getMessage(MessageKeys.COOKIE_DIALOG_EDIT_TITLE) :
                I18nUtil.getMessage(MessageKeys.COOKIE_DIALOG_ADD_TITLE);

        int result = showCookieFormDialog(panel, title);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String value = valueField.getText().trim();
            String domain = domainField.getText().trim();
            String path = pathField.getText().trim();
            boolean secure = secureBox.isSelected();
            boolean httpOnly = httpOnlyBox.isSelected();

            // 验证必填字段
            if (name.isEmpty() || domain.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.COOKIE_DIALOG_ERROR_EMPTY),
                        I18nUtil.getMessage(MessageKeys.COOKIE_DIALOG_ERROR_TITLE),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 编辑模式：先删除旧的，再添加新的
            if (isEdit) {
                HttpCookieStore.removeCookie(defaultName, defaultDomain, defaultPath);
            }

            HttpCookieStore.addCookie(name, value, domain, path, secure, httpOnly);
        }
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        return label;
    }

    private int showCookieFormDialog(JPanel formPanel, String title) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);

        JPanel rootPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(rootPanel);
        rootPanel.add(formPanel, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        ToolWindowSurfaceStyle.applyDialogFooter(footerPanel);
        JButton cancelButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_CANCEL), false);
        JButton okButton = ModernButtonFactory.createButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK), true);
        final int[] result = {JOptionPane.CANCEL_OPTION};
        cancelButton.addActionListener(e -> dialog.dispose());
        okButton.addActionListener(e -> {
            result[0] = JOptionPane.OK_OPTION;
            dialog.dispose();
        });
        footerPanel.add(cancelButton);
        footerPanel.add(okButton);
        rootPanel.add(footerPanel, BorderLayout.SOUTH);

        dialog.setContentPane(rootPanel);
        dialog.getRootPane().setDefaultButton(okButton);
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        dialog.pack();
        dialog.setMinimumSize(new Dimension(420, dialog.getHeight()));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return result[0];
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        HttpCookieStore.unregisterCookieChangeListener(cookieListener);
    }
}
