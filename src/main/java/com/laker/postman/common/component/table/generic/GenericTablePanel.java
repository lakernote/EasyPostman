package com.laker.postman.common.component.table.generic;

import javax.swing.*;
import java.awt.*;

/**
 * 通用的表格面板，支持泛型TableModel。
 * 适用于 BaseTableModel<T> 搭配使用。
 */
public class GenericTablePanel<T> extends JPanel {
    protected final JTable table;
    protected final BaseTableModel<T> tableModel;

    public GenericTablePanel(BaseTableModel<T> model) {
        this.tableModel = model;
        this.table = new JTable(model);
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public JTable getTable() {
        return table;
    }

    public BaseTableModel<T> getTableModel() {
        return tableModel;
    }
}

