package com.laker.postman.panel.collections.editor;

import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.service.curl.CurlImportUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiFunction;

import static com.laker.postman.util.ClipboardUtil.getClipboardCurlText;

/**
 * “+ Tab” 的点击控制器。
 * <p>
 * 新建请求、剪贴板 cURL 识别和导入都属于用户动作编排，不放在 RequestEditorPanel 的布局代码里。
 */
@RequiredArgsConstructor
final class RequestEditorPlusTabController {
    private final JTabbedPane tabbedPane;
    private final Component dialogParent;
    private final String plusTabTitle;
    private final String defaultRequestTitle;
    private final BiFunction<String, RequestItemProtocolEnum, RequestEditSubPanel> requestTabCreator;

    void install() {
        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }
        });
    }

    private void handleMousePressed(MouseEvent e) {
        int clickedTabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
        if (!isPlusTab(clickedTabIndex)) {
            return;
        }

        if (tryCreateRequestFromClipboardCurl()) {
            return;
        }
        requestTabCreator.apply(defaultRequestTitle, RequestItemProtocolEnum.HTTP);
    }

    private boolean isPlusTab(int tabIndex) {
        return tabIndex >= 0
                && tabIndex == tabbedPane.getTabCount() - 1
                && plusTabTitle.equals(tabbedPane.getTitleAt(tabIndex));
    }

    private boolean tryCreateRequestFromClipboardCurl() {
        String curlText = getClipboardCurlText();
        if (curlText == null) {
            return false;
        }
        int result = JOptionPane.showConfirmDialog(dialogParent,
                I18nUtil.getMessage(MessageKeys.CLIPBOARD_CURL_DETECTED),
                I18nUtil.getMessage(MessageKeys.IMPORT_CURL),
                JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return false;
        }

        try {
            HttpRequestItem item = CurlImportUtil.fromCurl(curlText);
            if (item == null) {
                return false;
            }
            RequestEditSubPanel tab = requestTabCreator.apply(null, item.getProtocol());
            item.setId(tab.getId());
            tab.initPanelData(item);
            clearClipboard();
            return true;
        } catch (Exception ex) {
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.PARSE_CURL_ERROR, ex.getMessage()));
            return true;
        }
    }

    private void clearClipboard() {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), null);
    }
}
