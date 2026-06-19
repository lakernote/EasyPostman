package com.laker.postman.panel.collections.editor.request.sub;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.observation.NetworkLogEvent;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.service.curl.CurlParser;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.common.component.notification.NotificationCenter;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * 网络日志面板，包含网络日志、请求详情和响应详情三个子Tab
 */
public class NetworkLogPanel extends JPanel {
    private final JTextPane logArea;
    private final StyledDocument doc;
    private final JTabbedPane tabbedPane;
    private final JTextPane requestDetailsPane;
    private final JTextPane responseDetailsPane;
    private final JButton copyActualCurlButton;
    private PreparedRequest currentRequest;

    // 性能优化配置 - 降低限制防止卡顿
    private static final int MAX_LINE_LENGTH = 500; // 单行最大长度
    private static final int MAX_LINES_PER_MESSAGE = 30; // 单条消息最大行数
    private static final int MAX_TOTAL_LENGTH = 50000; // 日志总长度限制（字符数）

    public NetworkLogPanel() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        // 设置边距
        setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 5));

        copyActualCurlButton = createCopyActualCurlButton();
        add(createToolbar(), BorderLayout.NORTH);

        // 创建 TabbedPane
        tabbedPane = new JTabbedPane(SwingConstants.LEFT);
        ToolWindowSurfaceStyle.applyTabbedPaneCard(tabbedPane);

        // 1. Network Log Tab
        logArea = new JTextPane();
        logArea.setEditable(false);
        logArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        ToolWindowSurfaceStyle.applyTextComponentCard(logArea);
        doc = logArea.getStyledDocument();
        JScrollPane logScroll = new JScrollPane(logArea);
        ToolWindowSurfaceStyle.applyScrollPaneCard(logScroll);
        tabbedPane.addTab("Log", logScroll);

        // 2. Request Details Tab
        requestDetailsPane = createDetailPane();
        JScrollPane requestDetailsScroll = new JScrollPane(requestDetailsPane);
        ToolWindowSurfaceStyle.applyScrollPaneCard(requestDetailsScroll);
        requestDetailsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        requestDetailsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tabbedPane.addTab("Request", requestDetailsScroll);

        // 3. Response Details Tab
        responseDetailsPane = createDetailPane();
        JScrollPane responseDetailsScroll = new JScrollPane(responseDetailsPane);
        ToolWindowSurfaceStyle.applyScrollPaneCard(responseDetailsScroll);
        responseDetailsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        responseDetailsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tabbedPane.addTab("Response", responseDetailsScroll);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        ToolWindowSurfaceStyle.applySectionHeader(toolbar, 0, 0, 4, 0);
        toolbar.add(copyActualCurlButton);
        return toolbar;
    }

    private JButton createCopyActualCurlButton() {
        String label = I18nUtil.getMessage(MessageKeys.NETWORK_LOG_COPY_ACTUAL_CURL);
        JButton button = new JButton(IconUtil.createThemed("icons/copy.svg", 16, 16));
        button.setPreferredSize(new Dimension(30, 30));
        button.setMaximumSize(new Dimension(30, 30));
        button.setFocusable(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.setToolTipText(label + " - " + I18nUtil.getMessage(MessageKeys.NETWORK_LOG_COPY_ACTUAL_CURL_TOOLTIP));
        button.getAccessibleContext().setAccessibleName(label);
        button.setEnabled(false);
        button.addActionListener(e -> copyActualCurlToClipboard());
        return button;
    }

    /**
     * 创建详情面板
     */
    private JTextPane createDetailPane() {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setContentType("text/html");
        pane.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        ToolWindowSurfaceStyle.applyTextComponentCard(pane);
        return pane;
    }

    /**
     * 添加日志
     *
     * @param stage 日志阶段枚举
     * @param msg   消息内容
     */
    public void appendLog(NetworkLogStage stage, String msg) {
        appendLog(stage, msg, null);
    }

    /**
     * 渲染 HTTP 执行层发布的日志事件。
     * <p>
     * 执行层只提供阶段和内容，颜色/图标等展示细节仍由 Swing 面板本地的 NetworkLogStage 决定。
     */
    public void appendLog(NetworkLogEvent event) {
        if (event == null) {
            return;
        }
        appendLog(NetworkLogStage.fromEventStage(event.stage()), event.message(), event.elapsedMs(), event.durationMs());
    }

    /**
     * 添加日志（支持时间偏移）
     *
     * @param stage     日志阶段枚举
     * @param msg       消息内容
     * @param elapsedMs 已用时间（毫秒），可为 null
     */
    public void appendLog(NetworkLogStage stage, String msg, Long elapsedMs) {
        appendLog(stage, msg, elapsedMs, null);
    }

    public void appendLog(NetworkLogStage stage, String msg, Long elapsedMs, Long durationMs) {
        SwingUtilities.invokeLater(() -> {
            try {
                NetworkLogStage resolvedStage = stage == null ? NetworkLogStage.DEFAULT : stage;
                // 检查并限制总日志长度，防止内存溢出
                if (doc.getLength() > MAX_TOTAL_LENGTH) {
                    // 删除前1/3的内容，保持日志可读性
                    int removeLength = MAX_TOTAL_LENGTH / 3;
                    doc.remove(0, removeLength);
                }

                // 内容截断优化：如果内容过长，进行截断
                String content = msg != null ? msg : "";
                if (content.length() > MAX_LINE_LENGTH * MAX_LINES_PER_MESSAGE) {
                    content = content.substring(0, MAX_LINE_LENGTH * MAX_LINES_PER_MESSAGE)
                            + "\n... [Content truncated, total " + content.length() + " characters]";
                }

                // 从枚举获取配置
                String emoji = resolvedStage.getEmoji();
                Color stageColor = resolvedStage.getColor();
                boolean bold = resolvedStage.isBold();
                int fontSize = FontsUtil.getDefaultFont(Font.PLAIN).getSize();
                SimpleAttributeSet stageStyle = createTextAttributes(stageColor, true, fontSize);
                SimpleAttributeSet contentStyle = createTextAttributes(getDefaultTextColor(), bold, fontSize);

                // 插入 emoji + 阶段名 + 时间（如果有）
                StringBuilder stageText = new StringBuilder();
                stageText.append(emoji).append(" [").append(resolvedStage.getStageName()).append("]");
                if (elapsedMs != null) {
                    stageText.append(" +").append(elapsedMs).append("ms");
                }
                if (durationMs != null) {
                    stageText.append(elapsedMs == null ? " " : ", ")
                            .append("phase=")
                            .append(durationMs)
                            .append("ms");
                }
                stageText.append(" ");
                doc.insertString(doc.getLength(), stageText.toString(), stageStyle);

                doc.insertString(doc.getLength(), formatContent(content), contentStyle);

                // 自动滚动到底部
                logArea.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                // ignore
            }
        });
    }

    /**
     * 获取主题适配的默认文本颜色
     */
    private Color getDefaultTextColor() {
        return NetworkLogStage.DEFAULT.getColor();
    }

    private SimpleAttributeSet createTextAttributes(Color color, boolean bold, int fontSize) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, color);
        StyleConstants.setBold(attributes, bold);
        StyleConstants.setFontSize(attributes, fontSize);
        return attributes;
    }

    private String formatContent(String content) {
        StringBuilder formatted = new StringBuilder();
        // 多行内容缩进美化，限制行数和每行长度
        String[] lines = content.split("\\n");
        int lineCount = Math.min(lines.length, MAX_LINES_PER_MESSAGE);
        for (int i = 0; i < lineCount; i++) {
            String line = lines[i];
            // 限制单行长度
            if (line.length() > MAX_LINE_LENGTH) {
                line = line.substring(0, MAX_LINE_LENGTH) + "...";
            }
            if (i > 0) {
                formatted.append("\n    ");
            }
            formatted.append(line);
        }
        // 如果行数被截断，添加提示
        if (lines.length > MAX_LINES_PER_MESSAGE) {
            formatted.append("\n    ... [")
                    .append(lines.length - MAX_LINES_PER_MESSAGE)
                    .append(" more lines omitted]");
        }
        formatted.append("\n");
        return formatted.toString();
    }


    public void clearLog() {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.remove(0, doc.getLength());
            } catch (BadLocationException e) {
                // ignore
            }
        });
    }

    /**
     * 更新请求详情
     */
    public void setRequestDetails(PreparedRequest request) {
        if (requestDetailsPane == null) return;
        currentRequest = request;
        updateCopyActualCurlButtonState();
        if (request == null) {
            requestDetailsPane.setText(I18nUtil.getMessage(MessageKeys.HISTORY_EMPTY_BODY));
            return;
        }
        String html = HttpHtmlRenderer.renderRequest(request);
        requestDetailsPane.setText(html);
        requestDetailsPane.setCaretPosition(0);
    }

    /**
     * 更新响应详情
     */
    public void setResponseDetails(HttpResponse response) {
        if (responseDetailsPane == null) return;
        if (response == null) {
            responseDetailsPane.setText(I18nUtil.getMessage(MessageKeys.HISTORY_EMPTY_BODY));
            return;
        }
        String html = HttpHtmlRenderer.renderResponse(response);
        responseDetailsPane.setText(html);
        responseDetailsPane.setCaretPosition(0);
    }

    /**
     * 清空所有详情面板
     */
    public void clearAllDetails() {
        currentRequest = null;
        updateCopyActualCurlButtonState();
        if (requestDetailsPane != null) {
            requestDetailsPane.setText("");
        }
        if (responseDetailsPane != null) {
            responseDetailsPane.setText("");
        }
    }

    private void updateCopyActualCurlButtonState() {
        if (copyActualCurlButton != null) {
            copyActualCurlButton.setEnabled(CurlParser.canExportActualCurl(currentRequest));
        }
    }

    private void copyActualCurlToClipboard() {
        try {
            String curl = CurlParser.toActualCurl(currentRequest);
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(curl), null);
            NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.NETWORK_LOG_COPY_ACTUAL_CURL_SUCCESS));
        } catch (Exception ex) {
            NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.NETWORK_LOG_COPY_ACTUAL_CURL_FAIL, ex.getMessage()));
        }
    }
}
