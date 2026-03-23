package com.laker.postman.plugin.capture;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.button.CloseButton;
import com.laker.postman.common.component.button.CopyButton;
import com.laker.postman.common.component.table.EnhancedTablePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.NotificationUtil;
import com.laker.postman.util.UserSettingsUtil;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

public class CapturePanel extends JPanel {
    private static final String SETTING_BIND_HOST = "plugin.capture.bindHost";
    private static final String SETTING_BIND_PORT = "plugin.capture.bindPort";
    private static final String SETTING_SYNC_SYSTEM_PROXY = "plugin.capture.syncSystemProxy";
    private static final String SETTING_CAPTURE_HOST_FILTER = "plugin.capture.hostFilter";

    private final CaptureProxyService proxyService = CaptureRuntime.proxyService();
    private final MacCertificateInstallService macCertificateInstallService = new MacCertificateInstallService();
    private final WindowsCertificateInstallService windowsCertificateInstallService = new WindowsCertificateInstallService();

    private JTextField hostField;
    private JSpinner portSpinner;
    private JButton toggleProxyButton;
    private JButton clearButton;
    private JMenuItem installCaMenuItem;
    private JMenuItem openCaMenuItem;
    private JPopupMenu statusPopupMenu;
    private JCheckBox syncSystemProxyCheckBox;
    private JCheckBox popupSyncSystemProxyCheckBox;
    private JTextField captureHostsField;
    private JPanel quickFilterPanel;
    private JLabel captureFilterLabel;
    private JPanel captureStatusPanel;
    private StatusChipLabel captureTrustChipLabel;
    private StatusChipLabel captureProxyChipLabel;
    private StatusChipLabel statusPopupTrustLabel;
    private StatusChipLabel statusPopupProxyLabel;
    private JLabel statusPopupPathLabel;
    private JLabel statusPopupDetailLabel;
    private JButton refreshStatusButton;
    private EnhancedTablePanel tablePanel;
    private RSyntaxTextArea requestDetailArea;
    private RSyntaxTextArea responseDetailArea;
    private RSyntaxTextArea streamDetailArea;
    private JTabbedPane detailTabs;
    private JSplitPane detailSplit;
    private JToggleButton detailToggleButton;
    private boolean detailPanelVisible;
    private JLabel detailMethodLabel;
    private JLabel detailProtocolLabel;
    private JLabel detailStatusLabel;
    private JLabel detailHostLabel;
    private JLabel detailDurationLabel;
    private JLabel detailTimeLabel;
    private JLabel requestPathLabel;
    private JLabel requestHeadersLabel;
    private JLabel requestBytesLabel;
    private JLabel requestTypeLabel;
    private JLabel responseStatusLabel;
    private JLabel responseHeadersLabel;
    private JLabel responseBytesLabel;
    private JLabel responseTypeLabel;
    private JLabel streamEventsLabel;
    private JLabel streamProtocolLabel;
    private final Map<String, JToggleButton> quickFilterButtons = new LinkedHashMap<>();
    private boolean syncingQuickFilters;
    private boolean operationInProgress;
    private CaptureFlow selectedFlow;

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
                "[][grow,fill]12[]12[]6[]push[]",
                "[][][][]"));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(0, 0, 8, 0)));

        hostField = new JTextField(defaultHost());
        hostField.setColumns(16);
        hostField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                t(MessageKeys.TOOLBOX_CAPTURE_BIND_HOST_PLACEHOLDER));
        portSpinner = new JSpinner(new SpinnerNumberModel(defaultPort(), 1, 65535, 1));
        configurePortSpinner();
        captureHostsField = new JTextField(defaultCaptureHostFilter());
        captureHostsField.setColumns(28);
        captureHostsField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                t(MessageKeys.TOOLBOX_CAPTURE_HOSTS_PLACEHOLDER));
        captureHostsField.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_HOSTS_TOOLTIP));
        captureHostsField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleCaptureFilterChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleCaptureFilterChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleCaptureFilterChanged();
            }
        });

        toggleProxyButton = new JButton();
        clearButton = new JButton(t(MessageKeys.TOOLBOX_CAPTURE_CLEAR), IconUtil.createThemed("icons/clear.svg", 16, 16));
        syncSystemProxyCheckBox = new JCheckBox(t(MessageKeys.TOOLBOX_CAPTURE_SYNC_MACOS_PROXY), defaultSyncSystemProxy());
        detailToggleButton = new JToggleButton();
        detailToggleButton.setIcon(IconUtil.createThemed("icons/detail.svg", 16, 16));
        detailToggleButton.setSelectedIcon(IconUtil.createColored("icons/detail.svg", 16, 16, ModernColors.PRIMARY));
        detailToggleButton.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL));
        detailToggleButton.setSelected(false);
        detailToggleButton.setPreferredSize(new Dimension(28, 28));
        detailToggleButton.setFocusable(false);
        detailToggleButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        initStatusPopupMenu();

        toggleProxyButton.addActionListener(e -> {
            if (proxyService.isRunning()) {
                stopProxy();
            } else {
                startProxy();
            }
        });
        clearButton.addActionListener(e -> proxyService.sessionStore().clear());
        detailToggleButton.addActionListener(e -> {
            detailPanelVisible = detailToggleButton.isSelected();
            if (detailPanelVisible) {
                showDetailPanel();
            } else {
                hideDetailPanel();
            }
        });

        captureFilterLabel = new JLabel();
        captureFilterLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        captureFilterLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        quickFilterPanel = buildQuickFilterPanel();
        captureTrustChipLabel = new StatusChipLabel();
        captureProxyChipLabel = new StatusChipLabel();
        captureStatusPanel = new JPanel(new MigLayout("insets 0, novisualpadding", "[]6[]", "[]"));
        captureStatusPanel.setOpaque(false);
        captureStatusPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        captureStatusPanel.add(captureTrustChipLabel);
        captureStatusPanel.add(captureProxyChipLabel);
        MouseAdapter statusClickListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showStatusPopup(captureStatusPanel, 0, captureStatusPanel.getHeight());
            }
        };
        captureStatusPanel.addMouseListener(statusClickListener);
        captureTrustChipLabel.addMouseListener(statusClickListener);
        captureProxyChipLabel.addMouseListener(statusClickListener);

        panel.add(new JLabel(t(MessageKeys.TOOLBOX_CAPTURE_BIND)), "gapright 8");
        panel.add(hostField, "wmin 180");
        panel.add(portSpinner, "wmin 90");
        panel.add(toggleProxyButton, "wmin 110");
        panel.add(clearButton);
        panel.add(detailToggleButton);
        panel.add(new JLabel(), "push, wrap");
        panel.add(new JLabel(t(MessageKeys.TOOLBOX_CAPTURE_CAPTURE_HOSTS)), "gapright 8");
        panel.add(captureHostsField, "span 6, growx, wrap");
        panel.add(quickFilterPanel, "skip 1, span 6, growx, wrap");
        panel.add(captureFilterLabel, "span, split 2, growx");
        panel.add(captureStatusPanel, "gapleft push");
        syncQuickFilterButtonsFromField();
        return panel;
    }

    private JComponent buildContent() {
        tablePanel = new EnhancedTablePanel(columnNames());
        JTable table = tablePanel.getTable();
        table.getSelectionModel().addListSelectionListener(this::handleSelectionChanged);
        disableTableTooltips(table);
        hideIdColumn(table);

        requestDetailArea = createDetailArea();
        responseDetailArea = createDetailArea();
        streamDetailArea = createDetailArea();
        requestDetailArea.setText(t(MessageKeys.TOOLBOX_CAPTURE_IDLE_DETAILS));
        responseDetailArea.setText(t(MessageKeys.TOOLBOX_CAPTURE_IDLE_DETAILS));
        streamDetailArea.setText(t(MessageKeys.TOOLBOX_CAPTURE_IDLE_DETAILS));

        detailTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        detailTabs.addTab(t(MessageKeys.TOOLBOX_CAPTURE_TAB_REQUEST), buildRequestDetailTab());
        detailTabs.addTab(t(MessageKeys.TOOLBOX_CAPTURE_TAB_RESPONSE), buildResponseDetailTab());
        detailTabs.addTab(t(MessageKeys.TOOLBOX_CAPTURE_TAB_STREAM), buildStreamDetailTab());
        detailTabs.setPreferredSize(new Dimension(360, 200));

        JPanel detailHeader = new JPanel(new MigLayout("insets 4 10 4 8, fillx", "[]8[]8[]8[]8[]push[]4[]", "[]"));
        detailHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")));
        detailMethodLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_METHOD) + ": -", ModernColors.INFO);
        detailProtocolLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_PROTOCOL) + ": -", new Color(0, 110, 170));
        detailStatusLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_STATUS) + ": -", ModernColors.SUCCESS);
        detailHostLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_HOST) + ": -", new Color(120, 80, 200));
        detailDurationLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_DURATION) + ": -", new Color(180, 100, 0));
        detailTimeLabel = buildChipLabel("-", null);
        detailTimeLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        detailTimeLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        CopyButton copyDetailButton = new CopyButton();
        copyDetailButton.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_COPY_DETAIL));
        copyDetailButton.addActionListener(e -> copyDetail());

        JButton copyCurlButton = new JButton("cURL");
        copyCurlButton.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_COPY_CURL));
        copyCurlButton.setFocusable(false);
        copyCurlButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        copyCurlButton.addActionListener(e -> copyAsCurl());

        CloseButton closeDetailButton = new CloseButton();
        closeDetailButton.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_CLOSE_DETAIL));
        closeDetailButton.addActionListener(e -> {
            detailToggleButton.setSelected(false);
            hideDetailPanel();
        });

        detailHeader.add(detailMethodLabel);
        detailHeader.add(detailProtocolLabel);
        detailHeader.add(detailStatusLabel);
        detailHeader.add(detailHostLabel);
        detailHeader.add(detailDurationLabel);
        detailHeader.add(detailTimeLabel);
        detailHeader.add(copyCurlButton);
        detailHeader.add(copyDetailButton);
        detailHeader.add(closeDetailButton);

        JPanel detailPanel = new JPanel(new BorderLayout(0, 0));
        detailPanel.setMinimumSize(new Dimension(0, 0));
        detailPanel.add(detailHeader, BorderLayout.NORTH);
        detailPanel.add(detailTabs, BorderLayout.CENTER);

        detailSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, detailPanel);
        detailSplit.setResizeWeight(1.0);
        detailSplit.setDividerSize(3);
        detailSplit.setContinuousLayout(true);
        detailSplit.setBorder(BorderFactory.createEmptyBorder());
        SwingUtilities.invokeLater(this::hideDetailPanel);
        return detailSplit;
    }

    private void initStatusPopupMenu() {
        statusPopupMenu = new JPopupMenu();

        JPanel content = new JPanel(new MigLayout(
                "insets 10, fillx, novisualpadding",
                "[grow,fill]",
                "[][][][][]8[]"));
        content.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")));

        JLabel titleLabel = new JLabel(t(MessageKeys.TOOLBOX_CAPTURE_STATUS_DETAILS));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 0));

        statusPopupTrustLabel = new StatusChipLabel();
        statusPopupProxyLabel = new StatusChipLabel();
        statusPopupPathLabel = new JLabel();
        statusPopupDetailLabel = new JLabel();
        statusPopupPathLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        statusPopupDetailLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        statusPopupPathLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        statusPopupDetailLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        popupSyncSystemProxyCheckBox = new JCheckBox(t(MessageKeys.TOOLBOX_CAPTURE_SYNC_MACOS_PROXY), syncSystemProxyCheckBox.isSelected());
        popupSyncSystemProxyCheckBox.setOpaque(false);
        popupSyncSystemProxyCheckBox.addActionListener(e -> {
            syncSystemProxyCheckBox.setSelected(popupSyncSystemProxyCheckBox.isSelected());
            updateCaptureStatusLabel();
        });

        installCaMenuItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA));
        openCaMenuItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_OPEN_CA));
        installCaMenuItem.addActionListener(e -> {
            statusPopupMenu.setVisible(false);
            installCa();
        });
        openCaMenuItem.addActionListener(e -> {
            statusPopupMenu.setVisible(false);
            openCa();
        });

        JButton installCaButton = new JButton(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA));
        installCaButton.addActionListener(e -> installCaMenuItem.doClick());
        JButton openCaButton = new JButton(t(MessageKeys.TOOLBOX_CAPTURE_OPEN_CA));
        openCaButton.addActionListener(e -> openCaMenuItem.doClick());
        refreshStatusButton = new JButton(t(MessageKeys.TOOLBOX_CAPTURE_REFRESH_STATUS));
        refreshStatusButton.addActionListener(e -> refreshStatusPopup());

        JPanel actions = new JPanel(new MigLayout("insets 0, fillx, novisualpadding", "[][]push[]", "[]"));
        actions.setOpaque(false);
        actions.add(installCaButton);
        actions.add(openCaButton);
        actions.add(refreshStatusButton);

        content.add(titleLabel, "wrap");
        content.add(statusPopupTrustLabel, "split 2");
        content.add(statusPopupProxyLabel, "wrap");
        content.add(popupSyncSystemProxyCheckBox, "wrap");
        content.add(statusPopupPathLabel, "wrap");
        content.add(statusPopupDetailLabel, "wmin 320, wrap");
        content.add(actions, "growx");

        statusPopupMenu.add(content);
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
        if (rows.isEmpty()) {
            clearDetail();
        }
        updateStatus();
    }

    private void handleSelectionChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        JTable table = tablePanel.getTable();
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            clearDetail();
            return;
        }
        Object flowId = table.getValueAt(selectedRow, 0);
        CaptureFlow flow = proxyService.sessionStore().find(String.valueOf(flowId));
        if (flow != null) {
            selectedFlow = flow;
            updateDetailHeader(flow);
            updateDetailAreas(flow);
            detailTabs.setSelectedIndex(0);
            if (!detailPanelVisible) {
                detailToggleButton.setSelected(true);
                showDetailPanel();
            }
        } else {
            clearDetail();
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
        setQuickFiltersEnabled(!busy && !running);
        syncSystemProxyCheckBox.setEnabled(!busy && !running && proxyService.isSystemProxySyncSupported());
        popupSyncSystemProxyCheckBox.setEnabled(!busy && !running && proxyService.isSystemProxySyncSupported());
        popupSyncSystemProxyCheckBox.setSelected(syncSystemProxyCheckBox.isSelected());

        updateCaptureFilterSummary();
        syncQuickFilterButtonsFromField();
        updateCaptureStatusLabel();

        boolean systemProxySupported = proxyService.isSystemProxySyncSupported();
        boolean certificateInstallSupported = isCertificateInstallSupported();

        if (!systemProxySupported) {
            syncSystemProxyCheckBox.setSelected(false);
            popupSyncSystemProxyCheckBox.setSelected(false);
            popupSyncSystemProxyCheckBox.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_SYNC_PROXY_TOOLTIP_UNSUPPORTED));
            syncSystemProxyCheckBox.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_SYNC_PROXY_TOOLTIP_UNSUPPORTED));
        } else {
            popupSyncSystemProxyCheckBox.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_SYNC_PROXY_TOOLTIP));
            syncSystemProxyCheckBox.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_SYNC_PROXY_TOOLTIP));
        }

        if (!certificateInstallSupported) {
            installCaMenuItem.setEnabled(false);
            openCaMenuItem.setEnabled(false);
            refreshStatusButton.setEnabled(true);
        } else {
            installCaMenuItem.setEnabled(!busy);
            openCaMenuItem.setEnabled(!busy);
            refreshStatusButton.setEnabled(true);
        }
    }

    private void handleCaptureFilterChanged() {
        if (syncingQuickFilters) {
            return;
        }
        syncQuickFilterButtonsFromField();
        if (!proxyService.isRunning()) {
            updateCaptureFilterSummary();
        }
    }

    private void updateCaptureFilterSummary() {
        captureFilterLabel.setText(proxyService.isRunning()
                ? proxyService.captureFilterSummary()
                : CaptureRequestFilter.parse(captureHostsField.getText()).summary());
    }

    private JPanel buildQuickFilterPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 0, gapx 6, gapy 0, novisualpadding", "[]0[]0[]0[]0[]0[]0[]", "[]"));
        panel.setOpaque(false);
        addQuickFilterButton(panel, "http", t(MessageKeys.TOOLBOX_CAPTURE_QUICK_FILTER_HTTP));
        addQuickFilterButton(panel, "https", t(MessageKeys.TOOLBOX_CAPTURE_QUICK_FILTER_HTTPS));
        addQuickFilterButton(panel, "json", t(MessageKeys.TOOLBOX_CAPTURE_QUICK_FILTER_JSON));
        addQuickFilterButton(panel, "image", t(MessageKeys.TOOLBOX_CAPTURE_QUICK_FILTER_IMAGE));
        addQuickFilterButton(panel, "js", t(MessageKeys.TOOLBOX_CAPTURE_QUICK_FILTER_JS));
        addQuickFilterButton(panel, "css", t(MessageKeys.TOOLBOX_CAPTURE_QUICK_FILTER_CSS));
        addQuickFilterButton(panel, "api", t(MessageKeys.TOOLBOX_CAPTURE_QUICK_FILTER_API));
        return panel;
    }

    private void addQuickFilterButton(JPanel panel, String token, String text) {
        JToggleButton button = new QuickFilterButton(text);
        button.addActionListener(e -> toggleQuickFilterToken(token, button.isSelected()));
        quickFilterButtons.put(token, button);
        panel.add(button);
    }

    private void toggleQuickFilterToken(String token, boolean selected) {
        if (syncingQuickFilters) {
            return;
        }
        List<String> tokens = new ArrayList<>(parseFilterTokens(captureHostsField.getText()));
        removeQuickFilterToken(tokens, token);
        if (selected) {
            tokens.add(token);
        }
        syncingQuickFilters = true;
        try {
            captureHostsField.setText(String.join(" ", tokens));
        } finally {
            syncingQuickFilters = false;
        }
        syncQuickFilterButtonsFromField();
        updateCaptureFilterSummary();
    }

    private void syncQuickFilterButtonsFromField() {
        List<String> tokens = parseFilterTokens(captureHostsField.getText());
        syncingQuickFilters = true;
        try {
            quickFilterButtons.forEach((token, button) -> button.setSelected(hasQuickFilterToken(tokens, token)));
        } finally {
            syncingQuickFilters = false;
        }
    }

    private void setQuickFiltersEnabled(boolean enabled) {
        quickFilterButtons.values().forEach(button -> button.setEnabled(enabled));
    }

    private List<String> parseFilterTokens(String rawValue) {
        List<String> tokens = new ArrayList<>();
        if (rawValue == null || rawValue.isBlank()) {
            return tokens;
        }
        for (String token : rawValue.trim().split("[,;\\s\\r\\n]+")) {
            if (token != null && !token.isBlank()) {
                tokens.add(token.trim());
            }
        }
        return tokens;
    }

    private boolean hasQuickFilterToken(List<String> tokens, String canonicalToken) {
        for (String token : tokens) {
            String normalized = normalizeQuickFilterToken(token);
            if (canonicalToken.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private void removeQuickFilterToken(List<String> tokens, String canonicalToken) {
        tokens.removeIf(token -> canonicalToken.equals(normalizeQuickFilterToken(token)));
    }

    private String normalizeQuickFilterToken(String token) {
        if (token == null) {
            return "";
        }
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("!")) {
            normalized = normalized.substring(1).trim();
        }
        return switch (normalized) {
            case "http", "scheme:http" -> "http";
            case "https", "scheme:https" -> "https";
            case "json", "type:json" -> "json";
            case "image", "img", "type:image", "type:img" -> "image";
            case "js", "type:js" -> "js";
            case "css", "type:css" -> "css";
            case "api", "type:api" -> "api";
            default -> normalized;
        };
    }

    private boolean isCertificateInstallSupported() {
        return macCertificateInstallService.isSupported() || windowsCertificateInstallService.isSupported();
    }

    private void updateCaptureStatusLabel() {
        CaptureTrustStatus trustStatus = safeCaptureTrustStatus();
        String tooltip = buildCaptureStatusTooltip();
        captureTrustChipLabel.setChip(resolveTrustChipText(trustStatus), resolveTrustChipColor(trustStatus));
        captureProxyChipLabel.setChip(resolveProxyChipText(), resolveProxyChipColor());
        captureTrustChipLabel.setToolTipText(tooltip);
        captureProxyChipLabel.setToolTipText(tooltip);
        captureStatusPanel.setToolTipText(tooltip);
        refreshStatusPopup();
    }

    private void refreshStatusPopup() {
        CaptureTrustStatus trustStatus = safeCaptureTrustStatus();
        statusPopupTrustLabel.setChip(resolveTrustChipText(trustStatus), resolveTrustChipColor(trustStatus));
        statusPopupProxyLabel.setChip(resolveProxyChipText(), resolveProxyChipColor());
        statusPopupPathLabel.setText(caPathSummaryText());
        String detail = trustStatus.detail();
        statusPopupDetailLabel.setText(detail.isBlank()
                ? ""
                : "<html><div style='width:320px'>" + escapeHtml(detail).replace("\n", "<br>") + "</div></html>");
    }

    private void showStatusPopup(JComponent invoker, int x, int y) {
        refreshStatusPopup();
        statusPopupMenu.show(invoker, x, y);
    }

    private String buildCaptureStatusTooltip() {
        StringBuilder builder = new StringBuilder("<html>");
        builder.append(escapeHtml(caPathSummaryText()));
        String detail = caTrustDetailText();
        if (!detail.isBlank()) {
            builder.append("<br>").append(escapeHtml(detail));
        }
        builder.append("<br>").append(escapeHtml(systemProxySummaryText()));
        builder.append("</html>");
        return builder.toString();
    }

    private String caPathSummaryText() {
        try {
            return t(MessageKeys.TOOLBOX_CAPTURE_CA_PATH, proxyService.rootCertificatePath());
        } catch (Exception ex) {
            return t(MessageKeys.TOOLBOX_CAPTURE_CA_PATH_UNAVAILABLE);
        }
    }

    private String caTrustSummaryText() {
        try {
            CaptureTrustStatus trustStatus = resolveCaptureTrustStatus();
            if (!trustStatus.supported()) {
                return t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_UNSUPPORTED);
            }
            if (trustStatus.trusted()) {
                return t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_TRUSTED);
            }
            if (trustStatus.installed()) {
                return t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_VERIFY);
            }
            return t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_NOT_INSTALLED);
        } catch (Exception ex) {
            return t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_UNKNOWN);
        }
    }

    private String caTrustDetailText() {
        try {
            return resolveCaptureTrustStatus().detail();
        } catch (Exception ignored) {
            // Ignore detail lookup failures and keep the summary available.
        }
        return "";
    }

    private String systemProxySummaryText() {
        if (!proxyService.isSystemProxySyncSupported()) {
            return t(MessageKeys.TOOLBOX_CAPTURE_SYSTEM_PROXY_UNSUPPORTED);
        }
        if (proxyService.isSystemProxySynced()) {
            return proxyService.systemProxyStatus();
        }
        if (!proxyService.isRunning() && syncSystemProxyCheckBox != null && syncSystemProxyCheckBox.isSelected()) {
            return t(MessageKeys.TOOLBOX_CAPTURE_SYSTEM_PROXY_PENDING);
        }
        return t(MessageKeys.TOOLBOX_CAPTURE_SYSTEM_PROXY_MANUAL);
    }

    private CaptureTrustStatus safeCaptureTrustStatus() {
        try {
            return resolveCaptureTrustStatus();
        } catch (Exception ex) {
            return new CaptureTrustStatus(false, false, false, t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_UNKNOWN));
        }
    }

    private String resolveTrustChipText(CaptureTrustStatus trustStatus) {
        if (!trustStatus.supported()) {
            return t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_UNSUPPORTED);
        }
        if (trustStatus.trusted()) {
            return t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_TRUSTED);
        }
        if (trustStatus.installed()) {
            return t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_VERIFY);
        }
        return t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_NOT_INSTALLED);
    }

    private Color resolveTrustChipColor(CaptureTrustStatus trustStatus) {
        if (!trustStatus.supported()) {
            return new Color(120, 120, 120);
        }
        if (trustStatus.trusted()) {
            return ModernColors.SUCCESS;
        }
        if (trustStatus.installed()) {
            return new Color(180, 100, 0);
        }
        return new Color(160, 60, 60);
    }

    private String resolveProxyChipText() {
        return systemProxySummaryText();
    }

    private Color resolveProxyChipColor() {
        if (!proxyService.isSystemProxySyncSupported()) {
            return new Color(120, 120, 120);
        }
        if (proxyService.isSystemProxySynced()) {
            return ModernColors.INFO;
        }
        if (!proxyService.isRunning() && syncSystemProxyCheckBox != null && syncSystemProxyCheckBox.isSelected()) {
            return new Color(180, 100, 0);
        }
        return new Color(120, 80, 200);
    }

    private CaptureTrustStatus resolveCaptureTrustStatus() throws Exception {
        String certificatePath = proxyService.rootCertificatePath();
        if (macCertificateInstallService.isSupported()) {
            MacCertificateInstallService.CertificateTrustStatus trustStatus =
                    macCertificateInstallService.trustStatus(certificatePath);
            return new CaptureTrustStatus(true, trustStatus.installed(), trustStatus.trusted(), trustStatus.detail());
        }
        if (windowsCertificateInstallService.isSupported()) {
            WindowsCertificateInstallService.WindowsTrustStatus trustStatus =
                    windowsCertificateInstallService.trustStatus(certificatePath);
            return new CaptureTrustStatus(true, trustStatus.installed(), trustStatus.trusted(), trustStatus.detail());
        }
        return new CaptureTrustStatus(false, false, false, "");
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
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

    private void disableTableTooltips(JTable table) {
        TableCellRenderer renderer = table.getDefaultRenderer(Object.class);
        table.setDefaultRenderer(Object.class, (tbl, value, selected, focus, row, column) -> {
            java.awt.Component component = renderer.getTableCellRendererComponent(tbl, value, selected, focus, row, column);
            if (component instanceof JComponent jComponent) {
                jComponent.setToolTipText(null);
            }
            return component;
        });
        table.setToolTipText(null);
        table.getTableHeader().setToolTipText(null);
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

    private void openCa() {
        try {
            String caPath = proxyService.rootCertificatePath();
            openCertificate(caPath);
            NotificationUtil.showInfo(t(MessageKeys.TOOLBOX_CAPTURE_OPEN_CA_SUCCESS));
        } catch (Exception ex) {
            NotificationUtil.showError(t(MessageKeys.TOOLBOX_CAPTURE_OPEN_CA_FAILED, ex.getMessage()));
        }
    }

    private void installCa() {
        if (macCertificateInstallService.isSupported()) {
            installCaOnMac();
            return;
        }
        if (windowsCertificateInstallService.isSupported()) {
            installCaOnWindows();
            return;
        }
        NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_TOOLTIP_UNSUPPORTED));
    }

    private void installCaOnMac() {
        try {
            String caPath = proxyService.rootCertificatePath();
            int removed = macCertificateInstallService.removeMatchingLoginKeychainCertificates(caPath);
            boolean systemInstallAttempted = false;
            boolean loginInstallAttempted = false;
            MacCertificateInstallService.CertificateTrustStatus trustStatus = macCertificateInstallService.trustStatus(caPath);
            if (!trustStatus.trusted()) {
                try {
                    macCertificateInstallService.installToSystemKeychainWithPrompt(caPath);
                    systemInstallAttempted = true;
                } catch (Exception ignored) {
                    // Fall back to login-keychain installation below.
                }
                trustStatus = macCertificateInstallService.trustStatus(caPath);
            }
            if (!trustStatus.trusted()) {
                macCertificateInstallService.installToLoginKeychain(caPath);
                loginInstallAttempted = true;
                trustStatus = macCertificateInstallService.trustStatus(caPath);
            }
            updateStatus();
            macCertificateInstallService.openKeychainAccess();
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
                macCertificateInstallService.openCertificate(caPath);
                showManualTrustGuide(caPath, trustStatus.detail());
                NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_WARN_TRUST));
            } else {
                macCertificateInstallService.openCertificate(caPath);
                showManualTrustGuide(caPath, trustStatus.detail());
                NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_WARN_VISIBLE));
            }
        } catch (Exception ex) {
            NotificationUtil.showError(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_FAILED, ex.getMessage()));
        }
    }

    private void installCaOnWindows() {
        try {
            String caPath = proxyService.rootCertificatePath();
            WindowsCertificateInstallService.WindowsTrustStatus trustStatus = windowsCertificateInstallService.trustStatus(caPath);
            if (!trustStatus.trusted()) {
                windowsCertificateInstallService.installToCurrentUserRoot(caPath);
                trustStatus = windowsCertificateInstallService.trustStatus(caPath);
            }
            updateStatus();
            windowsCertificateInstallService.openCertificateManager();
            if (trustStatus.trusted()) {
                NotificationUtil.showSuccess(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_SUCCESS_WINDOWS,
                        t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_REMOVED_NONE)));
            } else if (trustStatus.installed()) {
                windowsCertificateInstallService.openCertificate(caPath);
                showWindowsManualTrustGuide(caPath, trustStatus.detail());
                NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_WARN_TRUST));
            } else {
                windowsCertificateInstallService.openCertificate(caPath);
                showWindowsManualTrustGuide(caPath, trustStatus.detail());
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

    private void showWindowsManualTrustGuide(String caPath, String detail) {
        JTextArea guide = new JTextArea(t(MessageKeys.TOOLBOX_CAPTURE_MANUAL_TRUST_GUIDE_WINDOWS, caPath, detail));
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

    private void openCertificate(String certificatePath) throws Exception {
        if (macCertificateInstallService.isSupported()) {
            macCertificateInstallService.openCertificate(certificatePath);
            return;
        }
        if (windowsCertificateInstallService.isSupported()) {
            windowsCertificateInstallService.openCertificate(certificatePath);
            return;
        }
        throw new IllegalStateException(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_TOOLTIP_UNSUPPORTED));
    }

    private void setOperationState(boolean busy) {
        operationInProgress = busy;
        updateStatus();
    }

    private void showDetailPanel() {
        detailPanelVisible = true;
        int totalHeight = detailSplit.getHeight();
        int location = totalHeight > 0 ? (int) (totalHeight * 0.60) : 320;
        detailSplit.setDividerLocation(location);
    }

    private void hideDetailPanel() {
        detailPanelVisible = false;
        detailSplit.setDividerLocation(1.0);
    }

    private void clearDetail() {
        selectedFlow = null;
        detailMethodLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_METHOD) + ": -");
        detailProtocolLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_PROTOCOL) + ": -");
        detailStatusLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_STATUS) + ": -");
        detailHostLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_HOST) + ": -");
        detailDurationLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_DURATION) + ": -");
        detailTimeLabel.setText("-");
        requestPathLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_PATH) + ": -");
        requestHeadersLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_HEADERS) + ": -");
        requestBytesLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_BYTES) + ": -");
        requestTypeLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_CONTENT_TYPE) + ": -");
        responseStatusLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_STATUS) + ": -");
        responseHeadersLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_HEADERS) + ": -");
        responseBytesLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_BYTES) + ": -");
        responseTypeLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_CONTENT_TYPE) + ": -");
        streamEventsLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_EVENTS) + ": -");
        streamProtocolLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_PROTOCOL) + ": -");
        requestDetailArea.setText(t(MessageKeys.TOOLBOX_CAPTURE_IDLE_DETAILS));
        requestDetailArea.setCaretPosition(0);
        requestDetailArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        responseDetailArea.setText(t(MessageKeys.TOOLBOX_CAPTURE_IDLE_DETAILS));
        responseDetailArea.setCaretPosition(0);
        responseDetailArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        streamDetailArea.setText(t(MessageKeys.TOOLBOX_CAPTURE_IDLE_DETAILS));
        streamDetailArea.setCaretPosition(0);
        streamDetailArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        detailTabs.setSelectedIndex(0);
    }

    private void updateDetailHeader(CaptureFlow flow) {
        detailMethodLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_METHOD) + ": " + flow.method());
        detailProtocolLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_PROTOCOL) + ": " + flow.protocolText());
        detailStatusLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_STATUS) + ": " + flow.statusDisplayText());
        detailHostLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_HOST) + ": " + flow.host());
        detailDurationLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_DURATION) + ": " + flow.durationMs() + " ms");
        detailTimeLabel.setText(flow.startedAtText());
        requestPathLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_PATH) + ": " + displayValue(flow.path()));
        requestHeadersLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_HEADERS) + ": " + flow.requestHeaderCount());
        requestBytesLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_BYTES) + ": " + flow.requestSize());
        requestTypeLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_CONTENT_TYPE) + ": " + displayValue(flow.requestContentType()));
        responseStatusLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_STATUS) + ": " + flow.statusDisplayText());
        responseHeadersLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_HEADERS) + ": " + flow.responseHeaderCount());
        responseBytesLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_BYTES) + ": " + flow.responseSize());
        responseTypeLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_CONTENT_TYPE) + ": " + displayValue(flow.responseContentType()));
        streamEventsLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_EVENTS) + ": " + flow.streamEventCount());
        streamProtocolLabel.setText(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_PROTOCOL) + ": " + flow.protocolText());
    }

    private void copyDetail() {
        RSyntaxTextArea activeArea = activeDetailArea();
        String detailText = activeArea.getText().trim();
        if (detailText.isEmpty()) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(detailText), null);
        NotificationUtil.showSuccess(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_COPIED));
    }

    private void copyAsCurl() {
        if (selectedFlow == null) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(selectedFlow.curlCommand()), null);
        if (selectedFlow.curlBodyPartial()) {
            NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_CURL_COPIED_PARTIAL));
        } else {
            NotificationUtil.showSuccess(t(MessageKeys.TOOLBOX_CAPTURE_CURL_COPIED));
        }
    }

    private RSyntaxTextArea createDetailArea() {
        RSyntaxTextArea area = new RSyntaxTextArea();
        area.setEditable(false);
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        area.setCodeFoldingEnabled(true);
        area.setAntiAliasingEnabled(true);
        area.setHighlightCurrentLine(true);
        area.setLineWrap(false);
        area.setWrapStyleWord(false);
        area.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        return area;
    }

    private void configurePortSpinner() {
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(portSpinner, "0");
        DecimalFormat format = editor.getFormat();
        format.setGroupingUsed(false);
        NumberFormatter formatter = (NumberFormatter) editor.getTextField().getFormatter();
        formatter.setValueClass(Integer.class);
        formatter.setAllowsInvalid(false);
        formatter.setCommitsOnValidEdit(true);
        editor.getTextField().setFormatterFactory(new DefaultFormatterFactory(formatter));
        editor.getTextField().setColumns(6);
        portSpinner.setEditor(editor);
    }

    private JComponent buildRequestDetailTab() {
        requestPathLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_PATH) + ": -", ModernColors.INFO);
        requestHeadersLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_HEADERS) + ": -", new Color(120, 80, 200));
        requestBytesLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_BYTES) + ": -", new Color(20, 150, 100));
        requestTypeLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_CONTENT_TYPE) + ": -", new Color(180, 100, 0));
        return buildDetailTabPanel(requestPathLabel, requestHeadersLabel, requestBytesLabel, requestTypeLabel, requestDetailArea);
    }

    private JComponent buildResponseDetailTab() {
        responseStatusLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_STATUS) + ": -", ModernColors.SUCCESS);
        responseHeadersLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_HEADERS) + ": -", new Color(120, 80, 200));
        responseBytesLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_BYTES) + ": -", new Color(20, 150, 100));
        responseTypeLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_CONTENT_TYPE) + ": -", new Color(180, 100, 0));
        return buildDetailTabPanel(responseStatusLabel, responseHeadersLabel, responseBytesLabel, responseTypeLabel, responseDetailArea);
    }

    private JComponent buildStreamDetailTab() {
        streamEventsLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_EVENTS) + ": -", ModernColors.INFO);
        streamProtocolLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_PROTOCOL) + ": -", new Color(120, 80, 200));
        JLabel streamBytesLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_BYTES) + ": -", new Color(20, 150, 100));
        JLabel streamTypeLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_CONTENT_TYPE) + ": -", new Color(180, 100, 0));
        streamBytesLabel.setVisible(false);
        streamTypeLabel.setVisible(false);
        return buildDetailTabPanel(streamEventsLabel, streamProtocolLabel, streamBytesLabel, streamTypeLabel, streamDetailArea);
    }

    private JComponent buildDetailTabPanel(JLabel first,
                                           JLabel second,
                                           JLabel third,
                                           JLabel fourth,
                                           RSyntaxTextArea detailArea) {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        JPanel header = new JPanel(new MigLayout("insets 6 8 4 8, fillx, novisualpadding", "[]8[]8[]8[]push", "[]"));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")));
        header.add(first);
        header.add(second);
        header.add(third);
        header.add(fourth, "growx");

        SearchableTextArea searchableDetail = new SearchableTextArea(detailArea, false);
        searchableDetail.setLineNumbersEnabled(false);
        panel.add(header, BorderLayout.NORTH);
        panel.add(searchableDetail, BorderLayout.CENTER);
        return panel;
    }

    private void updateDetailAreas(CaptureFlow flow) {
        updateDetailArea(requestDetailArea, flow.requestDetailText());
        updateDetailArea(responseDetailArea, flow.responseDetailText());
        updateDetailArea(streamDetailArea, flow.streamDetailText());
    }

    private void updateDetailArea(RSyntaxTextArea area, String text) {
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        area.setText(text == null ? "" : text);
        area.setCaretPosition(0);
    }

    private String displayValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private RSyntaxTextArea activeDetailArea() {
        int index = detailTabs.getSelectedIndex();
        if (index == 0) {
            return requestDetailArea;
        }
        if (index == 1) {
            return responseDetailArea;
        }
        return streamDetailArea;
    }

    private JLabel buildChipLabel(String text, Color bgColor) {
        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (bgColor != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 140));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        if (bgColor != null) {
            label.setForeground(ModernColors.isDarkTheme()
                    ? new Color(Math.min(bgColor.getRed() + 80, 255),
                    Math.min(bgColor.getGreen() + 80, 255),
                    Math.min(bgColor.getBlue() + 80, 255))
                    : bgColor.darker());
        }
        label.setBorder(new EmptyBorder(2, 6, 2, 6));
        label.setOpaque(false);
        return label;
    }

    private static final class QuickFilterButton extends JToggleButton {

        private QuickFilterButton(String text) {
            super(text);
            setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
            setBorder(new EmptyBorder(3, 8, 3, 8));
            setFocusable(false);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            updateColors();
        }

        @Override
        public void setSelected(boolean selected) {
            super.setSelected(selected);
            updateColors();
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            updateColors();
        }

        private void updateColors() {
            Color borderColor = isSelected() ? ModernColors.PRIMARY : UIManager.getColor("Separator.foreground");
            if (borderColor == null) {
                borderColor = ModernColors.NEUTRAL;
            }
            if (!isEnabled()) {
                setForeground(UIManager.getColor("Label.disabledForeground"));
            } else if (isSelected()) {
                setForeground(ModernColors.isDarkTheme()
                        ? new Color(Math.min(borderColor.getRed() + 80, 255),
                        Math.min(borderColor.getGreen() + 80, 255),
                        Math.min(borderColor.getBlue() + 80, 255))
                        : borderColor.darker());
            } else {
                setForeground(UIManager.getColor("Label.foreground"));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color borderColor = isSelected() ? ModernColors.PRIMARY : UIManager.getColor("Separator.foreground");
            if (borderColor == null) {
                borderColor = ModernColors.NEUTRAL;
            }
            Color fillColor;
            if (!isEnabled()) {
                fillColor = new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 18);
            } else if (isSelected()) {
                fillColor = new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 40);
            } else {
                fillColor = new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 12);
            }
            g2.setColor(fillColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), isSelected() ? 160 : 90));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class StatusChipLabel extends JLabel {
        private Color bgColor;

        private StatusChipLabel() {
            setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
            setBorder(new EmptyBorder(2, 6, 2, 6));
            setOpaque(false);
        }

        private void setChip(String text, Color color) {
            setText(text);
            bgColor = color;
            if (bgColor != null) {
                setForeground(ModernColors.isDarkTheme()
                        ? new Color(Math.min(bgColor.getRed() + 80, 255),
                        Math.min(bgColor.getGreen() + 80, 255),
                        Math.min(bgColor.getBlue() + 80, 255))
                        : bgColor.darker());
            } else {
                setForeground(UIManager.getColor("Label.foreground"));
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (bgColor != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 140));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
            super.paintComponent(g);
        }
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

    private record CaptureTrustStatus(boolean supported, boolean installed, boolean trusted, String detail) {
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
