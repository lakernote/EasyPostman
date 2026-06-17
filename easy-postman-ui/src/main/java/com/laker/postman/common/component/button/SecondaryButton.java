package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 次要按钮 - 现代化设计
 * 完全支持亮色和暗色主题自适应
 * 用于次要操作（如取消、保存等）
 *
 * @author laker
 */
public class SecondaryButton extends JButton {
    private static final int ICON_SIZE = 14;

    public SecondaryButton(String text) {
        this(text, null);
    }

    public SecondaryButton(String text, String iconPath) {
        super(text);

        if (iconPath != null && !iconPath.isEmpty()) {
            setIcon(IconUtil.createThemed(iconPath, ICON_SIZE, ICON_SIZE));
            setIconTextGap(4);
        }

        // 设置字体和样式
        setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        putClientProperty(FlatClientProperties.STYLE_CLASS, ModernButtonFactory.SECONDARY_STYLE_CLASS);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setRolloverEnabled(true);
    }
}
