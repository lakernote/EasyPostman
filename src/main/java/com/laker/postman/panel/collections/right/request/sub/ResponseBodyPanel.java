package com.laker.postman.panel.collections.right.request.sub;

import cn.hutool.core.util.XmlUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.NotificationUtil;
import lombok.Getter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * 响应体面板，展示响应体内容和格式化按钮
 */
public class ResponseBodyPanel extends JPanel {
    @Getter
    private final RSyntaxTextArea responseBodyPane;
    private final JButton downloadButton;
    private String currentFilePath;
    private String fileName = DEFAULT_FILE_NAME; // 默认下载文件名
    private final FlatTextField searchField;
    private Map<String, List<String>> lastHeaders;
    private final JComboBox<String> syntaxComboBox;
    private final JButton formatButton;
    private final JButton prevButton;
    private final JButton nextButton;
    RTextScrollPane scrollPane;

    // 常量定义
    private static final int LARGE_RESPONSE_THRESHOLD = 500 * 1024; // 500KB threshold
    private static final int MAX_AUTO_FORMAT_SIZE = 1024 * 1024; // 1MB max for auto-format
    private static final int ICON_SIZE = 16;
    private static final int BUFFER_SIZE = 8192;
    private static final String DEFAULT_FILE_NAME = "downloaded_file";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String SKIP_AUTO_FORMAT_MESSAGE = " Skip auto-format for large response.";


    private final JLabel sizeWarningLabel;

    public ResponseBodyPanel() {
        setLayout(new BorderLayout());
        responseBodyPane = new RSyntaxTextArea();
        responseBodyPane.setEditable(false);
        responseBodyPane.setCodeFoldingEnabled(true);
        responseBodyPane.setLineWrap(false); // 禁用自动换行以提升大文本性能
        responseBodyPane.setHighlightCurrentLine(false); // 关闭选中行高亮
        scrollPane = new RTextScrollPane(responseBodyPane);
        scrollPane.setLineNumbersEnabled(true); // 显示行号
        add(scrollPane, BorderLayout.CENTER);

        // 顶部工具栏优化：左侧为搜索，右侧为格式化、语法下拉、下载
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setLayout(new BorderLayout());

        // 左侧操作区
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        syntaxComboBox = new JComboBox<>(SyntaxType.getDisplayNames());
        leftPanel.add(syntaxComboBox);

        // 添加大小提示标签
        sizeWarningLabel = new JLabel();
        sizeWarningLabel.setForeground(new Color(200, 100, 0));
        sizeWarningLabel.setVisible(false);
        leftPanel.add(sizeWarningLabel);

        toolBar.add(leftPanel, BorderLayout.WEST);

        // 右侧搜索区
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        searchField = new SearchTextField();
        prevButton = new JButton(new FlatSVGIcon("icons/arrow-up.svg", ICON_SIZE, ICON_SIZE));
        prevButton.setToolTipText("Previous");
        nextButton = new JButton(new FlatSVGIcon("icons/arrow-down.svg", ICON_SIZE, ICON_SIZE));
        nextButton.setToolTipText("Next");
        rightPanel.add(searchField);
        rightPanel.add(prevButton);
        rightPanel.add(nextButton);
        formatButton = new JButton(new FlatSVGIcon("icons/format.svg", ICON_SIZE, ICON_SIZE));
        formatButton.setToolTipText("Format");
        rightPanel.add(formatButton);
        downloadButton = new JButton(new FlatSVGIcon("icons/download.svg", ICON_SIZE, ICON_SIZE));
        downloadButton.setToolTipText("Download");
        downloadButton.setVisible(false);
        rightPanel.add(downloadButton);
        toolBar.add(rightPanel, BorderLayout.EAST);
        add(toolBar, BorderLayout.NORTH);

        downloadButton.addActionListener(e -> saveFile());
        formatButton.addActionListener(e -> formatContent());
        searchField.addActionListener(e -> search(true));
        prevButton.addActionListener(e -> search(false));
        nextButton.addActionListener(e -> search(true));
        syntaxComboBox.addActionListener(e -> onSyntaxComboChanged());
    }

