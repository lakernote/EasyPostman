package com.laker.postman.plugin.capture;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContext;
import com.laker.postman.plugin.api.PluginContributionSupport;

public class CapturePlugin implements EasyPostmanPlugin {

    @Override
    public void onLoad(PluginContext context) {
        PluginContributionSupport.registerToolbox(
                context,
                "capture",
                "Capture",
                "icons/capture.svg",
                "toolbox.group.dev",
                "DEV",
                CapturePanel::new,
                CapturePlugin.class
        );
    }
}
