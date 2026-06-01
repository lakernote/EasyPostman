package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.common.component.MarkdownEditorPanel;
import com.laker.postman.common.component.tab.IndicatorTabComponent;
import com.laker.postman.common.component.table.FormDataTablePanel;
import com.laker.postman.common.component.table.FormUrlencodedTablePanel;
import com.laker.postman.panel.collections.editor.request.sub.AuthTabPanel;
import com.laker.postman.panel.collections.editor.request.sub.EasyRequestHttpHeadersPanel;
import com.laker.postman.panel.collections.editor.request.sub.EasyRequestParamsPanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestBodyPanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestSettingsPanel;
import com.laker.postman.panel.collections.editor.request.sub.ScriptPanel;
import com.laker.postman.request.edit.HttpRequestEditorContentSummary;
import com.laker.postman.request.edit.HttpRequestEditorDraft;
import com.laker.postman.request.edit.HttpRequestSettingsDraft;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.model.RequestItemProtocolEnum;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.util.ArrayList;
import java.util.List;

final class RequestTabStateHelper {
    private final RequestItemProtocolEnum protocol;
    private final JTextField urlField;
    private final JComboBox<String> methodBox;
    private final MarkdownEditorPanel descriptionEditor;
    private final EasyRequestParamsPanel paramsPanel;
    private final AuthTabPanel authTabPanel;
    private final EasyRequestHttpHeadersPanel headersPanel;
    private final RequestBodyPanel requestBodyPanel;
    private final RequestSettingsPanel requestSettingsPanel;
    private final ScriptPanel scriptPanel;
    private final IndicatorTabComponent paramsTabIndicator;
    private final IndicatorTabComponent authTabIndicator;
    private final IndicatorTabComponent headersTabIndicator;
    private final IndicatorTabComponent bodyTabIndicator;
    private final IndicatorTabComponent settingsTabIndicator;
    private final IndicatorTabComponent scriptsTabIndicator;

    RequestTabStateHelper(RequestItemProtocolEnum protocol,
                          JTextField urlField,
                          JComboBox<String> methodBox,
                          MarkdownEditorPanel descriptionEditor,
                          EasyRequestParamsPanel paramsPanel,
                          AuthTabPanel authTabPanel,
                          EasyRequestHttpHeadersPanel headersPanel,
                          RequestBodyPanel requestBodyPanel,
                          RequestSettingsPanel requestSettingsPanel,
                          ScriptPanel scriptPanel,
                          IndicatorTabComponent paramsTabIndicator,
                          IndicatorTabComponent authTabIndicator,
                          IndicatorTabComponent headersTabIndicator,
                          IndicatorTabComponent bodyTabIndicator,
                          IndicatorTabComponent settingsTabIndicator,
                          IndicatorTabComponent scriptsTabIndicator) {
        this.protocol = protocol;
        this.urlField = urlField;
        this.methodBox = methodBox;
        this.descriptionEditor = descriptionEditor;
        this.paramsPanel = paramsPanel;
        this.authTabPanel = authTabPanel;
        this.headersPanel = headersPanel;
        this.requestBodyPanel = requestBodyPanel;
        this.requestSettingsPanel = requestSettingsPanel;
        this.scriptPanel = scriptPanel;
        this.paramsTabIndicator = paramsTabIndicator;
        this.authTabIndicator = authTabIndicator;
        this.headersTabIndicator = headersTabIndicator;
        this.bodyTabIndicator = bodyTabIndicator;
        this.settingsTabIndicator = settingsTabIndicator;
        this.scriptsTabIndicator = scriptsTabIndicator;
    }

