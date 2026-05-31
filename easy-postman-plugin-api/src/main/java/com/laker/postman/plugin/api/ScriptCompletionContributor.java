package com.laker.postman.plugin.api;

/**
 * 向脚本编辑器追加补全项。
 */
@FunctionalInterface
public interface ScriptCompletionContributor {

    void contribute(ScriptCompletionSink sink);
}
