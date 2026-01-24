package com.laker.postman.service.swagger;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.experimental.UtilityClass;

/**
 * Swagger/OpenAPI 共享工具类
 * 提供各版本解析器通用的工具方法
 */
@UtilityClass
class SwaggerCommonUtil {

    /**
     * 判断是否不是 HTTP 方法
     */
    static boolean isNotHttpMethod(String method) {
        return !"get".equalsIgnoreCase(method) &&
                !"post".equalsIgnoreCase(method) &&
                !"put".equalsIgnoreCase(method) &&
                !"delete".equalsIgnoreCase(method) &&
                !"patch".equalsIgnoreCase(method) &&
                !"head".equalsIgnoreCase(method) &&
                !"options".equalsIgnoreCase(method);
    }

    /**
     * 从 schema 生成示例 JSON
     */
    static String generateExampleFromSchema(JSONObject schema) {
        if (schema == null) {
            return "{}";
        }

        // 优先使用 example/examples
        Object example = getExample(schema);
        if (example != null) {
            if (example instanceof JSONObject || example instanceof JSONArray) {
                return example.toString();
            }
            return JSONUtil.toJsonStr(example);
        }

        String type = getSchemaType(schema);

        switch (type) {
            case "object":
                JSONObject properties = schema.getJSONObject("properties");
                if (properties == null) {
                    return "{}";
                }
                JSONObject result = new JSONObject();
                for (String key : properties.keySet()) {
                    JSONObject propSchema = properties.getJSONObject(key);
                    result.set(key, generateExampleValue(propSchema));
                }
                return result.toString();

            case "array":
                JSONObject items = schema.getJSONObject("items");
                JSONArray array = new JSONArray();
                if (items != null) {
                    array.add(generateExampleValue(items));
                }
                return array.toString();

            default:
                return JSONUtil.toJsonStr(generateExampleValue(schema));
        }
    }

    /**
     * 生成示例值
     */
    static Object generateExampleValue(JSONObject schema) {
        if (schema == null) {
            return "";
        }

        // 优先使用 example/examples
        Object example = getExample(schema);
        if (example != null) {
            return example;
        }

        String type = getSchemaType(schema);

        switch (type) {
            case "string":
                String format = schema.getStr("format", "");
                if ("date".equals(format)) {
                    return "2024-01-01";
                } else if ("date-time".equals(format)) {
                    return "2024-01-01T00:00:00Z";
                } else if ("email".equals(format)) {
                    return "user@example.com";
                }
                return "string";

            case "integer":
                return 0;

            case "number":
                return 0.0;

            case "boolean":
                return false;

            case "array":
                return new JSONArray();

            case "object":
                return new JSONObject();

            default:
                return "";
        }
    }

    /**
     * 获取 schema 类型（支持 OpenAPI 3.1 的 type 数组）
     */
    static String getSchemaType(JSONObject schema) {
        if (schema == null) {
            return "string";
        }

        Object typeObj = schema.get("type");
        if (typeObj instanceof String) {
            return (String) typeObj;
        } else if (typeObj instanceof JSONArray) {
            // OpenAPI 3.1: type: ["string", "null"]
            JSONArray types = (JSONArray) typeObj;
            for (Object t : types) {
                if (!"null".equals(t)) {
                    return (String) t;
                }
            }
        }
        return "string";
    }

    /**
     * 获取示例（优先 examples，再 fallback 到 example）
     * 支持 OpenAPI 3.1 的 examples 数组
     */
    static Object getExample(JSONObject schema) {
        if (schema == null) {
            return null;
        }

        // OpenAPI 3.1: examples (数组)
        if (schema.containsKey("examples")) {
            JSONArray examples = schema.getJSONArray("examples");
            if (examples != null && !examples.isEmpty()) {
                return examples.get(0);
            }
        }

        // OpenAPI 3.0 / Swagger 2.0: example (单个)
        if (schema.containsKey("example")) {
            return schema.get("example");
        }

        return null;
    }
}
