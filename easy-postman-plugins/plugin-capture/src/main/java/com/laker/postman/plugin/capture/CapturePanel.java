package com.laker.postman.plugin.capture;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.ChipLabel;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.ToolWindowChrome;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.CopyButton;
import com.laker.postman.common.component.table.EnhancedTablePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.plugin.api.PluginStorage;
import com.laker.postman.plugin.api.service.RequestCollectionImportService;
import com.laker.postman.util.EditorThemeUtil;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.common.component.notification.NotificationCenter;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
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
import javax.swing.Timer;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.JTextComponent;
import javax.swing.text.NumberFormatter;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

public class CapturePanel extends JPanel {
    private static final int TABLE_COLUMN_COUNT = 10;
    private static final int SOURCE_COLUMN_INDEX = 2;
    private static final int PID_COLUMN_INDEX = 3;
    private static final int TYPE_COLUMN_INDEX = 4;
    private static final int METHOD_COLUMN_INDEX = 5;
    private static final int URL_COLUMN_INDEX = 6;
    private static final int STATUS_COLUMN_INDEX = 7;
    private static final int DURATION_COLUMN_INDEX = 8;
    private static final int SIZE_COLUMN_INDEX = 9;
    static final int REQUEST_RESPONSE_DETAIL_DISPLAY_LIMIT = 20_000;
    static final int DETAIL_SPLIT_DIVIDER_SIZE = 9;
    private static final Integer[] RETENTION_LIMIT_OPTIONS = {100, 300, 1000};

    private final CaptureProxyService proxyService = CaptureRuntime.proxyService();
    private final CaptureRequestCollectionImporter requestCollectionImporter;
    private final CaptureSettingsStore settingsStore;
    private final CaptureSettings initialSettings;
    private final MacCertificateInstallService macCertificateInstallService = new MacCertificateInstallService();
    private final WindowsCertificateInstallService windowsCertificateInstallService = new WindowsCertificateInstallService();

    private JTextField hostField;
    private JSpinner portSpinner;
    private JComboBox<Integer> retentionLimitComboBox;
    private JButton toggleProxyButton;
    private JButton clearButton;
    private JMenuItem installCaMenuItem;
    private JMenuItem openCaMenuItem;
    private JPopupMenu statusPopupMenu;
    private JCheckBox syncSystemProxyCheckBox;
    private JCheckBox popupSyncSystemProxyCheckBox;
    private JTextField captureFilterField;
    private JPanel quickFilterPanel;
    private JLabel captureFilterLabel;
    private JPanel viewPresetPanel;
    private JPanel captureStatusPanel;
    private StatusChipLabel captureTrustChipLabel;
    private StatusChipLabel captureProxyChipLabel;
    private StatusChipLabel statusPopupTrustLabel;
    private StatusChipLabel statusPopupProxyLabel;
    private JLabel statusPopupPathLabel;
    private JLabel statusPopupDetailLabel;
    private JButton refreshStatusButton;
    private EnhancedTablePanel tablePanel;
    private JTextArea requestDetailArea;
    private JTextArea responseDetailArea;
    private RSyntaxTextArea streamDetailArea;
    private RSyntaxTextArea diagnosticsDetailArea;
    private JTabbedPane detailTabs;
    private JSplitPane detailSplit;
    private boolean detailPanelVisible;
    private boolean refreshingTable;
    private JLabel detailMethodLabel;
    private JLabel detailProtocolLabel;
    private JLabel detailStatusLabel;
    private JLabel detailDurationLabel;
    private JLabel detailTimeLabel;
    private final Map<String, JToggleButton> quickFilterButtons = new LinkedHashMap<>();
    private final Map<CaptureViewPreset, JToggleButton> viewPresetButtons = new LinkedHashMap<>();
    private final EnumSet<CaptureViewPreset> activeViewPresets = EnumSet.noneOf(CaptureViewPreset.class);
    private String sourceIncludeFilter = "";
    private String sourceExcludeFilter = "";
    private boolean syncingQuickFilters;
    private boolean operationInProgress;
    private CaptureFlow selectedFlow;
    private Timer refreshTimer;
    private int totalFlowCount;
    private int visibleFlowCount;
    private volatile CaptureStatusSnapshot captureStatusSnapshot = checkingStatusSnapshot();
    private SwingWorker<CaptureStatusSnapshot, Void> statusRefreshWorker;

    public CapturePanel() {
        this(null, PluginStorage.noop());
    }

    CapturePanel(RequestCollectionImportService importService, PluginStorage storage) {
        requestCollectionImporter = new CaptureRequestCollectionImporter(importService);
        settingsStore = new CaptureSettingsStore(storage == null ? PluginStorage.noop() : storage);
        initialSettings = settingsStore.load();
        proxyService.sessionStore().setMaxFlows(initialSettings.maxFlows());
        initUI();
        proxyService.sessionStore().addChangeListener(this::scheduleRefreshTable);
        refreshTable();
        updateStatus();
        refreshCaptureStatusAsync(false);
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        ToolWindowSurfaceStyle.applyCard(this);

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        ToolWindowSurfaceStyle.applyPanelTreeCard(this);
    }

