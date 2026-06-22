package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.common.component.EasyPasswordField;
import com.laker.postman.common.component.notification.NotificationCenter;
import com.laker.postman.common.component.setting.SettingsFieldRow;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.service.sync.WebDavRemoteSnapshot;
import com.laker.postman.service.sync.WebDavSyncService;
import com.laker.postman.service.sync.WebDavSyncSettings;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import okhttp3.HttpUrl;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

public class WebDavSyncSettingsPanel extends ModernSettingsPanel {
    private static final int FIELD_SPACING = 8;
    private static final int SECTION_SPACING = 12;
    private static final int ACTION_GAP = 8;
    private static final int ACTION_BUTTON_HEIGHT = 34;
    private static final int ACTION_BUTTON_MIN_WIDTH = 96;
    private static final int ACTION_BUTTON_HORIZONTAL_PADDING = 36;

    private final WebDavSyncService syncService = new WebDavSyncService();

    private JCheckBox enabledCheckBox;
    private JTextField serverUrlField;
    private JTextField remoteDirectoryField;
    private JTextField usernameField;
    private EasyPasswordField passwordField;
    private JLabel lastSyncLabel;
    private JTextArea statusArea;
    private JButton testConnectionButton;
    private JButton uploadButton;
    private JButton restoreButton;
    private SettingsFieldRow serverUrlRow;
    private SettingsFieldRow remoteDirectoryRow;
    private SettingsFieldRow usernameRow;
    private SettingsFieldRow passwordRow;

    @Override
    protected void buildContent(JPanel contentPanel) {
        JPanel section = createModernSection(
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_TITLE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_DESCRIPTION)
        );

