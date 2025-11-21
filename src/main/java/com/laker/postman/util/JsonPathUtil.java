package com.laker.postman.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonPathUtil {

    /**
     * 从JSON对象中提取指定路径的值，支持简单的$.a.b[0].c格式
     *
     * @param jsonObj 可以是JSONObject/JSONArray/String
     * @param path    形如$.a.b[0].c
     * @return 提取到的值，找不到返回null
     */
    public static String extractJsonPath(Object jsonObj, String path) {
        if (jsonObj == null || path == null || path.isEmpty()) return null;
        if (jsonObj instanceof String str) {
            if (!JSONUtil.isTypeJSON(str)) return null;
            jsonObj = JSONUtil.parse(str);
        }
        if (path.startsWith("$.")) path = path.substring(2);
        String[] segments = path.split("\\.");
        Object current = jsonObj;
        for (String segment : segments) {
            if (current == null) return null;
            if (segment.contains("[") && segment.contains("]")) {
                String arrayName = segment.substring(0, segment.indexOf("["));
                String indexStr = segment.substring(segment.indexOf("[") + 1, segment.indexOf("]"));
                int index;
                try {
                    index = Integer.parseInt(indexStr);
                } catch (Exception e) {
                    return null;
                }
                if (current instanceof JSONObject obj) {
                    Object arr = obj.get(arrayName);
                    if (arr instanceof JSONArray jsonArr) {
                        if (index >= 0 && index < jsonArr.size()) {
                            current = jsonArr.get(index);
                        } else return null;
                    } else return null;
                } else return null;
            } else {
                if (current instanceof JSONObject obj) {
                    current = obj.get(segment);
                } else return null;
            }
        }
        return current != null ? current.toString() : null;
    }
}