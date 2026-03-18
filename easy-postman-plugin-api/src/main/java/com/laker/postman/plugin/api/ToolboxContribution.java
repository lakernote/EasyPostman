package com.laker.postman.plugin.api;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * 工具箱面板扩展定义。
 */
public record ToolboxContribution(
        String id,
        String displayName,
        String iconPath,
        String groupId,
        String groupDisplayName,
        Supplier<JPanel> panelSupplier,
        ClassLoader iconClassLoader
) {

    public ToolboxContribution(
            String id,
            String displayName,
            String iconPath,
            String groupId,
            String groupDisplayName,
            Supplier<JPanel> panelSupplier
    ) {
        this(id, displayName, iconPath, groupId, groupDisplayName, panelSupplier, null);
    }
}
