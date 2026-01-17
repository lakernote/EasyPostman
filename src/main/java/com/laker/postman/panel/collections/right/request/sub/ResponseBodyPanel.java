package com.laker.postman.panel.collections.right.request.sub;

import cn.hutool.core.util.XmlUtil;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.button.*;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.*;
import lombok.Getter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 响应体面板，展示 HTTP 响应体内容
 * <p>
 * 主要功能：
 * - 语法高亮显示（JSON、XML、HTML、JavaScript、CSS 等）
 * - 自动/手动格式化
 * - 文本搜索
 * - 自动换行控制
 * - 下载响应内容
 * - 大文件优化处理
 * </p>
 */
public class ResponseBodyPanel extends JPanel {
    @Getter
    private final RSyntaxTextArea responseBodyPane;
    private final DownloadButton downloadButton;
    @Getter
    private SaveResponseButton saveResponseButton; // 保存响应按钮（仅HTTP请求）
    private String currentFilePath;
    private String fileName = DEFAULT_FILE_NAME; // 默认下载文件名
    private final SearchTextField searchField;
    private Map<String, List<String>> lastHeaders;
    private final EasyComboBox<String> syntaxComboBox;
    private final FormatButton formatButton;
    private final PreviousButton prevButton;
    private final NextButton nextButton;
    private final WrapToggleButton wrapButton;
    RTextScrollPane scrollPane;

    // 常量定义
    private static final int LARGE_RESPONSE_THRESHOLD = 500 * 1024; // 500KB threshold
    private static final int MAX_AUTO_FORMAT_SIZE = 1024 * 1024; // 1MB max for auto-format
    private static final int BUFFER_SIZE = 8192;
    private static final String DEFAULT_FILE_NAME = "downloaded_file";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String SKIP_AUTO_FORMAT_MESSAGE = " Skip auto-format for large response.";


    private final JLabel sizeWarningLabel;

    public ResponseBodyPanel(boolean enableSaveButton) {
        setLayout(new BorderLayout());
        responseBodyPane = new RSyntaxTextArea();
        responseBodyPane.setEditable(false);
        responseBodyPane.setCodeFoldingEnabled(true);
        responseBodyPane.setLineWrap(false); // 禁用自动换行以提升大文本性能
        responseBodyPane.setHighlightCurrentLine(false); // 关闭选中行高亮

        // 设置字体 - 使用用户设置的字体大小
        updateEditorFont();

        // 加载编辑器主题 - 支持亮色和暗色主题自适应
        EditorThemeUtil.loadTheme(responseBodyPane);

        scrollPane = new RTextScrollPane(responseBodyPane);
        scrollPane.setLineNumbersEnabled(true); // 显示行号
        add(scrollPane, BorderLayout.CENTER);

        JPanel toolBarPanel = new JPanel();
        toolBarPanel.setLayout(new BoxLayout(toolBarPanel, BoxLayout.X_AXIS));
        toolBarPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        // 语法选择下拉框
        syntaxComboBox = new EasyComboBox<>(SyntaxType.getDisplayNames(), EasyComboBox.WidthMode.DYNAMIC);
        syntaxComboBox.setFocusable(false);
        toolBarPanel.add(syntaxComboBox);
        toolBarPanel.add(Box.createHorizontalStrut(4)); // 间隔

        // 添加大小提示标签
        sizeWarningLabel = new JLabel();
        sizeWarningLabel.setForeground(new Color(200, 100, 0));
        sizeWarningLabel.setVisible(false);
        toolBarPanel.add(sizeWarningLabel);

        // 弹性空间，将右侧控件推到右边
        toolBarPanel.add(Box.createHorizontalGlue());

        // 搜索框
        searchField = new SearchTextField();
        toolBarPanel.add(searchField);
        toolBarPanel.add(Box.createHorizontalStrut(4));

        // 搜索导航按钮
        prevButton = new PreviousButton();
        toolBarPanel.add(prevButton);
        toolBarPanel.add(Box.createHorizontalStrut(4));

        nextButton = new NextButton();
        toolBarPanel.add(nextButton);
        toolBarPanel.add(Box.createHorizontalStrut(4));

        // 换行按钮
        wrapButton = new WrapToggleButton();
        toolBarPanel.add(wrapButton);
        toolBarPanel.add(Box.createHorizontalStrut(4));

        // 格式化按钮
        formatButton = new FormatButton();
        toolBarPanel.add(formatButton);
        toolBarPanel.add(Box.createHorizontalStrut(4));

        // 下载按钮
        downloadButton = new DownloadButton();
        toolBarPanel.add(downloadButton);

        // 只有 HTTP 请求才显示保存响应按钮
        if (enableSaveButton) {
            toolBarPanel.add(Box.createHorizontalStrut(4));
            saveResponseButton = new SaveResponseButton();
            toolBarPanel.add(saveResponseButton);
        }

        add(toolBarPanel, BorderLayout.NORTH);

        downloadButton.addActionListener(e -> saveFile());
        formatButton.addActionListener(e -> formatContent());
        wrapButton.addActionListener(e -> toggleLineWrap());
        searchField.addActionListener(e -> search(true));
        prevButton.addActionListener(e -> search(false));
        nextButton.addActionListener(e -> search(true));
        syntaxComboBox.addActionListener(e -> onSyntaxComboChanged());

        // 监听搜索选项变化，触发重新搜索
        searchField.addPropertyChangeListener("caseSensitive", evt -> {
            if (!searchField.getText().isEmpty()) {
                search(true);
            }
        });
        searchField.addPropertyChangeListener("wholeWord", evt -> {
            if (!searchField.getText().isEmpty()) {
                search(true);
            }
        });
    }

