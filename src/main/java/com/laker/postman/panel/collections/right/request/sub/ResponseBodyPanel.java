package com.laker.postman.panel.collections.right.request.sub;

import cn.hutool.core.util.XmlUtil;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.button.*;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.*;
import lombok.Getter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
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
    private final SearchButton searchButton; // 搜索按钮
    private Map<String, List<String>> lastHeaders;
    private final EasyComboBox<String> syntaxComboBox;
    private final FormatButton formatButton;
    private final CopyButton copyButton;
    private final WrapToggleButton wrapButton;
    private final SearchableTextArea searchableTextArea; // 带搜索功能的文本编辑器

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
        // 设置边距
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        responseBodyPane = new RSyntaxTextArea();
        responseBodyPane.setEditable(false);
        responseBodyPane.setCodeFoldingEnabled(true);
        responseBodyPane.setLineWrap(false); // 禁用自动换行以提升大文本性能
        responseBodyPane.setHighlightCurrentLine(false); // 关闭选中行高亮

        // 设置字体 - 使用用户设置的字体大小
        updateEditorFont();

        // 加载编辑器主题 - 支持亮色和暗色主题自适应
        EditorThemeUtil.loadTheme(responseBodyPane);

        // 使用 SearchableTextArea 包装，禁用替换功能（仅搜索）
        searchableTextArea = new SearchableTextArea(responseBodyPane, false);
        add(searchableTextArea, BorderLayout.CENTER);

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

        // 搜索按钮
        searchButton = new SearchButton();
        searchButton.addActionListener(e -> {
            responseBodyPane.requestFocusInWindow();
            searchableTextArea.showSearch();
        });
        toolBarPanel.add(searchButton);
        toolBarPanel.add(Box.createHorizontalStrut(1)); // 间隔

        // 换行按钮
        wrapButton = new WrapToggleButton();
        toolBarPanel.add(wrapButton);
        toolBarPanel.add(Box.createHorizontalStrut(1));

        // 格式化按钮
        formatButton = new FormatButton();
        toolBarPanel.add(formatButton);
        toolBarPanel.add(Box.createHorizontalStrut(1));

        // 复制按钮
        copyButton = new CopyButton();
        toolBarPanel.add(copyButton);
        toolBarPanel.add(Box.createHorizontalStrut(1));

        // 下载按钮
        downloadButton = new DownloadButton();
        toolBarPanel.add(downloadButton);

        // 只有 HTTP 请求才显示保存响应按钮
        if (enableSaveButton) {
            toolBarPanel.add(Box.createHorizontalStrut(1));
            saveResponseButton = new SaveResponseButton();
            toolBarPanel.add(saveResponseButton);
        }

        add(toolBarPanel, BorderLayout.NORTH);

        downloadButton.addActionListener(e -> saveFile());
        formatButton.addActionListener(e -> formatContent());
        copyButton.addActionListener(e -> copyToClipboard());
        wrapButton.addActionListener(e -> toggleLineWrap());
        syntaxComboBox.addActionListener(e -> onSyntaxComboChanged());
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
     * 复制内容到剪贴板
     * 将响应体内容复制到系统剪贴板
     */
    private void copyToClipboard() {
        String text = responseBodyPane.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
            NotificationUtil.showInfo("Content copied to clipboard");
        } catch (Exception ex) {
            NotificationUtil.showError("Copy Error: " + ex.getMessage());
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
        searchButton.setEnabled(enabled);
        downloadButton.setEnabled(enabled);
        searchableTextArea.setEnabled(enabled);

        if (formatButton != null) formatButton.setEnabled(enabled);
        if (copyButton != null) copyButton.setEnabled(enabled);
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
