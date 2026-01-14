package com.laker.postman.common.component.button;
import com.laker.postman.util.IconUtil;
import javax.swing.*;
import java.awt.*;
/**
 * 下一个按钮
 * 用于搜索结果导航中的下一个匹配项
 */
public class NextButton extends JButton {
    public NextButton() {
        super();
        setIcon(IconUtil.createThemed("icons/arrow-down.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(false); // 去掉按钮的焦点边框
        setToolTipText("Next");
    }
}
