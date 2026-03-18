package com.laker.postman.plugin.runtime;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContext;
import com.laker.postman.plugin.api.PluginDescriptor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

/**
 * 插件加载器。
 * <p>
 * 负责类加载、实例化、生命周期调用，以及把插件注册动作接入运行时注册表。
 * </p>
 */
@Slf4j
final class PluginLoader {

    private PluginLoader() {
    }

    static void loadPluginJar(Path jarPath,
                              PluginDescriptor descriptor,
                              PluginRegistry registry,
                              List<EasyPostmanPlugin> loadedPlugins,
                              List<URLClassLoader> pluginClassLoaders,
                              List<PluginFileInfo> loadedPluginFiles,
                              ClassLoader parentClassLoader) {
        try {
            URLClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, parentClassLoader);
            Class<?> entryClass = Class.forName(descriptor.entryClass(), true, classLoader);
            Object instance = entryClass.getDeclaredConstructor().newInstance();
            if (!(instance instanceof EasyPostmanPlugin plugin)) {
                throw new IllegalStateException("Plugin entry class does not implement EasyPostmanPlugin: " + descriptor.entryClass());
            }
            plugin.onLoad(new PluginContextImpl(descriptor, registry));
            loadedPlugins.add(plugin);
            pluginClassLoaders.add(classLoader);
            loadedPluginFiles.add(new PluginFileInfo(descriptor, jarPath, true, true, true));
        } catch (Exception e) {
            log.error("Failed to load plugin jar: {}", jarPath, e);
        }
    }

    static void startPlugins(List<EasyPostmanPlugin> plugins) {
        for (EasyPostmanPlugin plugin : plugins) {
            try {
                plugin.onStart();
            } catch (Exception e) {
                log.error("Failed to start plugin: {}", plugin.getClass().getName(), e);
            }
        }
    }

    static void stopPlugins(List<EasyPostmanPlugin> plugins) {
        for (EasyPostmanPlugin plugin : plugins) {
            try {
                plugin.onStop();
            } catch (Exception e) {
                log.warn("Failed to stop plugin: {}", plugin.getClass().getName(), e);
            }
        }
    }

    static void closeClassLoaders(List<URLClassLoader> classLoaders) {
        for (URLClassLoader classLoader : classLoaders) {
            try {
                classLoader.close();
            } catch (IOException e) {
                log.warn("Failed to close plugin classloader", e);
            }
        }
    }

    private static final class PluginContextImpl implements PluginContext {
        private final PluginDescriptor descriptor;
        private final PluginRegistry registry;

        private PluginContextImpl(PluginDescriptor descriptor, PluginRegistry registry) {
            this.descriptor = descriptor;
            this.registry = registry;
        }

        @Override
        public PluginDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public void registerScriptApi(String alias, java.util.function.Supplier<Object> factory) {
            registry.registerScriptApi(descriptor.id(), alias, factory);
        }

        @Override
        public <T> void registerService(Class<T> type, T service) {
            registry.registerService(descriptor.id(), type, service);
        }

        @Override
        public void registerToolboxContribution(com.laker.postman.plugin.api.ToolboxContribution contribution) {
            registry.registerToolboxContribution(contribution);
        }

        @Override
        public void registerScriptCompletionContributor(com.laker.postman.plugin.api.ScriptCompletionContributor contributor) {
            registry.registerScriptCompletionContributor(contributor);
        }

        @Override
        public void registerSnippet(com.laker.postman.plugin.api.SnippetDefinition definition) {
            registry.registerSnippet(definition);
        }
    }
}
