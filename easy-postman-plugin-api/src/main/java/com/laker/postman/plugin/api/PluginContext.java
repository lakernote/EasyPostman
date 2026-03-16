package com.laker.postman.plugin.api;

import java.util.function.Supplier;

/**
 * 提供给插件的注册上下文。
 * <p>
 * 插件 onLoad 阶段只应该通过这个接口向宿主“声明能力”，
 * 比如注册脚本 API、服务对象、Toolbox 面板、补全和 Snippet。
 * 这样可以避免插件直接反向依赖宿主内部实现。
 * </p>
 */
public interface PluginContext {

    /**
        * 当前插件自身的描述信息。
        */
    PluginDescriptor descriptor();

    /**
     * 注册脚本 API。
     * 例如插件可以把自己暴露成 pm.redis / pm.kafka。
     */
    void registerScriptApi(String alias, Supplier<Object> factory);

    /**
     * 注册宿主和其他桥接层可消费的服务对象。
     */
    <T> void registerService(Class<T> type, T service);

    /**
     * 注册 Toolbox 面板扩展。
     */
    void registerToolboxContribution(ToolboxContribution contribution);

    /**
     * 注册脚本自动补全扩展。
     */
    void registerScriptCompletionContributor(ScriptCompletionContributor contributor);

    /**
     * 注册脚本片段。
     */
    void registerSnippet(SnippetDefinition definition);
}
