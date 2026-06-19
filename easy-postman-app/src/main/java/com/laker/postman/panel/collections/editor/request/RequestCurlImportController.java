package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.curl.CurlImportUtil;
import com.laker.postman.service.curl.CurlParser;
import com.laker.postman.service.curl.CurlRequest;
import com.laker.postman.util.AsyncClipboardUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.common.component.notification.NotificationCenter;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * cURL 输入导入控制器。
 * <p>
 * URL 输入框只负责承载文本，识别 cURL、解析请求和清理剪贴板都集中在这里。
 */
@RequiredArgsConstructor
final class RequestCurlImportController {
    private final JTextField urlField;
    private final Consumer<HttpRequestItem> requestImporter;

    void detectAndImport(boolean loadingData) {
        if (loadingData) {
            return;
        }

        String text = urlField.getText();
        if (text != null && text.trim().toLowerCase().startsWith("curl")) {
            SwingUtilities.invokeLater(() -> importCurl(text.trim()));
        }
    }

    private void importCurl(String text) {
        try {
            CurlRequest curlRequest = CurlParser.parse(text);
            if (curlRequest == null || curlRequest.url == null) {
                return;
            }

            HttpRequestItem item = CurlImportUtil.fromCurlRequest(curlRequest);
            if (item == null) {
                return;
            }

            requestImporter.accept(item);
            AsyncClipboardUtil.clearStringAsync();
            NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.PARSE_CURL_SUCCESS));
        } catch (Exception ignored) {
            // 用户可能还在输入半截 cURL，保持静默，避免输入过程中频繁弹错。
        }
    }
}
