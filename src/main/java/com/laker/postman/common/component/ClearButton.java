package com.laker.postman.common.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;

/**
 * 通用开始按钮，带图标和统一样式。
 */
public class ClearButton extends JButton {
    public ClearButton() {
        super(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));
        setIcon(new FlatSVGIcon("icons/clear.svg", 20, 20));
        setFocusable(false); // 去掉按钮的焦点边框
    }
}
