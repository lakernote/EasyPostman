package com.laker.postman.plugin.api;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * 插件贡献注册辅助工具。
 * <p>
 * 把多个官方插件里重复出现的注册样板代码收敛到一起，
 * 让插件入口类更聚焦在“声明自己提供哪些能力”。
 * </p>
 */
public final class PluginContributionSupport {

    public static final String SNIPPET_CATEGORY_EXAMPLES = "EXAMPLES";

    private PluginContributionSupport() {
    }

    public static void registerToolbox(PluginContext context,
                                       String id,
                                       String displayName,
                                       String iconPath,
                                       String groupId,
                                       String groupDisplayName,
                                       Supplier<JPanel> panelSupplier,
                                       Class<?> ownerClass) {
        context.registerToolboxContribution(new ToolboxContribution(
                id,
                displayName,
                iconPath,
                groupId,
                groupDisplayName,
                panelSupplier,
                ownerClass == null ? null : ownerClass.getClassLoader()
        ));
    }

    public static void addScriptApiCompletions(DefaultCompletionProvider provider,
                                               String alias,
                                               String apiDisplayName,
                                               String... methodNames) {
        if (provider == null || alias == null || alias.isBlank()) {
            return;
        }

        provider.addCompletion(new BasicCompletion(provider, "pm.plugin", "pm.plugin(alias)"));
        provider.addCompletion(new BasicCompletion(provider,
                "pm.plugin(\"" + alias + "\")",
                apiDisplayName));

        for (String methodName : methodNames) {
            if (methodName == null || methodName.isBlank()) {
                continue;
            }
            provider.addCompletion(new BasicCompletion(provider,
                    "pm.plugin(\"" + alias + "\")." + methodName,
                    "pm.plugin(\"" + alias + "\")." + methodName + "(options)"));
        }

        provider.addCompletion(new BasicCompletion(provider, "pm." + alias, apiDisplayName));
        for (String methodName : methodNames) {
            if (methodName == null || methodName.isBlank()) {
                continue;
            }
            provider.addCompletion(new BasicCompletion(provider,
                    "pm." + alias + "." + methodName,
                    "pm." + alias + "." + methodName + "(options)"));
        }
    }

    public static void addShorthandCompletion(DefaultCompletionProvider provider,
                                              String inputText,
                                              String replacementText,
                                              String shortDescription) {
        if (provider == null || inputText == null || inputText.isBlank()) {
            return;
        }
        provider.addCompletion(new ShorthandCompletion(
                provider,
                inputText,
                replacementText == null ? "" : replacementText,
                shortDescription
        ));
    }

    public static void registerSnippet(PluginContext context,
                                       String categoryId,
                                       String title,
                                       String description,
                                       String code) {
        if (context == null) {
            return;
        }
        context.registerSnippet(new SnippetDefinition(categoryId, title, description, code));
    }

    public static void registerExampleSnippet(PluginContext context,
                                              String title,
                                              String description,
                                              String code) {
        registerSnippet(context, SNIPPET_CATEGORY_EXAMPLES, title, description, code);
    }
}
