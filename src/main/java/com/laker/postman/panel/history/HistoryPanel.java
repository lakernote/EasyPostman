package com.laker.postman.panel.history;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.Colors;
import com.laker.postman.common.panel.BasePanel;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.RequestHistoryItem;
import com.laker.postman.util.FontUtil;
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

    // 支持带重定向链、线程名和连接信息的历史记录
    public void addRequestHistory(String method, String url, String requestBody, String requestHeaders, String responseStatus, String responseHeaders, String responseBody, HttpResponse resp) {
        RequestHistoryItem item = new RequestHistoryItem(
                method,
                url,
                requestBody,
                requestHeaders,
                responseStatus,
                responseHeaders,
                responseBody,
                System.currentTimeMillis(),
                resp
        );
        if (historyListModel != null) {
            historyListModel.add(0, item);
        }
    }


    private String formatHistoryDetailPrettyHtml(RequestHistoryItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:monospace;font-size:9px;'>");
        sb.append("<b>[Method]</b> <span style='color:#1976d2;'>").append(item.method).append("</span> ");
        sb.append("<b>[URL]</b> <span style='color:#388e3c;'>").append(item.url).append("</span><br><br>");
        sb.append("<b>[Protocol]</b> <span style='color:#1976d2;'>")
                .append(item.response != null && item.response.protocol != null ? item.response.protocol : "(Unknown)")
                .append("</span><br><br>");
        sb.append("<b>[Thread]</b> <span style='color:#d2691e;'>").append(item.threadName == null ? "(None)" : item.threadName).append("</span><br><br>");
        if (item.connectionInfo != null && !item.connectionInfo.isEmpty()) {
            sb.append("<b>[Connection]</b> <span style='color:#1976d2;'>").append(escapeHtml(item.connectionInfo)).append("</span><br><br>");
        }
        sb.append("<b>[Request Headers]</b><br><pre style='margin:0;'>")
                .append(item.requestHeaders == null || item.requestHeaders.isEmpty() ? "(None)" : escapeHtml(item.requestHeaders)).append("</pre><br>");
        sb.append("<b>[Request Body]</b><br><pre style='margin:0;'>")
                .append(item.requestBody == null || item.requestBody.isEmpty() ? "(None)" : escapeHtml(item.requestBody)).append("</pre><br>");
        // 添加分割线
        sb.append("<hr style='border:0;border-top:1.5px dashed #bbb;margin:12px 0;'>");
        sb.append("<b>[Response Status]</b> <span style='color:#1976d2;'>").append(escapeHtml(item.responseStatus)).append("</span><br>");
        sb.append("<b>[Response Headers]</b><br><pre style='margin:0;'>")
                .append(item.responseHeaders == null || item.responseHeaders.isEmpty() ? "(None)" : escapeHtml(item.responseHeaders)).append("</pre><br>");
        sb.append("<b>[Response Body]</b><br><pre style='margin:0;'>")
                .append(item.responseBody == null || item.responseBody.isEmpty() ? "(None)" : escapeHtml(item.responseBody)).append("</pre>");
        sb.append("</body></html>");
        return sb.toString();
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