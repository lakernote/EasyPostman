package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.common.component.table.FormDataTablePanel;
import com.laker.postman.common.component.table.FormUrlencodedTablePanel;
import com.laker.postman.request.edit.HttpRequestEditorContentSummary;
import com.laker.postman.request.edit.HttpRequestEditorDraft;
import com.laker.postman.request.edit.HttpRequestSettingsDraft;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
final class RequestTabStateController {
    private final RequestItemProtocolEnum protocol;
    private final RequestViewComponents view;

    void bindListeners(Runnable dirtyAction) {
        addDocumentListener(view.urlField.getDocument(), dirtyAction);
        view.methodBox.addActionListener(e -> dirtyAction.run());
        view.descriptionEditor.addDocumentListener(createDocumentListener(dirtyAction));
        view.headersPanel.addTableModelListener(e -> dirtyAction.run());
        view.pathVariablesPanel.addTableModelListener(e -> dirtyAction.run());
        view.paramsPanel.addTableModelListener(e -> dirtyAction.run());
        view.authTabPanel.addDirtyListener(dirtyAction);
        view.requestSettingsPanel.addDirtyListener(dirtyAction);

        if (protocol.isHttpProtocol()) {
            if (view.requestBodyPanel.getBodyArea() != null) {
                addDocumentListener(view.requestBodyPanel.getBodyArea().getDocument(), dirtyAction);
                view.requestBodyPanel.getBodyArea().getDocument().addDocumentListener(createDocumentListener(this::updateTabIndicators));
            }
            if (view.requestBodyPanel.getBinaryFilePathField() != null) {
                addDocumentListener(view.requestBodyPanel.getBinaryFilePathField().getDocument(), dirtyAction);
                view.requestBodyPanel.getBinaryFilePathField().getDocument().addDocumentListener(createDocumentListener(this::updateTabIndicators));
            }
            if (view.requestBodyPanel.getFormDataTablePanel() != null) {
                view.requestBodyPanel.getFormDataTablePanel().addTableModelListener(e -> dirtyAction.run());
                view.requestBodyPanel.getFormDataTablePanel().addTableModelListener(e -> updateTabIndicators());
            }
            if (view.requestBodyPanel.getFormUrlencodedTablePanel() != null) {
                view.requestBodyPanel.getFormUrlencodedTablePanel().addTableModelListener(e -> dirtyAction.run());
                view.requestBodyPanel.getFormUrlencodedTablePanel().addTableModelListener(e -> updateTabIndicators());
            }
        }

        view.scriptPanel.addDirtyListeners(dirtyAction);

        view.pathVariablesPanel.addTableModelListener(e -> updateTabIndicators());
        view.paramsPanel.addTableModelListener(e -> updateTabIndicators());
        view.authTabPanel.addDirtyListener(this::updateTabIndicators);
        view.headersPanel.addTableModelListener(e -> updateTabIndicators());
        view.requestSettingsPanel.addDirtyListener(this::updateTabIndicators);
        view.scriptPanel.addDirtyListeners(this::updateTabIndicators);
    }

    void updateTabIndicators() {
        SwingUtilities.invokeLater(() -> {
            HttpRequestEditorContentSummary summary =
                    HttpRequestEditorContentSummary.from(collectContentDraft());
            if (view.paramsTabIndicator != null) {
                view.paramsTabIndicator.setShowIndicator(summary.isHasParams());
            }
            if (view.authTabIndicator != null) {
                view.authTabIndicator.setShowIndicator(summary.isHasAuth());
            }
            if (view.headersTabIndicator != null) {
                view.headersTabIndicator.setShowIndicator(summary.isHasHeaders());
            }
            if (view.bodyTabIndicator != null) {
                view.bodyTabIndicator.setShowIndicator(summary.isHasBody());
            }
            if (view.settingsTabIndicator != null) {
                view.settingsTabIndicator.setShowIndicator(summary.isHasSettings());
            }
            if (view.scriptsTabIndicator != null) {
                view.scriptsTabIndicator.setShowIndicator(summary.isHasScripts());
            }
        });
    }

    private HttpRequestEditorDraft collectContentDraft() {
        HttpRequestSettingsDraft settings = view.requestSettingsPanel.collectSettings();
        return HttpRequestEditorDraft.builder()
                .protocol(protocol)
                .pathVariables(view.pathVariablesPanel.getParamsListFromModel())
                .params(view.paramsPanel.getParamsListFromModel())
                .headers(view.headersPanel.getHeadersListFromModel())
                .bodyType(view.requestBodyPanel.getBodyType())
                .body(readBody())
                .formData(readFormDataFromModel())
                .urlencoded(readUrlencodedFromModel())
                .authType(view.authTabPanel.getAuthType())
                .authUsername(view.authTabPanel.getUsername())
                .authPassword(view.authTabPanel.getPassword())
                .authToken(view.authTabPanel.getToken())
                .authApiKeyName(view.authTabPanel.getApiKeyName())
                .authApiKeyValue(view.authTabPanel.getApiKeyValue())
                .authApiKeyPlacement(view.authTabPanel.getApiKeyPlacement())
                .followRedirects(settings.getFollowRedirects())
                .cookieJarEnabled(settings.getCookieJarEnabled())
                .proxyPolicy(settings.getProxyPolicy())
                .httpVersion(settings.getHttpVersion())
                .requestTimeoutMs(settings.getRequestTimeoutMs())
                .prescript(view.scriptPanel.getPrescript())
                .postscript(view.scriptPanel.getPostscript())
                .build();
    }

    private List<HttpFormData> readFormDataFromModel() {
        FormDataTablePanel formDataPanel = view.requestBodyPanel.getFormDataTablePanel();
        return formDataPanel == null ? new ArrayList<>() : formDataPanel.getFormDataListFromModel();
    }

    private String readBody() {
        return view.requestBodyPanel.getBodyContent(view.requestBodyPanel.getBodyType());
    }

    private List<HttpFormUrlencoded> readUrlencodedFromModel() {
        FormUrlencodedTablePanel urlencodedPanel = view.requestBodyPanel.getFormUrlencodedTablePanel();
        return urlencodedPanel == null ? new ArrayList<>() : urlencodedPanel.getFormDataListFromModel();
    }

    private void addDocumentListener(Document document, Runnable action) {
        document.addDocumentListener(createDocumentListener(action));
    }

    private DocumentListener createDocumentListener(Runnable action) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        };
    }
}
