package com.laker.postman.common.component.button;

import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 清空按钮
 * 用于清空控制台或其他内容
 */
public class ClearButton extends JButton {
    /**
     * 默认构造函数：使用 SMALL 图标
     */
    public ClearButton() {
        this(IconUtil.SIZE_MEDIUM);
    }

    /**
     * 自定义构造函数
     *
     * @param iconSize 图标大小
     */
    public ClearButton(int iconSize) {
        super();
        setIcon(IconUtil.createThemed("icons/clear.svg", iconSize, iconSize));
        setToolTipText("Clear");
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
