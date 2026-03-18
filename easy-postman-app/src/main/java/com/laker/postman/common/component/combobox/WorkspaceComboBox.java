package com.laker.postman.common.component.combobox;

import com.laker.postman.model.Workspace;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.util.FontsUtil;
import lombok.Setter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * 工作区下拉框组件
 */
public class WorkspaceComboBox extends JComboBox<Workspace> {
    private boolean isUpdating = false;
    @Setter
    private Consumer<Workspace> onWorkspaceChange;

    public WorkspaceComboBox() {
        setRenderer(new WorkspaceItemRenderer());
        setPreferredSize(new Dimension(120, 28));
        setMaximumSize(new Dimension(120, 28));
        setFont(FontsUtil.getDefaultFont(Font.PLAIN)); // 使用用户设置的字体大小
        setFocusable(false);
        // 设置无边框样式
        setBorder(new EmptyBorder(0, 0, 0, 0));

        // 应用自定义样式
        applyCustomStyle();

        addActionListener(e -> {
            if (isUpdating) return;
            Workspace workspace = (Workspace) getSelectedItem();
            if (workspace != null && onWorkspaceChange != null) {
                onWorkspaceChange.accept(workspace);
            }
        });
        reload();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // 主题切换后重新应用自定义样式
        applyCustomStyle();
    }

    /**
     * 应用自定义样式，使用 FlatLaf 的 client property 方式
     * 这样只会影响这个特定的 ComboBox 实例
     */
    private void applyCustomStyle() {
        Color backgroundColor = UIManager.getColor("MenuBar.background");
        if (backgroundColor != null) {
            String styleValue = String.format(
                "buttonBackground: %s; " +
                "buttonEditableBackground: %s; " +
                "buttonFocusedBackground: %s; " +
                "buttonSeparatorColor: null; " +
                "buttonDisabledSeparatorColor: null",
                colorToHex(backgroundColor),
                colorToHex(backgroundColor),
                colorToHex(backgroundColor)
            );

            putClientProperty("FlatLaf.style", styleValue);
            setBackground(backgroundColor);
        }
    }

    /**
     * 将 Color 转换为十六进制字符串
     */
    private static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * 重新加载工作区列表
     */
    public void reload() {
        isUpdating = true;
        removeAllItems();

        try {
            WorkspaceService workspaceService = WorkspaceService.getInstance();
            List<Workspace> workspaces = workspaceService.getAllWorkspaces();
            Workspace currentWorkspace = workspaceService.getCurrentWorkspace();

            for (Workspace workspace : workspaces) {
                addItem(workspace);
            }

            // 设置当前选中的工作区
            if (currentWorkspace != null) {
                setSelectedItem(currentWorkspace);
            } else if (getItemCount() > 0) {
                setSelectedIndex(0);
            }
        } catch (Exception e) {
            // 加载失败时静默处理
        } finally {
            isUpdating = false;
        }
    }

    /**
     * 选中指定工作区
     */
    public void setSelectedWorkspace(Workspace workspace) {
        if (workspace == null) return;
        isUpdating = true;
        for (int i = 0; i < getItemCount(); i++) {
            Workspace item = getItemAt(i);
            if (item.getId().equals(workspace.getId())) {
                setSelectedIndex(i);
                break;
            }
        }
        isUpdating = false;
    }

    /**
     * 获取当前选中的工作区
     */
    public Workspace getSelectedWorkspace() {
        return (Workspace) getSelectedItem();
    }
}
