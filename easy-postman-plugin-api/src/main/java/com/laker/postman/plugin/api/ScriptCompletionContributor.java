package com.laker.postman.plugin.api;

import org.fife.ui.autocomplete.DefaultCompletionProvider;

/**
 * 向脚本编辑器追加补全项。
 */
@FunctionalInterface
public interface ScriptCompletionContributor {

    void contribute(DefaultCompletionProvider provider);
}
