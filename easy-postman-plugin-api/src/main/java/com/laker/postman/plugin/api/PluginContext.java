package com.laker.postman.plugin.api;

import java.util.function.Supplier;

/**
 * 提供给插件的注册上下文。
 */
public interface PluginContext {

    PluginDescriptor descriptor();

    void registerScriptApi(String alias, Supplier<Object> factory);

    <T> void registerService(Class<T> type, T service);

    void registerToolboxContribution(ToolboxContribution contribution);

    void registerScriptCompletionContributor(ScriptCompletionContributor contributor);

    void registerSnippet(SnippetDefinition definition);
}
