package com.laker.postman.panel.workspace.components;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.model.Workspace;
import com.laker.postman.model.WorkspaceType;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * 自定义列表单元格渲染器
 */
public class WorkspaceListCellRenderer extends DefaultListCellRenderer {
    private static final String HTML_START = "<html>";
    private static final String HTML_END = "</html>";

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof Workspace workspace) {
            configureWorkspaceIcon(workspace);
            configureWorkspaceText(workspace);
            configureWorkspaceStyle();
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        }
        return this;
    }

    private void configureWorkspaceIcon(Workspace workspace) {
        FlatSVGIcon icon;
        if (workspace.getType() == WorkspaceType.LOCAL) {
            icon = IconUtil.createThemed("icons/local.svg", 18, 18);
        } else {
            // 如果 Git 工作区没有配置远程 URL，使用橙色警告图标
            if (workspace.getGitRemoteUrl() == null || workspace.getGitRemoteUrl().trim().isEmpty()) {
                icon = IconUtil.create("icons/git-warning.svg", 20, 20);
            } else {
                icon = IconUtil.create("icons/git.svg", 20, 20);
            }
        }
        setIcon(icon);
    }

    private void configureWorkspaceText(Workspace workspace) {
        StringBuilder text = new StringBuilder();
        text.append(HTML_START);

        Workspace current = WorkspaceService.getInstance().getCurrentWorkspace();
        boolean isCurrent = current != null && current.getId().equals(workspace.getId());

        // 如果是当前工作区，设置文字颜色为主题色
        if (isCurrent) {
            text.append("<b style='color: #0078d4;'>").append(workspace.getName()).append("</b>");
        } else {
            text.append("<b>").append(workspace.getName()).append("</b>");
        }

        text.append("<br>");

        // 描述部分也根据是否为当前工作区设置不同颜色
        if (isCurrent) {
            text.append("<small style='color: #0078d4;'>");
        } else {
            text.append("<small style='color: gray;'>");
        }

        text.append(workspace.getType() == WorkspaceType.LOCAL ?
                I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE_LOCAL) : I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE_GIT));

        // 显示描述信息（警告状态已通过橙色图标表示）
        if (workspace.getDescription() != null && !workspace.getDescription().trim().isEmpty()) {
            text.append(" - ").append(workspace.getDescription());
        }

        text.append("</small>");
        text.append(HTML_END);

        setText(text.toString());
    }

    private void configureWorkspaceStyle() {
        setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        setIconTextGap(5); // 图标和文字间距
    }

    /**
     * 动态计算单元格高度
     * 基于字体大小和内容（两行文字：名称+描述）
     */
    public static int calculateCellHeight() {
        Font font = FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1);
        FontMetrics metrics = new JLabel().getFontMetrics(font);

        // 两行文字的高度 + 上下内边距 + 行间距
        int lineHeight = metrics.getHeight();
        int twoLinesHeight = lineHeight * 2;
        int verticalPadding = 10; // 上下各5像素
        int lineSpacing = 4; // 行间距

        return twoLinesHeight + verticalPadding + lineSpacing;
    }
}