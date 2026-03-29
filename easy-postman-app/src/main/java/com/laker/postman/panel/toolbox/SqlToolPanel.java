package com.laker.postman.panel.toolbox;

import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.util.EditorThemeUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.SqlFormatter;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL 工具面板。
 */
@Slf4j
public class SqlToolPanel extends JPanel {

    private RSyntaxTextArea inputArea;
    private RSyntaxTextArea outputArea;
    private JLabel statusLabel;

    private JSpinner indentSpinner;
    private JComboBox<SqlFormatter.SqlDialect> dialectComboBox;
    private JCheckBox uppercaseKeywordsCheck;
    private JCheckBox addSemicolonCheck;
    private JCheckBox lineBreakAndCheck;
    private JCheckBox lineBreakCommaCheck;

    public SqlToolPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createTopPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createStatusPanel(), BorderLayout.SOUTH);

        setupKeyBindings();
    }

    private JPanel createTopPanel() {
        JPanel container = new JPanel(new BorderLayout(0, 6));
        container.add(createToolbarPanel(), BorderLayout.NORTH);
        container.add(createOptionsPanel(), BorderLayout.SOUTH);
        return container;
    }

    private JPanel createToolbarPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel leftBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton formatBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_FORMAT));
        JButton compressBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_COMPRESS));
        JButton validateBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATE));

        formatBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_TOOLTIP_FORMAT));
        compressBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_TOOLTIP_COMPRESS));
        validateBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_TOOLTIP_VALIDATE));

        leftBtnPanel.add(formatBtn);
        leftBtnPanel.add(compressBtn);
        leftBtnPanel.add(validateBtn);

        JPanel rightBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        JButton sampleBtn = new JButton("📝 " + I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_SAMPLE));
        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        JButton pasteBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_PASTE));
        JButton clearBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));
        JButton swapBtn = new JButton("↕ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_SWAP));

        sampleBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_TOOLTIP_SAMPLE));
        copyBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_TOOLTIP_COPY));
        pasteBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_TOOLTIP_PASTE));
        clearBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_TOOLTIP_CLEAR));
        swapBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_TOOLTIP_SWAP));

        rightBtnPanel.add(sampleBtn);
        rightBtnPanel.add(copyBtn);
        rightBtnPanel.add(pasteBtn);
        rightBtnPanel.add(clearBtn);
        rightBtnPanel.add(swapBtn);

        topPanel.add(leftBtnPanel, BorderLayout.WEST);
        topPanel.add(rightBtnPanel, BorderLayout.EAST);

        formatBtn.addActionListener(e -> formatSql());
        compressBtn.addActionListener(e -> compressSql());
        validateBtn.addActionListener(e -> validateSql());
        sampleBtn.addActionListener(e -> loadSampleSql());
        copyBtn.addActionListener(e -> copyToClipboard());
        pasteBtn.addActionListener(e -> pasteFromClipboard());
        clearBtn.addActionListener(e -> clearAll());
        swapBtn.addActionListener(e -> swapInputOutput());

        return topPanel;
    }

    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        Color borderColor = UIManager.getColor("Component.borderColor");
        if (borderColor == null) {
            borderColor = UIManager.getColor("Separator.foreground");
        }
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, borderColor),
                BorderFactory.createEmptyBorder(6, 0, 6, 0)
        ));

        panel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_INDENT) + ":"));
        indentSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 8, 1));
        indentSpinner.setPreferredSize(new Dimension(60, 26));
        panel.add(indentSpinner);

        panel.add(new JSeparator(SwingConstants.VERTICAL));

        panel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_DIALECT) + ":"));
        dialectComboBox = new JComboBox<>(SqlFormatter.SqlDialect.values());
        dialectComboBox.setPreferredSize(new Dimension(140, 28));
        dialectComboBox.setSelectedItem(SqlFormatter.SqlDialect.GENERIC);
        dialectComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SqlFormatter.SqlDialect dialect) {
                    setText(getDialectDisplayName(dialect));
                }
                return this;
            }
        });
        panel.add(dialectComboBox);

        panel.add(new JSeparator(SwingConstants.VERTICAL));

        uppercaseKeywordsCheck = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_UPPERCASE_KEYWORDS), true);
        addSemicolonCheck = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_ADD_SEMICOLON), true);
        lineBreakAndCheck = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_LINE_BREAK_AND_OR), true);
        lineBreakCommaCheck = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_LINE_BREAK_COMMA), true);

        panel.add(uppercaseKeywordsCheck);
        panel.add(addSemicolonCheck);
        panel.add(lineBreakAndCheck);
        panel.add(lineBreakCommaCheck);

        return panel;
    }

    private JPanel createCenterPanel() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(createEditorPanel(true));
        splitPane.setBottomComponent(createEditorPanel(false));
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.5);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createEditorPanel(boolean input) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JLabel titleLabel = new JLabel(I18nUtil.getMessage(
                input ? MessageKeys.TOOLBOX_SQL_INPUT : MessageKeys.TOOLBOX_SQL_OUTPUT));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(titleLabel, BorderLayout.WEST);
        panel.add(headerPanel, BorderLayout.NORTH);

        RSyntaxTextArea textArea = createSqlTextArea();
        textArea.setEditable(input);
        SearchableTextArea searchableTextArea = new SearchableTextArea(textArea, input);
        searchableTextArea.setLineNumbersEnabled(true);
        panel.add(searchableTextArea, BorderLayout.CENTER);

        if (input) {
            inputArea = textArea;
        } else {
            outputArea = textArea;
        }

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        statusLabel = new JLabel(" ");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statusPanel.add(statusLabel);
        return statusPanel;
    }

    private RSyntaxTextArea createSqlTextArea() {
        RSyntaxTextArea textArea = new RSyntaxTextArea(10, 40);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setAutoIndentEnabled(true);
        textArea.setTabSize(2);
        textArea.setTabsEmulated(true);
        textArea.setMarkOccurrences(true);
        textArea.setPaintTabLines(true);
        textArea.setAnimateBracketMatching(true);
        EditorThemeUtil.loadTheme(textArea);
        return textArea;
    }

    private void setupKeyBindings() {
        addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                "format-sql", e -> formatSql());
        addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                "compress-sql", e -> compressSql());
        addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                "validate-sql", e -> validateSql());
        addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                "swap-sql", e -> swapInputOutput());
        addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK),
                "clear-sql", e -> clearAll());
    }

    private void addKeyBinding(KeyStroke keyStroke, String actionName, java.awt.event.ActionListener action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionName);
        getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.actionPerformed(e);
            }
        });
    }

    private void formatSql() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            outputArea.setText("");
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_EMPTY), false);
            return;
        }

        try {
            String formatted = SqlFormatter.format(input, createFormatOption());
            outputArea.setText(formatted);
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_FORMATTED,
                    String.valueOf(countLines(formatted)), String.valueOf(formatted.length())), true);
        } catch (Exception ex) {
            log.error("SQL format error", ex);
            handleFormatError(ex);
        }
    }

    private void compressSql() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            outputArea.setText("");
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_EMPTY), false);
            return;
        }

        try {
            String compressed = SqlFormatter.compress(input, getSelectedDialect());
            outputArea.setText(compressed);
            int reduction = input.length() - compressed.length();
            double percent = reduction > 0 ? (reduction * 100.0) / input.length() : 0;
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_COMPRESSED,
                    String.valueOf(Math.max(0, reduction)), String.format("%.1f", percent)), true);
        } catch (Exception ex) {
            log.error("SQL compress error", ex);
            handleFormatError(ex);
        }
    }

    private void validateSql() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            outputArea.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EMPTY));
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_EMPTY), false);
            return;
        }

        try {
            SqlFormatter.ValidationResult validationResult = SqlFormatter.validate(input, getSelectedDialect());
            List<String> issues = new ArrayList<>(validationResult.issues());

            boolean hasValidKeyword = validationResult.statementCount() > 0;
            StringBuilder info = new StringBuilder();
            if (issues.isEmpty() && hasValidKeyword) {
                info.append("✓ ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_VALID)).append("\n\n");
            } else if (!hasValidKeyword) {
                info.append("⚠ ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_NO_KEYWORDS)).append("\n\n");
            } else {
                info.append("❌ ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_ISSUES)).append("\n\n");
            }

            info.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_CHARACTERS))
                    .append(": ").append(input.length()).append("\n");
            info.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_LINES))
                    .append(": ").append(countLines(input)).append("\n");
            info.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_STATEMENTS))
                    .append(": ").append(Math.max(1, validationResult.statementCount())).append("\n");

            if (!issues.isEmpty()) {
                info.append("\n").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_FOUND_ISSUES)).append("\n");
                for (int i = 0; i < issues.size(); i++) {
                    info.append(i + 1).append(". ").append(issues.get(i)).append("\n");
                }
            }

            outputArea.setText(info.toString());
            updateStatus(issues.isEmpty() && hasValidKeyword
                            ? I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_VALIDATED)
                            : I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_INVALID),
                    issues.isEmpty() && hasValidKeyword);
        } catch (Exception ex) {
            log.error("SQL validate error", ex);
            handleFormatError(ex);
        }
    }

    private SqlFormatter.FormatOption createFormatOption() {
        int indentSize = (Integer) indentSpinner.getValue();
        boolean uppercaseKeywords = uppercaseKeywordsCheck.isSelected();
        boolean addSemicolon = addSemicolonCheck.isSelected();
        boolean lineBreakAnd = lineBreakAndCheck.isSelected();
        boolean lineBreakComma = lineBreakCommaCheck.isSelected();

        return new SqlFormatter.FormatOption()
                .setIndent(indentSize)
                .setDialect(getSelectedDialect())
                .setUppercaseKeywords(uppercaseKeywords)
                .setAddSemicolon(addSemicolon)
                .setLineBreakBeforeAnd(lineBreakAnd)
                .setLineBreakBeforeOr(lineBreakAnd)
                .setLineBreakAfterComma(lineBreakComma);
    }

    private void handleFormatError(Exception ex) {
        String errorMessage = "❌ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_ERROR) + ":\n\n" + ex.getMessage();
        outputArea.setText(errorMessage);
        updateStatus("❌ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_ERROR) + ": " + ex.getMessage(), false);
    }

    private void loadSampleSql() {
        String sampleSql = switch (getSelectedDialect()) {
            case MYSQL -> "select u.id,u.name,sum(o.total) total from `users` u left join `orders` o on u.id=o.user_id where u.status=1 and o.created_at>='2024-01-01' group by u.id order by total desc limit 20";
            case POSTGRESQL -> "with recent_orders as (select user_id,total from orders where created_at >= now() - interval '7 day') select u.id,u.name,sum(r.total) as total from \"users\" u join recent_orders r on u.id=r.user_id group by u.id,u.name returning u.id";
            case SQLSERVER -> "select top 20 u.id,u.name,o.total from [users] u left join [orders] o on u.id=o.user_id where u.status=1 order by o.total desc";
            case ORACLE -> "select u.id,u.name,o.total from users u left join orders o on u.id=o.user_id where u.status=1 and o.created_at >= to_date('2024-01-01','yyyy-mm-dd') order by o.total desc";
            case GENERIC -> "select u.id,u.name,u.email,u.created_at,o.order_id,o.total,o.status from users u left join orders o on u.id=o.user_id where u.status=1 and u.created_at>='2024-01-01' and (o.total>100 or o.status='paid') group by u.id having count(o.order_id)>0 order by u.created_at desc,o.total desc limit 100";
        };
        inputArea.setText(sampleSql);
        outputArea.setText("");
        updateStatus("✅ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_SAMPLE_LOADED), true);
    }

    private SqlFormatter.SqlDialect getSelectedDialect() {
        Object selected = dialectComboBox.getSelectedItem();
        return selected instanceof SqlFormatter.SqlDialect dialect ? dialect : SqlFormatter.SqlDialect.GENERIC;
    }

    private String getDialectDisplayName(SqlFormatter.SqlDialect dialect) {
        return switch (dialect) {
            case MYSQL -> I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_DIALECT_MYSQL);
            case POSTGRESQL -> I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_DIALECT_POSTGRESQL);
            case SQLSERVER -> I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_DIALECT_SQLSERVER);
            case ORACLE -> I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_DIALECT_ORACLE);
            case GENERIC -> I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_DIALECT_GENERIC);
        };
    }

    private int countLines(String text) {
        return text.isEmpty() ? 0 : text.split("\n", -1).length;
    }

    private void swapInputOutput() {
        String inputText = inputArea.getText();
        String outputText = outputArea.getText();
        inputArea.setText(outputText);
        outputArea.setText(inputText);
        updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_SWAPPED), true);
    }

    private void pasteFromClipboard() {
        try {
            String text = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            if (text != null && !text.isEmpty()) {
                inputArea.setText(text);
                outputArea.setText("");
                updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_PASTED), true);
            }
        } catch (Exception ex) {
            log.error("Paste error", ex);
            updateStatus("❌ " + ex.getMessage(), false);
        }
    }

    private void copyToClipboard() {
        String text = outputArea.getText();
        if (!text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_COPIED), true);
        } else {
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_OUTPUT_EMPTY), false);
        }
    }

    private void clearAll() {
        inputArea.setText("");
        outputArea.setText("");
        updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_CLEARED), true);
    }

    private void updateStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.setForeground(success ? new Color(0, 128, 0) : new Color(180, 0, 0));

        Timer timer = new Timer(3000, e -> statusLabel.setText(" "));
        timer.setRepeats(false);
        timer.start();
    }
}
