package com.laker.postman.plugin.capture;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContext;
import com.laker.postman.plugin.api.PluginContributionSupport;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

public class CapturePlugin implements EasyPostmanPlugin {

    @Override
    public void onLoad(PluginContext context) {
        PluginContributionSupport.registerToolbox(
                context,
                "capture",
                t(MessageKeys.TOOLBOX_CAPTURE),
                "icons/capture.svg",
                "toolbox.group.dev",
                t(MessageKeys.TOOLBOX_GROUP_DEV),
                CapturePanel::new,
                CapturePlugin.class
        );
    }

    @Override
    public void onStop() {
        CaptureRuntime.stopQuietly();
    }
}
