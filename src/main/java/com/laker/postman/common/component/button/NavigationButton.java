package com.laker.postman.common.component.button;

import com.laker.postman.util.IconUtil;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * 导航按钮
 * 用于上一个/下一个导航操作
 */
public class NavigationButton extends JButton {

    /**
     * 导航方向枚举
     */
    @Getter
    public enum Direction {
        PREVIOUS("icons/arrow-up.svg", "Previous"),
        NEXT("icons/arrow-down.svg", "Next");

        private final String iconPath;
        private final String tooltip;

        Direction(String iconPath, String tooltip) {
            this.iconPath = iconPath;
            this.tooltip = tooltip;
        }

    }

    public NavigationButton(Direction direction) {
        super();
        setIcon(IconUtil.createThemed(direction.getIconPath(), 18, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(direction.getTooltip());
        setFocusable(false); // 去掉按钮的焦点边框
    }
}
