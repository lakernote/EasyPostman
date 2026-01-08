package com.laker.postman.common.component.button;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;

/**
 * 通用停止按钮，带图标和统一样式。
 */
public class StopButton extends JButton {
    public StopButton() {
        super(I18nUtil.getMessage(MessageKeys.BUTTON_STOP));
        FlatSVGIcon icon = new FlatSVGIcon("icons/stop.svg", 20, 20);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
        setIcon(icon);
        setFocusable(false); // 去掉按钮的焦点边框
    }
}
