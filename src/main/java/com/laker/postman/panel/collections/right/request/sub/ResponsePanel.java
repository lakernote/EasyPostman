package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.model.TestResult;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.TimeDisplayUtil;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 响应部分面板，包含响应体、响应头、测试结果、网络日志、耗时等
 */
@Getter
public class ResponsePanel extends JPanel {
    private final JLabel statusCodeLabel;
    private final JLabel responseTimeLabel;
    private final JLabel responseSizeLabel;
    private final ResponseHeadersPanel responseHeadersPanel;
    private final ResponseBodyPanel responseBodyPanel;
    private final NetworkLogPanel networkLogPanel;
    private final WaterfallChartPanel timingChartPanel;
    private final JEditorPane testsPane;
    private final JButton[] tabButtons;
    private int selectedTabIndex = 0;
    private final JPanel cardPanel;
    private final String[] tabNames;
    private final RequestItemProtocolEnum protocol;
    private final WebSocketResponsePanel webSocketResponsePanel;

    public ResponsePanel(RequestItemProtocolEnum protocol) {
        this.protocol = protocol;
        setLayout(new BorderLayout());
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        if (protocol.isWebSocketProtocol()) {
            tabNames = new String[]{I18nUtil.getMessage(MessageKeys.MENU_FILE_LOG), I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_HEADERS), I18nUtil.getMessage(MessageKeys.TAB_TESTS)};
            tabButtons = new JButton[tabNames.length];
            for (int i = 0; i < tabNames.length; i++) {
                tabButtons[i] = new TabButton(tabNames[i], i);
                tabBar.add(tabButtons[i]);
            }
            JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 4));
            statusCodeLabel = new JLabel();
            responseTimeLabel = new JLabel();
            responseSizeLabel = new JLabel();
            statusBar.add(statusCodeLabel);
            statusBar.add(responseTimeLabel);
            statusBar.add(responseSizeLabel);
            JPanel topResponseBar = new JPanel(new BorderLayout());
            topResponseBar.add(tabBar, BorderLayout.WEST);
            topResponseBar.add(statusBar, BorderLayout.EAST);
            add(topResponseBar, BorderLayout.NORTH);
            cardPanel = new JPanel(new CardLayout());
            webSocketResponsePanel = new WebSocketResponsePanel();
            responseHeadersPanel = new ResponseHeadersPanel();
            JPanel testsPanel = new JPanel(new BorderLayout());
            testsPane = new JEditorPane();
            testsPane.setContentType("text/html");
            testsPane.setEditable(false);
            JScrollPane testsScrollPane = new JScrollPane(testsPane);
            testsPanel.add(testsScrollPane, BorderLayout.CENTER);
            cardPanel.add(webSocketResponsePanel, tabNames[0]);
            cardPanel.add(responseHeadersPanel, tabNames[1]);
            cardPanel.add(testsPanel, tabNames[2]);
            networkLogPanel = null;
            timingChartPanel = null;
            responseBodyPanel = null;
            add(cardPanel, BorderLayout.CENTER);
        } else {
            tabNames = new String[]{
                    I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_BODY),
                    I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_HEADERS),
                    I18nUtil.getMessage(MessageKeys.TAB_TESTS),
                    I18nUtil.getMessage(MessageKeys.TAB_NETWORK_LOG),
                    I18nUtil.getMessage(MessageKeys.TAB_TIMING)
            };
            tabButtons = new JButton[tabNames.length];
            for (int i = 0; i < tabNames.length; i++) {
                tabButtons[i] = new TabButton(tabNames[i], i);
                tabBar.add(tabButtons[i]);
            }
            JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 4));
            statusCodeLabel = new JLabel();
            responseTimeLabel = new JLabel();
            responseSizeLabel = new JLabel();
            statusBar.add(statusCodeLabel);
            statusBar.add(responseTimeLabel);
            statusBar.add(responseSizeLabel);
            JPanel topResponseBar = new JPanel(new BorderLayout());
            topResponseBar.add(tabBar, BorderLayout.WEST);
            topResponseBar.add(statusBar, BorderLayout.EAST);
            add(topResponseBar, BorderLayout.NORTH);
            cardPanel = new JPanel(new CardLayout());
            responseBodyPanel = new ResponseBodyPanel();
            responseBodyPanel.setEnabled(false);
            responseBodyPanel.setBodyText(null);
            responseHeadersPanel = new ResponseHeadersPanel();
            JPanel testsPanel = new JPanel(new BorderLayout());
            testsPane = new JEditorPane();
            testsPane.setContentType("text/html");
            testsPane.setEditable(false);
            JScrollPane testsScrollPane = new JScrollPane(testsPane);
            testsPanel.add(testsScrollPane, BorderLayout.CENTER);
            networkLogPanel = new NetworkLogPanel();
            timingChartPanel = new WaterfallChartPanel(new java.util.ArrayList<>());
            cardPanel.add(responseBodyPanel, tabNames[0]);
            cardPanel.add(responseHeadersPanel, tabNames[1]);
            cardPanel.add(testsPanel, tabNames[2]);
            cardPanel.add(networkLogPanel, tabNames[3]);
            cardPanel.add(new JScrollPane(timingChartPanel), tabNames[4]);
            webSocketResponsePanel = null;
            add(cardPanel, BorderLayout.CENTER);
        }
        for (int i = 0; i < tabButtons.length; i++) {
            final int idx = i;
            tabButtons[i].addActionListener(e -> {
                CardLayout cl = (CardLayout) cardPanel.getLayout();
                cl.show(cardPanel, tabNames[idx]);
                selectedTabIndex = idx;
                for (JButton btn : tabButtons) {
                    btn.repaint();
                }
            });
        }
        // 默认所有按钮不可用
        setResponseTabButtonsEnable(false);
    }

    public void setResponseTabButtonsEnable(boolean enable) {
        for (JButton btn : tabButtons) {
            btn.setEnabled(enable);
        }
    }

    public void setResponseBody(HttpResponse resp) {
        if (protocol.isWebSocketProtocol()) {
            // WebSocket响应体由 WebSocketResponsePanel 维护，不做处理
            return;
        }
        responseBodyPanel.setBodyText(resp);
    }

    public void setResponseHeaders(HttpResponse resp) {
        responseHeadersPanel.setHeaders(resp.headers);
        // 动态设置Headers按钮文本和颜色
        int headersTabIndex = 1;
        if (tabButtons.length > headersTabIndex) {
            JButton headersBtn = tabButtons[headersTabIndex];
            int count = (resp.headers != null) ? resp.headers.size() : 0;
            if (count > 0) {
                String countText = " (" + count + ")";
                String countHtml = I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_HEADERS) +
                        "<span style='color:#009900;font-weight:bold;'>" + countText + "</span>";
                headersBtn.setText("<html>" + countHtml + "</html>");
                headersBtn.setForeground(Color.BLACK);
            } else {
                headersBtn.setText(I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_HEADERS));
                headersBtn.setForeground(Color.BLACK);
            }
        }
    }

    public void setTiming(HttpResponse resp) {
        if (timingChartPanel == null) return;
        List<WaterfallChartPanel.Stage> stages = new ArrayList<>();
        if (resp != null && resp.httpEventInfo != null) {
            stages = WaterfallChartPanel.buildStandardStages(resp.httpEventInfo);
        }
        timingChartPanel.setStages(stages);
    }

    public void setStatus(String statusText, Color color) {
        statusCodeLabel.setText(statusText);
        statusCodeLabel.setForeground(color);
    }

    public void setResponseTime(long ms) {
        responseTimeLabel.setText(String.format(I18nUtil.getMessage(MessageKeys.STATUS_DURATION), TimeDisplayUtil.formatElapsedTime(ms)));
    }

    public void setResponseSize(int bytes) {
        responseSizeLabel.setText(I18nUtil.getMessage(MessageKeys.STATUS_RESPONSE_SIZE).replace("--", getSizeText(bytes)));
    }

    public void setTestResults(List<TestResult> testResults) {
        String html = HttpHtmlRenderer.renderTestResults(testResults);
        testsPane.setText(html);
        testsPane.setCaretPosition(0);
        // 动态设置Tests按钮文本和颜色
        int testsTabIndex = 2;
        if (tabButtons.length > testsTabIndex) {
            JButton testsBtn = tabButtons[testsTabIndex];
            if (testResults != null && !testResults.isEmpty()) {
                boolean allPassed = testResults.stream().allMatch(r -> r.passed);
                String countText = " (" + testResults.size() + ")";
                String color = allPassed ? "#009900" : "#d32f2f";
                String countHtml = I18nUtil.getMessage(MessageKeys.TAB_TESTS) + "<span style='color:" + color + ";font-weight:bold;'>" + countText + "</span>";
                testsBtn.setText("<html>" + countHtml + "</html>");
                testsBtn.setForeground(Color.BLACK);
            } else {
                testsBtn.setText(I18nUtil.getMessage(MessageKeys.TAB_TESTS));
                testsBtn.setForeground(Color.BLACK);
            }
        }
    }

    public void clearAll() {
        responseHeadersPanel.setHeaders(new LinkedHashMap<>());
        if (protocol.isWebSocketProtocol()) {
            if (webSocketResponsePanel != null) webSocketResponsePanel.clearMessages();
        } else {
            responseBodyPanel.setBodyText(null);
            timingChartPanel.removeAll();
            timingChartPanel.revalidate();
            timingChartPanel.repaint();
            networkLogPanel.clearLog();
        }
        setTestResults(new ArrayList<>());
    }

    private String getSizeText(int bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / 1024.0 / 1024.0);
    }

    // 自定义TabButton，支持底部高亮
    private class TabButton extends JButton {
        private final int tabIndex;

        public TabButton(String text, int tabIndex) {
            super(text);
            this.tabIndex = tabIndex;
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (selectedTabIndex == tabIndex) {
                g.setColor(new Color(141, 188, 223));
                g.fillRect(0, getHeight() - 3, getWidth(), 3);
            }
        }
    }
}
