package com.laker.postman.common.component.table;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 改进的行拖动TransferHandler实现
 * 移除拖动阈值，使拖动更加灵敏
 */
@Slf4j
public class ImprovedTableRowTransferHandler extends TransferHandler {
    private final DataFlavor localObjectFlavor;
    private int[] rows = null;
    private int addIndex = -1;
    private int addCount = 0;
    private final DefaultTableModel model;
    private final Consumer<Boolean> dragStateCallback;

    public ImprovedTableRowTransferHandler(DefaultTableModel model, Consumer<Boolean> dragStateCallback) {
        this.model = model;
        this.dragStateCallback = dragStateCallback;
        localObjectFlavor = new DataFlavor(List.class, "List of items");
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTable table = (JTable) c;
        rows = table.getSelectedRows();

        if (rows == null || rows.length == 0) {
            return null;
        }

        // 通知开始拖拽，禁用自动补空行
        if (dragStateCallback != null) {
            dragStateCallback.accept(true);
        }

        List<Object[]> transferredObjects = new ArrayList<>();
        for (int row : rows) {
            Object[] rowData = new Object[model.getColumnCount()];
            for (int col = 0; col < model.getColumnCount(); col++) {
                rowData[col] = model.getValueAt(row, col);
            }
            transferredObjects.add(rowData);
        }

        return new ImprovedTableRowsTransferable(transferredObjects, localObjectFlavor);
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
        DefaultTableModel targetModel = (DefaultTableModel) target.getModel();
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
                targetModel.insertRow(index++, value);
            }
            return true;
        } catch (UnsupportedFlavorException | IOException e) {
            log.error("Error importing data: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    protected void exportDone(JComponent c, Transferable t, int action) {
        try {
            if ((action == MOVE) && rows != null && addCount > 0) {
                // 删除原始行
                for (int i = rows.length - 1; i >= 0; i--) {
                    if (rows[i] >= addIndex) {
                        model.removeRow(rows[i] + addCount);
                    } else {
                        model.removeRow(rows[i]);
                    }
                }
            }
        } finally {
            // 拖拽完成，重新启用自动补空行
            if (dragStateCallback != null) {
                dragStateCallback.accept(false);
            }
            rows = null;
            addCount = 0;
            addIndex = -1;
        }
    }

    /**
     * 自定义 Transferable 用于表格行拖动
     */
    private record ImprovedTableRowsTransferable(List<Object[]> data, DataFlavor flavor) implements Transferable {

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
}
