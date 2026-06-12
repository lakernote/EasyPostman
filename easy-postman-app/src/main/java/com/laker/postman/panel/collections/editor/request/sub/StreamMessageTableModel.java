package com.laker.postman.panel.collections.editor.request.sub;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

final class StreamMessageTableModel<T> extends AbstractTableModel {

    private final String[] columnNames;
    private final CellValueProvider<T> valueProvider;
    private final List<T> rows = new ArrayList<>();

    StreamMessageTableModel(String[] columnNames, CellValueProvider<T> valueProvider) {
        this.columnNames = columnNames.clone();
        this.valueProvider = valueProvider;
    }

    @Override
    public int getRowCount() {
        return rows.size();
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
        return valueProvider.valueAt(rows.get(rowIndex), columnIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    void appendRows(List<T> rowsToAppend) {
        if (rowsToAppend == null || rowsToAppend.isEmpty()) {
            return;
        }
        int firstRow = rows.size();
        rows.addAll(rowsToAppend);
        fireTableRowsInserted(firstRow, rows.size() - 1);
    }

    void removeFirstRows(int count) {
        if (count <= 0 || rows.isEmpty()) {
            return;
        }
        int removedCount = Math.min(count, rows.size());
        rows.subList(0, removedCount).clear();
        fireTableRowsDeleted(0, removedCount - 1);
    }

    void setRows(List<T> newRows) {
        rows.clear();
        if (newRows != null && !newRows.isEmpty()) {
            rows.addAll(newRows);
        }
        fireTableDataChanged();
    }

    void clear() {
        if (rows.isEmpty()) {
            return;
        }
        int lastRow = rows.size() - 1;
        rows.clear();
        fireTableRowsDeleted(0, lastRow);
    }

    T getRow(int modelRow) {
        return rows.get(modelRow);
    }

    @FunctionalInterface
    interface CellValueProvider<T> {
        Object valueAt(T row, int column);
    }
}
