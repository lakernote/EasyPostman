package com.laker.postman.panel.collections.right.request.sub;

import cn.hutool.core.util.XmlUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.service.setting.SettingManager;
import lombok.Getter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 响应体面板，展示响应体内容和格式化按钮
 */
public class ResponseBodyPanel extends JPanel {
    @Getter
    private final RSyntaxTextArea responseBodyPane;
    private final JButton downloadButton;
    private String currentFilePath;
    private String fileName = "downloaded_file"; // 默认下载文件名
    private final FlatTextField searchField;
    private Map<String, List<String>> lastHeaders;
    private final JComboBox<String> syntaxComboBox;
    private final JButton formatButton;
    private final JButton prevButton;
    private final JButton nextButton;
    RTextScrollPane scrollPane;
    private CompletableFuture<Void> currentFormatTask;
    private static final int LARGE_RESPONSE_THRESHOLD = 500 * 1024; // 500KB threshold
    private static final int MAX_AUTO_FORMAT_SIZE = 1024 * 1024; // 1MB max for auto-format
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
        syntaxComboBox = new JComboBox<>(new String[]{
                "Auto Detect", "JSON", "XML", "HTML", "JavaScript", "CSS", "Plain Text"
        });
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
        prevButton = new JButton(new FlatSVGIcon("icons/arrow-up.svg", 16, 16));
        prevButton.setToolTipText("Previous");
        nextButton = new JButton(new FlatSVGIcon("icons/arrow-down.svg", 16, 16));
        nextButton.setToolTipText("Next");
        rightPanel.add(searchField);
        rightPanel.add(prevButton);
        rightPanel.add(nextButton);
        formatButton = new JButton(new FlatSVGIcon("icons/format.svg", 16, 16));
        formatButton.setToolTipText("Format");
        rightPanel.add(formatButton);
        downloadButton = new JButton(new FlatSVGIcon("icons/download.svg", 16, 16));
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
        if (idx == 0) { // 自动识别
            String syntax = detectSyntax(responseBodyPane.getText(), getCurrentContentTypeFromHeaders());
            responseBodyPane.setSyntaxEditingStyle(syntax);
        } else if (idx == 1) {
            responseBodyPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        } else if (idx == 2) {
            responseBodyPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        } else if (idx == 3) {
            responseBodyPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML);
        } else if (idx == 4) {
            responseBodyPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        } else if (idx == 5) {
            responseBodyPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CSS);
        } else {
            responseBodyPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        }
    }

    private void search(boolean forward) {
        String keyword = searchField.getText();
        if (keyword == null || keyword.isEmpty()) return;
        String text = responseBodyPane.getText();
        if (text == null || text.isEmpty()) return;
        int caret = responseBodyPane.getCaretPosition();
        int pos = -1;
        if (forward) {
            // 向后查找
            int start = caret;
            if (responseBodyPane.getSelectedText() != null && responseBodyPane.getSelectedText().equals(keyword)) {
                start = caret + 1;
            }
            pos = text.indexOf(keyword, start);
            if (pos == -1) {
                // 循环查找
                pos = text.indexOf(keyword);
            }
        } else {
            // 向前查找
            int start = caret - 1;
            if (responseBodyPane.getSelectedText() != null && responseBodyPane.getSelectedText().equals(keyword)) {
                start = caret - keyword.length() - 1;
            }
            if (start < 0) start = text.length() - 1;
            pos = text.lastIndexOf(keyword, start);
            if (pos == -1) {
                pos = text.lastIndexOf(keyword);
            }
        }
        if (pos != -1) {
            responseBodyPane.setCaretPosition(pos);
            responseBodyPane.select(pos, pos + keyword.length());
            responseBodyPane.requestFocusInWindow();
        }
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
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "保存文件失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void formatContent() {
        String text = responseBodyPane.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        // 取消之前的格式化任务
        if (currentFormatTask != null && !currentFormatTask.isDone()) {
            currentFormatTask.cancel(true);
        }

        String contentType = getCurrentContentTypeFromHeaders();
        int textSize = text.getBytes().length;

        // 大文件警告
        if (textSize > MAX_AUTO_FORMAT_SIZE) {
            int result = JOptionPane.showConfirmDialog(this,
                    String.format("响应体较大 (%.2f MB)，格式化可能需要一些时间，确定要继续吗？", textSize / 1024.0 / 1024.0),
                    "确认", JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }

        formatButton.setEnabled(false);
        formatButton.setToolTipText("Formatting...");

        // 异步格式化
        currentFormatTask = CompletableFuture.runAsync(() -> {
            try {
                String formatted = null;
                if (contentType.contains("json")) {
                    JSON json = JSONUtil.parse(text);
                    formatted = JSONUtil.toJsonPrettyStr(json);
                } else if (contentType.contains("xml")) {
                    formatted = XmlUtil.format(text);
                }

                if (formatted != null) {
                    final String finalFormatted = formatted;
                    SwingUtilities.invokeLater(() -> {
                        responseBodyPane.setText(finalFormatted);
                        responseBodyPane.setCaretPosition(0);
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "格式化失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    formatButton.setEnabled(true);
                    formatButton.setToolTipText("Format");
                });
            }
        });
    }

    private String getCurrentContentTypeFromHeaders() {
        if (lastHeaders != null) {
            for (String key : lastHeaders.keySet()) {
                if (key != null && key.equalsIgnoreCase("Content-Type")) {
                    java.util.List<String> values = lastHeaders.get(key);
                    if (values != null && !values.isEmpty()) {
                        return values.get(0);
                    }
                }
            }
        }
        return "";
    }

    public void setBodyText(HttpResponse resp) {
        // 取消之前的格式化任务
        if (currentFormatTask != null && !currentFormatTask.isDone()) {
            currentFormatTask.cancel(true);
        }

        if (resp == null) {
            responseBodyPane.setText("");
            currentFilePath = null;
            fileName = "downloaded_file";
            lastHeaders = null;
            downloadButton.setVisible(false);
            responseBodyPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
            responseBodyPane.setCaretPosition(0);
            searchField.setText("");
            syntaxComboBox.setSelectedIndex(0);
            sizeWarningLabel.setVisible(false);
            return;
        }

        this.currentFilePath = resp.filePath;
        this.fileName = resp.fileName;
        this.lastHeaders = resp.headers;
        String filePath = resp.filePath;
        String text = resp.body;
        String contentType = "";

        if (resp.headers != null) {
            for (String key : resp.headers.keySet()) {
                if (key != null && key.equalsIgnoreCase("Content-Type")) {
                    java.util.List<String> values = resp.headers.get(key);
                    if (values != null && !values.isEmpty()) {
                        contentType = values.get(0);
                        break;
                    }
                }
            }
        }

        int textSize = text != null ? text.getBytes().length : 0;
        boolean isLargeResponse = textSize > LARGE_RESPONSE_THRESHOLD;

        // 显示大小信息
        if (isLargeResponse) {
            sizeWarningLabel.setText(String.format("  [%.2f MB]", textSize / 1024.0 / 1024.0));
            sizeWarningLabel.setVisible(true);
        } else {
            sizeWarningLabel.setVisible(false);
        }

        // 动态选择高亮类型
        String syntax = detectSyntax(text, contentType);

        // 自动匹配下拉框选项
        int syntaxIndex = 0;
        if (SyntaxConstants.SYNTAX_STYLE_JSON.equals(syntax)) {
            syntaxIndex = 1;
        } else if (SyntaxConstants.SYNTAX_STYLE_XML.equals(syntax)) {
            syntaxIndex = 2;
        } else if (SyntaxConstants.SYNTAX_STYLE_HTML.equals(syntax)) {
            syntaxIndex = 3;
        } else if (SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT.equals(syntax)) {
            syntaxIndex = 4;
        } else if (SyntaxConstants.SYNTAX_STYLE_CSS.equals(syntax)) {
            syntaxIndex = 5;
        } else if (SyntaxConstants.SYNTAX_STYLE_NONE.equals(syntax)) {
            syntaxIndex = 6;
        }
        syntaxComboBox.setSelectedIndex(syntaxIndex);

        // 对于大文件，先禁用语法高亮再设置文本，然后再启用
        if (isLargeResponse) {
            responseBodyPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
            responseBodyPane.setText(text);
            // 异步启用语法高亮
            SwingUtilities.invokeLater(() -> responseBodyPane.setSyntaxEditingStyle(syntax));
        } else {
            responseBodyPane.setSyntaxEditingStyle(syntax);
            responseBodyPane.setText(text);
        }

        // 根据设置和大小决定是否自动格式化
        if (SettingManager.isAutoFormatResponse() && textSize < MAX_AUTO_FORMAT_SIZE) {
            autoFormatIfPossible(text, contentType);
        } else if (SettingManager.isAutoFormatResponse() && textSize >= MAX_AUTO_FORMAT_SIZE) {
            // 大文件不自动格式化，提示用户手动格式化
            sizeWarningLabel.setText(sizeWarningLabel.getText() + " Skip auto-format for large response.");
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
        } else {
            // 大文件异步格式化
            CompletableFuture.runAsync(() -> {
                try {
                    String formatted = null;
                    if (contentType != null && contentType.toLowerCase().contains("json")) {
                        JSON json = JSONUtil.parse(text);
                        formatted = JSONUtil.toJsonPrettyStr(json);
                    } else if (contentType != null && contentType.toLowerCase().contains("xml")) {
                        formatted = XmlUtil.format(text);
                    }

                    if (formatted != null) {
                        final String finalFormatted = formatted;
                        SwingUtilities.invokeLater(() -> {
                            responseBodyPane.setText(finalFormatted);
                            responseBodyPane.setCaretPosition(0);
                        });
                    }
                } catch (Exception ex) {
                    // 格式化失败时静默忽略，保持原始内容
                }
            });
        }
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
}

