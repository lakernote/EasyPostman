package com.laker.postman.panel.functional.table;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class RunnerTableModel extends AbstractTableModel {

    private final String[] columns = {"Select", "Name", "URL", "Method"};
    private final List<RunnerRowData> rows = new ArrayList<>();

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) return Boolean.class;
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        RunnerRowData row = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> row.selected;
            case 1 -> row.name;
            case 2 -> row.url;
            case 3 -> row.method;
            default -> null;
        };
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        RunnerRowData row = rows.get(rowIndex);
        if (columnIndex == 0) {
            row.selected = (Boolean) aValue;
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    public void addRow(RunnerRowData row) {
        rows.add(row);
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }

    public void clear() {
        int size = rows.size();
        if (size > 0) {
            rows.clear();
            fireTableRowsDeleted(0, size - 1);
        }
    }

    public RunnerRowData getRow(int rowIndex) {
        return rows.get(rowIndex);
    }

    public void moveRow(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) return;
        RunnerRowData row = rows.remove(fromIndex);
        if (toIndex > fromIndex) toIndex--;
        rows.add(toIndex, row);
        fireTableDataChanged();
    }
}