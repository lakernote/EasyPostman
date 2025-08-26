package com.laker.postman.common.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * 通用开始按钮，带图标和统一样式。
 */
public class StartButton extends JButton {
    public StartButton() {
        super(I18nUtil.getMessage(MessageKeys.BUTTON_START));
        setIcon(new FlatSVGIcon("icons/start.svg"));
        setPreferredSize(new Dimension(90, 28));
    }
}
