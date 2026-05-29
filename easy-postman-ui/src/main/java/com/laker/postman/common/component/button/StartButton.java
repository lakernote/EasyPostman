package com.laker.postman.common.component.button;

import com.laker.postman.util.CommonI18n;
import com.laker.postman.util.CommonMessageKeys;
import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 通用开始按钮，带图标和统一样式。
 */
public class StartButton extends JButton {
    public StartButton() {
        setIcon(IconUtil.createThemed("icons/start.svg", 20, 20));
        setToolTipText(CommonI18n.get(CommonMessageKeys.BUTTON_START));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
