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

public class CapturePanel extends JPanel {
    private static final String SETTING_BIND_HOST = "plugin.capture.bindHost";
    private static final String SETTING_BIND_PORT = "plugin.capture.bindPort";
    private static final String SETTING_SYNC_SYSTEM_PROXY = "plugin.capture.syncSystemProxy";
    private static final String SETTING_CAPTURE_HOST_FILTER = "plugin.capture.hostFilter";
    private static final String[] COLUMNS = {"ID", "Time", "Method", "Host", "Path", "Status", "Duration(ms)", "Req(bytes)", "Resp(bytes)"};

    private final CaptureProxyService proxyService = new CaptureProxyService();
    private final MacCertificateInstallService certificateInstallService = new MacCertificateInstallService();

    private JTextField hostField;
    private JSpinner portSpinner;
    private JButton startButton;
    private JButton stopButton;
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
        captureHostsField.setToolTipText("Only capture these hosts. Use commas or spaces, for example: baidu.com, google.com");

        startButton = new JButton("Start", IconUtil.createThemed("icons/start.svg", 16, 16));
        stopButton = new JButton("Stop", IconUtil.createThemed("icons/stop.svg", 16, 16));
        clearButton = new JButton("Clear", IconUtil.createThemed("icons/clear.svg", 16, 16));
        installCaButton = new JButton("Install CA");
        openCaButton = new JButton("Open CA");
        copyCaPathButton = new JButton("Copy CA Path", IconUtil.createThemed("icons/copy.svg", 16, 16));
        syncSystemProxyCheckBox = new JCheckBox("Sync macOS Proxy", defaultSyncSystemProxy());
        syncSystemProxyCheckBox.setOpaque(false);
        syncSystemProxyCheckBox.setToolTipText("Automatically apply the capture proxy to macOS system proxy settings");

        startButton.addActionListener(e -> startProxy());
        stopButton.addActionListener(e -> stopProxy());
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

