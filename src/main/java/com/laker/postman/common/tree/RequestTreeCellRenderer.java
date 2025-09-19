package com.laker.postman.common.tree;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.service.http.HttpUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * 自定义树节点渲染器，用于美化 JTree 的节点显示
 */
public class RequestTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();
        if (userObject instanceof Object[] obj) {
            if (RequestCollectionsLeftPanel.GROUP.equals(obj[0])) {
                setIcon(new FlatSVGIcon("icons/group.svg", 16, 16));
                setText((String) obj[1]);
            } else if (RequestCollectionsLeftPanel.REQUEST.equals(obj[0])) {
                // 直接用彩色文本显示 method + name，method 彩色，name 默认色
                HttpRequestItem item = (HttpRequestItem) obj[1];
                String method = item.getMethod();
                String name = item.getName();
                RequestItemProtocolEnum protocol = item.getProtocol();
                String methodColor = HttpUtil.getMethodColor(method);

                // 根据协议类型设置不同的图标和显示样式
                if (protocol.isWebSocketProtocol()) {
                    setIcon(new FlatSVGIcon("icons/websocket.svg", 16, 16));
                    setText("<html><span style='color:#1976D2;font-weight:bold;font-size:7px'>WS</span> <span style='font-size:8.5px'>" + name + "</span></html>");
                } else {
                    if (protocol.isSseProtocol()) {
                        setIcon(new FlatSVGIcon("icons/sse.svg", 16, 16));
                    } else {
                        setIcon(new FlatSVGIcon("icons/http.svg", 16, 16));
                    }
                    setText("<html><span style='color:" + methodColor + ";font-weight:bold;font-size:7px'>" +
                            (method == null ? "" : method) + "</span> <span style='font-size:8.5px'>" + name + "</span></html>");
                }
            }
        }
        if (sel) setBackgroundSelectionColor(new Color(255, 230, 180));
        else setBackgroundNonSelectionColor(getBackground());
        return this;
    }
}