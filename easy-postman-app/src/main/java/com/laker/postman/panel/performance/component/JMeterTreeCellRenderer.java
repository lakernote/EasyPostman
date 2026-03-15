package com.laker.postman.panel.performance.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.service.http.HttpUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class JMeterTreeCellRenderer extends DefaultTreeCellRenderer {

    public static final int SIZE = 18;

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObj = node.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode) {
            // 设置图标
            switch (jtNode.type) {
                case THREAD_GROUP -> label.setIcon(IconUtil.createThemed("icons/user-group.svg", SIZE, SIZE));
                case REQUEST -> label.setIcon(resolveRequestIcon(jtNode.httpRequestItem));
                case ASSERTION -> label.setIcon(IconUtil.createThemed("icons/warning.svg", SIZE, SIZE));
                case TIMER -> label.setIcon(IconUtil.createThemed("icons/time.svg", SIZE, SIZE));
                case SSE_CONNECT -> label.setIcon(IconUtil.createThemed("icons/connect.svg", SIZE, SIZE));
                case SSE_AWAIT -> label.setIcon(new FlatSVGIcon("icons/time.svg", SIZE, SIZE));
                case WS_CONNECT -> label.setIcon(IconUtil.createThemed("icons/connect.svg", SIZE, SIZE));
                case WS_SEND -> label.setIcon(IconUtil.createThemed("icons/ws-send.svg", SIZE, SIZE));
                case WS_AWAIT -> label.setIcon(new FlatSVGIcon("icons/time.svg", SIZE, SIZE));
                case WS_CLOSE -> label.setIcon(IconUtil.createThemed("icons/ws-close.svg", SIZE, SIZE));
                case ROOT -> label.setIcon(new FlatSVGIcon("icons/computer.svg", SIZE, SIZE));
            }

            String text = jtNode.name;
            if (!jtNode.enabled) {
                Color disabledColor = new Color(150, 150, 150);
                if (!sel) {
                    label.setForeground(disabledColor);
                }
                label.setFont(FontsUtil.getDefaultFont(Font.ITALIC));
                label.setText("<html><strike>" + text + "</strike></html>");
            } else {
                // 启用状态：恢复正常样式
                label.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
                label.setText(text);
            }
        }
        return label;
    }

    private Icon resolveRequestIcon(HttpRequestItem item) {
        RequestItemProtocolEnum protocol = item != null && item.getProtocol() != null
                ? item.getProtocol()
                : RequestItemProtocolEnum.HTTP;
        if (protocol.isSseProtocol() || (protocol.isHttpProtocol() && HttpUtil.isSSERequest(item))) {
            return new FlatSVGIcon("icons/sse.svg", SIZE, SIZE);
        }
        if (protocol.isWebSocketProtocol()) {
            return new FlatSVGIcon("icons/websocket.svg", SIZE, SIZE);
        }
        return new FlatSVGIcon("icons/http.svg", SIZE, SIZE);
    }
}
