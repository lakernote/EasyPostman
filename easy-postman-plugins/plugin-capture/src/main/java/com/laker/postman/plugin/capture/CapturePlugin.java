package com.laker.postman.plugin.capture;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContext;
import com.laker.postman.plugin.api.PluginContributionSupport;
import com.laker.postman.plugin.api.service.RequestCollectionImportService;

import java.lang.reflect.Method;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

public class CapturePlugin implements EasyPostmanPlugin {

    @Override
    public void onLoad(PluginContext context) {
        context.registerI18nBundle(CaptureI18n.BUNDLE_NAME);
        RequestCollectionImportService importService = context.getService(RequestCollectionImportService.class);
        PluginContributionSupport.registerToolbox(
                context,
                "capture",
                t(MessageKeys.TOOLBOX_CAPTURE),
                "icons/capture.svg",
                "toolbox.group.dev",
                t(MessageKeys.TOOLBOX_GROUP_DEV),
                () -> new CapturePanel(importService, context.storage()),
                CapturePlugin.class
        );
        registerStatusBarShortcut(context);
    }

    @Override
    public void onStop() {
        CaptureRuntime.stopQuietly();
    }

    private void registerStatusBarShortcut(PluginContext context) {
        try {
            Method method = PluginContributionSupport.class.getMethod(
                    "registerToolboxStatusBarAction",
                    PluginContext.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    int.class,
                    Class.class
            );
            method.invoke(
                    null,
                    context,
                    "capture",
                    t(MessageKeys.TOOLBOX_CAPTURE_STATUS_BAR_OPEN),
                    "icons/capture.svg",
                    "capture",
                    100,
                    CapturePlugin.class
            );
        } catch (NoSuchMethodException ignored) {
            // Older hosts do not expose the optional status-bar shortcut SPI.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to register capture status bar shortcut", e);
        }
    }
}
