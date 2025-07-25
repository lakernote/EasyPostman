package com.laker.postman.panel.functional.table;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

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
        TableModel model = table.getModel();
        if (rows == null) return false;
        if (model instanceof RunnerTableModel runnerModel) {
            // 只支持单行拖拽（可扩展为多行）
            for (int from : rows) {
                int to = index;
                runnerModel.moveRow(from, to);
                if (to > from) index--; // 移动后下标变化
            }
            table.getSelectionModel().setSelectionInterval(index, index + rows.length - 1);
            return true;
        }
        // 复制行数据
        DefaultTableModel defModel = (DefaultTableModel) model;
        Object[][] rowData = new Object[rows.length][defModel.getColumnCount()];
        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < defModel.getColumnCount(); j++) {
                rowData[i][j] = defModel.getValueAt(rows[i], j);
            }
        }
        // 插入到目标位置
        addIndex = index;
        addCount = rowData.length;
        for (Object[] rowDatum : rowData) {
            defModel.insertRow(index++, rowDatum);
        }
        // 选中新插入的行
        table.getSelectionModel().setSelectionInterval(addIndex, addIndex + addCount - 1);
        return true;
    }

    @Override
    protected void exportDone(JComponent c, Transferable t, int action) {
        if (action == MOVE && rows != null) {
            TableModel model = table.getModel();
            if (model instanceof RunnerTableModel) {
                // 数据已在 moveRow 中同步，无需再删
            } else {
                DefaultTableModel defModel = (DefaultTableModel) model;
                // 删除原有行（从后往前删）
                if (addIndex > rows[0]) {
                    for (int i = rows.length - 1; i >= 0; i--) {
                        defModel.removeRow(rows[i]);
                    }
                } else {
                    for (int i = rows.length - 1; i >= 0; i--) {
                        defModel.removeRow(rows[i] + addCount);
                    }
                }
            }
        }
        rows = null;
        addCount = 0;
        addIndex = -1;
    }
}