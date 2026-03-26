package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;

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
            requestEditSubPanel = new RequestEditSubPanel("", protocol);
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
                             java.util.function.BiConsumer<DefaultMutableTreeNode, JMeterTreeNode> syncRequestStructureAction) {
        if (requestEditSubPanel == null || node == null) {
            return;
        }
        Object userObj = node.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST) {
            jtNode.httpRequestItem = requestEditSubPanel.getCurrentRequest();
            syncRequestStructureAction.accept(node, jtNode);
        }
    }
}
