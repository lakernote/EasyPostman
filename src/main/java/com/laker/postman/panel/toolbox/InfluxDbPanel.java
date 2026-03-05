package com.laker.postman.panel.toolbox;

import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.button.*;
import com.laker.postman.common.component.table.EnhancedTablePanel;
import com.laker.postman.util.*;
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
import java.util.concurrent.TimeUnit;

@Slf4j
public class InfluxDbPanel extends JPanel {

    private enum QueryMode {
        INFLUXQL_V1,
        FLUX_V2
    }

    private record TemplateItem(String name, String query) {
    }

    private record HttpResult(int code, String body, long costMs) {
    }

    private static class TagConditionRow {
        JPanel panel;
        EasyComboBox<String> keyCombo;
        EasyComboBox<String> valueCombo;
    }

    private JComboBox<String> hostCombo;
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

    private PrimaryButton connectBtn;
    private SecondaryButton disconnectBtn;
    private JLabel connectionStatusLabel;
    private CardLayout btnCardLayout;
    private JPanel btnCard;

    private RSyntaxTextArea queryEditor;
    private RSyntaxTextArea resultArea;
    private SearchableTextArea searchableResultArea;
    private SearchableTextArea searchableQueryArea;
    private EnhancedTablePanel resultTablePanel;
    private JTabbedPane resultTabs;
    private PrimaryButton executeBtn;
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

    private transient OkHttpClient httpClient;
    private String baseUrl = "http://localhost:8086";
    private boolean connected = false;
    private String lastResponseBody = "";
    private boolean suppressMeasurementSync = false;

    private static final int MAX_HOST_HISTORY = 5;
    private static final int MAX_HISTORY = 20;
    private final Deque<HistoryEntry> requestHistory = new ArrayDeque<>();
    private DefaultListModel<HistoryEntry> historyListModel;
    private JList<HistoryEntry> historyList;

    private static final String LABEL_DISABLED_FG = "Label.disabledForeground";
    private static final String SEPARATOR_FG = "Separator.foreground";
    private static final String AUTHORIZATION = "Authorization";
    private static final String TOKEN_PREFIX = "Token ";
    private static final String APPLICATION_VND_FLUX = "application/vnd.flux";
    private static final String TEXT_CSV = "text/csv";

    private static class HistoryEntry {
        final QueryMode mode;
        final String db;
        final String org;
        final String measurement;
        final String query;

        HistoryEntry(QueryMode mode, String db, String org, String measurement, String query) {
            this.mode = mode;
            this.db = db;
            this.org = org;
            this.measurement = measurement;
            this.query = query;
        }
    }

    private static final TemplateItem[] FLUX_TEMPLATES = {
            new TemplateItem("Latest 100 Points",
                    """
                            from(bucket: "example")
                              |> range(start: -1h)
                              |> sort(columns: ["_time"], desc: true)
                              |> limit(n: 100)
                            """),
            new TemplateItem("Count By Measurement",
                    """
                            from(bucket: "example")
                              |> range(start: -24h)
                              |> group(columns: ["_measurement"])
                              |> count()
                            """),
            new TemplateItem("Mean By 1m",
                    """
                            from(bucket: "example")
                              |> range(start: -6h)
                              |> aggregateWindow(every: 1m, fn: mean, createEmpty: false)
                              |> yield(name: "mean")
                            """),
            new TemplateItem("Top 10 Values",
                    """
                            from(bucket: "example")
                              |> range(start: -24h)
                              |> sort(columns: ["_value"], desc: true)
                              |> limit(n: 10)
                            """),
            new TemplateItem("Group By Tag",
                    """
                            from(bucket: "example")
                              |> range(start: -24h)
                              |> filter(fn: (r) => r._measurement == "cpu")
                              |> group(columns: ["host"])
                              |> last()
                            """),
            new TemplateItem("Field Selector",
                    """
                            from(bucket: "example")
                              |> range(start: -1h)
                              |> filter(fn: (r) => r._measurement == "cpu")
                              |> filter(fn: (r) => r._field == "usage_user")
                              |> limit(n: 200)
                            """),
            new TemplateItem("Pivot View",
                    """
                            from(bucket: "example")
                              |> range(start: -30m)
                              |> filter(fn: (r) => r._measurement == "cpu")
                              |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
                            """)
    };

