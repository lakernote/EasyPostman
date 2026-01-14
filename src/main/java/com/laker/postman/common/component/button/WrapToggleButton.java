package com.laker.postman.common.component.button;
import com.laker.postman.util.IconUtil;
import javax.swing.*;
import java.awt.*;
/**
 * 自动换行切换按钮
 * 用于切换文本编辑器的自动换行功能
 */
public class WrapToggleButton extends JToggleButton {
    public WrapToggleButton() {
        super();
        setIcon(IconUtil.createThemed("icons/wrap.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(false); // 去掉按钮的焦点边框
        setSelected(false); // 默认不启用换行
        setToolTipText("Toggle Line Wrap");
    }
}
