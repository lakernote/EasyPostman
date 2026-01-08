package com.laker.postman.common.component.button;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;

/**
 * CSV 数据按钮，带图标和统一样式。
 */
public class CSVButton extends JButton {
    public CSVButton() {
        super("  CSV ");
        FlatSVGIcon icon = new FlatSVGIcon("icons/csv.svg", 20, 20);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
        setIcon(icon);
        setFocusable(false); // 去掉按钮的焦点边框
    }
}
