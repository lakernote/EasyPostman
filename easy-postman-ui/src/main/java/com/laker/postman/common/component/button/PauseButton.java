package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.CommonI18n;
import com.laker.postman.util.CommonMessageKeys;
import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Generic pause button with the shared toolbar styling.
 */
public class PauseButton extends JButton {
    public PauseButton() {
        setIcon(IconUtil.createThemed("icons/pause.svg", 20, 20));
        setToolTipText(CommonI18n.get(CommonMessageKeys.BUTTON_PAUSE));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
