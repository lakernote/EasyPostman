package com.laker.postman.common.component.button;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * 通用搜索按钮，带图标和统一样式。
 */
public class SearchButton extends JButton {
    public SearchButton() {
        super();
        setIcon(IconUtil.createThemed("icons/search.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setToolTipText(I18nUtil.getMessage(MessageKeys.BUTTON_SEARCH));
        setFocusable(false); // 去掉按钮的焦点边框
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
