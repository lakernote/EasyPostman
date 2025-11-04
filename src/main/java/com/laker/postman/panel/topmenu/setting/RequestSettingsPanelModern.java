package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 现代化请求设置面板
 */
public class RequestSettingsPanelModern extends ModernSettingsPanel {
    private static final int FIELD_SPACING = 16;
    private static final int SECTION_SPACING = 24;

    private JTextField maxBodySizeField;
    private JTextField requestTimeoutField;
    private JTextField maxDownloadSizeField;
    private JCheckBox followRedirectsCheckBox;
    private JCheckBox sslVerificationDisabledCheckBox;

    @Override
    protected void buildContent(JPanel contentPanel) {
        // 请求设置区域
        JPanel requestSection = createModernSection(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TITLE),
                ""
        );

        // 响应体最大显示大小
        maxBodySizeField = new JTextField(10);
        int maxBodySizeKB = SettingManager.getMaxBodySize() / 1024;
        maxBodySizeField.setText(String.valueOf(maxBodySizeKB));
        JPanel maxBodySizeRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_MAX_BODY_SIZE) + " (KB)",
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_MAX_BODY_SIZE_TOOLTIP),
                maxBodySizeField
        );
        requestSection.add(maxBodySizeRow);
        requestSection.add(createVerticalSpace(FIELD_SPACING));

        // 请求超时时间
        requestTimeoutField = new JTextField(10);
        requestTimeoutField.setText(String.valueOf(SettingManager.getRequestTimeout()));
        JPanel requestTimeoutRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TIMEOUT) + " (seconds)",
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TIMEOUT_TOOLTIP),
                requestTimeoutField
        );
        requestSection.add(requestTimeoutRow);
        requestSection.add(createVerticalSpace(FIELD_SPACING));

        // 最大响应下载大小
        maxDownloadSizeField = new JTextField(10);
        int maxDownloadSizeMB = SettingManager.getMaxDownloadSize() / (1024 * 1024);
        maxDownloadSizeField.setText(String.valueOf(maxDownloadSizeMB));
        JPanel maxDownloadSizeRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_MAX_DOWNLOAD_SIZE) + " (MB)",
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_MAX_DOWNLOAD_SIZE_TOOLTIP),
                maxDownloadSizeField
        );
        requestSection.add(maxDownloadSizeRow);
        requestSection.add(createVerticalSpace(FIELD_SPACING));

        // 自动重定向设置
        followRedirectsCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_FOLLOW_REDIRECTS_CHECKBOX),
                SettingManager.isFollowRedirects()
        );
        JPanel followRedirectsRow = createCheckBoxRow(
                followRedirectsCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_FOLLOW_REDIRECTS_TOOLTIP)
        );
        requestSection.add(followRedirectsRow);
        requestSection.add(createVerticalSpace(FIELD_SPACING));

        // SSL 验证设置
        sslVerificationDisabledCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SSL_VERIFICATION_CHECKBOX),
                SettingManager.isRequestSslVerificationDisabled()
        );
        JPanel sslVerificationRow = createCheckBoxRow(
                sslVerificationDisabledCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SSL_VERIFICATION_TOOLTIP)
        );
        requestSection.add(sslVerificationRow);

        contentPanel.add(requestSection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));

        setPreferredSize(new Dimension(650, 450));
        setupValidators();
        
        // 跟踪所有组件的初始值
        trackComponentValue(maxBodySizeField);
        trackComponentValue(requestTimeoutField);
        trackComponentValue(maxDownloadSizeField);
        trackComponentValue(followRedirectsCheckBox);
        trackComponentValue(sslVerificationDisabledCheckBox);
    }

    private void setupValidators() {
        setupValidator(
                maxBodySizeField,
                this::isPositiveInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_MAX_BODY_SIZE_ERROR)
        );
        setupValidator(
                requestTimeoutField,
                this::isPositiveInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_TIMEOUT_ERROR)
        );
        setupValidator(
                maxDownloadSizeField,
                this::isPositiveInteger,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_MAX_DOWNLOAD_SIZE_ERROR)
        );
    }

    @Override
    protected void registerListeners() {
        saveBtn.addActionListener(e -> saveSettings(true));
        applyBtn.addActionListener(e -> saveSettings(false));
        cancelBtn.addActionListener(e -> {
            if (confirmDiscardChanges()) {
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window instanceof JDialog dialog) {
                    dialog.dispose();
                }
            }
        });

        // 键盘快捷键
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("control S"), "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveSettings(false);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        actionMap.put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cancelBtn.doClick();
            }
        });
    }

    private void saveSettings(boolean closeAfterSave) {
        // 验证所有字段
        if (!validateAllFields()) {
            NotificationUtil.showError(
            I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_ERROR_MESSAGE));
            return;
        }

        try {
            // 保存请求设置
            int maxBodySizeKB = Integer.parseInt(maxBodySizeField.getText().trim());
            SettingManager.setMaxBodySize(maxBodySizeKB * 1024);
            SettingManager.setRequestTimeout(Integer.parseInt(requestTimeoutField.getText().trim()));

            int maxDownloadSizeMB = Integer.parseInt(maxDownloadSizeField.getText().trim());
            SettingManager.setMaxDownloadSize(maxDownloadSizeMB * 1024 * 1024);

            SettingManager.setFollowRedirects(followRedirectsCheckBox.isSelected());
            SettingManager.setRequestSslVerificationDisabled(sslVerificationDisabledCheckBox.isSelected());

            // Clear client cache to apply new settings
            OkHttpClientManager.clearClientCache();
            
            // 重新跟踪当前值
            originalValues.clear();
            trackComponentValue(maxBodySizeField);
            trackComponentValue(requestTimeoutField);
            trackComponentValue(maxDownloadSizeField);
            trackComponentValue(followRedirectsCheckBox);
            trackComponentValue(sslVerificationDisabledCheckBox);
            setHasUnsavedChanges(false);

            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_SUCCESS_MESSAGE));

            // 根据参数决定是否关闭对话框
            if (closeAfterSave) {
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window instanceof JDialog dialog) {
                    dialog.dispose();
                }
            }
        } catch (Exception ex) {
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_ERROR_MESSAGE) + ": " + ex.getMessage());
        }
    }
}

