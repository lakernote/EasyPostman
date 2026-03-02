package com.laker.postman.panel.toolbox;

import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.button.ClearButton;
import com.laker.postman.common.component.button.CopyButton;
import com.laker.postman.common.component.button.PrimaryButton;
import com.laker.postman.common.component.table.EnhancedTablePanel;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import tools.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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

    private JTextField hostField;
    private JComboBox<QueryMode> modeCombo;

    private JTextField tokenField;
    private JTextField orgField;

    private EasyComboBox<String> dbCombo;
    private EasyComboBox<String> measurementCombo;
    private JTextField userField;
    private JPasswordField passwordField;

    private PrimaryButton connectBtn;
    private JLabel connectionStatusLabel;

    private RSyntaxTextArea queryEditor;
    private RSyntaxTextArea resultArea;
    private SearchableTextArea searchableResultArea;
    private EnhancedTablePanel resultTablePanel;
    private JPanel resultViewPanel;
    private CardLayout resultViewLayout;
    private PrimaryButton executeBtn;
    private JLabel respStatusLabel;
    private JLabel queryLabel;

    private EasyComboBox<String> templateCombo;
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

    private static final String LABEL_DISABLED_FG = "Label.disabledForeground";
    private static final String SEPARATOR_FG = "Separator.foreground";
    private static final String AUTHORIZATION = "Authorization";
    private static final String TOKEN_PREFIX = "Token ";
    private static final String APPLICATION_VND_FLUX = "application/vnd.flux";
    private static final String TEXT_CSV = "text/csv";

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
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildConnectionPanel(), BorderLayout.NORTH);
        add(buildMainPanel(), BorderLayout.CENTER);
        switchMode(getSelectedMode());
    }

    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        JPanel baseRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        hostField = new JTextField(baseUrl, 22);
        hostField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_HOST_PLACEHOLDER));
        hostField.addActionListener(e -> doConnect());

        modeCombo = new JComboBox<>(QueryMode.values());
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

        connectionStatusLabel = new JLabel("●");
        connectionStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        connectionStatusLabel.setFont(connectionStatusLabel.getFont().deriveFont(Font.BOLD, 14f));
        connectionStatusLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_STATUS_NOT_CONNECTED));

        baseRow.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_HOST)));
        baseRow.add(hostField);
        baseRow.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_MODE)));
        baseRow.add(modeCombo);
        baseRow.add(connectBtn);
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
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

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
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

        dbCombo = new EasyComboBox<>(EasyComboBox.WidthMode.DYNAMIC);
        dbCombo.addActionListener(e -> {
            if (suppressComboEvents) return;
            if (getSelectedMode() != QueryMode.INFLUXQL_V1) return;
            String db = getSelectedDatabase();
            if (!db.isBlank() && connected) {
                loadMeasurements(db);
            }
        });

        measurementCombo = new EasyComboBox<>(EasyComboBox.WidthMode.DYNAMIC);
        measurementCombo.addActionListener(e -> {
            if (suppressComboEvents) return;
            if (getSelectedMode() != QueryMode.INFLUXQL_V1) return;
            String db = getSelectedDatabase();
            String measurement = getSelectedMeasurement();
            if (!db.isBlank() && !measurement.isBlank() && connected) {
                loadFieldKeys(db, measurement);
                loadTagKeys(db, measurement);
            }
            if (templateCombo != null && templateCombo.getSelectedIndex() >= 0) {
                loadTemplate();
            }
        });

        JButton reloadMetaBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_RELOAD_META));
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

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)));

        templateCombo = new EasyComboBox<>(EasyComboBox.WidthMode.DYNAMIC);

        JButton loadTemplateBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_LOAD_TEMPLATE));
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
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(executeBtn);
        toolbar.add(copyBtn);
        toolbar.add(clearBtn);

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

        JPanel queryPanel = new JPanel(new BorderLayout());
        queryLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_QUERY_TITLE));
        queryLabel.setFont(queryLabel.getFont().deriveFont(Font.BOLD, 11f));
        queryLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        queryEditor = createEditor(true);
        registerCtrlEnterShortcut(executeBtn);

        RTextScrollPane queryScroll = new RTextScrollPane(queryEditor);
        queryScroll.setLineNumbersEnabled(true);
        queryScroll.setBorder(BorderFactory.createEmptyBorder());
        queryPanel.add(queryLabel, BorderLayout.NORTH);
        queryPanel.add(queryScroll, BorderLayout.CENTER);

        JPanel resultPanel = new JPanel(new BorderLayout());
        JPanel respHeader = new JPanel(new BorderLayout());
        respHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        JLabel respLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_RESPONSE_TITLE));
        respLabel.setFont(respLabel.getFont().deriveFont(Font.BOLD, 11f));
        respStatusLabel = new JLabel("");
        respStatusLabel.setFont(respStatusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        respStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        respHeader.add(respLabel, BorderLayout.WEST);
        respHeader.add(respStatusLabel, BorderLayout.EAST);

        resultArea = createEditor(false);
        searchableResultArea = new SearchableTextArea(resultArea, false);
        resultTablePanel = new EnhancedTablePanel(new String[]{});
        resultViewLayout = new CardLayout();
        resultViewPanel = new JPanel(resultViewLayout);
        resultViewPanel.add(resultTablePanel, "table");
        resultViewPanel.add(searchableResultArea, "raw");
        showRawResult();
        resultPanel.add(respHeader, BorderLayout.NORTH);
        resultPanel.add(resultViewPanel, BorderLayout.CENTER);

        editorSplit.setTopComponent(queryPanel);
        editorSplit.setBottomComponent(resultPanel);
        queryPanel.setMinimumSize(new Dimension(0, 80));
        resultPanel.setMinimumSize(new Dimension(0, 140));
        // setResizeWeight 只影响后续 resize；这里显式设置初始位置
        SwingUtilities.invokeLater(() -> editorSplit.setDividerLocation(0.2));
        panel.add(editorSplit, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildV1QueryBuilderPanel() {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        wrapper.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)));

        // Fields 只读浏览
        fieldCombo = new EasyComboBox<>(EasyComboBox.WidthMode.DYNAMIC);
        wrapper.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_FIELD)));
        wrapper.add(fieldCombo);

        wrapper.add(new JSeparator(SwingConstants.VERTICAL));

        // Tag 浏览区
        wrapper.add(new JLabel("Tags:"));
        tagRowsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        wrapper.add(tagRowsPanel);

        addTagConditionRow();
        return wrapper;
    }

    private void addTagConditionRow() {
        TagConditionRow row = new TagConditionRow();
        row.panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));

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


    private RSyntaxTextArea createEditor(boolean editable) {
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
        textArea.setEditable(editable);
        EditorThemeUtil.loadTheme(textArea);
        return textArea;
    }

    private QueryMode getSelectedMode() {
        QueryMode mode = (QueryMode) modeCombo.getSelectedItem();
        return mode == null ? QueryMode.FLUX_V2 : mode;
    }

    private void switchMode(QueryMode mode) {
        connected = false;
        connectionStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        connectionStatusLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_STATUS_NOT_CONNECTED));

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
        String inputHost = hostField.getText().trim();
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
                    connectionStatusLabel.setForeground(Color.RED);
                    connectionStatusLabel.setToolTipText(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_STATUS_NOT_CONNECTED));
                    NotificationUtil.showError(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_ERR_CONNECT_FAILED),
                            ex.getMessage()));
                }
            }
        };
        worker.execute();
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
                String user = userField.getText().trim();
                String pass = new String(passwordField.getPassword()).trim();
                if (!user.isBlank()) path.append("&u=").append(enc(user));
                if (!pass.isBlank()) path.append("&p=").append(enc(pass));
                return callHttp("GET", path.toString(), null, null, "application/json", mode);
            }

            @Override
            protected void done() {
                executeBtn.setEnabled(true);
                try {
                    HttpResult result = get();
                    lastResponseBody = result.body();
                    renderResponse(lastResponseBody);
                    if (result.code() >= 200 && result.code() < 300) {
                        respStatusLabel.setText(MessageFormat.format(
                                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_STATUS_OK),
                                String.valueOf(result.costMs())));
                        respStatusLabel.setForeground(new Color(0, 160, 0));
                    } else {
                        respStatusLabel.setText(MessageFormat.format(
                                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_STATUS_ERROR),
                                String.valueOf(result.costMs())));
                        respStatusLabel.setForeground(Color.RED);
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
                return callHttp("GET", "/query?q=" + enc("SHOW DATABASES"), null, null, "application/json", QueryMode.INFLUXQL_V1);
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
                String path = "/query?db=" + enc(db) + "&q=" + enc(q);
                return callHttp("GET", path, null, null, "application/json", QueryMode.INFLUXQL_V1);
            }

            @Override
            protected void done() {
                try {
                    HttpResult result = get();
                    if (result.code() >= 200 && result.code() < 300) {
                        List<String> measurements = parseInfluxColumnValues(result.body(), 0);
                        String previous = getSelectedMeasurement();
                        setComboOptions(measurementCombo, measurements, previous);

                        String measurement = getSelectedMeasurement();
                        if (!db.isBlank() && !measurement.isBlank()) {
                            loadFieldKeys(db, measurement);
                            loadTagKeys(db, measurement);
                        }
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
                String path = "/query?db=" + enc(db) + "&q=" + enc(q);
                return callHttp("GET", path, null, null, "application/json", QueryMode.INFLUXQL_V1);
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
                String path = "/query?db=" + enc(db) + "&q=" + enc(q);
                return callHttp("GET", path, null, null, "application/json", QueryMode.INFLUXQL_V1);
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
                String path = "/query?db=" + enc(db) + "&q=" + enc(q);
                return callHttp("GET", path, null, null, "application/json", QueryMode.INFLUXQL_V1);
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

    private void renderResponse(String body) {
        resultArea.setText(body == null ? "" : body);
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

            List<String> headers = rows.get(0);
            if (headers == null || headers.isEmpty()) return false;

            String[] columns = headers.toArray(new String[0]);
            List<Object[]> dataRows = new ArrayList<>();
            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
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

    private Object nodeToValue(JsonNode node) {
        if (node == null || node.isNull()) return "";
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) return node.numberValue();
        if (node.isBoolean()) return node.asBoolean();
        return node.toString();
    }

    private void showTableResult() {
        if (resultViewLayout != null && resultViewPanel != null) {
            resultViewLayout.show(resultViewPanel, "table");
        }
    }

    private void showRawResult() {
        if (resultViewLayout != null && resultViewPanel != null) {
            resultViewLayout.show(resultViewPanel, "raw");
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
