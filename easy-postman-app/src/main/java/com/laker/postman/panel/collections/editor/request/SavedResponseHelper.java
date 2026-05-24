package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.SavedResponse;
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
            SavedResponse savedResponse = SavedResponse.fromRequestAndResponse(name, lastRequest, lastResponse);
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
