package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.model.TestResult;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.TimeDisplayUtil;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
            // WebSocket 专用布局
            tabNames = new String[]{I18nUtil.getMessage(MessageKeys.MENU_FILE_LOG), I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_HEADERS)};
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
            cardPanel.add(webSocketResponsePanel, tabNames[0]);
            cardPanel.add(responseHeadersPanel, tabNames[1]);
            networkLogPanel = null;
            timingChartPanel = null;
            responseBodyPanel = null;
            testsPane = null;
            add(cardPanel, BorderLayout.CENTER);
        } else if (protocol == RequestItemProtocolEnum.SSE) {
            // SSE: 只用 HTTP 的响应体和响应头，不再用 StreamTestPanel
            tabNames = new String[]{
                    I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_BODY),
                    I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_HEADERS)
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
            networkLogPanel = null;
            timingChartPanel = null;
            cardPanel.add(responseBodyPanel, tabNames[0]);
            cardPanel.add(responseHeadersPanel, tabNames[1]);
            webSocketResponsePanel = null;
            testsPane = null;
            add(cardPanel, BorderLayout.CENTER);
        } else {
            // HTTP 普通请求
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
            timingChartPanel = new WaterfallChartPanel(new ArrayList<>(), null);
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
        HttpEventInfo info = null;
        if (resp != null && resp.httpEventInfo != null) {
            info = resp.httpEventInfo;
            stages = WaterfallChartPanel.buildStandardStages(info);
        }
        timingChartPanel.setStages(stages);
        timingChartPanel.setHttpEventInfo(info);
    }

    public void setStatus(String statusText, Color color) {
        statusCodeLabel.setText(statusText);
        statusCodeLabel.setForeground(color);
    }

    public void setResponseTime(long ms) {
        responseTimeLabel.setText(String.format(I18nUtil.getMessage(MessageKeys.STATUS_DURATION), TimeDisplayUtil.formatElapsedTime(ms)));
    }

    public void setResponseSize(long bytes) {
        responseSizeLabel.setText(I18nUtil.getMessage(MessageKeys.STATUS_RESPONSE_SIZE).replace("--", getSizeText(bytes)));
    }

    public void setResponseSize(long bytes, HttpEventInfo httpEventInfo) {
        responseSizeLabel.setText(I18nUtil.getMessage(MessageKeys.STATUS_RESPONSE_SIZE).replace("--", getSizeText(bytes)));

        // Remove default tooltip
        responseSizeLabel.setToolTipText(null);

        // Remove existing mouse listeners to avoid duplicates
        MouseListener[] listeners = responseSizeLabel.getMouseListeners();
        for (MouseListener listener : listeners) {
            responseSizeLabel.removeMouseListener(listener);
        }

        // Add custom tooltip behavior
        if (httpEventInfo != null) {
            String tooltip = String.format("<html>" +
                            "<div style='font-family: \"Segoe UI\", Arial, sans-serif; font-size: 9px; width: 160px; padding: 2px;'>" +
                            "<div style='color: #2196F3; font-weight: 600; font-size: 10px; margin-bottom: 4px;'>Response Size</div>" +
                            "<div style='margin-left: 8px; line-height: 1.2;'>" +
                            "<div style='color: #555555; margin-bottom: 1px;'>Headers: <span style='font-weight: 500; color: #333333;'>%,d bytes</span></div>" +
                            "<div style='color: #555555; margin-bottom: 1px;'>Body: <span style='font-weight: 500; color: #333333;'>%,d bytes</span></div>" +
                            "<div style='margin-left: 8px; color: #777777; font-size: 9px;'>Uncompressed: <span style='font-weight: 500; color: #555555;'>%,d bytes</span></div>" +
                            "</div>" +
                            "<div style='border-top: 1px solid #E3E8F0; margin: 6px 0;'></div>" +
                            "<div style='color: #2196F3; font-weight: 600; font-size: 10px; margin-bottom: 4px;'>Request Size</div>" +
                            "<div style='margin-left: 8px; line-height: 1.2;'>" +
                            "<div style='color: #555555; margin-bottom: 1px;'>Headers: <span style='font-weight: 500; color: #333333;'>%,d bytes</span></div>" +
                            "<div style='color: #555555;'>Body: <span style='font-weight: 500; color: #333333;'>%,d bytes</span></div>" +
                            "</div>" +
                            "</div>" +
                            "</html>",
                    httpEventInfo.getHeaderBytesReceived(),
                    httpEventInfo.getBodyBytesReceived(),
                    bytes,
                    httpEventInfo.getHeaderBytesSent(),
                    httpEventInfo.getBodyBytesSent()
            );

            responseSizeLabel.addMouseListener(new MouseAdapter() {
                private Timer showTimer;
                private Timer hideTimer;

                @Override
                public void mouseEntered(MouseEvent e) {
                    // Cancel any pending hide timer
                    if (hideTimer != null) {
                        hideTimer.stop();
                    }

                    // Show tooltip after a short delay (like Postman)
                    showTimer = new Timer(400, evt -> EasyPostmanStyleTooltip.showTooltip(responseSizeLabel, tooltip));
                    showTimer.setRepeats(false);
                    showTimer.start();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    // Cancel show timer if mouse exits before tooltip shows
                    if (showTimer != null) {
                        showTimer.stop();
                    }

                    // Hide tooltip with a small delay to prevent flicker
                    hideTimer = new Timer(200, evt -> EasyPostmanStyleTooltip.hideTooltip());
                    hideTimer.setRepeats(false);
                    hideTimer.start();
                }
            });
        }
    }

    public void setTestResults(List<TestResult> testResults) {
        if (testsPane == null) return; // 防止 NPE
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
            webSocketResponsePanel.clearMessages();
        }

        if (protocol.isSseProtocol()) {
            responseBodyPanel.setBodyText(null);
        }
        if (protocol.isHttpProtocol()) {
            responseBodyPanel.setBodyText(null);
            timingChartPanel.removeAll();
            timingChartPanel.revalidate();
            timingChartPanel.repaint();
            networkLogPanel.clearLog();
        }

        if (testsPane != null) {
            setTestResults(new ArrayList<>());
        }
    }

    private String getSizeText(long bytes) {
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

    // Enhanced tooltip component matching EasyPostman styling
    private static class EasyPostmanStyleTooltip extends JWindow {
        private static EasyPostmanStyleTooltip instance;
        private static Timer autoHideTimer;

        private EasyPostmanStyleTooltip(Window parent) {
            super(parent);
            setAlwaysOnTop(true);
            setType(Window.Type.POPUP);
        }

        public static void showTooltip(Component parent, String html) {
            hideTooltip();

            Window parentWindow = SwingUtilities.getWindowAncestor(parent);
            instance = new EasyPostmanStyleTooltip(parentWindow);

            JLabel content = new JLabel(html);
            content.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11)); // 减小字体
            content.setOpaque(true);
            // Use colors matching EasyPostManColors theme
            content.setBackground(new Color(250, 251, 253)); // Very light background
            content.setForeground(new Color(51, 51, 51)); // Dark text for readability
            content.setBorder(new CompoundBorder(
                    new LineBorder(new Color(200, 210, 220), 1), // Soft border color
                    new EmptyBorder(6, 8, 6, 8) // 减少内边距
            ));

            instance.add(content);
            instance.pack();

            // Smart positioning - above the component, centered
            Point screenLocation = parent.getLocationOnScreen();
            int tooltipWidth = instance.getWidth();
            int tooltipHeight = instance.getHeight();

            // Center horizontally on the component
            int x = screenLocation.x + (parent.getWidth() - tooltipWidth) / 2;
            int y = screenLocation.y - tooltipHeight - 6; // 6px gap above

            // Screen bounds checking
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration()
            );

            int screenWidth = screenSize.width - screenInsets.right;

            // Adjust horizontal position if needed
            if (x + tooltipWidth > screenWidth) {
                x = screenWidth - tooltipWidth - 10;
            }
            if (x < screenInsets.left) {
                x = screenInsets.left + 10;
            }

            // If tooltip doesn't fit above, show below
            if (y < screenInsets.top) {
                y = screenLocation.y + parent.getHeight() + 6;
            }

            instance.setLocation(x, y);

            // Subtle appearance with soft shadow effect
            instance.setOpacity(0.0f);
            instance.setVisible(true);

            // Gentle fade-in animation
            Timer fadeIn = new Timer(30, null);
            fadeIn.addActionListener(e -> {
                float opacity = instance.getOpacity() + 0.08f;
                if (opacity >= 0.96f) {
                    instance.setOpacity(0.96f); // Slightly transparent for elegance
                    fadeIn.stop();
                } else {
                    instance.setOpacity(opacity);
                }
            });
            fadeIn.start();

            // Auto-hide after 6 seconds (balanced timing)
            if (autoHideTimer != null) {
                autoHideTimer.stop();
            }
            autoHideTimer = new Timer(6000, e -> hideTooltip());
            autoHideTimer.setRepeats(false);
            autoHideTimer.start();
        }

        public static void hideTooltip() {
            if (instance != null) {
                // Gentle fade-out animation
                Timer fadeOut = new Timer(30, null);
                fadeOut.addActionListener(e -> {
                    float opacity = instance.getOpacity() - 0.12f;
                    if (opacity <= 0.0f) {
                        instance.setVisible(false);
                        instance.dispose();
                        instance = null;
                        fadeOut.stop();
                    } else {
                        instance.setOpacity(opacity);
                    }
                });
                fadeOut.start();
            }
            if (autoHideTimer != null) {
                autoHideTimer.stop();
                autoHideTimer = null;
            }
        }
    }
}
