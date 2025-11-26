package com.laker.postman.common.component.tree;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
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

    private static final int ICON_SIZE = 16;
    private static final int METHOD_FONT_PX = 8;
    private static final int NAME_FONT_PX = 9;

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();
        if (userObject instanceof Object[] obj) {
            if (RequestCollectionsLeftPanel.GROUP.equals(obj[0])) {
                setIcon(new FlatSVGIcon("icons/group.svg", ICON_SIZE, ICON_SIZE));
                Object groupData = obj[1];
                String groupName = groupData instanceof RequestGroup requestGroup ? requestGroup.getName() : String.valueOf(groupData);
                setText(groupName);
            } else if (RequestCollectionsLeftPanel.REQUEST.equals(obj[0])) {
                HttpRequestItem item = (HttpRequestItem) obj[1];
                applyRequestRendering(item);
            }
        }

        return this;
    }

    // Extracted to reduce cognitive complexity of the main method
    private void applyRequestRendering(HttpRequestItem item) {
        String method = item.getMethod();
        String name = item.getName();
        RequestItemProtocolEnum protocol = item.getProtocol();
        String methodColor = HttpUtil.getMethodColor(method);

        if (protocol.isWebSocketProtocol()) {
            method = "WS";
            methodColor = "#29cea5";
        } else if (protocol.isSseProtocol()) {
            method = "SSE";
            methodColor = "#7fbee3";
        }

        setText(buildStyledText(method, methodColor, name));
    }

    // Build HTML with escaped content and consistent font sizes
    private static String buildStyledText(String method, String methodColor, String name) {
        String safeMethod = method == null ? "" : escapeHtml(method);
        String safeName = name == null ? "" : escapeHtml(name);
        String color = methodColor == null ? "#000" : methodColor;
        // simple concatenation is clearer for this short html fragment
        return "<html>" +
                "<span style='color:" + color + ";font-weight:bold;font-size:" + METHOD_FONT_PX + "px'>" +
                safeMethod +
                "</span> " +
                "<span style='font-size:" + NAME_FONT_PX + "px'>" +
                safeName +
                "</span></html>";
    }

    // Minimal HTML escape to avoid broken rendering or injection
    private static String escapeHtml(String s) {
        if (s == null) return null;
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}