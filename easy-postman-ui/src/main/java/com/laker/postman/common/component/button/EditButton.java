package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 编辑按钮
 */
public class EditButton extends JButton {
    public EditButton() {
        this(IconUtil.SIZE_SMALL);
    }

    public EditButton(int iconSize) {
        setIcon(IconUtil.createThemed("icons/edit.svg", iconSize, iconSize));
        setToolTipText("Edit");
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