    private JComponent buildTopBar() {
        JPanel panel = new JPanel(new MigLayout(
                "insets 8, fillx, novisualpadding",
                "[][grow,fill]12[]8[]12[]6[]6[]push[]",
                "[][][][][]"));
        ToolWindowSurfaceStyle.applySectionHeader(panel, 0, 0, 8, 0);

        hostField = new JTextField(defaultHost());
        hostField.setColumns(16);
        hostField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                t(MessageKeys.TOOLBOX_CAPTURE_BIND_HOST_PLACEHOLDER));
        portSpinner = new JSpinner(new SpinnerNumberModel(defaultPort(), 1, 65535, 1));
        configurePortSpinner();
        retentionLimitComboBox = new JComboBox<>(RETENTION_LIMIT_OPTIONS);
        retentionLimitComboBox.setSelectedItem(defaultMaxFlows());
        retentionLimitComboBox.setFocusable(false);
        retentionLimitComboBox.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_RETENTION_TOOLTIP));
        captureFilterField = new JTextField(defaultCaptureFilter());
        captureFilterField.setColumns(28);
        captureFilterField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                t(MessageKeys.TOOLBOX_CAPTURE_HOSTS_PLACEHOLDER));
        captureFilterField.setToolTipText(htmlTooltip(t(MessageKeys.TOOLBOX_CAPTURE_HOSTS_TOOLTIP), 420));
        captureFilterField.getDocument().addDocumentListener(new DocumentListener() {
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
        initStatusPopupMenu();

        toggleProxyButton.addActionListener(e -> {
            if (proxyService.isRunning()) {
                stopProxy();
            } else {
                startProxy();
            }
        });
        clearButton.addActionListener(e -> proxyService.sessionStore().clear());
        retentionLimitComboBox.addActionListener(e -> handleRetentionLimitChanged());

        captureFilterLabel = new JLabel();
        captureFilterLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        captureFilterLabel.setForeground(ModernColors.getTextSecondary());
        quickFilterPanel = buildQuickFilterPanel();
        viewPresetPanel = buildViewPresetPanel();
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
        panel.add(new JLabel(t(MessageKeys.TOOLBOX_CAPTURE_RETENTION)), "gapleft 4");
        panel.add(retentionLimitComboBox, "wmin 82");
        panel.add(new JLabel(), "push, wrap");
        panel.add(new JLabel(t(MessageKeys.TOOLBOX_CAPTURE_CAPTURE_HOSTS)), "gapright 8");
        panel.add(captureFilterField, "span 7, growx, wrap");
        panel.add(quickFilterPanel, "skip 1, span 7, growx, wrap");
        panel.add(new JLabel(t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER)), "gapright 8");
        panel.add(viewPresetPanel, "span 7, growx, wrap");
        panel.add(captureFilterLabel, "span, split 2, growx");
        panel.add(captureStatusPanel, "gapleft push");
        syncQuickFilterButtonsFromField();
        return panel;
    }

    private JComponent buildContent() {
        tablePanel = new EnhancedTablePanel(columnNames());
        tablePanel.setCellDetailDialogOnDoubleClickEnabled(false);
        ToolWindowSurfaceStyle.applyPanelTreeCard(tablePanel);
        tablePanel.setRowCountFormatter((filtered, total) ->
                formatRetainedRowCount(filtered, total, proxyService.sessionStore().maxFlows()));
        tablePanel.setAutoResizeOnRefresh(false);
        tablePanel.setContextMenuCustomizer(this::appendTableContextMenu);
        JTable table = tablePanel.getTable();
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getSelectionModel().addListSelectionListener(this::handleSelectionChanged);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)
                        || e.getClickCount() != 2
                        || table.rowAtPoint(e.getPoint()) < 0) {
                    return;
                }
                showTableFlowDetailAtRow(table.rowAtPoint(e.getPoint()));
            }
        });
        disableTableTooltips(table);
        hideIdColumn(table);
        configureTableColumns(table);
        resetTableSort();

        requestDetailArea = createPlainDetailArea();
        responseDetailArea = createPlainDetailArea();
        streamDetailArea = createDetailArea();
        diagnosticsDetailArea = createDetailArea();
        requestDetailArea.setText(idleDetailText());
        responseDetailArea.setText(idleDetailText());
        streamDetailArea.setText(idleDetailText());
        diagnosticsDetailArea.setText(idleDetailText());

        detailTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        detailTabs.addTab(t(MessageKeys.TOOLBOX_CAPTURE_TAB_REQUEST), buildRequestDetailTab());
        detailTabs.addTab(t(MessageKeys.TOOLBOX_CAPTURE_TAB_RESPONSE), buildResponseDetailTab());
        detailTabs.addTab(t(MessageKeys.TOOLBOX_CAPTURE_TAB_STREAM), buildStreamDetailTab());
        detailTabs.addTab(t(MessageKeys.TOOLBOX_CAPTURE_TAB_DIAGNOSTICS), buildDiagnosticsDetailTab());
        detailTabs.setPreferredSize(new Dimension(360, 200));
        ToolWindowSurfaceStyle.applyTabbedPaneCard(detailTabs);

        JPanel detailHeader = new JPanel(new MigLayout(
                "insets 4 10 4 8, fillx",
                "[]6[]6[]6[]6[]push[]4[]4[]",
                "[]"));
        ToolWindowSurfaceStyle.applySectionHeader(detailHeader);
        detailMethodLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_METHOD) + ": -", ModernColors.getInfo());
        detailProtocolLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_PROTOCOL) + ": -", ModernColors.getSecondary());
        detailStatusLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_STATUS) + ": -", CaptureStatusStyle.accentFor(null));
        detailDurationLabel = buildChipLabel(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_DURATION) + ": -", ModernColors.getWarningDark());
        detailTimeLabel = buildChipLabel("-", null);
        detailTimeLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        detailTimeLabel.setForeground(ModernColors.getTextSecondary());

        CopyButton copyDetailButton = new CopyButton();
        copyDetailButton.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_COPY_DETAIL));
        copyDetailButton.addActionListener(e -> copyDetail());

        JButton copyCurlButton = new JButton("cURL");
        copyCurlButton.setToolTipText(t(MessageKeys.TOOLBOX_CAPTURE_COPY_CURL));
        copyCurlButton.setFocusable(false);
        copyCurlButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        copyCurlButton.addActionListener(e -> copyAsCurl());

        JButton hideDetailButton = createToolWindowToolbarButton(
                "icons/tool-window-hide.svg",
                t(MessageKeys.TOOLBOX_CAPTURE_CLOSE_DETAIL)
        );
        hideDetailButton.addActionListener(e -> hideDetailPanel());

        detailHeader.add(detailMethodLabel);
        detailHeader.add(detailProtocolLabel);
        detailHeader.add(detailStatusLabel);
        detailHeader.add(detailDurationLabel);
        detailHeader.add(detailTimeLabel);
        detailHeader.add(copyCurlButton);
        detailHeader.add(copyDetailButton);
        detailHeader.add(hideDetailButton);

        JPanel detailPanel = new JPanel(new BorderLayout(0, 0));
        detailPanel.setOpaque(false);
        detailPanel.setMinimumSize(new Dimension(0, 0));
        detailPanel.add(detailHeader, BorderLayout.NORTH);
        detailPanel.add(detailTabs, BorderLayout.CENTER);

        detailSplit = ToolWindowChrome.createVerticalInnerSplitPane(tablePanel, detailPanel, 320);
        detailSplit.setDividerSize(DETAIL_SPLIT_DIVIDER_SIZE);
        detailSplit.setResizeWeight(1.0);
        SwingUtilities.invokeLater(this::hideDetailPanel);
        return detailSplit;
    }

    private void initStatusPopupMenu() {
        statusPopupMenu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(statusPopupMenu);

        JPanel content = new JPanel(new MigLayout(
                "insets 10, fillx, novisualpadding",
                "[grow,fill]",
                "[][][][][]8[]"));
        ToolWindowSurfaceStyle.applyCard(content);
        content.setBorder(BorderFactory.createEmptyBorder());

        JLabel titleLabel = new JLabel(t(MessageKeys.TOOLBOX_CAPTURE_STATUS_DETAILS));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 0));

        statusPopupTrustLabel = new StatusChipLabel();
        statusPopupProxyLabel = new StatusChipLabel();
        statusPopupPathLabel = new JLabel();
        statusPopupDetailLabel = new JLabel();
        statusPopupPathLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        statusPopupDetailLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        statusPopupPathLabel.setForeground(ModernColors.getTextSecondary());
        statusPopupDetailLabel.setForeground(ModernColors.getTextSecondary());
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
        refreshStatusButton.addActionListener(e -> refreshCaptureStatusAsync(true));

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
            NotificationCenter.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_WARN_BIND_HOST_REQUIRED));
            return;
        }
        if (operationInProgress) {
            return;
        }
        int port = ((Number) portSpinner.getValue()).intValue();
        boolean syncSystemProxy = syncSystemProxyCheckBox.isSelected();
        String captureFilter = captureFilterField.getText().trim();
        int maxFlows = currentMaxFlows();

        setOperationState(true);
        SwingWorker<StartResult, Void> worker = new SwingWorker<>() {
            @Override
            protected StartResult doInBackground() throws Exception {
                proxyService.start(host, port, syncSystemProxy, captureFilter);
                settingsStore.save(new CaptureSettings(host, port, syncSystemProxy, captureFilter, maxFlows));
                return new StartResult(host, port, proxyService.isSystemProxySynced());
            }

            @Override
            protected void done() {
                setOperationState(false);
                try {
                    StartResult result = get();
                    updateStatus();
                    refreshCaptureStatusAsync(true);
                    String portText = String.valueOf(result.port());
                    NotificationCenter.showSuccess(result.systemProxySynced()
                            ? t(MessageKeys.TOOLBOX_CAPTURE_START_SUCCESS_SYNCED, result.host(), portText)
                            : t(MessageKeys.TOOLBOX_CAPTURE_START_SUCCESS, result.host(), portText));
                } catch (Exception ex) {
                    updateStatus();
                    refreshCaptureStatusAsync(true);
                    NotificationCenter.showError(t(MessageKeys.TOOLBOX_CAPTURE_START_FAILED, rootMessage(ex)));
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
                    refreshCaptureStatusAsync(true);
                    NotificationCenter.showInfo(synced
                            ? t(MessageKeys.TOOLBOX_CAPTURE_STOP_SUCCESS_SYNCED)
                            : t(MessageKeys.TOOLBOX_CAPTURE_STOP_SUCCESS));
                } catch (Exception ex) {
                    updateStatus();
                    refreshCaptureStatusAsync(true);
                    NotificationCenter.showError(t(MessageKeys.TOOLBOX_CAPTURE_STOP_FAILED, rootMessage(ex)));
                }
            }
        };
        worker.execute();
    }

    private void scheduleRefreshTable() {
        SwingUtilities.invokeLater(() -> {
            if (refreshTimer == null) {
                refreshTimer = new Timer(120, e -> refreshTable());
                refreshTimer.setRepeats(false);
            }
            refreshTimer.restart();
        });
    }

    private void refreshTable() {
        List<String> selectedIds = selectedFlowIds();
        CaptureFlow selectedFlowBeforeRefresh = selectedFlow;
        List<CaptureFlow> snapshot = proxyService.sessionStore().snapshot();
        List<CaptureFlow> visibleFlows = visibleFlows(snapshot);
        totalFlowCount = snapshot.size();
        visibleFlowCount = visibleFlows.size();
        List<Object[]> rows = new ArrayList<>();
        for (CaptureFlow flow : visibleFlows) {
            rows.add(flow.toRow());
        }
        JTable table = tablePanel.getTable();
        refreshingTable = true;
        try {
            tablePanel.setDataPreserveView(rows);
            hideIdColumn(table);
            configureTableColumns(table);
            restoreSelectedRows(selectedIds);
        } finally {
            refreshingTable = false;
        }
        CaptureFlow retainedSelectedFlow = findVisibleSelectedFlow(selectedFlowBeforeRefresh, visibleFlows);
        if (rows.isEmpty()) {
            clearDetail();
        } else if (retainedSelectedFlow != null) {
            selectedFlow = retainedSelectedFlow;
            updateDetailHeader(retainedSelectedFlow);
            updateDetailAreas(retainedSelectedFlow, true);
        } else if (table.getSelectedRow() < 0) {
            clearDetail();
        } else if (selectedFlow != null) {
            CaptureFlow latestSelectedFlow = proxyService.sessionStore().find(selectedFlow.id());
            if (latestSelectedFlow != null) {
                selectedFlow = latestSelectedFlow;
                updateDetailHeader(latestSelectedFlow);
                updateDetailAreas(latestSelectedFlow, true);
            }
        }
    }

    static CaptureFlow findVisibleSelectedFlow(CaptureFlow selectedFlow, List<CaptureFlow> visibleFlows) {
        if (selectedFlow == null || visibleFlows == null || visibleFlows.isEmpty()) {
            return null;
        }
        String selectedId = selectedFlow.id();
        for (CaptureFlow flow : visibleFlows) {
            if (flow != null && selectedId.equals(flow.id())) {
                return flow;
            }
        }
        return null;
    }

    private List<CaptureFlow> visibleFlows(List<CaptureFlow> snapshot) {
        List<CaptureFlow> flows = new ArrayList<>();
        for (CaptureFlow flow : snapshot) {
            if (matchesViewPresets(flow)) {
                flows.add(flow);
            }
        }
        if (activeViewPresets.contains(CaptureViewPreset.ERROR_PRIORITY)) {
            flows.sort(errorPriorityComparator());
        }
        return flows;
    }

    private boolean matchesViewPresets(CaptureFlow flow) {
        if (!matchesSourceDisplayFilter(flow, sourceIncludeFilter, sourceExcludeFilter)) {
            return false;
        }
        if (activeViewPresets.contains(CaptureViewPreset.ERRORS_ONLY)
                && !CaptureFlowClassifier.isError(flow)) {
            return false;
        }
        if (activeViewPresets.contains(CaptureViewPreset.SLOW_ONLY)
                && !CaptureFlowClassifier.isSlow(flow)) {
            return false;
        }
        if (activeViewPresets.contains(CaptureViewPreset.HIDE_STATIC)
                && CaptureFlowClassifier.isStaticResource(flow)) {
            return false;
        }
        if (activeViewPresets.contains(CaptureViewPreset.HIDE_TELEMETRY)
                && CaptureFlowClassifier.isTelemetry(flow)) {
            return false;
        }
        return !activeViewPresets.contains(CaptureViewPreset.API_ONLY)
                || CaptureFlowClassifier.isApiTraffic(flow);
    }

    static boolean matchesSourceDisplayFilter(CaptureFlow flow, String includeSource, String excludeSource) {
        String source = sourceFilterValue(flow);
        String include = cleanFilterValue(includeSource);
        String exclude = cleanFilterValue(excludeSource);
        if (!include.isBlank() && !source.equals(include)) {
            return false;
        }
        return exclude.isBlank() || !source.equals(exclude);
    }

    private Comparator<CaptureFlow> errorPriorityComparator() {
        return (left, right) -> {
            int priority = Integer.compare(CaptureFlowClassifier.errorPriority(left), CaptureFlowClassifier.errorPriority(right));
            if (priority != 0) {
                return priority;
            }
            return Integer.compare(right.sequence(), left.sequence());
        };
    }

    private void appendTableContextMenu(JPopupMenu menu) {
        List<CaptureFlow> flows = selectedFlows();
        CaptureFlow flow = flows.isEmpty() ? null : flows.get(0);
        String selectedHost = selectedHost(flows);
        String selectedSource = selectedSource(flows);
        CaptureSourceInfo selectedSourceInfo = selectedSourceInfo(flows);
        String selectedPid = selectedSourceInfo == null ? "" : cleanFilterValue(selectedSourceInfo.processId());
        String selectedProcess = selectedSourceInfo == null ? "" : cleanFilterValue(selectedSourceInfo.processName());
        JMenuItem onlyRequestItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_CONTEXT_ONLY_REQUEST));
        onlyRequestItem.setEnabled(!operationInProgress && flow != null);
        onlyRequestItem.addActionListener(e -> applyOnlyRequestFilter(flow));

        JMenuItem onlyHostItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_CONTEXT_ONLY_HOST));
        onlyHostItem.setEnabled(!operationInProgress && !selectedHost.isBlank());
        onlyHostItem.addActionListener(e -> applyOnlyHostFilter(selectedHost));

        JMenuItem onlyPathItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_CONTEXT_ONLY_PATH));
        onlyPathItem.setEnabled(!operationInProgress && flow != null && !flow.path().isBlank());
        onlyPathItem.addActionListener(e -> applyOnlyPathFilter(flow.path()));

        JMenuItem excludeHostItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_CONTEXT_EXCLUDE_HOST));
        excludeHostItem.setEnabled(!operationInProgress && !selectedHost.isBlank());
        excludeHostItem.addActionListener(e -> applyExcludeHostFilter(selectedHost));

        JMenuItem excludePathItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_CONTEXT_EXCLUDE_PATH));
        excludePathItem.setEnabled(!operationInProgress && flow != null && !flow.path().isBlank());
        excludePathItem.addActionListener(e -> applyExcludePathFilter(flow.path()));

        JMenuItem onlyMethodItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_CONTEXT_ONLY_METHOD));
        onlyMethodItem.setEnabled(!operationInProgress && flow != null && !flow.method().isBlank());
        onlyMethodItem.addActionListener(e -> applyOnlyMethodFilter(flow.method()));

        JMenuItem onlyPidItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_CONTEXT_ONLY_PID));
        onlyPidItem.setEnabled(!operationInProgress && !selectedPid.isBlank());
        onlyPidItem.addActionListener(e -> applyOnlyPidFilter(selectedPid));

        JMenuItem onlyProcessItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_CONTEXT_ONLY_PROCESS));
        onlyProcessItem.setEnabled(!operationInProgress && !selectedProcess.isBlank());
        onlyProcessItem.addActionListener(e -> applyOnlyProcessFilter(selectedProcess));

        JMenuItem excludePidItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_CONTEXT_EXCLUDE_PID));
        excludePidItem.setEnabled(!operationInProgress && !selectedPid.isBlank());
        excludePidItem.addActionListener(e -> applyExcludePidFilter(selectedPid));

        JMenuItem excludeProcessItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_CONTEXT_EXCLUDE_PROCESS));
        excludeProcessItem.setEnabled(!operationInProgress && !selectedProcess.isBlank());
        excludeProcessItem.addActionListener(e -> applyExcludeProcessFilter(selectedProcess));

        JMenuItem onlySourceItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_CONTEXT_ONLY_SOURCE));
        onlySourceItem.setEnabled(flow != null && !selectedSource.isBlank());
        onlySourceItem.addActionListener(e -> applyOnlySourceViewFilter(selectedSource));

        JMenuItem excludeSourceItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_CONTEXT_EXCLUDE_SOURCE));
        excludeSourceItem.setEnabled(flow != null && !selectedSource.isBlank());
        excludeSourceItem.addActionListener(e -> applyExcludeSourceViewFilter(selectedSource));

        JMenuItem clearSourceFilterItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_CONTEXT_CLEAR_SOURCE_FILTER));
        clearSourceFilterItem.setEnabled(hasActiveSourceDisplayFilter());
        clearSourceFilterItem.addActionListener(e -> clearSourceViewFilter());

        JMenuItem resetSortItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_CONTEXT_RESET_SORT));
        resetSortItem.addActionListener(e -> {
            resetTableSort();
            NotificationCenter.showInfo(t(MessageKeys.TOOLBOX_CAPTURE_SORT_RESET));
        });

        JCheckBoxMenuItem errorPriorityItem = new JCheckBoxMenuItem(
                t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_ERROR_PRIORITY),
                activeViewPresets.contains(CaptureViewPreset.ERROR_PRIORITY));
        errorPriorityItem.addActionListener(e -> setViewPresetSelected(CaptureViewPreset.ERROR_PRIORITY, errorPriorityItem.isSelected()));

        JMenuItem importItem = new JMenuItem(t(MessageKeys.TOOLBOX_CAPTURE_IMPORT));
        importItem.setEnabled(!operationInProgress && !flows.isEmpty());
        importItem.addActionListener(e -> importSelectedFlows());
        menu.addSeparator();
        menu.add(onlyRequestItem);
        menu.add(onlyHostItem);
        menu.add(onlyPathItem);
        menu.add(onlyMethodItem);
        menu.add(onlyPidItem);
        menu.add(onlyProcessItem);
        menu.add(excludeHostItem);
        menu.add(excludePathItem);
        menu.add(excludePidItem);
        menu.add(excludeProcessItem);
        menu.addSeparator();
        menu.add(onlySourceItem);
        menu.add(excludeSourceItem);
        menu.add(clearSourceFilterItem);
        menu.addSeparator();
        menu.add(errorPriorityItem);
        menu.add(resetSortItem);
        menu.addSeparator();
        menu.add(importItem);
    }

    private String selectedHost(List<CaptureFlow> flows) {
        if (flows == null || flows.isEmpty()) {
            return "";
        }
        String host = flows.get(0).host();
        return host == null ? "" : host.trim();
    }

    private String selectedSource(List<CaptureFlow> flows) {
        if (flows == null || flows.isEmpty()) {
            return "";
        }
        return sourceFilterValue(flows.get(0));
    }

    private CaptureSourceInfo selectedSourceInfo(List<CaptureFlow> flows) {
        if (flows == null || flows.isEmpty()) {
            return null;
        }
        return flows.get(0).sourceInfo();
    }

    private void applyOnlySourceViewFilter(String source) {
        sourceIncludeFilter = cleanFilterValue(source);
        sourceExcludeFilter = "";
        updateCaptureFilterSummary();
        refreshTable();
    }

    private void applyExcludeSourceViewFilter(String source) {
        sourceIncludeFilter = "";
        sourceExcludeFilter = cleanFilterValue(source);
        updateCaptureFilterSummary();
        refreshTable();
    }

    private void clearSourceViewFilter() {
        sourceIncludeFilter = "";
        sourceExcludeFilter = "";
        updateCaptureFilterSummary();
        refreshTable();
        NotificationCenter.showInfo(t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_SOURCE_CLEARED));
    }

    private void applyOnlyHostFilter(String host) {
        applyCaptureFilterText(
                CaptureFilterExpressions.onlyHost(host),
                MessageKeys.TOOLBOX_CAPTURE_FILTER_UPDATED_ONLY_HOST,
                host
        );
    }

    private void applyOnlyRequestFilter(CaptureFlow flow) {
        applyCaptureFilterText(
                CaptureFilterExpressions.onlyRequest(flow.method(), flow.host(), flow.path()),
                MessageKeys.TOOLBOX_CAPTURE_FILTER_UPDATED_ONLY_REQUEST,
                flow.method() + " " + flow.host() + flow.path()
        );
    }

    private void applyOnlyPathFilter(String path) {
        applyCaptureFilterText(
                CaptureFilterExpressions.onlyPath(path),
                MessageKeys.TOOLBOX_CAPTURE_FILTER_UPDATED_ONLY_PATH,
                path
        );
    }

    private void applyExcludePathFilter(String path) {
        applyCaptureFilterText(
                CaptureFilterExpressions.excludePath(captureFilterField.getText(), path),
                MessageKeys.TOOLBOX_CAPTURE_FILTER_UPDATED_EXCLUDE_PATH,
                path
        );
    }

    private void applyOnlyMethodFilter(String method) {
        applyCaptureFilterText(
                CaptureFilterExpressions.onlyMethod(method),
                MessageKeys.TOOLBOX_CAPTURE_FILTER_UPDATED_ONLY_METHOD,
                method
        );
    }

    private void applyOnlyPidFilter(String pid) {
        applyCaptureFilterText(
                CaptureFilterExpressions.onlyPid(pid),
                MessageKeys.TOOLBOX_CAPTURE_FILTER_UPDATED_ONLY_PID,
                pid
        );
    }

    private void applyOnlyProcessFilter(String processName) {
        applyCaptureFilterText(
                CaptureFilterExpressions.onlyProcess(processName),
                MessageKeys.TOOLBOX_CAPTURE_FILTER_UPDATED_ONLY_PROCESS,
                processName
        );
    }

    private void applyExcludeHostFilter(String host) {
        applyCaptureFilterText(
                CaptureFilterExpressions.excludeHost(captureFilterField.getText(), host),
                MessageKeys.TOOLBOX_CAPTURE_FILTER_UPDATED_EXCLUDE_HOST,
                host
        );
    }

    private void applyExcludePidFilter(String pid) {
        applyCaptureFilterText(
                CaptureFilterExpressions.excludePid(captureFilterField.getText(), pid),
                MessageKeys.TOOLBOX_CAPTURE_FILTER_UPDATED_EXCLUDE_PID,
                pid
        );
    }

    private void applyExcludeProcessFilter(String processName) {
        applyCaptureFilterText(
                CaptureFilterExpressions.excludeProcess(captureFilterField.getText(), processName),
                MessageKeys.TOOLBOX_CAPTURE_FILTER_UPDATED_EXCLUDE_PROCESS,
                processName
        );
    }

    private void applyCaptureFilterText(String filterText, String successKey, String host) {
        syncingQuickFilters = true;
        try {
            captureFilterField.setText(filterText);
        } finally {
            syncingQuickFilters = false;
        }
        syncQuickFilterButtonsFromField();
        if (applyCaptureFilterFromField()) {
            NotificationCenter.showSuccess(t(successKey, host));
        } else {
            NotificationCenter.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_FILTER_UPDATE_FAILED, filterText));
        }
    }

    private void resetTableSort() {
        activeViewPresets.remove(CaptureViewPreset.ERROR_PRIORITY);
        syncViewPresetButtons();
        if (tablePanel != null) {
            tablePanel.setSort(1, false);
        }
    }

    private void setViewPresetSelected(CaptureViewPreset preset, boolean selected) {
        JToggleButton button = viewPresetButtons.get(preset);
        if (button != null && button.isSelected() != selected) {
            button.setSelected(selected);
        }
        toggleViewPreset(preset, selected);
    }

    private void syncViewPresetButtons() {
        viewPresetButtons.forEach((preset, button) -> button.setSelected(activeViewPresets.contains(preset)));
    }

    private List<String> selectedFlowIds() {
        if (tablePanel == null || tablePanel.getTable() == null) {
            return List.of();
        }
        JTable table = tablePanel.getTable();
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows == null || selectedRows.length == 0) {
            return List.of();
        }
        List<String> flowIds = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            Object flowId = table.getValueAt(selectedRow, 0);
            if (flowId != null) {
                flowIds.add(String.valueOf(flowId));
            }
        }
        return flowIds;
    }

    private void restoreSelectedRows(List<String> flowIds) {
        if (flowIds == null || flowIds.isEmpty() || tablePanel == null || tablePanel.getTable() == null) {
            return;
        }
        JTable table = tablePanel.getTable();
        table.clearSelection();
        for (int row = 0; row < table.getRowCount(); row++) {
            Object flowId = table.getValueAt(row, 0);
            if (flowId != null && flowIds.contains(String.valueOf(flowId))) {
                table.addRowSelectionInterval(row, row);
            }
        }
    }

    private void handleSelectionChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting() || refreshingTable) {
            return;
        }
        JTable table = tablePanel.getTable();
        if (table.getSelectedRow() < 0 && !detailPanelVisible) {
            clearDetail();
        }
    }

    private void showTableFlowDetailAtRow(int row) {
        if (tablePanel == null || tablePanel.getTable() == null) {
            return;
        }
        JTable table = tablePanel.getTable();
        if (row < 0 || row >= table.getRowCount()) {
            return;
        }
        Object flowId = table.getValueAt(row, 0);
        CaptureFlow flow = proxyService.sessionStore().find(String.valueOf(flowId));
        if (flow != null) {
            showFlowDetail(flow);
        }
    }

    private void showFlowDetail(CaptureFlow flow) {
        selectedFlow = flow;
        updateDetailHeader(flow);
        updateDetailAreas(flow, false);
        detailTabs.setSelectedIndex(0);
        showDetailPanel();
    }

    private void updateStatus() {
        boolean running = proxyService.isRunning();
        boolean busy = operationInProgress;
        updateToggleProxyButton(running, busy);
        clearButton.setEnabled(!busy);
        hostField.setEnabled(!busy && !running);
        portSpinner.setEnabled(!busy && !running);
        captureFilterField.setEnabled(!busy);
        setQuickFiltersEnabled(!busy);
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
        applyCaptureFilterFromField();
    }

    private void updateCaptureFilterSummary() {
        String captureSummary = proxyService.isRunning()
                ? proxyService.captureFilterSummary()
                : summarizeDraftCaptureFilter(captureFilterField.getText());
        captureFilterLabel.setText(captureSummary + "    " + viewFilterSummary());
    }

    private boolean applyCaptureFilterFromField() {
        String filterText = captureFilterField.getText().trim();
        try {
            CaptureRequestFilter.parse(filterText);
            if (proxyService.isRunning()) {
                proxyService.updateCaptureFilter(filterText);
            }
            saveCurrentSettings(filterText);
            updateCaptureFilterSummary();
            return true;
        } catch (IllegalArgumentException ex) {
            captureFilterLabel.setText(summarizeDraftCaptureFilter(filterText) + "    " + viewFilterSummary());
            return false;
        }
    }

    private void saveCurrentSettings(String captureFilter) {
        settingsStore.save(new CaptureSettings(
                hostField.getText().trim(),
                ((Number) portSpinner.getValue()).intValue(),
                syncSystemProxyCheckBox.isSelected(),
                captureFilter,
                currentMaxFlows()
        ));
    }

    private void handleRetentionLimitChanged() {
        int maxFlows = currentMaxFlows();
        proxyService.sessionStore().setMaxFlows(maxFlows);
        saveCurrentSettings(captureFilterField.getText().trim());
        refreshTable();
    }

    private int currentMaxFlows() {
        Object selected = retentionLimitComboBox == null ? null : retentionLimitComboBox.getSelectedItem();
        if (selected instanceof Number number) {
            return CaptureSessionStore.normalizeMaxFlows(number.intValue());
        }
        return proxyService.sessionStore().maxFlows();
    }

    static String formatRetainedRowCount(int filteredRows, int totalRows, int maxFlows) {
        int normalizedMaxFlows = CaptureSessionStore.normalizeMaxFlows(maxFlows);
        if (filteredRows != totalRows) {
            return t(MessageKeys.TOOLBOX_CAPTURE_ROWS_FILTERED_LIMIT,
                    String.valueOf(filteredRows),
                    String.valueOf(totalRows),
                    String.valueOf(normalizedMaxFlows));
        }
        return t(MessageKeys.TOOLBOX_CAPTURE_ROWS_LIMIT,
                String.valueOf(totalRows),
                String.valueOf(normalizedMaxFlows));
    }

    static String summarizeDraftCaptureFilter(String rawValue) {
        try {
            return CaptureRequestFilter.parse(rawValue).summary();
        } catch (IllegalArgumentException ex) {
            String draft = rawValue == null ? "" : rawValue.trim();
            return t(MessageKeys.TOOLBOX_CAPTURE_FILTER_INVALID, draft.isEmpty() ? "..." : draft);
        }
    }

    private String viewFilterSummary() {
        List<String> labels = new ArrayList<>();
        for (CaptureViewPreset preset : CaptureViewPreset.values()) {
            if (activeViewPresets.contains(preset)) {
                labels.add(viewPresetText(preset));
            }
        }
        if (!sourceIncludeFilter.isBlank()) {
            labels.add(t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_SOURCE_ONLY, sourceIncludeFilter));
        }
        if (!sourceExcludeFilter.isBlank()) {
            labels.add(t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_SOURCE_EXCLUDED, sourceExcludeFilter));
        }
        if (labels.isEmpty()) {
            return t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_ALL);
        }
        return t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_RULES, String.join(", ", labels));
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
        addQuickFilterButton(panel, "sse", t(MessageKeys.TOOLBOX_CAPTURE_QUICK_FILTER_SSE));
        addQuickFilterButton(panel, "websocket", t(MessageKeys.TOOLBOX_CAPTURE_QUICK_FILTER_WS));
        return panel;
    }

    private JPanel buildViewPresetPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 0, gapx 6, gapy 0, novisualpadding", "[]0[]0[]0[]0[]0[]0[]", "[]"));
        panel.setOpaque(false);
        addViewPresetButton(panel, CaptureViewPreset.ERRORS_ONLY, t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_ERRORS));
        addViewPresetButton(panel, CaptureViewPreset.SLOW_ONLY, t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_SLOW));
        addViewPresetButton(panel, CaptureViewPreset.HIDE_STATIC, t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_HIDE_STATIC));
        addViewPresetButton(panel, CaptureViewPreset.HIDE_TELEMETRY, t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_HIDE_TELEMETRY));
        addViewPresetButton(panel, CaptureViewPreset.API_ONLY, t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_API_ONLY));
        addViewPresetButton(panel, CaptureViewPreset.ERROR_PRIORITY, t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_ERROR_PRIORITY));
        return panel;
    }

    private void addQuickFilterButton(JPanel panel, String token, String text) {
        JToggleButton button = new QuickFilterButton(text);
        button.addActionListener(e -> toggleQuickFilterToken(token, button.isSelected()));
        quickFilterButtons.put(token, button);
        panel.add(button);
    }

    private void addViewPresetButton(JPanel panel, CaptureViewPreset preset, String text) {
        JToggleButton button = new QuickFilterButton(text);
        button.addActionListener(e -> toggleViewPreset(preset, button.isSelected()));
        viewPresetButtons.put(preset, button);
        panel.add(button);
    }

    private void toggleQuickFilterToken(String token, boolean selected) {
        if (syncingQuickFilters) {
            return;
        }
        List<String> tokens = new ArrayList<>(CaptureQuickFilterTokens.parse(captureFilterField.getText()));
        CaptureQuickFilterTokens.removeToken(tokens, token, selected);
        if (selected) {
            tokens.add(token);
        }
        syncingQuickFilters = true;
        try {
            captureFilterField.setText(String.join(" ", tokens));
        } finally {
            syncingQuickFilters = false;
        }
        syncQuickFilterButtonsFromField();
        applyCaptureFilterFromField();
    }

    private void syncQuickFilterButtonsFromField() {
        List<String> tokens = CaptureQuickFilterTokens.parse(captureFilterField.getText());
        syncingQuickFilters = true;
        try {
            quickFilterButtons.forEach((token, button) -> button.setSelected(CaptureQuickFilterTokens.hasIncludedToken(tokens, token)));
        } finally {
            syncingQuickFilters = false;
        }
    }

    private void setQuickFiltersEnabled(boolean enabled) {
        quickFilterButtons.values().forEach(button -> button.setEnabled(enabled));
        viewPresetButtons.values().forEach(button -> button.setEnabled(enabled));
    }

    private void toggleViewPreset(CaptureViewPreset preset, boolean selected) {
        if (selected) {
            activeViewPresets.add(preset);
        } else {
            activeViewPresets.remove(preset);
        }
        if (preset == CaptureViewPreset.ERROR_PRIORITY) {
            if (selected) {
                tablePanel.clearSort();
            } else {
                resetTableSort();
            }
        }
        updateCaptureFilterSummary();
        refreshTable();
    }

    private boolean isCertificateInstallSupported() {
        return macCertificateInstallService.isSupported() || windowsCertificateInstallService.isSupported();
    }

    private void updateCaptureStatusLabel() {
        CaptureStatusSnapshot snapshot = captureStatusSnapshot;
        CaptureTrustStatus trustStatus = snapshot.trustStatus();
        String tooltip = buildCaptureStatusTooltip(snapshot);
        captureTrustChipLabel.setChip(resolveTrustChipText(trustStatus), resolveTrustChipColor(trustStatus));
        captureProxyChipLabel.setChip(resolveProxyChipText(), resolveProxyChipColor());
        captureTrustChipLabel.setToolTipText(tooltip);
        captureProxyChipLabel.setToolTipText(tooltip);
        captureStatusPanel.setToolTipText(tooltip);
        refreshStatusPopup(snapshot);
    }

    private void refreshStatusPopup() {
        refreshStatusPopup(captureStatusSnapshot);
    }

    private void refreshStatusPopup(CaptureStatusSnapshot snapshot) {
        CaptureTrustStatus trustStatus = snapshot.trustStatus();
        statusPopupTrustLabel.setChip(resolveTrustChipText(trustStatus), resolveTrustChipColor(trustStatus));
        statusPopupProxyLabel.setChip(resolveProxyChipText(), resolveProxyChipColor());
        statusPopupPathLabel.setText(snapshot.caPathText());
        String detail = trustStatus.detail();
        statusPopupDetailLabel.setText(detail.isBlank()
                ? ""
                : "<html><div style='width:320px'>" + escapeHtml(detail).replace("\n", "<br>") + "</div></html>");
    }

    private void showStatusPopup(JComponent invoker, int x, int y) {
        refreshStatusPopup();
        refreshCaptureStatusAsync(false);
        statusPopupMenu.show(invoker, x, y);
    }

    private String buildCaptureStatusTooltip(CaptureStatusSnapshot snapshot) {
        StringBuilder builder = new StringBuilder("<html>");
        builder.append(escapeHtml(snapshot.caPathText()));
        String detail = snapshot.trustStatus().detail();
        if (!detail.isBlank()) {
            builder.append("<br>").append(escapeHtml(detail));
        }
        builder.append("<br>").append(escapeHtml(systemProxySummaryText()));
        builder.append("</html>");
        return builder.toString();
    }

    private void refreshCaptureStatusAsync(boolean force) {
        SwingWorker<CaptureStatusSnapshot, Void> currentWorker = statusRefreshWorker;
        if (currentWorker != null && !currentWorker.isDone()) {
            if (!force) {
                return;
            }
            currentWorker.cancel(true);
        }
        if (force || captureStatusSnapshot.trustStatus().checking()) {
            captureStatusSnapshot = checkingStatusSnapshot();
            updateCaptureStatusLabel();
        }

        SwingWorker<CaptureStatusSnapshot, Void> worker = new SwingWorker<>() {
            @Override
            protected CaptureStatusSnapshot doInBackground() {
                return resolveCaptureStatusSnapshot();
            }

            @Override
            protected void done() {
                if (statusRefreshWorker != this || isCancelled()) {
                    return;
                }
                try {
                    captureStatusSnapshot = get();
                } catch (Exception ex) {
                    captureStatusSnapshot = unavailableStatusSnapshot(ex);
                }
                updateCaptureStatusLabel();
            }
        };
        statusRefreshWorker = worker;
        worker.execute();
    }

    private CaptureStatusSnapshot resolveCaptureStatusSnapshot() {
        try {
            String certificatePath = proxyService.rootCertificatePath();
            CaptureTrustStatus trustStatus = resolveCaptureTrustStatus(certificatePath);
            return new CaptureStatusSnapshot(trustStatus, t(MessageKeys.TOOLBOX_CAPTURE_CA_PATH, certificatePath));
        } catch (Exception ex) {
            return unavailableStatusSnapshot(ex);
        }
    }

    private CaptureStatusSnapshot checkingStatusSnapshot() {
        return new CaptureStatusSnapshot(
                new CaptureTrustStatus(false, false, false, true, false, ""),
                t(MessageKeys.TOOLBOX_CAPTURE_CA_PATH_CHECKING)
        );
    }

    private CaptureStatusSnapshot unavailableStatusSnapshot(Exception ex) {
        String message = rootMessage(ex);
        String detail = message == null || message.isBlank()
                ? t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_UNKNOWN)
                : t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_UNKNOWN) + ": " + message;
        return new CaptureStatusSnapshot(
                new CaptureTrustStatus(false, false, false, false, true, detail),
                t(MessageKeys.TOOLBOX_CAPTURE_CA_PATH_UNAVAILABLE)
        );
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

    private String resolveTrustChipText(CaptureTrustStatus trustStatus) {
        if (trustStatus.checking()) {
            return t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_CHECKING);
        }
        if (trustStatus.unknown()) {
            return t(MessageKeys.TOOLBOX_CAPTURE_CA_TRUST_UNKNOWN);
        }
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
        if (trustStatus.checking() || trustStatus.unknown()) {
            return ModernColors.getNeutral();
        }
        if (!trustStatus.supported()) {
            return ModernColors.getNeutral();
        }
        if (trustStatus.trusted()) {
            return ModernColors.getSuccess();
        }
        if (trustStatus.installed()) {
            return ModernColors.getWarningDark();
        }
        return ModernColors.getError();
    }

    private String resolveProxyChipText() {
        return systemProxySummaryText();
    }

    private Color resolveProxyChipColor() {
        if (!proxyService.isSystemProxySyncSupported()) {
            return ModernColors.getNeutral();
        }
        if (proxyService.isSystemProxySynced()) {
            return ModernColors.getInfo();
        }
        if (!proxyService.isRunning() && syncSystemProxyCheckBox != null && syncSystemProxyCheckBox.isSelected()) {
            return ModernColors.getWarningDark();
        }
        return ModernColors.getAccent();
    }

    private CaptureTrustStatus resolveCaptureTrustStatus(String certificatePath) throws Exception {
        if (macCertificateInstallService.isSupported()) {
            MacCertificateInstallService.CertificateTrustStatus trustStatus =
                    macCertificateInstallService.trustStatus(certificatePath);
            return new CaptureTrustStatus(true, trustStatus.installed(), trustStatus.trusted(), false, false, trustStatus.detail());
        }
        if (windowsCertificateInstallService.isSupported()) {
            WindowsCertificateInstallService.WindowsTrustStatus trustStatus =
                    windowsCertificateInstallService.trustStatus(certificatePath);
            return new CaptureTrustStatus(true, trustStatus.installed(), trustStatus.trusted(), false, false, trustStatus.detail());
        }
        return new CaptureTrustStatus(false, false, false, false, false, "");
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    static String htmlTooltip(String value, int width) {
        String escaped = (value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        int normalizedWidth = Math.max(240, Math.min(width, 520));
        return "<html><div style='width:" + normalizedWidth + "px;'>" + escaped + "</div></html>";
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

    static TableColumnSpec[] captureTableColumnSpecs() {
        return new TableColumnSpec[]{
                new TableColumnSpec(0, 0, 0, 0, false),
                new TableColumnSpec(1, 56, 64, 78, true),
                new TableColumnSpec(SOURCE_COLUMN_INDEX, 78, 110, 150, true),
                new TableColumnSpec(PID_COLUMN_INDEX, 42, 54, 70, true),
                new TableColumnSpec(TYPE_COLUMN_INDEX, 48, 62, 78, true),
                new TableColumnSpec(METHOD_COLUMN_INDEX, 50, 64, 82, true),
                new TableColumnSpec(URL_COLUMN_INDEX, 200, 300, 0, true),
                new TableColumnSpec(STATUS_COLUMN_INDEX, 44, 58, 72, true),
                new TableColumnSpec(DURATION_COLUMN_INDEX, 52, 68, 82, true),
                new TableColumnSpec(SIZE_COLUMN_INDEX, 52, 68, 82, true)
        };
    }

    private void configureTableColumns(JTable table) {
        if (table.getColumnModel().getColumnCount() < TABLE_COLUMN_COUNT) {
            return;
        }
        for (TableColumnSpec spec : captureTableColumnSpecs()) {
            TableColumn column = table.getColumnModel().getColumn(spec.columnIndex());
            column.setMinWidth(spec.minWidth());
            column.setPreferredWidth(spec.preferredWidth());
            if (spec.maxWidth() > 0) {
                column.setMaxWidth(spec.maxWidth());
            }
            column.setResizable(spec.resizable());
        }
        configureSourceColumnRenderer(table);
        configurePidColumnRenderer(table);
        configureTypeColumnRenderer(table);
        configureMethodColumnRenderer(table);
        configureUrlColumnRenderer(table);
        configureStatusColumnRenderer(table);
        configureDurationColumnRenderer(table);
        configureBytesColumnRenderer(table, SIZE_COLUMN_INDEX);
    }

    private void configureSourceColumnRenderer(JTable table) {
        TableCellRenderer renderer = table.getDefaultRenderer(Object.class);
        table.getColumnModel().getColumn(SOURCE_COLUMN_INDEX).setCellRenderer((tbl, value, selected, focus, row, column) -> {
            Component component = renderer.getTableCellRendererComponent(tbl, value, selected, focus, row, column);
            if (component instanceof JLabel label) {
                String text = value == null ? "" : String.valueOf(value);
                resetCaptureTableLabel(label, selected, SwingConstants.LEFT, text.isBlank() ? null : text);
                if (!selected) {
                    label.setForeground(CaptureTableStyle.sourceForegroundFor(value));
                }
            }
            return component;
        });
    }

    private void configurePidColumnRenderer(JTable table) {
        TableCellRenderer renderer = table.getDefaultRenderer(Object.class);
        table.getColumnModel().getColumn(PID_COLUMN_INDEX).setCellRenderer((tbl, value, selected, focus, row, column) -> {
            Component component = renderer.getTableCellRendererComponent(tbl, value, selected, focus, row, column);
            if (component instanceof JLabel label) {
                String text = value == null ? "" : String.valueOf(value);
                resetCaptureTableLabel(label, selected, SwingConstants.RIGHT, text.isBlank() || "-".equals(text) ? null : text);
                if (!selected) {
                    label.setForeground(ModernColors.getTextSecondary());
                }
            }
            return component;
        });
    }

    private void configureTypeColumnRenderer(JTable table) {
        TableCellRenderer renderer = table.getDefaultRenderer(Object.class);
        table.getColumnModel().getColumn(TYPE_COLUMN_INDEX).setCellRenderer((tbl, value, selected, focus, row, column) -> {
            Component component = renderer.getTableCellRendererComponent(tbl, value, selected, focus, row, column);
            if (component instanceof JLabel label) {
                resetCaptureTableLabel(label, selected, SwingConstants.LEFT, null);
                if (!selected) {
                    label.setForeground(CaptureTableStyle.typeForegroundFor(value));
                }
            }
            return component;
        });
    }

    private void configureMethodColumnRenderer(JTable table) {
        TableCellRenderer renderer = table.getDefaultRenderer(Object.class);
        table.getColumnModel().getColumn(METHOD_COLUMN_INDEX).setCellRenderer((tbl, value, selected, focus, row, column) -> {
            Component component = renderer.getTableCellRendererComponent(tbl, value, selected, focus, row, column);
            if (component instanceof JLabel label) {
                resetCaptureTableLabel(label, selected, SwingConstants.LEFT, null);
                if (!selected) {
                    label.setForeground(CaptureTableStyle.methodForegroundFor(value));
                }
            }
            return component;
        });
    }

    private void configureUrlColumnRenderer(JTable table) {
        TableCellRenderer renderer = table.getDefaultRenderer(Object.class);
        table.getColumnModel().getColumn(URL_COLUMN_INDEX).setCellRenderer((tbl, value, selected, focus, row, column) -> {
            Component component = renderer.getTableCellRendererComponent(tbl, value, selected, focus, row, column);
            if (component instanceof JLabel label) {
                String text = value == null ? "" : String.valueOf(value);
                resetCaptureTableLabel(label, selected, SwingConstants.LEFT, text.isBlank() ? null : text);
            }
            return component;
        });
    }

    private void configureStatusColumnRenderer(JTable table) {
        TableCellRenderer renderer = table.getDefaultRenderer(Object.class);
        table.getColumnModel().getColumn(STATUS_COLUMN_INDEX).setCellRenderer((tbl, value, selected, focus, row, column) -> {
            Component component = renderer.getTableCellRendererComponent(tbl, value, selected, focus, row, column);
            if (component instanceof JLabel label) {
                resetCaptureTableLabel(label, selected, SwingConstants.LEFT, null);
                if (!selected) {
                    label.setForeground(CaptureStatusStyle.tableForegroundFor(value));
                }
            }
            return component;
        });
    }

    private void configureDurationColumnRenderer(JTable table) {
        TableCellRenderer renderer = table.getDefaultRenderer(Object.class);
        table.getColumnModel().getColumn(DURATION_COLUMN_INDEX).setCellRenderer((tbl, value, selected, focus, row, column) -> {
            Component component = renderer.getTableCellRendererComponent(tbl, value, selected, focus, row, column);
            if (component instanceof JLabel label) {
                resetCaptureTableLabel(label, selected, SwingConstants.RIGHT, value == null ? null : value + " ms");
                label.setText(CaptureValueFormat.duration(value));
                if (!selected) {
                    label.setForeground(CaptureTableStyle.durationForegroundFor(value));
                }
            }
            return component;
        });
    }

    private void configureBytesColumnRenderer(JTable table, int columnIndex) {
        TableCellRenderer renderer = table.getDefaultRenderer(Object.class);
        table.getColumnModel().getColumn(columnIndex).setCellRenderer((tbl, value, selected, focus, row, column) -> {
            Component component = renderer.getTableCellRendererComponent(tbl, value, selected, focus, row, column);
            if (component instanceof JLabel label) {
                resetCaptureTableLabel(label, selected, SwingConstants.RIGHT,
                        value == null ? null : value + " " + t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_BYTES));
                label.setText(CaptureValueFormat.bytes(value));
                if (!selected) {
                    label.setForeground(CaptureTableStyle.bytesForegroundFor(value));
                }
            }
            return component;
        });
    }

    private void disableTableTooltips(JTable table) {
        TableCellRenderer renderer = table.getDefaultRenderer(Object.class);
        table.setDefaultRenderer(Object.class, (tbl, value, selected, focus, row, column) -> {
            java.awt.Component component = renderer.getTableCellRendererComponent(tbl, value, selected, focus, row, column);
            if (component instanceof JComponent jComponent) {
                jComponent.setToolTipText(null);
            }
            if (component instanceof JLabel label) {
                resetCaptureTableLabel(label, selected, SwingConstants.LEFT, null);
            }
            return component;
        });
        table.setToolTipText(null);
        table.getTableHeader().setToolTipText(null);
    }

    static void resetCaptureTableLabel(JLabel label, boolean selected, int alignment, String tooltip) {
        if (label == null) {
            return;
        }
        label.setHorizontalAlignment(alignment);
        label.setToolTipText(tooltip);
        if (!selected) {
            label.setForeground(ModernColors.getTextPrimary());
        }
    }

    private String defaultHost() {
        return initialSettings.bindHost();
    }

    private int defaultPort() {
        return initialSettings.bindPort();
    }

    private boolean defaultSyncSystemProxy() {
        return initialSettings.syncSystemProxy();
    }

    private String defaultCaptureFilter() {
        return initialSettings.hostFilter();
    }

    private int defaultMaxFlows() {
        return initialSettings.maxFlows();
    }

    private void openCa() {
        try {
            String caPath = proxyService.rootCertificatePath();
            openCertificate(caPath);
            NotificationCenter.showInfo(t(MessageKeys.TOOLBOX_CAPTURE_OPEN_CA_SUCCESS));
        } catch (Exception ex) {
            NotificationCenter.showError(t(MessageKeys.TOOLBOX_CAPTURE_OPEN_CA_FAILED, ex.getMessage()));
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
        NotificationCenter.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_TOOLTIP_UNSUPPORTED));
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
                    NotificationCenter.showSuccess(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_SUCCESS_SYSTEM, removedMessage));
                } else if (loginInstallAttempted) {
                    NotificationCenter.showSuccess(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_SUCCESS_LOGIN, removedMessage));
                } else {
                    NotificationCenter.showSuccess(removedMessage);
                }
            } else if (trustStatus.installed()) {
                macCertificateInstallService.openCertificate(caPath);
                showManualTrustGuide(caPath, trustStatus.detail());
                NotificationCenter.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_WARN_TRUST));
            } else {
                macCertificateInstallService.openCertificate(caPath);
                showManualTrustGuide(caPath, trustStatus.detail());
                NotificationCenter.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_WARN_VISIBLE));
            }
        } catch (Exception ex) {
            NotificationCenter.showError(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_FAILED, ex.getMessage()));
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
                NotificationCenter.showSuccess(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_SUCCESS_WINDOWS,
                        t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_REMOVED_NONE)));
            } else if (trustStatus.installed()) {
                windowsCertificateInstallService.openCertificate(caPath);
                showWindowsManualTrustGuide(caPath, trustStatus.detail());
                NotificationCenter.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_WARN_TRUST));
            } else {
                windowsCertificateInstallService.openCertificate(caPath);
                showWindowsManualTrustGuide(caPath, trustStatus.detail());
                NotificationCenter.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_WARN_VISIBLE));
            }
        } catch (Exception ex) {
            NotificationCenter.showError(t(MessageKeys.TOOLBOX_CAPTURE_INSTALL_CA_FAILED, ex.getMessage()));
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
        setChipText(detailMethodLabel, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_METHOD) + ": -");
        setChipText(detailProtocolLabel, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_PROTOCOL) + ": -");
        setStatusChip(detailStatusLabel, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_STATUS) + ": -", null);
        setChipText(detailDurationLabel, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_DURATION) + ": -");
        detailTimeLabel.setText("-");
        detailTimeLabel.setToolTipText(null);
        String idleText = idleDetailText();
        requestDetailArea.setText(idleText);
        requestDetailArea.setCaretPosition(0);
        responseDetailArea.setText(idleText);
        responseDetailArea.setCaretPosition(0);
        streamDetailArea.setText(idleText);
        streamDetailArea.setCaretPosition(0);
        streamDetailArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        diagnosticsDetailArea.setText(idleText);
        diagnosticsDetailArea.setCaretPosition(0);
        diagnosticsDetailArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        detailTabs.setSelectedIndex(0);
    }

    private String idleDetailText() {
        if (totalFlowCount == 0) {
            return proxyService.isRunning()
                    ? t(MessageKeys.TOOLBOX_CAPTURE_EMPTY_RUNNING)
                    : t(MessageKeys.TOOLBOX_CAPTURE_IDLE_DETAILS);
        }
        if (visibleFlowCount == 0 && hasActiveViewFilter()) {
            return t(MessageKeys.TOOLBOX_CAPTURE_EMPTY_VIEW_FILTER_NO_MATCH);
        }
        if (!proxyService.isRunning()) {
            return t(MessageKeys.TOOLBOX_CAPTURE_EMPTY_STOPPED_HISTORY);
        }
        return t(MessageKeys.TOOLBOX_CAPTURE_EMPTY_SELECT_DETAIL);
    }

    private boolean hasActiveViewFilter() {
        if (hasActiveSourceDisplayFilter()) {
            return true;
        }
        for (CaptureViewPreset preset : activeViewPresets) {
            if (preset != CaptureViewPreset.ERROR_PRIORITY) {
                return true;
            }
        }
        return false;
    }

    private boolean hasActiveSourceDisplayFilter() {
        return !sourceIncludeFilter.isBlank() || !sourceExcludeFilter.isBlank();
    }

    private void updateDetailHeader(CaptureFlow flow) {
        setChipText(detailMethodLabel, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_METHOD) + ": " + flow.method());
        setChipText(detailProtocolLabel, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_PROTOCOL) + ": " + flow.protocolText());
        setStatusChip(detailStatusLabel,
                t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_STATUS) + ": " + flow.statusDisplayText(),
                flow.statusCode());
        setChipText(detailDurationLabel, t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_DURATION) + ": " + CaptureValueFormat.duration(flow.durationMs()));
        detailTimeLabel.setText(flow.startedAtText());
        detailTimeLabel.setToolTipText(flow.startedAtText());
    }

    private void copyDetail() {
        String detailText = activeDetailTextForCopy().trim();
        if (detailText.isEmpty()) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(detailText), null);
        NotificationCenter.showSuccess(t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_COPIED));
    }

    private void copyAsCurl() {
        if (selectedFlow == null) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(selectedFlow.curlCommand()), null);
        if (selectedFlow.curlBodyPartial()) {
            NotificationCenter.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_CURL_COPIED_PARTIAL));
        } else {
            NotificationCenter.showSuccess(t(MessageKeys.TOOLBOX_CAPTURE_CURL_COPIED));
        }
    }

    private void importSelectedFlows() {
        List<CaptureFlow> flows = selectedFlows();
        if (flows.isEmpty()) {
            NotificationCenter.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_IMPORT_EMPTY));
            return;
        }
        requestCollectionImporter.importFlows(flows);
    }

    private RSyntaxTextArea createDetailArea() {
        RSyntaxTextArea area = new RSyntaxTextArea();
        area.setEditable(false);
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        area.setCodeFoldingEnabled(true);
        area.setAntiAliasingEnabled(true);
        area.setHighlightCurrentLine(true);
        area.setLineWrap(true);
        area.setWrapStyleWord(false);
        area.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        EditorThemeUtil.loadTheme(area);
        return area;
    }

    private JTextArea createPlainDetailArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(false);
        area.setWrapStyleWord(false);
        area.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        area.setForeground(ModernColors.getTextPrimary());
        area.setBackground(ModernColors.getCardBackgroundColor());
        area.setBorder(new EmptyBorder(8, 10, 8, 10));
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
        return buildPlainDetailTabPanel(requestDetailArea);
    }

    private JComponent buildResponseDetailTab() {
        return buildPlainDetailTabPanel(responseDetailArea);
    }

    private JComponent buildStreamDetailTab() {
        return buildSearchableDetailTabPanel(streamDetailArea);
    }

    private JComponent buildDiagnosticsDetailTab() {
        return buildSearchableDetailTabPanel(diagnosticsDetailArea);
    }

    private JComponent buildPlainDetailTabPanel(JTextArea detailArea) {
        JScrollPane scrollPane = new JScrollPane(
                detailArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    private JComponent buildSearchableDetailTabPanel(RSyntaxTextArea detailArea) {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        SearchableTextArea searchableDetail = new SearchableTextArea(detailArea, false);
        searchableDetail.setLineNumbersEnabled(false);
        panel.add(searchableDetail, BorderLayout.CENTER);
        return panel;
    }

    private void updateDetailAreas(CaptureFlow flow) {
        updateDetailAreas(flow, true);
    }

    private void updateDetailAreas(CaptureFlow flow, boolean preserveView) {
        updateDetailArea(requestDetailArea, displayDetailTextForTab(flow, 0), preserveView);
        updateDetailArea(responseDetailArea, displayDetailTextForTab(flow, 1), preserveView);
        updateDetailArea(streamDetailArea, displayDetailTextForTab(flow, 2), preserveView);
        updateDetailArea(diagnosticsDetailArea, displayDetailTextForTab(flow, 3), preserveView);
    }

    private void updateDetailArea(JTextArea area, String text, boolean preserveView) {
        if (area instanceof RSyntaxTextArea syntaxArea) {
            syntaxArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        }
        if (preserveView) {
            updateDetailAreaTextPreservingView(area, text);
            return;
        }
        area.setText(text == null ? "" : text);
        area.setCaretPosition(0);
    }

    static void updateDetailAreaTextPreservingView(JTextComponent area, String text) {
        if (area == null) {
            return;
        }
        String normalized = text == null ? "" : text;
        if (normalized.equals(area.getText())) {
            return;
        }
        int caretPosition = Math.min(area.getCaretPosition(), normalized.length());
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, area);
        Point viewPosition = viewport == null ? null : viewport.getViewPosition();
        area.setText(normalized);
        area.setCaretPosition(Math.min(caretPosition, area.getDocument().getLength()));
        if (viewport != null && viewPosition != null) {
            SwingUtilities.invokeLater(() -> restoreDetailViewportPosition(area, viewport, viewPosition));
        }
    }

    private static void restoreDetailViewportPosition(JTextComponent area, JViewport viewport, Point viewPosition) {
        int maxX = Math.max(0, area.getWidth() - viewport.getExtentSize().width);
        int maxY = Math.max(0, area.getHeight() - viewport.getExtentSize().height);
        viewport.setViewPosition(new Point(
                Math.min(viewPosition.x, maxX),
                Math.min(viewPosition.y, maxY)
        ));
    }

    private String displayValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String sourceFilterValue(CaptureFlow flow) {
        if (flow == null || flow.sourceInfo() == null) {
            return "";
        }
        return cleanFilterValue(flow.sourceInfo().sourceTableText());
    }

    private static String cleanFilterValue(String value) {
        return value == null ? "" : value.trim();
    }

    private JTextArea activeDetailArea() {
        int index = detailTabs.getSelectedIndex();
        if (index == 0) {
            return requestDetailArea;
        }
        if (index == 1) {
            return responseDetailArea;
        }
        if (index == 2) {
            return streamDetailArea;
        }
        return diagnosticsDetailArea;
    }

    private String activeDetailTextForCopy() {
        if (selectedFlow != null) {
            return copyDetailText(selectedFlow);
        }
        JTextArea activeArea = activeDetailArea();
        return activeArea == null ? "" : activeArea.getText();
    }

    static String displayDetailTextForTab(CaptureFlow flow, int tabIndex) {
        String detailText = detailTextForTab(flow, tabIndex);
        if (tabIndex == 0 || tabIndex == 1) {
            return displayRequestResponseDetailText(detailText);
        }
        return detailText;
    }

    static String copyDetailText(CaptureFlow flow) {
        return flow == null ? "" : flow.detailText();
    }

    static String detailTextForTab(CaptureFlow flow, int tabIndex) {
        if (flow == null) {
            return "";
        }
        if (tabIndex == 0) {
            return flow.requestDetailText();
        }
        if (tabIndex == 1) {
            return flow.responseDetailText();
        }
        if (tabIndex == 2) {
            return flow.streamDetailText();
        }
        return flow.diagnosticsDetailText();
    }

    static String displayRequestResponseDetailText(String text) {
        String normalized = text == null ? "" : text;
        if (normalized.length() <= REQUEST_RESPONSE_DETAIL_DISPLAY_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, REQUEST_RESPONSE_DETAIL_DISPLAY_LIMIT)
                + "\n\n"
                + t(MessageKeys.TOOLBOX_CAPTURE_DETAIL_DISPLAY_TRUNCATED,
                REQUEST_RESPONSE_DETAIL_DISPLAY_LIMIT,
                normalized.length());
    }

    private List<CaptureFlow> selectedFlows() {
        if (tablePanel == null || tablePanel.getTable() == null) {
            return List.of();
        }
        JTable table = tablePanel.getTable();
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows == null || selectedRows.length == 0) {
            return List.of();
        }
        List<CaptureFlow> flows = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            Object flowId = table.getValueAt(selectedRow, 0);
            CaptureFlow flow = proxyService.sessionStore().find(String.valueOf(flowId));
            if (flow != null) {
                flows.add(flow);
            }
        }
        return flows;
    }

    private JLabel buildChipLabel(String text, Color bgColor) {
        return new ChipLabel(text, bgColor);
    }

    private JButton createToolWindowToolbarButton(String iconPath, String tooltip) {
        JButton button = new JButton(IconUtil.createThemed(iconPath, IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        return button;
    }

    private void setStatusChip(JLabel label, String text, Object statusValue) {
        setChipText(label, text);
        if (label instanceof ChipLabel chipLabel) {
            chipLabel.setAccentColor(CaptureStatusStyle.accentFor(statusValue));
        }
    }

    private void setChipText(JLabel label, String text) {
        setChipText(label, text, null);
    }

    private void setChipText(JLabel label, String text, String tooltip) {
        label.setText(text);
        label.setToolTipText(tooltip);
    }

    private static String ellipsize(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value;
        }
        if (maxChars <= 3) {
            return value.substring(0, maxChars);
        }
        return value.substring(0, maxChars - 3) + "...";
    }

    private String viewPresetText(CaptureViewPreset preset) {
        return switch (preset) {
            case ERRORS_ONLY -> t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_ERRORS);
            case SLOW_ONLY -> t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_SLOW);
            case HIDE_STATIC -> t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_HIDE_STATIC);
            case HIDE_TELEMETRY -> t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_HIDE_TELEMETRY);
            case API_ONLY -> t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_API_ONLY);
            case ERROR_PRIORITY -> t(MessageKeys.TOOLBOX_CAPTURE_VIEW_FILTER_ERROR_PRIORITY);
        };
    }

    private enum CaptureViewPreset {
        ERRORS_ONLY,
        SLOW_ONLY,
        HIDE_STATIC,
        HIDE_TELEMETRY,
        API_ONLY,
        ERROR_PRIORITY
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
            Color borderColor = isSelected() ? ModernColors.getPrimary() : ModernColors.getDividerBorderColor();
            if (borderColor == null) {
                borderColor = ModernColors.getNeutral();
            }
            if (!isEnabled()) {
                setForeground(ModernColors.getTextDisabled());
            } else if (isSelected()) {
                setForeground(ChipLabel.foregroundFor(borderColor));
            } else {
                setForeground(ModernColors.getTextPrimary());
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color borderColor = isSelected() ? ModernColors.getPrimary() : ModernColors.getDividerBorderColor();
            if (borderColor == null) {
                borderColor = ModernColors.getNeutral();
            }
            Color fillColor;
            if (!isEnabled()) {
                fillColor = ChipLabel.fillFor(borderColor, 18);
            } else if (isSelected()) {
                fillColor = ChipLabel.fillFor(borderColor, 40);
            } else {
                fillColor = ChipLabel.fillFor(borderColor, 12);
            }
            RoundRectangle2D.Float chipShape = new RoundRectangle2D.Float(
                    0.5f, 0.5f,
                    Math.max(0f, getWidth() - 1f),
                    Math.max(0f, getHeight() - 1f),
                    10, 10);
            g2.setColor(fillColor);
            g2.fill(chipShape);
            g2.setColor(ChipLabel.borderFor(borderColor, isSelected() ? 160 : 90));
            g2.draw(chipShape);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class StatusChipLabel extends ChipLabel {
        private StatusChipLabel() {
            super();
        }
    }

    private String[] columnNames() {
        return new String[]{
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_ID),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_SEQ),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_SOURCE),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_PID),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_TYPE),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_METHOD),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_URL),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_STATUS),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_DURATION_MS),
                t(MessageKeys.TOOLBOX_CAPTURE_COLUMN_SIZE)
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

    record TableColumnSpec(int columnIndex, int minWidth, int preferredWidth, int maxWidth, boolean resizable) {
    }

    private record CaptureStatusSnapshot(CaptureTrustStatus trustStatus, String caPathText) {
    }

    private record CaptureTrustStatus(boolean supported,
                                      boolean installed,
                                      boolean trusted,
                                      boolean checking,
                                      boolean unknown,
                                      String detail) {
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