    /**
     * 语法类型下拉框改变事件处理
     * 根据用户选择的语法类型更新编辑器的语法高亮
     */
    private void onSyntaxComboChanged() {
        int idx = syntaxComboBox.getSelectedIndex();
        SyntaxType syntaxType = SyntaxType.getByIndex(idx);

        String syntax;
        if (syntaxType == SyntaxType.AUTO_DETECT) {
            // 自动检测语法类型
            syntax = detectSyntax(responseBodyPane.getText(), getCurrentContentTypeFromHeaders());
        } else {
            // 使用用户选择的语法类型
            syntax = syntaxType.getSyntaxStyle();
        }

        responseBodyPane.setSyntaxEditingStyle(syntax);
    }

    /**
     * 切换自动换行状态
     */
    private void toggleLineWrap() {
        boolean isWrapEnabled = wrapButton.isSelected();
        responseBodyPane.setLineWrap(isWrapEnabled);
    }

    /**
     * 搜索关键字，支持大小写敏感和整词匹配
     *
     * @param forward true 表示向前搜索，false 表示向后搜索
     */
    private void search(boolean forward) {
        String keyword = searchField.getText();
        if (keyword == null || keyword.isEmpty()) {
            return;
        }

        String text = responseBodyPane.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        // 获取搜索选项
        boolean caseSensitive = searchField.isCaseSensitive();
        boolean wholeWord = searchField.isWholeWord();

        int caret = responseBodyPane.getCaretPosition();
        int pos;

        if (forward) {
            // 向后查找
            int start = caret;
            if (responseBodyPane.getSelectedText() != null) {
                start = responseBodyPane.getSelectionEnd();
            }
            pos = findNext(text, keyword, start, caseSensitive, wholeWord);
            if (pos == -1) {
                // 循环查找：从头开始
                pos = findNext(text, keyword, 0, caseSensitive, wholeWord);
            }
        } else {
            // 向前查找
            int start = caret;
            if (responseBodyPane.getSelectedText() != null) {
                start = responseBodyPane.getSelectionStart() - 1;
            }
            pos = findPrevious(text, keyword, start, caseSensitive, wholeWord);
            if (pos == -1) {
                // 循环查找：从末尾开始
                pos = findPrevious(text, keyword, text.length(), caseSensitive, wholeWord);
            }
        }

        if (pos != -1) {
            selectAndFocusText(pos, keyword.length());
        }
    }

