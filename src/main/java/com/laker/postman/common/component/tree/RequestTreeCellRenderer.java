package com.laker.postman.common.component.tree;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.model.SavedResponse;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.setting.SettingManager;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.text.SimpleDateFormat;

/**
 * 自定义树节点渲染器，用于美化 JTree 的节点显示
 */
public class RequestTreeCellRenderer extends DefaultTreeCellRenderer {

    private static final int ICON_SIZE = 16;

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
            } else if (RequestCollectionsLeftPanel.SAVED_RESPONSE.equals(obj[0])) {
                SavedResponse savedResponse = (SavedResponse) obj[1];
                applySavedResponseRendering(savedResponse);
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

    // Render saved response node with status code and timestamp
    private void applySavedResponseRendering(SavedResponse savedResponse) {
        setIcon(new FlatSVGIcon("icons/save-response.svg", 24, 24));

        String name = savedResponse.getName();
        int code = savedResponse.getCode();
        long timestamp = savedResponse.getTimestamp();

        // 格式化时间
        String timeStr = new SimpleDateFormat("MM-dd HH:mm").format(new java.util.Date(timestamp));

        // 根据状态码设置颜色
        String statusColor = getStatusColor(code);

        setText(buildSavedResponseText(name, code, timeStr, statusColor));
    }

    // Get status code color
    private static String getStatusColor(int code) {
        if (code >= 200 && code < 300) {
            return "#28a745"; // 绿色 - 成功
        } else if (code >= 300 && code < 400) {
            return "#17a2b8"; // 青色 - 重定向
        } else if (code >= 400 && code < 500) {
            return "#ffc107"; // 黄色 - 客户端错误
        } else if (code >= 500) {
            return "#dc3545"; // 红色 - 服务器错误
        }
        return "#6c757d"; // 灰色 - 其他
    }

    // Build styled text for saved response
    private static String buildSavedResponseText(String name, int code, String timeStr, String statusColor) {
        String safeName = name == null ? "" : escapeHtml(name);

        int baseFontSize = SettingManager.getUiFontSize();
        int nameFontSize = Math.max(8, baseFontSize - 4);
        int statusFontSize = Math.max(7, baseFontSize - 5);
        int timeFontSize = Math.max(7, baseFontSize - 6);

        return "<html>" +
                "<span style='font-size:" + nameFontSize + "px'>" + safeName + "</span> " +
                "<span style='color:" + statusColor + ";font-weight:bold;font-size:" + statusFontSize + "px'>" + code + "</span> " +
                "<span style='color:#999999;font-size:" + timeFontSize + "px'>" + timeStr + "</span>" +
                "</html>";
    }

    // Build HTML with escaped content and dynamic font sizes based on user settings
    private static String buildStyledText(String method, String methodColor, String name) {
        String safeMethod = method == null ? "" : escapeHtml(method);
        String safeName = name == null ? "" : escapeHtml(name);
        String color = methodColor == null ? "#000" : methodColor;

        // 获取用户设置的字体大小，并计算相对大小
        int baseFontSize = SettingManager.getUiFontSize();
        int methodFontSize = Math.max(7, baseFontSize - 5); // 方法名比标准字体小4号，最小8px
        int nameFontSize = Math.max(8, baseFontSize - 4);   // 请求名比标准字体小3号，最小9px

        // simple concatenation is clearer for this short html fragment
        return "<html>" +
                "<span style='color:" + color + ";font-weight:bold;font-size:" + methodFontSize + "px'>" +
                safeMethod +
                "</span> " +
                "<span style='font-size:" + nameFontSize + "px'>" +
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