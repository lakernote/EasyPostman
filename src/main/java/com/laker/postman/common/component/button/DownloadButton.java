package com.laker.postman.common.component.button;
import com.laker.postman.util.IconUtil;
import javax.swing.*;
import java.awt.*;
/**
 * 下载按钮
 * 用于下载响应内容到本地文件
 */
public class DownloadButton extends JButton {
    public DownloadButton() {
        super();
        setIcon(IconUtil.createThemed("icons/download.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(false); // 去掉按钮的焦点边框
        setToolTipText("Download");
    }
}
