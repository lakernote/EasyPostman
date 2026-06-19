package com.laker.postman.panel.update;

import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.platform.update.model.UpdateInfo;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 有新版本但暂无安装包的提示对话框
 */
public class NoAssetDialog extends JDialog {

    private boolean goToGitHub = false;

    public NoAssetDialog(Frame parent, UpdateInfo updateInfo) {
        super(parent, I18nUtil.getMessage(MessageKeys.UPDATE_AVAILABLE_NO_ASSET_TITLE,
                updateInfo.getLatestVersion()), true);
        initComponents(updateInfo);
        setMinimumSize(new Dimension(UpdateDialogStyle.NO_ASSET_DIALOG_MIN_WIDTH,
                UpdateDialogStyle.NO_ASSET_DIALOG_MIN_HEIGHT));
        pack();
        setSize(Math.max(getWidth(), UpdateDialogStyle.NO_ASSET_DIALOG_MIN_WIDTH),
                Math.max(getHeight(), UpdateDialogStyle.NO_ASSET_DIALOG_DEFAULT_HEIGHT));
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
    }

    private void initComponents(UpdateInfo updateInfo) {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogWindowChrome(this);
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        ToolWindowSurfaceStyle.applyDialogSurface(mainPanel);
        mainPanel.add(createHeaderPanel(updateInfo), BorderLayout.NORTH);
        mainPanel.add(createBodyPanel(updateInfo), BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        add(mainPanel);
    }

    private JPanel createHeaderPanel(UpdateInfo updateInfo) {
        String publishedAt = updateInfo.getReleaseInfo() != null
                ? updateInfo.getReleaseInfo().getStr("published_at", "") : "";
        return UpdateDialogStyle.createHeaderPanel(
                "icons/warning.svg",
                ModernColors.getWarning(),
                I18nUtil.getMessage(MessageKeys.UPDATE_AVAILABLE_NO_ASSET_TITLE, updateInfo.getLatestVersion()),
                UpdateTextFormatter.dialogVersionText(updateInfo.getCurrentVersion(), updateInfo.getLatestVersion()),
                UpdateTextFormatter.releasedOnText(publishedAt)
        );
    }

    private JPanel createBodyPanel(UpdateInfo updateInfo) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        ToolWindowSurfaceStyle.applyDialogSurface(panel);
        panel.setBorder(new EmptyBorder(18, 24, 14, 24));

        JLabel msgLabel = UpdateDialogStyle.createHtmlBodyLabel(
                I18nUtil.getMessage(MessageKeys.UPDATE_AVAILABLE_NO_ASSET_HINT, updateInfo.getLatestVersion()),
                520
        );
        panel.add(msgLabel, BorderLayout.NORTH);

        panel.add(UpdateDialogStyle.createTipBar(
                I18nUtil.getMessage(MessageKeys.UPDATE_AVAILABLE_NO_ASSET_TIP),
                ModernColors.getWarning(),
                480
        ), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogFooter(panel);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        ToolWindowSurfaceStyle.applyDialogSurface(buttonsPanel);

        JButton laterButton = UpdateDialogStyle.createSecondaryButton(I18nUtil.getMessage(MessageKeys.UPDATE_LATER));
        laterButton.addActionListener(e -> dispose());

        JButton goButton = UpdateDialogStyle.createPrimaryButton(
                I18nUtil.getMessage(MessageKeys.UPDATE_AVAILABLE_NO_ASSET_GO_GITHUB),
                "icons/download.svg"
        );
        goButton.addActionListener(e -> { goToGitHub = true; dispose(); });

        buttonsPanel.add(laterButton);
        buttonsPanel.add(goButton);
        panel.add(buttonsPanel, BorderLayout.EAST);

        UpdateDialogStyle.installDefaultButton(this, goButton);
        return panel;
    }

    public boolean showDialogAndWait() {
        setVisible(true);
        return goToGitHub;
    }

    /**
     * 显示对话框，返回用户是否选择前往 GitHub。
     */
    public static boolean show(Frame parent, UpdateInfo updateInfo) {
        return new NoAssetDialog(parent, updateInfo).showDialogAndWait();
    }
}
