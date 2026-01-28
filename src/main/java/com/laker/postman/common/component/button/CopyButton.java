package com.laker.postman.common.component.button;

import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 复制按钮
 * 用于复制文本内容到剪贴板
 */
public class CopyButton extends JButton {
    public CopyButton() {
        super();
        setIcon(IconUtil.createThemed("icons/copy.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(false); // 去掉按钮的焦点边框
        setToolTipText("Copy to Clipboard");
    }
}
