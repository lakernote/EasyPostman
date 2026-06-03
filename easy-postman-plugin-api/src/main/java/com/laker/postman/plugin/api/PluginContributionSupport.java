package com.laker.postman.plugin.api;

import javax.swing.*;
import java.util.function.Function;
import java.util.function.Consumer;
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

    public static void registerSettingsContribution(PluginContext context,
                                                    String id,
                                                    String titleKey,
                                                    int order,
                                                    String category,
                                                    String titleBundleName,
                                                    Function<PluginSettingsContributionContext, ? extends JComponent> panelFactory,
                                                    Class<?> ownerClass) {
        if (context == null) {
            return;
        }
        context.registerSettingsContribution(new PluginSettingsContribution(
                id,
                titleKey,
                order,
                category,
                panelFactory,
                titleBundleName,
                ownerClass == null ? null : ownerClass.getClassLoader()
        ));
    }

    public static void registerPluginMenuAction(PluginContext context,
                                                String id,
                                                String titleKey,
                                                int order,
                                                String titleBundleName,
                                                Consumer<PluginMenuActionContext> action,
                                                Class<?> ownerClass) {
        registerMenuAction(
                context,
                id,
                PluginMenuContribution.PARENT_MENU_PLUGINS,
                titleKey,
                order,
                titleBundleName,
                action,
                ownerClass
        );
    }

    public static void registerMenuAction(PluginContext context,
                                          String id,
                                          String parentMenuId,
                                          String titleKey,
                                          int order,
                                          String titleBundleName,
                                          Consumer<PluginMenuActionContext> action,
                                          Class<?> ownerClass) {
        if (context == null) {
            return;
        }
        context.registerMenuContribution(new PluginMenuContribution(
                id,
                parentMenuId,
                titleKey,
                order,
                action,
                titleBundleName,
                ownerClass == null ? null : ownerClass.getClassLoader()
        ));
    }

    public static void addScriptApiCompletions(ScriptCompletionSink sink,
                                               String alias,
                                               String apiDisplayName,
                                               String... methodNames) {
        if (sink == null || alias == null || alias.isBlank()) {
            return;
        }

        sink.basic("pm.plugin", "pm.plugin(alias)");
        sink.basic("pm.plugin(\"" + alias + "\")", apiDisplayName);

        for (String methodName : methodNames) {
            if (methodName == null || methodName.isBlank()) {
                continue;
            }
            sink.basic(
                    "pm.plugin(\"" + alias + "\")." + methodName,
                    "pm.plugin(\"" + alias + "\")." + methodName + "(options)");
        }

    }

    public static void addSnippetCompletion(ScriptCompletionSink sink,
                                            String inputText,
                                            String replacementText,
                                            String shortDescription) {
        if (sink == null || inputText == null || inputText.isBlank()) {
            return;
        }
        sink.shorthand(
                inputText,
                replacementText,
                shortDescription);
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
