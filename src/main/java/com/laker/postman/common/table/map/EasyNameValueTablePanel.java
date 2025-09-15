package com.laker.postman.common.table.map;

import com.laker.postman.common.component.EasyPostmanTextField;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 专用于Name-Value/Key-Value两列表格的面板，简化Map操作。
 */
public class EasyNameValueTablePanel extends EasyTablePanel {
    private final String nameCol;
    private final String valueCol;


    public EasyNameValueTablePanel() {
        this("Name", "Value");
    }


    public EasyNameValueTablePanel(String nameCol, String valueCol) {
        super(new String[]{nameCol, valueCol});
        this.nameCol = nameCol;
        this.valueCol = valueCol;

        setColumnEditor(0, new EasyPostmanTextFieldCellEditor());
        setColumnEditor(1, new EasyPostmanTextFieldCellEditor());
        setColumnRenderer(0, new EasyPostmanTextFieldCellRenderer());
        setColumnRenderer(1, new EasyPostmanTextFieldCellRenderer());
    }


    /**
     * 获取表格内容为Map（第一列为key，第二列为value）
     */
    public Map<String, String> getMap() {
        Map<String, String> map = new LinkedHashMap<>();
        List<Map<String, Object>> rows = getRows();
        for (Map<String, Object> row : rows) {
            Object keyObj = row.get(nameCol);
            Object valueObj = row.get(valueCol);
            String key = keyObj == null ? "" : keyObj.toString().trim();
            String value = valueObj == null ? "" : valueObj.toString().trim();
            if (!key.isEmpty()) { // 确保key不为空
                map.put(key, value);
            }
        }

        return map;
    }

    /**
     * 用Map数据填充表格
     */
    public void setMap(Map<String, String> map) {
        clear();
        if (map != null) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (var e : map.entrySet()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put(nameCol, e.getKey());
                row.put(valueCol, e.getValue());
                rows.add(row);
            }
            super.setRows(rows);
        }
    }

    // ========== EasyPostmanTextField cell renderer/editor ==========
    private static class EasyPostmanTextFieldCellRenderer extends EasyPostmanTextField implements TableCellRenderer {
        public EasyPostmanTextFieldCellRenderer() {
            super(1);
            setBorder(null);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }
            return this;
        }
    }

    private static class EasyPostmanTextFieldCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final EasyPostmanTextField textField = new EasyPostmanTextField(1);

        public EasyPostmanTextFieldCellEditor() {
            textField.setBorder(null);
        }

        @Override
        public Object getCellEditorValue() {
            return textField.getText();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            textField.setText(value == null ? "" : value.toString());
            return textField;
        }
    }
}