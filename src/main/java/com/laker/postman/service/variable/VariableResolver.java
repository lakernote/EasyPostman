package com.laker.postman.service.variable;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量解析服务
 * <p>
 * 解析字符串中的 {{variableName}} 占位符，支持嵌套解析
 * <p>
 * 变量优先级：临时变量 > 分组变量 > 环境变量 > 内置函数
 */
@Slf4j
@UtilityClass
public class VariableResolver {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(.+?)}}");

    /**
     * 变量提供者列表（按优先级排序）
     */
    private static final List<VariableProvider> PROVIDERS;

    static {
        List<VariableProvider> providers = new ArrayList<>(Arrays.asList(
                TemporaryVariableService.getInstance(),
                GroupVariableService.getInstance(),
                EnvironmentVariableService.getInstance(),
                BuiltInFunctionService.getInstance()
        ));
        providers.sort(Comparator.comparingInt(VariableProvider::getPriority));
        PROVIDERS = Collections.unmodifiableList(providers);
    }

    /**
     * 批量设置临时变量
     */
    public static void setAllTemporaryVariables(Map<String, String> variables) {
        TemporaryVariableService.getInstance().setAll(variables);
    }

    /**
     * 清空临时变量
     */
    public static void clearTemporaryVariables() {
        TemporaryVariableService.getInstance().clear();
    }

    /**
     * 替换文本中的变量占位符（支持嵌套解析）
     * <p>
     * 示例: {{baseUrl}}/api/users -> http://api.example.com/api/users
     */
    public static String resolve(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 多轮解析支持嵌套变量，最多 10 轮防止循环引用
        String result = text;
        int maxIterations = 10;
        int iteration = 0;

        while (iteration < maxIterations) {
            String beforeResolve = result;
            result = resolveOnce(result);

            if (result.equals(beforeResolve)) {
                break; // 无变化，已完成解析
            }

            iteration++;
        }

        if (iteration >= maxIterations) {
            log.warn("变量解析达到最大迭代次数({}), 可能存在循环引用: {}", maxIterations, text);
        }

        return result;
    }

    /**
     * 执行一轮变量解析
     */
    private static String resolveOnce(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Matcher matcher = VAR_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = resolveVariable(varName);

            if (value == null) {
                // 变量不存在，保留原样
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 解析单个变量（按优先级查找）
     */
    public static String resolveVariable(String varName) {
        if (varName == null || varName.isEmpty()) {
            return null;
        }

        for (VariableProvider provider : PROVIDERS) {
            if (provider.has(varName)) {
                String value = provider.get(varName);
                if (value != null) {
                    return value;
                }
            }
        }

        return null;
    }

    /**
     * 检查变量是否已定义
     */
    public static boolean isVariableDefined(String varName) {
        if (varName == null || varName.isEmpty()) {
            return false;
        }

        return PROVIDERS.stream().anyMatch(provider -> provider.has(varName));
    }

    /**
     * 获取所有可用变量
     */
    public static Map<String, String> getAllAvailableVariables() {
        Map<String, String> allVars = new LinkedHashMap<>();

        // 按优先级添加，使用 putIfAbsent 避免被低优先级覆盖
        for (VariableProvider provider : PROVIDERS) {
            Map<String, String> providerVars = provider.getAll();
            for (Map.Entry<String, String> entry : providerVars.entrySet()) {
                String value = entry.getValue();
                if (value != null && value.length() > 50) {
                    value = value.substring(0, 47) + "...";
                }
                allVars.putIfAbsent(entry.getKey(), value);
            }
        }

        // 为临时变量添加标记
        Map<String, String> tempVars = TemporaryVariableService.getInstance().getAll();
        for (String key : tempVars.keySet()) {
            if (allVars.containsKey(key)) {
                String value = allVars.get(key);
                if (value != null && value.length() > 50) {
                    value = value.substring(0, 47) + "...";
                }
                allVars.put(key, value + " (temp)");
            }
        }

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
