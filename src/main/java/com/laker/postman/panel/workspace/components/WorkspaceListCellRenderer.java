package com.laker.postman.panel.workspace.components;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.Workspace;
import com.laker.postman.model.WorkspaceType;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
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

        // 优化选中效果：使用表格选中背景色，文字保持深色，无边框
        if (isSelected) {
            setBackground(ModernColors.TABLE_SELECTION_BACKGROUND);
            setForeground(ModernColors.TEXT_PRIMARY);
        } else {
            setBackground(list.getBackground());
            setForeground(ModernColors.TEXT_PRIMARY);
        }
        return this;
    }

    private void configureWorkspaceIcon(Workspace workspace) {
        FlatSVGIcon icon;
        if (workspace.getType() == WorkspaceType.LOCAL) {
            icon = new FlatSVGIcon("icons/local.svg", 18, 18);
        } else {
            icon = new FlatSVGIcon("icons/git.svg", 20, 20);
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
        if (workspace.getDescription() != null && !workspace.getDescription().trim().isEmpty()) {
            text.append(" - ").append(workspace.getDescription());
        }
        text.append("</small>");
        text.append(HTML_END);

        setText(text.toString());
    }

    private void configureWorkspaceStyle() {
        setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
    }
}