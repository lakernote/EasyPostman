package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
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
        // 设置边距
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        // 表格模型和表格
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
        JMenuItem copySelected = new JMenuItem(I18nUtil.getMessage(MessageKeys.RESPONSE_HEADERS_COPY_SELECTED));
        JMenuItem copyCell = new JMenuItem(I18nUtil.getMessage(MessageKeys.RESPONSE_HEADERS_COPY_CELL));
        JMenuItem copyAll = new JMenuItem(I18nUtil.getMessage(MessageKeys.RESPONSE_HEADERS_COPY_ALL));
        JMenuItem selectAll = new JMenuItem(I18nUtil.getMessage(MessageKeys.RESPONSE_HEADERS_SELECT_ALL));
        popupMenu.add(copySelected);
        popupMenu.add(copyCell);
        popupMenu.add(copyAll);
        popupMenu.add(selectAll);
        headersTable.setComponentPopupMenu(popupMenu);
        // 复制选中行
        copySelected.addActionListener(e -> copySelectedRows());
        // 复制全部
        copyAll.addActionListener(e -> copyAllRows());
        // 复制单元格
        copyCell.addActionListener(e -> copySelectedCell());
        // 全选
        selectAll.addActionListener(e -> headersTable.selectAll());
        // 鼠标右键自动选中行/单元格
        headersTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int row = headersTable.rowAtPoint(e.getPoint());
                    int col = headersTable.columnAtPoint(e.getPoint());
                    if (row != -1 && col != -1) {
                        headersTable.changeSelection(row, col, false, false);
                    }
                }
            }

            @Override
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

    private void copySelectedCell() {
        int row = headersTable.getSelectedRow();
        int col = headersTable.getSelectedColumn();
        if (row == -1 || col == -1) return;
        int modelRow = headersTable.convertRowIndexToModel(row);
        String value = String.valueOf(tableModel.getValueAt(modelRow, col));
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
    }
}