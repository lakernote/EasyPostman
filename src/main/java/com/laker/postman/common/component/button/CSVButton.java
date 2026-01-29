package com.laker.postman.common.component.button;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * CSV 数据按钮，带图标和统一样式。
 */
public class CSVButton extends JButton {
    public CSVButton() {
        super("  CSV ");
        FlatSVGIcon icon = new FlatSVGIcon("icons/csv.svg", 20, 20);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
        setIcon(icon);
        // 扁平化设计
        setFocusable(false);// 去掉按钮的焦点边框
        setContentAreaFilled(false); // 不填充内容区域
        setBorderPainted(false); // 不绘制边框
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 添加悬停效果
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setContentAreaFilled(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setContentAreaFilled(false);
            }
        });
    }
}
