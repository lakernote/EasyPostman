package com.laker.postman.common.component.button;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;

/**
 * 通用导入按钮,带图标和统一样式。
 */
public class ImportButton extends JButton {
    public ImportButton() {
        super();
        FlatSVGIcon icon = new FlatSVGIcon("icons/import.svg", 20, 20);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
        setIcon(icon);
        setFocusable(false); // 去掉按钮的焦点边框
        setToolTipText(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_TOOLTIP));
    }
}

