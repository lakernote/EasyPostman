package com.laker.postman.common.component.button;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * 代码片段按钮
 * 用于打开代码片段对话框
 */
public class SnippetButton extends JButton {
    public SnippetButton() {
        super(I18nUtil.getMessage(MessageKeys.SCRIPT_BUTTON_SNIPPETS));
        setIcon(IconUtil.createThemed("icons/code.svg", 14, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // 设置手型光标
        setFocusable(false); // 去掉按钮的焦点边框
    }
}