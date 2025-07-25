package com.laker.postman.panel.functional.table;

import com.laker.postman.model.HttpResponse;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class RunnerTableModel extends AbstractTableModel {

    private final String[] columns = {"Select", "Name", "URL", "Method", "Time(ms)", "Status", "Assertion", "Detail"};
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
        if (columnIndex == 4) return Long.class;
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0 || columnIndex == 7;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        RunnerRowData row = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> row.selected;
            case 1 -> row.name;
            case 2 -> row.url;
            case 3 -> row.method;
            case 4 -> row.cost;
            case 5 -> row.status;
            case 6 -> row.assertion;
            case 7 -> "View";
            default -> null;
        };
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        RunnerRowData row = rows.get(rowIndex);
        switch (columnIndex) {
            case 0 -> row.selected = (Boolean) aValue;
            case 4 -> row.cost = (Long) aValue;
            case 5 -> row.status = (String) aValue;
            case 6 -> row.assertion = (String) aValue;
        }
        fireTableCellUpdated(rowIndex, columnIndex);
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

    public void setResponse(int rowIndex, HttpResponse resp, long cost, String status, String assertion) {
        RunnerRowData row = rows.get(rowIndex);
        row.response = resp;
        row.cost = cost;
        row.status = status;
        row.assertion = assertion;
        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    public void moveRow(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) return;
        RunnerRowData row = rows.remove(fromIndex);
        if (toIndex > fromIndex) toIndex--;
        rows.add(toIndex, row);
        fireTableDataChanged();
    }
}