package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.CommonI18n;
import com.laker.postman.util.CommonMessageKeys;
import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 压缩按钮
 * 用于压缩代码或文本内容
 */
public class CompressButton extends JButton {
    public CompressButton() {
        setIcon(IconUtil.createThemed("icons/compress.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setToolTipText(CommonI18n.get(CommonMessageKeys.BUTTON_COMPRESS));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
