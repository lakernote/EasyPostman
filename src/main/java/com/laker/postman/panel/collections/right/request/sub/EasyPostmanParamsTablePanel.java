package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.component.table.AbstractEasyPostmanTablePanel;
import com.laker.postman.common.component.table.EasyPostmanTextFieldCellEditor;
import com.laker.postman.common.component.table.EasyPostmanTextFieldCellRenderer;
import com.laker.postman.model.HttpParam;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Params table panel with checkbox and delete button columns
 * Similar to EasyHttpHeadersTablePanel but for request parameters
 */
@Slf4j
public class EasyPostmanParamsTablePanel extends AbstractEasyPostmanTablePanel<HttpParam> {

    // Column indices
    private static final int COL_ENABLED = 0;
    private static final int COL_KEY = 1;
    private static final int COL_VALUE = 2;
    private static final int COL_DELETE = 3;

    public EasyPostmanParamsTablePanel() {
        super(new String[]{"", "Key", "Value", ""});
        initializeComponents();
        initializeTableUI();
        setupCellRenderersAndEditors();
        setupTableListeners();
        addAutoAppendRowFeature();

        // Add initial empty row
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
        // Enable和Delete列不可编辑，Key和Value列可编辑
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

    /**
     * Get params list with enabled state (new format)
     */
    public List<HttpParam> getParamsList() {
        // Stop cell editing to ensure any in-progress edits are committed
        // Use parent class method with recursion protection
        stopCellEditingWithProtection();

        List<HttpParam> paramsList = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean enabled = getBooleanValue(i, COL_ENABLED);
            String key = getStringValue(i, COL_KEY);
            String value = getStringValue(i, COL_VALUE);

            // Only add non-empty params
            if (!key.isEmpty()) {
                paramsList.add(new HttpParam(enabled, key, value));
            }
        }
        return paramsList;
    }

    /**
     * Set params list with enabled state (new format)
     */
    public void setParamsList(List<HttpParam> paramsList) {
        // Stop cell editing before modifying table structure
        stopCellEditing();

        suppressAutoAppendRow = true;
        try {
            tableModel.setRowCount(0);
            if (paramsList != null) {
                for (HttpParam param : paramsList) {
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
