package com.laker.postman.common.component.setting;

import org.testng.annotations.Test;

import java.awt.Dimension;

import static org.testng.Assert.assertTrue;

public class SettingsHintLabelTest {

    @Test
    public void longHintShouldWrapWithinConfiguredWidth() {
        SettingsHintLabel label = new SettingsHintLabel(sidebarHint(), 320);

        Dimension preferredSize = label.getPreferredSize();

        assertTrue(label.getText().equals(sidebarHint()),
                "Hint text should remain plain text instead of relying on Swing HTML rendering");
        assertTrue(preferredSize.width <= 324,
                "Wrapped hint preferred width should stay within configured width");
        assertTrue(preferredSize.height > label.getFontMetrics(label.getFont()).getHeight(),
                "Long hint should wrap to multiple lines instead of clipping horizontally");
    }

    @Test
    public void mixedChineseEnglishHintShouldWrapInsteadOfUsingHtmlLabelRenderer() {
        SettingsHintLabel label = new SettingsHintLabel(
                "客户端证书能力已拆分为可选插件。安装官方 Client Certificate 插件后，即可继续配置 mTLS 证书并自动应用到请求。",
                360
        );

        Dimension preferredSize = label.getPreferredSize();

        assertTrue(preferredSize.width <= 364,
                "Mixed Chinese/English hint preferred width should stay within configured width");
        assertTrue(preferredSize.height > label.getFontMetrics(label.getFont()).getHeight(),
                "Mixed Chinese/English hint should wrap to multiple lines");
    }

    private static String sidebarHint() {
        return "拖动列表项可调整顺序；点击左侧复选框或按空格键可隐藏或显示菜单。至少保留一个菜单。";
    }
}