    /**
     * 向后查找匹配
     */
    private int findNext(String text, String keyword, int fromIndex, boolean caseSensitive, boolean wholeWord) {
        String searchText = caseSensitive ? text : text.toLowerCase();
        String searchKeyword = caseSensitive ? keyword : keyword.toLowerCase();

        int pos = fromIndex;
        while ((pos = searchText.indexOf(searchKeyword, pos)) != -1) {
            if (!wholeWord || isWholeWord(text, pos, keyword.length())) {
                return pos;
            }
            pos++;
        }
        return -1;
    }

    /**
     * 向前查找匹配
     */
    private int findPrevious(String text, String keyword, int fromIndex, boolean caseSensitive, boolean wholeWord) {
        if (fromIndex > text.length()) {
            fromIndex = text.length();
        }

        String searchText = caseSensitive ? text : text.toLowerCase();
        String searchKeyword = caseSensitive ? keyword : keyword.toLowerCase();

        int pos = fromIndex;
        while ((pos = searchText.lastIndexOf(searchKeyword, pos)) != -1) {
            if (!wholeWord || isWholeWord(text, pos, keyword.length())) {
                return pos;
            }
            pos--;
            if (pos < 0) break;
        }
        return -1;
    }

    /**
     * 判断是否为整词匹配
     */
    private boolean isWholeWord(String text, int start, int length) {
        int end = start + length;

        // 检查前一个字符
        if (start > 0) {
            char prevChar = text.charAt(start - 1);
            if (Character.isLetterOrDigit(prevChar) || prevChar == '_') {
                return false;
            }
        }

        // 检查后一个字符
        if (end < text.length()) {
            char nextChar = text.charAt(end);
            return !Character.isLetterOrDigit(nextChar) && nextChar != '_';
        }

        return true;
    }

    /**
     * 选中并聚焦到指定位置的文本
     *
     * @param pos    起始位置
     * @param length 长度
     */
    private void selectAndFocusText(int pos, int length) {
        responseBodyPane.setCaretPosition(pos);
        responseBodyPane.select(pos, pos + length);
        responseBodyPane.requestFocusInWindow();
    }

    /**
     * 保存文件
     * <p>
     * 支持两种保存模式：
     * 1. 如果有临时文件路径（大文件或二进制文件），从临时文件复制
     * 2. 否则直接保存编辑器中的文本内容
     * </p>
     */
    private void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save File");

        // 智能设置默认文件名和扩展名
        String defaultFileName = generateFileName();
        fileChooser.setSelectedFile(new File(defaultFileName));

