package com.laker.postman.panel.runner;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.datatransfer.*;
import java.io.IOException;

/**
 * 支持JTable行拖拽的TransferHandler
 */
public class TableRowTransferHandler extends TransferHandler {
    private final JTable table;
    private int[] rows = null;
    private int addIndex = -1; // 新插入的行索引
    private int addCount = 0; // 插入的行数

    public TableRowTransferHandler(JTable table) {
        this.table = table;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        rows = table.getSelectedRows();
        return new StringSelection(""); // 内容无关紧要
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDrop() && support.getComponent() == table;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) return false;
        JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
        int index = dl.getRow();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        if (rows == null) return false;
        // 复制行数据
        Object[][] rowData = new Object[rows.length][model.getColumnCount()];
        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < model.getColumnCount(); j++) {
                rowData[i][j] = model.getValueAt(rows[i], j);
            }
        }
        // 插入到目标位置
        addIndex = index;
        addCount = rowData.length;
        for (int i = 0; i < rowData.length; i++) {
            model.insertRow(index++, rowData[i]);
        }
        // 选中新插入的行
        table.getSelectionModel().setSelectionInterval(addIndex, addIndex + addCount - 1);
        return true;
    }

    @Override
    protected void exportDone(JComponent c, Transferable t, int action) {
        if (action == MOVE && rows != null) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            // 删除原有行（从后往前删）
            if (addIndex > rows[0]) {
                for (int i = rows.length - 1; i >= 0; i--) {
                    model.removeRow(rows[i]);
                }
            } else {
                for (int i = rows.length - 1; i >= 0; i--) {
                    model.removeRow(rows[i] + addCount);
                }
            }
        }
        rows = null;
        addCount = 0;
        addIndex = -1;
    }
}

