package com.laker.postman.common.component.button;

import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 编辑按钮
 * 用于编辑、修改等操作
 */
public class EditButton extends JButton {
    /**
     * 默认构造函数：使用 MEDIUM 尺寸图标
     */
    public EditButton() {
        this(IconUtil.SIZE_SMALL);
    }

    /**
     * 自定义构造函数
     *
     * @param iconSize 图标大小
     */
    public EditButton(int iconSize) {
        setIcon(IconUtil.createThemed("icons/edit.svg", iconSize, iconSize));
        setToolTipText("Edit");
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
