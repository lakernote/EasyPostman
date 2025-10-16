package com.laker.postman.common.component.table;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 行拖动TransferHandler实现
 */
@Slf4j
public class TableRowTransferHandler extends TransferHandler {
    private final DataFlavor localObjectFlavor;
    private int[] rows = null;
    private int addIndex = -1; // 新插入行的索引
    private int addCount = 0;  // 插入的行数
    private final DefaultTableModel model;
    private final EasyTablePanel tablePanel;

    public TableRowTransferHandler(DefaultTableModel model, EasyTablePanel tablePanel) {
        this.model = model;
        this.tablePanel = tablePanel;
        localObjectFlavor = new DataFlavor(List.class, "List of items");
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTable table = (JTable) c;
        rows = table.getSelectedRows();
        List<Object[]> transferredObjects = new ArrayList<>();
        for (int row : rows) {
            Object[] rowData = new Object[model.getColumnCount()];
            for (int col = 0; col < model.getColumnCount(); col++) {
                rowData[col] = model.getValueAt(row, col);
            }
            transferredObjects.add(rowData);
        }
        // 使用自定义 Transferable
        return new TableRowsTransferable(transferredObjects, localObjectFlavor);
    }

    @Override
    public boolean canImport(TransferSupport info) {
        return info.isDrop() && info.isDataFlavorSupported(localObjectFlavor);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    public boolean importData(TransferSupport info) {
        if (!canImport(info)) return false;
        JTable target = (JTable) info.getComponent();
        DefaultTableModel model = (DefaultTableModel) target.getModel();
        int index = ((JTable.DropLocation) info.getDropLocation()).getRow();
        // 防止插入到自身内部
        if (rows != null && index >= rows[0] && index <= rows[rows.length - 1] + 1) {
            return false;
        }
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> values = (List<Object[]>) info.getTransferable().getTransferData(localObjectFlavor);
            addIndex = index;
            addCount = values.size();
            for (Object[] value : values) {
                model.insertRow(index++, value);
            }
            return true;
        } catch (UnsupportedFlavorException | IOException e) {
            log.error("Error importing data: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    protected void exportDone(JComponent c, Transferable t, int action) {
        if ((action == MOVE) && rows != null) {
            // 删除原始行
            if (addCount > 0) {
                for (int i = rows.length - 1; i >= 0; i--) {
                    if (rows[i] >= addIndex) {
                        model.removeRow(rows[i] + addCount);
                    } else {
                        model.removeRow(rows[i]);
                    }
                }
            }
        }
        rows = null;
        addCount = 0;
        addIndex = -1;
    }
}

/**
 * 自定义 Transferable 用于表格行拖动
 */
record TableRowsTransferable(List<Object[]> data, DataFlavor flavor) implements Transferable {

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{flavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return this.flavor.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
        return data;
    }
}