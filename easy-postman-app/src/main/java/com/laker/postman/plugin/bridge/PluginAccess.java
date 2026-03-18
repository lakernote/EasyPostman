package com.laker.postman.plugin.bridge;

import com.laker.postman.plugin.api.ScriptCompletionContributor;
import com.laker.postman.plugin.api.SnippetDefinition;
import com.laker.postman.plugin.api.ToolboxContribution;
import com.laker.postman.plugin.runtime.PluginRuntime;

import java.util.List;
import java.util.Map;

/**
 * app 层访问插件运行时能力的统一入口。
 * <p>
 * UI、脚本上下文、桥接服务都通过这里拿插件贡献，避免在各处直接耦合 PluginRuntime。
 * 后续如果运行时实现调整，这里可以作为唯一收口点。
 * </p>
 */
public final class PluginAccess {

    private PluginAccess() {
    }

    public static Map<String, Object> createScriptApis() {
        return PluginRuntime.getRegistry().createScriptApis();
    }

    public static List<ToolboxContribution> getToolboxContributions() {
        return PluginRuntime.getRegistry().getToolboxContributions();
    }

    public static List<ScriptCompletionContributor> getScriptCompletionContributors() {
        return PluginRuntime.getRegistry().getScriptCompletionContributors();
    }

    public static List<SnippetDefinition> getSnippetDefinitions() {
        return PluginRuntime.getRegistry().getSnippetDefinitions();
    }

    public static <T> T getService(Class<T> type) {
        return PluginRuntime.getRegistry().getService(type);
    }
}
