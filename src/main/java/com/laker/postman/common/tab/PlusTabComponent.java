package com.laker.postman.common.tab;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;

/**
 * PlusTabComponent 用于显示一个加号图标的标签组件
 */
public class PlusTabComponent extends JPanel {
    public PlusTabComponent() {
        setOpaque(false); // 设置为透明背景
        setFocusable(false); // 不可获取焦点
        setLayout(new BorderLayout()); // 使用 BorderLayout 布局

        JLabel plusLabel = new JLabel();
        plusLabel.setIcon(new FlatSVGIcon("icons/plus.svg", 20, 20)); // 使用SVG图标
        plusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        plusLabel.setVerticalAlignment(SwingConstants.CENTER);
        add(plusLabel, BorderLayout.CENTER);
    }
}