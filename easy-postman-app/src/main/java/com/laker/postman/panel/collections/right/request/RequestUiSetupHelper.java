package com.laker.postman.panel.collections.right.request;

import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.model.RequestEditSubPanelType;
import com.laker.postman.panel.collections.right.request.sub.AuthTabPanel;
import com.laker.postman.panel.collections.right.request.sub.EasyRequestHttpHeadersPanel;
import com.laker.postman.panel.collections.right.request.sub.EasyRequestParamsPanel;
import com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel;
import com.laker.postman.panel.collections.right.request.sub.ResponsePanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.BooleanSupplier;

final class RequestUiSetupHelper {
    private RequestUiSetupHelper() {
    }

    static void bindUrlField(JTextField urlField,
                             RequestPreparationFeedbackHelper feedbackHelper,
                             Runnable detectAndParseCurl,
                             Runnable parseUrlParamsToParamsPanel,
                             Runnable autoPrependProtocolAction) {
        urlField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                feedbackHelper.clearUrlValidationFeedback(urlField);
                detectAndParseCurl.run();
                parseUrlParamsToParamsPanel.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                feedbackHelper.clearUrlValidationFeedback(urlField);
                parseUrlParamsToParamsPanel.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                feedbackHelper.clearUrlValidationFeedback(urlField);
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
                                       EasyRequestParamsPanel paramsPanel,
                                       AuthTabPanel authTabPanel,
                                       ActionListener wsSendAction) {
        if (protocol.isWebSocketProtocol()) {
            requestBodyPanel.setWsSendActionListener(wsSendAction);
            reqTabs.setSelectedComponent(requestBodyPanel);
            reqTabs.remove(authTabPanel);
            requestBodyPanel.setWebSocketConnected(false);
        } else if (protocol.isSseProtocol()) {
            reqTabs.setSelectedComponent(paramsPanel);
            reqTabs.remove(authTabPanel);
        } else {
            reqTabs.setSelectedComponent(paramsPanel);
        }
    }

    static void bindSaveResponseButton(RequestItemProtocolEnum protocol,
                                       RequestEditSubPanelType panelType,
                                       ResponsePanel responsePanel,
                                       ActionListener saveAction) {
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
