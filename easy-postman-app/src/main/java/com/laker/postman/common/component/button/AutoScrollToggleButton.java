package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
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
        setToolTipText("Auto-scroll to bottom");
        setPreferredSize(new Dimension(28, 28));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        setSelected(true); // 默认启用自动滚动
        updateIcon(true);

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
