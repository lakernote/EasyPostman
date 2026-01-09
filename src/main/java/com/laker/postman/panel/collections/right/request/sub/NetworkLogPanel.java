package com.laker.postman.panel.collections.right.request.sub;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.jetbrains.annotations.NotNull;

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

        // 创建 TabbedPane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FontsUtil.getDefaultFont(Font.PLAIN));

        // 1. Network Log Tab - 添加图标
        logArea = new JTextPane();
        logArea.setEditable(false);
        doc = logArea.getStyledDocument();
        JScrollPane logScroll = new JScrollPane(logArea);

        // 创建带图标的标签
        JLabel networkLogLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TAB_NETWORK_LOG));
        FlatSVGIcon networkLogIcon = new FlatSVGIcon("icons/console.svg", 14, 14);
        networkLogIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("TabbedPane.foreground")));
        networkLogLabel.setIcon(networkLogIcon);
        tabbedPane.addTab(null, logScroll);
        tabbedPane.setTabComponentAt(0, networkLogLabel);

        // 2. Request Details Tab - 添加图标
        requestDetailsPane = createDetailPane();
        JScrollPane requestDetailsScroll = new JScrollPane(requestDetailsPane);
        requestDetailsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        requestDetailsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JLabel requestDetailsLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TAB_REQUEST_DETAILS));
        FlatSVGIcon requestDetailsIcon = new FlatSVGIcon("icons/arrow-up.svg", 14, 14);
        requestDetailsIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("TabbedPane.foreground")));
        requestDetailsLabel.setIcon(requestDetailsIcon);
        tabbedPane.addTab(null, requestDetailsScroll);
        tabbedPane.setTabComponentAt(1, requestDetailsLabel);

        // 3. Response Details Tab - 添加图标
        responseDetailsPane = createDetailPane();
        JScrollPane responseDetailsScroll = new JScrollPane(responseDetailsPane);
        responseDetailsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        responseDetailsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JLabel responseDetailsLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_DETAILS));
        FlatSVGIcon responseDetailsIcon = new FlatSVGIcon("icons/arrow-down.svg", 14, 14);
        responseDetailsIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("TabbedPane.foreground")));
        responseDetailsLabel.setIcon(responseDetailsIcon);
        tabbedPane.addTab(null, responseDetailsScroll);
        tabbedPane.setTabComponentAt(2, responseDetailsLabel);

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

                // 3. 选择 emoji 和优化颜色
                String emoji = getEmoji(stage);
                Color optimizedColor = optimizeColor(color, stage);

                // 4. 阶段名样式
                Style stageStyle = logArea.addStyle("stageStyle_" + System.nanoTime(), null);
                StyleConstants.setForeground(stageStyle, optimizedColor);
                StyleConstants.setBold(stageStyle, true);
                StyleConstants.setFontSize(stageStyle, 13);

                // 5. 正文样式
                Style contentStyle = logArea.addStyle("contentStyle_" + System.nanoTime(), null);
                StyleConstants.setForeground(contentStyle, color);
                StyleConstants.setBold(contentStyle, bold);
                StyleConstants.setFontSize(contentStyle, 13);

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

                // 自动滚动到底部
                logArea.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                // ignore
            }
        });
    }

    /**
     * 优化日志颜色，使用柔和的颜色方案
     */
    private Color optimizeColor(Color original, String stage) {
        if (stage == null) return original;

        // 使用柔和的颜色方案，避免颜色过重
        if (stage.contains("Failed") || stage.contains("failed") || stage.contains("canceled")) {
            return new Color(220, 100, 100); // 柔和的红色 - 错误
        } else if (stage.contains("callEnd") || stage.contains("cacheHit")) {
            return new Color(100, 180, 100); // 柔和的绿色 - 成功
        } else if (stage.contains("secureConnect")) {
            return new Color(180, 120, 200); // 柔和的紫色 - SSL/TLS
        } else if (stage.contains("connect")) {
            return new Color(100, 150, 220); // 柔和的蓝色 - 连接
        } else if (stage.contains("request")) {
            return new Color(220, 160, 100); // 柔和的橙色 - 请求
        } else if (stage.contains("response")) {
            return new Color(100, 180, 200); // 柔和的青色 - 响应
        }

        return original;
    }

    @NotNull
    private static String getEmoji(String stage) {
        if (stage == null) return "📋";

        // 错误和失败
        if (stage.contains("Failed") || stage.contains("failed")) {
            return "❌";
        }
        if (stage.contains("canceled")) {
            return "🚫";
        }

        // 成功和完成
        if (stage.contains("callEnd")) {
            return "✅";
        }
        if (stage.contains("cacheHit")) {
            return "💾";
        }

        // 安全连接
        if (stage.contains("secureConnectStart")) {
            return "🔐";
        }
        if (stage.contains("secureConnectEnd")) {
            return "🔒";
        }

        // 连接相关
        if (stage.contains("connectStart")) {
            return "🔌";
        }
        if (stage.contains("connectEnd")) {
            return "✔️";
        }
        if (stage.contains("connectFailed")) {
            return "⚠️";
        }
        if (stage.contains("connectionAcquired")) {
            return "🔗";
        }
        if (stage.contains("connectionReleased")) {
            return "🔓";
        }

        // DNS
        if (stage.contains("dnsStart")) {
            return "🔍";
        }
        if (stage.contains("dnsEnd")) {
            return "📍";
        }

        // 请求
        if (stage.contains("requestHeadersStart")) {
            return "📤";
        }
        if (stage.contains("requestHeadersEnd")) {
            return "📨";
        }
        if (stage.contains("requestBodyStart")) {
            return "📦";
        }
        if (stage.contains("requestBodyEnd")) {
            return "✔️";
        }
        if (stage.contains("requestFailed")) {
            return "❌";
        }

        // 响应
        if (stage.contains("responseHeadersStart")) {
            return "📥";
        }
        if (stage.contains("responseHeadersEnd:redirect")) {
            return "🔀";
        }
        if (stage.contains("responseHeadersEnd")) {
            return "📬";
        }
        if (stage.contains("responseBodyStart")) {
            return "📄";
        }
        if (stage.contains("responseBodyEnd")) {
            return "✔️";
        }
        if (stage.contains("responseFailed")) {
            return "❌";
        }

        // 代理
        if (stage.contains("proxySelect")) {
            return "🌐";
        }

        // 重定向
        if (stage.contains("Redirect")) {
            return "↪️";
        }

        // 调用
        if (stage.contains("callStart")) {
            return "🚀";
        }
        if (stage.contains("callFailed")) {
            return "💥";
        }

        // 默认
        return "📋";
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

