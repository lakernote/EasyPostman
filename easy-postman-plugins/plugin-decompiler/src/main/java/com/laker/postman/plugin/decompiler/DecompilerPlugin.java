package com.laker.postman.plugin.decompiler;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContributionSupport;
import com.laker.postman.plugin.api.PluginContext;

public class DecompilerPlugin implements EasyPostmanPlugin {

    @Override
    public void onLoad(PluginContext context) {
        context.registerI18nBundle(DecompilerI18n.BUNDLE_NAME);
        PluginContributionSupport.registerToolbox(
                context,
                "decompiler",
                DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER),
                "icons/decompile.svg",
                MessageKeys.TOOLBOX_GROUP_DEV,
                DecompilerI18n.t(MessageKeys.TOOLBOX_GROUP_DEV),
                DecompilerPanel::new,
                DecompilerPlugin.class
        );
    }
}
