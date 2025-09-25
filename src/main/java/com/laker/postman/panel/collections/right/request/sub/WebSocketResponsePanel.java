package com.laker.postman.panel.collections.right.request.sub;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.model.MessageType;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket响应体面板，三列：icon、时间、内容，支持搜索、清除、类型过滤
 */
public class WebSocketResponsePanel extends JPanel {
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JComboBox<String> typeFilterBox;
    private final JTextField searchField;
    private final JButton clearButton;
    private final List<MessageRow> allRows = new ArrayList<>();

    private static final String[] COLUMN_NAMES = {
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_COLUMN_TYPE),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_COLUMN_TIME),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_COLUMN_CONTENT)
    };
    private static final String[] TYPE_FILTERS = {
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_ALL),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_SENT),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_RECEIVED),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_CONNECTED),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_CLOSED),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_WARNING),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_INFO),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_BINARY)
    };

    public WebSocketResponsePanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        // 顶部工具栏
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        typeFilterBox = new JComboBox<>(TYPE_FILTERS);
        searchField = new SearchTextField();
        clearButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR_MESSAGES));
        clearButton.setIcon(new FlatSVGIcon("icons/clear.svg", 16, 16));
        toolBar.add(searchField);
        toolBar.add(typeFilterBox);
        toolBar.add(clearButton);
        add(toolBar, BorderLayout.NORTH);

        // 表格
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.getColumnModel().getColumn(0).setMaxWidth(36);
        table.getColumnModel().getColumn(0).setCellRenderer(new IconCellRenderer());
        table.getColumnModel().getColumn(1).setMaxWidth(60);
        // 设置第2列（时间列）居中
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setPreferredWidth(400);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // 鼠标监听，右键第三列弹出菜单
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // 双击第3列弹窗
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    if (col == 2) {
                        String content = (String) table.getValueAt(row, 2);
                        showContentDialog(content);
                    }
                }
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    if (row >= 0 && col == 2) {
                        table.setRowSelectionInterval(row, row);
                        String content = (String) table.getValueAt(row, col);
                        JPopupMenu popupMenu = new JPopupMenu();
                        JMenuItem copyItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
                        JMenuItem detailItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.BUTTON_DETAIL));
                        copyItem.addActionListener(ev -> {
                            // 复制内容到剪贴板
                            StringSelection selection = new StringSelection(content);
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                        });
                        detailItem.addActionListener(ev -> showContentDialog(content));
                        popupMenu.add(copyItem);
                        popupMenu.add(detailItem);
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        add(scrollPane, BorderLayout.CENTER);

        // 美化表格
        JTableHeader header = table.getTableHeader();
        header.setFont(header.getFont().deriveFont(Font.BOLD, 12f));
        table.setForeground(new Color(40, 40, 40));
        table.setGridColor(new Color(220, 225, 235));
        table.setShowGrid(true);
        table.setSelectionBackground(new Color(220, 235, 255));
        table.setSelectionForeground(new Color(30, 60, 120));
        table.setRowHeight(24);
        table.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 230), 1));
        // 选中行加粗
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        });

        // 事件
        clearButton.addActionListener(e -> clearMessages());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterAndShow();
            }

            public void removeUpdate(DocumentEvent e) {
                filterAndShow();
            }

            public void changedUpdate(DocumentEvent e) {
                filterAndShow();
            }
        });
        typeFilterBox.addActionListener(e -> filterAndShow());
    }

    public void addMessage(MessageType type, String time, String content) {
        allRows.add(new MessageRow(type, time, content));
        SwingUtilities.invokeLater(this::filterAndShow);
    }

    public void clearMessages() {
        allRows.clear();
        SwingUtilities.invokeLater(this::filterAndShow);
    }

    private void filterAndShow() {
        // 确保在 EDT 内执行
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::filterAndShow);
            return;
        }
        String search = searchField.getText().trim().toLowerCase();
        String typeFilter = (String) typeFilterBox.getSelectedItem();
        List<MessageRow> filtered = allRows.stream()
                .filter(row -> (I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_ALL).equals(typeFilter)
                        || row.type.display.equals(typeFilter)))
                .filter(row -> search.isEmpty() || row.content.toLowerCase().contains(search))
                .toList();
        tableModel.setRowCount(0);
        for (MessageRow row : filtered) {
            tableModel.addRow(new Object[]{row.type.icon, row.time, row.content});
        }
    }


    // 行数据
    public static class MessageRow {
        public final MessageType type;
        public final String time;
        public final String content;

        public MessageRow(MessageType type, String time, String content) {
            this.type = type;
            this.time = time;
            this.content = content;
        }
    }

    // icon渲染
    private static class IconCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setIcon(value instanceof Icon ? (Icon) value : null);
            setText("");
            setHorizontalAlignment(CENTER);
            return this;
        }
    }

    // 弹窗显示完整内容，支持格式化和复制，支持ESC关闭
    private void showContentDialog(String content) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                I18nUtil.getMessage(MessageKeys.WEBSOCKET_DIALOG_TITLE), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        JButton formatBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_FORMAT));
        JButton rawBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_RAW));
        JButton cancelBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));
        btnPanel.add(copyBtn);
        btnPanel.add(formatBtn);
        btnPanel.add(rawBtn);
        btnPanel.add(cancelBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        dialog.setContentPane(panel);
        // 判断是否为JSON
        boolean isJson = JSONUtil.isTypeJSON(content);
        String[] rawContent = {content};
        textArea.setText(content);
        formatBtn.setEnabled(isJson);
        rawBtn.setEnabled(false);
        formatBtn.addActionListener(e -> {
            String formatted = formatJson(rawContent[0]);
            textArea.setText(formatted);
            formatBtn.setEnabled(false);
            rawBtn.setEnabled(true);
        });
        rawBtn.addActionListener(e -> {
            textArea.setText(rawContent[0]);
            formatBtn.setEnabled(isJson);
            rawBtn.setEnabled(false);
        });
        copyBtn.addActionListener(e -> {
            textArea.selectAll();
            textArea.copy();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());
        // 支持ESC关闭
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.getRootPane().setDefaultButton(cancelBtn);
        cancelBtn.requestFocusInWindow();
        dialog.setVisible(true);
    }


    // 简单格式化JSON
    private String formatJson(String str) {
        if (JSONUtil.isTypeJSON(str)) {
            JSON formatJson = JSONUtil.parse(str);
            return JSONUtil.toJsonPrettyStr(formatJson);
        }
        return str;

    }
}
