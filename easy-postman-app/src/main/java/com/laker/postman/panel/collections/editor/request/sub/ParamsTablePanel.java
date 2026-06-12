package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.request.model.HttpParam;


import com.laker.postman.common.component.table.AbstractTablePanel;
import com.laker.postman.common.component.table.EasyPostmanTextFieldCellEditor;
import com.laker.postman.common.component.table.EasySmartValueCellEditor;
import com.laker.postman.common.component.table.EasyTextFieldCellRenderer;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/**
 * Params table panel with checkbox and delete button columns
 * Similar to the request headers table panel but for request parameters
 */
@Slf4j
public class ParamsTablePanel extends AbstractTablePanel<HttpParam> {

    // Column indices
    private static final int COL_ENABLED = 0;
    private static final int COL_KEY = 1;
    private static final int COL_VALUE = 2;
    private static final int COL_DESCRIPTION = 3;
    private static final int COL_DELETE = 4;

    private final boolean generatedFromUrl;

    public ParamsTablePanel() {
        this(false);
    }

    private ParamsTablePanel(boolean generatedFromUrl) {
        super(new String[]{
                "",
                I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_KEY),
                I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_VALUE),
                I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_DESCRIPTION),
                ""
        });
        this.generatedFromUrl = generatedFromUrl;
        initializeComponents();
        initializeTableUI();
        setupCellRenderersAndEditors();
        setupTableListeners();
        if (!generatedFromUrl) {
            addAutoAppendRowFeature();

            // Add initial empty row
            addRow();
        }
    }

    static ParamsTablePanel pathVariablesPanel() {
        return new ParamsTablePanel(true);
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
        return generatedFromUrl ? COL_VALUE : COL_KEY;
    }

    @Override
    protected int getLastEditableColumnIndex() {
        return COL_DESCRIPTION;
    }

    @Override
    protected int getEnterTargetColumnIndex() {
        return generatedFromUrl ? COL_DESCRIPTION : COL_VALUE;
    }

    @Override
    protected boolean isCellEditableForNavigation(int row, int column) {
        if (generatedFromUrl) {
            return column == COL_VALUE || column == COL_DESCRIPTION;
        }
        return column == COL_KEY || column == COL_VALUE || column == COL_DESCRIPTION;
    }

    @Override
    protected boolean isCellEditable(int row, int column) {
        if (generatedFromUrl && (column == COL_ENABLED || column == COL_KEY || column == COL_DELETE)) {
            return false;
        }
        return super.isCellEditable(row, column);
    }

    @Override
    protected Class<?> getColumnClass(int columnIndex) {
        if (generatedFromUrl && columnIndex == COL_ENABLED) {
            return Object.class;
        }
        return super.getColumnClass(columnIndex);
    }

    @Override
    protected boolean hasContentInRow(int row) {
        String key = getStringValue(row, COL_KEY);
        String value = getStringValue(row, COL_VALUE);
        String description = getStringValue(row, COL_DESCRIPTION);
        return !key.isEmpty() || !value.isEmpty() || !description.isEmpty();
    }

    // ========== 初始化方法 ==========
    @Override
    protected void initializeTableUI() {
        // 调用父类的通用UI配置
        super.initializeTableUI();

        // 设置列宽
        if (generatedFromUrl) {
            setGeneratedFromUrlColumnWidths();
        } else {
            setEnabledColumnWidth(40);
            setDeleteColumnWidth();
        }

        // Setup Tab key navigation to move between columns in the same row
        setupTabKeyNavigation();
    }

    @Override
    protected boolean useTableScrollPane() {
        return false;
    }

    private void setupCellRenderersAndEditors() {
        // Set editors for Key and Value columns
        setColumnEditor(COL_KEY, new EasyPostmanTextFieldCellEditor());
        setColumnEditor(COL_VALUE, new EasySmartValueCellEditor());
        setColumnEditor(COL_DESCRIPTION, new EasySmartValueCellEditor());
        setColumnRenderer(COL_KEY, new EasyTextFieldCellRenderer());
        setColumnRenderer(COL_VALUE, new EasyTextFieldCellRenderer());
        setColumnRenderer(COL_DESCRIPTION, new EasyTextFieldCellRenderer());

        // Set custom renderer for delete column
        if (!generatedFromUrl) {
            setColumnRenderer(COL_DELETE, new DeleteButtonRenderer());
        }
    }

    @Override
    protected boolean isDeletableRow(int modelRow) {
        return !generatedFromUrl;
    }

    @Override
    protected boolean isContextMenuEnabled() {
        return !generatedFromUrl;
    }

    @Override
    public void clear() {
        if (!generatedFromUrl) {
            super.clear();
            return;
        }

        stopCellEditing();
        suppressAutoAppendRow = true;
        try {
            tableModel.setRowCount(0);
        } finally {
            suppressAutoAppendRow = false;
        }
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
            String description = getStringValue(i, COL_DESCRIPTION);

            // Only add non-empty params
            if (!key.isEmpty()) {
                paramsList.add(new HttpParam(enabled, key, value, description));
            }
        }
        return paramsList;
    }

    /**
     * 从 tableModel 直接读取，不停止单元格编辑。
     * 用于 tab 指示器等后台场景，避免打断用户正在进行的输入（如 Tab 导航）。
     */
    public List<HttpParam> getParamsListFromModel() {
        List<HttpParam> paramsList = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean enabled = getBooleanValue(i, COL_ENABLED);
            String key = getStringValue(i, COL_KEY);
            String value = getStringValue(i, COL_VALUE);
            String description = getStringValue(i, COL_DESCRIPTION);
            if (!key.isEmpty()) {
                paramsList.add(new HttpParam(enabled, key, value, description));
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
                    tableModel.addRow(createRow(param));
                }
            }

            // Ensure there's always an empty row at the end
            if (!generatedFromUrl && (tableModel.getRowCount() == 0 || hasContentInLastRow())) {
                tableModel.addRow(createEmptyRow());
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    int getPreferredTableHeight() {
        int visibleRows = Math.max(1, tableModel.getRowCount());
        int headerHeight = table.getTableHeader().getPreferredSize().height;
        Insets insets = getInsets();
        return insets.top + headerHeight + visibleRows * table.getRowHeight() + insets.bottom + 4;
    }

    private Object[] createRow(HttpParam param) {
        if (generatedFromUrl) {
            return new Object[]{"", param.getKey(), param.getValue(), param.getDescription(), ""};
        }
        return new Object[]{param.isEnabled(), param.getKey(), param.getValue(), param.getDescription(), ""};
    }

    private void setGeneratedFromUrlColumnWidths() {
        setEnabledColumnWidth(40);
        setDeleteColumnWidth();
    }
}
