package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.net.URI;

public class ProxySettingsPanelModern extends ModernSettingsPanel {
    private static final int FIELD_SPACING = 8;
    private static final int SECTION_SPACING = 12;
    private static final int FIELD_LABEL_WIDTH = 220;
    private static final int PREVIEW_ROW_WIDTH = 460;
    private static final String DEFAULT_PROXY_PREVIEW_TARGET = "example.com";
    private JCheckBox proxyEnabledCheckBox;
    private JComboBox<String> proxyModeComboBox;
    private JComboBox<String> proxyTypeComboBox;
    private JTextField proxyHostField;
    private JTextField proxyPortField;
    private JTextField proxyUsernameField;
    private JPasswordField proxyPasswordField;
    private JCheckBox sslVerificationDisabledCheckBox;
    private JTextArea proxyAuthHintArea;
    private JTextArea proxyStatusArea;
    private JTextField proxyPreviewTargetField;

    private record ProxyViewState(
            boolean proxyEnabled,
            boolean manualMode,
            boolean manualFieldsEnabled,
            boolean authFieldsEnabled,
            boolean showSystemPreviewControls
    ) {
    }

    @Override
    protected void buildContent(JPanel contentPanel) {
        JPanel proxySection = createModernSection(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_TITLE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_DESCRIPTION)
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
        proxyModeComboBox = new JComboBox<>(new String[]{
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_MODE_MANUAL),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_MODE_SYSTEM)
        });
        proxyModeComboBox.setSelectedIndex(SettingManager.isSystemProxyMode() ? 1 : 0);
        JPanel proxyModeRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_MODE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_MODE_TOOLTIP),
                proxyModeComboBox
        );
        proxySection.add(proxyModeRow);
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
        proxyAuthHintArea = createInfoTextArea();
        proxySection.add(proxyAuthHintArea);
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
        proxySection.add(createVerticalSpace(FIELD_SPACING));
        proxyPreviewTargetField = new JTextField(10);
        proxyPreviewTargetField.setText(DEFAULT_PROXY_PREVIEW_TARGET);
        proxySection.add(createPreviewFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_PREVIEW_TARGET),
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_PREVIEW_TARGET_TOOLTIP),
                proxyPreviewTargetField
        ));
        proxySection.add(createVerticalSpace(FIELD_SPACING));
        proxyStatusArea = createInfoTextArea();
        proxySection.add(proxyStatusArea);
        contentPanel.add(proxySection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));
        setupValidators();
        updateProxyInputState();
        trackProxyFormState();
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
        proxyEnabledCheckBox.addItemListener(e -> updateProxyInputState());
        proxyModeComboBox.addItemListener(e -> updateProxyInputState());
        proxyTypeComboBox.addItemListener(e -> updateProxyInputState());
        addRefreshListeners(proxyHostField, proxyPortField, proxyUsernameField, proxyPasswordField, proxyPreviewTargetField);
        saveBtn.addActionListener(e -> saveSettings(true));
        applyBtn.addActionListener(e -> saveSettings(false));
        cancelBtn.addActionListener(e -> {
            if (confirmDiscardChanges()) {
                closeDialog();
            }
        });
    }

    private void saveSettings(boolean closeAfterSave) {
        boolean manualMode = SettingManager.isManualProxyModeValue(getSelectedProxyMode());
        if (manualMode && !validateAllFields()) {
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_VALIDATION_ERROR_MESSAGE));
            return;
        }
        try {
            SettingManager.setProxyEnabled(isProxyEnabledSelected());
            SettingManager.setProxyMode(getSelectedProxyMode());
            if (manualMode) {
                SettingManager.setProxyType((String) proxyTypeComboBox.getSelectedItem());
                SettingManager.setProxyHost(getTrimmedText(proxyHostField));
                SettingManager.setProxyPort(Integer.parseInt(getTrimmedText(proxyPortField)));
            }
            SettingManager.setProxyUsername(getTrimmedText(proxyUsernameField));
            SettingManager.setProxyPassword(new String(proxyPasswordField.getPassword()));
            SettingManager.setProxySslVerificationDisabled(sslVerificationDisabledCheckBox.isSelected());
            OkHttpClientManager.clearClientCache();
            trackProxyFormState();
            setHasUnsavedChanges(false);

            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_SUCCESS_MESSAGE));

            if (closeAfterSave) {
                closeDialog();
            }
        } catch (Exception ex) {
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_ERROR_MESSAGE) + ": " + ex.getMessage());
        }
    }

    private String getSelectedProxyMode() {
        return proxyModeComboBox.getSelectedIndex() == 1
                ? SettingManager.PROXY_MODE_SYSTEM
                : SettingManager.PROXY_MODE_MANUAL;
    }

    private void updateProxyInputState() {
        ProxyViewState state = buildViewState();
        updateProxyControls(state);
        updateInformationalAreas(state);
    }

    private JTextArea createInfoTextArea() {
        JTextArea area = new ShrinkableInfoTextArea();
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        area.setForeground(getTextSecondaryColor());
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return area;
    }

    private JPanel createPreviewFieldRow(String labelText, String tooltip, JComponent inputComponent) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(getCardBackgroundColor());
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel label = new JLabel(labelText);
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        label.setForeground(getTextPrimaryColor());
        label.setPreferredSize(new Dimension(FIELD_LABEL_WIDTH, 32));
        label.setMinimumSize(new Dimension(FIELD_LABEL_WIDTH, 32));
        label.setMaximumSize(new Dimension(FIELD_LABEL_WIDTH, 32));
        if (tooltip != null && !tooltip.isEmpty()) {
            label.setToolTipText(tooltip);
        }

        inputComponent.setPreferredSize(new Dimension(PREVIEW_ROW_WIDTH, 34));
        inputComponent.setMaximumSize(new Dimension(PREVIEW_ROW_WIDTH, 34));

        row.add(label);
        row.add(Box.createHorizontalStrut(16));
        row.add(inputComponent);
        row.add(Box.createHorizontalGlue());
        return row;
    }

    private void addRefreshListeners(JTextField... fields) {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateProxyInputState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateProxyInputState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateProxyInputState();
            }
        };
        for (JTextField field : fields) {
            field.getDocument().addDocumentListener(listener);
        }
    }

    private ProxyViewState buildViewState() {
        boolean proxyEnabled = isProxyEnabledSelected();
        boolean manualMode = SettingManager.isManualProxyModeValue(getSelectedProxyMode());
        return new ProxyViewState(
                proxyEnabled,
                manualMode,
                proxyEnabled && manualMode,
                proxyEnabled,
                proxyEnabled && !manualMode
        );
    }

    private void updateProxyControls(ProxyViewState state) {
        proxyModeComboBox.setEnabled(state.proxyEnabled());
        proxyTypeComboBox.setEnabled(state.manualFieldsEnabled());
        proxyHostField.setEnabled(state.manualFieldsEnabled());
        proxyPortField.setEnabled(state.manualFieldsEnabled());
        proxyUsernameField.setEnabled(state.authFieldsEnabled());
        proxyPasswordField.setEnabled(state.authFieldsEnabled());
        sslVerificationDisabledCheckBox.setEnabled(state.proxyEnabled());
        proxyPreviewTargetField.setEnabled(state.showSystemPreviewControls());
    }

    private void updateInformationalAreas(ProxyViewState state) {
        boolean showSystemAuthHint = state.showSystemPreviewControls();
        proxyAuthHintArea.setVisible(showSystemAuthHint);
        proxyAuthHintArea.setText(showSystemAuthHint
                ? I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_SYSTEM_AUTH_HINT)
                : "");
        proxyStatusArea.setText(buildProxyStatusText(state));
    }

    private String buildProxyStatusText(ProxyViewState state) {
        if (!state.proxyEnabled()) {
            return I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_STATUS_DISABLED);
        }
        if (state.manualMode()) {
            String host = getTrimmedText(proxyHostField);
            String port = getTrimmedText(proxyPortField);
            if (host.isEmpty() || port.isEmpty()) {
                return I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_STATUS_MANUAL_INCOMPLETE);
            }
            return I18nUtil.getMessage(
                    MessageKeys.SETTINGS_PROXY_STATUS_MANUAL_ACTIVE,
                    proxyTypeComboBox.getSelectedItem(),
                    host,
                    port
            );
        }

        return I18nUtil.getMessage(
                MessageKeys.SETTINGS_PROXY_STATUS_SYSTEM_ACTIVE,
                getPreviewTargetDisplay(),
                describeSystemPreview("http"),
                describeSystemPreview("https")
        );
    }

    private String describeSystemPreview(String scheme) {
        OkHttpClientManager.ProxyInspection inspection = OkHttpClientManager.inspectSystemProxyForUrl(
                buildPreviewUrl(scheme)
        );
        if (!inspection.resolved()) {
            return I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_STATUS_UNAVAILABLE);
        }
        if (!inspection.active()) {
            return I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_STATUS_DIRECT);
        }
        return inspection.description();
    }

    private String getPreviewTargetDisplay() {
        String target = getPreviewTargetInput();
        if (!target.contains("://")) {
            return target;
        }
        try {
            URI uri = URI.create(target);
            String authority = uri.getRawAuthority();
            if (authority == null || authority.isBlank()) {
                return target;
            }
            StringBuilder display = new StringBuilder(authority);
            String path = uri.getRawPath();
            if (path != null && !path.isBlank()) {
                display.append(path);
            }
            String query = uri.getRawQuery();
            if (query != null && !query.isBlank()) {
                display.append('?').append(query);
            }
            return display.toString();
        } catch (Exception e) {
            return target;
        }
    }

    private String getPreviewTargetInput() {
        String value = getTrimmedText(proxyPreviewTargetField);
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_PROXY_PREVIEW_TARGET;
        }
        return value;
    }

    private String buildPreviewUrl(String scheme) {
        String target = getPreviewTargetInput();
        if (!target.contains("://")) {
            return scheme + "://" + target;
        }
        try {
            URI uri = URI.create(target);
            String authority = uri.getRawAuthority();
            if (authority == null || authority.isBlank()) {
                return scheme + "://" + target;
            }
            return new URI(
                    scheme,
                    authority,
                    uri.getRawPath(),
                    uri.getRawQuery(),
                    uri.getRawFragment()
            ).toString();
        } catch (Exception e) {
            return scheme + "://" + target.replaceFirst("^[a-zA-Z][a-zA-Z0-9+.-]*://", "");
        }
    }

    private static final class ShrinkableInfoTextArea extends JTextArea {
        @Override
        public Dimension getMinimumSize() {
            Dimension minimumSize = super.getMinimumSize();
            return new Dimension(0, minimumSize.height);
        }
    }

    private boolean isProxyEnabledSelected() {
        return proxyEnabledCheckBox.isSelected();
    }

    private String getTrimmedText(JTextField field) {
        String value = field.getText();
        return value == null ? "" : value.trim();
    }

    private void closeDialog() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JDialog dialog) {
            dialog.dispose();
        }
    }

    private void trackProxyFormState() {
        originalValues.clear();
        trackComponentValue(proxyEnabledCheckBox);
        trackComponentValue(proxyModeComboBox);
        trackComponentValue(proxyTypeComboBox);
        trackComponentValue(proxyHostField);
        trackComponentValue(proxyPortField);
        trackComponentValue(proxyUsernameField);
        trackComponentValue(proxyPasswordField);
        trackComponentValue(sslVerificationDisabledCheckBox);
    }
}
