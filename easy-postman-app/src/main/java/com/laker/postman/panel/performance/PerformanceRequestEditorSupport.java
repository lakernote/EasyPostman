package com.laker.postman.panel.performance;

import com.laker.postman.performance.core.model.NodeType;


import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.panel.performance.model.PerformanceTreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.function.Function;

final class PerformanceRequestEditorSupport {

    private final JPanel requestEditorHost;
    private final Function<HttpRequestItem, RequestItemProtocolEnum> protocolResolver;
    private RequestEditSubPanel requestEditSubPanel;
    private RequestItemProtocolEnum currentRequestEditorProtocol = RequestItemProtocolEnum.HTTP;

    PerformanceRequestEditorSupport(RequestEditSubPanel requestEditSubPanel,
                                    JPanel requestEditorHost,
                                    Function<HttpRequestItem, RequestItemProtocolEnum> protocolResolver) {
        this.requestEditSubPanel = requestEditSubPanel;
        this.requestEditorHost = requestEditorHost;
        this.protocolResolver = protocolResolver;
    }

    RequestEditSubPanel getRequestEditSubPanel() {
        return requestEditSubPanel;
    }

    void switchRequestEditor(HttpRequestItem item) {
        RequestItemProtocolEnum protocol = protocolResolver.apply(item);
        if (requestEditSubPanel == null || requestEditorHost == null || protocol != currentRequestEditorProtocol) {
            if (requestEditorHost != null && requestEditSubPanel != null) {
                requestEditorHost.remove(requestEditSubPanel);
            }
            requestEditSubPanel = RequestEditSubPanel.performanceSnapshot("", protocol, false);
            currentRequestEditorProtocol = protocol;
            if (requestEditorHost != null) {
                requestEditorHost.add(requestEditSubPanel, BorderLayout.CENTER);
                requestEditorHost.revalidate();
                requestEditorHost.repaint();
            }
        }

        if (item != null) {
            requestEditSubPanel.initPanelData(item);
            // 压测页使用延迟初始化壳子降低首开成本，但用户选中请求节点后
            // 必须立即升级为真实编辑器，否则会一直停留在骨架屏。
            requestEditSubPanel.ensureEditorInitialized();
        }
    }

    void saveRequestNodeData(DefaultMutableTreeNode node,
                             java.util.function.BiConsumer<DefaultMutableTreeNode, PerformanceTreeNode> syncRequestStructureAction) {
        if (requestEditSubPanel == null || node == null) {
            return;
        }
        Object userObj = node.getUserObject();
        if (userObj instanceof PerformanceTreeNode nodeData && nodeData.type == NodeType.REQUEST) {
            nodeData.httpRequestItem = requestEditSubPanel.getCurrentRequest();
            syncRequestStructureAction.accept(node, nodeData);
        }
    }
}
