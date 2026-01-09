package com.laker.postman.common.component.button;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;

/**
 * 通用导出按钮,带图标和统一样式。
 */
public class ExportButton extends JButton {
    public ExportButton() {
        super();
        FlatSVGIcon icon = new FlatSVGIcon("icons/export.svg", 20, 20);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
        setIcon(icon);
        setFocusable(false); // 去掉按钮的焦点边框
        setToolTipText(I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_TOOLTIP));
    }
}

