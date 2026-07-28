package com.laker.postman.common.component.setting;

import org.testng.annotations.Test;

import java.awt.Dimension;

import static org.testng.Assert.assertTrue;

public class SettingsHintLabelTest {

    private static final int HINT_WIDTH = 320;

    @Test
    public void hintShouldWrapOnWordBoundaries() {
        SettingsHintLabel label = new SettingsHintLabel(
                "Disabled by default. Enable it only when outbound access is restricted or requests need a corporate proxy.",
                320
        );

        assertTrue(label.getLineWrap(),
                "Hint text should wrap within the configured width");
        assertTrue(label.getWrapStyleWord(),
                "English hint text should wrap on word boundaries instead of splitting words");
    }

    @Test
    public void longHintShouldWrapWithinConfiguredWidth() {
        SettingsHintLabel label = new SettingsHintLabel("", HINT_WIDTH);
        String hint = textWiderThanTwoLines(label, sidebarHint());
        label.setPlainText(hint);

        Dimension preferredSize = label.getPreferredSize();

        assertTrue(label.getText().equals(hint),
                "Hint text should remain plain text instead of relying on Swing HTML rendering");
        assertTrue(preferredSize.width <= HINT_WIDTH + 4,
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

    private static String textWiderThanTwoLines(SettingsHintLabel label, String sample) {
        StringBuilder text = new StringBuilder(sample);
        int minimumWidth = HINT_WIDTH * 2;
        while (label.getFontMetrics(label.getFont()).stringWidth(text.toString()) <= minimumWidth) {
            text.append(sample);
        }
        return text.toString();
    }
}
