package com.laker.postman.plugin.capture;

import com.laker.postman.common.component.table.EnhancedTablePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.NotificationUtil;
import com.laker.postman.util.UserSettingsUtil;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

public class CapturePanel extends JPanel {
    private static final String SETTING_BIND_HOST = "plugin.capture.bindHost";
    private static final String SETTING_BIND_PORT = "plugin.capture.bindPort";
    private static final String SETTING_SYNC_SYSTEM_PROXY = "plugin.capture.syncSystemProxy";
    private static final String SETTING_CAPTURE_HOST_FILTER = "plugin.capture.hostFilter";

    private final CaptureProxyService proxyService = new CaptureProxyService();
    private final MacCertificateInstallService certificateInstallService = new MacCertificateInstallService();

    private JTextField hostField;
    private JSpinner portSpinner;
    private JButton toggleProxyButton;
    private JButton clearButton;
    private JButton installCaButton;
    private JButton openCaButton;
    private JButton copyCaPathButton;
    private JCheckBox syncSystemProxyCheckBox;
    private JTextField captureHostsField;
    private JLabel statusLabel;
    private JLabel caPathLabel;
    private JLabel caTrustLabel;
    private JLabel systemProxyLabel;
    private JLabel captureFilterLabel;
    private EnhancedTablePanel tablePanel;
    private JTextArea detailArea;
    private boolean operationInProgress;

    public CapturePanel() {
        initUI();
        proxyService.sessionStore().addChangeListener(() -> SwingUtilities.invokeLater(this::refreshTable));
        refreshTable();
        updateStatus();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
    }

