package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

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
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
