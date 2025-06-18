package com.laker.postman.common.table.map;

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

    public EasyNameValueTablePanel(String nameCol, String valueCol) {
        super(new String[]{nameCol, valueCol});
        this.nameCol = nameCol;
        this.valueCol = valueCol;
    }

    public EasyNameValueTablePanel() {
        this("Name", "Value");
    }

    /**
     * 获取表格内容为Map（第一列为key，第二列为value）
     */
    public Map<String, String> getMap() {

        Map<String, String> map = new LinkedHashMap<>();
        List<Map<String, Object>> rows = getRows();
        for (Map<String, Object> row : rows) {
            Object key = row.get(nameCol); // 获取第一列作为key
            Object value = row.get(valueCol); // 获取第二列作为value
            if (key != null && !key.toString().trim().isEmpty()) { // 确保key不为空
                map.put(key.toString(), value == null ? null : value.toString()); // 将value转换为String，null值也处理
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