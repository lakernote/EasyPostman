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
                // 美化日志输出
                // 1. 解析阶段名和正文
                int stageEnd = msg.indexOf("]");
                String stage = null;
                String content = msg;
                if (msg.startsWith("[") && stageEnd > 0) {
                    stage = msg.substring(0, stageEnd + 1);
                    content = msg.substring(stageEnd + 1).trim();
                }
                // 2. 选择 emoji
                String emoji = getEmoji(stage);
                // 3. 阶段名样式
                Style stageStyle = logArea.addStyle("stageStyle", null);
                StyleConstants.setForeground(stageStyle, color);
                StyleConstants.setBold(stageStyle, true);
                StyleConstants.setFontSize(stageStyle, 13);
                // 4. 正文样式
                Style contentStyle = logArea.addStyle("contentStyle", null);
                StyleConstants.setForeground(contentStyle, color);
                StyleConstants.setBold(contentStyle, bold);
                StyleConstants.setFontSize(contentStyle, 13);
                // 5. 插入 emoji+阶段名
                if (stage != null) {
                    doc.insertString(doc.getLength(), emoji + " " + stage + " ", stageStyle);
                } else {
                    doc.insertString(doc.getLength(), emoji + " ", stageStyle);
                }
                // 6. 多行内容缩进美化
                String[] lines = content.split("\\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    if (i > 0) {
                        doc.insertString(doc.getLength(), "\n    " + line, contentStyle);
                    } else {
                        doc.insertString(doc.getLength(), line, contentStyle);
                    }
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