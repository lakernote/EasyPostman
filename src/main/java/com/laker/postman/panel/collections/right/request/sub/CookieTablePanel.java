package com.laker.postman.panel.collections.right.request.sub;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.button.*;
import com.laker.postman.model.CookieInfo;
import com.laker.postman.service.http.CookieService;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

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
    private final JLabel emptyLabel;
    private final transient Runnable cookieListener = this::loadCookies;

    public CookieTablePanel() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // 顶部搜索和操作栏
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // 中间表格区域（包含空状态提示）
        JPanel centerPanel = new JPanel(new CardLayout());

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
        // 空状态提示
        emptyLabel = createEmptyStateLabel();

        centerPanel.add(scrollPane, "table");
        centerPanel.add(emptyLabel, "empty");
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
        CookieService.registerCookieChangeListener(cookieListener);
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

        PlusButton btnAdd = new PlusButton();

        EditButton btnEdit = new EditButton();

        CloseButton btnDelete = new CloseButton();

        ClearButton btnClear = new ClearButton();

        RefreshButton btnRefresh = new RefreshButton();

        btnAdd.addActionListener(e -> addCookieDialog());
        btnEdit.addActionListener(e -> editSelectedCookie());
        btnDelete.addActionListener(e -> deleteSelectedCookie());
        btnClear.addActionListener(e -> clearAllCookies());
        btnRefresh.addActionListener(e -> CookieService.refreshCookies());

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
    private JLabel createEmptyStateLabel() {
        JLabel label = new JLabel(
                "<html><div style='text-align: center;'>" +
                        "<p style='font-size: 14px; color: #888;'>" +
                        I18nUtil.getMessage(MessageKeys.COOKIE_EMPTY_STATE) +
                        "</p><p style='font-size: 12px; color: #aaa;'>" +
                        I18nUtil.getMessage(MessageKeys.COOKIE_EMPTY_STATE_HINT) +
                        "</p></div></html>",
                SwingConstants.CENTER
        );
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +2));
        try {
            label.setIcon(new FlatSVGIcon("icons/cookie.svg", 48, 48));
        } catch (Exception e) {
            // 图标加载失败也不影响
        }
        label.setVerticalTextPosition(SwingConstants.BOTTOM);
        label.setHorizontalTextPosition(SwingConstants.CENTER);
        return label;
    }

    /**
     * 过滤表格
     */
    private void filterTable() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }

    private void loadCookies() {
        model.setRowCount(0);
        List<CookieInfo> cookies = CookieService.getAllCookieInfos();
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
            CookieService.removeCookie(name, domain, path);
        }
    }

    private void clearAllCookies() {
        int confirm = JOptionPane.showConfirmDialog(this,
                I18nUtil.getMessage(MessageKeys.COOKIE_DIALOG_CLEAR_CONFIRM),
                I18nUtil.getMessage(MessageKeys.COOKIE_DIALOG_CLEAR_CONFIRM_TITLE), JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            CookieService.clearAllCookies();
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
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Name 字段
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUtil.getMessage(MessageKeys.COOKIE_FIELD_NAME) + ":"), gbc);

        JTextField nameField = new JTextField(defaultName, 20);
        nameField.setEnabled(!isEdit); // 编辑模式下名称不可修改
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(nameField, gbc);

        // Value 字段
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUtil.getMessage(MessageKeys.COOKIE_FIELD_VALUE) + ":"), gbc);

        JTextField valueField = new JTextField(defaultValue, 20);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(valueField, gbc);

        // Domain 字段
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUtil.getMessage(MessageKeys.COOKIE_FIELD_DOMAIN) + ":"), gbc);

        JTextField domainField = new JTextField(defaultDomain, 20);
        domainField.setEnabled(!isEdit); // 编辑模式下域名不可修改
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(domainField, gbc);

        // Path 字段
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        panel.add(new JLabel(I18nUtil.getMessage(MessageKeys.COOKIE_FIELD_PATH) + ":"), gbc);

        JTextField pathField = new JTextField(defaultPath, 20);
        pathField.setEnabled(!isEdit); // 编辑模式下路径不可修改
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(pathField, gbc);

        // Secure 和 HttpOnly 复选框
        JCheckBox secureBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.COOKIE_FIELD_SECURE), defaultSecure);
        JCheckBox httpOnlyBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.COOKIE_FIELD_HTTPONLY), defaultHttpOnly);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(secureBox, gbc);

        gbc.gridy = 5;
        panel.add(httpOnlyBox, gbc);

        // 添加说明
        if (isEdit) {
            JLabel hint = new JLabel("<html><i style='color: #888; font-size: 11px;'>" +
                    I18nUtil.getMessage(MessageKeys.COOKIE_EDIT_HINT) + "</i></html>");
            gbc.gridy = 6;
            gbc.insets = new Insets(10, 5, 5, 5);
            panel.add(hint, gbc);
        }

        String title = isEdit ?
                I18nUtil.getMessage(MessageKeys.COOKIE_DIALOG_EDIT_TITLE) :
                I18nUtil.getMessage(MessageKeys.COOKIE_DIALOG_ADD_TITLE);

        int result = JOptionPane.showConfirmDialog(this, panel, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

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
                CookieService.removeCookie(defaultName, defaultDomain, defaultPath);
            }

            CookieService.addCookie(name, value, domain, path, secure, httpOnly);
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        CookieService.unregisterCookieChangeListener(cookieListener);
    }
}