package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
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
public class ResponseHeadersPanel extends JPanel {
    private static final int COLUMN_NAME = 0;
    private static final int COLUMN_VALUE = 1;
    private static final int NAME_COLUMN_MIN_WIDTH = 140;
    private static final int NAME_COLUMN_MAX_WIDTH = 320;
    private static final int NAME_COLUMN_TEXT_PADDING = 36;
    private static final int VALUE_COLUMN_MIN_WIDTH = 240;

    private final JTable headersTable;
    private final DefaultTableModel tableModel;

    public ResponseHeadersPanel() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tableModel = new DefaultTableModel(new Object[]{
                I18nUtil.getMessage(MessageKeys.RESPONSE_HEADERS_COLUMN_NAME),
                I18nUtil.getMessage(MessageKeys.RESPONSE_HEADERS_COLUMN_VALUE)
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        headersTable = new JTable(tableModel);
        headersTable.setFillsViewportHeight(true);
        headersTable.setFocusable(false);
        headersTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        headersTable.setRowSorter(new TableRowSorter<>(tableModel));
        configureColumnWidths();
        JScrollPane scrollPane = new JScrollPane(headersTable);
        ToolWindowSurfaceStyle.applyTableScrollPaneCard(scrollPane, headersTable);
        add(scrollPane, BorderLayout.CENTER);
        JPopupMenu popupMenu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(popupMenu);
        JMenuItem copySelected = new JMenuItem(I18nUtil.getMessage(MessageKeys.RESPONSE_HEADERS_COPY_SELECTED));
        JMenuItem copyCell = new JMenuItem(I18nUtil.getMessage(MessageKeys.RESPONSE_HEADERS_COPY_CELL));
        JMenuItem copyAll = new JMenuItem(I18nUtil.getMessage(MessageKeys.RESPONSE_HEADERS_COPY_ALL));
        JMenuItem selectAll = new JMenuItem(I18nUtil.getMessage(MessageKeys.RESPONSE_HEADERS_SELECT_ALL));
        popupMenu.add(copySelected);
        popupMenu.add(copyCell);
        popupMenu.add(copyAll);
        popupMenu.add(selectAll);
        headersTable.setComponentPopupMenu(popupMenu);
        copySelected.addActionListener(e -> copySelectedRows());
        copyAll.addActionListener(e -> copyAllRows());
        copyCell.addActionListener(e -> copySelectedCell());
        selectAll.addActionListener(e -> headersTable.selectAll());
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
                    if (col == COLUMN_VALUE) {
                        String value = String.valueOf(
                                tableModel.getValueAt(headersTable.convertRowIndexToModel(row), COLUMN_VALUE));
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
                    }
                }
            }
        });
    }

    public void setHeaders(Map<String, List<String>> headers) {
        tableModel.setRowCount(0);
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                for (String value : entry.getValue()) {
                    tableModel.addRow(new Object[]{key, value});
                }
            }
        }
        updateNameColumnWidth();
    }

    private void configureColumnWidths() {
        TableColumn nameColumn = headersTable.getColumnModel().getColumn(COLUMN_NAME);
        nameColumn.setMinWidth(NAME_COLUMN_MIN_WIDTH);
        nameColumn.setPreferredWidth(NAME_COLUMN_MIN_WIDTH);

        TableColumn valueColumn = headersTable.getColumnModel().getColumn(COLUMN_VALUE);
        valueColumn.setMinWidth(VALUE_COLUMN_MIN_WIDTH);
    }

    private void updateNameColumnWidth() {
        int preferredWidth = calculateNameColumnPreferredWidth();
        TableColumn nameColumn = headersTable.getColumnModel().getColumn(COLUMN_NAME);
        nameColumn.setPreferredWidth(preferredWidth);
        nameColumn.setWidth(preferredWidth);
    }

    private int calculateNameColumnPreferredWidth() {
        FontMetrics metrics = headersTable.getFontMetrics(headersTable.getFont());
        int maxTextWidth = metrics.stringWidth(String.valueOf(tableModel.getColumnName(COLUMN_NAME)));
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            Object value = tableModel.getValueAt(row, COLUMN_NAME);
            if (value != null) {
                maxTextWidth = Math.max(maxTextWidth, metrics.stringWidth(value.toString()));
            }
        }
        return Math.max(NAME_COLUMN_MIN_WIDTH,
                Math.min(NAME_COLUMN_MAX_WIDTH, maxTextWidth + NAME_COLUMN_TEXT_PADDING));
    }

    private void copySelectedRows() {
        int[] rows = headersTable.getSelectedRows();
        if (rows.length == 0) return;
        StringBuilder sb = new StringBuilder();
        for (int row : rows) {
            int modelRow = headersTable.convertRowIndexToModel(row);
            sb.append(tableModel.getValueAt(modelRow, COLUMN_NAME)).append(": ")
                    .append(tableModel.getValueAt(modelRow, COLUMN_VALUE)).append("\n");
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
    }

    private void copyAllRows() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            sb.append(tableModel.getValueAt(i, COLUMN_NAME)).append(": ")
                    .append(tableModel.getValueAt(i, COLUMN_VALUE)).append("\n");
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
