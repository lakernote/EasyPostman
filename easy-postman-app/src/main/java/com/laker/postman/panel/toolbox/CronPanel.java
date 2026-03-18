package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.util.CronExpressionUtil;
import com.laker.postman.util.CronExpressionUtil.CronMode;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Date;
import java.util.List;

/**
 * Cron表达式工具面板
 * 支持 Spring/Quartz (6/7位) 和 Linux Crontab (5位) 两种模式切换
 */
@Slf4j
public class CronPanel extends JPanel {

    // ===== Mode =====
    private CronMode currentMode = CronMode.SPRING_QUARTZ;
    private JToggleButton springModeBtn;
    private JToggleButton linuxModeBtn;
    private JLabel modeBadge;

    // ===== Parse Tab =====
    private FlatTextField cronField;
    private JTextArea descriptionArea;
    private DefaultTableModel tableModel;
    private JLabel formatHintLabel;
    private JSpinner nextCountSpinner;
    /** Debounce timer: fires parseCron() 300 ms after the last keystroke. */
    private Timer debounceTimer;

    // ===== Generate Tab =====
    private JComboBox<String> secondCombo;
    private JComboBox<String> minuteCombo;
    private JComboBox<String> hourCombo;
    private JComboBox<String> dayCombo;
    private JComboBox<String> monthCombo;
    private JComboBox<String> weekCombo;
    private FlatTextField yearField;
    private JPanel secondRow;
    private JPanel yearRow;
    private JLabel generateFormatLabel;
    private JTextField generatedField;
    private JLabel generateDescLabel;
    private JTextArea presetArea;

