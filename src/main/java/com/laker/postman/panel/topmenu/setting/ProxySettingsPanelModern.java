package com.laker.postman.panel.topmenu.setting;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import javax.swing.*;
import java.awt.*;
public class ProxySettingsPanelModern extends ModernSettingsPanel {
    private static final int FIELD_SPACING = 8;
    private static final int SECTION_SPACING = 12;
    private JCheckBox proxyEnabledCheckBox;
    private JComboBox<String> proxyTypeComboBox;
    private JTextField proxyHostField;
    private JTextField proxyPortField;
    private JTextField proxyUsernameField;
    private JPasswordField proxyPasswordField;
    private JCheckBox sslVerificationDisabledCheckBox;
    @Override
    protected void buildContent(JPanel contentPanel) {
        JPanel proxySection = createModernSection(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_TITLE),
                ""
        );
        proxyEnabledCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_ENABLED_CHECKBOX),
                SettingManager.isProxyEnabled()
        );
        JPanel proxyEnabledRow = createCheckBoxRow(
                proxyEnabledCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_ENABLED_TOOLTIP)
        );
        proxySection.add(proxyEnabledRow);
        proxySection.add(createVerticalSpace(FIELD_SPACING));
        proxyTypeComboBox = new JComboBox<>(new String[]{
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_TYPE_HTTP),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_TYPE_SOCKS)
        });
        proxyTypeComboBox.setSelectedItem(SettingManager.getProxyType());
        JPanel proxyTypeRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_TYPE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_TYPE_TOOLTIP),
                proxyTypeComboBox
        );
        proxySection.add(proxyTypeRow);
        proxySection.add(createVerticalSpace(FIELD_SPACING));
        proxyHostField = new JTextField(10);
        proxyHostField.setText(SettingManager.getProxyHost());
        JPanel proxyHostRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_HOST),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_HOST_TOOLTIP),
                proxyHostField
        );
        proxySection.add(proxyHostRow);
        proxySection.add(createVerticalSpace(FIELD_SPACING));
        proxyPortField = new JTextField(10);
        proxyPortField.setText(String.valueOf(SettingManager.getProxyPort()));
        JPanel proxyPortRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_PORT),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_PORT_TOOLTIP),
                proxyPortField
        );
        proxySection.add(proxyPortRow);
        proxySection.add(createVerticalSpace(FIELD_SPACING));
        proxyUsernameField = new JTextField(10);
        proxyUsernameField.setText(SettingManager.getProxyUsername());
        JPanel proxyUsernameRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_USERNAME),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_USERNAME_TOOLTIP),
                proxyUsernameField
        );
        proxySection.add(proxyUsernameRow);
        proxySection.add(createVerticalSpace(FIELD_SPACING));
        proxyPasswordField = new JPasswordField(10);
        proxyPasswordField.setText(SettingManager.getProxyPassword());
        JPanel proxyPasswordRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_PASSWORD),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_PASSWORD_TOOLTIP),
                proxyPasswordField
        );
        proxySection.add(proxyPasswordRow);
        proxySection.add(createVerticalSpace(FIELD_SPACING));
        sslVerificationDisabledCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_SSL_VERIFICATION_CHECKBOX),
                SettingManager.isProxySslVerificationDisabled()
        );
        JPanel sslVerificationRow = createCheckBoxRow(
                sslVerificationDisabledCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_SSL_VERIFICATION_TOOLTIP)
        );
        proxySection.add(sslVerificationRow);
        contentPanel.add(proxySection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));
        setupValidators();

        // 跟踪所有组件的初始值
        trackComponentValue(proxyEnabledCheckBox);
        trackComponentValue(proxyTypeComboBox);
        trackComponentValue(proxyHostField);
        trackComponentValue(proxyPortField);
        trackComponentValue(proxyUsernameField);
        trackComponentValue(proxyPasswordField);
        trackComponentValue(sslVerificationDisabledCheckBox);
    }
    private void setupValidators() {
        setupValidator(
                proxyPortField,
                s -> isInteger(s) && Integer.parseInt(s) > 0 && Integer.parseInt(s) <= 65535,
                I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_PORT_ERROR)
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
    }
    private void saveSettings(boolean closeAfterSave) {
        if (!validateAllFields()) {
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_ERROR_MESSAGE));
            return;
        }
        try {
            SettingManager.setProxyEnabled(proxyEnabledCheckBox.isSelected());
            SettingManager.setProxyType((String) proxyTypeComboBox.getSelectedItem());
            SettingManager.setProxyHost(proxyHostField.getText().trim());
            SettingManager.setProxyPort(Integer.parseInt(proxyPortField.getText().trim()));
            SettingManager.setProxyUsername(proxyUsernameField.getText().trim());
            SettingManager.setProxyPassword(new String(proxyPasswordField.getPassword()));
            SettingManager.setProxySslVerificationDisabled(sslVerificationDisabledCheckBox.isSelected());
            OkHttpClientManager.clearClientCache();

            // 重新跟踪当前值
            originalValues.clear();
            trackComponentValue(proxyEnabledCheckBox);
            trackComponentValue(proxyTypeComboBox);
            trackComponentValue(proxyHostField);
            trackComponentValue(proxyPortField);
            trackComponentValue(proxyUsernameField);
            trackComponentValue(proxyPasswordField);
            trackComponentValue(sslVerificationDisabledCheckBox);
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
}
