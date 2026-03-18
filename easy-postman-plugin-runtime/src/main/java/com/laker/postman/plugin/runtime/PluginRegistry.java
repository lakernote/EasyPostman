package com.laker.postman.plugin.runtime;

import com.laker.postman.plugin.api.ScriptCompletionContributor;
import com.laker.postman.plugin.api.SnippetDefinition;
import com.laker.postman.plugin.api.ToolboxContribution;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * 运行时扩展注册表。
 * <p>
 * 可以把它理解为“插件加载后的能力汇总表”：
 * 插件 onLoad 时往里注册，宿主在运行时再从这里读取。
 * </p>
 */
@Slf4j
public class PluginRegistry {

    // alias -> 工厂，用于延迟创建 pm.xxx 类型的脚本 API 对象
    private final Map<String, Supplier<Object>> scriptApiFactories = new ConcurrentHashMap<>();
    // type -> service，供宿主桥接层按类型取服务
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();
    // Toolbox / 自动补全 / Snippet 都是 UI 侧可扩展点
    private final List<ToolboxContribution> toolboxContributions = new CopyOnWriteArrayList<>();
    private final List<ScriptCompletionContributor> scriptCompletionContributors = new CopyOnWriteArrayList<>();
    private final List<SnippetDefinition> snippetDefinitions = new CopyOnWriteArrayList<>();

    public void registerScriptApi(String alias, Supplier<Object> factory) {
        if (alias == null || alias.isBlank() || factory == null) {
            return;
        }
        scriptApiFactories.put(alias, factory);
    }

    public Map<String, Object> createScriptApis() {
        Map<String, Object> apis = new LinkedHashMap<>();
        for (Map.Entry<String, Supplier<Object>> entry : scriptApiFactories.entrySet()) {
            try {
                // 某个插件 API 初始化失败时，只跳过当前插件，避免拖垮整个脚本环境
                Object api = entry.getValue().get();
                if (api != null) {
                    apis.put(entry.getKey(), api);
                }
            } catch (Throwable t) {
                log.error("Failed to create script API for plugin alias: {}", entry.getKey(), t);
            }
        }
        return apis;
    }

    public <T> void registerService(Class<T> type, T service) {
        if (type == null || service == null) {
            return;
        }
        services.put(type, service);
    }

    public <T> T getService(Class<T> type) {
        Object service = services.get(type);
        if (service == null) {
            return null;
        }
        return type.cast(service);
    }

    public void registerToolboxContribution(ToolboxContribution contribution) {
        if (contribution != null) {
            toolboxContributions.add(contribution);
        }
    }

    public List<ToolboxContribution> getToolboxContributions() {
        return new ArrayList<>(toolboxContributions);
    }

    public void registerScriptCompletionContributor(ScriptCompletionContributor contributor) {
        if (contributor != null) {
            scriptCompletionContributors.add(contributor);
        }
    }

    public List<ScriptCompletionContributor> getScriptCompletionContributors() {
        return new ArrayList<>(scriptCompletionContributors);
    }

    public void registerSnippet(SnippetDefinition definition) {
        if (definition != null) {
            snippetDefinitions.add(definition);
        }
    }

    public List<SnippetDefinition> getSnippetDefinitions() {
        return new ArrayList<>(snippetDefinitions);
    }

    public void clear() {
        // 应用退出或运行时重置时清空，避免跨会话残留旧插件状态
        scriptApiFactories.clear();
        services.clear();
        toolboxContributions.clear();
        scriptCompletionContributors.clear();
        snippetDefinitions.clear();
    }
}
