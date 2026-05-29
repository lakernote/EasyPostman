package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;

/**
 * 帮助按钮
 * 用于显示帮助、指南等信息
 */
public class HelpButton extends JButton {
    public HelpButton() {
        setFocusable(false);// 去掉按钮的焦点边框
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_HELP);
    }
}
