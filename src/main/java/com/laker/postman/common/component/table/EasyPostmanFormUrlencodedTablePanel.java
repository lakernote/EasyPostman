package com.laker.postman.common.component.table;

import lombok.Getter;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Form-Urlencoded 表格面板组件
 * 专门用于处理 application/x-www-form-urlencoded 类型的请求体数据
 * 支持 Key、Value 两列结构
 */
public class EasyPostmanFormUrlencodedTablePanel extends JPanel {

    @Getter
    private final EasyTablePanel tablePanel;

    private static final String[] COLUMNS = {"Key", "Value"};

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
        setLayout(new BorderLayout());

        // 创建表格面板
        tablePanel = new EasyTablePanel(COLUMNS, 24, popupMenuEnabled, autoAppendRowEnabled);

        // 设置 Key 和 Value 列的编辑器和渲染器
        tablePanel.setColumnEditor(0, new EasyPostmanTextFieldCellEditor());
        tablePanel.setColumnEditor(1, new EasyPostmanTextFieldCellEditor());
        tablePanel.setColumnRenderer(0, new EasyPostmanTextFieldCellRenderer());
        tablePanel.setColumnRenderer(1, new EasyPostmanTextFieldCellRenderer());

        add(tablePanel, BorderLayout.CENTER);
    }

    /**
     * 添加表格模型监听器
     *
     * @param listener 表格模型监听器
     */
    public void addTableModelListener(TableModelListener listener) {
        if (tablePanel != null) {
            tablePanel.addTableModelListener(listener);
        }
    }

    /**
     * 清空表格数据
     */
    public void clear() {
        if (tablePanel != null) {
            tablePanel.clear();
        }
    }

    /**
     * 添加一行数据
     *
     * @param values 行数据
     */
    public void addRow(Object... values) {
        if (tablePanel != null) {
            tablePanel.addRow(values);
        }
    }

    /**
     * 获取所有行数据
     *
     * @return 所有行数据的列表
     */
    public List<Map<String, Object>> getRows() {
        return tablePanel != null ? tablePanel.getRows() : List.of();
    }

    /**
     * 获取 Form-Urlencoded 数据
     *
     * @return Key-Value 形式的表单数据
     */
    public Map<String, String> getFormData() {
        Map<String, String> formData = new LinkedHashMap<>();
        if (tablePanel != null) {
            List<Map<String, Object>> rows = tablePanel.getRows();
            for (Map<String, Object> row : rows) {
                String key = row.get("Key") == null ? null : row.get("Key").toString();
                String value = row.get("Value") == null ? null : row.get("Value").toString();
                if (key != null && !key.trim().isEmpty()) {
                    formData.put(key.trim(), value == null ? "" : value);
                }
            }
        }
        return formData;
    }

    /**
     * 设置 Form-Urlencoded 数据
     *
     * @param formData Key-Value 形式的表单数据
     */
    public void setFormData(Map<String, String> formData) {
        clear();
        if (formData != null && !formData.isEmpty()) {
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                addRow(entry.getKey(), entry.getValue());
            }
        }
    }
}

