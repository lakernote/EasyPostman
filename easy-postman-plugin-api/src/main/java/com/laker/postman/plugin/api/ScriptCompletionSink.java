package com.laker.postman.plugin.api;

/**
 * 插件向宿主提交脚本补全项的中立出口。
 */
public interface ScriptCompletionSink {

    void add(ScriptCompletionItem item);

    default void basic(String inputText, String shortDescription) {
        if (inputText == null || inputText.isBlank()) {
            return;
        }
        add(new ScriptCompletionItem(
                ScriptCompletionKind.BASIC,
                inputText,
                inputText,
                shortDescription
        ));
    }

    default void shorthand(String inputText, String replacementText, String shortDescription) {
        if (inputText == null || inputText.isBlank()) {
            return;
        }
        add(new ScriptCompletionItem(
                ScriptCompletionKind.SHORTHAND,
                inputText,
                replacementText == null ? "" : replacementText,
                shortDescription
        ));
    }
}
