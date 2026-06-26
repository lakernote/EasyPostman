package com.example;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContext;
import com.laker.postman.plugin.api.PluginMenuContribution;
import com.laker.postman.plugin.api.PluginSettingsContribution;
import com.laker.postman.plugin.api.PluginUpdateMetadata;
import com.laker.postman.plugin.api.PluginUpdateMetadataContribution;
import com.laker.postman.plugin.api.StatusBarActionContribution;

import javax.swing.JPanel;
import java.util.concurrent.atomic.AtomicInteger;

public class TestRuntimePlugin implements EasyPostmanPlugin {

    private static final AtomicInteger LOAD_COUNT = new AtomicInteger();
    private static final AtomicInteger START_COUNT = new AtomicInteger();
    private static final AtomicInteger STOP_COUNT = new AtomicInteger();

    public static void reset() {
        LOAD_COUNT.set(0);
        START_COUNT.set(0);
        STOP_COUNT.set(0);
    }

    public static int getLoadCount() {
        return LOAD_COUNT.get();
    }

    public static int getStartCount() {
        return START_COUNT.get();
    }

    public static int getStopCount() {
        return STOP_COUNT.get();
    }

    @Override
    public void onLoad(PluginContext context) {
        LOAD_COUNT.incrementAndGet();
        try {
            context.storage().writeString("runtime-storage.txt", context.descriptor().id());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to write plugin storage test file", e);
        }
        context.registerScriptApi("testRuntime", Object::new);
        context.registerSettingsContribution(new PluginSettingsContribution(
                "test-runtime-settings",
                "test.runtime.settings.title",
                900,
                PluginSettingsContribution.CATEGORY_EXTENSIONS,
                settingsContext -> new JPanel(),
                "test-runtime-messages"
        ));
        context.registerMenuContribution(new PluginMenuContribution(
                "test-runtime-action",
                PluginMenuContribution.PARENT_MENU_PLUGINS,
                "test.runtime.action.title",
                900,
                actionContext -> {
                },
                "test-runtime-messages"
        ));
        context.registerStatusBarActionContribution(new StatusBarActionContribution(
                "test-runtime-status-action",
                "Open runtime tool",
                "icons/global-variables.svg",
                StatusBarActionContribution.TARGET_TOOLBOX,
                "runtime-tool",
                900
        ));
        context.registerUpdateMetadataContribution(new PluginUpdateMetadataContribution(
                "test-runtime-update-metadata",
                900,
                () -> java.util.List.of(new PluginUpdateMetadata(
                        context.descriptor().id(),
                        context.descriptor().name(),
                        "1.0.1",
                        "Test runtime plugin update",
                        "https://example.com/test-runtime-1.0.1.jar",
                        "https://example.com/test-runtime",
                        "sha256-test-runtime-1.0.1"
                ))
        ));
    }

    @Override
    public void onStart() {
        START_COUNT.incrementAndGet();
    }

    @Override
    public void onStop() {
        STOP_COUNT.incrementAndGet();
    }
}
