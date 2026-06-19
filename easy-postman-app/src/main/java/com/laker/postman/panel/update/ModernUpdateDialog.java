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
 * 现代化更新对话框 - 简洁清晰的更新提示
 */
public class ModernUpdateDialog extends JDialog {

    private int userChoice = -1;

    public ModernUpdateDialog(Frame parent, UpdateInfo updateInfo) {
        super(parent, I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE), true);
        initComponents(updateInfo);
        setMinimumSize(new Dimension(UpdateDialogStyle.UPDATE_DIALOG_MIN_WIDTH,
                UpdateDialogStyle.UPDATE_DIALOG_MIN_HEIGHT));
        pack();
        setSize(Math.max(getWidth(), UpdateDialogStyle.UPDATE_DIALOG_MIN_WIDTH),
                Math.max(getHeight(), UpdateDialogStyle.UPDATE_DIALOG_DEFAULT_HEIGHT));
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
    }

    private void initComponents(UpdateInfo updateInfo) {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogWindowChrome(this);
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        ToolWindowSurfaceStyle.applyDialogSurface(mainPanel);
        mainPanel.add(createHeaderPanel(updateInfo), BorderLayout.NORTH);
        mainPanel.add(createChangelogPanel(updateInfo), BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        add(mainPanel);
    }

    private JPanel createHeaderPanel(UpdateInfo updateInfo) {
        String publishedAt = updateInfo.getReleaseInfo() != null
                ? updateInfo.getReleaseInfo().getStr("published_at", "") : "";
        return UpdateDialogStyle.createHeaderPanel(
                "icons/info.svg",
                ModernColors.getPrimary(),
                I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE),
                UpdateTextFormatter.dialogVersionText(updateInfo.getCurrentVersion(), updateInfo.getLatestVersion()),
                UpdateTextFormatter.releasedOnText(publishedAt)
        );
    }

    private JPanel createChangelogPanel(UpdateInfo updateInfo) {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 8));
        ToolWindowSurfaceStyle.applyDialogSurface(mainPanel);
        mainPanel.setBorder(new EmptyBorder(12, 24, 12, 24));

        JLabel titleLabel = UpdateDialogStyle.createSectionTitle(I18nUtil.getMessage(MessageKeys.UPDATE_WHATS_NEW));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JTextArea textArea = new JTextArea(UpdateTextFormatter.changelog(updateInfo.getReleaseInfo()));
        JScrollPane scrollPane = UpdateDialogStyle.createFramedReadOnlyScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(0, UpdateDialogStyle.CHANGELOG_PREFERRED_HEIGHT));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        ToolWindowSurfaceStyle.applyDialogFooter(panel);

        panel.add(UpdateDialogStyle.createFooterTip(I18nUtil.getMessage(MessageKeys.UPDATE_SAVE_TIP)),
                BorderLayout.WEST);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        ToolWindowSurfaceStyle.applyDialogSurface(buttonsPanel);

        JButton laterButton = UpdateDialogStyle.createSecondaryButton(I18nUtil.getMessage(MessageKeys.UPDATE_LATER));
        laterButton.addActionListener(e -> { userChoice = 2; dispose(); });

        JButton ignoreButton = UpdateDialogStyle.createSecondaryButton(I18nUtil.getMessage(MessageKeys.UPDATE_IGNORE_VERSION));
        ignoreButton.addActionListener(e -> { userChoice = 3; dispose(); });

        JButton manualButton = UpdateDialogStyle.createSecondaryButton(I18nUtil.getMessage(MessageKeys.UPDATE_MANUAL_DOWNLOAD));
        manualButton.addActionListener(e -> { userChoice = 0; dispose(); });

        JButton autoButton = UpdateDialogStyle.createPrimaryButton(
                I18nUtil.getMessage(MessageKeys.UPDATE_NOW),
                "icons/download.svg"
        );
        autoButton.addActionListener(e -> { userChoice = 1; dispose(); });

        buttonsPanel.add(ignoreButton);
        buttonsPanel.add(laterButton);
        buttonsPanel.add(manualButton);
        buttonsPanel.add(autoButton);
        panel.add(buttonsPanel, BorderLayout.EAST);

        UpdateDialogStyle.installDefaultButton(this, autoButton);
        return panel;
    }

    public int showDialogAndGetChoice() {
        setVisible(true);
        return userChoice;
    }

    public static int showUpdateDialog(Frame parent, UpdateInfo updateInfo) {
        return new ModernUpdateDialog(parent, updateInfo).showDialogAndGetChoice();
    }
}
