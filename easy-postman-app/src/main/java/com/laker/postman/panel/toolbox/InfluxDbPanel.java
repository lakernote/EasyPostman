package com.laker.postman.panel.toolbox;

import com.laker.postman.common.component.notification.NotificationCenter;

import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.FallbackAwareRSyntaxTextArea;
import com.laker.postman.common.component.AppToolWindowChrome;
import com.laker.postman.common.component.ToolWindowSidebarHeader;
import com.laker.postman.common.component.ToolWindowSidebarToolbar;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.*;
import com.laker.postman.common.component.connection.ConnectionToolbarUi;
import com.laker.postman.common.component.dialog.TextInputDialog;
import com.laker.postman.common.component.table.EnhancedTablePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.http.runtime.okhttp.OkHttpClientManager;
import com.laker.postman.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;
import okhttp3.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import tools.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

@Slf4j
public class InfluxDbPanel extends JPanel {

    enum QueryMode {
        INFLUXQL_V1,
        FLUX_V2
    }

    private record TemplateItem(String nameKey, String query) {
    }

    private record HttpResult(int code, String body, long costMs) {
    }

    private static class TagConditionRow {
        JPanel panel;
        EasyComboBox<String> keyCombo;
        EasyComboBox<String> valueCombo;
    }

    private JComboBox<InfluxDbConnectionProfile> profileCombo;
    private JButton newProfileBtn;
    private JButton saveProfileBtn;
    private JButton saveAsProfileBtn;
    private JButton deleteProfileBtn;
    private JTextField hostField;
    private JComboBox<QueryMode> modeCombo;

    private JTextField tokenField;
    private JTextField orgField;

    private EasyComboBox<String> dbCombo;
    private EasyComboBox<String> measurementCombo;
    private DefaultListModel<String> measurementListModel;
    private DefaultListModel<String> measurementFilteredModel;
    private JList<String> measurementList;
    private SearchTextField measurementSearchField;
    private JTextField userField;
    private JPasswordField passwordField;

    private JButton connectBtn;
    private JButton disconnectBtn;
    private CardLayout btnCardLayout;
    private JPanel btnCard;
    private final InfluxDbConnectionProfileStore connectionProfileStore = new InfluxDbConnectionProfileStore();
    private boolean loadingConnectionProfiles;

    private RSyntaxTextArea queryEditor;
    private RSyntaxTextArea resultArea;
    private SearchableTextArea searchableResultArea;
    private SearchableTextArea searchableQueryArea;
    private EnhancedTablePanel resultTablePanel;
    private JTabbedPane resultTabs;
    private CompactPrimaryButton executeBtn;
    private JLabel respStatusLabel;
    private JLabel queryLabel;

    private JComboBox<String> templateCombo;
    private TemplateItem[] currentTemplates = new TemplateItem[]{};

    private JPanel v1QueryBuilderPanel;
    private EasyComboBox<String> fieldCombo;

    private JPanel tagRowsPanel;
    private final List<TagConditionRow> tagRows = new ArrayList<>();
    private List<String> cachedTagKeys = new ArrayList<>();
    private boolean suppressComboEvents = false;

    private String baseUrl = "http://localhost:8086";
    private boolean connected = false;
    private String lastResponseBody = "";
    private boolean suppressMeasurementSync = false;
    private boolean suppressModeSwitch = false;

    private static final int MAX_HISTORY = 20;
    private final Deque<HistoryEntry> requestHistory = new ArrayDeque<>();
    private DefaultListModel<HistoryEntry> historyListModel;
    private JList<HistoryEntry> historyList;

    private static final String AUTHORIZATION = "Authorization";
    private static final String TOKEN_PREFIX = "Token ";
    private static final String APPLICATION_VND_FLUX = "application/vnd.flux";
    private static final String TEXT_CSV = "text/csv";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int WRITE_TIMEOUT_MS = 30_000;
    private static final String CONNECT_CARD = "connect";
    private static final String DISCONNECT_CARD = "disconnect";
    private static final int HOST_FIELD_WIDTH = 240;
    private static final int MODE_FIELD_WIDTH = 90;
    private static final int TOKEN_FIELD_WIDTH = HOST_FIELD_WIDTH;
    private static final int ORG_FIELD_WIDTH = 130;
    private static final int DB_FIELD_WIDTH = HOST_FIELD_WIDTH;
    private static final int MEASUREMENT_FIELD_WIDTH = 130;
    private static final int AUTH_FIELD_WIDTH = 120;
    private static final int QUERY_TOOLBAR_CONTROL_HEIGHT = 32;

    @RequiredArgsConstructor
    private static class HistoryEntry {
        final QueryMode mode;
        final String db;
        final String org;
        final String measurement;
        final String query;
    }

    private static final TemplateItem[] FLUX_TEMPLATES = {
            new TemplateItem(MessageKeys.TOOLBOX_INFLUX_TEMPLATE_FLUX_LATEST_100,
                    """
                            from(bucket: "example")
                              |> range(start: -1h)
                              |> sort(columns: ["_time"], desc: true)
                              |> limit(n: 100)
                            """),
            new TemplateItem(MessageKeys.TOOLBOX_INFLUX_TEMPLATE_FLUX_COUNT_BY_MEASUREMENT,
                    """
                            from(bucket: "example")
                              |> range(start: -24h)
                              |> group(columns: ["_measurement"])
                              |> count()
                            """),
            new TemplateItem(MessageKeys.TOOLBOX_INFLUX_TEMPLATE_MEAN_BY_1M,
                    """
                            from(bucket: "example")
                              |> range(start: -6h)
                              |> aggregateWindow(every: 1m, fn: mean, createEmpty: false)
                              |> yield(name: "mean")
                            """),
            new TemplateItem(MessageKeys.TOOLBOX_INFLUX_TEMPLATE_FLUX_TOP_10_VALUES,
                    """
                            from(bucket: "example")
                              |> range(start: -24h)
                              |> sort(columns: ["_value"], desc: true)
                              |> limit(n: 10)
                            """),
            new TemplateItem(MessageKeys.TOOLBOX_INFLUX_TEMPLATE_FLUX_GROUP_BY_TAG,
                    """
                            from(bucket: "example")
                              |> range(start: -24h)
                              |> filter(fn: (r) => r._measurement == "cpu")
                              |> group(columns: ["host"])
                              |> last()
                            """),
            new TemplateItem(MessageKeys.TOOLBOX_INFLUX_TEMPLATE_FLUX_FIELD_SELECTOR,
                    """
                            from(bucket: "example")
                              |> range(start: -1h)
                              |> filter(fn: (r) => r._measurement == "cpu")
                              |> filter(fn: (r) => r._field == "usage_user")
                              |> limit(n: 200)
                            """),
            new TemplateItem(MessageKeys.TOOLBOX_INFLUX_TEMPLATE_FLUX_PIVOT_VIEW,
                    """
                            from(bucket: "example")
                              |> range(start: -30m)
                              |> filter(fn: (r) => r._measurement == "cpu")
                              |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
                            """)
    };

    private static final TemplateItem[] INFLUXQL_TEMPLATES = {
            new TemplateItem(MessageKeys.TOOLBOX_INFLUX_TEMPLATE_INFLUXQL_LATEST_100,
                    """
                            SELECT *
                            FROM ${measurement}
                            ORDER BY time DESC
                            LIMIT 100
                            """),
            new TemplateItem(MessageKeys.TOOLBOX_INFLUX_TEMPLATE_INFLUXQL_COUNT_LAST_1H,
                    """
                            SELECT COUNT(*)
                            FROM ${measurement}
                            WHERE time > now() - 1h
                            """),
            new TemplateItem(MessageKeys.TOOLBOX_INFLUX_TEMPLATE_MEAN_BY_1M,
                    """
                            SELECT MEAN("value")
                            FROM ${measurement}
                            WHERE time > now() - 6h
                            GROUP BY time(1m)
                            FILL(null)
                            """),
            new TemplateItem(MessageKeys.TOOLBOX_INFLUX_TEMPLATE_INFLUXQL_TAG_VALUES,
                    """
                            SHOW TAG VALUES
                            FROM ${measurement}
                            WITH KEY = "host"
                            LIMIT 200
                            """),
            new TemplateItem(MessageKeys.TOOLBOX_INFLUX_TEMPLATE_INFLUXQL_FIELD_KEYS,
                    """
                            SHOW FIELD KEYS
                            FROM ${measurement}
                            """),
            new TemplateItem(MessageKeys.TOOLBOX_INFLUX_TEMPLATE_INFLUXQL_SERIES_CARDINALITY,
                    """
                            SHOW SERIES CARDINALITY
                            FROM ${measurement}
                            """),
            new TemplateItem(MessageKeys.TOOLBOX_INFLUX_TEMPLATE_INFLUXQL_LAST_POINT_PER_HOST,
                    """
                            SELECT LAST("value")
                            FROM ${measurement}
                            WHERE time > now() - 24h
                            GROUP BY "host"
                            """)
    };

