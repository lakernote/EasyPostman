package com.laker.postman.panel.collections.right.request.sub;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
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
import java.util.stream.Collectors;

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
        setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
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
        table.getColumnModel().getColumn(1).setMaxWidth(60);
        // 设置第2列（时间列）居中
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setPreferredWidth(400);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // 鼠标监听，点击第三列弹窗显示完整内容
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 2 && e.getClickCount() >= 1) {
                    String content = (String) table.getValueAt(row, col);
                    showContentDialog(content);
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
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
                .filter(row -> ("全部".equals(typeFilter) || row.type.display.equals(typeFilter)))
                .filter(row -> search.isEmpty() || row.content.toLowerCase().contains(search))
                .collect(Collectors.toList());
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

    // 弹窗显示完整内容，支持格式化和复制
    private void showContentDialog(String content) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "消息内容", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton copyBtn = new JButton("复制");
        JButton formatBtn = new JButton("格式化");
        JButton rawBtn = new JButton("原始");
        JButton cancelBtn = new JButton("取消");
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