    private void onSyntaxComboChanged() {
        int idx = syntaxComboBox.getSelectedIndex();
        SyntaxType syntaxType = SyntaxType.getByIndex(idx);

        String syntax;
        if (syntaxType == SyntaxType.AUTO_DETECT) {
            // 自动检测
            syntax = detectSyntax(responseBodyPane.getText(), getCurrentContentTypeFromHeaders());
        } else {
            // 从枚举中获取语法样式
            syntax = syntaxType.getSyntaxStyle();
        }

        responseBodyPane.setSyntaxEditingStyle(syntax);
    }

    private void search(boolean forward) {
        String keyword = searchField.getText();
        if (keyword == null || keyword.isEmpty()) {
            return;
        }

        String text = responseBodyPane.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        int pos = forward ? searchForward(text, keyword) : searchBackward(text, keyword);

        if (pos != -1) {
            selectAndFocusText(pos, keyword.length());
        }
    }

    private int searchForward(String text, String keyword) {
        int caret = responseBodyPane.getCaretPosition();
        int start = caret;

        if (isKeywordSelected(keyword)) {
            start = caret + 1;
        }

        int pos = text.indexOf(keyword, start);
        return pos == -1 ? text.indexOf(keyword) : pos;
    }

    private int searchBackward(String text, String keyword) {
        int caret = responseBodyPane.getCaretPosition();
        int start = caret - 1;

        if (isKeywordSelected(keyword)) {
            start = caret - keyword.length() - 1;
        }

        if (start < 0) {
            start = text.length() - 1;
        }

        int pos = text.lastIndexOf(keyword, start);
        return pos == -1 ? text.lastIndexOf(keyword) : pos;
    }

    private boolean isKeywordSelected(String keyword) {
        String selectedText = responseBodyPane.getSelectedText();
        return selectedText != null && selectedText.equals(keyword);
    }

    private void selectAndFocusText(int pos, int length) {
        responseBodyPane.setCaretPosition(pos);
        responseBodyPane.select(pos, pos + length);
        responseBodyPane.requestFocusInWindow();
    }

    private void saveFile() {
        if (currentFilePath == null) return;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save File");
        fileChooser.setSelectedFile(new File(fileName));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File destFile = fileChooser.getSelectedFile();
            try (InputStream in = new FileInputStream(currentFilePath);
                 OutputStream out = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } catch (Exception ex) {
                NotificationUtil.showError("Save File Error: " + ex.getMessage());
            }
        }
    }

    private void formatContent() {
        String text = responseBodyPane.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        String contentType = getCurrentContentTypeFromHeaders();

        try {
            String formatted = null;
            if (contentType.contains("json")) {
                JSON json = JSONUtil.parse(text);
                formatted = JSONUtil.toJsonPrettyStr(json);
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

    public void setBodyText(HttpResponse resp) {
        if (resp == null) {
            clearResponseBody();
            return;
        }

        this.currentFilePath = resp.filePath;
        this.fileName = resp.fileName;
        this.lastHeaders = resp.headers;
        String filePath = resp.filePath;
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

        downloadButton.setVisible(filePath != null && !filePath.isEmpty());
        responseBodyPane.setCaretPosition(0);
    }

    // 动态检测内容类型
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

    // 自动格式化（如果可能）
    private void autoFormatIfPossible(String text, String contentType) {
        if (text == null || text.isEmpty()) {
            return;
        }

        int textSize = text.getBytes().length;

        // 小文件直接格式化
        if (textSize < LARGE_RESPONSE_THRESHOLD) {
            try {
                if (contentType != null && contentType.toLowerCase().contains("json")) {
                    JSON json = JSONUtil.parse(text);
                    String pretty = JSONUtil.toJsonPrettyStr(json);
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
        downloadButton.setVisible(false);
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

}