    private JComponent buildTopBar() {
        JPanel panel = new JPanel(new MigLayout(
                "insets 8, fillx, novisualpadding",
                "[][grow,fill]12[]12[]6[]12[]6[]6[]push[]",
                "[][][]"));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(0, 0, 8, 0)));

        hostField = new JTextField(defaultHost());
        hostField.setColumns(16);
        portSpinner = new JSpinner(new SpinnerNumberModel(defaultPort(), 1, 65535, 1));
        ((JSpinner.DefaultEditor) portSpinner.getEditor()).getTextField().setColumns(6);
        captureHostsField = new JTextField(defaultCaptureHostFilter());
        captureHostsField.setColumns(28);
        captureHostsField.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_HOSTS_TOOLTIP));

        toggleProxyButton = new JButton();
        clearButton = new JButton(t(MessageKeys.TOOLBOX_CAPTURE_CLEAR), IconUtil.createThemed("icons/clear.svg", 16, 16));
        installCaButton = new JButton(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA));
        openCaButton = new JButton(t(MessageKeys.TOOLBOX_CAPTURE_OPEN_CA));
        copyCaPathButton = new JButton(t(MessageKeys.TOOLBOX_CAPTURE_COPY_CA_PATH), IconUtil.createThemed("icons/copy.svg", 16, 16));
        syncSystemProxyCheckBox = new JCheckBox(t(MessageKeys.TOOLBOX_CAPTURE_SYNC_MACOS_PROXY), defaultSyncSystemProxy());
        syncSystemProxyCheckBox.setOpaque(false);
        syncSystemProxyCheckBox.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_SYNC_PROXY_TOOLTIP));

        toggleProxyButton.addActionListener(e -> {
            if (proxyService.isRunning()) {
                stopProxy();
            } else {
                startProxy();
            }
        });
        clearButton.addActionListener(e -> proxyService.sessionStore().clear());
        installCaButton.addActionListener(e -> installCa());
        openCaButton.addActionListener(e -> openCa());
        copyCaPathButton.addActionListener(e -> copyCaPath());

        statusLabel = new JLabel();
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1));
        caPathLabel = new JLabel();
        caPathLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        caPathLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        caTrustLabel = new JLabel();
        caTrustLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        systemProxyLabel = new JLabel();
        systemProxyLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        systemProxyLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        captureFilterLabel = new JLabel();
        captureFilterLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        captureFilterLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        panel.add(new JLabel(t(MessageKeys.TOOLBOX_CAPTURE_BIND)), "gapright 8");
        panel.add(hostField, "wmin 180");
        panel.add(portSpinner, "wmin 90");
        panel.add(toggleProxyButton, "wmin 110");
        panel.add(clearButton);
        panel.add(syncSystemProxyCheckBox);
        panel.add(installCaButton);
        panel.add(openCaButton);
        panel.add(copyCaPathButton);
        panel.add(statusLabel, "gapleft 12, wrap");
        panel.add(new JLabel(t(MessageKeys.TOOLBOX_CAPTURE_CAPTURE_HOSTS)), "gapright 8");
        panel.add(captureHostsField, "span 8, growx, wrap");
        panel.add(caPathLabel, "span, growx, wrap");
        panel.add(caTrustLabel, "span, split 2");
        panel.add(systemProxyLabel, "gapleft 16, wrap");
        panel.add(captureFilterLabel, "span, growx");
        return panel;
    }

    private JComponent buildContent() {
        tablePanel = new EnhancedTablePanel(columnNames());
        JTable table = tablePanel.getTable();
        table.getSelectionModel().addListSelectionListener(this::handleSelectionChanged);
        hideIdColumn(table);

        detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setLineWrap(false);
        detailArea.setWrapStyleWord(false);
        detailArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        detailArea.setText(t(MessageKeys.TOOLBOX_CAPTURE_IDLE_DETAILS));

        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setPreferredSize(new Dimension(360, 200));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, detailScroll);
        splitPane.setResizeWeight(0.68);
        splitPane.setDividerSize(3);
        splitPane.setContinuousLayout(true);
        return splitPane;
    }

    private void startProxy() {
        String host = hostField.getText().trim();
        if (host.isBlank()) {
            NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_WARN_BIND_HOST_REQUIRED));
            return;
        }
        if (operationInProgress) {
            return;
        }
        int port = ((Number) portSpinner.getValue()).intValue();
        boolean syncSystemProxy = syncSystemProxyCheckBox.isSelected();
        String captureHostFilter = captureHostsField.getText().trim();

        setOperationState(true);
        SwingWorker<StartResult, Void> worker = new SwingWorker<>() {
            @Override
            protected StartResult doInBackground() throws Exception {
                proxyService.start(host, port, syncSystemProxy, captureHostFilter);
                UserSettingsUtil.set(SETTING_BIND_HOST, host);
                UserSettingsUtil.set(SETTING_BIND_PORT, port);
                UserSettingsUtil.set(SETTING_SYNC_SYSTEM_PROXY, syncSystemProxy);
                UserSettingsUtil.set(SETTING_CAPTURE_HOST_FILTER, captureHostFilter);
                return new StartResult(host, port, proxyService.isSystemProxySynced());
            }

            @Override
            protected void done() {
                setOperationState(false);
                try {
                    StartResult result = get();
                    updateStatus();
                    NotificationUtil.showSuccess(result.systemProxySynced()
                            ? t(MessageKeys.TOOLBOX_CAPTURE_START_SUCCESS_SYNCED, result.host(), result.port())
                            : t(MessageKeys.TOOLBOX_CAPTURE_START_SUCCESS, result.host(), result.port()));
                } catch (Exception ex) {
                    updateStatus();
                    NotificationUtil.showError(t(MessageKeys.TOOLBOX_CAPTURE_START_FAILED, rootMessage(ex)));
                }
            }
        };
        worker.execute();
    }

    private void stopProxy() {
        if (operationInProgress) {
            return;
        }
        setOperationState(true);
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                boolean synced = proxyService.isSystemProxySynced();
                proxyService.stop();
                return synced;
            }

            @Override
            protected void done() {
                setOperationState(false);
                try {
                    boolean synced = get();
                    updateStatus();
                    NotificationUtil.showInfo(synced
                            ? t(MessageKeys.TOOLBOX_CAPTURE_STOP_SUCCESS_SYNCED)
                            : t(MessageKeys.TOOLBOX_CAPTURE_STOP_SUCCESS));
                } catch (Exception ex) {
                    updateStatus();
                    NotificationUtil.showError(t(MessageKeys.TOOLBOX_CAPTURE_STOP_FAILED, rootMessage(ex)));
                }
            }
        };
        worker.execute();
    }

    private void refreshTable() {
        List<Object[]> rows = new ArrayList<>();
        for (CaptureFlow flow : proxyService.sessionStore().snapshot()) {
            rows.add(flow.toRow());
        }
        tablePanel.setData(rows);
        hideIdColumn(tablePanel.getTable());
        updateStatus();
    }

    private void handleSelectionChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        JTable table = tablePanel.getTable();
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        Object flowId = table.getValueAt(selectedRow, 0);
        CaptureFlow flow = proxyService.sessionStore().find(String.valueOf(flowId));
        if (flow != null) {
            detailArea.setText(flow.detailText());
            detailArea.setCaretPosition(0);
        }
    }

    private void updateStatus() {
        boolean running = proxyService.isRunning();
        boolean busy = operationInProgress;
        updateToggleProxyButton(running, busy);
        clearButton.setEnabled(!busy);
        hostField.setEnabled(!busy && !running);
        portSpinner.setEnabled(!busy && !running);
        captureHostsField.setEnabled(!busy && !running);
        syncSystemProxyCheckBox.setEnabled(!busy && !running && proxyService.isSystemProxySyncSupported());

        if (running) {
            statusLabel.setForeground(ModernColors.SUCCESS);
            statusLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_STATUS_RUNNING, proxyService.listenHost(), proxyService.listenPort()));
        } else {
            statusLabel.setForeground(new Color(0xB85C00));
            statusLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_STATUS_STOPPED));
        }
        captureFilterLabel.setText(running
                ? proxyService.captureFilterSummary()
                : CaptureHostFilter.parse(captureHostsField.getText()).summary());

        try {
            String caPath = proxyService.rootCertificatePath();
            caPathLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_CA_PATH, caPath));
            caPathLabel.setToolTipText(caPath);
            updateCaTrustStatus(caPath);
            copyCaPathButton.setEnabled(!busy);
            openCaButton.setEnabled(!busy && certificateInstallService.isSupported());
            installCaButton.setEnabled(!busy && certificateInstallService.isSupported());
        } catch (Exception ex) {
            caPathLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_CA_PATH_UNAVAILABLE));
            caPathLabel.setToolTipText(ex.getMessage());
            caTrustLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_UNKNOWN));
            caTrustLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            caTrustLabel.setToolTipText(ex.getMessage());
            copyCaPathButton.setEnabled(false);
            openCaButton.setEnabled(false);
            installCaButton.setEnabled(false);
        }

        if (!proxyService.isSystemProxySyncSupported()) {
            systemProxyLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_SYSTEM_PROXY_UNSUPPORTED));
            syncSystemProxyCheckBox.setSelected(false);
            syncSystemProxyCheckBox.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_SYNC_PROXY_TOOLTIP_UNSUPPORTED));
            installCaButton.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_TOOLTIP_UNSUPPORTED));
            openCaButton.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_OPEN_CA_TOOLTIP_UNSUPPORTED));
            caTrustLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_MACOS_ONLY));
            caTrustLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            caTrustLabel.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_TOOLTIP_MACOS_ONLY));
        } else {
            systemProxyLabel.setText(proxyService.isSystemProxySynced()
                    ? t(MessageKeys.TOOLBOX_CAPTURE_SYSTEM_PROXY_SYNCED, proxyService.listenHost(), proxyService.listenPort())
                    : t(MessageKeys.TOOLBOX_CAPTURE_SYSTEM_PROXY_MANUAL));
            syncSystemProxyCheckBox.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_SYNC_PROXY_TOOLTIP));
            installCaButton.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_TOOLTIP));
            openCaButton.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_OPEN_CA_TOOLTIP));
        }
    }

    private void hideIdColumn(JTable table) {
        if (table.getColumnModel().getColumnCount() == 0) {
            return;
        }
        TableColumn idColumn = table.getColumnModel().getColumn(0);
        idColumn.setMinWidth(0);
        idColumn.setMaxWidth(0);
        idColumn.setPreferredWidth(0);
        idColumn.setResizable(false);
    }

    private String defaultHost() {
        String saved = UserSettingsUtil.getString(SETTING_BIND_HOST);
        return saved == null || saved.isBlank() ? "127.0.0.1" : saved;
    }

    private int defaultPort() {
        Integer saved = UserSettingsUtil.getInt(SETTING_BIND_PORT);
        return saved == null ? 8888 : saved;
    }

    private boolean defaultSyncSystemProxy() {
        return Boolean.TRUE.equals(UserSettingsUtil.getBoolean(SETTING_SYNC_SYSTEM_PROXY));
    }

    private String defaultCaptureHostFilter() {
        String saved = UserSettingsUtil.getString(SETTING_CAPTURE_HOST_FILTER);
        return saved == null ? "" : saved;
    }

    private void copyCaPath() {
        try {
            String caPath = proxyService.rootCertificatePath();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(caPath), null);
            NotificationUtil.showSuccess(t(MessageKeys.TOOLBOX_CAPTURE_COPY_CA_PATH_SUCCESS));
        } catch (Exception ex) {
            NotificationUtil.showError(t(MessageKeys.TOOLBOX_CAPTURE_COPY_CA_PATH_FAILED, ex.getMessage()));
        }
    }

    private void openCa() {
        try {
            String caPath = proxyService.rootCertificatePath();
            certificateInstallService.openCertificate(caPath);
            NotificationUtil.showInfo(t(MessageKeys.TOOLBOX_CAPTURE_OPEN_CA_SUCCESS));
        } catch (Exception ex) {
            NotificationUtil.showError(t(MessageKeys.TOOLBOX_CAPTURE_OPEN_CA_FAILED, ex.getMessage()));
        }
    }

    private void installCa() {
        try {
            String caPath = proxyService.rootCertificatePath();
            int removed = certificateInstallService.removeMatchingLoginKeychainCertificates(caPath);
            boolean systemInstallAttempted = false;
            boolean loginInstallAttempted = false;
            MacCertificateInstallService.CertificateTrustStatus trustStatus = certificateInstallService.trustStatus(caPath);
            if (!trustStatus.trusted()) {
                try {
                    certificateInstallService.installToSystemKeychainWithPrompt(caPath);
                    systemInstallAttempted = true;
                } catch (Exception ignored) {
                    // Fall back to login-keychain installation below.
                }
                trustStatus = certificateInstallService.trustStatus(caPath);
            }
            if (!trustStatus.trusted()) {
                certificateInstallService.installToLoginKeychain(caPath);
                loginInstallAttempted = true;
                trustStatus = certificateInstallService.trustStatus(caPath);
            }
            updateStatus();
            certificateInstallService.openKeychainAccess();
            if (trustStatus.trusted()) {
                String removedMessage = removed > 1
                        ? t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_REMOVED_MULTI, removed)
                        : removed == 1
                        ? t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_REMOVED_SINGLE)
                        : t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_REMOVED_NONE);
                if (systemInstallAttempted) {
                    NotificationUtil.showSuccess(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_SUCCESS_SYSTEM, removedMessage));
                } else if (loginInstallAttempted) {
                    NotificationUtil.showSuccess(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_SUCCESS_LOGIN, removedMessage));
                } else {
                    NotificationUtil.showSuccess(removedMessage);
                }
            } else if (trustStatus.installed()) {
                certificateInstallService.openCertificate(caPath);
                showManualTrustGuide(caPath, trustStatus.detail());
                NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_WARN_TRUST));
            } else {
                certificateInstallService.openCertificate(caPath);
                showManualTrustGuide(caPath, trustStatus.detail());
                NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_WARN_VISIBLE));
            }
        } catch (Exception ex) {
            NotificationUtil.showError(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_FAILED, ex.getMessage()));
        }
    }

    private void showManualTrustGuide(String caPath, String detail) {
        JTextArea guide = new JTextArea(t(MessageKeys.TOOLBOX_CAPTURE_MANUAL_TRUST_GUIDE, caPath, detail));
        guide.setEditable(false);
        guide.setLineWrap(true);
        guide.setWrapStyleWord(true);
        guide.setCaretPosition(0);
        guide.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        guide.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(guide);
        scrollPane.setPreferredSize(new Dimension(560, 260));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                t(MessageKeys.TOOLBOX_CAPTURE_MANUAL_TRUST_TITLE),
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void updateCaTrustStatus(String caPath) {
        if (!certificateInstallService.isSupported()) {
            caTrustLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_MACOS_ONLY));
            caTrustLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            caTrustLabel.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_TOOLTIP_MACOS_ONLY));
            return;
        }
        try {
            MacCertificateInstallService.CertificateTrustStatus trustStatus = certificateInstallService.trustStatus(caPath);
            caTrustLabel.setToolTipText(trustStatus.detail());
            if (trustStatus.trusted()) {
                caTrustLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_TRUSTED));
                caTrustLabel.setForeground(ModernColors.SUCCESS_DARK);
                return;
            }
            if (trustStatus.installed()) {
                caTrustLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_VERIFY));
                caTrustLabel.setForeground(ModernColors.WARNING_DARKER);
                return;
            }
            caTrustLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_NOT_INSTALLED));
            caTrustLabel.setForeground(ModernColors.WARNING_DARKER);
        } catch (Exception ex) {
            caTrustLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_UNKNOWN));
            caTrustLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            caTrustLabel.setToolTipText(ex.getMessage());
        }
    }

    private void setOperationState(boolean busy) {
        operationInProgress = busy;
        updateStatus();
    }

    private String[] columnNames() {
        return new String[]{
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_ID),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_TIME),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_METHOD),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_HOST),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_PATH),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_STATUS),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_DURATION_MS),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_REQ_BYTES),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_RESP_BYTES)
        };
    }

    private String rootMessage(Exception ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? ex.getMessage() : current.getMessage();
    }

    private record StartResult(String host, int port, boolean systemProxySynced) {
    }

    private void updateToggleProxyButton(boolean running, boolean busy) {
        if (busy) {
            if (running) {
                toggleProxyButton.setText(t(MessageKeys.TOOLBOX_CAPTURE_STOPPING));
                toggleProxyButton.setIcon(IconUtil.createThemed("icons/stop.svg", 16, 16));
            } else {
                toggleProxyButton.setText(t(MessageKeys.TOOLBOX_CAPTURE_STARTING));
                toggleProxyButton.setIcon(IconUtil.createThemed("icons/start.svg", 16, 16));
            }
            toggleProxyButton.setEnabled(false);
            return;
        }
        if (running) {
            toggleProxyButton.setText(t(MessageKeys.TOOLBOX_CAPTURE_STOP));
            toggleProxyButton.setIcon(IconUtil.createThemed("icons/stop.svg", 16, 16));
        } else {
            toggleProxyButton.setText(t(MessageKeys.TOOLBOX_CAPTURE_START));
            toggleProxyButton.setIcon(IconUtil.createThemed("icons/start.svg", 16, 16));
        }
        toggleProxyButton.setEnabled(true);
    }
}
