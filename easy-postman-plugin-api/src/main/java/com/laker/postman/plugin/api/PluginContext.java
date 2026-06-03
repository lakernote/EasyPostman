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
     * 例如插件可以把自己暴露成 pm.plugin("redis") / pm.plugin("kafka")。
     */
    void registerScriptApi(String alias, Supplier<Object> factory);

    /**
     * 注册宿主和其他桥接层可消费的服务对象。
     */
    <T> void registerService(Class<T> type, T service);

    /**
     * 获取宿主或其他插件已注册的桥接服务。
     */
    default <T> T getService(Class<T> type) {
        return null;
    }

    /**
     * 注册 Toolbox 面板扩展。
     */
    void registerToolboxContribution(ToolboxContribution contribution);

    /**
     * 注册设置页扩展。
     */
    default void registerSettingsContribution(PluginSettingsContribution contribution) {
        // Runtime implementations can expose this through the host settings dialog.
    }

    /**
     * 注册脚本自动补全扩展。
     */
    void registerScriptCompletionContributor(ScriptCompletionContributor contributor);

    /**
     * 注册脚本片段。
     */
    void registerSnippet(SnippetDefinition definition);

    /**
     * 注册插件自有国际化资源包。
     * <p>
     * bundleName 使用 {@link java.util.ResourceBundle#getBundle(String)} 的 base name，
     * 例如 {@code redis-messages}。
     * </p>
     */
    default void registerI18nBundle(String bundleName) {
        // Runtime implementations can hook this into the shared i18n registry.
    }
}
