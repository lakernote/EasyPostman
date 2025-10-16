package com.laker.postman.common.component.table.map;

import com.laker.postman.common.component.table.EasyPostmanTextFieldCellEditor;
import com.laker.postman.common.component.table.EasyPostmanTextFieldCellRenderer;

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
}