        WebDavSyncSettings settings = SettingManager.getWebDavSyncSettings();
        enabledCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_ENABLED_CHECKBOX),
                settings.enabled()
        );
        section.add(createCheckBoxRow(
                enabledCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_ENABLED_TOOLTIP)
        ));
        section.add(createVerticalSpace(FIELD_SPACING));

        serverUrlField = new JTextField(settings.serverUrl(), 24);
        serverUrlRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_SERVER_URL),
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_SERVER_URL_TOOLTIP),
                serverUrlField
        );
        section.add(serverUrlRow);
        section.add(createVerticalSpace(FIELD_SPACING));

        remoteDirectoryField = new JTextField(settings.remoteDirectory(), 24);
        remoteDirectoryRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_REMOTE_DIRECTORY),
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_REMOTE_DIRECTORY_TOOLTIP),
                remoteDirectoryField
        );
        section.add(remoteDirectoryRow);
        section.add(createVerticalSpace(FIELD_SPACING));

        usernameField = new JTextField(settings.username(), 24);
        usernameRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_USERNAME),
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_USERNAME_TOOLTIP),
                usernameField
        );
        section.add(usernameRow);
        section.add(createVerticalSpace(FIELD_SPACING));

        passwordField = new EasyPasswordField(24);
        passwordField.setText(settings.password());
        passwordRow = createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_PASSWORD),
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_PASSWORD_TOOLTIP),
                passwordField
        );
        section.add(passwordRow);
        section.add(createVerticalSpace(FIELD_SPACING));

        lastSyncLabel = new JLabel(formatLastSyncTime(SettingManager.getWebDavSyncLastSyncTime()));
        section.add(createFieldRow(
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_LAST_SYNC_TIME),
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_LAST_SYNC_TIME_TOOLTIP),
                lastSyncLabel
        ));
        section.add(createVerticalSpace(FIELD_SPACING));

        JPanel actionRow = createActionRow();
        section.add(actionRow);
        section.add(createVerticalSpace(FIELD_SPACING));

        statusArea = createInfoTextArea();
        section.add(statusArea);

        contentPanel.add(section);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));

        trackWebDavFormState();
        updateControlState();
    }

    @Override
    protected void registerListeners() {
        enabledCheckBox.addItemListener(e -> updateControlState());
        addRefreshListeners(serverUrlField, remoteDirectoryField, usernameField, passwordField);
        saveBtn.addActionListener(e -> saveSettings(true));
        applyBtn.addActionListener(e -> saveSettings(false));
        cancelBtn.addActionListener(e -> {
            if (confirmDiscardChanges()) {
                closeDialog();
            }
        });
        testConnectionButton.addActionListener(e -> runTestConnection());
        uploadButton.addActionListener(e -> runUpload());
        restoreButton.addActionListener(e -> runRestore());
        registerSaveShortcut(() -> saveSettings(false));
    }

    private JPanel createActionRow() {
        JPanel actionRow = new JPanel();
        actionRow.setLayout(new BoxLayout(actionRow, BoxLayout.X_AXIS));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, ACTION_BUTTON_HEIGHT));
        testConnectionButton = createActionButton(
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_ACTION_TEST),
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_ACTION_TEST_TOOLTIP),
                false
        );
        uploadButton = createActionButton(
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_ACTION_UPLOAD),
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_ACTION_UPLOAD_TOOLTIP),
                true
        );
        restoreButton = createActionButton(
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_ACTION_RESTORE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_ACTION_RESTORE_TOOLTIP),
                false
        );
        actionRow.add(Box.createHorizontalStrut(SettingsFieldRow.DEFAULT_LABEL_WIDTH + SettingsFieldRow.DEFAULT_GAP));
        actionRow.add(testConnectionButton);
        actionRow.add(Box.createHorizontalStrut(ACTION_GAP));
        actionRow.add(uploadButton);
        actionRow.add(Box.createHorizontalStrut(ACTION_GAP));
        actionRow.add(restoreButton);
        actionRow.add(Box.createHorizontalGlue());
        return actionRow;
    }

    private JButton createActionButton(String text, String tooltip, boolean primary) {
        JButton button = createModernButton(text, primary);
        button.setToolTipText(tooltip);
        Dimension size = actionButtonSize(button);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        return button;
    }

    private Dimension actionButtonSize(AbstractButton button) {
        int textWidth = button.getFontMetrics(button.getFont()).stringWidth(button.getText());
        int width = Math.max(ACTION_BUTTON_MIN_WIDTH, textWidth + ACTION_BUTTON_HORIZONTAL_PADDING);
        return new Dimension(width, ACTION_BUTTON_HEIGHT);
    }

    private JTextArea createInfoTextArea() {
        JTextArea area = new JTextArea();
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

    private void addRefreshListeners(JTextField... fields) {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateControlState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateControlState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateControlState();
            }
        };
        for (JTextField field : fields) {
            field.getDocument().addDocumentListener(listener);
        }
    }

    private void saveSettings(boolean closeAfterSave) {
        if (enabledCheckBox.isSelected() && !validateEndpoint()) {
            return;
        }
        try {
            SettingManager.setWebDavSyncSettings(formSettings());
            trackWebDavFormState();
            setHasUnsavedChanges(false);
            NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_SUCCESS_MESSAGE));
            if (closeAfterSave) {
                closeDialog();
            }
        } catch (Exception ex) {
            NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_ERROR_MESSAGE) + ": " + ex.getMessage());
        }
    }

    private void runTestConnection() {
        if (!validateEndpoint()) {
            return;
        }
        if (!saveSettingsForAction()) {
            return;
        }
        WebDavSyncSettings settings = formSettings();
        runAsync(
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_STATUS_TESTING),
                () -> {
                    syncService.testConnection(settings);
                    return null;
                },
                ignored -> {
                    setStatus(I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_TEST_SUCCESS));
                    NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_TEST_SUCCESS));
                }
        );
    }

    private void runUpload() {
        if (!validateEndpoint()) {
            return;
        }
        if (!saveSettingsForAction()) {
            return;
        }
        WebDavSyncSettings settings = formSettings();
        runAsync(
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_STATUS_CHECKING_REMOTE),
                () -> syncService.fetchRemoteSnapshot(settings),
                remoteSnapshot -> {
                    if (confirmUpload(remoteSnapshot)) {
                        performUpload(settings);
                    } else {
                        updateControlState();
                    }
                }
        );
    }

    private void performUpload(WebDavSyncSettings settings) {
        runAsync(
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_STATUS_UPLOADING),
                () -> {
                    syncService.uploadSnapshot(settings);
                    return Instant.now().toEpochMilli();
                },
                timestamp -> {
                    SettingManager.setWebDavSyncLastSyncTime(timestamp);
                    lastSyncLabel.setText(formatLastSyncTime(timestamp));
                    setStatus(I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_UPLOAD_SUCCESS));
                    NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_UPLOAD_SUCCESS));
                }
        );
    }

    private void runRestore() {
        if (!validateEndpoint()) {
            return;
        }
        if (!saveSettingsForAction()) {
            return;
        }
        WebDavSyncSettings settings = formSettings();
        runAsync(
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_STATUS_CHECKING_REMOTE),
                () -> syncService.fetchRemoteSnapshot(settings),
                remoteSnapshot -> {
                    if (confirmRestore(remoteSnapshot)) {
                        performRestore(settings);
                    } else {
                        updateControlState();
                    }
                }
        );
    }

    private void performRestore(WebDavSyncSettings settings) {
        runAsync(
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_STATUS_RESTORING),
                () -> syncService.restoreSnapshot(settings),
                result -> {
                    long timestamp = Instant.now().toEpochMilli();
                    SettingManager.setWebDavSyncLastSyncTime(timestamp);
                    lastSyncLabel.setText(formatLastSyncTime(timestamp));
                    setStatus(I18nUtil.getMessage(
                            MessageKeys.SETTINGS_WEBDAV_SYNC_RESTORE_SUCCESS,
                            result.backupPath().toString()
                    ));
                    NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_RESTORE_SUCCESS_SHORT));
                    showRestoreRestartPrompt(result.backupPath().toString());
                }
        );
    }

    private boolean saveSettingsForAction() {
        try {
            SettingManager.setWebDavSyncSettings(formSettings());
            trackWebDavFormState();
            setHasUnsavedChanges(false);
            return true;
        } catch (Exception ex) {
            NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_ERROR_MESSAGE) + ": " + ex.getMessage());
            return false;
        }
    }

    private void showRestoreRestartPrompt(String backupPath) {
        JOptionPane.showMessageDialog(
                this,
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_RESTORE_RESTART_MESSAGE, backupPath),
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_RESTORE_RESTART_TITLE),
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private <T> void runAsync(String busyMessage, SyncTask<T> task, SyncSuccess<T> success) {
        setBusy(true);
        setStatus(busyMessage);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.run();
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    success.accept(get());
                } catch (Exception ex) {
                    String message = rootMessage(ex);
                    setStatus(I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_ACTION_FAILED, message));
                    NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_ACTION_FAILED, message));
                }
            }
        }.execute();
    }

    private boolean validateEndpoint() {
        WebDavSyncSettings settings = formSettings();
        if (!settings.hasEndpoint()) {
            NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_VALIDATION_REQUIRED));
            return false;
        }
        HttpUrl parsedUrl = HttpUrl.parse(settings.serverUrl());
        if (parsedUrl == null || !isHttpScheme(parsedUrl.scheme())) {
            NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_VALIDATION_URL));
            serverUrlField.requestFocus();
            return false;
        }
        if (shouldWarnInsecureHttp(parsedUrl)) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_VALIDATION_HTTP_WARNING));
        }
        return true;
    }

    private boolean confirmUpload(Optional<WebDavRemoteSnapshot> remoteSnapshot) {
        int result = JOptionPane.showConfirmDialog(
                this,
                I18nUtil.getMessage(
                        MessageKeys.SETTINGS_WEBDAV_SYNC_UPLOAD_CONFIRM_MESSAGE,
                        remoteSnapshotText(remoteSnapshot)
                ),
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_UPLOAD_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    private boolean confirmRestore(Optional<WebDavRemoteSnapshot> remoteSnapshot) {
        int result = JOptionPane.showConfirmDialog(
                this,
                I18nUtil.getMessage(
                        MessageKeys.SETTINGS_WEBDAV_SYNC_RESTORE_CONFIRM_MESSAGE,
                        remoteSnapshotText(remoteSnapshot)
                ),
                I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_RESTORE_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    private String remoteSnapshotText(Optional<WebDavRemoteSnapshot> remoteSnapshot) {
        if (remoteSnapshot == null || remoteSnapshot.isEmpty()) {
            return I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_REMOTE_SNAPSHOT_NONE);
        }
        WebDavRemoteSnapshot snapshot = remoteSnapshot.get();
        return I18nUtil.getMessage(
                MessageKeys.SETTINGS_WEBDAV_SYNC_REMOTE_SNAPSHOT_DETAIL,
                valueOrUnknown(formatRemoteCreatedAt(snapshot.createdAt())),
                valueOrUnknown(snapshot.appVersion()),
                formatBytes(snapshot.snapshotBytes())
        );
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank()
                ? I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_REMOTE_SNAPSHOT_UNKNOWN)
                : value;
    }

    private String formatRemoteCreatedAt(String createdAt) {
        if (createdAt == null || createdAt.isBlank()) {
            return "";
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date.from(Instant.parse(createdAt)));
        } catch (Exception e) {
            return createdAt;
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unitIndex]);
    }

    private void updateControlState() {
        boolean enabled = enabledCheckBox.isSelected();
        boolean hasEndpoint = formSettings().hasEndpoint();
        serverUrlRow.setEnabled(enabled);
        remoteDirectoryRow.setEnabled(enabled);
        usernameRow.setEnabled(enabled);
        passwordRow.setEnabled(enabled);
        testConnectionButton.setEnabled(enabled && hasEndpoint);
        uploadButton.setEnabled(enabled && hasEndpoint);
        restoreButton.setEnabled(enabled && hasEndpoint);
        if (!enabled) {
            setStatus(I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_STATUS_DISABLED));
        } else if (!hasEndpoint) {
            setStatus(I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_STATUS_NOT_CONFIGURED));
        } else {
            setStatus(I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_STATUS_READY));
        }
    }

    private void setBusy(boolean busy) {
        boolean actionsEnabled = !busy && enabledCheckBox.isSelected() && formSettings().hasEndpoint();
        testConnectionButton.setEnabled(actionsEnabled);
        uploadButton.setEnabled(actionsEnabled);
        restoreButton.setEnabled(actionsEnabled);
        saveBtn.setEnabled(!busy);
        cancelBtn.setEnabled(!busy);
        applyBtn.setEnabled(!busy && hasUnsavedChanges());
    }

    private WebDavSyncSettings formSettings() {
        return new WebDavSyncSettings(
                enabledCheckBox.isSelected(),
                text(serverUrlField),
                text(remoteDirectoryField),
                text(usernameField),
                new String(passwordField.getPassword())
        );
    }

    private String text(JTextField field) {
        String value = field.getText();
        return value == null ? "" : value.trim();
    }

    private String formatLastSyncTime(long timestamp) {
        return timestamp > 0
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp))
                : I18nUtil.getMessage(MessageKeys.SETTINGS_WEBDAV_SYNC_NEVER_SYNCED);
    }

    private void setStatus(String status) {
        if (statusArea != null) {
            statusArea.setText(status == null ? "" : status);
        }
    }

    private void trackWebDavFormState() {
        originalValues.clear();
        trackComponentValue(enabledCheckBox);
        trackComponentValue(serverUrlField);
        trackComponentValue(remoteDirectoryField);
        trackComponentValue(usernameField);
        trackComponentValue(passwordField);
    }

    private void closeDialog() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JDialog dialog) {
            dialog.dispose();
        }
    }

    private static boolean isHttpScheme(String scheme) {
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    static boolean shouldWarnInsecureHttp(HttpUrl url) {
        if (url == null || !"http".equalsIgnoreCase(url.scheme())) {
            return false;
        }
        String host = url.host() == null ? "" : url.host().toLowerCase(Locale.ROOT);
        return !("localhost".equals(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host)
                || "0:0:0:0:0:0:0:1".equals(host));
    }

    private static String rootMessage(Exception ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    @FunctionalInterface
    private interface SyncTask<T> {
        T run() throws Exception;
    }

    @FunctionalInterface
    private interface SyncSuccess<T> {
        void accept(T result);
    }
}
