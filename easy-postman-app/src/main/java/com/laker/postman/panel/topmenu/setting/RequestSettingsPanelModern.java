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
    private static final int FIELD_SPACING = 8;
    private static final int SECTION_SPACING = 12;
    private static final int BYTES_PER_KB = 1024;

    private JTextField maxBodySizeField;
    private JTextField requestTimeoutField;
    private JTextField maxDownloadSizeField;
    private JCheckBox followRedirectsCheckBox;
    private JCheckBox sslVerificationDisabledCheckBox;
    private JComboBox<String> defaultProtocolComboBox;
    private JCheckBox remoteScriptRequireEnabledCheckBox;
    private JCheckBox remoteScriptRequireAllowHttpCheckBox;
    private JTextField remoteScriptAllowedHostsField;
    private JTextField remoteScriptConnectTimeoutField;
    private JTextField remoteScriptReadTimeoutField;
    private JTextField remoteScriptMaxSizeField;

    @Override
    protected void buildContent(JPanel contentPanel) {
        JPanel requestSection = createModernSection(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TITLE),
                ""
        );

        maxBodySizeField = new JTextField(10);
        int maxBodySizeKB = SettingManager.getMaxBodySize() / 1024;
        maxBodySizeField.setText(String.valueOf(maxBodySizeKB));
        JPanel maxBodySizeRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_MAX_BODY_SIZE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_MAX_BODY_SIZE_TOOLTIP),
                maxBodySizeField
        );
        requestSection.add(maxBodySizeRow);
        requestSection.add(createVerticalSpace(FIELD_SPACING));

        requestTimeoutField = new JTextField(10);
        requestTimeoutField.setText(String.valueOf(SettingManager.getRequestTimeout()));
        JPanel requestTimeoutRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TIMEOUT),
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TIMEOUT_TOOLTIP),
                requestTimeoutField
        );
        requestSection.add(requestTimeoutRow);
        requestSection.add(createVerticalSpace(FIELD_SPACING));

        maxDownloadSizeField = new JTextField(10);
        int maxDownloadSizeMB = SettingManager.getMaxDownloadSize() / (1024 * 1024);
        maxDownloadSizeField.setText(String.valueOf(maxDownloadSizeMB));
        JPanel maxDownloadSizeRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_MAX_DOWNLOAD_SIZE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_MAX_DOWNLOAD_SIZE_TOOLTIP),
                maxDownloadSizeField
        );
        requestSection.add(maxDownloadSizeRow);
        requestSection.add(createVerticalSpace(FIELD_SPACING));

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

        sslVerificationDisabledCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SSL_VERIFICATION_CHECKBOX),
                SettingManager.isRequestSslVerificationDisabled()
        );
        JPanel sslVerificationRow = createCheckBoxRow(
                sslVerificationDisabledCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SSL_VERIFICATION_TOOLTIP)
        );
        requestSection.add(sslVerificationRow);
        requestSection.add(createVerticalSpace(FIELD_SPACING));

        String[] protocolOptions = {"https", "http"};
        defaultProtocolComboBox = new JComboBox<>(protocolOptions);
        defaultProtocolComboBox.setSelectedItem(SettingManager.getDefaultProtocol());
        JPanel defaultProtocolRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_DEFAULT_PROTOCOL),
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_DEFAULT_PROTOCOL_TOOLTIP),
                defaultProtocolComboBox
        );
        requestSection.add(defaultProtocolRow);

        contentPanel.add(requestSection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));

        JPanel scriptSection = createModernSection(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SCRIPT_TITLE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SCRIPT_DESCRIPTION)
        );

        remoteScriptRequireEnabledCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SCRIPT_REMOTE_REQUIRE_ENABLED),
                SettingManager.isRemoteJsRequireEnabled()
        );
        scriptSection.add(createCheckBoxRow(
                remoteScriptRequireEnabledCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SCRIPT_REMOTE_REQUIRE_ENABLED_TOOLTIP)
        ));
        scriptSection.add(createVerticalSpace(FIELD_SPACING));

        remoteScriptRequireAllowHttpCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SCRIPT_REMOTE_REQUIRE_ALLOW_HTTP),
                SettingManager.isInsecureRemoteJsRequireEnabled()
        );
        scriptSection.add(createCheckBoxRow(
                remoteScriptRequireAllowHttpCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SCRIPT_REMOTE_REQUIRE_ALLOW_HTTP_TOOLTIP)
        ));
        scriptSection.add(createVerticalSpace(FIELD_SPACING));

        remoteScriptAllowedHostsField = new JTextField(10);
        remoteScriptAllowedHostsField.setText(SettingManager.getRemoteJsRequireAllowedHosts());
        scriptSection.add(createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SCRIPT_REMOTE_REQUIRE_ALLOWED_HOSTS),
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SCRIPT_REMOTE_REQUIRE_ALLOWED_HOSTS_TOOLTIP),
                remoteScriptAllowedHostsField
        ));
        scriptSection.add(createVerticalSpace(FIELD_SPACING));

        remoteScriptConnectTimeoutField = new JTextField(10);
        remoteScriptConnectTimeoutField.setText(String.valueOf(SettingManager.getRemoteJsRequireConnectTimeoutMs()));
        scriptSection.add(createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SCRIPT_REMOTE_REQUIRE_CONNECT_TIMEOUT),
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SCRIPT_REMOTE_REQUIRE_CONNECT_TIMEOUT_TOOLTIP),
                remoteScriptConnectTimeoutField
        ));
        scriptSection.add(createVerticalSpace(FIELD_SPACING));

        remoteScriptReadTimeoutField = new JTextField(10);
        remoteScriptReadTimeoutField.setText(String.valueOf(SettingManager.getRemoteJsRequireReadTimeoutMs()));
        scriptSection.add(createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SCRIPT_REMOTE_REQUIRE_READ_TIMEOUT),
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SCRIPT_REMOTE_REQUIRE_READ_TIMEOUT_TOOLTIP),
                remoteScriptReadTimeoutField
        ));
        scriptSection.add(createVerticalSpace(FIELD_SPACING));

        remoteScriptMaxSizeField = new JTextField(10);
        remoteScriptMaxSizeField.setText(String.valueOf(SettingManager.getRemoteJsRequireMaxBytes() / BYTES_PER_KB));
        scriptSection.add(createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SCRIPT_REMOTE_REQUIRE_MAX_SIZE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_SCRIPT_REMOTE_REQUIRE_MAX_SIZE_TOOLTIP),
                remoteScriptMaxSizeField
        ));

        contentPanel.add(scriptSection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));

        setupValidators();
        updateRemoteScriptControls();

        trackComponentValue(maxBodySizeField);
        trackComponentValue(requestTimeoutField);
        trackComponentValue(maxDownloadSizeField);
        trackComponentValue(followRedirectsCheckBox);
        trackComponentValue(sslVerificationDisabledCheckBox);
        trackComponentValue(defaultProtocolComboBox);
        trackComponentValue(remoteScriptRequireEnabledCheckBox);
        trackComponentValue(remoteScriptRequireAllowHttpCheckBox);
        trackComponentValue(remoteScriptAllowedHostsField);
        trackComponentValue(remoteScriptConnectTimeoutField);
        trackComponentValue(remoteScriptReadTimeoutField);
        trackComponentValue(remoteScriptMaxSizeField);
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
        setupValidator(
                remoteScriptConnectTimeoutField,
                value -> !remoteScriptRequireEnabledCheckBox.isSelected() || isStrictlyPositiveInteger(value),
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_SCRIPT_REMOTE_CONNECT_TIMEOUT_ERROR)
        );
        setupValidator(
                remoteScriptReadTimeoutField,
                value -> !remoteScriptRequireEnabledCheckBox.isSelected() || isStrictlyPositiveInteger(value),
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_SCRIPT_REMOTE_READ_TIMEOUT_ERROR)
        );
        setupValidator(
                remoteScriptMaxSizeField,
                value -> !remoteScriptRequireEnabledCheckBox.isSelected() || isStrictlyPositiveInteger(value),
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_SCRIPT_REMOTE_MAX_SIZE_ERROR)
        );
    }

    @Override
    protected void registerListeners() {
        remoteScriptRequireEnabledCheckBox.addItemListener(e -> updateRemoteScriptControls());
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

        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("control S"), "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveSettings(false);
            }
        });
    }

    private void saveSettings(boolean closeAfterSave) {
        if (!validateAllFields()) {
            NotificationUtil.showError(
                    I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_ERROR_MESSAGE));
            return;
        }

        try {
            int maxBodySizeKB = Integer.parseInt(maxBodySizeField.getText().trim());
            SettingManager.setMaxBodySize(maxBodySizeKB * 1024);
            SettingManager.setRequestTimeout(Integer.parseInt(requestTimeoutField.getText().trim()));

            int maxDownloadSizeMB = Integer.parseInt(maxDownloadSizeField.getText().trim());
            SettingManager.setMaxDownloadSize(maxDownloadSizeMB * 1024 * 1024);

            SettingManager.setFollowRedirects(followRedirectsCheckBox.isSelected());
            SettingManager.setRequestSslVerificationDisabled(sslVerificationDisabledCheckBox.isSelected());
            SettingManager.setDefaultProtocol((String) defaultProtocolComboBox.getSelectedItem());
            SettingManager.setRemoteJsRequireEnabled(remoteScriptRequireEnabledCheckBox.isSelected());
            SettingManager.setInsecureRemoteJsRequireEnabled(remoteScriptRequireAllowHttpCheckBox.isSelected());
            SettingManager.setRemoteJsRequireAllowedHosts(remoteScriptAllowedHostsField.getText());
            SettingManager.setRemoteJsRequireConnectTimeoutMs(Integer.parseInt(remoteScriptConnectTimeoutField.getText().trim()));
            SettingManager.setRemoteJsRequireReadTimeoutMs(Integer.parseInt(remoteScriptReadTimeoutField.getText().trim()));
            SettingManager.setRemoteJsRequireMaxBytes(Integer.parseInt(remoteScriptMaxSizeField.getText().trim()) * BYTES_PER_KB);

            OkHttpClientManager.clearClientCache();

            originalValues.clear();
            trackComponentValue(maxBodySizeField);
            trackComponentValue(requestTimeoutField);
            trackComponentValue(maxDownloadSizeField);
            trackComponentValue(followRedirectsCheckBox);
            trackComponentValue(sslVerificationDisabledCheckBox);
            trackComponentValue(defaultProtocolComboBox);
            trackComponentValue(remoteScriptRequireEnabledCheckBox);
            trackComponentValue(remoteScriptRequireAllowHttpCheckBox);
            trackComponentValue(remoteScriptAllowedHostsField);
            trackComponentValue(remoteScriptConnectTimeoutField);
            trackComponentValue(remoteScriptReadTimeoutField);
            trackComponentValue(remoteScriptMaxSizeField);
            setHasUnsavedChanges(false);

            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_SUCCESS_MESSAGE));

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

    private void updateRemoteScriptControls() {
        boolean remoteEnabled = remoteScriptRequireEnabledCheckBox.isSelected();
        remoteScriptRequireAllowHttpCheckBox.setEnabled(remoteEnabled);
        remoteScriptAllowedHostsField.setEnabled(remoteEnabled);
        remoteScriptConnectTimeoutField.setEnabled(remoteEnabled);
        remoteScriptReadTimeoutField.setEnabled(remoteEnabled);
        remoteScriptMaxSizeField.setEnabled(remoteEnabled);
    }

    private boolean isStrictlyPositiveInteger(String value) {
        return isInteger(value) && Integer.parseInt(value) > 0;
    }
}
