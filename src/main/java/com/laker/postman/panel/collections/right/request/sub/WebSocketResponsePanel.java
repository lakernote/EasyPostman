package com.laker.postman.panel.collections.right.request.sub;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.component.SearchTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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

    private static final String[] COLUMN_NAMES = {"类型", "时间", "内容"};
    private static final String[] TYPE_FILTERS = {"全部", "发送", "接收", "连接", "断开", "警告", "信息", "二进制"};

    public WebSocketResponsePanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        // 顶部工具栏
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        typeFilterBox = new JComboBox<>(TYPE_FILTERS);
        searchField = new SearchTextField();
        clearButton = new JButton("清除消息");
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
        table.setRowHeight(22);
        table.getColumnModel().getColumn(0).setMaxWidth(32);
        table.getColumnModel().getColumn(0).setCellRenderer(new IconCellRenderer());
        table.getColumnModel().getColumn(1).setMaxWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(400);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

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
        filterAndShow();
    }

    public void clearMessages() {
        allRows.clear();
        filterAndShow();
    }

    private void filterAndShow() {
        String search = searchField.getText().trim().toLowerCase();
        String typeFilter = (String) typeFilterBox.getSelectedItem();
        List<MessageRow> filtered = allRows.stream()
                .filter(row -> ("全部".equals(typeFilter) || row.type.display.equals(typeFilter)))
                .filter(row -> search.isEmpty() || row.content.toLowerCase().contains(search))
                .toList();
        tableModel.setRowCount(0);
        for (MessageRow row : filtered) {
            tableModel.addRow(new Object[]{row.type.icon, row.time, row.content});
        }
    }

    // 消息类型
    public enum MessageType {
        SENT("发送", new FlatSVGIcon("icons/ws-send.svg", 16, 16)),
        RECEIVED("接收", new FlatSVGIcon("icons/ws-receive.svg", 16, 16)),
        CONNECTED("连接", new FlatSVGIcon("icons/ws-connect.svg", 16, 16)),
        CLOSED("断开", new FlatSVGIcon("icons/ws-close.svg", 16, 16)),
        WARNING("警告", new FlatSVGIcon("icons/warning.svg", 16, 16)),
        INFO("信息", new FlatSVGIcon("icons/ws-info.svg", 16, 16)),
        BINARY("二进制", new FlatSVGIcon("icons/binary.svg", 16, 16));
        public final String display;
        public final Icon icon;

        MessageType(String display, Icon icon) {
            this.display = display;
            this.icon = icon;
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
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setIcon(value instanceof Icon ? (Icon) value : null);
            setText("");
            setHorizontalAlignment(CENTER);
            return this;
        }
    }
}
