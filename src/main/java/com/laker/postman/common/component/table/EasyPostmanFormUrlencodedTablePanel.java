package com.laker.postman.common.component.table;

import com.laker.postman.model.HttpFormUrlencoded;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Form-Urlencoded 表格面板组件
 * 专门用于处理 application/x-www-form-urlencoded 类型的请求体数据
 * 支持 Enable、Key、Value 和 Delete 列结构
 */
@Slf4j
public class EasyPostmanFormUrlencodedTablePanel extends AbstractEasyPostmanTablePanel<HttpFormUrlencoded> {

    // Column indices
    private static final int COL_ENABLED = 0;
    private static final int COL_KEY = 1;
    private static final int COL_VALUE = 2;
    private static final int COL_DELETE = 3;

    /**
     * 构造函数，创建默认的 Form-Urlencoded 表格面板
     */
    public EasyPostmanFormUrlencodedTablePanel() {
        this(true, true);
    }

    /**
     * 构造函数，支持自定义配置
     *
     * @param popupMenuEnabled     是否启用右键菜单
     * @param autoAppendRowEnabled 是否启用自动补空行
     */
    public EasyPostmanFormUrlencodedTablePanel(boolean popupMenuEnabled, boolean autoAppendRowEnabled) {
        super(new String[]{"", "Key", "Value", ""});
        initializeComponents();
        initializeTableUI();
        setupCellRenderersAndEditors();
        if (popupMenuEnabled) {
            setupTableListeners();
        }
        if (autoAppendRowEnabled) {
            addAutoAppendRowFeature();
        }

        // Add initial empty row (same as ParamsTablePanel)
        addRow();
    }

    // ========== 实现抽象方法 ==========

    @Override
    protected int getEnabledColumnIndex() {
        return COL_ENABLED;
    }

    @Override
    protected int getDeleteColumnIndex() {
        return COL_DELETE;
    }

    @Override
    protected int getFirstEditableColumnIndex() {
        return COL_KEY;
    }

    @Override
    protected int getLastEditableColumnIndex() {
        return COL_VALUE;
    }

    @Override
    protected boolean isCellEditableForNavigation(int row, int column) {
        return column == COL_KEY || column == COL_VALUE;
    }

    @Override
    protected boolean hasContentInRow(int row) {
        String key = getStringValue(row, COL_KEY);
        String value = getStringValue(row, COL_VALUE);
        return !key.isEmpty() || !value.isEmpty();
    }

