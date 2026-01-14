package com.laker.postman.common.component.button;
import com.laker.postman.util.IconUtil;
import javax.swing.*;
import java.awt.*;
/**
 * 上一个按钮
 * 用于搜索结果导航中的上一个匹配项
 */
public class PreviousButton extends JButton {
    public PreviousButton() {
        super();
        setIcon(IconUtil.createThemed("icons/arrow-up.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(false); // 去掉按钮的焦点边框
        setToolTipText("Previous");
    }
}
