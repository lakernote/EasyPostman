package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.request.model.HttpRequestItem;


import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.component.MarkdownEditorPanel;
import com.laker.postman.common.component.table.FormDataTablePanel;
import com.laker.postman.common.component.table.FormUrlencodedTablePanel;
import com.laker.postman.request.edit.HttpRequestEditorDraft;
import com.laker.postman.request.edit.HttpRequestEditorDraftMapper;
import com.laker.postman.request.edit.HttpRequestSettingsDraft;
import com.laker.postman.panel.collections.editor.request.sub.AuthTabPanel;
import com.laker.postman.panel.collections.editor.request.sub.EasyRequestHttpHeadersPanel;
import com.laker.postman.panel.collections.editor.request.sub.EasyRequestParamsPanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestBodyPanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestSettingsPanel;
import com.laker.postman.panel.collections.editor.request.sub.ScriptPanel;
import com.laker.postman.util.XmlUtil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class RequestEditorBinder {
    private final JTextField urlField;
    private final JComboBox<String> methodBox;
    private final EasyRequestParamsPanel paramsPanel;
    private final EasyRequestHttpHeadersPanel headersPanel;
    private final RequestBodyPanel requestBodyPanel;
    private final RequestSettingsPanel requestSettingsPanel;
    private final AuthTabPanel authTabPanel;
    private final ScriptPanel scriptPanel;
    private final MarkdownEditorPanel descriptionEditor;

    RequestEditorBinder(JTextField urlField,
                        JComboBox<String> methodBox,
                        EasyRequestParamsPanel paramsPanel,
                        EasyRequestHttpHeadersPanel headersPanel,
                        RequestBodyPanel requestBodyPanel,
                        RequestSettingsPanel requestSettingsPanel,
                        AuthTabPanel authTabPanel,
                        ScriptPanel scriptPanel,
                        MarkdownEditorPanel descriptionEditor) {
        this.urlField = urlField;
        this.methodBox = methodBox;
        this.paramsPanel = paramsPanel;
        this.headersPanel = headersPanel;
        this.requestBodyPanel = requestBodyPanel;
        this.requestSettingsPanel = requestSettingsPanel;
        this.authTabPanel = authTabPanel;
        this.scriptPanel = scriptPanel;
        this.descriptionEditor = descriptionEditor;
    }

    void populate(HttpRequestItem item) {
        if (item == null) {
            return;
        }
        populate(HttpRequestEditorDraftMapper.fromRequestItem(item));
    }

    private void populate(HttpRequestEditorDraft draft) {
        String url = draft.getUrl();
        urlField.setText(url);
        urlField.setCaretPosition(0);

        if (draft.getParams() != null && !draft.getParams().isEmpty()) {
            paramsPanel.setParamsList(copyList(draft.getParams()));
        } else {
            paramsPanel.clear();
        }
        methodBox.setSelectedItem(draft.getMethod());

        if (draft.getHeaders() != null && !draft.getHeaders().isEmpty()) {
            headersPanel.setHeadersList(copyList(draft.getHeaders()));
        } else {
            headersPanel.setHeadersList(new ArrayList<>());
        }

        String body = draft.getBody();
        requestBodyPanel.getBodyArea().setText(body);
        String bodyType = normalizeBodyType(draft.getBodyType(), body);
        requestBodyPanel.getBodyTypeComboBox().setSelectedItem(bodyType);
        applyRawType(body);
        applyFormData(draft.getFormData());
        applyUrlencoded(draft.getUrlencoded());

        authTabPanel.setAuthType(draft.getAuthType());
        authTabPanel.setUsername(draft.getAuthUsername());
        authTabPanel.setPassword(draft.getAuthPassword());
        authTabPanel.setToken(draft.getAuthToken());
        requestSettingsPanel.populate(settingsFromDraft(draft));

        scriptPanel.setPrescript(draft.getPrescript() == null ? "" : draft.getPrescript());
        scriptPanel.setPostscript(draft.getPostscript() == null ? "" : draft.getPostscript());
        descriptionEditor.setText(draft.getDescription() == null ? "" : draft.getDescription());
    }

    void populateSavedResponseRequest(SavedResponse.OriginalRequest originalRequest) {
        if (originalRequest == null) {
            return;
        }
        populate(HttpRequestEditorDraftMapper.fromSavedResponseOriginalRequest(originalRequest));
    }

    HttpRequestEditorDraft collectCurrentDraft(String id,
                                               String name,
                                               RequestItemProtocolEnum currentProtocol,
                                               List<SavedResponse> responses,
                                               boolean fromModel) {
        // fromModel=true 用于脏检查场景，避免 stopCellEditing 打断用户正在编辑表格单元格。
        String bodyType = resolveCurrentBodyType();
        HttpRequestSettingsDraft settings = requestSettingsPanel.collectSettings();
        return HttpRequestEditorDraft.builder()
                .id(id)
                .name(name)
                .description(descriptionEditor.getText())
                .url(urlField.getText().trim())
                .method((String) methodBox.getSelectedItem())
                .protocol(currentProtocol)
                .headers(fromModel ? headersPanel.getHeadersListFromModel() : headersPanel.getHeadersList())
                .params(fromModel ? paramsPanel.getParamsListFromModel() : paramsPanel.getParamsList())
                .bodyType(bodyType)
                .body(readBodyText(bodyType))
                .formData(readFormDataList(bodyType, fromModel))
                .urlencoded(readUrlencodedList(bodyType, fromModel))
                .authType(authTabPanel.getAuthType())
                .authUsername(authTabPanel.getUsername())
                .authPassword(authTabPanel.getPassword())
                .authToken(authTabPanel.getToken())
                .followRedirects(settings.getFollowRedirects())
                .cookieJarEnabled(settings.getCookieJarEnabled())
                .httpVersion(settings.getHttpVersion())
                .requestTimeoutMs(settings.getRequestTimeoutMs())
                .prescript(scriptPanel.getPrescript())
                .postscript(scriptPanel.getPostscript())
                .responses(responses)
                .build();
    }

    private HttpRequestSettingsDraft settingsFromDraft(HttpRequestEditorDraft draft) {
        return HttpRequestSettingsDraft.builder()
                .followRedirects(draft.getFollowRedirects())
                .cookieJarEnabled(draft.getCookieJarEnabled())
                .httpVersion(draft.getHttpVersion())
                .requestTimeoutMs(draft.getRequestTimeoutMs())
                .build();
    }

    private String readBodyText(String bodyType) {
        if (RequestBodyPanel.BODY_TYPE_RAW.equals(bodyType)) {
            return requestBodyPanel.getRawBody();
        }
        return requestBodyPanel.getBodyArea().getText().trim();
    }

    private List<HttpFormData> readFormDataList(String bodyType, boolean fromModel) {
        if (!RequestBodyPanel.BODY_TYPE_FORM_DATA.equals(bodyType)) {
            return new ArrayList<>();
        }
        FormDataTablePanel formDataTablePanel = requestBodyPanel.getFormDataTablePanel();
        return fromModel ? formDataTablePanel.getFormDataListFromModel() : formDataTablePanel.getFormDataList();
    }

    private List<HttpFormUrlencoded> readUrlencodedList(String bodyType, boolean fromModel) {
        if (!RequestBodyPanel.BODY_TYPE_FORM_URLENCODED.equals(bodyType)) {
            return new ArrayList<>();
        }
        FormUrlencodedTablePanel urlencodedTablePanel = requestBodyPanel.getFormUrlencodedTablePanel();
        return fromModel ? urlencodedTablePanel.getFormDataListFromModel() : urlencodedTablePanel.getFormDataList();
    }

    private void applyRawType(String body) {
        JComboBox<String> rawTypeComboBox = requestBodyPanel.getRawTypeComboBox();
        if (rawTypeComboBox == null) {
            return;
        }
        if (CharSequenceUtil.isBlank(body)) {
            rawTypeComboBox.setSelectedItem(RequestBodyPanel.RAW_TYPE_TEXT);
            return;
        }
        if (JSONUtil.isTypeJSON(body)) {
            rawTypeComboBox.setSelectedItem(RequestBodyPanel.RAW_TYPE_JSON);
        } else if (XmlUtil.isXml(body)) {
            rawTypeComboBox.setSelectedItem(RequestBodyPanel.RAW_TYPE_XML);
        } else {
            rawTypeComboBox.setSelectedItem(RequestBodyPanel.RAW_TYPE_TEXT);
        }
    }

    private void applyFormData(List<HttpFormData> formDataList) {
        FormDataTablePanel formDataTablePanel = requestBodyPanel.getFormDataTablePanel();
        if (formDataTablePanel == null) {
            return;
        }
        formDataTablePanel.setFormDataList(copyList(formDataList));
    }

    private void applyUrlencoded(List<HttpFormUrlencoded> urlencodedList) {
        FormUrlencodedTablePanel formUrlencodedTablePanel = requestBodyPanel.getFormUrlencodedTablePanel();
        if (formUrlencodedTablePanel == null) {
            return;
        }
        formUrlencodedTablePanel.setFormDataList(copyList(urlencodedList));
    }

    private String resolveCurrentBodyType() {
        String selectedBodyType = Objects.requireNonNull(requestBodyPanel.getBodyTypeComboBox().getSelectedItem()).toString();
        return normalizeBodyType(selectedBodyType, requestBodyPanel.getRawBody());
    }

    private String normalizeBodyType(String requestedBodyType, String body) {
        if (isSupportedBodyType(requestedBodyType)) {
            return requestedBodyType;
        }
        if (CharSequenceUtil.isNotBlank(body) && isSupportedBodyType(RequestBodyPanel.BODY_TYPE_RAW)) {
            return RequestBodyPanel.BODY_TYPE_RAW;
        }
        if (isSupportedBodyType(RequestBodyPanel.BODY_TYPE_NONE)) {
            return RequestBodyPanel.BODY_TYPE_NONE;
        }
        if (isSupportedBodyType(RequestBodyPanel.BODY_TYPE_RAW)) {
            return RequestBodyPanel.BODY_TYPE_RAW;
        }
        JComboBox<String> bodyTypeComboBox = requestBodyPanel.getBodyTypeComboBox();
        if (bodyTypeComboBox.getItemCount() > 0) {
            return bodyTypeComboBox.getItemAt(0);
        }
        return RequestBodyPanel.BODY_TYPE_NONE;
    }

    private boolean isSupportedBodyType(String bodyType) {
        if (CharSequenceUtil.isBlank(bodyType)) {
            return false;
        }
        JComboBox<String> bodyTypeComboBox = requestBodyPanel.getBodyTypeComboBox();
        for (int i = 0; i < bodyTypeComboBox.getItemCount(); i++) {
            if (bodyType.equals(bodyTypeComboBox.getItemAt(i))) {
                return true;
            }
        }
        return false;
    }

    private <T> List<T> copyList(List<T> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }
}
