package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestLinePanel;
import com.laker.postman.panel.collections.editor.request.sub.ResponsePanel;
import com.laker.postman.service.collections.CollectionTreeNodes;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
final class SavedResponseHelper {

    String promptResponseName(Component owner) {
        String defaultName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        return (String) JOptionPane.showInputDialog(
                owner,
                I18nUtil.getMessage(MessageKeys.RESPONSE_SAVE_DIALOG_MESSAGE),
                I18nUtil.getMessage(MessageKeys.RESPONSE_SAVE_DIALOG_TITLE),
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                defaultName
        );
    }

    void saveResponse(String name,
                      PreparedRequest lastRequest,
                      HttpResponse lastResponse,
                      HttpRequestItem originalRequestItem) {
        try {
            SavedResponse savedResponse = fromRequestAndResponse(name, lastRequest, lastResponse);
            CollectionTreePanel leftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
            DefaultMutableTreeNode requestNode = findRequestNodeInTree(leftPanel.getRootTreeNode(), originalRequestItem);

            if (requestNode == null) {
                log.warn("无法找到请求节点，保存响应失败");
                NotificationUtil.showWarning(I18nUtil.getMessage("无法找到请求节点"));
                return;
            }

            HttpRequestItem treeRequestItem = CollectionTreeNodes.request(requestNode).orElse(null);
            if (treeRequestItem == null) {
                log.warn("请求节点数据异常，保存响应失败");
                NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.RESPONSE_SAVE_ERROR, ""));
                return;
            }
            if (treeRequestItem.getResponse() == null) {
                treeRequestItem.setResponse(new ArrayList<>());
            }
            treeRequestItem.getResponse().add(savedResponse);

            if (originalRequestItem.getResponse() == null) {
                originalRequestItem.setResponse(new ArrayList<>());
            }
            originalRequestItem.getResponse().add(savedResponse);

            requestNode.add(CollectionTreeNodes.savedResponseNode(savedResponse));

            leftPanel.getTreeModel().reload(requestNode);
            leftPanel.getRequestTree().expandPath(new TreePath(requestNode.getPath()));
            leftPanel.getPersistence().saveRequestGroups();

            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.RESPONSE_SAVE_SUCCESS, name));
        } catch (Exception ex) {
            log.error("保存响应失败", ex);
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.RESPONSE_SAVE_ERROR, ex.getMessage()));
        }
    }

    private static SavedResponse fromRequestAndResponse(String name, PreparedRequest request, HttpResponse response) {
        SavedResponse saved = new SavedResponse();
        saved.setId(UUID.randomUUID().toString());
        saved.setName(name);
        saved.setTimestamp(System.currentTimeMillis());

        SavedResponse.OriginalRequest originalRequest = new SavedResponse.OriginalRequest();
        originalRequest.setMethod(request.method);
        originalRequest.setUrl(request.url);
        originalRequest.setHeaders(request.headersList != null ? new ArrayList<>(request.headersList) : new ArrayList<>());
        originalRequest.setParams(request.paramsList != null ? new ArrayList<>(request.paramsList) : new ArrayList<>());
        originalRequest.setBodyType(request.bodyType);
        originalRequest.setBody(request.body);
        originalRequest.setFormDataList(request.formDataList != null ? new ArrayList<>(request.formDataList) : new ArrayList<>());
        originalRequest.setUrlencodedList(request.urlencodedList != null ? new ArrayList<>(request.urlencodedList) : new ArrayList<>());
        saved.setOriginalRequest(originalRequest);

        saved.setCode(response.code);
        saved.setHeaders(toSavedHeaders(response));
        saved.setBody(response.body);
        saved.setCostMs(response.costMs);
        saved.setBodySize(response.bodySize);
        saved.setHeadersSize(response.headersSize);
        saved.setPreviewLanguage(detectPreviewLanguage(response));

        return saved;
    }

    private static List<HttpHeader> toSavedHeaders(HttpResponse response) {
        List<HttpHeader> headers = new ArrayList<>();
        if (response.headers == null) {
            return headers;
        }
        for (Map.Entry<String, List<String>> entry : response.headers.entrySet()) {
            String key = entry.getKey();
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                String value = String.join(", ", entry.getValue());
                headers.add(new HttpHeader(true, key, value));
            }
        }
        return headers;
    }

    private static String detectPreviewLanguage(HttpResponse response) {
        if (response.headers != null) {
            for (Map.Entry<String, List<String>> entry : response.headers.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase("Content-Type")) {
                    List<String> values = entry.getValue();
                    if (values != null && !values.isEmpty()) {
                        String contentType = values.get(0).toLowerCase();
                        if (contentType.contains("json")) return "json";
                        if (contentType.contains("xml")) return "xml";
                        if (contentType.contains("html")) return "html";
                        if (contentType.contains("javascript")) return "javascript";
                        if (contentType.contains("css")) return "css";
                        if (contentType.contains("text")) return "text";
                    }
                }
            }
        }

        String body = response.body;
        if (body != null && !body.isEmpty()) {
            String trimmed = body.trim();
            if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                    (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
                return "json";
            }
            if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
                if (trimmed.toLowerCase().contains("<html")) return "html";
                if (trimmed.toLowerCase().contains("<?xml")) return "xml";
            }
        }

        return "text";
    }

    void displaySavedResponse(ResponsePanel responsePanel,
                              RequestLinePanel requestLinePanel,
                              ActionListener sendAction,
                              SavedResponse savedResponse) {
        HttpResponse response = toHttpResponse(savedResponse);
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

    private HttpResponse toHttpResponse(SavedResponse savedResponse) {
        HttpResponse response = new HttpResponse();
        response.code = savedResponse.getCode();
        response.body = savedResponse.getBody();
        response.headers = new LinkedHashMap<>();
        List<HttpHeader> headers = savedResponse.getHeaders();
        if (headers != null) {
            for (HttpHeader header : headers) {
                response.headers.put(header.getKey(), List.of(header.getValue()));
            }
        }
        response.costMs = savedResponse.getCostMs();
        response.bodySize = savedResponse.getBodySize();
        response.headersSize = savedResponse.getHeadersSize();
        return response;
    }

    private DefaultMutableTreeNode findRequestNodeInTree(DefaultMutableTreeNode node, HttpRequestItem item) {
        if (node == null || item == null || item.getId() == null) {
            return null;
        }

        HttpRequestItem nodeItem = CollectionTreeNodes.request(node).orElse(null);
        if (nodeItem != null) {
            if (nodeItem != null && item.getId().equals(nodeItem.getId())) {
                return node;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            DefaultMutableTreeNode result = findRequestNodeInTree(child, item);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
