package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.component.table.AbstractEasyPostmanTablePanel;
import com.laker.postman.common.component.table.EasyPostmanTextFieldCellEditor;
import com.laker.postman.common.component.table.EasyTextFieldCellRenderer;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.*;

@Slf4j
public class EasyHttpHeadersTablePanel extends AbstractEasyPostmanTablePanel<Map<String, Object>> {

    // Default header keys for consistency
    private static final String USER_AGENT = "User-Agent";
    private static final String ACCEPT = "Accept";
    private static final String ACCEPT_ENCODING = "Accept-Encoding";
    private static final String CONNECTION = "Connection";
    private static final Set<String> DEFAULT_HEADER_KEYS = new HashSet<>();

    static {
        DEFAULT_HEADER_KEYS.add(USER_AGENT);
        DEFAULT_HEADER_KEYS.add(ACCEPT);
        DEFAULT_HEADER_KEYS.add(ACCEPT_ENCODING);
        DEFAULT_HEADER_KEYS.add(CONNECTION);
    }

    // Column indices
    private static final int COL_ENABLED = 0;
    private static final int COL_KEY = 1;
    private static final int COL_VALUE = 2;
    private static final int COL_DELETE = 3;

    public EasyHttpHeadersTablePanel() {
        super(new String[]{"", "Key", "Value", ""});
        initializeComponents();
        initializeTableUI();
        setupCellRenderersAndEditors();
        setupTableListeners();
        addAutoAppendRowFeature();
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

    @Override
    protected boolean isCellEditable(int row, int column) {
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

        // Check if this is a default header
        Object keyObj = tableModel.getValueAt(row, COL_KEY);
        if (keyObj != null) {
            String key = keyObj.toString().trim();
            if (DEFAULT_HEADER_KEYS.contains(key)) {
                // For default headers: Key column not editable, Value column editable
                return column == COL_VALUE;
            }
        }

        // For non-default headers: Key and Value columns editable
        return column == COL_KEY || column == COL_VALUE;
    }

    @Override
    protected boolean canDeleteRow(int modelRow) {
        // Check if it's a default header
        Object keyObj = tableModel.getValueAt(modelRow, COL_KEY);
        String keyStr = keyObj == null ? "" : keyObj.toString().trim();
        return !DEFAULT_HEADER_KEYS.contains(keyStr);
    }

    @Override
    protected JPopupMenu createContextPopupMenu(int viewRow) {
        JPopupMenu menu = new JPopupMenu();

        // Convert view row to model row if needed
        int modelRow = viewRow;
        if (viewRow >= 0 && table.getRowSorter() != null) {
            modelRow = table.getRowSorter().convertRowIndexToModel(viewRow);
        }

        // Check if the selected row contains a default header
        boolean isDefaultHeaderRow = false;
        if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
            Object keyObj = tableModel.getValueAt(modelRow, COL_KEY);
            if (keyObj != null) {
                String key = keyObj.toString().trim();
                isDefaultHeaderRow = DEFAULT_HEADER_KEYS.contains(key);
            }
        }

        if (isDefaultHeaderRow) {
            // For default header rows, show a disabled menu item
            JMenuItem defaultHeaderItem = new JMenuItem("Default Header (Cannot Delete)");
            defaultHeaderItem.setEnabled(false);
            menu.add(defaultHeaderItem);
        } else {
            // For normal rows, show add/remove options
            JMenuItem addItem = new JMenuItem("Add");
            addItem.addActionListener(e -> addRowAndScroll());
            menu.add(addItem);

            JMenuItem removeItem = new JMenuItem("Remove");
            removeItem.addActionListener(e -> deleteSelectedRow());
            menu.add(removeItem);
        }

        return menu;
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
        setColumnRenderer(COL_KEY, new EasyTextFieldCellRenderer());
        setColumnRenderer(COL_VALUE, new EasyTextFieldCellRenderer());

        // Set custom renderer for delete column
        setColumnRenderer(COL_DELETE, new DeleteButtonRenderer());
    }

    /**
     * Get all rows as a list of maps (model data, not view data)
     */
    public List<Map<String, Object>> getRows() {
        // Stop cell editing to ensure any in-progress edits are committed
        // Use parent class method with recursion protection
        stopCellEditingWithProtection();

        List<Map<String, Object>> rows = new ArrayList<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            // Store enabled state
            row.put("Enabled", getBooleanValue(i, COL_ENABLED));
            // Store Key and Value
            row.put("Key", getStringValue(i, COL_KEY));
            row.put("Value", getStringValue(i, COL_VALUE));
            rows.add(row);
        }

        return rows;
    }

    /**
     * Set all rows from a list of maps
     */
    public void setRows(List<Map<String, Object>> rows) {
        suppressAutoAppendRow = true;
        try {
            // Clear existing data
            tableModel.setRowCount(0);

            // Add new rows
            if (rows != null && !rows.isEmpty()) {
                for (Map<String, Object> row : rows) {
                    Object enabled = row.get("Enabled");
                    if (enabled == null) {
                        enabled = true; // Default to enabled
                    }
                    Object key = row.get("Key");
                    Object value = row.get("Value");

                    tableModel.addRow(new Object[]{enabled, key, value, ""});
                }
            }

            // Ensure there's always an empty row at the end
            ensureEmptyLastRow();
        } finally {
            suppressAutoAppendRow = false;
        }
    }
}
