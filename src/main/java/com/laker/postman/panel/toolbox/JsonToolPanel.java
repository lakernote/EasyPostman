package com.laker.postman.panel.toolbox;

import cn.hutool.json.JSONUtil;
import com.laker.postman.util.EditorThemeUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.JsonUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON工具面板 - 使用RSyntaxTextArea提供语法高亮和更好的编辑体验
 */
@Slf4j
public class JsonToolPanel extends JPanel {

    private RSyntaxTextArea inputArea;
    private RSyntaxTextArea outputArea;
    private JLabel statusLabel;

    public JsonToolPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部工具栏
        JPanel topPanel = new JPanel(new BorderLayout());

        // 左侧按钮组
        JPanel leftBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JButton formatBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_FORMAT));
        JButton compressBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_COMPRESS));
        JButton validateBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_VALIDATE));
        JButton escapeBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_ESCAPE));
        JButton unescapeBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_UNESCAPE));
        JButton sortBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_SORT_KEYS));

        formatBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_TOOLTIP_FORMAT));
        compressBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_TOOLTIP_COMPRESS));
        validateBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_TOOLTIP_VALIDATE));
        escapeBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_TOOLTIP_ESCAPE));
        unescapeBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_TOOLTIP_UNESCAPE));
        sortBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_TOOLTIP_SORT));

        leftBtnPanel.add(formatBtn);
        leftBtnPanel.add(compressBtn);
        leftBtnPanel.add(validateBtn);
        leftBtnPanel.add(new JSeparator(SwingConstants.VERTICAL));
        leftBtnPanel.add(escapeBtn);
        leftBtnPanel.add(unescapeBtn);
        leftBtnPanel.add(sortBtn);

        // 右侧按钮组
        JPanel rightBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));

        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        JButton pasteBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_PASTE));
        JButton clearBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));
        JButton swapBtn = new JButton("↕ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_SWAP));

        copyBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_TOOLTIP_COPY));
        pasteBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_TOOLTIP_PASTE));
        clearBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_TOOLTIP_CLEAR));
        swapBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_TOOLTIP_SWAP));

        rightBtnPanel.add(copyBtn);
        rightBtnPanel.add(pasteBtn);
        rightBtnPanel.add(clearBtn);
        rightBtnPanel.add(swapBtn);

        topPanel.add(leftBtnPanel, BorderLayout.WEST);
        topPanel.add(rightBtnPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // 中间分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // 输入区域 - 使用RSyntaxTextArea
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        JLabel inputLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_INPUT));
        inputPanel.add(inputLabel, BorderLayout.NORTH);

        inputArea = createJsonTextArea();
        inputArea.setEditable(true);
        RTextScrollPane inputScrollPane = new RTextScrollPane(inputArea);
        inputScrollPane.setLineNumbersEnabled(true); // 显示行号
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);

        // 输出区域 - 使用RSyntaxTextArea
        JPanel outputPanel = new JPanel(new BorderLayout(5, 5));
        JLabel outputLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_OUTPUT));
        outputPanel.add(outputLabel, BorderLayout.NORTH);

        outputArea = createJsonTextArea();
        outputArea.setEditable(false);
        RTextScrollPane outputScrollPane = new RTextScrollPane(outputArea);
        outputScrollPane.setLineNumbersEnabled(true); // 显示行号
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);

        splitPane.setTopComponent(inputPanel);
        splitPane.setBottomComponent(outputPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.5); // 平均分配空间

        add(splitPane, BorderLayout.CENTER);

        // 底部状态栏
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        statusLabel = new JLabel(" ");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);

        // 按钮事件
        formatBtn.addActionListener(e -> formatJson());
        compressBtn.addActionListener(e -> compressJson());
        validateBtn.addActionListener(e -> validateJson());
        escapeBtn.addActionListener(e -> escapeJson());
        unescapeBtn.addActionListener(e -> unescapeJson());
        sortBtn.addActionListener(e -> sortJsonKeys());
        copyBtn.addActionListener(e -> copyToClipboard());
        pasteBtn.addActionListener(e -> pasteFromClipboard());
        clearBtn.addActionListener(e -> clearAll());
        swapBtn.addActionListener(e -> swapInputOutput());

        // 添加快捷键
        setupKeyBindings();
    }

    /**
     * 创建配置好的JSON文本编辑区域
     */
    private RSyntaxTextArea createJsonTextArea() {
        RSyntaxTextArea textArea = new RSyntaxTextArea(10, 40);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        textArea.setCodeFoldingEnabled(true); // 启用代码折叠
        textArea.setAntiAliasingEnabled(true); // 抗锯齿
        textArea.setAutoIndentEnabled(true); // 自动缩进
        textArea.setTabSize(2); // Tab大小为2个空格
        textArea.setTabsEmulated(true); // 用空格模拟Tab
        textArea.setMarkOccurrences(true); // 标记相同内容
        textArea.setPaintTabLines(true); // 显示缩进线
        textArea.setAnimateBracketMatching(true); // 括号匹配动画
        EditorThemeUtil.loadTheme(textArea);
        return textArea;
    }

    /**
     * 设置快捷键
     */
    private void setupKeyBindings() {
        // Ctrl+Shift+F - Format
        addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                "format", e -> formatJson());

        // Ctrl+Shift+C - Compress
        addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                "compress", e -> compressJson());

        // Ctrl+Shift+V - Validate
        addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                "validate", e -> validateJson());

        // Ctrl+L - Clear
        addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK),
                "clear", e -> clearAll());
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

    /**
     * 格式化JSON
     */
    private void formatJson() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            outputArea.setText("");
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_EMPTY), false);
            return;
        }

        try {
            String formatted = JsonUtil.toJsonPrettyStr(input);
            outputArea.setText(formatted);
            int lines = formatted.split("\n").length;
            String message = I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_FORMATTED,
                    String.valueOf(lines), String.valueOf(formatted.length()));
            updateStatus(message, true);
        } catch (Exception ex) {
            log.error("JSON format error", ex);
            outputArea.setText("❌ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_ERROR) + ":\n\n" + ex.getMessage());
            updateStatus("❌ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_ERROR) + ": " + ex.getMessage(), false);
        }
    }

    /**
     * 压缩JSON
     */
    private void compressJson() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            outputArea.setText("");
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_EMPTY), false);
            return;
        }

        try {
            String compressed = JSONUtil.toJsonStr(JSONUtil.parse(input));
            outputArea.setText(compressed);
            int reduction = input.length() - compressed.length();
            double percent = (reduction * 100.0) / input.length();
            String message = I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_COMPRESSED,
                    String.valueOf(reduction), String.format("%.1f", percent));
            updateStatus(message, true);
        } catch (Exception ex) {
            log.error("JSON compress error", ex);
            outputArea.setText("❌ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_ERROR) + ":\n\n" + ex.getMessage());
            updateStatus("❌ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_ERROR) + ": " + ex.getMessage(), false);
        }
    }

    /**
     * 验证JSON
     */
    private void validateJson() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            outputArea.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_VALIDATION_EMPTY));
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_EMPTY), false);
            return;
        }

        try {
            Object parsed = JSONUtil.parse(input);
            String type = parsed.getClass().getSimpleName();
            int lines = input.split("\n").length;
            int chars = input.length();

            // 统计JSON结构信息
            StringBuilder info = new StringBuilder();
            info.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_VALIDATION_VALID)).append("\n\n");
            info.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_VALIDATION_TYPE)).append(": ").append(type).append("\n");
            info.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_VALIDATION_CHARACTERS)).append(": ").append(chars).append("\n");
            info.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_VALIDATION_LINES)).append(": ").append(lines).append("\n");

            // 尝试统计键值对数量
            if (parsed instanceof cn.hutool.json.JSONObject obj) {
                info.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_VALIDATION_KEYS)).append(": ").append(obj.size()).append("\n");
            } else if (parsed instanceof cn.hutool.json.JSONArray arr) {
                info.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_VALIDATION_ARRAY_LENGTH)).append(": ").append(arr.size()).append("\n");
            }

            outputArea.setText(info.toString());
            String message = I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_VALIDATED, type);
            updateStatus(message, true);
        } catch (Exception ex) {
            log.error("JSON validate error", ex);
            String errorMsg = "❌ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_ERROR) + ":\n\n" + ex.getMessage();
            outputArea.setText(errorMsg);
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_INVALID), false);
        }
    }

    /**
     * Escape JSON字符串（转义特殊字符）
     */
    private void escapeJson() {
        String input = inputArea.getText();
        if (input.isEmpty()) {
            outputArea.setText("");
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_EMPTY), false);
            return;
        }

        try {
            // 转义引号、反斜杠、换行符等
            String escaped = input
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            outputArea.setText(escaped);
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_ESCAPED), true);
        } catch (Exception ex) {
            log.error("Escape error", ex);
            updateStatus("❌ " + ex.getMessage(), false);
        }
    }

    /**
     * Unescape JSON字符串（反转义）
     */
    private void unescapeJson() {
        String input = inputArea.getText();
        if (input.isEmpty()) {
            outputArea.setText("");
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_EMPTY), false);
            return;
        }

        try {
            // 反转义
            String unescaped = input
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");

            // 处理Unicode转义
            Pattern pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
            Matcher matcher = pattern.matcher(unescaped);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String unicode = matcher.group(1);
                char ch = (char) Integer.parseInt(unicode, 16);
                matcher.appendReplacement(sb, String.valueOf(ch));
            }
            matcher.appendTail(sb);

            outputArea.setText(sb.toString());
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_UNESCAPED), true);
        } catch (Exception ex) {
            log.error("Unescape error", ex);
            updateStatus("❌ " + ex.getMessage(), false);
        }
    }

    /**
     * 排序JSON的键
     */
    private void sortJsonKeys() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            outputArea.setText("");
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_EMPTY), false);
            return;
        }

        try {
            Object parsed = JSONUtil.parse(input);
            // 使用有序的JSONObject来保持键的排序
            if (parsed instanceof cn.hutool.json.JSONObject obj) {
                String sorted = JsonUtil.toJsonPrettyStr(sortJsonObject(obj));
                outputArea.setText(sorted);
                updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_SORTED), true);
            } else {
                outputArea.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_NOT_OBJECT) + "\n\n" +
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_VALIDATION_TYPE) + ": " + parsed.getClass().getSimpleName());
                updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_NOT_OBJECT), false);
            }
        } catch (Exception ex) {
            log.error("Sort keys error", ex);
            outputArea.setText("❌ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_ERROR) + ":\n\n" + ex.getMessage());
            updateStatus("❌ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_ERROR) + ": " + ex.getMessage(), false);
        }
    }

    /**
     * 递归排序JSON对象的键
     */
    private cn.hutool.json.JSONObject sortJsonObject(cn.hutool.json.JSONObject obj) {
        cn.hutool.json.JSONObject sorted = new cn.hutool.json.JSONObject(true); // true表示使用LinkedHashMap保持顺序
        obj.keySet().stream().sorted().forEach(key -> {
            Object value = obj.get(key);
            if (value instanceof cn.hutool.json.JSONObject) {
                sorted.set(key, sortJsonObject((cn.hutool.json.JSONObject) value));
            } else if (value instanceof cn.hutool.json.JSONArray) {
                sorted.set(key, sortJsonArray((cn.hutool.json.JSONArray) value));
            } else {
                sorted.set(key, value);
            }
        });
        return sorted;
    }

    /**
     * 递归处理JSON数组中的对象
     */
    private cn.hutool.json.JSONArray sortJsonArray(cn.hutool.json.JSONArray arr) {
        cn.hutool.json.JSONArray sorted = new cn.hutool.json.JSONArray();
        for (Object item : arr) {
            if (item instanceof cn.hutool.json.JSONObject) {
                sorted.add(sortJsonObject((cn.hutool.json.JSONObject) item));
            } else if (item instanceof cn.hutool.json.JSONArray) {
                sorted.add(sortJsonArray((cn.hutool.json.JSONArray) item));
            } else {
                sorted.add(item);
            }
        }
        return sorted;
    }

    /**
     * 交换输入和输出区域的内容
     */
    private void swapInputOutput() {
        String inputText = inputArea.getText();
        String outputText = outputArea.getText();
        inputArea.setText(outputText);
        outputArea.setText(inputText);
        updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_SWAPPED), true);
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
                updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_PASTED), true);
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
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_COPIED), true);
        } else {
            updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_OUTPUT_EMPTY), false);
        }
    }

    /**
     * 清空所有区域
     */
    private void clearAll() {
        inputArea.setText("");
        outputArea.setText("");
        updateStatus(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON_STATUS_CLEARED), true);
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
