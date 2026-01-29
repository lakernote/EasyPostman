package com.laker.postman.common.component.button;

import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 自动换行切换按钮
 * 用于切换文本编辑器的自动换行功能
 */
public class WrapToggleButton extends JToggleButton {

    private static final String ICON_PATH = "icons/wrap.svg";

    public WrapToggleButton() {
        updateIcon(false);
        setToolTipText("Toggle Line Wrap");
        // 扁平化设计
        setFocusable(false);// 去掉按钮的焦点边框
        setBorderPainted(false); // 不绘制边框
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setSelected(false); // 默认不启用换行

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
