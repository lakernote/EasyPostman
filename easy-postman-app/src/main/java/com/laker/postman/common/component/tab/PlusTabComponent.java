package com.laker.postman.common.component.tab;

import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

import static com.laker.postman.util.IconUtil.SIZE_MEDIUM;

/**
 * PlusTabComponent 用于显示一个加号图标的标签组件
 */
public class PlusTabComponent extends JPanel {
    public PlusTabComponent() {
        setOpaque(false); // 设置为透明背景
        setFocusable(false); // 不可获取焦点
        setLayout(new BorderLayout()); // 使用 BorderLayout 布局

        JLabel plusLabel = new JLabel();
        plusLabel.setIcon(IconUtil.createThemed("icons/plus.svg", SIZE_MEDIUM, SIZE_MEDIUM)); // 使用SVG图标
        plusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        plusLabel.setVerticalAlignment(SwingConstants.CENTER);
        add(plusLabel, BorderLayout.CENTER);
    }
}