    private static final TemplateItem[] INFLUXQL_TEMPLATES = {
            new TemplateItem("Latest 100 Rows",
                    """
                            SELECT *
                            FROM ${measurement}
                            ORDER BY time DESC
                            LIMIT 100
                            """),
            new TemplateItem("Count Last 1h",
                    """
                            SELECT COUNT(*)
                            FROM ${measurement}
                            WHERE time > now() - 1h
                            """),
            new TemplateItem("Mean By 1m",
                    """
                            SELECT MEAN("value")
                            FROM ${measurement}
                            WHERE time > now() - 6h
                            GROUP BY time(1m)
                            FILL(null)
                            """),
            new TemplateItem("Tag Values (host)",
                    """
                            SHOW TAG VALUES
                            FROM ${measurement}
                            WITH KEY = "host"
                            LIMIT 200
                            """),
            new TemplateItem("Field Keys",
                    """
                            SHOW FIELD KEYS
                            FROM ${measurement}
                            """),
            new TemplateItem("Series Cardinality",
                    """
                            SHOW SERIES CARDINALITY
                            FROM ${measurement}
                            """),
            new TemplateItem("Last Point Per Host",
                    """
                            SELECT LAST("value")
                            FROM ${measurement}
                            WHERE time > now() - 24h
                            GROUP BY "host"
                            """)
    };

    public InfluxDbPanel() {
        initHttpClient();
        initUI();
    }

