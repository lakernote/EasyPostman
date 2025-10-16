package com.laker.postman.common.component.table.generic;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用的表格模型，支持泛型数据行。
 * 子类只需实现 getColumnCount/getColumnName/getValueAtRow
 */
public abstract class BaseTableModel<T> extends AbstractTableModel {
    protected final List<T> data = new ArrayList<>();

    public void addRow(T row) {
        data.add(row);
        fireTableRowsInserted(data.size() - 1, data.size() - 1);
    }

    public void clear() {
        data.clear();
        fireTableDataChanged();
    }

    public T getRow(int row) {
        return data.get(row);
    }

    public List<T> getRows() {
        return new ArrayList<>(data);
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public abstract int getColumnCount();

    @Override
    public abstract String getColumnName(int col);

    @Override
    public Object getValueAt(int row, int col) {
        return getValueAtRow(data.get(row), col);
    }

    /**
     * 子类实现：返回某行某列的值
     */
    protected abstract Object getValueAtRow(T row, int col);
}

