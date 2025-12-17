package com.laker.postman.common.component.button;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;

/**
 * 通用开始按钮，带图标和统一样式。
 */
public class CSVButton extends JButton {
    public CSVButton() {
        super("  CSV ");
        setIcon(new FlatSVGIcon("icons/csv.svg", 20, 20));
        setFocusable(false); // 去掉按钮的焦点边框
    }
}
