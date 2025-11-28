package com.laker.postman.service.js;

import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 脚本执行上下文
 * 封装脚本执行所需的所有参数和环境信息
 */
@Getter
@Builder
public class ScriptExecutionContext {
    /**
     * 脚本内容
     */
    private final String script;

    /**
     * 脚本类型（用于日志标识）
     */
    private final ScriptType scriptType;

    /**
     * 变量绑定（如request、response、pm等）
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

