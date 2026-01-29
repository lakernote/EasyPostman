package com.laker.postman.common.component.button;

import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 上一个按钮
 * 用于搜索结果导航中的上一个匹配项
 */
public class PreviousButton extends JButton {
    public PreviousButton() {
        super();
        setIcon(IconUtil.createThemed("icons/arrow-up.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setToolTipText("Previous");
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
