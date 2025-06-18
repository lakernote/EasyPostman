package com.laker.postman.common.table;

import javax.swing.*;
import javax.swing.event.CellEditorListener;

public class TextOrFileTableCellEditor extends DefaultCellEditor {
    private final DefaultCellEditor textEditor = new DefaultCellEditor(new JTextField());
    // 直接传 table 作为父组件即可
    private FileCellEditor fileEditor;

    public TextOrFileTableCellEditor() {
        super(new JTextField());
    }

    @Override
    public java.awt.Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        Object type = table.getValueAt(row, 1);
        if ("File".equals(type)) {
            // 每次都新建，传 table 作为父组件
            fileEditor = new FileCellEditor(table);
            return fileEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        } else {
            return textEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
    }

    @Override
    public Object getCellEditorValue() {
        if (fileEditor != null) {
            Object fileValue = fileEditor.getCellEditorValue();
            if (fileValue != null && !FileCellEditor.COLUMN_TEXT.equals(fileValue) && !fileValue.toString().isEmpty()) {
                return fileValue;
            }
        }
        return textEditor.getCellEditorValue();
    }

    @Override
    public boolean isCellEditable(java.util.EventObject anEvent) {
        return true;
    }

    @Override
    public boolean shouldSelectCell(java.util.EventObject anEvent) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        boolean textStopped = textEditor.stopCellEditing();
        boolean fileStopped = fileEditor == null || fileEditor.stopCellEditing();
        return textStopped && fileStopped;
    }

    @Override
    public void cancelCellEditing() {
        textEditor.cancelCellEditing();
        if (fileEditor != null) fileEditor.cancelCellEditing();
    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {
        textEditor.addCellEditorListener(l);
        if (fileEditor != null) fileEditor.addCellEditorListener(l);
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
        textEditor.removeCellEditorListener(l);
        if (fileEditor != null) fileEditor.removeCellEditorListener(l);
    }
}