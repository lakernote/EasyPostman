package com.laker.postman.common.component.button;
import com.laker.postman.util.IconUtil;
import javax.swing.*;
import java.awt.*;
/**
 * 保存响应按钮
 * 用于保存HTTP响应到变量或文件
 */
public class SaveResponseButton extends JButton {
    public SaveResponseButton() {
        super();
        setIcon(IconUtil.createThemed("icons/save.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(false); // 去掉按钮的焦点边框
        setToolTipText("Save Response");
    }
}
