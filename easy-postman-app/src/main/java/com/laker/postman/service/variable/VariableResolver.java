package com.laker.postman.service.variable;

import com.laker.postman.model.VariableInfo;
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
     * 获取变量的类型
     *
     * @param varName 变量名
     * @return 变量类型，如果变量不存在则返回 null
     */
    public static VariableType getVariableType(String varName) {
        if (varName == null || varName.isEmpty()) {
            return null;
        }

        for (VariableProvider provider : PROVIDERS) {
            if (provider.has(varName)) {
                return provider.getType();
            }
        }

        return null;
    }

    /**
     * 获取所有可用变量（包含类型信息）
     *
     * @return 变量信息列表
     */
    public static List<VariableInfo> getAllAvailableVariablesWithType() {
        Map<String, VariableInfo> varMap = new LinkedHashMap<>();

        // 按优先级添加，使用 putIfAbsent 避免被低优先级覆盖
        for (VariableProvider provider : PROVIDERS) {
            Map<String, String> providerVars = provider.getAll();
            for (Map.Entry<String, String> entry : providerVars.entrySet()) {
                varMap.putIfAbsent(entry.getKey(),
                        new VariableInfo(entry.getKey(), entry.getValue(), provider.getType()));
            }
        }

        return new ArrayList<>(varMap.values());
    }

    /**
     * 根据前缀过滤变量列表（包含类型信息）
     *
     * @param prefix 过滤前缀
     * @return 变量信息列表
     */
    public static List<VariableInfo> filterVariablesWithType(String prefix) {
        List<VariableInfo> allVars = getAllAvailableVariablesWithType();
        if (prefix == null || prefix.isEmpty()) {
            return allVars;
        }

        List<VariableInfo> filtered = new ArrayList<>();
        String lowerPrefix = prefix.toLowerCase();
        for (VariableInfo varInfo : allVars) {
            if (varInfo.getName().toLowerCase().startsWith(lowerPrefix)) {
                filtered.add(varInfo);
            }
        }
        return filtered;
    }
}
