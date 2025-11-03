package com.laker.postman.service.postman;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.model.Environment;

import java.util.*;

/**
 * Postman环境变量解析器
 * 负责解析和导出Postman环境变量（v2.1格式）
 */
public class PostmanEnvironmentParser {

    /**
     * 解析Postman环境变量JSON，转换为Environment列表
     */
    public static List<Environment> parsePostmanEnvironments(String json) {
        List<Environment> result = new ArrayList<>();
        // 兼容单个对象或数组
        if (JSONUtil.isTypeJSONArray(json)) {
            JSONArray arr = JSONUtil.parseArray(json);
            for (Object obj : arr) {
                parseSingleEnvObj((JSONObject) obj, result);
            }
        } else {
            JSONObject envObj = JSONUtil.parseObj(json);
            parseSingleEnvObj(envObj, result);
        }
        return result;
    }

    /**
     * 解析单个环境变量对象
     */
    private static void parseSingleEnvObj(JSONObject envObj, List<Environment> result) {
        String name = envObj.getStr("name", "未命名环境");
        Environment env = new Environment(name);
        env.setId(UUID.randomUUID().toString());
        JSONArray values = envObj.getJSONArray("values");
        if (values != null) {
            for (Object v : values) {
                JSONObject vObj = (JSONObject) v;
                String key = vObj.getStr("key", "");
                String value = vObj.getStr("value", "");
                boolean enabled = vObj.getBool("enabled", true);
                if (!key.isEmpty() && enabled) {
                    env.addVariable(key, value);
                }
            }
        }
        result.add(env);
    }

    /**
     * 导出单个环境为Postman环境变量JSON字符串
     */
    public static String toPostmanEnvironmentJson(Environment env) {
        JSONObject obj = new JSONObject();
        obj.put("id", UUID.randomUUID().toString());
        obj.put("name", env.getName());
        JSONArray values = new JSONArray();
        if (env.getVariables() != null) {
            for (Map.Entry<String, String> entry : env.getVariables().entrySet()) {
                JSONObject v = new JSONObject();
                v.put("key", entry.getKey());
                v.put("value", entry.getValue());
                v.put("enabled", true);
                values.add(v);
            }
        }
        obj.put("values", values);
        obj.put("_postman_variable_scope", "environment");
        obj.put("_postman_exported_at", java.time.ZonedDateTime.now().toString());
        obj.put("_postman_exported_using", "EasyPostman");
        return obj.toStringPretty();
    }
}

