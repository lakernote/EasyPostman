package com.laker.postman.common.component.button;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;

/**
 * 通用保存按钮，带图标和统一样式。
 */
public class SaveButton extends JButton {
    public SaveButton() {
        super(I18nUtil.getMessage(MessageKeys.BUTTON_SAVE));
        setIcon(IconUtil.createThemed("icons/save.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        setFocusable(false); // 去掉按钮的焦点边框
        setToolTipText(I18nUtil.getMessage(MessageKeys.BUTTON_SAVE_TOOLTIP));
    }
}
