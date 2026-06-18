package com.laker.postman.panel.collections.editor.request;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.component.table.FormDataTablePanel;
import com.laker.postman.common.component.table.FormUrlencodedTablePanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestBodyPanel;
import com.laker.postman.request.edit.HttpRequestEditorDraft;
import com.laker.postman.request.edit.HttpRequestEditorDraftMapper;
import com.laker.postman.request.edit.HttpRequestSettingsDraft;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.util.XmlUtil;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
final class RequestEditorBinder {
    private final RequestViewComponents view;

    void populate(HttpRequestItem item) {
        if (item == null) {
            return;
        }
        populate(HttpRequestEditorDraftMapper.fromRequestItem(item));
    }

    private void populate(HttpRequestEditorDraft draft) {
        String url = draft.getUrl();
        view.urlField.setText(url);
        view.urlField.setCaretPosition(0);

        view.paramsTabPanel.setPathVariablesList(resolveEditablePathVariables(url, draft.getPathVariables()));
        if (draft.getParams() != null && !draft.getParams().isEmpty()) {
            view.paramsPanel.setParamsList(copyList(draft.getParams()));
        } else {
            view.paramsPanel.clear();
        }
        view.methodBox.setSelectedItem(draft.getMethod());

        if (draft.getHeaders() != null && !draft.getHeaders().isEmpty()) {
            view.headersPanel.setHeadersList(copyList(draft.getHeaders()));
        } else {
            view.headersPanel.setHeadersList(new ArrayList<>());
        }

        String body = draft.getBody();
        view.requestBodyPanel.getBodyArea().setText(body);
        String bodyType = normalizeBodyType(draft.getBodyType(), body);
        view.requestBodyPanel.getBodyTypeComboBox().setSelectedItem(bodyType);
        applyRawType(body);
        applyFormData(draft.getFormData());
        applyUrlencoded(draft.getUrlencoded());

        view.authTabPanel.setAuthType(draft.getAuthType());
        view.authTabPanel.setUsername(draft.getAuthUsername());
        view.authTabPanel.setPassword(draft.getAuthPassword());
        view.authTabPanel.setToken(draft.getAuthToken());
        view.authTabPanel.setApiKeyName(draft.getAuthApiKeyName());
        view.authTabPanel.setApiKeyValue(draft.getAuthApiKeyValue());
        view.authTabPanel.setApiKeyPlacement(draft.getAuthApiKeyPlacement());
        view.requestSettingsPanel.populate(settingsFromDraft(draft));

        view.scriptPanel.setPrescript(draft.getPrescript() == null ? "" : draft.getPrescript());
        view.scriptPanel.setPostscript(draft.getPostscript() == null ? "" : draft.getPostscript());
        view.descriptionEditor.setText(draft.getDescription() == null ? "" : draft.getDescription());
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
        HttpRequestSettingsDraft settings = view.requestSettingsPanel.collectSettings();
        return HttpRequestEditorDraft.builder()
                .id(id)
                .name(name)
                .description(view.descriptionEditor.getText())
                .url(view.urlField.getText().trim())
                .method((String) view.methodBox.getSelectedItem())
                .protocol(currentProtocol)
                .headers(fromModel ? view.headersPanel.getHeadersListFromModel() : view.headersPanel.getHeadersList())
                .pathVariables(fromModel ? view.paramsTabPanel.getPathVariablesListFromModel() : view.paramsTabPanel.getPathVariablesList())
                .params(fromModel ? view.paramsPanel.getParamsListFromModel() : view.paramsPanel.getParamsList())
                .bodyType(bodyType)
                .body(readBodyText(bodyType))
                .formData(readFormDataList(bodyType, fromModel))
                .urlencoded(readUrlencodedList(bodyType, fromModel))
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
                .responses(responses)
                .build();
    }

    private List<HttpParam> resolveEditablePathVariables(
            String url,
            List<HttpParam> pathVariables) {
        if (pathVariables != null && !pathVariables.isEmpty()) {
            return copyList(pathVariables);
        }
        return RequestUrlEditorSupport.mergePathVariablesFromUrl(url, new ArrayList<>());
    }

    private HttpRequestSettingsDraft settingsFromDraft(HttpRequestEditorDraft draft) {
        return HttpRequestSettingsDraft.builder()
                .followRedirects(draft.getFollowRedirects())
                .cookieJarEnabled(draft.getCookieJarEnabled())
                .proxyPolicy(draft.getProxyPolicy())
                .httpVersion(draft.getHttpVersion())
                .requestTimeoutMs(draft.getRequestTimeoutMs())
                .build();
    }

    private String readBodyText(String bodyType) {
        if (RequestBodyPanel.BODY_TYPE_RAW.equals(bodyType)) {
            return view.requestBodyPanel.getRawBody();
        }
        return view.requestBodyPanel.getBodyArea().getText().trim();
    }

    private List<HttpFormData> readFormDataList(String bodyType, boolean fromModel) {
        if (!RequestBodyPanel.BODY_TYPE_FORM_DATA.equals(bodyType)) {
            return new ArrayList<>();
        }
        FormDataTablePanel formDataTablePanel = view.requestBodyPanel.getFormDataTablePanel();
        return fromModel ? formDataTablePanel.getFormDataListFromModel() : formDataTablePanel.getFormDataList();
    }

    private List<HttpFormUrlencoded> readUrlencodedList(String bodyType, boolean fromModel) {
        if (!RequestBodyPanel.BODY_TYPE_FORM_URLENCODED.equals(bodyType)) {
            return new ArrayList<>();
        }
        FormUrlencodedTablePanel urlencodedTablePanel = view.requestBodyPanel.getFormUrlencodedTablePanel();
        return fromModel ? urlencodedTablePanel.getFormDataListFromModel() : urlencodedTablePanel.getFormDataList();
    }

    private void applyRawType(String body) {
        JComboBox<String> rawTypeComboBox = view.requestBodyPanel.getRawTypeComboBox();
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
        FormDataTablePanel formDataTablePanel = view.requestBodyPanel.getFormDataTablePanel();
        if (formDataTablePanel == null) {
            return;
        }
        formDataTablePanel.setFormDataList(copyList(formDataList));
    }

    private void applyUrlencoded(List<HttpFormUrlencoded> urlencodedList) {
        FormUrlencodedTablePanel formUrlencodedTablePanel = view.requestBodyPanel.getFormUrlencodedTablePanel();
        if (formUrlencodedTablePanel == null) {
            return;
        }
        formUrlencodedTablePanel.setFormDataList(copyList(urlencodedList));
    }

    private String resolveCurrentBodyType() {
        String selectedBodyType = Objects.requireNonNull(view.requestBodyPanel.getBodyTypeComboBox().getSelectedItem()).toString();
        return normalizeBodyType(selectedBodyType, view.requestBodyPanel.getRawBody());
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
        JComboBox<String> bodyTypeComboBox = view.requestBodyPanel.getBodyTypeComboBox();
        if (bodyTypeComboBox.getItemCount() > 0) {
            return bodyTypeComboBox.getItemAt(0);
        }
        return RequestBodyPanel.BODY_TYPE_NONE;
    }

    private boolean isSupportedBodyType(String bodyType) {
        if (CharSequenceUtil.isBlank(bodyType)) {
            return false;
        }
        JComboBox<String> bodyTypeComboBox = view.requestBodyPanel.getBodyTypeComboBox();
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
