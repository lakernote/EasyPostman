package com.laker.postman.plugin.api;

/**
 * EasyPostman 插件入口。
 */
public interface EasyPostmanPlugin {

    /**
     * 插件加载时调用，用于注册扩展点。
     */
    void onLoad(PluginContext context);

    /**
     * 所有扩展注册完成后调用。
     */
    default void onStart() {
    }

    /**
     * 应用退出时调用。
     */
    default void onStop() {
    }
}
