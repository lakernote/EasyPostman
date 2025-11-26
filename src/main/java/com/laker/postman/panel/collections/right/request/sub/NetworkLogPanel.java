package com.laker.postman.panel.collections.right.request.sub;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;

/**
 * 网络日志面板，支持日志追加、清空、搜索等功能，并可显示重定向链
 */
public class NetworkLogPanel extends JPanel {
    private final JTextPane logArea;
    private final StyledDocument doc;

    // 性能优化配置
    private static final int MAX_LINE_LENGTH = 500; // 单行最大长度
    private static final int MAX_LINES_PER_MESSAGE = 50; // 单条消息最大行数
    private static final int MAX_TOTAL_LENGTH = 100000; // 日志总长度限制（字符数）

    public NetworkLogPanel() {
        setLayout(new BorderLayout());
        // 日志区
        logArea = new JTextPane();
        logArea.setEditable(false);
        doc = logArea.getStyledDocument();
        JScrollPane logScroll = new JScrollPane(logArea);
        add(logScroll, BorderLayout.CENTER);
    }

    public void appendLog(String msg, Color color, boolean bold) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 检查并限制总日志长度，防止内存溢出
                if (doc.getLength() > MAX_TOTAL_LENGTH) {
                    // 删除前1/3的内容，保持日志可读性
                    int removeLength = MAX_TOTAL_LENGTH / 3;
                    doc.remove(0, removeLength);
                }

                // 美化日志输出
                // 1. 解析阶段名和正文
                int stageEnd = msg.indexOf("]");
                String stage = null;
                String content = msg;
                if (msg.startsWith("[") && stageEnd > 0) {
                    stage = msg.substring(0, stageEnd + 1);
                    content = msg.substring(stageEnd + 1).trim();
                }

                // 2. 内容截断优化：如果内容过长，进行截断
                if (content.length() > MAX_LINE_LENGTH * MAX_LINES_PER_MESSAGE) {
                    content = content.substring(0, MAX_LINE_LENGTH * MAX_LINES_PER_MESSAGE)
                            + "\n... [Content truncated, total " + content.length() + " characters]";
                }

                // 3. 选择 emoji
                String emoji = getEmoji(stage);
                // 4. 阶段名样式
                Style stageStyle = logArea.addStyle("stageStyle", null);
                StyleConstants.setForeground(stageStyle, color);
                StyleConstants.setBold(stageStyle, true);
                StyleConstants.setFontSize(stageStyle, 12);
                // 5. 正文样式
                Style contentStyle = logArea.addStyle("contentStyle", null);
                StyleConstants.setForeground(contentStyle, color);
                StyleConstants.setBold(contentStyle, bold);
                StyleConstants.setFontSize(contentStyle, 12);
                // 6. 插入 emoji+阶段名
                if (stage != null) {
                    doc.insertString(doc.getLength(), emoji + " " + stage + " ", stageStyle);
                } else {
                    doc.insertString(doc.getLength(), emoji + " ", stageStyle);
                }
                // 7. 多行内容缩进美化，限制行数和每行长度
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
                logArea.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                // ignore
            }
        });
    }

    @NotNull
    private static String getEmoji(String stage) {
        String emoji = "🔹";
        if (stage != null) {
            if (stage.contains("Failed") || stage.contains("failed") || stage.contains("canceled")) {
                emoji = "❌";
            } else if (stage.contains("callEnd") || stage.contains("cacheHit")) {
                emoji = "✅";
            } else if (stage.contains("secureConnect")) {
                emoji = "🔒";
            } else if (stage.contains("connect")) {
                emoji = "🌐";
            } else if (stage.contains("request")) {
                emoji = "➡️";
            } else if (stage.contains("response")) {
                emoji = "⬅️";
            }
        }
        return emoji;
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
}