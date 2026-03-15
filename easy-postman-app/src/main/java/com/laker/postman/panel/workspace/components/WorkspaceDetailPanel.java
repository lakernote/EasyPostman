package com.laker.postman.panel.workspace.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.model.GitRepoSource;
import com.laker.postman.model.Workspace;
import com.laker.postman.model.WorkspaceType;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class WorkspaceDetailPanel extends JPanel {

    public WorkspaceDetailPanel(Workspace workspace) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel infoSection = new JPanel(new GridBagLayout());
        infoSection.setBorder(BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_BASIC_INFO)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // 名称
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        infoSection.add(new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_NAME) + ":"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel nameLabel = new JLabel(workspace.getName());
        nameLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        infoSection.add(nameLabel, gbc);

        // 类型
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        infoSection.add(new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE) + ":"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        infoSection.add(new JLabel(workspace.getType() == WorkspaceType.LOCAL ?
                I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE_LOCAL) :
                I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE_GIT)), gbc);

        // 路径 —— 交互式路径组件
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        infoSection.add(new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_PATH) + ":"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        infoSection.add(new PathFieldPanel(workspace.getPath()), gbc);

        // 描述
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        infoSection.add(new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_DESCRIPTION) + ":"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        infoSection.add(new JLabel(workspace.getDescription()), gbc);

        // 创建时间
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        infoSection.add(new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_CREATED_TIME) + ":"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        infoSection.add(new JLabel(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(workspace.getCreatedAt()))), gbc);

        add(infoSection, BorderLayout.NORTH);

        if (workspace.getType() == WorkspaceType.GIT) {
            add(createGitInfoPanel(workspace), BorderLayout.CENTER);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 交互式路径组件：路径文本 + 复制按钮 + 在文件管理器中打开按钮
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 路径交互组件：路径文本 + 复制按钮 + 在文件管理器中打开按钮，从左向右紧密排列。
     */
    private static class PathFieldPanel extends JPanel {

        PathFieldPanel(String path) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setOpaque(false);

            JLabel pathLabel = new JLabel(path);
            pathLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
            pathLabel.setToolTipText(path);
            pathLabel.setPreferredSize(new Dimension(
                    Math.min(pathLabel.getPreferredSize().width, 400),
                    pathLabel.getPreferredSize().height));
            pathLabel.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            add(pathLabel);

            add(Box.createHorizontalStrut(8));
            add(makeCopyBtn(path));
            add(makeOpenBtn(path));
        }

        private JButton makeCopyBtn(String path) {
            JButton btn = makeIconBtn(
                    IconUtil.createThemed("icons/copy.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL),
                    I18nUtil.getMessage(MessageKeys.WORKSPACE_PATH_COPY_TOOLTIP));
            btn.addActionListener(e -> {
                Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new StringSelection(path), null);
                // 临时换绿色 check 图标作为成功反馈
                btn.setIcon(IconUtil.createColored("icons/check.svg",
                        IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL, new Color(40, 167, 69)));
                Timer t = new Timer(1500, ev ->
                        btn.setIcon(IconUtil.createThemed("icons/copy.svg",
                                IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL)));
                t.setRepeats(false);
                t.start();
            });
            return btn;
        }

        private JButton makeOpenBtn(String path) {
            JButton btn = makeIconBtn(
                    IconUtil.createThemed("icons/file.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL),
                    I18nUtil.getMessage(MessageKeys.WORKSPACE_PATH_OPEN_TOOLTIP));
            btn.addActionListener(e -> {
                try {
                    File dir = new File(path);
                    if (!dir.exists()) {
                        JOptionPane.showMessageDialog(btn, path,
                                I18nUtil.getMessage(MessageKeys.WORKSPACE_PATH_NOT_EXIST),
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    Desktop.getDesktop().open(dir);
                } catch (Exception ex) {
                    log.warn("Failed to open directory: {}", path, ex);
                    JOptionPane.showMessageDialog(btn, ex.getMessage(),
                            I18nUtil.getMessage(MessageKeys.ERROR),
                            JOptionPane.ERROR_MESSAGE);
                }
            });
            return btn;
        }

        /**
         * 无边框图标按钮，hover/press 跟随 FlatLaf 主题背景色
         */
        private static JButton makeIconBtn(Icon icon, String tooltip) {
            JButton btn = new JButton(icon);
            btn.setToolTipText(tooltip);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
            return btn;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Git 信息面板
    // ──────────────────────────────────────────────────────────────────────────

    private JPanel createGitInfoPanel(Workspace workspace) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_GIT_INFO)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;

        addRow(panel, gbc, row++, I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_REPO_SOURCE) + ":",
                workspace.getGitRepoSource() == GitRepoSource.CLONED
                        ? I18nUtil.getMessage(MessageKeys.WORKSPACE_CLONE_FROM_REMOTE)
                        : I18nUtil.getMessage(MessageKeys.WORKSPACE_INIT_LOCAL));

        addRow(panel, gbc, row++, I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_REMOTE_REPO) + ":",
                workspace.getGitRemoteUrl());

        addRow(panel, gbc, row++, I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_LOCAL_BRANCH) + ":",
                workspace.getCurrentBranch());

        addRow(panel, gbc, row++, I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_REMOTE_BRANCH) + ":",
                workspace.getRemoteBranch());

        String shortCommitId = workspace.getLastCommitId() == null ? "" :
                workspace.getLastCommitId().length() > 8
                        ? workspace.getLastCommitId().substring(0, 8)
                        : workspace.getLastCommitId();
        addRow(panel, gbc, row, I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_LAST_COMMIT) + ":",
                shortCommitId);

        return panel;
    }

    /**
     * 向 GridBagLayout 面板追加一行 label + value
     */
    private static void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel v = new JLabel(value == null ? "" : value);
        v.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        panel.add(v, gbc);
    }
}
