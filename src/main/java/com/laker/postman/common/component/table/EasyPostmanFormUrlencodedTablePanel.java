package com.laker.postman.common.component.table;

import com.laker.postman.model.HttpFormUrlencoded;
import lombok.extern.slf4j.Slf4j;

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
    @Override
    protected void initializeTableUI() {
        // 调用父类的通用UI配置
        super.initializeTableUI();

        // 设置列宽
        setEnabledColumnWidth(40);
        setDeleteColumnWidth();

        // Setup Tab key navigation to move between columns in the same row
        setupTabKeyNavigation();
    }

    private void setupCellRenderersAndEditors() {
        // Set editors for Key and Value columns
        setColumnEditor(COL_KEY, new EasyPostmanTextFieldCellEditor());
        setColumnEditor(COL_VALUE, new EasySmartValueCellEditor());
        setColumnRenderer(COL_KEY, new EasyTextFieldCellRenderer());
        setColumnRenderer(COL_VALUE, new EasyTextFieldCellRenderer());

        // Set custom renderer for delete column
        setColumnRenderer(COL_DELETE, new DeleteButtonRenderer());
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
