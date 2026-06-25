package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.ToolWindowActionToolbar;
import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;

/**
 * 帮助按钮
 * 用于显示帮助、指南等信息
 */
public class HelpButton extends JButton {
    public HelpButton() {
        setIcon(IconUtil.createPrimary("icons/help.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        setFocusable(false);// 去掉按钮的焦点边框
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setAlignmentY(Component.CENTER_ALIGNMENT);
        Dimension size = new Dimension(ToolWindowActionToolbar.ACTION_SIZE, ToolWindowActionToolbar.ACTION_SIZE);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setMargin(new Insets(0, 0, 0, 0));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        putClientProperty(FlatClientProperties.STYLE,
                "minimumWidth: " + ToolWindowActionToolbar.ACTION_SIZE
                        + "; minimumHeight: " + ToolWindowActionToolbar.ACTION_SIZE
                        + "; margin: 0,0,0,0");
    }
}
