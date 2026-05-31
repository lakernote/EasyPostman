package com.laker.postman.plugin.api;

/**
 * 不绑定具体编辑器实现的脚本补全项。
 */
public record ScriptCompletionItem(
        ScriptCompletionKind kind,
        String inputText,
        String replacementText,
        String shortDescription
) {
}