        int userSelection = fileChooser.showSaveDialog(SingletonFactory.getInstance(MainFrame.class));
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File destFile = fileChooser.getSelectedFile();
            try {
                if (currentFilePath != null && !currentFilePath.isEmpty()) {
                    // 如果是文件下载（如二进制文件），从临时文件复制
                    try (InputStream in = new FileInputStream(currentFilePath);
                         OutputStream out = new FileOutputStream(destFile)) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int len;
                        while ((len = in.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                    }
                } else {
                    // 如果是文本响应，直接保存文本内容
                    String content = responseBodyPane.getText();
                    if (content != null && !content.isEmpty()) {
                        try (OutputStreamWriter writer = new OutputStreamWriter(
                                new FileOutputStream(destFile), StandardCharsets.UTF_8)) {
                            writer.write(content);
                        }
                    }
                }
                NotificationUtil.showInfo("File saved successfully: " + destFile.getAbsolutePath());
            } catch (Exception ex) {
                NotificationUtil.showError("Save File Error: " + ex.getMessage());
            }
        }
    }

    /**
     * 智能生成文件名，根据内容类型添加合适的扩展名
     */
    private String generateFileName() {
        // 如果有明确的文件名（来自 Content-Disposition），使用它
        if (fileName != null && !fileName.equals(DEFAULT_FILE_NAME) && !fileName.isEmpty()) {
            return fileName;
        }

        // 否则根据内容类型生成文件名
        String contentType = getCurrentContentTypeFromHeaders();
        String extension = FileExtensionUtil.guessExtension(contentType);

        // 使用统一的智能文件名生成逻辑
        if (extension == null) {
            extension = ".txt";
        }
        return FileExtensionUtil.generateSmartFileName(extension);
    }


    /**
     * 格式化内容
     * 根据 Content-Type 对 JSON 或 XML 进行格式化美化
     */
    private void formatContent() {
        String text = responseBodyPane.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        String contentType = getCurrentContentTypeFromHeaders();

        try {
            String formatted = null;
            if (contentType.contains("json")) {
                formatted = JsonUtil.toJsonPrettyStr(text);
            } else if (contentType.contains("xml")) {
                formatted = XmlUtil.format(text);
            }

            if (formatted != null) {
                responseBodyPane.setText(formatted);
                responseBodyPane.setCaretPosition(0);
            }
        } catch (Exception ex) {
            NotificationUtil.showError("Format Error: " + ex.getMessage());
        }
    }

    /**
     * 从响应头中获取 Content-Type
     *
     * @return Content-Type 的值，如果不存在则返回空字符串
     */
    private String getCurrentContentTypeFromHeaders() {
        if (lastHeaders != null) {
            for (Map.Entry<String, List<String>> entry : lastHeaders.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(CONTENT_TYPE_HEADER)) {
                    List<String> values = entry.getValue();
                    if (values != null && !values.isEmpty()) {
                        return values.get(0);
                    }
                }
            }
        }
        return "";
    }

    /**
     * 设置响应体内容
     * <p>
     * 该方法会：
     * 1. 自动检测语法类型并设置高亮
     * 2. 显示文件大小警告（如果超过阈值）
     * 3. 根据设置决定是否自动格式化
     * </p>
     *
     * @param resp HTTP 响应对象
     */
    public void setBodyText(HttpResponse resp) {
        if (resp == null) {
            clearResponseBody();
            return;
        }

        this.currentFilePath = resp.filePath;
        this.fileName = resp.fileName;
        this.lastHeaders = resp.headers;
        String text = resp.body;
        String contentType = extractContentType(resp.headers);

        int textSize = text != null ? text.getBytes().length : 0;
        boolean isLargeResponse = textSize > LARGE_RESPONSE_THRESHOLD;

        // 显示大小信息
        updateSizeWarning(textSize, isLargeResponse);

        // 动态选择高亮类型
        String syntax = detectSyntax(text, contentType);

        // 自动匹配下拉框选项
        int syntaxIndex = SyntaxType.getBySyntaxStyle(syntax).getIndex();
        syntaxComboBox.setSelectedIndex(syntaxIndex);

        // 设置语法高亮和文本
        responseBodyPane.setSyntaxEditingStyle(syntax);
        responseBodyPane.setText(text);

        // 根据设置和大小决定是否自动格式化
        if (SettingManager.isAutoFormatResponse() && textSize < MAX_AUTO_FORMAT_SIZE) {
            autoFormatIfPossible(text, contentType);
        } else if (SettingManager.isAutoFormatResponse() && textSize >= MAX_AUTO_FORMAT_SIZE) {
            // 大文件不自动格式化，提示用户手动格式化
            sizeWarningLabel.setText(sizeWarningLabel.getText() + SKIP_AUTO_FORMAT_MESSAGE);
        }

        responseBodyPane.setCaretPosition(0);
    }

    /**
     * 动态检测语法类型
     * <p>
     * 检测策略：
     * 1. 优先根据 Content-Type 响应头判断
     * 2. 其次根据内容特征判断（如 JSON 的 {} 或 []，XML 的 < >）
     * </p>
     *
     * @param text        文本内容
     * @param contentType Content-Type 响应头
     * @return 语法类型常量（来自 SyntaxConstants）
     */
    private String detectSyntax(String text, String contentType) {
        if (contentType != null) contentType = contentType.toLowerCase();
        if (contentType != null) {
            if (contentType.contains("json")) return SyntaxConstants.SYNTAX_STYLE_JSON;
            if (contentType.contains("xml")) return SyntaxConstants.SYNTAX_STYLE_XML;
            if (contentType.contains("html")) return SyntaxConstants.SYNTAX_STYLE_HTML;
            if (contentType.contains("javascript")) return SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;
            if (contentType.contains("css")) return SyntaxConstants.SYNTAX_STYLE_CSS;
            if (contentType.contains("text")) return SyntaxConstants.SYNTAX_STYLE_NONE;
        }
        // 内容自动识别
        if (text == null || text.isEmpty()) return SyntaxConstants.SYNTAX_STYLE_NONE;
        String t = text.trim();
        if ((t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"))) {
            return SyntaxConstants.SYNTAX_STYLE_JSON;
        }
        if (t.startsWith("<") && t.endsWith(">")) {
            if (t.toLowerCase().contains("<html")) return SyntaxConstants.SYNTAX_STYLE_HTML;
            if (t.toLowerCase().contains("<?xml")) return SyntaxConstants.SYNTAX_STYLE_XML;
        }
        return SyntaxConstants.SYNTAX_STYLE_NONE;
    }

    /**
     * 自动格式化内容（如果可能）
     * <p>
     * 只有在满足以下条件时才会自动格式化：
     * 1. 用户开启了自动格式化设置
     * 2. 文件大小小于阈值（500KB）
     * 3. 内容类型为 JSON 或 XML
     * </p>
     *
     * @param text        文本内容
     * @param contentType Content-Type 响应头
     */
    private void autoFormatIfPossible(String text, String contentType) {
        if (text == null || text.isEmpty()) {
            return;
        }

        int textSize = text.getBytes().length;

        // 小文件直接格式化
        if (textSize < LARGE_RESPONSE_THRESHOLD) {
            try {
                if (contentType != null && contentType.toLowerCase().contains("json")) {
                    String pretty = JsonUtil.toJsonPrettyStr(text);
                    responseBodyPane.setText(pretty);
                } else if (contentType != null && contentType.toLowerCase().contains("xml")) {
                    String pretty = XmlUtil.format(text);
                    responseBodyPane.setText(pretty);
                }
            } catch (Exception ex) {
                // 格式化失败时静默忽略，保持原始内容
            }
        }
        // 大文件不自动格式化
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        responseBodyPane.setEnabled(enabled);
        syntaxComboBox.setEnabled(enabled);
        searchField.setEnabled(enabled);
        downloadButton.setEnabled(enabled);
        scrollPane.setEnabled(enabled);

        if (formatButton != null) formatButton.setEnabled(enabled);
        if (prevButton != null) prevButton.setEnabled(enabled);
        if (nextButton != null) nextButton.setEnabled(enabled);
        if (wrapButton != null) wrapButton.setEnabled(enabled);
        if (saveResponseButton != null) saveResponseButton.setEnabled(enabled);
    }

    // ========== 辅助方法 ==========

    /**
     * 清空响应体内容
     */
    private void clearResponseBody() {
        responseBodyPane.setText("");
        currentFilePath = null;
        fileName = DEFAULT_FILE_NAME;
        lastHeaders = null;
        responseBodyPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        responseBodyPane.setCaretPosition(0);
        searchField.setText("");
        syntaxComboBox.setSelectedIndex(SyntaxType.AUTO_DETECT.getIndex());
        sizeWarningLabel.setVisible(false);
    }

    /**
     * 从响应头中提取 Content-Type
     */
    private String extractContentType(Map<String, List<String>> headers) {
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(CONTENT_TYPE_HEADER)) {
                    List<String> values = entry.getValue();
                    if (values != null && !values.isEmpty()) {
                        return values.get(0);
                    }
                }
            }
        }
        return "";
    }

    /**
     * 更新大小警告标签
     */
    private void updateSizeWarning(int textSize, boolean isLargeResponse) {
        if (isLargeResponse) {
            sizeWarningLabel.setText(String.format("  [%.2f MB]", textSize / 1024.0 / 1024.0));
            sizeWarningLabel.setVisible(true);
        } else {
            sizeWarningLabel.setVisible(false);
        }
    }

    /**
     * 更新编辑器字体
     * 使用用户设置的字体大小
     */
    private void updateEditorFont() {
        responseBodyPane.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
    }

}
