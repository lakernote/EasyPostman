package com.laker.postman.panel.toolbox;

import com.laker.postman.util.EditorThemeUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.SqlFormatter;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL工具面板 - 使用专业的 SqlFormatter 提供 SQL 格式化和压缩功能
 * 参考 Druid SQLFormatter 实现
 */
@Slf4j
public class SqlToolPanel extends JPanel {

    private RSyntaxTextArea inputArea;
    private RSyntaxTextArea outputArea;
    private JLabel statusLabel;

    // 格式化选项
    private JSpinner indentSpinner;
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

        // 顶部工具栏
        JPanel topPanel = new JPanel(new BorderLayout());

        // 左侧按钮组
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

        // 右侧按钮组
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

        add(topPanel, BorderLayout.NORTH);

        // 中间主内容区域（包含选项面板和编辑器）
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));

        // 格式化选项面板
        JPanel optionsPanel = createOptionsPanel();
        mainPanel.add(optionsPanel, BorderLayout.NORTH);

        // 中间分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // 输入区域
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        JLabel inputLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_INPUT));
        inputPanel.add(inputLabel, BorderLayout.NORTH);

        inputArea = createSqlTextArea();
        inputArea.setEditable(true);
        RTextScrollPane inputScrollPane = new RTextScrollPane(inputArea);
        inputScrollPane.setLineNumbersEnabled(true);
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);

        // 输出区域
        JPanel outputPanel = new JPanel(new BorderLayout(5, 5));

        // 输出标题栏（包含标签和信息）
        JPanel outputHeaderPanel = new JPanel(new BorderLayout());
        JLabel outputLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_OUTPUT));

        // 输出信息标签（显示行数和字符数）
        JLabel outputInfoLabel = new JLabel(" ");
        outputInfoLabel.setFont(outputInfoLabel.getFont().deriveFont(Font.PLAIN, 10f));
        outputInfoLabel.setForeground(Color.GRAY);

        outputHeaderPanel.add(outputLabel, BorderLayout.WEST);
        outputHeaderPanel.add(outputInfoLabel, BorderLayout.EAST);
        outputPanel.add(outputHeaderPanel, BorderLayout.NORTH);

        outputArea = createSqlTextArea();
        outputArea.setEditable(false);

        // 添加文档监听器，实时更新输出统计信息
        outputArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateOutputInfo(outputInfoLabel); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateOutputInfo(outputInfoLabel); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateOutputInfo(outputInfoLabel); }
        });

        RTextScrollPane outputScrollPane = new RTextScrollPane(outputArea);
        outputScrollPane.setLineNumbersEnabled(true);
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);

        splitPane.setTopComponent(inputPanel);
        splitPane.setBottomComponent(outputPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.5);

        mainPanel.add(splitPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        // 底部状态栏
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        statusLabel = new JLabel(" ");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);

        // 按钮事件
        formatBtn.addActionListener(e -> formatSql());
        compressBtn.addActionListener(e -> compressSql());
        validateBtn.addActionListener(e -> validateSql());
        sampleBtn.addActionListener(e -> loadSampleSql());
        copyBtn.addActionListener(e -> copyToClipboard());
        pasteBtn.addActionListener(e -> pasteFromClipboard());
        clearBtn.addActionListener(e -> clearAll());
        swapBtn.addActionListener(e -> swapInputOutput());
    }

    /**
     * 创建格式化选项面板
     */
    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_OPTIONS)));


        // 缩进大小
        panel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_INDENT) + ":"));
        indentSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 8, 1));
        indentSpinner.setPreferredSize(new Dimension(60, 25));
        panel.add(indentSpinner);

        // 关键字大写
        uppercaseKeywordsCheck = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_UPPERCASE_KEYWORDS), true);
        panel.add(uppercaseKeywordsCheck);

        // 添加分号
        addSemicolonCheck = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_ADD_SEMICOLON), true);
        panel.add(addSemicolonCheck);

        // AND/OR 换行
        lineBreakAndCheck = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_LINE_BREAK_AND_OR), true);
        panel.add(lineBreakAndCheck);

        // 逗号后换行
        lineBreakCommaCheck = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_LINE_BREAK_COMMA), true);
        panel.add(lineBreakCommaCheck);

        return panel;
    }

    /**
     * 创建配置好的SQL文本编辑区域
     */
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


    /**
     * 标准格式化SQL
     */
    private void formatSql() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            outputArea.setText("");
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_EMPTY), false);
            return;
        }

        try {
            SqlFormatter.FormatOption option = createFormatOption();
            String formatted = SqlFormatter.format(input, option);
            outputArea.setText(formatted);
            int lines = formatted.split("\n").length;
            String message = I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_FORMATTED,
                    String.valueOf(lines), String.valueOf(formatted.length()));
            updateStatus(message, true);
        } catch (Exception ex) {
            log.error("SQL format error", ex);
            handleFormatError(ex);
        }
    }

    /**
     * 压缩SQL
     */
    private void compressSql() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            outputArea.setText("");
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_EMPTY), false);
            return;
        }

        try {
            String compressed = SqlFormatter.compress(input);
            outputArea.setText(compressed);
            int reduction = input.length() - compressed.length();
            double percent = reduction > 0 ? (reduction * 100.0) / input.length() : 0;
            String message = I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_COMPRESSED,
                    String.valueOf(Math.max(0, reduction)), String.format("%.1f", percent));
            updateStatus(message, true);
        } catch (Exception ex) {
            log.error("SQL compress error", ex);
            handleFormatError(ex);
        }
    }

    /**
     * 创建格式化选项
     */
    private SqlFormatter.FormatOption createFormatOption() {
        int indentSize = (Integer) indentSpinner.getValue();
        boolean uppercaseKeywords = uppercaseKeywordsCheck.isSelected();
        boolean addSemicolon = addSemicolonCheck.isSelected();
        boolean lineBreakAnd = lineBreakAndCheck.isSelected();
        boolean lineBreakComma = lineBreakCommaCheck.isSelected();

        return new SqlFormatter.FormatOption()
                .setIndent(indentSize)
                .setUppercaseKeywords(uppercaseKeywords)
                .setAddSemicolon(addSemicolon)
                .setLineBreakBeforeAnd(lineBreakAnd)
                .setLineBreakBeforeOr(lineBreakAnd)
                .setLineBreakAfterComma(lineBreakComma);
    }

    /**
     * 处理格式化错误
     */
    private void handleFormatError(Exception ex) {
        String errorMsg = "❌ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_ERROR) + ":\n\n" + ex.getMessage();
        outputArea.setText(errorMsg);
        updateStatus("❌ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_ERROR) + ": " + ex.getMessage(), false);
    }

    /**
     * 验证SQL - 基本的语法检查
     */
    private void validateSql() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            outputArea.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EMPTY));
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_EMPTY), false);
            return;
        }

        try {
            List<String> issues = new ArrayList<>();

            // 基本语法检查
            int chars = input.length();
            int lines = input.split("\n").length;
            int statements = input.split(";").length;

            // 检查括号匹配
            int openParen = countOccurrences(input, '(');
            int closeParen = countOccurrences(input, ')');
            if (openParen != closeParen) {
                issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_PAREN_MISMATCH,
                        String.valueOf(openParen), String.valueOf(closeParen)));
            }

            // 检查引号匹配
            int singleQuotes = countOccurrences(input, '\'');
            if (singleQuotes % 2 != 0) {
                issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_QUOTE_MISMATCH));
            }

            // 检查常见SQL关键字
            boolean hasSelect = containsIgnoreCase(input, "SELECT");
            boolean hasInsert = containsIgnoreCase(input, "INSERT");
            boolean hasUpdate = containsIgnoreCase(input, "UPDATE");
            boolean hasDelete = containsIgnoreCase(input, "DELETE");
            boolean hasCreate = containsIgnoreCase(input, "CREATE");

            boolean hasValidKeyword = hasSelect || hasInsert || hasUpdate || hasDelete || hasCreate;

            StringBuilder info = new StringBuilder();
            if (issues.isEmpty() && hasValidKeyword) {
                info.append("✓ ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_VALID)).append("\n\n");
            } else if (!hasValidKeyword) {
                info.append("⚠ ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_NO_KEYWORDS)).append("\n\n");
            } else {
                info.append("❌ ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_ISSUES)).append("\n\n");
            }

            info.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_CHARACTERS)).append(": ").append(chars).append("\n");
            info.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_LINES)).append(": ").append(lines).append("\n");
            info.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_STATEMENTS)).append(": ").append(statements).append("\n");

            if (!issues.isEmpty()) {
                info.append("\n").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_FOUND_ISSUES)).append("\n");
                for (int i = 0; i < issues.size(); i++) {
                    info.append(i + 1).append(". ").append(issues.get(i)).append("\n");
                }
            }

            outputArea.setText(info.toString());
            updateStatus(issues.isEmpty() ?
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_VALIDATED) :
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_INVALID), issues.isEmpty());
        } catch (Exception ex) {
            log.error("SQL validate error", ex);
            outputArea.setText("❌ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_ERROR) + ":\n\n" + ex.getMessage());
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_INVALID), false);
        }
    }


    /**
     * 加载示例 SQL
     */
    private void loadSampleSql() {
        String sampleSql =
            "select u.id,u.name,u.email,u.created_at,o.order_id,o.total,o.status " +
            "from users u " +
            "left join orders o on u.id=o.user_id " +
            "where u.status=1 and u.created_at>='2024-01-01' and (o.total>100 or o.status='paid') " +
            "group by u.id " +
            "having count(o.order_id)>0 " +
            "order by u.created_at desc,o.total desc " +
            "limit 100";

        inputArea.setText(sampleSql);
        updateStatus("✅ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_SAMPLE_LOADED), true);
    }

    /**
     * 更新输出区域信息
     */
    private void updateOutputInfo(JLabel infoLabel) {
        String text = outputArea.getText();
        if (text.isEmpty()) {
            infoLabel.setText(" ");
            return;
        }

        int lines = text.split("\n").length;
        int chars = text.length();
        String format = I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_OUTPUT_INFO_FORMAT,
                String.valueOf(lines), String.valueOf(chars));
        infoLabel.setText("  " + format);
    }

    /**
     * 统计字符出现次数
     */
    private int countOccurrences(String str, char ch) {
        return (int) str.chars().filter(c -> c == ch).count();
    }

    /**
     * 忽略大小写检查包含
     */
    private boolean containsIgnoreCase(String str, String search) {
        return str.toLowerCase().contains(search.toLowerCase());
    }

    /**
     * 交换输入和输出区域的内容
     */
    private void swapInputOutput() {
        String inputText = inputArea.getText();
        String outputText = outputArea.getText();
        inputArea.setText(outputText);
        outputArea.setText(inputText);
        updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_SWAPPED), true);
    }

    /**
     * 从剪贴板粘贴
     */
    private void pasteFromClipboard() {
        try {
            String text = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            if (text != null && !text.isEmpty()) {
                inputArea.setText(text);
                updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_PASTED), true);
            }
        } catch (Exception ex) {
            log.error("Paste error", ex);
            updateStatus("❌ " + ex.getMessage(), false);
        }
    }

    /**
     * 复制到剪贴板
     */
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

    /**
     * 清空所有区域
     */
    private void clearAll() {
        inputArea.setText("");
        outputArea.setText("");
        updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_STATUS_CLEARED), true);
    }

    /**
     * 更新状态栏
     */
    private void updateStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.setForeground(success ? new Color(0, 128, 0) : new Color(180, 0, 0));

        // 3秒后清除状态
        Timer timer = new Timer(3000, e -> statusLabel.setText(" "));
        timer.setRepeats(false);
        timer.start();
    }
}