    public CronPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        add(buildModeBar(), BorderLayout.NORTH);
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_TAB_PARSE), buildParsePanel());
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_TAB_GENERATE), buildGeneratePanel());
        add(tabbedPane, BorderLayout.CENTER);
    }

    // =========================================================
    // Mode bar
    // =========================================================
    private JPanel buildModeBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                UIManager.getColor("Separator.foreground")));

        JLabel lbl = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_MODE_LABEL) + ":");
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        bar.add(lbl);

        springModeBtn = new JToggleButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_MODE_SPRING));
        linuxModeBtn  = new JToggleButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_MODE_LINUX));
        springModeBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_MODE_TOOLTIP_SPRING));
        linuxModeBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_MODE_TOOLTIP_LINUX));

        ButtonGroup group = new ButtonGroup();
        group.add(springModeBtn);
        group.add(linuxModeBtn);
        springModeBtn.setSelected(true);
        bar.add(springModeBtn);
        bar.add(linuxModeBtn);

        modeBadge = new JLabel("S  M  H  D  Mo  W  [Y]");
        modeBadge.setOpaque(true);
        modeBadge.setBackground(new Color(0x4C8CF8));
        modeBadge.setForeground(Color.WHITE);
        modeBadge.setFont(modeBadge.getFont().deriveFont(Font.BOLD, 11f));
        modeBadge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        bar.add(Box.createHorizontalStrut(8));
        bar.add(modeBadge);

        springModeBtn.addActionListener(e -> { currentMode = CronMode.SPRING_QUARTZ; applyModeToUI(); });
        linuxModeBtn.addActionListener(e  -> { currentMode = CronMode.LINUX_CRONTAB; applyModeToUI(); });
        return bar;
    }

    // =========================================================
    // Parse panel
    // =========================================================
    private JPanel buildParsePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        // ── input area ──────────────────────────────────────────────
        JPanel inputPanel = new JPanel(new BorderLayout(5, 4));

        formatHintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FORMAT_SPRING));
        formatHintLabel.setFont(formatHintLabel.getFont().deriveFont(Font.ITALIC, 11f));
        formatHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        inputPanel.add(formatHintLabel, BorderLayout.NORTH);

        JPanel fieldRow = new JPanel(new BorderLayout(5, 0));
        cronField = new FlatTextField();
        cronField.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +2));
        cronField.setText("0 0 12 * * ?");
        cronField.setPlaceholderText(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PLACEHOLDER_SPRING));
        fieldRow.add(cronField, BorderLayout.CENTER);

        JPanel countPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        countPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_NEXT_COUNT) + ":"));
        nextCountSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 50, 1));
        nextCountSpinner.setPreferredSize(new Dimension(60, 24));
        countPanel.add(nextCountSpinner);
        fieldRow.add(countPanel, BorderLayout.EAST);
        inputPanel.add(fieldRow, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 4));
        JButton parseBtn  = createAccentButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PARSE));
        JButton copyBtn   = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        JButton clearBtn  = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));
        btnRow.add(parseBtn);
        btnRow.add(copyBtn);
        btnRow.add(clearBtn);
        inputPanel.add(btnRow, BorderLayout.SOUTH);
        panel.add(inputPanel, BorderLayout.NORTH);

        // ── split: description (top) + execution times (bottom) ─────
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.4);   // 40% description, 60% table
        split.setBorder(null);

        JPanel descPanel = new JPanel(new BorderLayout(4, 4));
        descPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESCRIPTION),
                TitledBorder.LEFT, TitledBorder.TOP));
        descriptionArea = new JTextArea();
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descPanel.add(new JScrollPane(descriptionArea), BorderLayout.CENTER);
        split.setTopComponent(descPanel);

        JPanel tablePanel = new JPanel(new BorderLayout(4, 4));
        tablePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_NEXT_EXECUTIONS),
                TitledBorder.LEFT, TitledBorder.TOP));
        String[] cols = {"#", I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_EXECUTION_TIME)};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable nextExecutionTable = new JTable(tableModel);
        nextExecutionTable.getColumnModel().getColumn(0).setMaxWidth(50);
        // Double-click a row → copy the expression back to the input field
        nextExecutionTable.setToolTipText(null);
        tablePanel.add(new JScrollPane(nextExecutionTable), BorderLayout.CENTER);
        split.setBottomComponent(tablePanel);
        panel.add(split, BorderLayout.CENTER);

        // ── events ───────────────────────────────────────────────────
        // Real-time parse with 300 ms debounce
        debounceTimer = new Timer(300, e -> parseCron());
        debounceTimer.setRepeats(false);
        cronField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { debounceTimer.restart(); }
            @Override public void removeUpdate(DocumentEvent e)  { debounceTimer.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { debounceTimer.restart(); }
        });
        // Spinner change → re-parse immediately
        nextCountSpinner.addChangeListener(e -> parseCron());

        parseBtn.addActionListener(e -> parseCron());
        copyBtn.addActionListener(e  -> copyWithFeedback(copyBtn, cronField.getText()));
        clearBtn.addActionListener(e -> {
            cronField.setText("");
            descriptionArea.setText("");
            tableModel.setRowCount(0);
        });

        // Trigger initial parse so the panel shows results on first open
        SwingUtilities.invokeLater(this::parseCron);
        return panel;
    }

    // =========================================================
    // Generate panel
    // =========================================================
    private JPanel buildGeneratePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel fmtRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        generateFormatLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FORMAT_SPRING));
        generateFormatLabel.setFont(generateFormatLabel.getFont().deriveFont(Font.ITALIC, 11f));
        generateFormatLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        fmtRow.add(generateFormatLabel);
        top.add(fmtRow);
        top.add(Box.createVerticalStrut(4));

        secondRow = createFieldRow(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_SECOND) + ":",
                secondCombo = createEditableCombo("*","0","15","30","45","0/5","0/10","0/15","0/30"));
        top.add(secondRow);

        minuteCombo = createEditableCombo("*","0","15","30","45","0/5","0/10","0/15","0/30");
        hourCombo   = createEditableCombo("*","0","6","8","12","18","0/2","0/4","0/6","0/12");
        dayCombo    = createEditableCombo("*","?","1","15","L","1-15","*/2");
        monthCombo  = createEditableCombo("*","1","2","3","4","5","6","7","8","9","10","11","12","1-6","*/2","*/3");
        weekCombo   = createEditableCombo("?","*","1","2","3","4","5","6","7","1-5","MON-FRI","SUN","MON","TUE","WED","THU","FRI","SAT");

        top.add(createFieldRow(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_MINUTE) + ":", minuteCombo));
        top.add(createFieldRow(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_HOUR)   + ":", hourCombo));
        top.add(createFieldRow(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_DAY)    + ":", dayCombo));
        top.add(createFieldRow(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_MONTH)  + ":", monthCombo));
        top.add(createFieldRow(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_WEEK)   + ":", weekCombo));

        yearRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JLabel yearLbl = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_YEAR_OPTIONAL) + ":");
        yearLbl.setPreferredSize(new Dimension(160, 25));
        yearField = new FlatTextField();
        yearField.setColumns(10);
        yearField.setPlaceholderText(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_YEAR_PLACEHOLDER));
        yearRow.add(yearLbl);
        yearRow.add(yearField);
        top.add(yearRow);
        top.add(Box.createVerticalStrut(6));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JButton generateBtn = createAccentButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_GENERATE));
        JButton copyGenBtn  = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        JButton presetBtn   = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_QUICK_PRESETS) + " ▾");
        btnRow.add(generateBtn);
        btnRow.add(copyGenBtn);
        btnRow.add(presetBtn);
        top.add(btnRow);
        top.add(Box.createVerticalStrut(4));

        // Result row
        JPanel resultRow = new JPanel(new BorderLayout(6, 0));
        resultRow.setBorder(BorderFactory.createEmptyBorder(0, 4, 2, 4));
        resultRow.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_GENERATED) + ":"), BorderLayout.WEST);
        generatedField = new JTextField();
        generatedField.setEditable(false);
        generatedField.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +2));
        resultRow.add(generatedField, BorderLayout.CENTER);
        top.add(resultRow);

        // Live description of generated expression
        generateDescLabel = new JLabel(" ");
        generateDescLabel.setFont(generateDescLabel.getFont().deriveFont(Font.ITALIC, 11f));
        generateDescLabel.setForeground(new Color(0x2AA665));
        generateDescLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 4, 4));
        top.add(generateDescLabel);

        panel.add(top, BorderLayout.NORTH);

        // Preset panel — double-click to use
        JPanel presetPanel = new JPanel(new BorderLayout(4, 4));
        presetPanel.setBorder(BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_COMMON_PRESETS)));
        presetArea = new JTextArea();
        presetArea.setEditable(false);
        presetArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        presetArea.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_TAB_PARSE) + " ← double-click");
        updatePresetArea();

        // Double-click on a preset line → extract expression and use it
        presetArea.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) applyPresetFromClick();
            }
        });

        presetPanel.add(new JScrollPane(presetArea), BorderLayout.CENTER);
        panel.add(presetPanel, BorderLayout.CENTER);

        // Live-update generated expression and description on any combo change
        java.awt.event.ActionListener liveUpdate = e -> refreshGenerated();
        secondCombo.addActionListener(liveUpdate);
        minuteCombo.addActionListener(liveUpdate);
        hourCombo.addActionListener(liveUpdate);
        dayCombo.addActionListener(liveUpdate);
        monthCombo.addActionListener(liveUpdate);
        weekCombo.addActionListener(liveUpdate);
        yearField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { refreshGenerated(); }
            @Override public void removeUpdate(DocumentEvent e)  { refreshGenerated(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshGenerated(); }
        });

        generateBtn.addActionListener(e -> {
            refreshGenerated();
            // Also push to parse tab
            cronField.setText(generatedField.getText());
        });
        copyGenBtn.addActionListener(e -> copyWithFeedback(copyGenBtn, generatedField.getText()));
        presetBtn.addActionListener(e -> showPresetMenu(presetBtn, generatedField));

        // Initial value
        SwingUtilities.invokeLater(this::refreshGenerated);
        return panel;
    }

    /** Extract the cron expression from the double-clicked preset line and use it. */
    private void applyPresetFromClick() {
        int pos = presetArea.getCaretPosition();
        try {
            int lineStart = javax.swing.text.Utilities.getRowStart(presetArea, pos);
            int lineEnd   = javax.swing.text.Utilities.getRowEnd(presetArea, pos);
            String line   = presetArea.getText().substring(lineStart, lineEnd).trim();
            // Lines look like:  "  Every minute                   * * * * *"
            // The expression is the last whitespace-separated token(s) — grab from last 2+ spaces
            int sep = line.lastIndexOf("  ");
            if (sep >= 0) {
                String expr = line.substring(sep).trim();
                if (CronExpressionUtil.isValid(expr, currentMode)) {
                    cronField.setText(expr);
                    generatedField.setText(expr);
                    parseCron();
                }
            }
        } catch (Exception ex) {
            log.debug("applyPresetFromClick: {}", ex.getMessage());
        }
    }

    /** Refresh the generated expression field + live description label. */
    private void refreshGenerated() {
        String cron = buildCronFromCombos();
        generatedField.setText(cron);
        if (CronExpressionUtil.isValid(cron, currentMode)) {
            generateDescLabel.setText(buildDescription(cron, currentMode));
            generateDescLabel.setForeground(new Color(0x2AA665));
        } else {
            generateDescLabel.setText(" ");
        }
    }

    private void updatePresetArea() {
        StringBuilder sb = new StringBuilder();
        if (currentMode == CronMode.SPRING_QUARTZ) {
            sb.append("── Spring / Quartz  (Second Minute Hour Day Month Week [Year]) ──\n\n");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_SECOND),    "* * * * * ?");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_MINUTE),    "0 * * * * ?");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_5MIN),      "0 */5 * * * ?");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_15MIN),     "0 */15 * * * ?");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_30MIN),     "0 */30 * * * ?");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_HOUR),      "0 0 * * * ?");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_2HOUR),     "0 0 */2 * * ?");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_DAILY_NOON),      "0 0 12 * * ?");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_DAILY_MIDNIGHT),  "0 0 0 * * ?");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_MONDAY_9AM),      "0 0 9 ? * MON");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_WEEKDAY_9AM),     "0 0 9 ? * MON-FRI");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_FIRST_DAY_MONTH), "0 0 0 1 * ?");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_LAST_DAY_MONTH),  "0 0 0 L * ?");
        } else {
            sb.append("── Linux Crontab  (Minute Hour Day Month Week) ──\n\n");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_MINUTE),    "* * * * *");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_5MIN),      "*/5 * * * *");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_15MIN),     "*/15 * * * *");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_30MIN),     "*/30 * * * *");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_HOUR),      "0 * * * *");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_2HOUR),     "0 */2 * * *");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_DAILY_NOON),      "0 12 * * *");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_DAILY_MIDNIGHT),  "0 0 * * *");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_MONDAY_9AM),      "0 9 * * 1");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_WEEKDAY_9AM),     "0 9 * * 1-5");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_FIRST_DAY_MONTH), "0 0 1 * *");
            appendPreset(sb, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_LAST_DAY_MONTH),  "0 0 28-31 * *");
        }

        sb.append("\n").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_SPECIAL_CHARS)).append(":\n");
        sb.append("  *   ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_SPECIAL_WILDCARD)).append("\n");
        if (currentMode == CronMode.SPRING_QUARTZ) {
            sb.append("  ?   ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_SPECIAL_QUESTION)).append("\n");
            sb.append("  L   ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_SPECIAL_LAST)).append("\n");
            sb.append("  W   ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_SPECIAL_WEEKDAY)).append("\n");
            sb.append("  #   ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_SPECIAL_HASH)).append("\n");
        }
        sb.append("  -   ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_SPECIAL_RANGE)).append("\n");
        sb.append("  ,   ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_SPECIAL_LIST)).append("\n");
        sb.append("  /   ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_SPECIAL_STEP)).append("\n");
        if (presetArea != null) {
            presetArea.setText(sb.toString());
            presetArea.setCaretPosition(0);
        }
    }

    private void appendPreset(StringBuilder sb, String label, String expr) {
        sb.append(String.format("  %-32s %s%n", label, expr));
    }

    // =========================================================
    // Mode switch
    // =========================================================
    private void applyModeToUI() {
        boolean linux = (currentMode == CronMode.LINUX_CRONTAB);

        modeBadge.setText(linux ? "M  H  D  Mo  W" : "S  M  H  D  Mo  W  [Y]");
        modeBadge.setBackground(linux ? new Color(0x2AA665) : new Color(0x4C8CF8));

        formatHintLabel.setText(linux
                ? I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FORMAT_LINUX)
                : I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FORMAT_SPRING));
        cronField.setPlaceholderText(linux
                ? I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PLACEHOLDER_LINUX)
                : I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PLACEHOLDER_SPRING));
        cronField.setText(linux ? "0 12 * * *" : "0 0 12 * * ?");
        descriptionArea.setText("");
        tableModel.setRowCount(0);

        generateFormatLabel.setText(linux
                ? I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FORMAT_LINUX)
                : I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FORMAT_SPRING));
        secondRow.setVisible(!linux);
        yearRow.setVisible(!linux);

        // Swap week combo options: Linux uses 0-7 numerics, Spring uses 1-7 + named
        weekCombo.removeAllItems();
        if (linux) {
            for (String s : new String[]{"*","0","1","2","3","4","5","6","7","1-5"}) weekCombo.addItem(s);
        } else {
            for (String s : new String[]{"?","*","1","2","3","4","5","6","7","1-5","MON-FRI","SUN","MON","TUE","WED","THU","FRI","SAT"}) weekCombo.addItem(s);
        }
        weekCombo.setSelectedIndex(0);

        updatePresetArea();
        SwingUtilities.invokeLater(this::parseCron);
        revalidate();
        repaint();
    }

    // =========================================================
    // Parse
    // =========================================================
    private void parseCron() {
        String expr = cronField.getText().trim();
        if (expr.isEmpty()) {
            descriptionArea.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_ERROR_EMPTY));
            tableModel.setRowCount(0);
            return;
        }

        String[] parts = expr.split("\\s+");
        CronMode mode = (parts.length == 5) ? CronMode.LINUX_CRONTAB : CronMode.SPRING_QUARTZ;

        if (!CronExpressionUtil.isValid(expr, mode)) {
            String errKey = (mode == CronMode.LINUX_CRONTAB)
                    ? MessageKeys.TOOLBOX_CRON_ERROR_INVALID_LINUX
                    : MessageKeys.TOOLBOX_CRON_ERROR_INVALID_SPRING;
            descriptionArea.setText(I18nUtil.getMessage(errKey));
            tableModel.setRowCount(0);
            return;
        }

        try {
            StringBuilder desc = new StringBuilder();
            desc.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_ANALYSIS)).append("\n\n");
            desc.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_EXPRESSION)).append(":  ").append(expr).append("\n");
            desc.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_MODE_LABEL)).append(":  ").append(
                    mode == CronMode.LINUX_CRONTAB
                            ? I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_MODE_LINUX)
                            : I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_MODE_SPRING)
            ).append("\n\n");
            desc.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELDS)).append(":\n");

            if (mode == CronMode.LINUX_CRONTAB) {
                desc.append("  ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_MINUTE)).append(":  ").append(parts[0]).append("\n");
                desc.append("  ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_HOUR))  .append(":  ").append(parts[1]).append("\n");
                desc.append("  ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_DAY))   .append(":  ").append(parts[2]).append("\n");
                desc.append("  ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_MONTH)) .append(":  ").append(parts[3]).append("\n");
                desc.append("  ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_WEEK))  .append(":  ").append(parts[4]).append("\n");
            } else {
                desc.append("  ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_SECOND)).append(":  ").append(parts[0]).append("\n");
                desc.append("  ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_MINUTE)).append(":  ").append(parts[1]).append("\n");
                desc.append("  ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_HOUR))  .append(":  ").append(parts[2]).append("\n");
                desc.append("  ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_DAY))   .append(":  ").append(parts[3]).append("\n");
                desc.append("  ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_MONTH)) .append(":  ").append(parts[4]).append("\n");
                desc.append("  ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_FIELD_WEEK_SPRING)).append(":  ").append(parts[5]).append("\n");
                if (parts.length > 6) {
                    desc.append("  ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_YEAR)).append(":  ").append(parts[6]).append("\n");
                }
            }

            desc.append("\n").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESCRIPTION)).append(":\n");
            desc.append(buildDescription(expr, mode));
            descriptionArea.setText(desc.toString());
            descriptionArea.setCaretPosition(0);

            int count = (int) nextCountSpinner.getValue();
            List<Date> times = CronExpressionUtil.getNextExecutionTimes(expr, count, mode);
            tableModel.setRowCount(0);
            int idx = 1;
            for (Date t : times) {
                tableModel.addRow(new Object[]{idx++, CronExpressionUtil.formatDate(t)});
            }
            if (times.isEmpty()) {
                tableModel.addRow(new Object[]{"N/A", I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_UNABLE_CALCULATE)});
            }

        } catch (Exception ex) {
            descriptionArea.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_ERROR_PARSE) + ":\n" + ex.getMessage());
            tableModel.setRowCount(0);
            log.error("Cron parse error", ex);
        }
    }

    // =========================================================
    // i18n description builder
    // =========================================================
    private String buildDescription(String expr, CronMode mode) {
        try {
            String normalized = CronExpressionUtil.normalizeCron(expr, mode);
            String[] p = normalized.trim().split("\\s+");
            if (p.length < 6) return "";

            String sec  = p[0];
            String min  = p[1];
            String hour = p[2];
            String day  = p[3];
            String mon  = p[4];
            String week = p[5];
            String year = p.length > 6 ? p[6] : "*";

            StringBuilder sb = new StringBuilder();

            if (mode == CronMode.LINUX_CRONTAB) {
                if ("*".equals(min) && "*".equals(hour)) {
                    sb.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESC_EVERY_MINUTE));
                } else if ("*".equals(hour)) {
                    sb.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESC_EVERY_HOUR_AT_MINUTE)
                            .replace("{0}", min));
                } else {
                    sb.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESC_AT)
                            .replace("{0}", fmtHM(hour, min)));
                }
            } else {
                if ("*".equals(sec) && "*".equals(min) && "*".equals(hour)) {
                    sb.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESC_EVERY_SECOND));
                } else if ("*".equals(min) && "*".equals(hour)) {
                    sb.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESC_EVERY_MINUTE_AT_SECOND)
                            .replace("{0}", sec));
                } else if ("*".equals(hour)) {
                    sb.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESC_EVERY_HOUR_AT)
                            .replace("{0}", fmtMS(min, sec)));
                } else {
                    sb.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESC_AT)
                            .replace("{0}", fmtHMS(hour, min, sec)));
                }
            }

            if (!"*".equals(day) && !"?".equals(day)) {
                sb.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESC_ON_DAY).replace("{0}", day));
            }
            if (!"*".equals(week) && !"?".equals(week)) {
                sb.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESC_ON_WEEKDAY)
                        .replace("{0}", describeWeekI18n(week, mode)));
            }
            if (!"*".equals(mon)) {
                sb.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESC_IN_MONTH)
                        .replace("{0}", describeMonthI18n(mon)));
            }
            if (!"*".equals(year)) {
                sb.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESC_IN_YEAR).replace("{0}", year));
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("buildDescription error", e);
            return "";
        }
    }

    private static String fmtHMS(String h, String m, String s) { return pad(h) + ":" + pad(m) + ":" + pad(s); }
    private static String fmtHM(String h, String m)            { return pad(h) + ":" + pad(m); }
    private static String fmtMS(String m, String s)            { return pad(m) + ":" + pad(s); }
    private static String pad(String v) {
        try { return String.format("%02d", Integer.parseInt(v)); }
        catch (NumberFormatException e) { return v; }
    }

    private String describeWeekI18n(String weekExpr, CronMode mode) {
        String expr = weekExpr.toUpperCase()
                .replace("SUN","0").replace("MON","1").replace("TUE","2")
                .replace("WED","3").replace("THU","4").replace("FRI","5").replace("SAT","6");
        if ("1-5".equals(expr) || "MON-FRI".equals(weekExpr.toUpperCase())) {
            return I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESC_WEEK_WEEKDAYS);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (Character.isDigit(c)) {
                int v = c - '0';
                if (mode == CronMode.SPRING_QUARTZ) v = v - 1; // 1-based → 0-based
                if (v == 7 || v == -1) v = 0;
                sb.append(v >= 0 && v <= 6
                        ? I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESC_WEEKDAYS[v])
                        : c);
            } else if (c == '-') {
                sb.append("~");
            } else if (c == ',') {
                sb.append("、");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String describeMonthI18n(String monthExpr) {
        if (monthExpr.contains("/")) {
            return I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESC_EVERY_N_MONTHS)
                    .replace("{0}", monthExpr.split("/")[1]);
        }
        try {
            int m = Integer.parseInt(monthExpr);
            if (m >= 1 && m <= 12) return I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESC_MONTHS[m]);
        } catch (NumberFormatException ignored) { }
        return monthExpr;
    }

    // =========================================================
    // Generate helpers
    // =========================================================
    private String buildCronFromCombos() {
        if (currentMode == CronMode.LINUX_CRONTAB) {
            return minuteCombo.getSelectedItem() + " "
                    + hourCombo.getSelectedItem() + " "
                    + dayCombo.getSelectedItem() + " "
                    + monthCombo.getSelectedItem() + " "
                    + weekCombo.getSelectedItem();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(secondCombo.getSelectedItem()).append(" ");
        sb.append(minuteCombo.getSelectedItem()).append(" ");
        sb.append(hourCombo.getSelectedItem()).append(" ");
        sb.append(dayCombo.getSelectedItem()).append(" ");
        sb.append(monthCombo.getSelectedItem()).append(" ");
        sb.append(weekCombo.getSelectedItem());
        String year = yearField.getText().trim();
        if (!year.isEmpty()) sb.append(" ").append(year);
        return sb.toString();
    }

    private void showPresetMenu(JButton anchor, JTextField target) {
        JPopupMenu menu = new JPopupMenu();
        if (currentMode == CronMode.LINUX_CRONTAB) {
            addPreset(menu, target, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_MINUTE),  "* * * * *");
            addPreset(menu, target, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_5MIN),    "*/5 * * * *");
            addPreset(menu, target, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_HOUR),    "0 * * * *");
            addPreset(menu, target, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_DAILY_NOON),    "0 12 * * *");
            addPreset(menu, target, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_DAILY_MIDNIGHT),"0 0 * * *");
            addPreset(menu, target, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_WEEKDAY_9AM),   "0 9 * * 1-5");
        } else {
            addPreset(menu, target, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_MINUTE),  "0 * * * * ?");
            addPreset(menu, target, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_5MIN),    "0 */5 * * * ?");
            addPreset(menu, target, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_EVERY_HOUR),    "0 0 * * * ?");
            addPreset(menu, target, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_DAILY_NOON),    "0 0 12 * * ?");
            addPreset(menu, target, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_DAILY_MIDNIGHT),"0 0 0 * * ?");
            addPreset(menu, target, I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PRESET_WEEKDAY_9AM),   "0 0 9 ? * MON-FRI");
        }
        menu.show(anchor, 0, anchor.getHeight());
    }

    private void addPreset(JPopupMenu menu, JTextField target, String label, String expr) {
        JMenuItem item = new JMenuItem(label + "  →  " + expr);
        item.addActionListener(e -> {
            target.setText(expr);
            cronField.setText(expr);
            parseCron();
        });
        menu.add(item);
    }

    // =========================================================
    // Util
    // =========================================================
    private JButton createAccentButton(String text) {
        JButton btn = new JButton(text);
        btn.putClientProperty("JButton.buttonType", "default");
        return btn;
    }

    private JComboBox<String> createEditableCombo(String... items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setEditable(true);
        combo.setPreferredSize(new Dimension(220, 26));
        return combo;
    }

    private JPanel createFieldRow(String label, JComboBox<String> combo) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JLabel lbl = new JLabel(label);
        lbl.setPreferredSize(new Dimension(160, 25));
        row.add(lbl);
        row.add(combo);
        return row;
    }

    /** Copy text to clipboard and briefly change the button label to give feedback. */
    private void copyWithFeedback(JButton btn, String text) {
        if (text == null || text.isEmpty()) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        String original = btn.getText();
        btn.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_COPIED));
        btn.setEnabled(false);
        Timer restore = new Timer(1200, e -> {
            btn.setText(original);
            btn.setEnabled(true);
        });
        restore.setRepeats(false);
        restore.start();
    }
}
