package com.laker.postman.service.variable;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量解析服务 - 统一的变量解析入口
 * <p>
 * 采用策略模式，通过 {@link VariableProvider} 接口统一管理多种变量源
 * <p>
 * 职责：
 * <ul>
 *   <li>解析字符串中的变量占位符 {{variableName}}</li>
 *   <li>按优先级查找变量值：临时变量 > 环境变量 > 内置函数</li>
 *   <li>提供变量查询和过滤功能</li>
 * </ul>
 * <p>
 * 变量优先级：
 * <ol>
 *   <li>临时变量 (优先级=1) - 仅在当前请求生命周期内有效</li>
 *   <li>环境变量 (优先级=2) - 从当前激活的环境中获取</li>
 *   <li>内置函数 (优先级=3) - 动态生成的值</li>
 * </ol>
 *
 * @see VariableProvider 变量提供者接口
 * @see TemporaryVariableService 临时变量提供者
 * @see EnvironmentVariableService 环境变量提供者
 * @see BuiltInFunctionService 内置函数提供者
 */
@Slf4j
@UtilityClass
public class VariableResolver {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(.+?)}}");

    /**
     * 变量提供者列表，按优先级排序（优先级数值越小越优先）
     */
    private static final List<VariableProvider> PROVIDERS;

    static {
        // 初始化提供者列表并按优先级排序
        List<VariableProvider> providers = new ArrayList<>(Arrays.asList(
                TemporaryVariableService.getInstance(),
                EnvironmentVariableService.getInstance(),
                BuiltInFunctionService.getInstance()
        ));
        // 按优先级排序：优先级数值越小越靠前
        providers.sort(Comparator.comparingInt(VariableProvider::getPriority));
        PROVIDERS = Collections.unmodifiableList(providers);
    }

    /**
     * 批量设置临时变量（用于同步 pm.variables）
     *
     * @param variables 变量 Map
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
     * 替换文本中的变量占位符
     * <p>
     * 示例: {{baseUrl}}/api/users -> https://api.example.com/api/users
     * <p>
     * 变量优先级: 临时变量 > 环境变量 > 内置函数
     *
     * @param text 包含变量占位符的文本
     * @return 替换后的文本
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
     * <p>
     * 按优先级查找：遍历所有 VariableProvider，优先级小的先查找
     *
     * @param varName 变量名
     * @return 变量值，如果未找到则返回 null
     */
    public static String resolveVariable(String varName) {
        if (varName == null || varName.isEmpty()) {
            return null;
        }

        // 按优先级遍历所有变量提供者
        for (VariableProvider provider : PROVIDERS) {
            if (provider.has(varName)) {
                String value = provider.get(varName);
                if (value != null) {
                    log.debug("Resolved variable '{}' from {}: {}", varName, provider.getName(), value);
                    return value;
                }
            }
        }

        log.debug("Variable '{}' not found in any provider", varName);
        return null;
    }

    /**
     * 检查变量是否已定义（包括临时变量、环境变量、内置函数）
     *
     * @param varName 变量名
     * @return 是否已定义
     */
    public static boolean isVariableDefined(String varName) {
        if (varName == null || varName.isEmpty()) {
            return false;
        }

        // 检查任一提供者是否包含该变量
        return PROVIDERS.stream().anyMatch(provider -> provider.has(varName));
    }

    /**
     * 获取所有可用的变量（包括临时变量、环境变量和内置函数）
     *
     * @return Map<变量名, 变量值或描述>
     */
    public static Map<String, String> getAllAvailableVariables() {
        Map<String, String> allVars = new LinkedHashMap<>();

        // 正序遍历，优先级高的先添加，使用 putIfAbsent 避免被低优先级的同名变量覆盖
        for (VariableProvider provider : PROVIDERS) {
            Map<String, String> providerVars = provider.getAll();
            for (Map.Entry<String, String> entry : providerVars.entrySet()) {
                String value = entry.getValue();
                // 如果值太长，截断显示
                if (value != null && value.length() > 50) {
                    value = value.substring(0, 47) + "...";
                }
                // 使用 putIfAbsent 保证高优先级的变量不会被覆盖
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
     *
     * @param prefix 前缀
     * @return 过滤后的变量 Map
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
