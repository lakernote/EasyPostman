package com.laker.postman.common.component.button;

import com.laker.postman.util.CommonI18n;
import com.laker.postman.util.CommonMessageKeys;
import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 通用搜索按钮，带图标和统一样式。
 */
public class SearchButton extends JButton {
    public SearchButton() {
        setIcon(IconUtil.createThemed("icons/search.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setToolTipText(CommonI18n.get(CommonMessageKeys.BUTTON_SEARCH));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
