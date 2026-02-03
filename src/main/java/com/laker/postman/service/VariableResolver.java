package com.laker.postman.service;

import com.laker.postman.model.Environment;
import com.laker.postman.util.VariableUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量解析服务，负责统一处理所有类型的变量解析
 * 优先级: 临时变量 > 环境变量 > 内置函数
 */
@Slf4j
@UtilityClass
public class VariableResolver {


    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(.+?)}}");

    // 临时变量，仅本次请求有效，优先级高于环境变量
    private static final ThreadLocal<Map<String, String>> temporaryVariables =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    /**
     * 批量设置临时变量（用于同步 pm.variables）
     */
    public static void setAllTemporaryVariables(Map<String, String> variables) {
        if (variables != null) {
            temporaryVariables.get().putAll(variables);
        }
    }

    /**
     * 清空临时变量
     */
    public static void clearTemporaryVariables() {
        temporaryVariables.get().clear();
        temporaryVariables.remove();
    }

    /**
     * 替换文本中的变量占位符
     * 例如: {{baseUrl}}/api/users -> https://api.example.com/api/users
     * 优先级: 临时变量 > 环境变量 > 内置函数
     */
    public static String resolve(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Matcher matcher = VAR_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = resolveVariable(varName);

            // 如果变量不存在，保留原样
            if (value == null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                // 使用 Matcher.quoteReplacement 来避免 $ 和 \ 被当作特殊字符处理
                matcher.appendReplacement(result, Matcher.quoteReplacement(value));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 解析单个变量
     * 优先级: 临时变量 > 环境变量 > 内置函数
     */
    public static String resolveVariable(String varName) {
        if (varName == null || varName.isEmpty()) {
            return null;
        }

        // 1. 优先查临时变量
        String value = temporaryVariables.get().get(varName);
        if (value != null) {
            return value;
        }

        // 2. 查环境变量
        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        if (activeEnv != null) {
            value = activeEnv.getVariable(varName);
            if (value != null) {
                return value;
            }
        }

        // 3. 检查是否是内置函数
        if (VariableUtil.isBuiltInFunction(varName)) {
            return VariableUtil.generateBuiltInFunctionValue(varName);
        }

        return null;
    }

    /**
     * 检查变量是否已定义（包括临时变量、环境变量、内置函数）
     */
    public static boolean isVariableDefined(String varName) {
        if (varName == null || varName.isEmpty()) {
            return false;
        }

        // 检查临时变量
        if (temporaryVariables.get().containsKey(varName)) {
            return true;
        }

        // 检查环境变量
        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        if (activeEnv != null && activeEnv.getVariable(varName) != null) {
            return true;
        }

        // 检查内置函数
        return VariableUtil.isBuiltInFunction(varName);
    }

    /**
     * 获取所有可用的变量（包括临时变量、环境变量和内置函数）
     * 返回 Map<变量名, 变量值或描述>
     */
    public static Map<String, String> getAllAvailableVariables() {
        Map<String, String> allVars = new LinkedHashMap<>();

        // 1. 添加当前激活环境的变量
        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        if (activeEnv != null && activeEnv.getVariables() != null) {
            for (Map.Entry<String, String> entry : activeEnv.getVariables().entrySet()) {
                String value = entry.getValue();
                // 如果值太长，截断显示
                if (value != null && value.length() > 50) {
                    value = value.substring(0, 47) + "...";
                }
                allVars.put(entry.getKey(), value);
            }
        }

        // 2. 添加临时变量（会覆盖同名的环境变量）
        for (Map.Entry<String, String> entry : temporaryVariables.get().entrySet()) {
            String value = entry.getValue();
            if (value != null && value.length() > 50) {
                value = value.substring(0, 47) + "...";
            }
            allVars.put(entry.getKey(), value + " (temp)");
        }

        // 3. 添加内置函数
        allVars.putAll(VariableUtil.getAllBuiltInFunctions());

        return allVars;
    }

    /**
     * 根据前缀过滤变量列表
     */
    public static Map<String, String> filterVariables(String prefix) {
        Map<String, String> allVars = getAllAvailableVariables();
        if (prefix == null || prefix.isEmpty()) {
            return allVars;
        }

        Map<String, String> filtered = new LinkedHashMap<>();
        String lowerPrefix = prefix.toLowerCase();
        for (Map.Entry<String, String> entry : allVars.entrySet()) {
            if (entry.getKey().toLowerCase().startsWith(lowerPrefix)) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }
}