    void bindListeners(Runnable dirtyAction) {
        addDocumentListener(urlField.getDocument(), dirtyAction);
        methodBox.addActionListener(e -> dirtyAction.run());
        descriptionEditor.addDocumentListener(createDocumentListener(dirtyAction));
        headersPanel.addTableModelListener(e -> dirtyAction.run());
        paramsPanel.addTableModelListener(e -> dirtyAction.run());
        authTabPanel.addDirtyListener(dirtyAction);
        requestSettingsPanel.addDirtyListener(dirtyAction);

        if (protocol.isHttpProtocol()) {
            if (requestBodyPanel.getBodyArea() != null) {
                addDocumentListener(requestBodyPanel.getBodyArea().getDocument(), dirtyAction);
                requestBodyPanel.getBodyArea().getDocument().addDocumentListener(createDocumentListener(this::updateTabIndicators));
            }
            if (requestBodyPanel.getFormDataTablePanel() != null) {
                requestBodyPanel.getFormDataTablePanel().addTableModelListener(e -> dirtyAction.run());
                requestBodyPanel.getFormDataTablePanel().addTableModelListener(e -> updateTabIndicators());
            }
            if (requestBodyPanel.getFormUrlencodedTablePanel() != null) {
                requestBodyPanel.getFormUrlencodedTablePanel().addTableModelListener(e -> dirtyAction.run());
                requestBodyPanel.getFormUrlencodedTablePanel().addTableModelListener(e -> updateTabIndicators());
            }
        }

        scriptPanel.addDirtyListeners(dirtyAction);

        paramsPanel.addTableModelListener(e -> updateTabIndicators());
        authTabPanel.addDirtyListener(this::updateTabIndicators);
        headersPanel.addTableModelListener(e -> updateTabIndicators());
        requestSettingsPanel.addDirtyListener(this::updateTabIndicators);
        scriptPanel.addDirtyListeners(this::updateTabIndicators);
    }

    void updateTabIndicators() {
        SwingUtilities.invokeLater(() -> {
            HttpRequestEditorContentSummary summary =
                    HttpRequestEditorContentSummary.from(collectContentDraft());
            if (paramsTabIndicator != null) {
                paramsTabIndicator.setShowIndicator(summary.isHasParams());
            }
            if (authTabIndicator != null) {
                authTabIndicator.setShowIndicator(summary.isHasAuth());
            }
            if (headersTabIndicator != null) {
                headersTabIndicator.setShowIndicator(summary.isHasHeaders());
            }
            if (bodyTabIndicator != null) {
                bodyTabIndicator.setShowIndicator(summary.isHasBody());
            }
            if (settingsTabIndicator != null) {
                settingsTabIndicator.setShowIndicator(summary.isHasSettings());
            }
            if (scriptsTabIndicator != null) {
                scriptsTabIndicator.setShowIndicator(summary.isHasScripts());
            }
        });
    }

    private HttpRequestEditorDraft collectContentDraft() {
        HttpRequestSettingsDraft settings = requestSettingsPanel.collectSettings();
        return HttpRequestEditorDraft.builder()
                .protocol(protocol)
                .params(paramsPanel.getParamsListFromModel())
                .headers(headersPanel.getHeadersListFromModel())
                .bodyType(requestBodyPanel.getBodyType())
                .body(requestBodyPanel.getRawBody())
                .formData(readFormDataFromModel())
                .urlencoded(readUrlencodedFromModel())
                .authType(authTabPanel.getAuthType())
                .followRedirects(settings.getFollowRedirects())
                .cookieJarEnabled(settings.getCookieJarEnabled())
                .httpVersion(settings.getHttpVersion())
                .requestTimeoutMs(settings.getRequestTimeoutMs())
                .prescript(scriptPanel.getPrescript())
                .postscript(scriptPanel.getPostscript())
                .build();
    }

    private List<HttpFormData> readFormDataFromModel() {
        FormDataTablePanel formDataPanel = requestBodyPanel.getFormDataTablePanel();
        return formDataPanel == null ? new ArrayList<>() : formDataPanel.getFormDataListFromModel();
    }

    private List<HttpFormUrlencoded> readUrlencodedFromModel() {
        FormUrlencodedTablePanel urlencodedPanel = requestBodyPanel.getFormUrlencodedTablePanel();
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
