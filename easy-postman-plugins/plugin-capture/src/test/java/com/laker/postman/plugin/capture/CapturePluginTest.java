package com.laker.postman.plugin.capture;

import com.laker.postman.plugin.api.PluginContext;
import com.laker.postman.plugin.api.PluginDescriptor;
import com.laker.postman.plugin.api.ScriptCompletionContributor;
import com.laker.postman.plugin.api.SnippetDefinition;
import com.laker.postman.plugin.api.StatusBarActionContribution;
import com.laker.postman.plugin.api.ToolboxContribution;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class CapturePluginTest {

    @Test
    public void shouldRegisterToolboxPanelAndStatusBarShortcut() {
        TestPluginContext context = new TestPluginContext();

        new CapturePlugin().onLoad(context);

        assertEquals(context.toolboxContributions.size(), 1);
        assertEquals(context.statusBarActions.size(), 1);
        StatusBarActionContribution shortcut = context.statusBarActions.get(0);
        assertEquals(shortcut.id(), "capture");
        assertEquals(shortcut.iconPath(), "icons/capture.svg");
        assertEquals(shortcut.targetType(), StatusBarActionContribution.TARGET_TOOLBOX);
        assertEquals(shortcut.targetId(), "capture");
        assertEquals(shortcut.order(), 100);
        assertFalse(shortcut.tooltip().isBlank());
    }

    private static final class TestPluginContext implements PluginContext {
        private final List<ToolboxContribution> toolboxContributions = new ArrayList<>();
        private final List<StatusBarActionContribution> statusBarActions = new ArrayList<>();

        @Override
        public PluginDescriptor descriptor() {
            return new PluginDescriptor("plugin-capture", "Capture", "1.0.0", CapturePlugin.class.getName());
        }

        @Override
        public void registerScriptApi(String alias, Supplier<Object> factory) {
        }

        @Override
        public <T> void registerService(Class<T> type, T service) {
        }

        @Override
        public void registerToolboxContribution(ToolboxContribution contribution) {
            toolboxContributions.add(contribution);
        }

        @Override
        public void registerStatusBarActionContribution(StatusBarActionContribution contribution) {
            statusBarActions.add(contribution);
        }

        @Override
        public void registerScriptCompletionContributor(ScriptCompletionContributor contributor) {
        }

        @Override
        public void registerSnippet(SnippetDefinition definition) {
        }
    }
}
