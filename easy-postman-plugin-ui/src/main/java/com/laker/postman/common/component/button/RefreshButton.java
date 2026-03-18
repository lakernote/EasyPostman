package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 通用刷新按钮，带图标和统一样式。
 */
public class RefreshButton extends JButton {
    public RefreshButton() {
        setIcon(IconUtil.createThemed("icons/refresh.svg", 20, 20));
        setToolTipText("Refresh");
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
