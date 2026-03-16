package com.laker.postman.plugin.runtime;

import com.laker.postman.plugin.api.ScriptCompletionContributor;
import com.laker.postman.plugin.api.SnippetDefinition;
import com.laker.postman.plugin.api.ToolboxContribution;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * 运行时扩展注册表。
 */
public class PluginRegistry {

    private final Map<String, Supplier<Object>> scriptApiFactories = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();
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
            apis.put(entry.getKey(), entry.getValue().get());
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
}
