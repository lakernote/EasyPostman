package com.laker.postman.panel.collections.edit;

import lombok.Getter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

/**
 * 响应头面板，只读展示响应头内容
 */
@Getter
public class ResponseHeadersPanel extends JPanel {
    private final JTable headersTable;
    private final DefaultTableModel tableModel;

    public ResponseHeadersPanel() {
        setLayout(new BorderLayout());
        tableModel = new DefaultTableModel(new Object[]{"Name", "Value"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        headersTable = new JTable(tableModel);
        headersTable.setFillsViewportHeight(true);
        headersTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        headersTable.setRowSorter(new TableRowSorter<>(tableModel));
        JScrollPane scrollPane = new JScrollPane(headersTable);
        add(scrollPane, BorderLayout.CENTER);
        // 右键菜单
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copySelected = new JMenuItem("Copy Selected");
        JMenuItem copyAll = new JMenuItem("Copy All");
        popupMenu.add(copySelected);
        popupMenu.add(copyAll);
        headersTable.setComponentPopupMenu(popupMenu);
        // 复制选中行
        copySelected.addActionListener(e -> copySelectedRows());
        // 复制全部
        copyAll.addActionListener(e -> copyAllRows());
        // 双击复制 value
        headersTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && headersTable.getSelectedRow() != -1) {
                    int row = headersTable.getSelectedRow();
                    int col = headersTable.getSelectedColumn();
                    if (col == 1) { // Value 列
                        String value = String.valueOf(tableModel.getValueAt(headersTable.convertRowIndexToModel(row), 1));
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
                    }
                }
            }
        });
    }

    public void setHeaders(Map<String, List<String>> headers) {
        tableModel.setRowCount(0);
        if (headers == null || headers.isEmpty()) return;
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                tableModel.addRow(new Object[]{key, value});
            }
        }
    }

    private void copySelectedRows() {
        int[] rows = headersTable.getSelectedRows();
        if (rows.length == 0) return;
        StringBuilder sb = new StringBuilder();
        for (int row : rows) {
            int modelRow = headersTable.convertRowIndexToModel(row);
            sb.append(tableModel.getValueAt(modelRow, 0)).append(": ")
                    .append(tableModel.getValueAt(modelRow, 1)).append("\n");
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
    }

    private void copyAllRows() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            sb.append(tableModel.getValueAt(i, 0)).append(": ")
                    .append(tableModel.getValueAt(i, 1)).append("\n");
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
    }
}