        panel.add(new JLabel("Bind"), "gapright 8");
        panel.add(hostField, "wmin 180");
        panel.add(portSpinner, "wmin 90");
        panel.add(startButton);
        panel.add(stopButton);
        panel.add(clearButton);
        panel.add(syncSystemProxyCheckBox);
        panel.add(installCaButton);
        panel.add(openCaButton);
        panel.add(copyCaPathButton);
        panel.add(statusLabel, "gapleft 12, wrap");
        panel.add(new JLabel("Capture Hosts"), "gapright 8");
        panel.add(captureHostsField, "span 8, growx, wrap");
        panel.add(caPathLabel, "span, growx, wrap");
        panel.add(caTrustLabel, "span, split 2");
        panel.add(systemProxyLabel, "gapleft 16, wrap");
        panel.add(captureFilterLabel, "span");
        return panel;
    }

    private JComponent buildContent() {
        tablePanel = new EnhancedTablePanel(COLUMNS);
        JTable table = tablePanel.getTable();
        table.getSelectionModel().addListSelectionListener(this::handleSelectionChanged);
        hideIdColumn(table);

        detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setLineWrap(false);
        detailArea.setWrapStyleWord(false);
        detailArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        detailArea.setText("""
                Capture proxy is idle.

                MVP scope:
                - HTTP explicit proxy
                - HTTPS CONNECT + MITM
                - request / response headers
                - body preview
                - timing and sizes

                Not implemented yet:
                - WebSocket frames
                - SSE event stream parsing
                """);

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
            NotificationUtil.showWarning("Bind host is required");
            return;
        }
        int port = ((Number) portSpinner.getValue()).intValue();
        String captureHostFilter = captureHostsField.getText().trim();
        try {
            boolean syncSystemProxy = syncSystemProxyCheckBox.isSelected();
            proxyService.start(host, port, syncSystemProxy, captureHostFilter);
            UserSettingsUtil.set(SETTING_BIND_HOST, host);
            UserSettingsUtil.set(SETTING_BIND_PORT, port);
            UserSettingsUtil.set(SETTING_SYNC_SYSTEM_PROXY, syncSystemProxy);
            UserSettingsUtil.set(SETTING_CAPTURE_HOST_FILTER, captureHostFilter);
            updateStatus();
            if (proxyService.isSystemProxySynced()) {
                NotificationUtil.showSuccess("Capture proxy started and macOS proxy synced to " + host + ":" + port);
            } else {
                NotificationUtil.showSuccess("Capture proxy started on " + host + ":" + port);
            }
        } catch (Exception ex) {
            NotificationUtil.showError("Failed to start capture proxy: " + ex.getMessage());
            updateStatus();
        }
    }

    private void stopProxy() {
        try {
            boolean synced = proxyService.isSystemProxySynced();
            proxyService.stop();
            updateStatus();
            NotificationUtil.showInfo(synced
                    ? "Capture proxy stopped and macOS proxy restored"
                    : "Capture proxy stopped");
        } catch (Exception ex) {
            updateStatus();
            NotificationUtil.showError("Failed to stop capture proxy cleanly: " + ex.getMessage());
        }
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
        startButton.setEnabled(!running);
        stopButton.setEnabled(running);
        hostField.setEnabled(!running);
        portSpinner.setEnabled(!running);
        captureHostsField.setEnabled(!running);
        syncSystemProxyCheckBox.setEnabled(!running && proxyService.isSystemProxySyncSupported());

        if (running) {
            statusLabel.setForeground(ModernColors.SUCCESS);
            statusLabel.setText("RUNNING  " + proxyService.listenHost() + ":" + proxyService.listenPort());
        } else {
            statusLabel.setForeground(new Color(0xB85C00));
            statusLabel.setText("STOPPED");
        }
        captureFilterLabel.setText(running
                ? proxyService.captureFilterSummary()
                : CaptureHostFilter.parse(captureHostsField.getText()).summary());

        try {
            String caPath = proxyService.rootCertificatePath();
            caPathLabel.setText("Root CA: " + caPath);
            caPathLabel.setToolTipText(caPath);
            updateCaTrustStatus(caPath);
            copyCaPathButton.setEnabled(true);
            openCaButton.setEnabled(certificateInstallService.isSupported());
            installCaButton.setEnabled(certificateInstallService.isSupported());
        } catch (Exception ex) {
            caPathLabel.setText("Root CA: unavailable");
            caPathLabel.setToolTipText(ex.getMessage());
            caTrustLabel.setText("CA Trust: unknown");
            caTrustLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            caTrustLabel.setToolTipText(ex.getMessage());
            copyCaPathButton.setEnabled(false);
            openCaButton.setEnabled(false);
            installCaButton.setEnabled(false);
        }

        if (!proxyService.isSystemProxySyncSupported()) {
            systemProxyLabel.setText("System proxy: unsupported on this OS");
            syncSystemProxyCheckBox.setSelected(false);
            syncSystemProxyCheckBox.setToolTipText("System proxy sync is only supported on macOS");
            installCaButton.setToolTipText("CA install helper is only supported on macOS");
            openCaButton.setToolTipText("CA install helper is only supported on macOS");
            caTrustLabel.setText("CA Trust: macOS only");
            caTrustLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            caTrustLabel.setToolTipText("CA trust verification is only supported on macOS");
        } else {
            systemProxyLabel.setText(proxyService.systemProxyStatus());
            syncSystemProxyCheckBox.setToolTipText("Automatically apply the capture proxy to macOS system proxy settings");
            installCaButton.setToolTipText("Install the generated root CA into macOS keychains and request admin approval if browser trust is not effective");
            openCaButton.setToolTipText("Open the generated root CA certificate file");
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
            NotificationUtil.showSuccess("Root CA path copied");
        } catch (Exception ex) {
            NotificationUtil.showError("Failed to copy root CA path: " + ex.getMessage());
        }
    }

    private void openCa() {
        try {
            String caPath = proxyService.rootCertificatePath();
            certificateInstallService.openCertificate(caPath);
            NotificationUtil.showInfo("Root CA opened");
        } catch (Exception ex) {
            NotificationUtil.showError("Failed to open root CA: " + ex.getMessage());
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
                String removedMessage = removed > 0
                        ? "Removed " + removed + " old CA entr" + (removed == 1 ? "y" : "ies") + " and installed the current root CA"
                        : "Root CA installed and trusted in macOS";
                if (systemInstallAttempted) {
                    NotificationUtil.showSuccess(removedMessage + " after system-level trust approval");
                } else if (loginInstallAttempted) {
                    NotificationUtil.showSuccess(removedMessage + " in the login keychain");
                } else {
                    NotificationUtil.showSuccess(removedMessage);
                }
            } else if (trustStatus.installed()) {
                certificateInstallService.openCertificate(caPath);
                showManualTrustGuide(caPath, trustStatus.detail());
                NotificationUtil.showWarning("Root CA is installed, but macOS trust is still not effective. In Keychain Access, confirm the certificate is trusted for SSL.");
            } else {
                certificateInstallService.openCertificate(caPath);
                showManualTrustGuide(caPath, trustStatus.detail());
                NotificationUtil.showWarning("Root CA install finished, but the certificate is still not visible in macOS keychains.");
            }
        } catch (Exception ex) {
            NotificationUtil.showError("Failed to install root CA: " + ex.getMessage());
        }
    }

    private void showManualTrustGuide(String caPath, String detail) {
        JTextArea guide = new JTextArea("""
                EasyPostman could not make the CA trusted automatically.

                JMeter-style fallback on macOS:

                1. In Keychain Access, import/open the certificate shown below.
                2. Prefer adding it to the System keychain if macOS asks.
                3. Open the certificate entry, expand Trust, and set:
                   Secure Sockets Layer (SSL) -> Always Trust
                4. Close the dialog and enter your macOS password if prompted.
                5. Fully restart the browser and test HTTPS again.

                Certificate path:
                """ + caPath + "\n\nCurrent trust status:\n" + detail);
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
                "Install Root CA",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void updateCaTrustStatus(String caPath) {
        if (!certificateInstallService.isSupported()) {
            caTrustLabel.setText("CA Trust: macOS only");
            caTrustLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            caTrustLabel.setToolTipText("CA trust verification is only supported on macOS");
            return;
        }
        try {
            MacCertificateInstallService.CertificateTrustStatus trustStatus = certificateInstallService.trustStatus(caPath);
            caTrustLabel.setToolTipText(trustStatus.detail());
            if (trustStatus.trusted()) {
                caTrustLabel.setText("CA Trust: trusted");
                caTrustLabel.setForeground(ModernColors.SUCCESS_DARK);
                return;
            }
            if (trustStatus.installed()) {
                caTrustLabel.setText("CA Trust: installed, verify trust");
                caTrustLabel.setForeground(ModernColors.WARNING_DARKER);
                return;
            }
            caTrustLabel.setText("CA Trust: not installed");
            caTrustLabel.setForeground(ModernColors.WARNING_DARKER);
        } catch (Exception ex) {
            caTrustLabel.setText("CA Trust: unknown");
            caTrustLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            caTrustLabel.setToolTipText(ex.getMessage());
        }
    }
}
