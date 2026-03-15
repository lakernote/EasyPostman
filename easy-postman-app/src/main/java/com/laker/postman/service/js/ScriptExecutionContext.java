package com.laker.postman.service.js;

import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 脚本执行上下文配置
 * <p>
 * 封装脚本执行所需的所有配置信息和运行时数据，包括脚本内容、变量绑定、
 * 脚本类型等。该类作为脚本执行流程中各组件之间传递数据的载体。
 * </p>
 *
 * <h3>主要职责：</h3>
 * <ul>
 *   <li>存储脚本内容和类型（前置/后置）</li>
 *   <li>管理脚本执行的变量绑定（pm, env, request, response 等）</li>
 *   <li>提供构建器模式方便创建实例</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 使用构建器创建执行上下文
 * ScriptExecutionContext context = ScriptExecutionContext.builder()
 *     .script("pm.environment.set('timestamp', Date.now());")
 *     .scriptType(ScriptType.PRE_REQUEST)
 *     .bindings(bindings)
 *     .build();
 *
 * // 执行脚本
 * JsScriptExecutor.execute(context);
 * }</pre>
 *
 * @see JsScriptExecutor
 * @see ScriptType
 * @author laker
 */
@Getter
@Builder
public class ScriptExecutionContext {
    /**
     * 脚本内容（JavaScript 代码）
     */
    private final String script;

    /**
     * 脚本类型（用于日志标识和错误追踪）
     * @see ScriptType
     */
    private final ScriptType scriptType;

    /**
     * 变量绑定 Map
     * <p>
     * 包含脚本执行时可访问的所有变量，例如：
     * <ul>
     *   <li>"pm" → PostmanApiContext 实例</li>
     *   <li>"postman" → PostmanApiContext 实例（别名）</li>
     *   <li>"env" → Environment 实例</li>
     *   <li>"request" → PreparedRequest 实例</li>
     *   <li>"response" → HttpResponse 实例（仅后置脚本）</li>
     *   <li>"responseBody" → 响应体字符串（仅后置脚本）</li>
     *   <li>"statusCode" → 响应状态码（仅后置脚本）</li>
     * </ul>
     * </p>
     */
    @Builder.Default
    private final Map<String, Object> bindings = new HashMap<>();

    /**
     * 输出回调
     */
    private final JsScriptExecutor.OutputCallback outputCallback;

    /**
     * 是否在错误时显示对话框
     */
    @Builder.Default
    private final boolean showErrorDialog = false;

    /**
     * 脚本类型枚举
     */
    @Getter
    public enum ScriptType {
        PRE_REQUEST("PreScript"),
        POST_REQUEST("PostScript"),
        TEST("Test"),
        CUSTOM("Custom");

        private final String displayName;

        ScriptType(String displayName) {
            this.displayName = displayName;
        }

    }

    /**
     * 添加绑定变量
     */
    public void addBinding(String name, Object value) {
        bindings.put(name, value);
    }

    /**
     * 批量添加绑定变量
     */
    public void addBindings(Map<String, Object> newBindings) {
        if (newBindings != null) {
            bindings.putAll(newBindings);
        }
    }
}
