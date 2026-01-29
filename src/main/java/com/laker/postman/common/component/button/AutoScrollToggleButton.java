package com.laker.postman.common.component.button;

import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 自动滚动切换按钮
 * 用于切换控制台的自动滚动功能
 */
public class AutoScrollToggleButton extends JToggleButton {

    private static final String ICON_PATH = "icons/auto-scroll.svg";

    public AutoScrollToggleButton() {
        super();
        updateIcon(true); // 默认选中状态
        setToolTipText("Auto-scroll to bottom");
        setPreferredSize(new Dimension(28, 28));
        // 扁平化设计
        setFocusable(false);// 去掉按钮的焦点边框
        setBorderPainted(false); // 不绘制边框
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setSelected(true); // 默认启用自动滚动

        // 添加状态改变监听器，动态更新图标颜色
        addItemListener(e -> updateIcon(isSelected()));
    }

    /**
     * 根据按钮选中状态更新图标颜色
     *
     * @param selected 是否选中
     */
    private void updateIcon(boolean selected) {
        if (selected) {
            // 选中状态：使用白色图标
            setIcon(IconUtil.createColored(ICON_PATH, IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL, Color.WHITE));
        } else {
            // 未选中状态：使用主题适配颜色
            setIcon(IconUtil.createThemed(ICON_PATH, IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        }
    }
}
