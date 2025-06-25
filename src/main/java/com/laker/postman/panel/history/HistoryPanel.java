package com.laker.postman.panel.history;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.Colors;
import com.laker.postman.common.panel.BasePanel;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RequestHistoryItem;
import com.laker.postman.util.FontUtil;
import com.laker.postman.util.HttpEventInfo;
import com.laker.postman.util.JComponentUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 历史记录面板
 */
public class HistoryPanel extends BasePanel {
    public static final String EMPTY_BODY_HTML = "<html><body>Please select a record.</body></html>";
    private JList<RequestHistoryItem> historyList;
    private JPanel historyDetailPanel;
    private JTextPane historyDetailPane;
    private DefaultListModel<RequestHistoryItem> historyListModel;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
        JPanel titlePanel = new JPanel(new BorderLayout());
        // 复合边框
        titlePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY), // 外边框
                BorderFactory.createEmptyBorder(4, 8, 4, 8) // 内边框
        ));
        JLabel title = new JLabel("History");
        title.setFont(FontUtil.getDefaultFont(Font.BOLD, 13));
        JButton clearBtn = new JButton(new FlatSVGIcon("icons/clear.svg"));
        clearBtn.setMargin(new Insets(0, 4, 0, 4));
        clearBtn.setBackground(Colors.PANEL_BACKGROUND);
        clearBtn.setBorder(BorderFactory.createEmptyBorder());
        clearBtn.setFocusPainted(false);
        clearBtn.addActionListener(e -> clearRequestHistory());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(clearBtn);
        titlePanel.add(title, BorderLayout.WEST);
        titlePanel.add(btnPanel, BorderLayout.EAST);
        add(titlePanel, BorderLayout.PAGE_START);

        // 历史列表
        historyListModel = new DefaultListModel<>();
        historyList = new JList<>(historyListModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RequestHistoryItem item) {
                    String text = String.format("[%s] %s", item.method, item.url);
                    text = JComponentUtils.ellipsisText(text, list, 0, 50); // 超出宽度显示省略号
                    label.setText(text);
                    label.setToolTipText(String.format("[%s] %s", item.method, item.url));
                }
                if (isSelected) {
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                    label.setBackground(new Color(180, 215, 255));
                } else {
                    label.setFont(label.getFont().deriveFont(Font.PLAIN));
                }
                return label;
            }
        });
        JScrollPane listScroll = new JScrollPane(historyList);
        listScroll.setPreferredSize(new Dimension(220, 240));
        listScroll.setMinimumSize(new Dimension(220, 240));
        listScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 水平滚动条不需要，内容不会超出


        // 详情区
        historyDetailPanel = new JPanel(new BorderLayout());
        historyDetailPane = new JTextPane();
        historyDetailPane.setEditable(false);
        historyDetailPane.setContentType("text/html");
        historyDetailPane.setFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
        JScrollPane detailScroll = new JScrollPane(historyDetailPane);
        detailScroll.setPreferredSize(new Dimension(340, 240));
        detailScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        detailScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        historyDetailPanel.add(detailScroll, BorderLayout.CENTER);
        historyDetailPane.setText(EMPTY_BODY_HTML);
        historyDetailPanel.setVisible(true);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, historyDetailPanel);
        split.setDividerLocation(220);
        split.setDividerSize(1);
        add(split, BorderLayout.CENTER);
        setMinimumSize(new Dimension(0, 120));

        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int idx = historyList.getSelectedIndex();
                if (idx == -1) {
                    historyDetailPane.setText(EMPTY_BODY_HTML);
                } else {
                    RequestHistoryItem item = historyListModel.get(idx);
                    historyDetailPane.setText(formatHistoryDetailPrettyHtml(item));
                    historyDetailPane.setCaretPosition(0);
                }
            }
        });
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = historyList.locationToIndex(e.getPoint());
                    if (idx != -1) {
                        historyList.setSelectedIndex(idx);
                    }
                }
            }
        });
        SwingUtilities.invokeLater(() -> historyList.repaint());
    }

    @Override
    protected void registerListeners() {

    }

    public void addRequestHistory(PreparedRequest req, HttpResponse resp) {
        RequestHistoryItem item = new RequestHistoryItem(req, resp);
        if (historyListModel != null) {
            historyListModel.add(0, item);
        }
    }


    private String formatHistoryDetailPrettyHtml(RequestHistoryItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:monospace;font-size:9px;'>");
        // ====== 基本信息 Chrome风格单栏 ======
        sb.append("<div style='margin-bottom:12px;'>");
        sb.append("<b style='color:#1976d2;'>").append(item.method).append("</b> ");
        sb.append("<span style='color:#388e3c;'>").append(escapeHtml(item.url)).append("</span><br>");
        sb.append("<span style='color:#888;'>Status</span>: <b>").append(item.responseCode).append("</b>  ");
        sb.append("<span style='color:#888;'>Protocol</span>: ")
                .append(item.response != null && item.response.protocol != null ? item.response.protocol : "-").append("  ");
        sb.append("<span style='color:#888;'>Thread</span>: ")
                .append(item.threadName == null ? "-" : escapeHtml(item.threadName)).append("  ");
        sb.append("<span style='color:#888;'>Connection</span>: ");
        if (item.response != null && item.response.httpEventInfo != null) {
            sb.append(escapeHtml(item.response.httpEventInfo.getLocalAddress() + " -> " + item.response.httpEventInfo.getRemoteAddress()));
        } else {
            sb.append("-");
        }
        sb.append("</div>");

        // ====== 请求头部分 ======
        sb.append("<div style='margin-bottom:8px;'><b style='color:#1976d2;'>[Request Headers]</b></div>");
        sb.append("<pre style='margin:0;'>");
        if (item.request != null && item.request.headers != null && !item.request.headers.isEmpty()) {
            for (var entry : item.request.headers.entrySet()) {
                sb.append(escapeHtml(entry.getKey())).append(": ").append(escapeHtml(entry.getValue())).append("\n");
            }
        } else {
            sb.append("(None)");
        }
        sb.append("</pre>");

        // ====== 请求体部分，优先formData、formFiles ======
        sb.append("<div style='margin:8px 0 4px 0;'><b style='color:#1976d2;'>[Request Body]</b></div>");
        boolean hasForm = false;
        if (item.request != null) {
            if (item.request.formData != null && !item.request.formData.isEmpty()) {
                hasForm = true;
                sb.append("<b>form-data</b><br><pre style='margin:0;'>");
                for (var entry : item.request.formData.entrySet()) {
                    sb.append(escapeHtml(entry.getKey())).append(" = ").append(escapeHtml(entry.getValue())).append("\n");
                }
                sb.append("</pre>");
            }
            if (item.request.formFiles != null && !item.request.formFiles.isEmpty()) {
                hasForm = true;
                sb.append("<b>form-files</b><br><pre style='margin:0;'>");
                for (var entry : item.request.formFiles.entrySet()) {
                    sb.append(escapeHtml(entry.getKey())).append(" = ").append(escapeHtml(entry.getValue())).append("\n");
                }
                sb.append("</pre>");
            }
            if (!hasForm && item.request.body != null && !item.request.body.isEmpty()) {
                sb.append("<pre style='margin:0;'>").append(escapeHtml(item.request.body)).append("</pre>");
            }
            if (!hasForm && (item.request.body == null || item.request.body.isEmpty())) {
                sb.append("<pre style='margin:0;'>(None)</pre>");
            }
        } else {
            sb.append("<pre style='margin:0;'>(None)</pre>");
        }
        sb.append("<hr style='border:0;border-top:1.5px dashed #bbb;margin:12px 0;'>");

        // ====== 响应头部分 ======
        sb.append("<div style='margin-bottom:8px;'><b style='color:#388e3c;'>[Response Headers]</b></div>");
        sb.append("<pre style='margin:0;'>");
        if (item.response != null && item.response.headers != null && !item.response.headers.isEmpty()) {
            for (var entry : item.response.headers.entrySet()) {
                sb.append(escapeHtml(entry.getKey())).append(": ");
                if (entry.getValue() != null) {
                    sb.append(escapeHtml(String.join(", ", entry.getValue())));
                }
                sb.append("\n");
            }
        } else {
            sb.append("(None)");
        }
        sb.append("</pre>");
        // ====== 响应体部分 ======
        sb.append("<div style='margin:8px 0 4px 0;'><b style='color:#388e3c;'>[Response Body]</b></div>");
        sb.append("<pre style='margin:0;'>");
        if (item.response != null && item.response.body != null && !item.response.body.isEmpty()) {
            sb.append(escapeHtml(item.response.body));
        } else {
            sb.append("(None)");
        }
        sb.append("</pre>");

        // ====== 阶段耗时统计与美化输出 ======
        if (item.response != null && item.response.httpEventInfo != null) {
            sb.append("<hr style='border:0;border-top:2px solid #1976d2;margin:16px 0 8px 0;'>");
            sb.append("<div style='font-size:11px;'><b style='color:#1976d2;'>[Timing]</b></div>");
            HttpEventInfo info = item.response.httpEventInfo;
            long dns = info.getDnsEnd() > 0 && info.getDnsStart() > 0 ? info.getDnsEnd() - info.getDnsStart() : -1;
            long connect = info.getConnectEnd() > 0 && info.getConnectStart() > 0 ? info.getConnectEnd() - info.getConnectStart() : -1;
            long tls = info.getSecureConnectEnd() > 0 && info.getSecureConnectStart() > 0 ? info.getSecureConnectEnd() - info.getSecureConnectStart() : -1;
            long reqHeaders = info.getRequestHeadersEnd() > 0 && info.getRequestHeadersStart() > 0 ? info.getRequestHeadersEnd() - info.getRequestHeadersStart() : -1;
            long reqBody = info.getRequestBodyEnd() > 0 && info.getRequestBodyStart() > 0 ? info.getRequestBodyEnd() - info.getRequestBodyStart() : -1;
            long respBody = info.getResponseBodyEnd() > 0 && info.getResponseBodyStart() > 0 ? info.getResponseBodyEnd() - info.getResponseBodyStart() : -1;
            long total = info.getCallEnd() > 0 && info.getCallStart() > 0 ? info.getCallEnd() - info.getCallStart() : -1;
            long serverCost = -1;
            if (info.getResponseHeadersStart() > 0) {
                if (info.getRequestBodyEnd() > 0) {
                    serverCost = info.getResponseHeadersStart() - info.getRequestBodyEnd();
                } else if (info.getRequestHeadersEnd() > 0) {
                    serverCost = info.getResponseHeadersStart() - info.getRequestHeadersEnd();
                }
            }
            sb.append("<div style='margin:8px 0 8px 0;'>");
            sb.append("<div style='font-size:11px;'><b style='color:#1976d2;'>[Timing Timeline]</b></div>");
            sb.append("<table style='border-collapse:collapse;margin:8px 0 8px 0;'>");
            sb.append("<tr><td style='padding:2px 8px 2px 0;color:#333;'><b>Total</b></td><td style='color:#d32f2f;font-weight:bold;'>")
                    .append(total >= 0 ? total + " ms" : "-")
                    .append("</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>Queueing</td><td>")
                    .append(info.getQueueingCost() > 0 ? info.getQueueingCost() + " ms" : "-")
                    .append("</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>Stalled</td><td>")
                    .append(info.getStalledCost() > 0 ? info.getStalledCost() + " ms" : "-")
                    .append("</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>DNS Lookup</td><td>")
                    .append(dns >= 0 ? dns + " ms" : "-")
                    .append("</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>Initial Connection (TCP)</td><td>")
                    .append(connect >= 0 ? connect + " ms" : "-")
                    .append("</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>SSL/TLS</td><td>")
                    .append(tls >= 0 ? tls + " ms" : "-")
                    .append("</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>Request Sent</td><td>")
                    .append((reqHeaders >= 0 || reqBody >= 0) ? ((reqHeaders >= 0 ? reqHeaders : 0) + (reqBody >= 0 ? reqBody : 0)) + " ms" : "-")
                    .append("</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0;color:#1976d2;'><b>Waiting (TTFB)</b></td><td style='color:#388e3c;font-weight:bold;'>")
                    .append(serverCost >= 0 ? serverCost + " ms" : "-")
                    .append("</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>Content Download</td><td>")
                    .append(respBody >= 0 ? respBody + " ms" : "-")
                    .append("</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>OkHttp Idle Connections</td><td>")
                    .append(item.response.idleConnectionCount)
                    .append("</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>OkHttp Total Connections</td><td>")
                    .append(item.response.connectionCount)
                    .append("</td></tr>");
            sb.append("</table>");
            sb.append("<div style='font-size:10px;color:#888;margin-top:2px;'>");
            sb.append("各阶段含义参考Chrome DevTools：Queueing(排队，OkHttp近似为newCall到callStart间)、Stalled(阻塞，近似为callStart到connectStart间)、DNS Lookup、Initial Connection (TCP)、SSL/TLS、Request Sent、Waiting (TTFB)(服务端处理)、Content Download(内容下载)。<br>");
            sb.append("Queueing和Stalled为近似值，受OkHttp实现限制，仅供参考。<br>");
            sb.append("OkHttp Idle/Total Connections为请求时刻连接池快照，仅供参考。");
            sb.append("</div>");
            sb.append("</div>");
        }
        // ====== eventinfo详细内容 ======
        if (item.response != null && item.response.httpEventInfo != null) {
            sb.append("<hr style='border:0;border-top:1.5px dashed #bbb;margin:12px 0'>");
            sb.append("<div style='font-size:11px;'><b style='color:#1976d2;'>[Event Info]</b></div>");
            com.laker.postman.util.HttpEventInfo info = item.response.httpEventInfo;
            sb.append("<table style='border-collapse:collapse;background:#f7f7f7;border-radius:4px;padding:6px 8px;color:#444;margin:8px 0 8px 0;'>");
            sb.append("<tr><td style='padding:2px 8px 2px 0;color:#888;'>Local</td><td>" + escapeHtml(info.getLocalAddress()) + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0;color:#888;'>Remote</td><td>" + escapeHtml(info.getRemoteAddress()) + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0;color:#888;'>Protocol</td><td>" + (info.getProtocol() != null ? info.getProtocol().toString() : "-") + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0;color:#888;'>TLS</td><td>" + (info.getTlsVersion() != null ? info.getTlsVersion() : "-") + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0;color:#888;'>Thread</td><td>" + (info.getThreadName() != null ? info.getThreadName() : "-") + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0;color:#888;'>Error</td><td>" + (info.getErrorMessage() != null ? escapeHtml(info.getErrorMessage()) : "-") + "</td></tr>");
            sb.append("<tr><td colspan='2'><hr style='border:0;border-top:1px dashed #bbb;margin:4px 0'></td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0;color:#1976d2;'>CallStart</td><td>" + formatMillis(info.getCallStart()) + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>DnsStart</td><td>").append(formatMillis(info.getDnsStart())).append("</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>DnsEnd</td><td>").append(formatMillis(info.getDnsEnd())).append("</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>ConnectStart</td><td>" + formatMillis(info.getConnectStart()) + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>ConnectEnd</td><td>" + formatMillis(info.getConnectEnd()) + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>SecureConnectStart</td><td>" + formatMillis(info.getSecureConnectStart()) + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>SecureConnectEnd</td><td>" + formatMillis(info.getSecureConnectEnd()) + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>RequestHeadersStart</td><td>" + formatMillis(info.getRequestHeadersStart()) + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>RequestHeadersEnd</td><td>" + formatMillis(info.getRequestHeadersEnd()) + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>RequestBodyStart</td><td>" + formatMillis(info.getRequestBodyStart()) + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>RequestBodyEnd</td><td>" + formatMillis(info.getRequestBodyEnd()) + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>ResponseHeadersStart</td><td>" + formatMillis(info.getResponseHeadersStart()) + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>ResponseHeadersEnd</td><td>" + formatMillis(info.getResponseHeadersEnd()) + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>ResponseBodyStart</td><td>" + formatMillis(info.getResponseBodyStart()) + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0'>ResponseBodyEnd</td><td>" + formatMillis(info.getResponseBodyEnd()) + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0;color:#1976d2;'>CallEnd</td><td>" + formatMillis(info.getCallEnd()) + "</td></tr>");
            sb.append("<tr><td style='padding:2px 8px 2px 0;color:#d32f2f;'>CallFailed</td><td>" + formatMillis(info.getCallFailed()) + "</td></tr>");
            sb.append("</table>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * 毫秒时间戳转为 hh:mm:ss.SSS 格式
     */
    private String formatMillis(long millis) {
        if (millis <= 0) return "-";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
        java.util.Date date = new java.util.Date(millis);
        return sdf.format(date);
    }


    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }


    private void clearRequestHistory() {
        historyListModel.clear();
        historyDetailPane.setText(EMPTY_BODY_HTML);
        historyDetailPanel.setVisible(true);
    }
}