    // ========== 初始化方法 ==========

    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(237, 237, 237)),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        // Initialize table model with custom cell editing logic
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == COL_ENABLED) {
                    return Boolean.class;
                }
                return Object.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                if (!editable) {
                    return false;
                }

                // Checkbox column is always editable
                if (column == COL_ENABLED) {
                    return true;
                }

                // Delete column is not editable (uses custom renderer)
                if (column == COL_DELETE) {
                    return false;
                }

                // Key and Value columns are editable
                return column == COL_KEY || column == COL_VALUE;
            }
        };

        // Create table
        table = new JTable(tableModel);

        // Wrap table in JScrollPane to show headers
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    protected void initializeTableUI() {
        // 调用父类的通用UI配置
        super.initializeTableUI();

        // 设置列宽
        setEnabledColumnWidth(40);
        setDeleteColumnWidth(40);

        // Setup Tab key navigation to move between columns in the same row
        setupTabKeyNavigation();
    }

    private void setupCellRenderersAndEditors() {

        // Set editors for Key and Value columns
        setColumnEditor(COL_KEY, new EasyPostmanTextFieldCellEditor());
        setColumnEditor(COL_VALUE, new EasyPostmanTextFieldCellEditor());
        setColumnRenderer(COL_KEY, new EasyPostmanTextFieldCellRenderer());
        setColumnRenderer(COL_VALUE, new EasyPostmanTextFieldCellRenderer());

        // Set custom renderer for delete column
        setColumnRenderer(COL_DELETE, new DeleteButtonRenderer());
    }

    private void setupTableListeners() {
        addTableRightMouseListener();
        addDeleteButtonListener();
    }

    /**
     * Add mouse listener for delete button clicks
     */
    private void addDeleteButtonListener() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!editable) {
                    return;
                }

                // Only react to left mouse button clicks for delete action
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                int column = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());

                if (column == COL_DELETE && row >= 0) {
                    // Convert view row to model row
                    int modelRow = row;
                    if (table.getRowSorter() != null) {
                        modelRow = table.getRowSorter().convertRowIndexToModel(row);
                    }

                    // Check if row is valid
                    if (modelRow < 0 || modelRow >= tableModel.getRowCount()) {
                        return;
                    }

                    // Prevent deleting the last row (keep at least one empty row like Postman)
                    // The last row is always the empty template row
                    int rowCount = tableModel.getRowCount();
                    if (modelRow == rowCount - 1 && rowCount == 1) {
                        // Don't delete if it's the only row
                        return;
                    }

                    // Stop cell editing before deleting
                    stopCellEditing();

                    // Delete the row
                    tableModel.removeRow(modelRow);

                    // Ensure there's always an empty row at the end
                    ensureEmptyLastRow();
                }
            }
        });
    }


    /**
     * Set empty cell white background renderer
     */
    private void setEmptyCellWhiteBackgroundRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    c.setBackground(Color.WHITE);
                }

                return c;
            }
        };

        table.setDefaultRenderer(Object.class, renderer);
    }

    /**
     * Set column editor
     */
    public void setColumnEditor(int column, TableCellEditor editor) {
        if (column >= 0 && column < table.getColumnCount()) {
            table.getColumnModel().getColumn(column).setCellEditor(editor);
        }
    }

    /**
     * Set column renderer
     */
    public void setColumnRenderer(int column, TableCellRenderer renderer) {
        if (column >= 0 && column < table.getColumnCount()) {
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }
    }

    /**
     * Add right-click menu listener
     */
    private void addTableRightMouseListener() {
        MouseAdapter tableMouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            private void showPopupMenu(MouseEvent e) {
                if (!editable) {
                    return;
                }

                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                }

                JPopupMenu contextMenu = createContextPopupMenu();
                contextMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        };

        table.addMouseListener(tableMouseListener);
    }

    /**
     * Create context menu
     */
    private JPopupMenu createContextPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem addItem = new JMenuItem("Add");
        addItem.addActionListener(e -> addRowAndScroll());
        menu.add(addItem);

        JMenuItem removeItem = new JMenuItem("Remove");
        removeItem.addActionListener(e -> deleteSelectedRow());
        menu.add(removeItem);

        return menu;
    }

    /**
     * 添加一行数据 (内部使用)
     *
     * @param values 行数据
     */
    private void addRow(Object... values) {
        if (values == null || values.length == 0) {
            tableModel.addRow(new Object[]{true, "", "", ""});
        } else if (values.length == 2) {
            // Legacy support: if 2 values provided, treat as Key, Value
            tableModel.addRow(new Object[]{true, values[0], values[1], ""});
        } else if (values.length == 3) {
            // New format: enabled, key, value
            tableModel.addRow(new Object[]{values[0], values[1], values[2], ""});
        } else {
            // Ensure we have exactly 4 columns
            Object[] row = new Object[4];
            row[0] = true; // Default enabled
            for (int i = 0; i < Math.min(values.length, 2); i++) {
                row[i + 1] = values[i];
            }
            // Fill remaining columns with empty strings
            for (int i = values.length; i < 3; i++) {
                row[i + 1] = "";
            }
            row[3] = ""; // Delete column
            tableModel.addRow(row);
        }
    }

    /**
     * Add a new row and scroll to it
     */
    public void addRowAndScroll() {
        addRow();
        scrollToLastRow();
    }

    /**
     * Delete the currently selected row
     */
    public void deleteSelectedRow() {
        // Stop cell editing before modifying table structure
        stopCellEditing();

        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            // Convert view index to model index if using row sorter
            int modelRow = selectedRow;
            if (table.getRowSorter() != null) {
                modelRow = table.getRowSorter().convertRowIndexToModel(selectedRow);
            }

            if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
                int rowCount = tableModel.getRowCount();

                // Don't delete if it's the only row (keep at least one empty row like Postman)
                if (rowCount <= 1) {
                    return;
                }

                // Delete the row
                tableModel.removeRow(modelRow);
                ensureEmptyLastRow();
            }
        }
    }


    /**
     * Get form-urlencoded data list with enabled state (new format)
     */
    public List<HttpFormUrlencoded> getFormDataList() {
        // Stop cell editing to ensure any in-progress edits are committed to the table model
        // Use parent class method with recursion protection
        stopCellEditingWithProtection();

        List<HttpFormUrlencoded> dataList = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean enabled = getBooleanValue(i, COL_ENABLED);
            String key = getStringValue(i, COL_KEY);
            String value = getStringValue(i, COL_VALUE);

            // Only add non-empty params
            if (!key.isEmpty()) {
                dataList.add(new HttpFormUrlencoded(enabled, key, value));
            }
        }
        return dataList;
    }

    /**
     * Set form-urlencoded data list with enabled state (new format)
     */
    public void setFormDataList(List<HttpFormUrlencoded> dataList) {
        // Stop cell editing before modifying table structure
        stopCellEditing();

        suppressAutoAppendRow = true;
        try {
            tableModel.setRowCount(0);
            if (dataList != null) {
                for (HttpFormUrlencoded param : dataList) {
                    tableModel.addRow(new Object[]{param.isEnabled(), param.getKey(), param.getValue(), ""});
                }
            }

            // Ensure there's always an empty row at the end
            if (tableModel.getRowCount() == 0 || hasContentInLastRow()) {
                tableModel.addRow(new Object[]{true, "", "", ""});
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }
}
