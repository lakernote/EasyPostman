package com.laker.postman.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

public final class ClipboardUtil {

    private static final Logger log = LoggerFactory.getLogger(ClipboardUtil.class);

    private ClipboardUtil() {
    }

    /**
     * 检查剪贴板是否有 cURL 命令，有则返回文本，否则返回 null。
     */
    public static String getClipboardCurlText() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable content = clipboard.getContents(null);
            if (content != null && content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = (String) content.getTransferData(DataFlavor.stringFlavor);
                if (text.trim().toLowerCase().startsWith("curl")) {
                    return text.trim();
                }
            }
        } catch (Exception e) {
            log.warn("获取剪贴板 cURL 文本失败", e);
        }
        return null;
    }
}
