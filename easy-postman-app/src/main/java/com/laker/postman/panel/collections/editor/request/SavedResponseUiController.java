package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.common.component.dialog.TextInputDialog;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.panel.collections.editor.CollectionTreeEditorGateway;
import com.laker.postman.panel.collections.editor.request.sub.RequestLinePanel;
import com.laker.postman.panel.collections.editor.request.sub.ResponsePanel;
import com.laker.postman.service.collections.SavedResponseSnapshotMapper;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.common.component.notification.NotificationCenter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 已保存响应相关的 UI 操作控制器。
 */
@Slf4j
@RequiredArgsConstructor
final class SavedResponseUiController {
    private final CollectionTreeEditorGateway collectionTreeGateway;

    String promptResponseName(Component owner) {
        String defaultName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        return TextInputDialog.showRequiredName(
                owner,
                I18nUtil.getMessage(MessageKeys.RESPONSE_SAVE_DIALOG_TITLE),
                defaultName,
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_SAVED_RESPONSE_EMPTY)
        ).orElse(null);
    }

    void saveResponse(String name,
                      PreparedRequest lastRequest,
                      HttpResponse lastResponse,
                      HttpRequestItem originalRequestItem) {
        try {
            SavedResponse savedResponse = SavedResponseSnapshotMapper.fromExchange(name, lastRequest, lastResponse);

            if (!collectionTreeGateway.saveResponseForRequest(originalRequestItem, savedResponse)) {
                log.warn("无法找到请求节点，保存响应失败");
                NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.RESPONSE_SAVE_REQUEST_NOT_FOUND));
                return;
            }

            NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.RESPONSE_SAVE_SUCCESS, name));
        } catch (Exception ex) {
            log.error("保存响应失败", ex);
            NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.RESPONSE_SAVE_ERROR, ex.getMessage()));
        }
    }

    void displaySavedResponse(ResponsePanel responsePanel,
                              RequestLinePanel requestLinePanel,
                              ActionListener sendAction,
                              SavedResponse savedResponse) {
        HttpResponse response = SavedResponseSnapshotMapper.toRuntimeResponse(savedResponse);
        SwingUtilities.invokeLater(() -> {
            requestLinePanel.setSendButtonToSend(sendAction);
            responsePanel.setResponseTabButtonsEnable(true);
            responsePanel.setResponseBody(response);
            responsePanel.setResponseHeaders(response);
            responsePanel.setStatus(response.code);
            responsePanel.setResponseTime(response.costMs);
            responsePanel.setResponseSize(response.bodySize, null);
            responsePanel.switchToTab(0);
            responsePanel.setResponseBodyEnabled(true);
        });
    }
}