    private void initHttpClient() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(buildConnectionPanel(), BorderLayout.NORTH);
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeftPanel(), buildMainPanel());
        mainSplit.setDividerLocation(240);
        mainSplit.setDividerSize(3);
        mainSplit.setResizeWeight(0.25);
        mainSplit.setContinuousLayout(true);
        add(mainSplit, BorderLayout.CENTER);
        switchMode(getSelectedMode());
    }

    private JPanel buildLeftPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setPreferredSize(new Dimension(230, 0));

        JTabbedPane leftTabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        leftTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_MANAGEMENT), buildMeasurementPanel());
        leftTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_HISTORY), buildHistoryPanel());

        wrapper.add(leftTabs, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildMeasurementPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setPreferredSize(new Dimension(230, 0));
        panel.setBorder(BorderFactory.createEmptyBorder());

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(4, 8, 4, 4)));
        JLabel titleLbl = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_MANAGEMENT));
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 12f));
        RefreshButton refreshBtn = new RefreshButton();
        refreshBtn.addActionListener(e -> {
            if (!connected || getSelectedMode() != QueryMode.INFLUXQL_V1) return;
            String db = getSelectedDatabase();
            if (!db.isBlank()) {
                loadMeasurements(db);
            }
        });
        titleBar.add(titleLbl, BorderLayout.CENTER);
        titleBar.add(refreshBtn, BorderLayout.EAST);

        measurementSearchField = new SearchTextField();
        measurementSearchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT_SEARCH_PLACEHOLDER));
        measurementSearchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JPanel searchBox = new JPanel(new BorderLayout());
        searchBox.setBorder(BorderFactory.createEmptyBorder(4, 6, 2, 6));
        searchBox.add(measurementSearchField, BorderLayout.CENTER);

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
        listScroll.setBorder(BorderFactory.createEmptyBorder());

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
        topArea.add(searchBox, BorderLayout.CENTER);

        panel.add(topArea, BorderLayout.NORTH);
        panel.add(listScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(4, 8, 4, 4)));
        JLabel titleLbl = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_HISTORY));
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 12f));
        ClearButton clearHistBtn = new ClearButton();
        clearHistBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_HISTORY_CLEAR));
        clearHistBtn.addActionListener(e -> {
            requestHistory.clear();
            historyListModel.clear();
        });
        titleBar.add(titleLbl, BorderLayout.CENTER);
        titleBar.add(clearHistBtn, BorderLayout.EAST);

        historyListModel = new DefaultListModel<>();
        historyList = new JList<>(historyListModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer(new HistoryCellRenderer());
        historyList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int idx = historyList.locationToIndex(e.getPoint());
                    if (idx >= 0) applyHistory(historyListModel.get(idx));
                }
            }
        });

        JScrollPane scroll = new JScrollPane(historyList);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JLabel tipLbl = new JLabel("<html><center><small>" +
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_HISTORY_EMPTY) + "</small></center></html>");
        tipLbl.setHorizontalAlignment(SwingConstants.CENTER);
        tipLbl.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
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
                String modeColor = h.mode == QueryMode.INFLUXQL_V1 ? "#2a7ec8" : "#28a745";
                String modeText = h.mode == QueryMode.INFLUXQL_V1 ? "InfluxQL" : "Flux";
                String query = h.query == null ? "" : h.query.strip();
                String firstLine = query.split("\\R", 2)[0];
                if (firstLine.length() > 38) firstLine = firstLine.substring(0, 37) + "…";
                lbl.setText("<html><b><font color='" + modeColor + "'>" + modeText + "</font></b> " + firstLine + "</html>");
                lbl.setToolTipText(query);
            }
            return lbl;
        }
    }

    private void applyHistory(HistoryEntry entry) {
        if (entry == null) return;
        modeCombo.setSelectedItem(entry.mode);
        if (entry.mode == QueryMode.INFLUXQL_V1) {
            setComboEditorText(dbCombo, entry.db);
            setComboEditorText(measurementCombo, entry.measurement);
            if (!entry.db.isBlank() && connected) {
                loadMeasurements(entry.db);
            }
        } else {
            orgField.setText(entry.org);
        }
        queryEditor.setText(entry.query);
        queryEditor.setCaretPosition(0);
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
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_NOT_CONNECTED));
            return;
        }
        if (getSelectedMode() != QueryMode.INFLUXQL_V1) return;
        String db = getSelectedDatabase();
        if (db.isBlank()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_DB_REQUIRED));
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
                        NotificationUtil.showSuccess(MessageFormat.format(
                                I18nUtil.getMessage(singleSuccessMsgKey), targets.get(0)));
                    } else {
                        NotificationUtil.showSuccess(MessageFormat.format(
                                I18nUtil.getMessage(batchSuccessMsgKey), targets.size()));
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("measurement mutation interrupted", ex);
                } catch (Exception ex) {
                    NotificationUtil.showError(MessageFormat.format(
                            I18nUtil.getMessage(failedMsgKey), ex.getMessage()));
                }
            }
        };
        worker.execute();
    }

    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));

        // 使用 MigLayout 实现连接配置基础行（insets 留出空间避免 FlatLaf focus 高亮被裁剪）
        JPanel baseRow = new JPanel(new MigLayout(
                "insets 4 0 4 0, fillx",
                "[][grow,fill]8[][grow,fill]8[]8[]",
                "[]"
        ));
        hostCombo = new JComboBox<>();
        hostCombo.setEditable(true);
        hostCombo.setPreferredSize(new Dimension(240, 32));
        hostCombo.addItem(baseUrl);
        JTextField hostEditor = (JTextField) hostCombo.getEditor().getEditorComponent();
        hostEditor.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_HOST_PLACEHOLDER));
        hostEditor.addActionListener(e -> doConnect());

        modeCombo = new JComboBox<>(QueryMode.values());
        modeCombo.setPreferredSize(new Dimension(180, 32));
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

        connectBtn = new PrimaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_CONNECT), "icons/connect.svg");
        connectBtn.addActionListener(e -> doConnect());
        disconnectBtn = new SecondaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_DISCONNECT), "icons/ws-close.svg");
        disconnectBtn.addActionListener(e -> doDisconnect());

        // 用 CardLayout 叠放，切换时不留空白
        btnCardLayout = new CardLayout();
        btnCard = new JPanel(btnCardLayout);
        btnCard.setOpaque(false);
        btnCard.add(connectBtn, "connect");
        btnCard.add(disconnectBtn, "disconnect");
        btnCardLayout.show(btnCard, "connect");

        connectionStatusLabel = new JLabel("●");
        connectionStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        connectionStatusLabel.setFont(connectionStatusLabel.getFont().deriveFont(Font.BOLD, 14f));
        connectionStatusLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_STATUS_NOT_CONNECTED));

        baseRow.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_HOST)));
        baseRow.add(hostCombo);
        baseRow.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MODE)));
        baseRow.add(modeCombo);
        baseRow.add(btnCard);
        baseRow.add(connectionStatusLabel);

        JPanel modeFields = new JPanel(new CardLayout());
        modeFields.add(buildV2FieldsPanel(), QueryMode.FLUX_V2.name());
        modeFields.add(buildV1FieldsPanel(), QueryMode.INFLUXQL_V1.name());

        panel.add(baseRow, BorderLayout.NORTH);
        panel.add(modeFields, BorderLayout.CENTER);
        panel.putClientProperty("modeFields", modeFields);
        return panel;
    }

    private JPanel buildV2FieldsPanel() {
        JPanel row = new JPanel(new MigLayout("insets 4 0 4 0, fillx", "[][grow,fill]8[][grow,fill]", "[]"));

        tokenField = new JTextField("", 26);
        tokenField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_TOKEN_PLACEHOLDER));
        tokenField.addActionListener(e -> doConnect());

        orgField = new JTextField("", 16);
        orgField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ORG_PLACEHOLDER));
        orgField.addActionListener(e -> doConnect());

        row.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_TOKEN)));
        row.add(tokenField);
        row.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ORG)));
        row.add(orgField);
        return row;
    }

    private JPanel buildV1FieldsPanel() {
        JPanel row = new JPanel(new MigLayout(
                "insets 4 0 4 0, fillx",
                "[]8[grow,fill]8[]8[grow,fill]8[]8[grow,fill]8[]8[grow,fill]8[]",
                "[]"
        ));

        dbCombo = new EasyComboBox<>(EasyComboBox.WidthMode.DYNAMIC);
        dbCombo.setPreferredSize(new Dimension(180, 32));
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
        measurementCombo.setPreferredSize(new Dimension(180, 32));
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

        SecondaryButton reloadMetaBtn = new SecondaryButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_RELOAD_META));
        reloadMetaBtn.addActionListener(e -> {
            String db = getSelectedDatabase();
            loadDatabases(() -> {
                String selectedDb = db.isBlank() ? getSelectedDatabase() : db;
                if (!selectedDb.isBlank()) loadMeasurements(selectedDb);
            });
        });

        userField = new JTextField("", 12);
        userField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_USER_PLACEHOLDER));

        passwordField = new JPasswordField("", 12);
        passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PASS_PLACEHOLDER));
        passwordField.addActionListener(e -> doConnect());

        row.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_DB)));
        row.add(dbCombo);
        row.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MEASUREMENT)));
        row.add(measurementCombo);
        row.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_USER)));
        row.add(userField);
        row.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_PASS)));
        row.add(passwordField);
        row.add(reloadMetaBtn);
        return row;
    }

    private JPanel buildMainPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        // ---- 工具栏（MigLayout）----
        JPanel toolbar = new JPanel(new MigLayout("insets 4, fillx", "[][][]8[][][]push", "[]"));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)));

        templateCombo = new JComboBox<>();
        templateCombo.setPreferredSize(new Dimension(180, 32));

        SecondaryButton loadTemplateBtn = new SecondaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_LOAD_TEMPLATE), "icons/load.svg");
        loadTemplateBtn.setPreferredSize(new Dimension(loadTemplateBtn.getPreferredSize().width, 32));
        loadTemplateBtn.addActionListener(e -> loadTemplate());

        executeBtn = new PrimaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_EXECUTE), "icons/send.svg");
        executeBtn.addActionListener(e -> executeQuery());

        CopyButton copyBtn = new CopyButton();
        copyBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_COPY_RESULT));
        copyBtn.addActionListener(e -> copyResult());

        ClearButton clearBtn = new ClearButton();
        clearBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_CLEAR));
        clearBtn.addActionListener(e -> {
            queryEditor.setText("");
            resultArea.setText("");
            lastResponseBody = "";
            resultTablePanel.clearData();
            showRawResult();
            respStatusLabel.setText("");
        });

        toolbar.add(templateCombo);
        toolbar.add(loadTemplateBtn);
        toolbar.add(new JSeparator(SwingConstants.VERTICAL), "growy, gap 2 2");
        toolbar.add(clearBtn);
        toolbar.add(copyBtn);
        toolbar.add(executeBtn);

        v1QueryBuilderPanel = buildV1QueryBuilderPanel();
        v1QueryBuilderPanel.setVisible(false);

        JPanel topArea = new JPanel(new BorderLayout());
        topArea.add(toolbar, BorderLayout.NORTH);
        topArea.add(v1QueryBuilderPanel, BorderLayout.CENTER);

        panel.add(topArea, BorderLayout.NORTH);

        JSplitPane editorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        editorSplit.setDividerSize(3);
        editorSplit.setContinuousLayout(true);
        editorSplit.setResizeWeight(0.2);
        editorSplit.setBorder(BorderFactory.createEmptyBorder());

        // 查询编辑器 - 参考 RequestBodyPanel，可编辑，用 SearchableTextArea 包装（启用搜索替换）
        JPanel queryPanel = new JPanel(new BorderLayout());
        // 查询编辑器顶部标题 + 工具按钮（MigLayout）
        JPanel queryHeaderBar = new JPanel(new MigLayout("insets 2 4 2 4, fillx", "[]push", "[]"));
        queryHeaderBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)));
        queryLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_QUERY_TITLE));
        queryLabel.setFont(queryLabel.getFont().deriveFont(Font.BOLD, 11f));
        queryHeaderBar.add(queryLabel);

        queryEditor = createQueryEditor();
        registerCtrlEnterShortcut(executeBtn);
        // 参考 RequestBodyPanel：可编辑编辑器使用 SearchableTextArea(area) 包装（启用搜索替换）
        searchableQueryArea = new SearchableTextArea(queryEditor);
        queryPanel.add(queryHeaderBar, BorderLayout.NORTH);
        queryPanel.add(searchableQueryArea, BorderLayout.CENTER);

        // 结果区
        JPanel resultPanel = new JPanel(new BorderLayout());
        // 响应标题栏（MigLayout）
        JPanel respHeader = new JPanel(new MigLayout("insets 2 4 2 4, fillx", "[]push[]", "[]"));
        respHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)));
        JLabel respLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_RESPONSE_TITLE));
        respLabel.setFont(respLabel.getFont().deriveFont(Font.BOLD, 11f));
        respStatusLabel = new JLabel("");
        respStatusLabel.setFont(respStatusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        respStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        respHeader.add(respLabel);
        respHeader.add(respStatusLabel);

        // 参考 ResponseBodyPanel：不可编辑编辑器使用 SearchableTextArea(area, false) 包装（仅搜索）
        resultArea = createResponseEditor();
        searchableResultArea = new SearchableTextArea(resultArea, false);
        resultTablePanel = new EnhancedTablePanel(new String[]{});
        resultTabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        resultTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_TAB_TABLE), resultTablePanel);
        resultTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_TAB_RAW), searchableResultArea);
        resultTabs.setSelectedIndex(1);
        resultPanel.add(respHeader, BorderLayout.NORTH);
        resultPanel.add(resultTabs, BorderLayout.CENTER);

        editorSplit.setTopComponent(queryPanel);
        editorSplit.setBottomComponent(resultPanel);
        queryPanel.setMinimumSize(new Dimension(0, 80));
        resultPanel.setMinimumSize(new Dimension(0, 140));
        SwingUtilities.invokeLater(() -> editorSplit.setDividerLocation(0.2));
        panel.add(editorSplit, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildV1QueryBuilderPanel() {
        JPanel wrapper = new JPanel(new MigLayout("insets 4 6 4 6, fillx", "[][fill]8[][][fill]push", "[]"));
        wrapper.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)));

        // Fields 只读浏览
        fieldCombo = new EasyComboBox<>(EasyComboBox.WidthMode.DYNAMIC);
        wrapper.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_FIELD)));
        wrapper.add(fieldCombo);

        wrapper.add(new JSeparator(SwingConstants.VERTICAL), "growy, gap 2 2");

        // Tag 浏览区
        wrapper.add(new JLabel("Tags:"));
        tagRowsPanel = new JPanel(new MigLayout("insets 0, fillx", "[][fill][]", "[]"));
        wrapper.add(tagRowsPanel);

        addTagConditionRow();
        return wrapper;
    }

    private void addTagConditionRow() {
        TagConditionRow row = new TagConditionRow();
        row.panel = new JPanel(new MigLayout("insets 0", "[][][]", "[]"));

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
        RSyntaxTextArea textArea = new RSyntaxTextArea(10, 40);
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
        updateEditorFont(textArea);
        return textArea;
    }

    private RSyntaxTextArea createResponseEditor() {
        RSyntaxTextArea textArea = new RSyntaxTextArea(10, 40);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setLineWrap(false);
        textArea.setHighlightCurrentLine(false);
        textArea.setEditable(false);
        EditorThemeUtil.loadTheme(textArea);
        updateEditorFont(textArea);
        return textArea;
    }

    private void updateEditorFont(RSyntaxTextArea editor) {
        if (editor != null) {
            editor.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        }
    }

    private QueryMode getSelectedMode() {
        QueryMode mode = (QueryMode) modeCombo.getSelectedItem();
        return mode == null ? QueryMode.FLUX_V2 : mode;
    }

    private void switchMode(QueryMode mode) {
        connected = false;
        clearMeasurementList();
        connectionStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        connectionStatusLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_STATUS_NOT_CONNECTED));
        btnCardLayout.show(btnCard, "connect");

        JPanel connectionPanel = (JPanel) getComponent(0);
        JPanel modeFields = (JPanel) connectionPanel.getClientProperty("modeFields");
        if (modeFields != null) {
            CardLayout card = (CardLayout) modeFields.getLayout();
            card.show(modeFields, mode.name());
        }

        if (mode == QueryMode.INFLUXQL_V1) {
            queryLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_QUERY_TITLE_V1));
            executeBtn.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_EXECUTE_V1));
            queryEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
            if (v1QueryBuilderPanel != null) v1QueryBuilderPanel.setVisible(true);
            setTemplates(INFLUXQL_TEMPLATES);
        } else {
            queryLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_QUERY_TITLE_V2));
            executeBtn.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_EXECUTE_V2));
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
            templateCombo.addItem(t.name());
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
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_HOST_REQUIRED));
            return;
        }
        if (inputHost.endsWith("/")) inputHost = inputHost.substring(0, inputHost.length() - 1);
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
                    connectionStatusLabel.setForeground(new Color(0, 180, 0));
                    connectionStatusLabel.setToolTipText(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_STATUS_CONNECTED), finalBaseUrl));
                    btnCardLayout.show(btnCard, "disconnect");
                    addHostHistory(finalBaseUrl);
                    NotificationUtil.showSuccess(MessageFormat.format(
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
                    connectionStatusLabel.setForeground(Color.RED);
                    connectionStatusLabel.setToolTipText(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_STATUS_NOT_CONNECTED));
                    btnCardLayout.show(btnCard, "connect");
                    NotificationUtil.showError(MessageFormat.format(
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
        connectionStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        connectionStatusLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_STATUS_NOT_CONNECTED));
        btnCardLayout.show(btnCard, "connect");
        respStatusLabel.setText("");
        NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_DISCONNECT_SUCCESS));
    }

    private String getCurrentHost() {
        Object selected = hostCombo.getEditor().getItem();
        return selected == null ? "" : selected.toString().trim();
    }

    private void addHostHistory(String host) {
        for (int i = 0; i < hostCombo.getItemCount(); i++) {
            if (host.equals(hostCombo.getItemAt(i))) {
                hostCombo.removeItemAt(i);
                break;
            }
        }
        hostCombo.insertItemAt(host, 0);
        while (hostCombo.getItemCount() > MAX_HOST_HISTORY) {
            hostCombo.removeItemAt(hostCombo.getItemCount() - 1);
        }
        hostCombo.setSelectedItem(host);
    }

    private void executeQuery() {
        if (!connected) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_NOT_CONNECTED));
            return;
        }

        String query = queryEditor.getText().trim();
        if (query.isEmpty()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_QUERY_REQUIRED));
            return;
        }

        QueryMode mode = getSelectedMode();
        if (mode == QueryMode.FLUX_V2) {
            if (orgField.getText().trim().isBlank()) {
                NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_ORG_REQUIRED));
                return;
            }
        } else {
            if (getSelectedDatabase().isBlank()) {
                NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_DB_REQUIRED));
                return;
            }
        }

        addToHistory(mode, getSelectedDatabase(), orgField.getText().trim(), getSelectedMeasurement(), query);
        executeBtn.setEnabled(false);
        respStatusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_STATUS_REQUESTING));
        respStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));

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
                    if (success) statusColor = new Color(0, 160, 0);
                    else if (code >= 500) statusColor = new Color(200, 50, 50);
                    else statusColor = new Color(210, 130, 0);
                    respStatusLabel.setForeground(statusColor);
                    if (logicalError) {
                        NotificationUtil.showError(influxError);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("influx query interrupted", ex);
                } catch (Exception ex) {
                    respStatusLabel.setText(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_STATUS_ERROR), "0"));
                    respStatusLabel.setForeground(Color.RED);
                    NotificationUtil.showError(ex.getMessage());
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
                        String previous = getSelectedMeasurement();
                        setComboOptions(measurementCombo, measurements, previous);
                        updateMeasurementList(measurements, getSelectedMeasurement());

                        String measurement = getSelectedMeasurement();
                        if (!db.isBlank() && !measurement.isBlank()) {
                            loadFieldKeys(db, measurement);
                            loadTagKeys(db, measurement);
                        }
                        // 连接后 measurement 确定，自动刷新模版变量
                        loadTemplate();
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
        try (Response response = httpClient.newCall(builder.build()).execute()) {
            long cost = System.currentTimeMillis() - start;
            String respBody = response.body() == null ? "" : response.body().string();
            return new HttpResult(response.code(), respBody, cost);
        }
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
        NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_RESULT_COPIED));
    }
}
