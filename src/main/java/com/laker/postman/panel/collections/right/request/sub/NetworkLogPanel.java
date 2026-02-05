package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;

/**
 * 网络日志面板，包含网络日志、请求详情和响应详情三个子Tab
 */
public class NetworkLogPanel extends JPanel {
    private final JTextPane logArea;
    private final StyledDocument doc;
    private final JTabbedPane tabbedPane;
    private final JTextPane requestDetailsPane;
    private final JTextPane responseDetailsPane;

    // 性能优化配置 - 降低限制防止卡顿
    private static final int MAX_LINE_LENGTH = 500; // 单行最大长度
    private static final int MAX_LINES_PER_MESSAGE = 30; // 单条消息最大行数
    private static final int MAX_TOTAL_LENGTH = 50000; // 日志总长度限制（字符数）

    public NetworkLogPanel() {
        setLayout(new BorderLayout());
        // 设置边距
        setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 5));

        // 创建 TabbedPane
        tabbedPane = new JTabbedPane(SwingConstants.LEFT);

        // 1. Network Log Tab
        logArea = new JTextPane();
        logArea.setEditable(false);
        doc = logArea.getStyledDocument();
        JScrollPane logScroll = new JScrollPane(logArea);
        tabbedPane.addTab("Log", logScroll);

        // 2. Request Details Tab
        requestDetailsPane = createDetailPane();
        JScrollPane requestDetailsScroll = new JScrollPane(requestDetailsPane);
        requestDetailsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        requestDetailsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tabbedPane.addTab("Request", requestDetailsScroll);

        // 3. Response Details Tab
        responseDetailsPane = createDetailPane();
        JScrollPane responseDetailsScroll = new JScrollPane(responseDetailsPane);
        responseDetailsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        responseDetailsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tabbedPane.addTab("Response", responseDetailsScroll);

        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * 创建详情面板
     */
    private JTextPane createDetailPane() {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setContentType("text/html");
        pane.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
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
     * 添加日志（支持时间偏移）
     *
     * @param stage     日志阶段枚举
     * @param msg       消息内容
     * @param elapsedMs 已用时间（毫秒），可为 null
     */
    public void appendLog(NetworkLogStage stage, String msg, Long elapsedMs) {
        SwingUtilities.invokeLater(() -> {
            try {
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
                String emoji = stage.getEmoji();
                Color stageColor = stage.getColor();
                boolean bold = stage.isBold();

                // 阶段名样式（使用枚举名称作为标签）
                Style stageStyle = logArea.addStyle("stageStyle_" + System.nanoTime(), null);
                StyleConstants.setForeground(stageStyle, stageColor);
                StyleConstants.setBold(stageStyle, true);
                StyleConstants.setFontSize(stageStyle, 13);

                // 正文样式
                Style contentStyle = logArea.addStyle("contentStyle_" + System.nanoTime(), null);
                StyleConstants.setForeground(contentStyle, getDefaultTextColor());
                StyleConstants.setBold(contentStyle, bold);
                StyleConstants.setFontSize(contentStyle, 13);

                // 插入 emoji + 阶段名 + 时间（如果有）
                StringBuilder stageText = new StringBuilder();
                stageText.append(emoji).append(" [").append(stage.getStageName()).append("]");
                if (elapsedMs != null) {
                    stageText.append(" +").append(elapsedMs).append("ms");
                }
                stageText.append(" ");
                doc.insertString(doc.getLength(), stageText.toString(), stageStyle);

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
                        doc.insertString(doc.getLength(), "\n    " + line, contentStyle);
                    } else {
                        doc.insertString(doc.getLength(), line, contentStyle);
                    }
                }
                // 如果行数被截断，添加提示
                if (lines.length > MAX_LINES_PER_MESSAGE) {
                    doc.insertString(doc.getLength(), "\n    ... [" + (lines.length - MAX_LINES_PER_MESSAGE) + " more lines omitted]", contentStyle);
                }
                doc.insertString(doc.getLength(), "\n", contentStyle);

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
        if (requestDetailsPane != null) {
            requestDetailsPane.setText("");
        }
        if (responseDetailsPane != null) {
            responseDetailsPane.setText("");
        }
    }
}

