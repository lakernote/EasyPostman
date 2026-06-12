package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.editor.request.sub.AuthTabPanel;
import com.laker.postman.panel.collections.editor.request.sub.EasyRequestHttpHeadersPanel;
import com.laker.postman.panel.collections.editor.request.sub.EasyRequestParamsPanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestBodyPanel;
import com.laker.postman.panel.collections.editor.request.sub.ResponsePanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.BooleanSupplier;

final class RequestEditorUiBinder {
    private RequestEditorUiBinder() {
    }

    static void bindUrlField(JTextField urlField,
                             RequestPreparationFeedbackPresenter feedbackPresenter,
                             Runnable detectAndParseCurl,
                             Runnable parseUrlParamsToParamsPanel,
                             Runnable autoPrependProtocolAction) {
        urlField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                feedbackPresenter.clearUrlValidationFeedback(urlField);
                detectAndParseCurl.run();
                parseUrlParamsToParamsPanel.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                feedbackPresenter.clearUrlValidationFeedback(urlField);
                parseUrlParamsToParamsPanel.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                feedbackPresenter.clearUrlValidationFeedback(urlField);
                detectAndParseCurl.run();
                parseUrlParamsToParamsPanel.run();
            }
        });
        urlField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                autoPrependProtocolAction.run();
            }
        });
        urlField.addActionListener(e -> autoPrependProtocolAction.run());
    }

    static void bindParamsSync(EasyRequestParamsPanel paramsPanel, Runnable parseParamsPanelToUrl) {
        paramsPanel.addTableModelListener(e -> parseParamsPanelToUrl.run());
    }

    static void applyInitialProtocolUi(RequestItemProtocolEnum protocol,
                                       JTabbedPane reqTabs,
                                       RequestBodyPanel requestBodyPanel,
                                       JComponent paramsTabPanel,
                                       AuthTabPanel authTabPanel,
                                       ActionListener wsSendAction) {
        if (protocol.isWebSocketProtocol()) {
            requestBodyPanel.setWsSendActionListener(wsSendAction);
            RequestTabSelector.removeIfPresent(reqTabs, authTabPanel);
            RequestTabSelector.selectFirstVisible(reqTabs, requestBodyPanel, paramsTabPanel);
            requestBodyPanel.setWebSocketConnected(false);
        } else if (protocol.isSseProtocol()) {
            RequestTabSelector.removeIfPresent(reqTabs, authTabPanel);
            RequestTabSelector.selectFirstVisible(reqTabs, paramsTabPanel, requestBodyPanel);
        } else {
            RequestTabSelector.selectFirstVisible(reqTabs, paramsTabPanel, requestBodyPanel);
        }
    }

    static void bindSaveResponseButton(RequestItemProtocolEnum protocol,
                                       RequestEditSubPanelType panelType,
                                       ResponsePanel responsePanel,
                                       ActionListener saveAction) {
        if (responsePanel == null) {
            return;
        }
        if (protocol.isHttpProtocol()
                && panelType != RequestEditSubPanelType.SAVED_RESPONSE
                && responsePanel.getSaveResponseButton() != null) {
            responsePanel.getSaveResponseButton().addActionListener(saveAction);
        }
    }

    static void bindBodyTypeHeaderSync(RequestBodyPanel requestBodyPanel,
                                       EasyRequestHttpHeadersPanel headersPanel,
                                       BooleanSupplier isLoadingDataSupplier) {
        requestBodyPanel.getBodyTypeComboBox().addActionListener(e -> {
            if (isLoadingDataSupplier.getAsBoolean()) {
                return;
            }

            String selectedType = (String) requestBodyPanel.getBodyTypeComboBox().getSelectedItem();
            if (RequestBodyPanel.BODY_TYPE_NONE.equals(selectedType)) {
                headersPanel.removeHeader("Content-Type");
                return;
            }

            String contentType = null;
            if (RequestBodyPanel.BODY_TYPE_RAW.equals(selectedType)) {
                contentType = "application/json";
            } else if (RequestBodyPanel.BODY_TYPE_FORM_URLENCODED.equals(selectedType)) {
                contentType = "application/x-www-form-urlencoded";
            } else if (RequestBodyPanel.BODY_TYPE_FORM_DATA.equals(selectedType)) {
                contentType = "multipart/form-data";
            }
            if (contentType != null) {
                headersPanel.setOrUpdateHeader("Content-Type", contentType);
            }
        });
    }
}
