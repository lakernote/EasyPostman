package com.laker.postman.panel.collections.right.request;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.component.MarkdownEditorPanel;
import com.laker.postman.common.component.table.FormDataTablePanel;
import com.laker.postman.common.component.table.FormUrlencodedTablePanel;
import com.laker.postman.model.HttpFormData;
import com.laker.postman.model.HttpFormUrlencoded;
import com.laker.postman.model.HttpParam;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.model.SavedResponse;
import com.laker.postman.panel.collections.right.request.sub.AuthTabPanel;
import com.laker.postman.panel.collections.right.request.sub.EasyRequestHttpHeadersPanel;
import com.laker.postman.panel.collections.right.request.sub.EasyRequestParamsPanel;
import com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel;
import com.laker.postman.panel.collections.right.request.sub.RequestSettingsPanel;
import com.laker.postman.panel.collections.right.request.sub.ScriptPanel;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.util.XmlUtil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class RequestFormDataHelper {
    private final JTextField urlField;
    private final JComboBox<String> methodBox;
    private final EasyRequestParamsPanel paramsPanel;
    private final EasyRequestHttpHeadersPanel headersPanel;
    private final RequestBodyPanel requestBodyPanel;
    private final RequestSettingsPanel requestSettingsPanel;
    private final AuthTabPanel authTabPanel;
    private final ScriptPanel scriptPanel;
    private final MarkdownEditorPanel descriptionEditor;
    private final JTabbedPane reqTabs;

    RequestFormDataHelper(JTextField urlField,
                          JComboBox<String> methodBox,
                          EasyRequestParamsPanel paramsPanel,
                          EasyRequestHttpHeadersPanel headersPanel,
                          RequestBodyPanel requestBodyPanel,
                          RequestSettingsPanel requestSettingsPanel,
                          AuthTabPanel authTabPanel,
                          ScriptPanel scriptPanel,
                          MarkdownEditorPanel descriptionEditor,
                          JTabbedPane reqTabs) {
        this.urlField = urlField;
        this.methodBox = methodBox;
        this.paramsPanel = paramsPanel;
        this.headersPanel = headersPanel;
        this.requestBodyPanel = requestBodyPanel;
        this.requestSettingsPanel = requestSettingsPanel;
        this.authTabPanel = authTabPanel;
        this.scriptPanel = scriptPanel;
        this.descriptionEditor = descriptionEditor;
        this.reqTabs = reqTabs;
    }

    void populate(HttpRequestItem item) {
        if (item == null) {
            return;
        }

        String url = item.getUrl();
        urlField.setText(url);
        urlField.setCaretPosition(0);

        if (CollUtil.isNotEmpty(item.getParamsList())) {
            paramsPanel.setParamsList(item.getParamsList());
        } else {
            List<HttpParam> urlParams = HttpUtil.getParamsListFromUrl(url);
            if (!urlParams.isEmpty()) {
                paramsPanel.setParamsList(urlParams);
                item.setParamsList(paramsPanel.getParamsList());
            } else {
                paramsPanel.clear();
            }
        }
        methodBox.setSelectedItem(item.getMethod());

        if (CollUtil.isNotEmpty(item.getHeadersList())) {
            headersPanel.setHeadersList(item.getHeadersList());
        } else {
            headersPanel.setHeadersList(new ArrayList<>());
        }
        item.setHeadersList(headersPanel.getHeadersList());

        requestBodyPanel.getBodyArea().setText(item.getBody());
        String bodyType = normalizeBodyType(resolveBodyType(item), item.getBody());
        item.setBodyType(bodyType);
        requestBodyPanel.getBodyTypeComboBox().setSelectedItem(bodyType);
        applyRawType(item.getBody());
        applyFormData(item.getFormDataList());
        applyUrlencoded(item.getUrlencodedList());

        authTabPanel.setAuthType(item.getAuthType());
        authTabPanel.setUsername(item.getAuthUsername());
        authTabPanel.setPassword(item.getAuthPassword());
        authTabPanel.setToken(item.getAuthToken());
        requestSettingsPanel.populate(item);

        scriptPanel.setPrescript(item.getPrescript() == null ? "" : item.getPrescript());
        scriptPanel.setPostscript(item.getPostscript() == null ? "" : item.getPostscript());
        descriptionEditor.setText(item.getDescription() == null ? "" : item.getDescription());
    }

    void populateSavedResponseRequest(SavedResponse.OriginalRequest originalRequest) {
        if (originalRequest == null) {
            return;
        }

        urlField.setText(HttpUtil.decodeUrlQueryForDisplay(originalRequest.getUrl()));
        urlField.setCaretPosition(0);
        methodBox.setSelectedItem(originalRequest.getMethod());
        paramsPanel.setParamsList(copyList(originalRequest.getParams()));
        headersPanel.setHeadersList(copyList(originalRequest.getHeaders()));

        String requestedBodyType = CharSequenceUtil.isBlank(originalRequest.getBodyType())
                ? RequestBodyPanel.BODY_TYPE_NONE
                : originalRequest.getBodyType();
        String bodyType = normalizeBodyType(requestedBodyType, originalRequest.getBody());
        requestBodyPanel.getBodyTypeComboBox().setSelectedItem(bodyType);

        String body = originalRequest.getBody();
        requestBodyPanel.getBodyArea().setText(body);
        applyRawType(body);
        applyFormData(originalRequest.getFormDataList());
        applyUrlencoded(originalRequest.getUrlencodedList());

        authTabPanel.setAuthType(null);
        authTabPanel.setUsername("");
        authTabPanel.setPassword("");
        authTabPanel.setToken("");
        requestSettingsPanel.populate(null);
        scriptPanel.setPrescript("");
        scriptPanel.setPostscript("");
        descriptionEditor.setText("");
    }

    HttpRequestItem buildCurrentRequest(String id,
                                        String name,
                                        RequestItemProtocolEnum currentProtocol,
                                        HttpRequestItem originalRequestItem,
                                        boolean fromModel) {
        // fromModel=true 用于脏检查场景，避免 stopCellEditing 打断用户正在编辑表格单元格。
        HttpRequestItem item = new HttpRequestItem();
        item.setId(id);
        item.setName(name);
        item.setDescription(descriptionEditor.getText());
        item.setUrl(urlField.getText().trim());
        item.setMethod((String) methodBox.getSelectedItem());
        item.setProtocol(currentProtocol);
        item.setHeadersList(fromModel ? headersPanel.getHeadersListFromModel() : headersPanel.getHeadersList());
        item.setParamsList(fromModel ? paramsPanel.getParamsListFromModel() : paramsPanel.getParamsList());
        item.setBody(requestBodyPanel.getBodyArea().getText().trim());
        item.setBodyType(resolveCurrentBodyType());

        String bodyType = item.getBodyType();
        if (RequestBodyPanel.BODY_TYPE_FORM_DATA.equals(bodyType)) {
            FormDataTablePanel formDataTablePanel = requestBodyPanel.getFormDataTablePanel();
            item.setFormDataList(fromModel ? formDataTablePanel.getFormDataListFromModel() : formDataTablePanel.getFormDataList());
            item.setBody("");
            item.setUrlencodedList(new ArrayList<>());
        } else if (RequestBodyPanel.BODY_TYPE_FORM_URLENCODED.equals(bodyType)) {
            FormUrlencodedTablePanel urlencodedTablePanel = requestBodyPanel.getFormUrlencodedTablePanel();
            item.setBody("");
            item.setFormDataList(new ArrayList<>());
            item.setUrlencodedList(fromModel ? urlencodedTablePanel.getFormDataListFromModel() : urlencodedTablePanel.getFormDataList());
        } else if (RequestBodyPanel.BODY_TYPE_RAW.equals(bodyType)) {
            item.setBody(requestBodyPanel.getRawBody());
            item.setFormDataList(new ArrayList<>());
            item.setUrlencodedList(new ArrayList<>());
        }

        item.setAuthType(authTabPanel.getAuthType());
        item.setAuthUsername(authTabPanel.getUsername());
        item.setAuthPassword(authTabPanel.getPassword());
        item.setAuthToken(authTabPanel.getToken());
        requestSettingsPanel.applyTo(item);
        item.setPrescript(scriptPanel.getPrescript());
        item.setPostscript(scriptPanel.getPostscript());

        if (originalRequestItem != null && originalRequestItem.getResponse() != null) {
            item.setResponse(originalRequestItem.getResponse());
        }
        return item;
    }

    void selectDefaultTabByRequestType(RequestItemProtocolEnum effectiveProtocol, HttpRequestItem item) {
        if (item == null) {
            return;
        }

        selectDefaultTab(
                effectiveProtocol,
                item.getMethod(),
                item.getBodyType(),
                item.getBody(),
                CollUtil.isNotEmpty(item.getFormDataList()) || CollUtil.isNotEmpty(item.getUrlencodedList()),
                CollUtil.isNotEmpty(item.getParamsList())
        );
    }

    void selectDefaultTabBySavedResponse(RequestItemProtocolEnum effectiveProtocol,
                                         SavedResponse.OriginalRequest originalRequest) {
        if (originalRequest == null) {
            return;
        }

        selectDefaultTab(
                effectiveProtocol,
                originalRequest.getMethod(),
                originalRequest.getBodyType(),
                originalRequest.getBody(),
                CollUtil.isNotEmpty(originalRequest.getFormDataList())
                        || CollUtil.isNotEmpty(originalRequest.getUrlencodedList()),
                CollUtil.isNotEmpty(originalRequest.getParams())
        );
    }

    private void selectDefaultTab(RequestItemProtocolEnum effectiveProtocol,
                                  String method,
                                  String bodyType,
                                  String body,
                                  boolean hasFormData,
                                  boolean hasParams) {
        if (effectiveProtocol != null && effectiveProtocol.isWebSocketProtocol()) {
            reqTabs.setSelectedComponent(requestBodyPanel);
            return;
        }
        if (effectiveProtocol != null && effectiveProtocol.isSseProtocol()) {
            reqTabs.setSelectedComponent(paramsPanel);
            return;
        }
        if (CharSequenceUtil.isNotBlank(body) && !RequestBodyPanel.BODY_TYPE_NONE.equals(bodyType)) {
            reqTabs.setSelectedComponent(requestBodyPanel);
            return;
        }
        if (hasFormData) {
            reqTabs.setSelectedComponent(requestBodyPanel);
            return;
        }
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            reqTabs.setSelectedComponent(hasParams ? paramsPanel : requestBodyPanel);
            return;
        }
        reqTabs.setSelectedComponent(paramsPanel);
    }

    private String resolveBodyType(HttpRequestItem item) {
        if (CharSequenceUtil.isNotBlank(item.getBodyType())) {
            return item.getBodyType();
        }

        item.setBodyType(RequestBodyPanel.BODY_TYPE_NONE);
        if (!supportsBody(item.getMethod())) {
            return item.getBodyType();
        }

        String contentType = HttpUtil.getHeaderIgnoreCase(item, "Content-Type");
        if (CharSequenceUtil.isBlank(contentType)) {
            return item.getBodyType();
        }
        if (contentType.contains("application/x-www-form-urlencoded")) {
            item.setBodyType(RequestBodyPanel.BODY_TYPE_FORM_URLENCODED);
        } else if (contentType.contains("multipart/form-data")) {
            item.setBodyType(RequestBodyPanel.BODY_TYPE_FORM_DATA);
        } else {
            item.setBodyType(RequestBodyPanel.BODY_TYPE_RAW);
        }
        return item.getBodyType();
    }

    private boolean supportsBody(String method) {
        return "POST".equals(method)
                || "PUT".equals(method)
                || "PATCH".equals(method)
                || "DELETE".equals(method);
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
