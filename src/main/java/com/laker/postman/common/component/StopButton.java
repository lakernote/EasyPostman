package com.laker.postman.common.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * 通用停止按钮，带图标和统一样式。
 */
public class StopButton extends JButton {
    public StopButton() {
        super(I18nUtil.getMessage(MessageKeys.BUTTON_STOP));
        setIcon(new FlatSVGIcon("icons/stop.svg"));
        setPreferredSize(new Dimension(90, 28));
    }
}
