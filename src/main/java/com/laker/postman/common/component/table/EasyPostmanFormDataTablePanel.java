package com.laker.postman.common.component.table;

import lombok.Getter;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.laker.postman.common.component.table.TableUIConstants.SELECT_FILE_TEXT;

/**
 * Form-Data 表格面板组件
 * 专门用于处理 form-data 类型的请求体数据
 * 支持 Key、Type(Text/File)、Value 三列结构
 */
public class EasyPostmanFormDataTablePanel extends JPanel {

    @Getter
    private final EasyTablePanel tablePanel;

    private static final String[] COLUMNS = {"Key", "Type", "Value"};
    private static final String[] TYPE_OPTIONS = {"Text", "File"};
    private static final String TYPE_TEXT = "Text";
    private static final String TYPE_FILE = "File";

    /**
     * 构造函数，创建默认的 Form-Data 表格面板
     */
    public EasyPostmanFormDataTablePanel() {
        this(true, true);
    }

    /**
     * 构造函数，支持自定义配置
     *
     * @param popupMenuEnabled     是否启用右键菜单
     * @param autoAppendRowEnabled 是否启用自动补空行
     */
    public EasyPostmanFormDataTablePanel(boolean popupMenuEnabled, boolean autoAppendRowEnabled) {
        setLayout(new BorderLayout());

        // 创建表格面板
        tablePanel = new EasyTablePanel(COLUMNS, 24, popupMenuEnabled, autoAppendRowEnabled);

        // 设置 Type 列为下拉框编辑器
        JComboBox<String> typeCombo = new JComboBox<>(TYPE_OPTIONS);
        tablePanel.setColumnEditor(1, new DefaultCellEditor(typeCombo));

        // 设置 Value 列根据 Type 动态切换编辑器和渲染器
        tablePanel.setColumnEditor(2, new TextOrFileTableCellEditor());
        tablePanel.setColumnRenderer(2, new TextOrFileTableCellRenderer());

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
     * 获取 Form-Data 中的文本数据
     *
     * @return Key-Value 形式的文本数据
     */
    public Map<String, String> getFormData() {
        Map<String, String> formData = new LinkedHashMap<>();
        if (tablePanel != null) {
            List<Map<String, Object>> rows = tablePanel.getRows();
            for (Map<String, Object> row : rows) {
                String key = row.get("Key") == null ? null : row.get("Key").toString();
                String type = row.get("Type") == null ? null : row.get("Type").toString();
                String value = row.get("Value") == null ? null : row.get("Value").toString();
                if (key != null && !key.trim().isEmpty() && TYPE_TEXT.equals(type)) {
                    formData.put(key.trim(), value);
                }
            }
        }
        return formData;
    }

    /**
     * 获取 Form-Data 中的文件数据
     *
     * @return Key-FilePath 形式的文件数据
     */
    public Map<String, String> getFormFiles() {
        Map<String, String> formFiles = new LinkedHashMap<>();
        if (tablePanel != null) {
            List<Map<String, Object>> rows = tablePanel.getRows();
            for (Map<String, Object> row : rows) {
                String key = row.get("Key") == null ? null : row.get("Key").toString();
                String type = row.get("Type") == null ? null : row.get("Type").toString();
                String value = row.get("Value") == null ? null : row.get("Value").toString();
                if (key != null && !key.trim().isEmpty() && TYPE_FILE.equals(type) && value != null && !value.equals(SELECT_FILE_TEXT)) {
                    formFiles.put(key.trim(), value);
                }
            }
        }
        return formFiles;
    }
}
