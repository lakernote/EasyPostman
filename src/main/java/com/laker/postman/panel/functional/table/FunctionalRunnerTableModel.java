package com.laker.postman.panel.functional.table;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.TimeDisplayUtil;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class FunctionalRunnerTableModel extends AbstractTableModel {

    private static final int COLUMN_COUNT = 7;
    private final List<RunnerRowData> rows = new ArrayList<>();

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_SELECT);
            case 1 -> I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_NAME);
            case 2 -> I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_URL);
            case 3 -> I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_METHOD);
            case 4 -> I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_STATUS);
            case 5 -> I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_TIME);
            case 6 -> I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_COLUMN_RESULT);
            default -> "";
        };
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
            case 4 -> row.status != null && !row.status.isEmpty() ? row.status : "-";
            case 5 -> row.cost > 0 ? TimeDisplayUtil.formatElapsedTime(row.cost) : "-";
            case 6 -> row.assertion != null && !row.assertion.isEmpty() ? row.assertion : "-";
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

    /**
     * 删除指定行
     */
    public void removeRow(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < rows.size()) {
            rows.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }

    /**
     * 删除选中的所有行
     *
     * @return 删除的行数
     */
    public int removeSelectedRows() {
        List<RunnerRowData> toRemove = new ArrayList<>();
        for (RunnerRowData row : rows) {
            if (row.selected) {
                toRemove.add(row);
            }
        }
        rows.removeAll(toRemove);
        if (!toRemove.isEmpty()) {
            fireTableDataChanged();
        }
        return toRemove.size();
    }

    /**
     * 设置所有行的选中状态
     */
    public void setAllSelected(boolean selected) {
        for (RunnerRowData row : rows) {
            row.selected = selected;
        }
        fireTableDataChanged();
    }

    /**
     * 获取选中的请求数量
     */
    public int getSelectedCount() {
        return (int) rows.stream().filter(row -> row.selected).count();
    }

    /**
     * 获取所有行数据（用于保存测试集）
     */
    public List<RunnerRowData> getAllRows() {
        return new ArrayList<>(rows);
    }

    /**
     * 检查是否有选中的行
     */
    public boolean hasSelectedRows() {
        return rows.stream().anyMatch(row -> row.selected);
    }
}