    public InfluxDbPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        ToolWindowSurfaceStyle.applyCard(this);
        add(buildConnectionPanel(), BorderLayout.NORTH);
        JSplitPane mainSplit = AppToolWindowChrome.createHorizontalInnerSplitPane(
                buildLeftPanel(),
                buildMainPanel(),
                240
        );
        mainSplit.setDividerLocation(240);
        mainSplit.setResizeWeight(0.25);
        add(mainSplit, BorderLayout.CENTER);
        switchMode(getSelectedMode());
        ToolWindowSurfaceStyle.applyPanelTreeCard(this);
    }

    private JPanel buildLeftPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(230, 0));

        JTabbedPane leftTabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        ToolWindowSurfaceStyle.applyTabbedPaneCard(leftTabs);
        leftTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_TAB), buildMeasurementPanel());
        leftTabs.setToolTipTextAt(0, I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_MANAGEMENT));
        leftTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_HISTORY), buildHistoryPanel());

        wrapper.add(leftTabs, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildMeasurementPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setPreferredSize(new Dimension(230, 0));
        panel.setBorder(BorderFactory.createEmptyBorder());

        RefreshButton refreshBtn = new RefreshButton();
        refreshBtn.addActionListener(e -> {
            if (!connected || getSelectedMode() != QueryMode.INFLUXQL_V1) return;
            String db = getSelectedDatabase();
            if (!db.isBlank()) {
                loadMeasurements(db);
            }
        });
        ToolWindowSidebarHeader titleBar = new ToolWindowSidebarHeader(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_MANAGEMENT), refreshBtn);

        measurementSearchField = new SearchTextField();
        measurementSearchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_SEARCH_PLACEHOLDER));
        measurementSearchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        measurementListModel = new DefaultListModel<>();
        measurementFilteredModel = new DefaultListModel<>();
        measurementList = new JList<>(measurementFilteredModel);
        measurementList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        measurementList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || suppressMeasurementSync) return;
            List<String> selectedValues = measurementList.getSelectedValuesList();
            if (selectedValues.size() == 1) {
                String selected = selectedValues.get(0);
                if (selected != null && !selected.isBlank()) {
                    selectMeasurementFromList(selected);
                }
            }
        });
        measurementList.addMouseListener(buildMeasurementListMouseListener());

        JScrollPane listScroll = new JScrollPane(measurementList);
        ToolWindowSurfaceStyle.applyListScrollPaneCard(listScroll, measurementList);

        measurementSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filterMeasurements(measurementSearchField.getText());
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filterMeasurements(measurementSearchField.getText());
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filterMeasurements(measurementSearchField.getText());
            }
        });

        JPanel topArea = new JPanel(new BorderLayout());
        topArea.add(titleBar, BorderLayout.NORTH);
        topArea.add(new ToolWindowSidebarToolbar(null, measurementSearchField), BorderLayout.CENTER);

        panel.add(topArea, BorderLayout.NORTH);
        panel.add(listScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));

        ClearButton clearHistBtn = new ClearButton();
        clearHistBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_HISTORY_CLEAR));
        clearHistBtn.addActionListener(e -> {
            requestHistory.clear();
            historyListModel.clear();
        });
        ToolWindowSidebarHeader titleBar = new ToolWindowSidebarHeader(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_HISTORY), clearHistBtn);

        historyListModel = new DefaultListModel<>();
        historyList = new JList<>(historyListModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer(new HistoryCellRenderer());
        historyList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (e.getClickCount() != 2) return;
                int idx = historyList.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    applyHistory(historyListModel.get(idx));
                }
            }
        });

        JScrollPane scroll = new JScrollPane(historyList);
        ToolWindowSurfaceStyle.applyListScrollPaneCard(scroll, historyList);

        JLabel tipLbl = new JLabel("<html><center><small>" +
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_HISTORY_EMPTY) + "</small></center></html>");
        tipLbl.setHorizontalAlignment(SwingConstants.CENTER);
        tipLbl.setForeground(ModernColors.getTextSecondary());
        tipLbl.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));

        panel.add(titleBar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(tipLbl, BorderLayout.SOUTH);
        return panel;
    }

    private static class HistoryCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            if (value instanceof HistoryEntry h) {
                String modeColor = h.mode == QueryMode.INFLUXQL_V1
                        ? ModernColors.toHtmlColor(ModernColors.getPrimary())
                        : ModernColors.toHtmlColor(ModernColors.getSuccess());
                String modeText = h.mode == QueryMode.INFLUXQL_V1 ? "InfluxQL" : "Flux";
                String query = h.query == null ? "" : h.query.strip();
                // Collapse all whitespace/newlines into a single space for the preview
                String queryPreview = query.replaceAll("\\s+", " ").trim();
                if (queryPreview.length() > 60) queryPreview = queryPreview.substring(0, 59) + "…";
                // Context info: db/measurement for V1, org for V2
                String context;
                if (h.mode == QueryMode.INFLUXQL_V1) {
                    String db = h.db == null || h.db.isBlank() ? "-" : h.db;
                    String ms = h.measurement == null || h.measurement.isBlank() ? "-" : h.measurement;
                    context = escapeHtml(db) + " / " + escapeHtml(ms);
                } else {
                    String org = h.org == null || h.org.isBlank() ? "-" : h.org;
                    context = escapeHtml(org);
                }
                lbl.setText("<html>"
                        + "<b><font color='" + modeColor + "'>" + modeText + "</font></b>"
                        + " <font color='" + ModernColors.toHtmlColor(ModernColors.getTextHint()) + "'><small>"
                        + context + "</small></font><br>"
                        + "<small>" + escapeHtml(queryPreview) + "</small>"
                        + "</html>");
                // Full query in tooltip
                lbl.setToolTipText("<html><pre style='font-size:11px'>" + escapeHtml(query) + "</pre></html>");
            }
            return lbl;
        }

        private static String escapeHtml(String text) {
            if (text == null) return "";
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }


    private void applyHistory(HistoryEntry entry) {
        if (entry == null) return;
        // suppressModeSwitch 防止 setSelectedItem 触发 switchMode 重置连接状态
        suppressModeSwitch = true;
        modeCombo.setSelectedItem(entry.mode);
        suppressModeSwitch = false;
        // 只更新 UI 外观，不碰连接状态
        updateModeUI(entry.mode);
        if (entry.mode == QueryMode.INFLUXQL_V1) {
            setComboEditorText(dbCombo, entry.db);
            setComboEditorText(measurementCombo, entry.measurement);
            if (!entry.db.isBlank() && connected) {
                // fromHistory=true：跳过 measurementCombo model 替换（防闪烁）和模板刷新（防覆盖 query）
                loadMeasurements(entry.db, true);
            }
        } else {
            orgField.setText(entry.org);
        }
        queryEditor.setText(entry.query);
        queryEditor.setCaretPosition(0);
    }

    /**
     * Update mode-specific UI elements (query label, button text, syntax, panels, templates)
     * WITHOUT touching the connection state. Used when restoring history entries.
     */
    private void updateModeUI(QueryMode mode) {
        showModeFields(mode);
        if (mode == QueryMode.INFLUXQL_V1) {
            queryLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_QUERY_TITLE_V1));
            updateExecuteButtonForMode(mode);
            queryEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
            if (v1QueryBuilderPanel != null) v1QueryBuilderPanel.setVisible(true);
        } else {
            queryLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_QUERY_TITLE_V2));
            updateExecuteButtonForMode(mode);
            queryEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
            if (v1QueryBuilderPanel != null) v1QueryBuilderPanel.setVisible(false);
        }
        revalidate();
        repaint();
    }

    private void updateExecuteButtonForMode(QueryMode mode) {
        executeBtn.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_EXECUTE_SHORT));
        executeBtn.setToolTipText(I18nUtil.getMessage(mode == QueryMode.INFLUXQL_V1
                ? MessageKeys.TOOLBOX_INFLUX_EXECUTE_V1
                : MessageKeys.TOOLBOX_INFLUX_EXECUTE_V2));
    }

    private void addToHistory(QueryMode mode, String db, String org, String measurement, String query) {
        String dbVal = db == null ? "" : db;
        String orgVal = org == null ? "" : org;
        String measurementVal = measurement == null ? "" : measurement;
        String queryVal = query == null ? "" : query;
        for (HistoryEntry e : requestHistory) {
            if (e.mode == mode && e.db.equals(dbVal) && e.org.equals(orgVal)
                    && e.measurement.equals(measurementVal) && e.query.equals(queryVal)) {
                requestHistory.remove(e);
                requestHistory.addFirst(e);
                rebuildHistoryListModel();
                return;
            }
        }
        requestHistory.addFirst(new HistoryEntry(mode, dbVal, orgVal, measurementVal, queryVal));
        while (requestHistory.size() > MAX_HISTORY) requestHistory.removeLast();
        rebuildHistoryListModel();
    }

    private void rebuildHistoryListModel() {
        historyListModel.clear();
        for (HistoryEntry e : requestHistory) historyListModel.addElement(e);
    }

    private java.awt.event.MouseAdapter buildMeasurementListMouseListener() {
        return new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
                    int idx = measurementList.locationToIndex(evt.getPoint());
                    if (idx >= 0) {
                        measurementList.setSelectedIndex(idx);
                        String measurement = measurementList.getSelectedValue();
                        executeMeasurementQuickQuery(measurement);
                    }
                }
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (evt.isPopupTrigger()) maybeShowMeasurementPopup(evt);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (evt.isPopupTrigger()) maybeShowMeasurementPopup(evt);
            }
        };
    }

    private void executeMeasurementQuickQuery(String measurement) {
        if (measurement == null || measurement.isBlank()) return;
        if (!connected) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_NOT_CONNECTED));
            return;
        }
        if (getSelectedMode() != QueryMode.INFLUXQL_V1) return;
        String db = getSelectedDatabase();
        if (db.isBlank()) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_DB_REQUIRED));
            return;
        }

        // 双击即查询：优先沿用当前模板并注入 measurement；无模板时回退到默认查询
        if (templateCombo != null && templateCombo.getSelectedIndex() >= 0) {
            loadTemplate();
        } else {
            queryEditor.setText("SELECT * FROM " + quoteIdentifier(measurement) + " ORDER BY time DESC LIMIT 100");
            queryEditor.setCaretPosition(0);
        }
        executeQuery();
    }

    private void maybeShowMeasurementPopup(java.awt.event.MouseEvent evt) {
        if (getSelectedMode() != QueryMode.INFLUXQL_V1) return;

        int idx = measurementList.locationToIndex(evt.getPoint());
        if (idx >= 0 && !measurementList.isSelectedIndex(idx)) {
            measurementList.setSelectedIndex(idx);
        }

        List<String> measurements = new ArrayList<>(measurementList.getSelectedValuesList());
        String db = getSelectedDatabase();

        JPopupMenu menu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);
        JMenuItem deleteItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_DELETE));
        deleteItem.setEnabled(connected && !measurements.isEmpty() && !db.isBlank());
        deleteItem.addActionListener(e -> deleteMeasurements(db, measurements));
        menu.add(deleteItem);

        JMenuItem clearItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_CLEAR));
        clearItem.setEnabled(connected && !measurements.isEmpty() && !db.isBlank());
        clearItem.addActionListener(e -> clearMeasurementsData(db, measurements));
        menu.add(clearItem);

        menu.show(measurementList, evt.getX(), evt.getY());
    }

    private void deleteMeasurements(String db, List<String> measurements) {
        List<String> valid = normalizeMeasurementTargets(measurements);
        if (!connected || db.isBlank() || valid.isEmpty()) return;

        String confirmMsg = valid.size() == 1
                ? MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_DELETE_CONFIRM), valid.get(0))
                : MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_DELETE_BATCH_CONFIRM), valid.size());
        int opt = JOptionPane.showConfirmDialog(this,
                confirmMsg,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_DELETE_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;

        runMeasurementMutation(db, valid,
                true,
                MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_DELETE_SUCCESS,
                MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_DELETE_BATCH_SUCCESS,
                MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_DELETE_FAILED);
    }

    private void clearMeasurementsData(String db, List<String> measurements) {
        List<String> valid = normalizeMeasurementTargets(measurements);
        if (!connected || db.isBlank() || valid.isEmpty()) return;

        String confirmMsg = valid.size() == 1
                ? MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_CLEAR_CONFIRM), valid.get(0))
                : MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_CLEAR_BATCH_CONFIRM), valid.size());
        int opt = JOptionPane.showConfirmDialog(this,
                confirmMsg,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_CLEAR_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;

        runMeasurementMutation(db, valid,
                false,
                MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_CLEAR_SUCCESS,
                MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_CLEAR_BATCH_SUCCESS,
                MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_CLEAR_FAILED);
    }

    private List<String> normalizeMeasurementTargets(List<String> measurements) {
        if (measurements == null || measurements.isEmpty()) return List.of();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String measurement : measurements) {
            if (measurement != null) {
                String trimmed = measurement.trim();
                if (!trimmed.isBlank()) set.add(trimmed);
            }
        }
        return new ArrayList<>(set);
    }

    private void runMeasurementMutation(String db, List<String> measurements, boolean dropMeasurement,
                                        String singleSuccessMsgKey, String batchSuccessMsgKey, String failedMsgKey) {
        List<String> targets = new ArrayList<>(measurements);
        SwingWorker<HttpResult, Void> worker = new SwingWorker<>() {
            @Override
            protected HttpResult doInBackground() throws Exception {
                HttpResult lastResult = null;
                for (String measurement : targets) {
                    String query = dropMeasurement
                            ? ("DROP MEASUREMENT " + quoteIdentifier(measurement))
                            : ("DELETE FROM " + quoteIdentifier(measurement));
                    String path = "/query?db=" + enc(db) + "&q=" + enc(query);
                    HttpResult result = callHttp("GET", path, null, null, "application/json", QueryMode.INFLUXQL_V1);
                    if (result.code() < 200 || result.code() >= 300) {
                        throw new IOException("[" + measurement + "] HTTP " + result.code() + "\n" + result.body());
                    }
                    String influxError = extractInfluxError(result.body());
                    if (!influxError.isBlank()) {
                        throw new IOException("[" + measurement + "] " + influxError);
                    }
                    lastResult = result;
                }
                return lastResult;
            }

            @Override
            protected void done() {
                try {
                    HttpResult result = get();
                    if (result != null) {
                        renderResponse(result.body());
                    }
                    loadMeasurements(db);
                    if (targets.size() == 1) {
                        NotificationCenter.showSuccess(MessageFormat.format(
                                I18nUtil.getMessage(singleSuccessMsgKey), targets.get(0)));
                    } else {
                        NotificationCenter.showSuccess(MessageFormat.format(
                                I18nUtil.getMessage(batchSuccessMsgKey), targets.size()));
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("measurement mutation interrupted", ex);
                } catch (Exception ex) {
                    NotificationCenter.showError(MessageFormat.format(
                            I18nUtil.getMessage(failedMsgKey), ex.getMessage()));
                }
            }
        };
        worker.execute();
    }

    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applySectionHeader(panel, 3, 6, 3, 6);

        JPanel form = new JPanel(new MigLayout(
                "insets 0, fillx, gapy 2, novisualpadding, hidemode 3",
                ConnectionToolbarUi.compactFormColumns(),
                "[][]"
        ));

        profileCombo = new JComboBox<>();
        profileCombo.setEditable(false);
        ConnectionToolbarUi.compactControl(profileCombo);
        profileCombo.setPrototypeDisplayValue(InfluxDbConnectionProfile.builder()
                .name("Default InfluxDB")
                .build());
        profileCombo.setRenderer(ConnectionToolbarUi.displayRenderer(InfluxDbConnectionProfile::getName));
        profileCombo.addActionListener(e -> applySelectedConnectionProfile());

        newProfileBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_NEW),
                "icons/plus.svg", e -> createNewConnectionProfile());
        saveProfileBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_SAVE),
                "icons/save.svg", e -> saveCurrentConnectionProfile(true));
        saveProfileBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_SAVE) + " (Ctrl+S)");
        saveAsProfileBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_SAVE_AS),
                "icons/duplicate.svg", e -> saveCurrentConnectionProfileAs());
        deleteProfileBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_DELETE),
                "icons/delete.svg", e -> deleteSelectedConnectionProfile());

        hostField = new JTextField(baseUrl);
        ConnectionToolbarUi.compactControl(hostField);
        hostField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_HOST_PLACEHOLDER));
        hostField.addActionListener(e -> doConnect());

        modeCombo = new JComboBox<>(QueryMode.values());
        ConnectionToolbarUi.compactControl(modeCombo);
        modeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof QueryMode mode) {
                    setText(mode == QueryMode.INFLUXQL_V1
                            ? I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MODE_V1)
                            : I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MODE_V2));
                }
                return this;
            }
        });
        modeCombo.addActionListener(e -> switchMode(getSelectedMode()));

        connectBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_CONNECT), "icons/connect.svg");
        connectBtn.addActionListener(e -> doConnect());
        disconnectBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_DISCONNECT), "icons/ws-close.svg");
        disconnectBtn.addActionListener(e -> doDisconnect());

        btnCardLayout = new CardLayout();
        btnCard = new JPanel(btnCardLayout);
        btnCard.setOpaque(false);
        btnCard.add(connectBtn, CONNECT_CARD);
        btnCard.add(disconnectBtn, DISCONNECT_CARD);
        btnCardLayout.show(btnCard, CONNECT_CARD);

        JPanel mainRow = new JPanel(new MigLayout(
                "insets 0, fillx, novisualpadding, gapx 0",
                ConnectionToolbarUi.profileActionColumns()
                        + ConnectionToolbarUi.connectionFieldColumns(HOST_FIELD_WIDTH) + "4"
                        + ConnectionToolbarUi.wideConnectionFieldColumns(MODE_FIELD_WIDTH)
                        + "6[]push",
                "[]"
        ));
        mainRow.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE)));
        mainRow.add(profileCombo);
        mainRow.add(newProfileBtn);
        mainRow.add(saveProfileBtn);
        mainRow.add(saveAsProfileBtn);
        mainRow.add(deleteProfileBtn);
        mainRow.add(ConnectionToolbarUi.verticalSeparator(),
                "w 1!, h " + ConnectionToolbarUi.VERTICAL_SEPARATOR_HEIGHT + "!");
        mainRow.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_HOST)));
        mainRow.add(hostField);
        mainRow.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MODE)));
        mainRow.add(modeCombo);
        mainRow.add(btnCard, "h " + ConnectionToolbarUi.CONNECTION_BUTTON_HEIGHT + "!");

        JPanel modeFields = new JPanel(new CardLayout());
        modeFields.add(buildV2FieldsPanel(), QueryMode.FLUX_V2.name());
        modeFields.add(buildV1FieldsPanel(), QueryMode.INFLUXQL_V1.name());

        form.add(mainRow, "wrap");
        form.add(modeFields);

        panel.add(form, BorderLayout.CENTER);
        ConnectionToolbarUi.lockConnectionPanelHeight(panel, true);
        panel.putClientProperty("modeFields", modeFields);
        ConnectionToolbarUi.registerSaveShortcut(form, () -> saveCurrentConnectionProfile(true));
        loadSavedConnectionProfiles(null);
        return panel;
    }

    private JPanel buildV2FieldsPanel() {
        JPanel row = new JPanel(new MigLayout(
                "insets 2 0 2 0, fillx, novisualpadding, gapx 0",
                ConnectionToolbarUi.profileActionColumns()
                        + ConnectionToolbarUi.connectionFieldColumns(TOKEN_FIELD_WIDTH) + "4"
                        + ConnectionToolbarUi.wideConnectionFieldColumns(ORG_FIELD_WIDTH) + "push",
                "[]"
        ));

        tokenField = new JTextField("", 26);
        ConnectionToolbarUi.compactControl(tokenField);
        tokenField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_TOKEN_PLACEHOLDER));
        tokenField.addActionListener(e -> doConnect());

        orgField = new JTextField("", 16);
        ConnectionToolbarUi.compactControl(orgField);
        orgField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ORG_PLACEHOLDER));
        orgField.addActionListener(e -> doConnect());

        row.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_TOKEN)), "skip 7");
        row.add(tokenField);
        row.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ORG)));
        row.add(orgField);
        return row;
    }

    private JPanel buildV1FieldsPanel() {
        JPanel row = new JPanel(new MigLayout(
                "insets 2 0 2 0, fillx, novisualpadding, gapx 0",
                ConnectionToolbarUi.profileActionColumns()
                        + ConnectionToolbarUi.connectionFieldColumns(DB_FIELD_WIDTH) + "4"
                        + ConnectionToolbarUi.wideConnectionFieldColumns(MEASUREMENT_FIELD_WIDTH) + "4"
                        + ConnectionToolbarUi.connectionFieldColumns(AUTH_FIELD_WIDTH) + "4"
                        + ConnectionToolbarUi.wideConnectionFieldColumns(AUTH_FIELD_WIDTH)
                        + "4[]push",
                "[]"
        ));

        dbCombo = new EasyComboBox<>(EasyComboBox.WidthMode.DYNAMIC);
        ConnectionToolbarUi.compactControl(dbCombo);
        dbCombo.addActionListener(e -> {
            if (suppressComboEvents) return;
            if (getSelectedMode() != QueryMode.INFLUXQL_V1) return;
            String db = getSelectedDatabase();
            if (!db.isBlank() && connected) {
                loadMeasurements(db);
            } else if (db.isBlank()) {
                clearMeasurementList();
            }
        });

        measurementCombo = new EasyComboBox<>(EasyComboBox.WidthMode.DYNAMIC);
        ConnectionToolbarUi.compactControl(measurementCombo);
        measurementCombo.addActionListener(e -> {
            if (suppressComboEvents) return;
            if (getSelectedMode() != QueryMode.INFLUXQL_V1) return;
            String db = getSelectedDatabase();
            String measurement = getSelectedMeasurement();
            syncMeasurementListSelection(measurement);
            if (!db.isBlank() && !measurement.isBlank() && connected) {
                loadFieldKeys(db, measurement);
                loadTagKeys(db, measurement);
            }
            if (templateCombo != null && templateCombo.getSelectedIndex() >= 0) {
                loadTemplate();
            }
        });

        JButton reloadMetaBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_RELOAD_META), "icons/refresh.svg");
        reloadMetaBtn.addActionListener(e -> {
            String db = getSelectedDatabase();
            loadDatabases(() -> {
                String selectedDb = db.isBlank() ? getSelectedDatabase() : db;
                if (!selectedDb.isBlank()) loadMeasurements(selectedDb);
            });
        });

        userField = new JTextField("", 12);
        ConnectionToolbarUi.compactControl(userField);
        userField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_USER_PLACEHOLDER));

        passwordField = new JPasswordField("", 12);
        ConnectionToolbarUi.compactControl(passwordField);
        passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PASS_PLACEHOLDER));
        passwordField.addActionListener(e -> doConnect());

        row.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_DB)), "skip 7");
        row.add(dbCombo);
        row.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT)));
        row.add(measurementCombo);
        row.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_USER)));
        row.add(userField);
        row.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PASS)));
        row.add(passwordField);
        row.add(reloadMetaBtn);
        return row;
    }

    private void loadSavedConnectionProfiles(String preferredProfileId) {
        loadingConnectionProfiles = true;
        profileCombo.removeAllItems();
        List<InfluxDbConnectionProfile> profiles = connectionProfileStore.loadProfiles();
        InfluxDbConnectionProfile activeProfile = connectionProfileStore.loadActiveProfile()
                .orElse(InfluxDbConnectionProfileStore.defaultProfile());
        String selectedProfileId = preferredProfileId == null || preferredProfileId.isBlank()
                ? activeProfile.getId()
                : preferredProfileId;
        InfluxDbConnectionProfile selectedProfile = null;
        for (InfluxDbConnectionProfile profile : profiles) {
            profileCombo.addItem(profile);
            if (profile.getId().equals(selectedProfileId)) {
                selectedProfile = profile;
            }
        }
        if (selectedProfile == null && profileCombo.getItemCount() > 0) {
            selectedProfile = profileCombo.getItemAt(0);
        }
        if (selectedProfile != null) {
            profileCombo.setSelectedItem(selectedProfile);
        }
        loadingConnectionProfiles = false;
        applyConnectionProfile(selectedProfile);
        updateProfileActionState();
    }

    private void applySelectedConnectionProfile() {
        if (loadingConnectionProfiles) {
            return;
        }
        InfluxDbConnectionProfile profile = getSelectedConnectionProfile();
        applyConnectionProfile(profile);
        if (profile != null) {
            connectionProfileStore.saveProfiles(connectionProfileStore.loadProfiles(), profile.getId());
        }
        updateProfileActionState();
    }

    private void applyConnectionProfile(InfluxDbConnectionProfile profile) {
        if (profile == null) {
            return;
        }
        baseUrl = InfluxDbConnectionProfileStore.normalizeBaseUrl(profile.getBaseUrl());
        hostField.setText(baseUrl);
        setSelectedModeWithoutSwitch(parseMode(profile.getMode()));
        tokenField.setText(defaultString(profile.getToken()));
        orgField.setText(defaultString(profile.getOrg()));
        setComboEditorText(dbCombo, defaultString(profile.getDatabase()));
        setComboEditorText(measurementCombo, defaultString(profile.getMeasurement()));
        userField.setText(defaultString(profile.getUsername()));
        passwordField.setText(defaultString(profile.getPassword()));
    }

    private void createNewConnectionProfile() {
        String initialName = uniqueProfileName(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_NEW_DEFAULT));
        String name = promptProfileName(initialName, null);
        if (name == null) {
            return;
        }
        InfluxDbConnectionProfile profile = buildProfile(UUID.randomUUID().toString(), name);
        connectionProfileStore.upsertProfile(profile);
        loadSavedConnectionProfiles(profile.getId());
        NotificationCenter.showSuccess(MessageFormat.format(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_SAVED), profile.getName()));
        hostField.requestFocusInWindow();
    }

    private void saveCurrentConnectionProfile(boolean notify) {
        InfluxDbConnectionProfile selectedProfile = getSelectedConnectionProfile();
        if (selectedProfile == null) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_NOT_SELECTED));
            return;
        }
        if (getCurrentHost().isBlank()) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_HOST_REQUIRED));
            return;
        }
        InfluxDbConnectionProfile profile = buildProfile(selectedProfile.getId(), selectedProfile.getName());
        connectionProfileStore.upsertProfile(profile);
        loadSavedConnectionProfiles(profile.getId());
        if (notify) {
            NotificationCenter.showSuccess(MessageFormat.format(
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_SAVED), profile.getName()));
        }
    }

    private void saveCurrentConnectionProfileAs() {
        if (getCurrentHost().isBlank()) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_HOST_REQUIRED));
            return;
        }
        InfluxDbConnectionProfile selectedProfile = getSelectedConnectionProfile();
        String initialName = selectedProfile == null
                ? connectionProfileNameSuggestion()
                : uniqueProfileName(selectedProfile.getName());
        String name = promptProfileName(initialName, null);
        if (name == null) {
            return;
        }
        InfluxDbConnectionProfile profile = buildProfile(UUID.randomUUID().toString(), name);
        connectionProfileStore.upsertProfile(profile);
        loadSavedConnectionProfiles(profile.getId());
        NotificationCenter.showSuccess(MessageFormat.format(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_SAVED), profile.getName()));
    }

    private void saveConnectionProfile(String finalBaseUrl, boolean notify) {
        InfluxDbConnectionProfile selectedProfile = getSelectedConnectionProfile();
        if (selectedProfile == null) {
            return;
        }
        InfluxDbConnectionProfile profile = buildProfile(
                selectedProfile.getId(),
                selectedProfile.getName(),
                InfluxDbConnectionProfileStore.normalizeBaseUrl(finalBaseUrl)
        );
        connectionProfileStore.upsertProfile(profile);
        loadSavedConnectionProfiles(profile.getId());
        if (notify) {
            NotificationCenter.showSuccess(MessageFormat.format(
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_SAVED), profile.getName()));
        }
    }

    private void deleteSelectedConnectionProfile() {
        InfluxDbConnectionProfile selectedProfile = getSelectedConnectionProfile();
        if (selectedProfile == null) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_NOT_SELECTED));
            return;
        }
        if (isDefaultProfile(selectedProfile)) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_DEFAULT_NOT_DELETABLE));
            return;
        }
        int result = JOptionPane.showConfirmDialog(this,
                MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_DELETE_CONFIRM),
                        selectedProfile.getName()),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_DELETE_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        String deletedName = selectedProfile.getName();
        connectionProfileStore.deleteProfile(selectedProfile.getId());
        loadSavedConnectionProfiles(InfluxDbConnectionProfileStore.DEFAULT_PROFILE_ID);
        NotificationCenter.showInfo(MessageFormat.format(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_DELETED), deletedName));
    }

    private InfluxDbConnectionProfile buildProfile(String profileId, String profileName) {
        return buildProfile(profileId, profileName,
                InfluxDbConnectionProfileStore.normalizeBaseUrl(getCurrentHost()));
    }

    private InfluxDbConnectionProfile buildProfile(String profileId, String profileName, String normalizedBaseUrl) {
        return InfluxDbConnectionProfile.builder()
                .id(profileId)
                .name(profileName)
                .baseUrl(normalizedBaseUrl)
                .mode(getSelectedMode().name())
                .token(tokenField.getText().trim())
                .org(orgField.getText().trim())
                .database(getSelectedDatabase())
                .measurement(getSelectedMeasurement())
                .username(userField.getText().trim())
                .password(new String(passwordField.getPassword()))
                .hostHistory(currentHostHistoryWith(normalizedBaseUrl))
                .build();
    }

    private String promptProfileName(String initialValue, String existingProfileId) {
        String name = TextInputDialog.showRequiredName(this,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_SAVE_AS_TITLE),
                initialValue,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_NAME_REQUIRED)
        ).orElse(null);
        if (name == null) return null;
        if (profileNameExists(name, existingProfileId)) {
            NotificationCenter.showWarning(MessageFormat.format(
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PROFILE_NAME_EXISTS), name));
            return null;
        }
        return name;
    }

    private boolean profileNameExists(String name, String ignoredProfileId) {
        String normalizedName = name == null ? "" : name.trim();
        for (InfluxDbConnectionProfile profile : connectionProfileStore.loadProfiles()) {
            boolean sameProfile = ignoredProfileId != null && ignoredProfileId.equals(profile.getId());
            if (!sameProfile && profile.getName().equalsIgnoreCase(normalizedName)) {
                return true;
            }
        }
        return false;
    }

    private String uniqueProfileName(String baseName) {
        String normalizedBaseName = baseName == null || baseName.isBlank()
                ? connectionProfileNameSuggestion()
                : baseName.trim();
        if (!profileNameExists(normalizedBaseName, null)) {
            return normalizedBaseName;
        }
        for (int i = 2; i < 1000; i++) {
            String candidate = normalizedBaseName + " " + i;
            if (!profileNameExists(candidate, null)) {
                return candidate;
            }
        }
        return normalizedBaseName + " " + System.currentTimeMillis();
    }

    private String connectionProfileNameSuggestion() {
        return InfluxDbConnectionProfileStore.normalizeBaseUrl(getCurrentHost());
    }

    private InfluxDbConnectionProfile getSelectedConnectionProfile() {
        Object selected = profileCombo.getSelectedItem();
        return selected instanceof InfluxDbConnectionProfile profile ? profile : null;
    }

    private boolean isDefaultProfile(InfluxDbConnectionProfile profile) {
        return profile != null && InfluxDbConnectionProfileStore.DEFAULT_PROFILE_ID.equals(profile.getId());
    }

    private void updateProfileActionState() {
        InfluxDbConnectionProfile selectedProfile = getSelectedConnectionProfile();
        boolean hasProfile = selectedProfile != null;
        saveProfileBtn.setEnabled(hasProfile);
        saveAsProfileBtn.setEnabled(hasProfile);
        deleteProfileBtn.setEnabled(hasProfile && !isDefaultProfile(selectedProfile));
    }

    private List<String> currentHostHistoryWith(String activeHost) {
        InfluxDbConnectionProfile selectedProfile = getSelectedConnectionProfile();
        List<String> existingHistory = selectedProfile == null ? List.of() : selectedProfile.getHostHistory();
        return InfluxDbConnectionProfileStore.normalizeHostHistory(existingHistory, activeHost);
    }

    private void setSelectedModeWithoutSwitch(QueryMode mode) {
        suppressModeSwitch = true;
        modeCombo.setSelectedItem(mode);
        suppressModeSwitch = false;
        showModeFields(mode);
    }

    private QueryMode parseMode(String mode) {
        String normalized = InfluxDbConnectionProfileStore.normalizeMode(mode);
        return QueryMode.valueOf(normalized);
    }

    private void showModeFields(QueryMode mode) {
        JPanel modeFields = findModeFieldsPanel();
        if (modeFields != null) {
            CardLayout card = (CardLayout) modeFields.getLayout();
            card.show(modeFields, mode.name());
        }
    }

    private JPanel findModeFieldsPanel() {
        if (getComponentCount() == 0 || !(getComponent(0) instanceof JPanel connectionPanel)) {
            return null;
        }
        Object modeFields = connectionPanel.getClientProperty("modeFields");
        return modeFields instanceof JPanel panel ? panel : null;
    }

    private JPanel buildMainPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        // ---- 工具栏（MigLayout）----
        JPanel toolbar = new JPanel(new MigLayout("insets 4, fillx", "[][][]8[][][]push", "[]"));
        ToolWindowSurfaceStyle.applySectionHeader(toolbar);

        templateCombo = new JComboBox<>();
        templateCombo.setPreferredSize(new Dimension(180, QUERY_TOOLBAR_CONTROL_HEIGHT));

        JButton loadTemplateBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_LOAD_TEMPLATE), "icons/load.svg", e -> loadTemplate());

        executeBtn = new CompactPrimaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_EXECUTE_SHORT), "icons/send.svg");
        executeBtn.addActionListener(e -> executeQuery());

        JButton copyBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_COPY_RESULT), "icons/copy.svg", e -> copyResult());

        JButton clearBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_CLEAR), "icons/clear.svg");
        clearBtn.addActionListener(e -> {
            queryEditor.setText("");
            resultArea.setText("");
            lastResponseBody = "";
            resultTablePanel.clearData();
            showRawResult();
            respStatusLabel.setText("");
        });

        toolbar.add(templateCombo, "w 180!, h " + QUERY_TOOLBAR_CONTROL_HEIGHT + "!");
        toolbar.add(loadTemplateBtn, "h " + QUERY_TOOLBAR_CONTROL_HEIGHT + "!");
        toolbar.add(new JSeparator(SwingConstants.VERTICAL), "growy, gap 2 2");
        toolbar.add(clearBtn);
        toolbar.add(copyBtn);
        toolbar.add(executeBtn);

        v1QueryBuilderPanel = buildV1QueryBuilderPanel();
        v1QueryBuilderPanel.setVisible(false);

        JPanel topArea = new JPanel(new BorderLayout());
        topArea.setOpaque(false);
        topArea.add(toolbar, BorderLayout.NORTH);
        topArea.add(v1QueryBuilderPanel, BorderLayout.CENTER);

        panel.add(topArea, BorderLayout.NORTH);

        // 查询编辑器 - 参考 RequestBodyPanel，可编辑，用 SearchableTextArea 包装（启用搜索替换）
        JPanel queryPanel = new JPanel(new BorderLayout());
        queryPanel.setOpaque(false);
        // 查询编辑器顶部标题 + 工具按钮（MigLayout）
        JPanel queryHeaderBar = new JPanel(new MigLayout("insets 2 4 2 4, fillx", "[]push", "[]"));
        ToolWindowSurfaceStyle.applySectionHeader(queryHeaderBar);
        queryLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_QUERY_TITLE));
        queryLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -2));
        queryHeaderBar.add(queryLabel);

        queryEditor = createQueryEditor();
        registerCtrlEnterShortcut(executeBtn);
        // 参考 RequestBodyPanel：可编辑编辑器使用 SearchableTextArea(area) 包装（启用搜索替换）
        searchableQueryArea = new SearchableTextArea(queryEditor);
        queryPanel.add(queryHeaderBar, BorderLayout.NORTH);
        queryPanel.add(searchableQueryArea, BorderLayout.CENTER);

        // 结果区
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setOpaque(false);
        // 响应标题栏（MigLayout）
        JPanel respHeader = new JPanel(new MigLayout("insets 2 4 2 4, fillx", "[]push[]", "[]"));
        ToolWindowSurfaceStyle.applySectionHeader(respHeader);
        JLabel respLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_RESPONSE_TITLE));
        respLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -2));
        respStatusLabel = new JLabel("");
        respStatusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        respStatusLabel.setForeground(ModernColors.getTextSecondary());
        respHeader.add(respLabel);
        respHeader.add(respStatusLabel);

        // 参考 ResponseBodyPanel：不可编辑编辑器使用 SearchableTextArea(area, false) 包装（仅搜索）
        resultArea = createResponseEditor();
        searchableResultArea = new SearchableTextArea(resultArea, false);
        resultTablePanel = new EnhancedTablePanel(new String[]{});
        resultTabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        ToolWindowSurfaceStyle.applyTabbedPaneCard(resultTabs);
        resultTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_TAB_TABLE), resultTablePanel);
        resultTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_TAB_RAW), searchableResultArea);
        resultTabs.setSelectedIndex(1);
        resultPanel.add(respHeader, BorderLayout.NORTH);
        resultPanel.add(resultTabs, BorderLayout.CENTER);

        JSplitPane editorSplit = AppToolWindowChrome.createVerticalInnerSplitPane(
                queryPanel,
                resultPanel,
                180
        );
        editorSplit.setResizeWeight(0.2);
        queryPanel.setMinimumSize(new Dimension(0, 80));
        resultPanel.setMinimumSize(new Dimension(0, 140));
        SwingUtilities.invokeLater(() -> editorSplit.setDividerLocation(0.2));
        panel.add(editorSplit, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildV1QueryBuilderPanel() {
        JPanel wrapper = new JPanel(new MigLayout("insets 4 6 4 6, fillx", "[][fill]8[][][fill]push", "[]"));
        ToolWindowSurfaceStyle.applySectionHeader(wrapper);

        // Fields 只读浏览
        fieldCombo = new EasyComboBox<>(EasyComboBox.WidthMode.DYNAMIC);
        wrapper.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_FIELD)));
        wrapper.add(fieldCombo);

        wrapper.add(new JSeparator(SwingConstants.VERTICAL), "growy, gap 2 2");

        // Tag 浏览区
        wrapper.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_TAGS)));
        tagRowsPanel = new JPanel(new MigLayout("insets 0, fillx", "[][fill][]", "[]"));
        tagRowsPanel.setOpaque(false);
        wrapper.add(tagRowsPanel);

        addTagConditionRow();
        return wrapper;
    }

    private void addTagConditionRow() {
        TagConditionRow row = new TagConditionRow();
        row.panel = new JPanel(new MigLayout("insets 0", "[][][]", "[]"));
        row.panel.setOpaque(false);

        row.keyCombo = new EasyComboBox<>(EasyComboBox.WidthMode.DYNAMIC);
        row.keyCombo.addActionListener(e -> {
            if (suppressComboEvents) return;
            if (getSelectedMode() != QueryMode.INFLUXQL_V1) return;
            String db = getSelectedDatabase();
            String measurement = getSelectedMeasurement();
            String key = getSelectedComboText(row.keyCombo);
            if (!db.isBlank() && !measurement.isBlank() && !key.isBlank() && connected) {
                loadTagValuesToCombo(db, measurement, key, row.valueCombo);
            }
        });

        row.valueCombo = new EasyComboBox<>(EasyComboBox.WidthMode.DYNAMIC);

        row.panel.add(row.keyCombo);
        row.panel.add(new JLabel("→"));
        row.panel.add(row.valueCombo);

        if (!cachedTagKeys.isEmpty()) {
            setComboOptions(row.keyCombo, cachedTagKeys, "");
            String db = getSelectedDatabase();
            String measurement = getSelectedMeasurement();
            String selectedKey = getSelectedComboText(row.keyCombo);
            if (connected && !db.isBlank() && !measurement.isBlank() && !selectedKey.isBlank()) {
                loadTagValuesToCombo(db, measurement, selectedKey, row.valueCombo);
            }
        }

        tagRows.add(row);
        tagRowsPanel.add(row.panel);
        tagRowsPanel.revalidate();
        tagRowsPanel.repaint();
    }


    private RSyntaxTextArea createQueryEditor() {
        RSyntaxTextArea textArea = new FallbackAwareRSyntaxTextArea(10, 40);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setAutoIndentEnabled(true);
        textArea.setTabSize(2);
        textArea.setTabsEmulated(true);
        textArea.setMarkOccurrences(true);
        textArea.setPaintTabLines(true);
        textArea.setAnimateBracketMatching(true);
        textArea.setEditable(true);
        EditorThemeUtil.loadTheme(textArea);
        return textArea;
    }

    private RSyntaxTextArea createResponseEditor() {
        RSyntaxTextArea textArea = new FallbackAwareRSyntaxTextArea(10, 40);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setLineWrap(false);
        textArea.setHighlightCurrentLine(false);
        textArea.setEditable(false);
        EditorThemeUtil.loadTheme(textArea);
        return textArea;
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private QueryMode getSelectedMode() {
        QueryMode mode = (QueryMode) modeCombo.getSelectedItem();
        return mode == null ? QueryMode.INFLUXQL_V1 : mode;
    }

    private void switchMode(QueryMode mode) {
        if (suppressModeSwitch) return;
        connected = false;
        clearMeasurementList();
        btnCardLayout.show(btnCard, CONNECT_CARD);
        showModeFields(mode);

        if (mode == QueryMode.INFLUXQL_V1) {
            queryLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_QUERY_TITLE_V1));
            updateExecuteButtonForMode(mode);
            queryEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
            if (v1QueryBuilderPanel != null) v1QueryBuilderPanel.setVisible(true);
            setTemplates(INFLUXQL_TEMPLATES);
        } else {
            queryLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_QUERY_TITLE_V2));
            updateExecuteButtonForMode(mode);
            queryEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
            if (v1QueryBuilderPanel != null) v1QueryBuilderPanel.setVisible(false);
            clearMeasurementList();
            setTemplates(FLUX_TEMPLATES);
        }
        revalidate();
        repaint();
    }

    private void setTemplates(TemplateItem[] templates) {
        currentTemplates = templates;
        templateCombo.removeAllItems();
        for (TemplateItem t : templates) {
            templateCombo.addItem(I18nUtil.getMessage(t.nameKey()));
        }
        if (templates.length > 0) {
            queryEditor.setText(applyTemplateVariables(templates[0].query()));
            queryEditor.setCaretPosition(0);
        } else {
            queryEditor.setText("");
        }
    }

    private void loadTemplate() {
        int idx = templateCombo.getSelectedIndex();
        if (idx >= 0 && idx < currentTemplates.length) {
            queryEditor.setText(applyTemplateVariables(currentTemplates[idx].query()));
            queryEditor.setCaretPosition(0);
        }
    }

    private String applyTemplateVariables(String template) {
        if (getSelectedMode() == QueryMode.INFLUXQL_V1) {
            String measurement = getSelectedMeasurement();
            if (measurement.isBlank()) measurement = "measurement";
            return template.replace("${measurement}", quoteIdentifier(measurement));
        }
        return template;
    }

    private void registerCtrlEnterShortcut(JButton executeButton) {
        SwingUtilities.invokeLater(() -> {
            if (queryEditor != null) {
                queryEditor.getInputMap().put(
                        KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER,
                                java.awt.event.InputEvent.CTRL_DOWN_MASK),
                        "executeQuery");
                queryEditor.getActionMap().put("executeQuery", new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        executeButton.doClick();
                    }
                });
            }
        });
    }

    private void doConnect() {
        String inputHost = getCurrentHost();
        if (inputHost.isEmpty()) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_HOST_REQUIRED));
            return;
        }
        inputHost = InfluxDbConnectionProfileStore.normalizeBaseUrl(inputHost);
        baseUrl = inputHost;

        QueryMode mode = getSelectedMode();
        connectBtn.setEnabled(false);
        String finalBaseUrl = baseUrl;

        SwingWorker<HttpResult, Void> worker = new SwingWorker<>() {
            @Override
            protected HttpResult doInBackground() throws Exception {
                if (mode == QueryMode.INFLUXQL_V1) {
                    return callHttp("GET", "/ping", null, null, null, mode);
                }
                return callHttp("GET", "/health", null, null, "application/json", mode);
            }

            @Override
            protected void done() {
                connectBtn.setEnabled(true);
                try {
                    HttpResult result = get();
                    if (result.code() < 200 || result.code() >= 300) {
                        throw new IOException("HTTP " + result.code() + "\n" + result.body());
                    }
                    connected = true;
                    btnCardLayout.show(btnCard, DISCONNECT_CARD);
                    addHostHistory(finalBaseUrl);
                    NotificationCenter.showSuccess(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_CONNECT_SUCCESS), finalBaseUrl));
                    if (mode == QueryMode.INFLUXQL_V1) {
                        loadDatabases(() -> {
                            String db = getSelectedDatabase();
                            if (!db.isBlank()) loadMeasurements(db);
                        });
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("influx connect interrupted", ex);
                } catch (Exception ex) {
                    connected = false;
                    clearMeasurementList();
                    btnCardLayout.show(btnCard, CONNECT_CARD);
                    NotificationCenter.showError(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_CONNECT_FAILED),
                            ex.getMessage()));
                }
            }
        };
        worker.execute();
    }

    private void doDisconnect() {
        connected = false;
        clearMeasurementList();
        btnCardLayout.show(btnCard, CONNECT_CARD);
        respStatusLabel.setText("");
        NotificationCenter.showInfo(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_DISCONNECT_SUCCESS));
    }

    private String getCurrentHost() {
        return hostField.getText().trim();
    }

    private void addHostHistory(String host) {
        hostField.setText(InfluxDbConnectionProfileStore.normalizeBaseUrl(host));
    }

    private void executeQuery() {
        if (!connected) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_NOT_CONNECTED));
            return;
        }

        String query = queryEditor.getText().trim();
        if (query.isEmpty()) {
            NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_QUERY_REQUIRED));
            return;
        }

        QueryMode mode = getSelectedMode();
        if (mode == QueryMode.FLUX_V2) {
            if (orgField.getText().trim().isBlank()) {
                NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_ORG_REQUIRED));
                return;
            }
        } else {
            if (getSelectedDatabase().isBlank()) {
                NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_DB_REQUIRED));
                return;
            }
        }

        addToHistory(mode, getSelectedDatabase(), orgField.getText().trim(), getSelectedMeasurement(), query);
        executeBtn.setEnabled(false);
        respStatusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_STATUS_REQUESTING));
        respStatusLabel.setForeground(ModernColors.getTextSecondary());

        SwingWorker<HttpResult, Void> worker = new SwingWorker<>() {
            @Override
            protected HttpResult doInBackground() throws Exception {
                if (mode == QueryMode.FLUX_V2) {
                    String path = "/api/v2/query?org=" + enc(orgField.getText().trim());
                    return callHttp("POST", path, query, APPLICATION_VND_FLUX, TEXT_CSV, mode);
                }
                StringBuilder path = new StringBuilder("/query?db=")
                        .append(enc(getSelectedDatabase()))
                        .append("&q=").append(enc(query))
                        .append("&epoch=ms");
                appendV1AuthQueryParams(path);
                return callHttp("GET", path.toString(), null, null, "application/json", mode);
            }

            @Override
            protected void done() {
                executeBtn.setEnabled(true);
                try {
                    HttpResult result = get();
                    lastResponseBody = result.body();
                    renderResponse(lastResponseBody);
                    String influxError = extractInfluxError(lastResponseBody);
                    boolean logicalError = !influxError.isBlank();
                    int code = result.code();
                    boolean success = code >= 200 && code < 300 && !logicalError;
                    String text = success
                            ? I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_STATUS_OK)
                            : I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_STATUS_ERROR);
                    String codePrefix = code > 0 ? code + " · " : "";
                    respStatusLabel.setText(codePrefix + MessageFormat.format(text, String.valueOf(result.costMs())));
                    Color statusColor;
                    if (success) statusColor = ModernColors.getSuccess();
                    else if (code >= 500) statusColor = ModernColors.getError();
                    else statusColor = ModernColors.getWarningDark();
                    respStatusLabel.setForeground(statusColor);
                    if (logicalError) {
                        NotificationCenter.showError(influxError);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("influx query interrupted", ex);
                } catch (Exception ex) {
                    respStatusLabel.setText(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_STATUS_ERROR), "0"));
                    respStatusLabel.setForeground(ModernColors.getError());
                    NotificationCenter.showError(ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadDatabases(Runnable afterLoad) {
        SwingWorker<HttpResult, Void> worker = new SwingWorker<>() {
            @Override
            protected HttpResult doInBackground() throws Exception {
                StringBuilder path = new StringBuilder("/query?q=").append(enc("SHOW DATABASES"));
                appendV1AuthQueryParams(path);
                return callHttp("GET", path.toString(), null, null, "application/json", QueryMode.INFLUXQL_V1);
            }

            @Override
            protected void done() {
                try {
                    HttpResult result = get();
                    if (result.code() >= 200 && result.code() < 300) {
                        List<String> dbs = parseInfluxColumnValues(result.body(), 0);
                        String previous = getSelectedDatabase();
                        setComboOptions(dbCombo, dbs, previous);
                        if (afterLoad != null) afterLoad.run();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("loadDatabases interrupted", ex);
                } catch (Exception ex) {
                    log.warn("loadDatabases error: {}", ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadMeasurements(String db) {
        loadMeasurements(db, false);
    }

    private void loadMeasurements(String db, boolean fromHistory) {
        SwingWorker<HttpResult, Void> worker = new SwingWorker<>() {
            @Override
            protected HttpResult doInBackground() throws Exception {
                String q = "SHOW MEASUREMENTS LIMIT 500";
                StringBuilder path = new StringBuilder("/query?db=")
                        .append(enc(db))
                        .append("&q=")
                        .append(enc(q));
                appendV1AuthQueryParams(path);
                return callHttp("GET", path.toString(), null, null, "application/json", QueryMode.INFLUXQL_V1);
            }

            @Override
            protected void done() {
                try {
                    HttpResult result = get();
                    if (result.code() >= 200 && result.code() < 300) {
                        List<String> measurements = parseInfluxColumnValues(result.body(), 0);
                        if (!fromHistory) {
                            // 正常场景：同步更新 measurementCombo 的下拉选项
                            String previous = getSelectedMeasurement();
                            setComboOptions(measurementCombo, measurements, previous);
                        }
                        // 历史回填场景：跳过 setComboOptions 避免 model 替换导致下拉框闪烁
                        // 左侧列表始终刷新
                        updateMeasurementList(measurements, getSelectedMeasurement());

                        String measurement = getSelectedMeasurement();
                        if (!db.isBlank() && !measurement.isBlank()) {
                            loadFieldKeys(db, measurement);
                            loadTagKeys(db, measurement);
                        }
                        // 历史回填时跳过 loadTemplate，避免覆盖回填的 query
                        if (!fromHistory) loadTemplate();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("loadMeasurements interrupted", ex);
                } catch (Exception ex) {
                    log.warn("loadMeasurements error: {}", ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadFieldKeys(String db, String measurement) {
        SwingWorker<HttpResult, Void> worker = new SwingWorker<>() {
            @Override
            protected HttpResult doInBackground() throws Exception {
                String q = "SHOW FIELD KEYS FROM " + quoteIdentifier(measurement);
                StringBuilder path = new StringBuilder("/query?db=")
                        .append(enc(db))
                        .append("&q=")
                        .append(enc(q));
                appendV1AuthQueryParams(path);
                return callHttp("GET", path.toString(), null, null, "application/json", QueryMode.INFLUXQL_V1);
            }

            @Override
            protected void done() {
                try {
                    HttpResult result = get();
                    if (result.code() >= 200 && result.code() < 300) {
                        List<String> fields = parseInfluxColumnValues(result.body(), 0);
                        String previous = getSelectedComboText(fieldCombo);
                        setComboOptions(fieldCombo, fields, previous);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("loadFieldKeys interrupted", ex);
                } catch (Exception ex) {
                    log.warn("loadFieldKeys error: {}", ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadTagKeys(String db, String measurement) {
        SwingWorker<HttpResult, Void> worker = new SwingWorker<>() {
            @Override
            protected HttpResult doInBackground() throws Exception {
                String q = "SHOW TAG KEYS FROM " + quoteIdentifier(measurement);
                StringBuilder path = new StringBuilder("/query?db=")
                        .append(enc(db))
                        .append("&q=")
                        .append(enc(q));
                appendV1AuthQueryParams(path);
                return callHttp("GET", path.toString(), null, null, "application/json", QueryMode.INFLUXQL_V1);
            }

            @Override
            protected void done() {
                try {
                    HttpResult result = get();
                    if (result.code() >= 200 && result.code() < 300) {
                        List<String> tagKeys = parseInfluxColumnValues(result.body(), 0);
                        updateTagKeyOptions(tagKeys);

                        for (TagConditionRow row : tagRows) {
                            String selectedKey = getSelectedComboText(row.keyCombo);
                            if (!selectedKey.isBlank()) {
                                loadTagValuesToCombo(db, measurement, selectedKey, row.valueCombo);
                            } else {
                                row.valueCombo.removeAllItems();
                            }
                        }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("loadTagKeys interrupted", ex);
                } catch (Exception ex) {
                    log.warn("loadTagKeys error: {}", ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void updateTagKeyOptions(List<String> tagKeys) {
        cachedTagKeys = new ArrayList<>(tagKeys);
        for (TagConditionRow row : tagRows) {
            String previous = getSelectedComboText(row.keyCombo);
            setComboOptions(row.keyCombo, tagKeys, previous);
        }
    }

    private void loadTagValuesToCombo(String db, String measurement, String tagKey, JComboBox<String> targetCombo) {
        SwingWorker<HttpResult, Void> worker = new SwingWorker<>() {
            @Override
            protected HttpResult doInBackground() throws Exception {
                String q = "SHOW TAG VALUES FROM " + quoteIdentifier(measurement)
                        + " WITH KEY = " + quoteIdentifier(tagKey) + " LIMIT 200";
                StringBuilder path = new StringBuilder("/query?db=")
                        .append(enc(db))
                        .append("&q=")
                        .append(enc(q));
                appendV1AuthQueryParams(path);
                return callHttp("GET", path.toString(), null, null, "application/json", QueryMode.INFLUXQL_V1);
            }

            @Override
            protected void done() {
                try {
                    HttpResult result = get();
                    if (result.code() >= 200 && result.code() < 300) {
                        List<String> tagValues = parseInfluxColumnValues(result.body(), 1);
                        String previous = getSelectedComboText(targetCombo);
                        setComboOptions(targetCombo, tagValues, previous);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("loadTagValuesToCombo interrupted", ex);
                } catch (Exception ex) {
                    log.warn("loadTagValuesToCombo error: {}", ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private List<String> parseInfluxColumnValues(String json, int index) {
        List<String> values = new ArrayList<>();
        try {
            JsonNode root = JsonUtil.readTree(json);
            JsonNode results = root.get("results");
            if (results == null || !results.isArray()) return values;
            for (JsonNode result : results) {
                JsonNode series = result.get("series");
                if (series == null || !series.isArray()) continue;
                for (JsonNode seriesNode : series) {
                    JsonNode rows = seriesNode.get("values");
                    if (rows == null || !rows.isArray()) continue;
                    for (JsonNode row : rows) {
                        if (row.isArray() && row.size() > index && !row.get(index).isNull()) {
                            String v = row.get(index).asText();
                            if (!v.isBlank() && !values.contains(v)) values.add(v);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("parseInfluxColumnValues failed: {}", ex.getMessage());
        }
        return values;
    }

    private String extractInfluxError(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            JsonNode root = JsonUtil.readTree(json);
            if (root.has("error") && !root.get("error").isNull()) {
                return root.get("error").asText("");
            }
            JsonNode results = root.get("results");
            if (results != null && results.isArray()) {
                for (JsonNode result : results) {
                    if (result.has("error") && !result.get("error").isNull()) {
                        return result.get("error").asText("");
                    }
                }
            }
        } catch (Exception ignored) {
            // 非 JSON 或解析失败时忽略
        }
        return "";
    }

    private void updateMeasurementList(List<String> measurements, String preferredSelection) {
        if (measurementListModel == null) return;
        measurementListModel.clear();
        for (String measurement : measurements) {
            measurementListModel.addElement(measurement);
        }
        filterMeasurements(measurementSearchField == null ? "" : measurementSearchField.getText());
        if (!preferredSelection.isBlank()) {
            syncMeasurementListSelection(preferredSelection);
        } else if (!measurementFilteredModel.isEmpty()) {
            measurementList.setSelectedIndex(0);
        }
    }

    private void clearMeasurementList() {
        if (measurementListModel != null) measurementListModel.clear();
        if (measurementFilteredModel != null) measurementFilteredModel.clear();
    }

    private void filterMeasurements(String keyword) {
        if (measurementFilteredModel == null || measurementListModel == null) return;
        String lower = keyword == null ? "" : keyword.trim().toLowerCase();
        String selected = measurementList == null ? "" : measurementList.getSelectedValue();
        measurementFilteredModel.clear();
        for (int i = 0; i < measurementListModel.size(); i++) {
            String name = measurementListModel.get(i);
            if (lower.isEmpty() || name.toLowerCase().contains(lower)) {
                measurementFilteredModel.addElement(name);
            }
        }
        if (selected != null && !selected.isBlank()) {
            syncMeasurementListSelection(selected);
        }
    }

    private void syncMeasurementListSelection(String measurement) {
        if (measurementList == null || measurement == null || measurement.isBlank()) return;
        suppressMeasurementSync = true;
        try {
            for (int i = 0; i < measurementFilteredModel.size(); i++) {
                if (measurement.equals(measurementFilteredModel.get(i))) {
                    measurementList.setSelectedIndex(i);
                    measurementList.ensureIndexIsVisible(i);
                    break;
                }
            }
        } finally {
            suppressMeasurementSync = false;
        }
    }

    private void selectMeasurementFromList(String measurement) {
        String current = getSelectedMeasurement();
        if (measurement == null || measurement.isBlank() || measurement.equals(current)) return;

        suppressComboEvents = true;
        measurementCombo.setSelectedItem(measurement);
        if (measurementCombo.getEditor().getEditorComponent() instanceof JTextField editor) {
            editor.setText(measurement);
        }
        suppressComboEvents = false;

        String db = getSelectedDatabase();
        if (!db.isBlank() && connected) {
            loadFieldKeys(db, measurement);
            loadTagKeys(db, measurement);
            if (templateCombo != null && templateCombo.getSelectedIndex() >= 0) {
                loadTemplate();
            }
        }
    }

    private void renderResponse(String body) {
        String text = body == null ? "" : body;
        if (!text.isBlank() && JsonUtil.isTypeJSON(text)) {
            text = JsonUtil.toJsonPrettyStr(text);
        }
        resultArea.setText(text);
        resultArea.setCaretPosition(0);

        if (body == null || body.isBlank()) {
            resultTablePanel.clearData();
            showRawResult();
            return;
        }

        if (tryRenderInfluxJsonTable(body)) return;
        if (tryRenderCsvTable(body)) return;
        showRawResult();
    }

    private boolean tryRenderInfluxJsonTable(String body) {
        try {
            JsonNode root = JsonUtil.readTree(body);
            JsonNode results = root.get("results");
            if (results == null || !results.isArray()) return false;

            List<String> tagKeys = new ArrayList<>();
            List<String> baseCols = null;
            List<Object[]> rows = new ArrayList<>();

            for (JsonNode result : results) {
                JsonNode seriesArr = result.get("series");
                if (seriesArr == null || !seriesArr.isArray()) continue;
                for (JsonNode series : seriesArr) {
                    JsonNode colsNode = series.get("columns");
                    if (colsNode == null || !colsNode.isArray()) continue;

                    if (baseCols == null) {
                        baseCols = new ArrayList<>();
                        for (JsonNode col : colsNode) baseCols.add(col.asText());
                    }

                    JsonNode tags = series.get("tags");
                    if (tags != null && tags.isObject()) {
                        for (java.util.Map.Entry<String, JsonNode> e : tags.properties()) {
                            String k = e.getKey();
                            if (!tagKeys.contains(k)) tagKeys.add(k);
                        }
                    }

                    JsonNode values = series.get("values");
                    if (values == null || !values.isArray()) continue;
                    String seriesName = series.has("name") ? series.get("name").asText("") : "";
                    for (JsonNode row : values) {
                        if (!row.isArray()) continue;
                        List<Object> merged = new ArrayList<>();
                        merged.add(seriesName);
                        for (int i = 0; i < (baseCols == null ? 0 : baseCols.size()); i++) {
                            merged.add(i < row.size() ? nodeToValue(row.get(i)) : "");
                        }
                        for (String tagKey : tagKeys) {
                            String tv = "";
                            if (tags != null && tags.has(tagKey) && !tags.get(tagKey).isNull()) {
                                tv = tags.get(tagKey).asText();
                            }
                            merged.add(tv);
                        }
                        rows.add(merged.toArray(new Object[0]));
                    }
                }
            }

            if (baseCols == null) return false;
            List<String> allCols = new ArrayList<>();
            allCols.add("_series");
            allCols.addAll(baseCols);
            for (String tk : tagKeys) allCols.add("tag:" + tk);

            resultTablePanel.resetAndSetData(allCols.toArray(new String[0]), rows);
            showTableResult();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean tryRenderCsvTable(String body) {
        try {
            CsvReader reader = CsvUtil.getReader();
            CsvData csvData = reader.readFromStr(body);
            List<CsvRow> rows = csvData.getRows();
            if (rows == null || rows.isEmpty()) return false;

            int headerIndex = 0;
            while (headerIndex < rows.size() && isFluxAnnotationRow(rows.get(headerIndex))) {
                headerIndex++;
            }
            if (headerIndex >= rows.size()) return false;

            List<String> headers = rows.get(headerIndex);
            if (headers == null || headers.isEmpty()) return false;

            String[] columns = headers.toArray(new String[0]);
            List<Object[]> dataRows = new ArrayList<>();
            for (int i = headerIndex + 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (isFluxAnnotationRow(row)) continue;
                Object[] arr = new Object[columns.length];
                for (int c = 0; c < columns.length; c++) {
                    arr[c] = c < row.size() ? row.get(c) : "";
                }
                dataRows.add(arr);
            }
            resultTablePanel.resetAndSetData(columns, dataRows);
            showTableResult();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isFluxAnnotationRow(List<String> row) {
        if (row == null || row.isEmpty()) return false;
        String firstCell = row.get(0);
        return firstCell != null && firstCell.startsWith("#");
    }

    private Object nodeToValue(JsonNode node) {
        if (node == null || node.isNull()) return "";
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) return node.numberValue();
        if (node.isBoolean()) return node.asBoolean();
        return node.toString();
    }

    private void showTableResult() {
        if (resultTabs != null && resultTabs.getTabCount() >= 2) {
            resultTabs.setSelectedIndex(0);
        }
    }

    private void showRawResult() {
        if (resultTabs != null && resultTabs.getTabCount() >= 2) {
            resultTabs.setSelectedIndex(1);
        }
    }

    private String getSelectedDatabase() {
        return getSelectedComboText(dbCombo);
    }

    private String getSelectedMeasurement() {
        return getSelectedComboText(measurementCombo);
    }

    private String getSelectedComboText(JComboBox<String> combo) {
        if (combo == null) return "";
        Object selected = combo.getEditor().getItem();
        if (selected == null || selected.toString().isBlank()) {
            selected = combo.getSelectedItem();
        }
        return selected == null ? "" : selected.toString().trim();
    }

    private void setComboEditorText(JComboBox<String> combo, String text) {
        if (combo == null) return;
        String value = text == null ? "" : text.trim();
        suppressComboEvents = true;
        combo.setSelectedItem(value);
        if (combo.getEditor().getEditorComponent() instanceof JTextField editor) {
            editor.setText(value);
            editor.setCaretPosition(editor.getText().length());
        } else {
            combo.getEditor().setItem(value);
        }
        suppressComboEvents = false;
    }

    private void setComboOptions(JComboBox<String> combo, List<String> options, String preferredSelection) {
        if (combo == null) return;
        List<String> uniq = new ArrayList<>(new LinkedHashSet<>(options));
        String select = preferredSelection;
        if (select == null) select = "";
        applyComboModel(combo, uniq, select);
        if (combo.getSelectedItem() == null && combo.getItemCount() > 0) {
            suppressComboEvents = true;
            combo.setSelectedIndex(0);
            suppressComboEvents = false;
        }
    }

    private void applyComboModel(JComboBox<String> combo, List<String> values, String editorText) {
        suppressComboEvents = true;
        combo.setModel(new DefaultComboBoxModel<>(values.toArray(new String[0])));
        combo.setSelectedItem(null);
        if (combo.getEditor().getEditorComponent() instanceof JTextField editor) {
            editor.setText(editorText == null ? "" : editorText);
            editor.setCaretPosition(editor.getText().length());
        } else {
            combo.getEditor().setItem(editorText == null ? "" : editorText);
        }
        suppressComboEvents = false;
    }


    private HttpResult callHttp(String method, String path, String body, String contentType,
                                String accept, QueryMode mode) throws IOException {
        String normalizedPath = path.startsWith("/") ? path : ("/" + path);
        String url = baseUrl + normalizedPath;

        Request.Builder builder = new Request.Builder().url(url);

        if (mode == QueryMode.FLUX_V2) {
            String token = tokenField.getText().trim();
            if (!token.isBlank()) {
                builder.header(AUTHORIZATION, TOKEN_PREFIX + token);
            }
        }

        if (accept != null && !accept.isBlank()) {
            builder.header("Accept", accept);
        }

        RequestBody reqBody = null;
        if (body != null && !body.isBlank()) {
            MediaType mediaType = contentType == null ? null : MediaType.parse(contentType);
            reqBody = RequestBody.create(body, mediaType);
        }

        switch (method) {
            case "POST" -> builder.post(reqBody == null ? RequestBody.create(new byte[0]) : reqBody);
            case "PUT" -> builder.put(reqBody == null ? RequestBody.create(new byte[0]) : reqBody);
            case "DELETE" -> {
                if (reqBody == null) builder.delete();
                else builder.delete(reqBody);
            }
            default -> builder.get();
        }

        long start = System.currentTimeMillis();
        try (Response response = getHttpClient().newCall(builder.build()).execute()) {
            long cost = System.currentTimeMillis() - start;
            String respBody = response.body() == null ? "" : response.body().string();
            return new HttpResult(response.code(), respBody, cost);
        }
    }

    private OkHttpClient getHttpClient() {
        return OkHttpClientManager.getClientForUrl(
                baseUrl,
                true,
                CONNECT_TIMEOUT_MS,
                READ_TIMEOUT_MS,
                WRITE_TIMEOUT_MS
        );
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void appendV1AuthQueryParams(StringBuilder path) {
        if (path == null) return;
        String user = userField == null ? "" : userField.getText().trim();
        String pass = passwordField == null ? "" : new String(passwordField.getPassword()).trim();
        if (!user.isBlank()) path.append("&u=").append(enc(user));
        if (!pass.isBlank()) path.append("&p=").append(enc(pass));
    }

    private String quoteIdentifier(String name) {
        String escaped = name.replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }


    private void copyResult() {
        String text = resultArea.getText();
        if (text == null || text.isBlank()) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(text), null);
        NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_RESULT_COPIED));
    }
}
