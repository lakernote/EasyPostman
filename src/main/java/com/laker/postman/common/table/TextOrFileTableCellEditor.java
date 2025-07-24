package com.laker.postman.common.table;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import java.awt.*;
import java.util.WeakHashMap;

/**
 * 文本或文件组合单元格编辑器
 * 增强版：支持文件类型过滤、动态配置、编辑器缓存
 */
public class TextOrFileTableCellEditor extends DefaultCellEditor {
    // 使用缓存避免频繁创建对象
    private static final WeakHashMap<Component, FileCellEditor> FILE_EDITOR_CACHE = new WeakHashMap<>();

    // 可配置的类型列索引，默认为1
    private int typeColumnIndex = 1;

    private final DefaultCellEditor textEditor;
    private FileCellEditor fileEditor;
    private String currentType;

    // 文件类型过滤器配置
    private String fileFilterDescription;
    private String[] fileExtensions;

    /**
     * 创建默认的文本或文件组合编辑器
     */
    public TextOrFileTableCellEditor() {
        super(new JTextField());

        // 创建自定义文本编辑器，添加自动选择文本功能
        JTextField textField = new JTextField();
        textField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                textField.selectAll();
            }
        });
        textEditor = new DefaultCellEditor(textField);
    }

    /**
     * 设置文件类型过滤器
     *
     * @param description 过滤器描述
     * @param extensions  文件扩展名，如 "json", "txt"
     */
    public void setFileFilter(String description, String... extensions) {
        this.fileFilterDescription = description;
        this.fileExtensions = extensions;

        // 如果文件编辑器已创建，则立即应用过滤器
        if (fileEditor != null) {
            fileEditor.setFileFilter(description, extensions);
        }
    }

    /**
     * 获取缓存的文件编辑器或创建新的
     */
    private FileCellEditor getOrCreateFileEditor(Component parent) {
        FileCellEditor editor = FILE_EDITOR_CACHE.get(parent);
        if (editor == null) {
            editor = new FileCellEditor(parent);

            // 如果有文件过滤器配置，应用到新创建的编辑器
            if (fileFilterDescription != null && fileExtensions != null) {
                editor.setFileFilter(fileFilterDescription, fileExtensions);
            }

            FILE_EDITOR_CACHE.put(parent, editor);
        }
        return editor;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        // 从类型列获取单元格类型
        Object type = table.getValueAt(row, typeColumnIndex);
        currentType = type == null ? "" : type.toString();

        // 根据类型选择合适的编辑器
        if (TableUIConstants.FILE_TYPE.equals(currentType)) {
            fileEditor = getOrCreateFileEditor(table);
            return fileEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        } else {
            fileEditor = null;
            return textEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
    }

    @Override
    public Object getCellEditorValue() {
        if (fileEditor != null && TableUIConstants.FILE_TYPE.equals(currentType)) {
            Object fileValue = fileEditor.getCellEditorValue();
            if (fileValue != null && !fileValue.toString().isEmpty()) {
                return fileValue;
            }
        }
        return textEditor.getCellEditorValue();
    }

    /**
     * 获取当前选中的文件
     * 如果当前不是文件类型，或未选择文件，返回null
     */
    public java.io.File getSelectedFile() {
        if (fileEditor != null && TableUIConstants.FILE_TYPE.equals(currentType)) {
            return fileEditor.getSelectedFile();
        }
        return null;
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