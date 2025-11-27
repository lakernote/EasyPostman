package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.topmenu.TopMenuBar;
import com.laker.postman.service.setting.ShortcutManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

/**
 * 快捷键设置面板
 */
public class ShortcutSettingsPanel extends JPanel {
    private JTable shortcutTable;
    private ShortcutTableModel tableModel;

    public ShortcutSettingsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // 标题面板
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SHORTCUT_SETTINGS_TITLE));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titlePanel.add(titleLabel, BorderLayout.NORTH);

        // 添加双击编辑提示
        JLabel hintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SHORTCUT_DOUBLE_CLICK_HINT));
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.PLAIN, 12f));
        hintLabel.setForeground(new java.awt.Color(128, 128, 128));
        titlePanel.add(hintLabel, BorderLayout.SOUTH);

        add(titlePanel, BorderLayout.NORTH);

        // 创建表格
        tableModel = new ShortcutTableModel();
        shortcutTable = new JTable(tableModel);
        shortcutTable.setRowHeight(32);
        shortcutTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 设置列宽（只有2列）
        shortcutTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        shortcutTable.getColumnModel().getColumn(1).setPreferredWidth(200);

        // 设置渲染器
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        shortcutTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);

        // 添加双击编辑功能
        shortcutTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editShortcut();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(shortcutTable);
        add(scrollPane, BorderLayout.CENTER);

        // 底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton editButton = new JButton(I18nUtil.getMessage(MessageKeys.SHORTCUT_EDIT));
        editButton.addActionListener(e -> editShortcut());

        JButton resetButton = new JButton(I18nUtil.getMessage(MessageKeys.SHORTCUT_RESET));
        resetButton.addActionListener(e -> resetToDefaults());

        buttonPanel.add(editButton);
        buttonPanel.add(resetButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 编辑快捷键
     */
    private void editShortcut() {
        int selectedRow = shortcutTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.SHORTCUT_SELECT_FIRST),
                    I18nUtil.getMessage(MessageKeys.TIP),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ShortcutItem item = tableModel.getShortcutItem(selectedRow);
        ShortcutEditDialog dialog = new ShortcutEditDialog(
                SwingUtilities.getWindowAncestor(this), item);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            tableModel.fireTableRowsUpdated(selectedRow, selectedRow);
            // 立即应用更改
            applySettings();
        }
    }

    /**
     * 重置为默认值
     */
    private void resetToDefaults() {
        int result = JOptionPane.showConfirmDialog(this,
                I18nUtil.getMessage(MessageKeys.SHORTCUT_RESET_CONFIRM),
                I18nUtil.getMessage(MessageKeys.TIP),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            ShortcutManager.resetToDefaults();
            tableModel.refresh();
            // 立即应用更改
            applySettings();
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.SHORTCUT_RESET_SUCCESS),
                    I18nUtil.getMessage(MessageKeys.SUCCESS),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 应用设置
     */
    public void applySettings() {
        ShortcutManager.save();
        // 通知需要重新注册快捷键
        notifyShortcutChanged();
    }

    /**
     * 通知快捷键已更改，需要重新注册
     */
    private void notifyShortcutChanged() {
        // 重新注册 RequestEditPanel 的快捷键
        SingletonFactory.getInstance(
                RequestEditPanel.class).reloadShortcuts();

        // 重新加载菜单栏快捷键
        SingletonFactory.getInstance(
                TopMenuBar.class).reloadShortcuts();
    }

    /**
     * 快捷键表格模型
     */
    private static class ShortcutTableModel extends AbstractTableModel {
        private final List<ShortcutItem> shortcuts;
        private final String[] columnNames;

        public ShortcutTableModel() {
            this.shortcuts = new ArrayList<>();
            this.columnNames = new String[]{
                    I18nUtil.getMessage(MessageKeys.SHORTCUT_ACTION),
                    I18nUtil.getMessage(MessageKeys.SHORTCUT_KEY)
            };
            loadShortcuts();
        }

        private void loadShortcuts() {
            shortcuts.clear();
            shortcuts.add(new ShortcutItem(ShortcutManager.NEW_REQUEST,
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_ADD_REQUEST)));
            shortcuts.add(new ShortcutItem(ShortcutManager.SAVE_REQUEST,
                    I18nUtil.getMessage(MessageKeys.SAVE_REQUEST)));
            shortcuts.add(new ShortcutItem(ShortcutManager.CLOSE_CURRENT_TAB,
                    I18nUtil.getMessage(MessageKeys.TAB_CLOSE_CURRENT)));
            shortcuts.add(new ShortcutItem(ShortcutManager.CLOSE_OTHER_TABS,
                    I18nUtil.getMessage(MessageKeys.TAB_CLOSE_OTHERS)));
            shortcuts.add(new ShortcutItem(ShortcutManager.CLOSE_ALL_TABS,
                    I18nUtil.getMessage(MessageKeys.TAB_CLOSE_ALL)));
            shortcuts.add(new ShortcutItem(ShortcutManager.EXIT_APP,
                    I18nUtil.getMessage(MessageKeys.EXIT_APP)));
        }

        public void refresh() {
            loadShortcuts();
            fireTableDataChanged();
        }

        public ShortcutItem getShortcutItem(int row) {
            return shortcuts.get(row);
        }

        @Override
        public int getRowCount() {
            return shortcuts.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ShortcutItem item = shortcuts.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> item.getActionName();
                case 1 -> ShortcutManager.getShortcutText(item.getId());
                default -> "";
            };
        }
    }

    /**
     * 快捷键项
     */
    static class ShortcutItem {
        private final String id;
        private final String actionName;

        public ShortcutItem(String id, String actionName) {
            this.id = id;
            this.actionName = actionName;
        }

        public String getId() {
            return id;
        }

        public String getActionName() {
            return actionName;
        }
    }

    /**
     * 快捷键编辑对话框
     */
    private static class ShortcutEditDialog extends JDialog {
        private final ShortcutItem item;
        private boolean confirmed = false;
        private int newKeyCode = -1;
        private int newModifiers = 0;
        private final JLabel currentKeyLabel;
        private final JTextField keyInputField;

        public ShortcutEditDialog(Window owner, ShortcutItem item) {
            super(owner, I18nUtil.getMessage(MessageKeys.SHORTCUT_EDIT_TITLE), ModalityType.APPLICATION_MODAL);
            this.item = item;

            setLayout(new BorderLayout(10, 10));
            ((JPanel) getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));

            // 中心面板
            JPanel centerPanel = new JPanel(new GridLayout(3, 2, 10, 10));

            centerPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.SHORTCUT_ACTION) + ":"));
            centerPanel.add(new JLabel(item.getActionName()));

            centerPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.SHORTCUT_CURRENT) + ":"));
            currentKeyLabel = new JLabel(ShortcutManager.getShortcutText(item.getId()));
            centerPanel.add(currentKeyLabel);

            centerPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.SHORTCUT_NEW) + ":"));
            keyInputField = new JTextField();
            keyInputField.setEditable(false);
            keyInputField.setFocusable(true);
            keyInputField.setText(I18nUtil.getMessage(MessageKeys.SHORTCUT_PRESS_KEY));
            keyInputField.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {
                    // 不处理
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    captureKey(e);
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    // 不处理
                }
            });
            centerPanel.add(keyInputField);

            add(centerPanel, BorderLayout.CENTER);

            // 按钮面板
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

            JButton okButton = new JButton(I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_SAVE));
            okButton.addActionListener(e -> confirm());

            JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_CANCEL));
            cancelButton.addActionListener(e -> dispose());

            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);

            add(buttonPanel, BorderLayout.SOUTH);

            setSize(450, 200);
            setLocationRelativeTo(owner);
        }

        private void captureKey(KeyEvent e) {
            int keyCode = e.getKeyCode();
            int modifiers = e.getModifiersEx();

            // 忽略纯修饰键
            if (keyCode == KeyEvent.VK_SHIFT || keyCode == KeyEvent.VK_CONTROL ||
                    keyCode == KeyEvent.VK_ALT || keyCode == KeyEvent.VK_META) {
                return;
            }

            // 检查冲突
            String conflict = ShortcutManager.checkConflict(item.getId(), keyCode, modifiers);
            if (conflict != null) {
                keyInputField.setText(I18nUtil.getMessage(MessageKeys.SHORTCUT_CONFLICT) + ": " + conflict);
                return;
            }

            this.newKeyCode = keyCode;
            this.newModifiers = modifiers;

            // 显示新快捷键
            ShortcutManager.ShortcutConfig tempConfig = new ShortcutManager.ShortcutConfig(
                    "", "", keyCode, modifiers, "", "");
            keyInputField.setText(tempConfig.getDisplayText());
        }

        private void confirm() {
            if (newKeyCode == -1) {
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.SHORTCUT_NOT_SET),
                        I18nUtil.getMessage(MessageKeys.TIP),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            ShortcutManager.setShortcut(item.getId(), newKeyCode, newModifiers);
            confirmed = true;
            dispose();
        }

        public boolean isConfirmed() {
            return confirmed;
        }
    }
}

