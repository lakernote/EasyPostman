package com.laker.postman.service.ideahttp;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.model.Environment;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * IntelliJ IDEA HTTP Client 环境变量解析器
 * 负责解析 IntelliJ IDEA HTTP Client 的 http-client.env.json 格式
 */
@UtilityClass
public class IntelliJHttpEnvParser {

    /**
     * 解析 IntelliJ IDEA HTTP Client 环境变量JSON，转换为Environment列表
     * <p>
     * 支持格式示例：
     * {
     * "dev": {
     * "host": "localhost",
     * "port": "8080"
     * },
     * "prod": {
     * "host": "example.com",
     * "port": "443"
     * }
     * }
     *
     * @param json IntelliJ IDEA HTTP Client 环境变量JSON字符串
     * @return Environment列表
     */
    public static List<Environment> parseIntelliJEnvironments(String json) {
        List<Environment> result = new ArrayList<>();

        try {
            // 预处理JSON: 移除可能的语法错误（如行尾的分号）
            String cleanedJson = preprocessJson(json);

            JSONObject rootObj = JSONUtil.parseObj(cleanedJson);

            // 遍历每个环境（顶层的每个key都是一个环境）
            for (Map.Entry<String, Object> entry : rootObj.entrySet()) {
                String envName = entry.getKey();
                Object envValue = entry.getValue();

                // 跳过非对象类型的值
                if (!(envValue instanceof JSONObject envVariables)) {
                    continue;
                }

                Environment env = new Environment(envName);
                env.setId("env-" + UUID.randomUUID());

                // 将该环境下的所有键值对添加为环境变量
                for (Map.Entry<String, Object> varEntry : envVariables.entrySet()) {
                    String key = varEntry.getKey();
                    Object value = varEntry.getValue();

                    // 将值转换为字符串
                    String valueStr = value != null ? value.toString() : "";
                    env.addVariable(key, valueStr);
                }

                result.add(env);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse IntelliJ IDEA HTTP environment file: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 预处理JSON字符串，修复常见的语法错误
     * 例如：将字符串值后面的分号替换为逗号 "value"; -> "value",
     */
    private static String preprocessJson(String json) {
        // 将字符串后紧跟的分号替换为逗号（这是IntelliJ IDEA HTTP文件可能出现的错误）
        // 匹配模式: "任意内容";  替换为: "任意内容",
        return json.replaceAll("\";", "\",");
    }

    /**
     * 导出单个环境为 IntelliJ IDEA HTTP Client 环境变量格式
     *
     * @param env Environment对象
     * @return IntelliJ IDEA HTTP Client 格式的JSON字符串
     */
    public static String toIntelliJEnvironmentJson(Environment env) {
        JSONObject rootObj = new JSONObject();
        JSONObject envObj = new JSONObject();

        if (env.getVariables() != null) {
            envObj.putAll(env.getVariables());
        }

        rootObj.set(env.getName(), envObj);
        return rootObj.toStringPretty();
    }

    /**
     * 导出多个环境为 IntelliJ IDEA HTTP Client 环境变量格式
     *
     * @param environments Environment列表
     * @return IntelliJ IDEA HTTP Client 格式的JSON字符串
     */
    public static String toIntelliJEnvironmentsJson(List<Environment> environments) {
        JSONObject rootObj = new JSONObject();

        for (Environment env : environments) {
            JSONObject envObj = new JSONObject();
            if (env.getVariables() != null) {
                envObj.putAll(env.getVariables());
            }
            rootObj.set(env.getName(), envObj);
        }

        return rootObj.toStringPretty();
    }
}
