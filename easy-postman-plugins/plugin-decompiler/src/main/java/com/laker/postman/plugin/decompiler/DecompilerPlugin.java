package com.laker.postman.plugin.decompiler;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContext;
import com.laker.postman.plugin.api.ToolboxContribution;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

public class DecompilerPlugin implements EasyPostmanPlugin {

    private static final String TOOLBOX_GROUP_DEV = "toolbox.group.dev";

    @Override
    public void onLoad(PluginContext context) {
        context.registerToolboxContribution(new ToolboxContribution(
                "decompiler",
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER),
                "icons/decompile.svg",
                TOOLBOX_GROUP_DEV,
                I18nUtil.getMessage(TOOLBOX_GROUP_DEV),
                DecompilerPanel::new,
                null
        ));
    }
}
