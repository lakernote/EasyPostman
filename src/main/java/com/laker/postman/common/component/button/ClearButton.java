package com.laker.postman.common.component.button;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;

/**
 * 通用清空按钮，带图标和统一样式。
 */
public class ClearButton extends JButton {
    public ClearButton() {
        super(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));
        FlatSVGIcon icon = new FlatSVGIcon("icons/clear.svg", 20, 20);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
        setIcon(icon);
        setFocusable(false); // 去掉按钮的焦点边框
    }
}
