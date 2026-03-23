package com.laker.postman.panel.collections.right.request;

import com.laker.postman.common.component.MarkdownEditorPanel;
import com.laker.postman.common.component.tab.IndicatorTabComponent;
import com.laker.postman.common.component.table.FormDataTablePanel;
import com.laker.postman.common.component.table.FormUrlencodedTablePanel;
import com.laker.postman.model.HttpFormData;
import com.laker.postman.model.HttpFormUrlencoded;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpParam;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.right.request.sub.AuthTabPanel;
import com.laker.postman.panel.collections.right.request.sub.EasyRequestHttpHeadersPanel;
import com.laker.postman.panel.collections.right.request.sub.EasyRequestParamsPanel;
import com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel;
import com.laker.postman.panel.collections.right.request.sub.RequestSettingsPanel;
import com.laker.postman.panel.collections.right.request.sub.ScriptPanel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
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
            if (paramsTabIndicator != null) {
                paramsTabIndicator.setShowIndicator(hasParamsContent());
            }
            if (authTabIndicator != null) {
                authTabIndicator.setShowIndicator(hasAuthContent());
            }
            if (headersTabIndicator != null) {
                headersTabIndicator.setShowIndicator(hasHeadersContent());
            }
            if (bodyTabIndicator != null) {
                bodyTabIndicator.setShowIndicator(hasBodyContent());
            }
            if (settingsTabIndicator != null) {
                settingsTabIndicator.setShowIndicator(hasSettingsContent());
            }
            if (scriptsTabIndicator != null) {
                scriptsTabIndicator.setShowIndicator(hasScriptsContent());
            }
        });
    }

    private boolean hasParamsContent() {
        List<HttpParam> params = paramsPanel.getParamsListFromModel();
        return params != null && params.stream().anyMatch(param ->
                param.getKey() != null && !param.getKey().trim().isEmpty()
        );
    }

    private boolean hasAuthContent() {
        String authType = authTabPanel.getAuthType();
        return authType != null
                && !AuthTabPanel.AUTH_TYPE_INHERIT.equals(authType)
                && !AuthTabPanel.AUTH_TYPE_NONE.equals(authType);
    }

    private boolean hasHeadersContent() {
        List<HttpHeader> headers = headersPanel.getHeadersListFromModel();
        return headers != null && headers.stream().anyMatch(header ->
                header.getKey() != null && !header.getKey().trim().isEmpty()
        );
    }

    private boolean hasBodyContent() {
        if (!protocol.isHttpProtocol()) {
            return false;
        }

        String bodyType = requestBodyPanel.getBodyType();
        if (bodyType == null) {
            return false;
        }

        switch (bodyType) {
            case "none":
                return false;
            case "raw":
            case "binary":
                String rawBody = requestBodyPanel.getRawBody();
                return rawBody != null && !rawBody.trim().isEmpty();
            case "form-data":
                FormDataTablePanel formDataPanel = requestBodyPanel.getFormDataTablePanel();
                if (formDataPanel == null) {
                    return false;
                }
                List<HttpFormData> formDataItems = formDataPanel.getFormDataListFromModel();
                return formDataItems != null && formDataItems.stream().anyMatch(item ->
                        item.getKey() != null && !item.getKey().trim().isEmpty()
                );
            case "x-www-form-urlencoded":
                FormUrlencodedTablePanel urlencodedPanel = requestBodyPanel.getFormUrlencodedTablePanel();
                if (urlencodedPanel == null) {
                    return false;
                }
                List<HttpFormUrlencoded> urlencodedItems = urlencodedPanel.getFormDataListFromModel();
                return urlencodedItems != null && urlencodedItems.stream().anyMatch(item ->
                        item.getKey() != null && !item.getKey().trim().isEmpty()
                );
            default:
                return false;
        }
    }

    private boolean hasScriptsContent() {
        String prescript = scriptPanel.getPrescript();
        String postscript = scriptPanel.getPostscript();
        return (prescript != null && !prescript.trim().isEmpty())
                || (postscript != null && !postscript.trim().isEmpty());
    }

    private boolean hasSettingsContent() {
        return requestSettingsPanel.hasCustomSettings();
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
