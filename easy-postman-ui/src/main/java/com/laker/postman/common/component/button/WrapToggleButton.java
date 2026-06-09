package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.UiI18n;
import com.laker.postman.util.UiMessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * 自动换行切换按钮
 * 用于切换文本编辑器的自动换行功能
 */
public class WrapToggleButton extends JToggleButton {

    private static final String ICON_PATH = "icons/wrap.svg";

    public WrapToggleButton() {
        setToolTipText(UiI18n.get(UiMessageKeys.BUTTON_TOGGLE_LINE_WRAP));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        setSelected(false);
        updateIcon(false);

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
            setIcon(IconUtil.createOnPrimary(ICON_PATH, IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        } else {
            setIcon(IconUtil.createThemed(ICON_PATH, IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        }
    }
}
