package com.laker.postman.common.component.button;

import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 格式化按钮
 * 用于格式化代码或文本内容
 */
public class FormatButton extends JButton {
    public FormatButton() {
        super();
        setIcon(IconUtil.createThemed("icons/format.svg", 18, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(false); // 去掉按钮的焦点边框
    }
}