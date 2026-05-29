package com.laker.postman.common.component.button;

import com.laker.postman.util.CommonI18n;
import com.laker.postman.util.CommonMessageKeys;
import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 通用导入按钮,带图标和统一样式。
 */
public class ImportButton extends JButton {
    public ImportButton() {
        setIcon(IconUtil.createThemed("icons/import.svg", 20, 20));
        setToolTipText(CommonI18n.get(CommonMessageKeys.BUTTON_IMPORT));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
