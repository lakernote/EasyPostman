package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.common.component.LoadingOverlay;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.FontsUtil;
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
public class ResponsePanel extends JPanel {
    private final JLabel statusCodeLabel;
    private final JLabel responseTimeLabel;
    private final JLabel responseSizeLabel;
    private final JLabel separator1; // 分隔符1：状态码和响应时间之间
    private final JLabel separator2; // 分隔符2：响应时间和响应大小之间
    private final ResponseHeadersPanel responseHeadersPanel;
    private final ResponseBodyPanel responseBodyPanel;
    @Getter
    private final NetworkLogPanel networkLogPanel;
    private final TimelinePanel timelinePanel;
    private final JEditorPane testsPane;
    private final JButton[] tabButtons;
    private EasyComboBox<String> tabComboBox; // 下拉框用于水平布局
    private final JPanel tabBar; // 保存tabBar引用，用于切换
    private final JPanel statusBar; // 保存statusBar引用
    private final JPanel topResponseBar; // 保存topResponseBar引用
    private int selectedTabIndex = 0;
    private final JPanel cardPanel;
    private final String[] tabNames;
    @Getter
    private final RequestItemProtocolEnum protocol;
    @Getter
    private final WebSocketResponsePanel webSocketResponsePanel;
    @Getter
    private final SSEResponsePanel sseResponsePanel;
    private final LoadingOverlay loadingOverlay;
    private boolean isHorizontalLayout = false; // 标记当前是否为水平布局

    public ResponsePanel(RequestItemProtocolEnum protocol, boolean enableSaveButton) {
        this.protocol = protocol;
        setLayout(new BorderLayout());
        tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        // 初始化状态栏组件 - 现代扁平风格
        statusCodeLabel = createModernStatusLabel();
        responseTimeLabel = createModernTimeLabel();
        responseSizeLabel = createModernSizeLabel();

        // 初始化分隔符（默认不显示）
        separator1 = createSeparator();
        separator2 = createSeparator();
        separator1.setVisible(false);
        separator2.setVisible(false);


        // 根据协议类型初始化相应的面板
        if (protocol.isWebSocketProtocol()) {
            // WebSocket 专用布局
            tabNames = new String[]{I18nUtil.getMessage(MessageKeys.MENU_FILE_LOG), I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_HEADERS)};
            tabButtons = new JButton[tabNames.length];
            for (int i = 0; i < tabNames.length; i++) {
                tabButtons[i] = new TabButton(tabNames[i], i);
                tabBar.add(tabButtons[i]);
            }
            statusBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
            // 现代扁平风格：紧凑布局，状态码带彩色背景框
            statusBar.add(statusCodeLabel);
            statusBar.add(separator1);
            statusBar.add(responseTimeLabel);
            statusBar.add(separator2);
            statusBar.add(responseSizeLabel);

            topResponseBar = new JPanel(new BorderLayout());
            topResponseBar.add(tabBar, BorderLayout.WEST);
            topResponseBar.add(statusBar, BorderLayout.EAST);
            cardPanel = new JPanel(new CardLayout());
            webSocketResponsePanel = new WebSocketResponsePanel();
            responseHeadersPanel = new ResponseHeadersPanel();
            cardPanel.add(webSocketResponsePanel, tabNames[0]);
            cardPanel.add(responseHeadersPanel, tabNames[1]);
            networkLogPanel = null;
            timelinePanel = null;
            responseBodyPanel = null;
            testsPane = null;
            sseResponsePanel = null;
        } else if (protocol == RequestItemProtocolEnum.SSE) {
            // SSE: 使用 SSEResponsePanel 和 ResponseHeadersPanel
            tabNames = new String[]{
                    I18nUtil.getMessage(MessageKeys.MENU_FILE_LOG),
                    I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_HEADERS)
            };
            tabButtons = new JButton[tabNames.length];
            for (int i = 0; i < tabNames.length; i++) {
                tabButtons[i] = new TabButton(tabNames[i], i);
                tabBar.add(tabButtons[i]);
            }
            statusBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 3));
            // 现代扁平风格：添加适当间距和分隔符
            statusBar.add(statusCodeLabel);
            statusBar.add(separator1);
            statusBar.add(responseTimeLabel);
            statusBar.add(separator2);
            statusBar.add(responseSizeLabel);
            topResponseBar = new JPanel(new BorderLayout());
            topResponseBar.add(tabBar, BorderLayout.WEST);
            topResponseBar.add(statusBar, BorderLayout.EAST);
            cardPanel = new JPanel(new CardLayout());
            sseResponsePanel = new SSEResponsePanel();
            responseHeadersPanel = new ResponseHeadersPanel();
            cardPanel.add(sseResponsePanel, tabNames[0]);
            cardPanel.add(responseHeadersPanel, tabNames[1]);
            networkLogPanel = null;
            timelinePanel = null;
            responseBodyPanel = null;
            webSocketResponsePanel = null;
            testsPane = null;
        } else {
            // HTTP 普通请求
            tabNames = new String[]{
                    I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_BODY),
                    I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_HEADERS),
                    I18nUtil.getMessage(MessageKeys.TAB_TESTS),
                    I18nUtil.getMessage(MessageKeys.TAB_NETWORK_LOG),
                    I18nUtil.getMessage(MessageKeys.TAB_TIMING),
                    I18nUtil.getMessage(MessageKeys.MENU_FILE_LOG)
            };
            tabButtons = new JButton[tabNames.length];
            for (int i = 0; i < tabButtons.length; i++) {
                tabButtons[i] = new TabButton(tabNames[i], i);
                // 默认情况下HTTP模式不显示日志tab
                if (i == 5) {
                    tabButtons[i].setVisible(false);
                }
                tabBar.add(tabButtons[i]);
            }
            statusBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 3));
            // 现代扁平风格：添加适当间距和分隔符
            statusBar.add(statusCodeLabel);
            statusBar.add(separator1);
            statusBar.add(responseTimeLabel);
            statusBar.add(separator2);
            statusBar.add(responseSizeLabel);

            topResponseBar = new JPanel(new BorderLayout());
            topResponseBar.add(tabBar, BorderLayout.WEST);
            topResponseBar.add(statusBar, BorderLayout.EAST);
            cardPanel = new JPanel(new CardLayout());
            responseBodyPanel = new ResponseBodyPanel(enableSaveButton); // 根据参数决定是否启用保存按钮
            responseBodyPanel.setEnabled(false);
            responseBodyPanel.setBodyText(null);
            responseHeadersPanel = new ResponseHeadersPanel();
            JPanel testsPanel = new JPanel(new BorderLayout());
            // 设置边框
            testsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            testsPane = new JEditorPane();
            testsPane.setContentType("text/html");
            testsPane.setEditable(false);
            JScrollPane testsScrollPane = new JScrollPane(testsPane);
            testsPanel.add(testsScrollPane, BorderLayout.CENTER);
            networkLogPanel = new NetworkLogPanel();
            timelinePanel = new TimelinePanel(new ArrayList<>(), null);
            JScrollPane timelineScrollPanel = new JScrollPane(timelinePanel);
            // 设置边框
            timelineScrollPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            // 按照指定顺序添加到 cardPanel
            // [Response Body] [Response Headers] [Tests] [Network Log] [Timing] [Log]
            cardPanel.add(responseBodyPanel, tabNames[0]);
            cardPanel.add(responseHeadersPanel, tabNames[1]);
            cardPanel.add(testsPanel, tabNames[2]);
            cardPanel.add(networkLogPanel, tabNames[3]);
            cardPanel.add(timelineScrollPanel, tabNames[4]);
            sseResponsePanel = new SSEResponsePanel();
            cardPanel.add(sseResponsePanel, tabNames[5]);
            webSocketResponsePanel = null;
        }

        // 检查初始布局状态，决定使用 tabBar 还是下拉框
        boolean isVertical = SettingManager.isLayoutVertical();
        isHorizontalLayout = !isVertical;

        if (isHorizontalLayout) {
            // 水平布局：使用下拉框替换 tabBar
            topResponseBar.remove(tabBar); // 移除默认的 tabBar

            // 创建下拉框
            tabComboBox = new EasyComboBox<>(getVisibleTabNames(), EasyComboBox.WidthMode.DYNAMIC);
            tabComboBox.setSelectedIndex(0);
            tabComboBox.addActionListener(e -> {
                int selectedVisibleIndex = tabComboBox.getSelectedIndex();
                int actualIndex = getActualTabIndex(selectedVisibleIndex);
                if (actualIndex != selectedTabIndex) {
                    selectedTabIndex = actualIndex;
                    CardLayout cl = (CardLayout) cardPanel.getLayout();
                    cl.show(cardPanel, tabNames[actualIndex]);
                }
            });

            // 创建包含下拉框的面板
            JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            comboPanel.add(tabComboBox);

            topResponseBar.add(comboPanel, BorderLayout.WEST);
        }

        // 创建包含topResponseBar和cardPanel的容器面板
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(topResponseBar, BorderLayout.NORTH);
        contentPanel.add(cardPanel, BorderLayout.CENTER);

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


        // 初始化加载遮罩层
        loadingOverlay = new LoadingOverlay();

        // 使用LayeredPane来叠加遮罩层，覆盖整个内容区域（包括tabs和status bar）
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(new OverlayLayout());

        // 将contentPanel（包含topResponseBar和cardPanel）作为基础层
        layeredPane.add(contentPanel, JLayeredPane.DEFAULT_LAYER);

        // 将loadingOverlay作为顶层
        layeredPane.add(loadingOverlay, JLayeredPane.PALETTE_LAYER);

        // 添加layeredPane到主面板
        add(layeredPane, BorderLayout.CENTER);
    }

    /**
     * 自定义LayoutManager，用于确保遮罩层覆盖整个cardPanel
     */
    private static class OverlayLayout implements LayoutManager2 {

        public OverlayLayout() {
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return parent.getSize();
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(0, 0);
        }

        @Override
        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                int w = parent.getWidth();
                int h = parent.getHeight();
                for (Component comp : parent.getComponents()) {
                    comp.setBounds(0, 0, w, h);
                }
            }
        }

        @Override
        public void addLayoutComponent(Component comp, Object constraints) {
        }

        @Override
        public Dimension maximumLayoutSize(Container target) {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        @Override
        public float getLayoutAlignmentX(Container target) {
            return 0.5f;
        }

        @Override
        public float getLayoutAlignmentY(Container target) {
            return 0.5f;
        }

        @Override
        public void invalidateLayout(Container target) {
        }
    }

    /**
     * 创建现代化的状态码Label - 带彩色圆角边框背景
     */
    private JLabel createModernStatusLabel() {
        JLabel label = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                if (getText() != null && !getText().isEmpty() && !getText().equals("...")) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // 根据状态码确定背景色
                    Color bgColor = getStatusBackgroundColor(getText());
                    g2d.setColor(bgColor);

                    // 绘制圆角矩形背景
                    g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                    g2d.dispose();
                }
                super.paintComponent(g);
            }

            private Color getStatusBackgroundColor(String statusText) {
                if (statusText.startsWith("2")) {
                    // 2xx 成功 - 绿色背景
                    return ModernColors.isDarkTheme()
                            ? new Color(34, 197, 94, 30)  // 半透明绿色
                            : new Color(34, 197, 94, 20);
                } else if (statusText.startsWith("3")) {
                    // 3xx 重定向 - 蓝色背景
                    return ModernColors.isDarkTheme()
                            ? new Color(59, 130, 246, 30)
                            : new Color(59, 130, 246, 20);
                } else if (statusText.startsWith("4")) {
                    // 4xx 客户端错误 - 橙色背景
                    return ModernColors.isDarkTheme()
                            ? new Color(245, 158, 11, 30)
                            : new Color(245, 158, 11, 20);
                } else if (statusText.startsWith("5")) {
                    // 5xx 服务器错误 - 红色背景
                    return ModernColors.isDarkTheme()
                            ? new Color(239, 68, 68, 30)
                            : new Color(239, 68, 68, 20);
                } else {
                    // 其他状态 - 灰色背景
                    return ModernColors.isDarkTheme()
                            ? new Color(100, 116, 139, 30)
                            : new Color(100, 116, 139, 20);
                }
            }
        };

        label.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        label.setOpaque(false);
        // 添加内边距
        label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        label.setToolTipText("Response Status Code");
        return label;
    }

    /**
     * 创建现代化的响应时间Label - 带时钟图标，紧凑样式
     */
    private JLabel createModernTimeLabel() {
        JLabel label = new JLabel();
        label.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        label.setForeground(ModernColors.getTextSecondary());
        label.setToolTipText("Response Time");
        return label;
    }

    /**
     * 创建现代化的响应大小Label - 紧凑样式
     */
    private JLabel createModernSizeLabel() {
        JLabel label = new JLabel();
        label.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        label.setForeground(ModernColors.getTextSecondary());
        label.setToolTipText("Response Size");
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return label;
    }

    /**
     * 创建状态栏项之间的分隔符 - 竖线样式，更紧凑
     */
    private JLabel createSeparator() {
        JLabel separator = new JLabel("•");
        separator.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        separator.setForeground(ModernColors.getTextPrimary());
        return separator;
    }

    public void setResponseTabButtonsEnable(boolean enable) {
        for (JButton btn : tabButtons) {
            btn.setEnabled(enable);
        }
        // 同步设置下拉框的启用状态
        if (tabComboBox != null) {
            tabComboBox.setEnabled(enable);
        }
    }

    public void setResponseBody(HttpResponse resp) {
        if (protocol.isWebSocketProtocol() || protocol.isSseProtocol()) {
            // WebSocket 和 SSE 响应体由专门的面板维护，不做处理
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
            } else {
                headersBtn.setText(I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_HEADERS));
            }
        }
    }

    public void setTiming(HttpResponse resp) {
        if (timelinePanel == null) return;
        List<TimelinePanel.Stage> stages = new ArrayList<>();
        HttpEventInfo info = null;
        if (resp != null && resp.httpEventInfo != null) {
            info = resp.httpEventInfo;
            stages = TimelinePanel.buildStandardStages(info);
        }
        timelinePanel.setStages(stages);
        timelinePanel.setHttpEventInfo(info);
    }

    /**
     * 设置响应状态码
     *
     * @param code HTTP 状态码（如 200, 404, 500）；传 0 或负数表示清空状态码
     */
    public void setStatus(int code) {
        if (code > 0) {
            // 显示状态码
            statusCodeLabel.setText(String.valueOf(code));
            // 根据状态码获取对应的颜色（使用 HttpUtil 工具方法）
            statusCodeLabel.setForeground(HttpUtil.getStatusColor(code));
        } else {
            // 清空状态码
            statusCodeLabel.setText("");
            statusCodeLabel.setForeground(ModernColors.getTextPrimary());
        }

        // 如果状态码有值，显示后续的分隔符
        boolean hasStatus = code > 0;
        separator1.setVisible(hasStatus);
    }

    public void setResponseTime(long ms) {
        // 现代扁平风格：直接显示时间值，无需 "耗时:" 前缀
        responseTimeLabel.setText(TimeDisplayUtil.formatElapsedTime(ms));
        // 使用主题适配的次要文本颜色
        responseTimeLabel.setForeground(ModernColors.getTextSecondary());

        // 如果响应时间有效，显示后续的分隔符
        boolean hasTime = ms >= 0;
        separator2.setVisible(hasTime);
    }

    public void setResponseSize(long bytes, HttpEventInfo httpEventInfo) {
        // 检查响应是否被压缩
        // bytes = 解压后的响应体大小（从 body.bytes() 获取，OkHttp 自动解压）
        // bodyBytesReceived = 网络层实际接收的字节数（从 OkHttp 事件监听器获取）
        //
        // 必须确保 bytes > bodyBytesReceived 才认为是压缩，原因如下：
        // 1. Chunked 编码：bodyBytesReceived 包含 chunk 头部元数据（如 "1a\r\n...data...\r\n"），可能大于实际内容
        // 2. HTTP/2 协议：bodyBytesReceived 包含 frame 头部开销，可能大于实际 payload
        // 3. 统计方式差异：事件监听器可能统计了额外的协议层开销
        // 如果 bodyBytesReceived > bytes，则 savedBytes 会变成负数，这是不合理的
        boolean isCompressed = httpEventInfo != null && bytes > 0 &&
                httpEventInfo.getBodyBytesReceived() > 0 &&
                bytes > httpEventInfo.getBodyBytesReceived();

        // Calculate compression ratio and saved bytes
        double compressionRatio = 0;
        long savedBytes = 0;
        if (isCompressed) {
            compressionRatio = (1 - (double) httpEventInfo.getBodyBytesReceived() / bytes) * 100;
            savedBytes = bytes - httpEventInfo.getBodyBytesReceived();
        }

        // 使用 ModernColors 统一颜色方案
        final Color colorCompressed = ModernColors.SUCCESS;           // 绿色 - 压缩成功
        final Color colorNormal = ModernColors.getTextPrimary();      // 主题适配的文本颜色
        final Color colorHoverCompressed = ModernColors.SUCCESS_DARK; // 深绿色 - 悬停时
        final Color colorHoverNormal = ModernColors.PRIMARY;          // 蓝色 - 悬停时

        // Build label text with compression info
        String sizeText;
        final Color normalColor;
        final Color hoverColor;

        if (isCompressed) {
            // 现代扁平风格：直接显示压缩后的大小和压缩比，无需"大小:"前缀
            sizeText = String.format("%s 📦%.0f%%", getSizeText(httpEventInfo.getBodyBytesReceived()), compressionRatio);
            normalColor = colorCompressed;
            hoverColor = colorHoverCompressed;
        } else {
            // 现代扁平风格：直接显示大小值，无需"大小:"前缀
            sizeText = getSizeText(bytes);
            normalColor = colorNormal;
            hoverColor = colorHoverNormal;
        }

        responseSizeLabel.setText(sizeText);
        responseSizeLabel.setForeground(normalColor);

        // Set cursor to hand when hovering to indicate it's interactive
        responseSizeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Remove default tooltip
        responseSizeLabel.setToolTipText(null);

        // Remove existing mouse listeners to avoid duplicates
        MouseListener[] listeners = responseSizeLabel.getMouseListeners();
        for (MouseListener listener : listeners) {
            responseSizeLabel.removeMouseListener(listener);
        }

        // Add custom tooltip behavior with hover color effects
        if (httpEventInfo != null) {
            // 定义主题自适应的 tooltip 颜色
            String colorTitlePrimary = toHtmlColor(ModernColors.PRIMARY);           // 标题蓝色
            String colorTextSecondary = toHtmlColor(ModernColors.getTextSecondary()); // 次要文本
            String colorTextPrimary = toHtmlColor(ModernColors.getTextPrimary());     // 主要文本
            String colorTextHint = toHtmlColor(ModernColors.getTextHint());           // 提示文本
            String colorSuccess = toHtmlColor(ModernColors.SUCCESS);                  // 成功绿色
            String colorSuccessDark = toHtmlColor(ModernColors.SUCCESS_DARK);         // 深绿色
            String colorBorder = toHtmlColor(ModernColors.getBorderLightColor());     // 边框颜色

            // 压缩信息背景色 - 根据主题调整
            String colorCompressBg = ModernColors.isDarkTheme()
                    ? "rgba(34, 197, 94, 0.15)"   // 暗色主题：半透明绿色
                    : "linear-gradient(135deg, #D1FAE5 0%, #A7F3D0 100%)"; // 亮色主题：渐变绿色

            String tooltip;
            if (isCompressed) {
                // Enhanced tooltip for compressed responses - 主题自适应配色
                tooltip = String.format("<html>" +
                                "<div style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", \"Helvetica Neue\", Arial, sans-serif; font-size: 10px; width: 220px; padding: 4px;'>" +
                                "<div style='color: %s; font-weight: 600; font-size: 11px; margin-bottom: 6px;'>🔽 Response Size</div>" +
                                "<div style='margin-left: 8px; line-height: 1.4;'>" +
                                "<div style='color: %s; margin-bottom: 3px;'>🏷️ Headers: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                                "<div style='color: %s; margin-bottom: 3px;'>📦 Body (Compressed): <span style='font-weight: 600; color: %s;'>%s</span></div>" +
                                "<div style='margin-left: 8px; color: %s; font-size: 9px; margin-bottom: 4px;'>🔓 Uncompressed: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                                "<div style='margin: 4px 0; padding: 6px 8px; background: %s; border-radius: 4px; border-left: 3px solid %s;'>" +
                                "<div style='color: %s; font-weight: 600; font-size: 10px; margin-bottom: 2px;'>✨ Compression Ratio: <span style='color: %s;'>%.1f%%</span></div>" +
                                "<div style='color: %s; font-weight: 600; font-size: 10px;'>💾 Saved: <span style='color: %s;'>%s</span></div>" +
                                "</div>" +
                                "</div>" +
                                "<div style='border-top: 1px solid %s; margin: 6px 0;'></div>" +
                                "<div style='color: %s; font-weight: 600; font-size: 11px; margin-bottom: 6px;'>🔼 Request Size</div>" +
                                "<div style='margin-left: 8px; line-height: 1.4;'>" +
                                "<div style='color: %s; margin-bottom: 3px;'>📋 Headers: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                                "<div style='color: %s;'>📝 Body: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                                "</div>" +
                                "</div>" +
                                "</html>",
                        colorTitlePrimary,  // 标题颜色
                        colorTextSecondary, colorTextPrimary, getSizeText(httpEventInfo.getHeaderBytesReceived()),
                        colorTextSecondary, colorSuccess, getSizeText(httpEventInfo.getBodyBytesReceived()),
                        colorTextHint, colorTextSecondary, getSizeText(bytes),
                        colorCompressBg, colorSuccess,  // 压缩背景和边框
                        colorSuccessDark, colorSuccessDark, compressionRatio,
                        colorSuccessDark, colorSuccessDark, getSizeText(savedBytes),
                        colorBorder,  // 分隔线
                        colorTitlePrimary,  // 请求大小标题
                        colorTextSecondary, colorTextPrimary, getSizeText(httpEventInfo.getHeaderBytesSent()),
                        colorTextSecondary, colorTextPrimary, getSizeText(httpEventInfo.getBodyBytesSent())
                );
            } else {
                // Standard tooltip for non-compressed responses - 主题自适应配色
                tooltip = String.format("<html>" +
                                "<div style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", \"Helvetica Neue\", Arial, sans-serif; font-size: 10px; width: 180px; padding: 4px;'>" +
                                "<div style='color: %s; font-weight: 600; font-size: 11px; margin-bottom: 6px;'>🔽 Response Size</div>" +
                                "<div style='margin-left: 8px; line-height: 1.4;'>" +
                                "<div style='color: %s; margin-bottom: 3px;'>🏷️ Headers: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                                "<div style='color: %s; margin-bottom: 3px;'>📦 Body: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                                "<div style='margin-left: 8px; color: %s; font-size: 9px;'>🔓 Uncompressed: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                                "</div>" +
                                "<div style='border-top: 1px solid %s; margin: 6px 0;'></div>" +
                                "<div style='color: %s; font-weight: 600; font-size: 11px; margin-bottom: 6px;'>🔼 Request Size</div>" +
                                "<div style='margin-left: 8px; line-height: 1.4;'>" +
                                "<div style='color: %s; margin-bottom: 3px;'>📋 Headers: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                                "<div style='color: %s;'>📝 Body: <span style='font-weight: 500; color: %s;'>%s</span></div>" +
                                "</div>" +
                                "</div>" +
                                "</html>",
                        colorTitlePrimary,  // 标题颜色
                        colorTextSecondary, colorTextPrimary, getSizeText(httpEventInfo.getHeaderBytesReceived()),
                        colorTextSecondary, colorTextPrimary, getSizeText(httpEventInfo.getBodyBytesReceived()),
                        colorTextHint, colorTextSecondary, getSizeText(bytes),
                        colorBorder,  // 分隔线
                        colorTitlePrimary,  // 请求大小标题
                        colorTextSecondary, colorTextPrimary, getSizeText(httpEventInfo.getHeaderBytesSent()),
                        colorTextSecondary, colorTextPrimary, getSizeText(httpEventInfo.getBodyBytesSent())
                );
            }

            responseSizeLabel.addMouseListener(new MouseAdapter() {
                private Timer showTimer;
                private Timer hideTimer;

                @Override
                public void mouseEntered(MouseEvent e) {
                    // 悬停时改变颜色，提供视觉反馈
                    responseSizeLabel.setForeground(hoverColor);

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
                    // 鼠标离开时恢复原色
                    responseSizeLabel.setForeground(normalColor);

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
            } else {
                testsBtn.setText(I18nUtil.getMessage(MessageKeys.TAB_TESTS));
            }
        }
    }

    public void clearAll() {
        // 清空状态栏
        setStatus(0); // 清空状态码
        responseTimeLabel.setText("");
        responseSizeLabel.setText("");
        separator2.setVisible(false);

        responseHeadersPanel.setHeaders(new LinkedHashMap<>());
        if (protocol.isWebSocketProtocol()) {
            webSocketResponsePanel.clearMessages();
        }

        if (protocol.isSseProtocol()) {
            sseResponsePanel.clearMessages();
        }
        if (protocol.isHttpProtocol()) {
            responseBodyPanel.setBodyText(null);
            timelinePanel.removeAll();
            timelinePanel.revalidate();
            timelinePanel.repaint();
            networkLogPanel.clearLog();
            networkLogPanel.clearAllDetails();
            sseResponsePanel.clearMessages();
        }

        if (testsPane != null) {
            setTestResults(new ArrayList<>());
        }
    }

    /**
     * 切换Tab按钮，http或sse
     */
    public void switchTabButtonHttpOrSse(String type) {
        if ("http".equals(type)) {
            tabButtons[0].setVisible(true);
            tabButtons[0].doClick();
            tabButtons[5].setVisible(false);
        } else {
            tabButtons[0].setVisible(false);
            tabButtons[5].setVisible(true);
            tabButtons[5].doClick();
        }
    }

    /**
     * 显示加载遮罩
     */
    public void showLoadingOverlay() {
        if (loadingOverlay != null) {
            SwingUtilities.invokeLater(loadingOverlay::showLoading);
        }
    }

    /**
     * 隐藏加载遮罩
     */
    public void hideLoadingOverlay() {
        if (loadingOverlay != null) {
            SwingUtilities.invokeLater(loadingOverlay::hideLoading);
        }
    }

    private String getSizeText(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / 1024.0 / 1024.0);
    }

    /**
     * 将 Color 转换为 HTML 颜色代码
     */
    private String toHtmlColor(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
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
            content.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
            content.setOpaque(true);
            // 使用 ModernColors 主题自适应背景色和边框色
            content.setBackground(ModernColors.getCardBackgroundColor()); // 卡片背景色
            content.setForeground(ModernColors.getTextPrimary()); // 主要文本颜色
            content.setBorder(new CompoundBorder(
                    new LineBorder(ModernColors.getBorderMediumColor(), 1), // 主题适配边框
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

            // Gentle fade-in animation with null check
            Timer fadeIn = new Timer(30, null);
            fadeIn.addActionListener(e -> {
                if (instance != null) { // 添加null检查
                    float opacity = instance.getOpacity() + 0.08f;
                    if (opacity >= 0.96f) {
                        instance.setOpacity(0.96f); // Slightly transparent for elegance
                        fadeIn.stop();
                    } else {
                        instance.setOpacity(opacity);
                    }
                } else {
                    fadeIn.stop(); // 如果instance为null，停止动画
                }
            });
            fadeIn.start();

            // Auto-hide after 10 seconds (balanced timing)
            if (autoHideTimer != null) {
                autoHideTimer.stop();
            }
            autoHideTimer = new Timer(10000, e -> hideTooltip());
            autoHideTimer.setRepeats(false);
            autoHideTimer.start();
        }

        public static void hideTooltip() {
            if (instance != null) {
                // Gentle fade-out animation with null check
                Timer fadeOut = new Timer(30, null);
                fadeOut.addActionListener(e -> {
                    if (instance != null) { // 添加null检查
                        float opacity = instance.getOpacity() - 0.12f;
                        if (opacity <= 0.0f) {
                            instance.setVisible(false);
                            instance.dispose();
                            instance = null;
                            fadeOut.stop();
                        } else {
                            instance.setOpacity(opacity);
                        }
                    } else {
                        fadeOut.stop(); // 如果instance为null，停止动画
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


    /**
     * 获取保存响应按钮
     * 代理到 ResponseBodyPanel 的保存按钮
     */
    public JButton getSaveResponseButton() {
        if (responseBodyPanel != null) {
            return responseBodyPanel.getSaveResponseButton();
        }
        return null;
    }

    /**
     * 设置响应体面板的启用状态
     */
    public void setResponseBodyEnabled(boolean enabled) {
        if (responseBodyPanel != null) {
            responseBodyPanel.setEnabled(enabled);
        }
    }

    /**
     * 切换到指定索引的 tab
     *
     * @param tabIndex tab 索引（0-based）
     */
    public void switchToTab(int tabIndex) {
        if (tabIndex < 0 || tabIndex >= tabButtons.length) {
            return;
        }

        if (tabButtons[tabIndex].isVisible() && tabButtons[tabIndex].isEnabled()) {
            tabButtons[tabIndex].doClick();
        }
    }


    /**
     * 更新请求详情（委托给 NetworkLogPanel）
     */
    public void setRequestDetails(PreparedRequest request) {
        if (networkLogPanel != null) {
            networkLogPanel.setRequestDetails(request);
        }
    }

    /**
     * 更新响应详情（委托给 NetworkLogPanel）
     */
    public void setResponseDetails(HttpResponse response) {
        if (networkLogPanel != null) {
            networkLogPanel.setResponseDetails(response);
        }
    }

    /**
     * 根据布局方向切换Tab显示方式
     *
     * @param isVertical true=垂直布局（上下），false=水平布局（左右）
     */
    public void updateLayoutOrientation(boolean isVertical) {
        // 如果布局没有变化，直接返回
        boolean newHorizontalLayout = !isVertical;
        if (this.isHorizontalLayout == newHorizontalLayout) {
            return;
        }
        this.isHorizontalLayout = newHorizontalLayout;

        if (topResponseBar == null || tabBar == null || statusBar == null) {
            return;
        }

        // 移除旧的组件
        topResponseBar.removeAll();

        if (isHorizontalLayout) {
            // 水平布局：使用下拉框
            if (tabComboBox == null) {
                // 创建下拉框（只创建一次）
                tabComboBox = new EasyComboBox<>(getVisibleTabNames(), EasyComboBox.WidthMode.DYNAMIC);
                tabComboBox.setSelectedIndex(getVisibleTabIndex(selectedTabIndex));
                // 同步当前 tab buttons 的启用状态
                tabComboBox.setEnabled(tabButtons.length > 0 && tabButtons[0].isEnabled());
                tabComboBox.addActionListener(e -> {
                    int selectedVisibleIndex = tabComboBox.getSelectedIndex();
                    int actualIndex = getActualTabIndex(selectedVisibleIndex);
                    if (actualIndex != selectedTabIndex) {
                        selectedTabIndex = actualIndex;
                        CardLayout cl = (CardLayout) cardPanel.getLayout();
                        cl.show(cardPanel, tabNames[actualIndex]);
                    }
                });
            } else {
                // 更新下拉框选项和选中项
                tabComboBox.removeAllItems();
                String[] visibleNames = getVisibleTabNames();
                for (String name : visibleNames) {
                    tabComboBox.addItem(name);
                }
                tabComboBox.setSelectedIndex(getVisibleTabIndex(selectedTabIndex));
                // 同步当前 tab buttons 的启用状态
                tabComboBox.setEnabled(tabButtons.length > 0 && tabButtons[0].isEnabled());
            }

            // 创建包含下拉框的面板
            JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            comboPanel.add(tabComboBox);

            topResponseBar.add(comboPanel, BorderLayout.WEST);
            topResponseBar.add(statusBar, BorderLayout.EAST);
        } else {
            // 垂直布局：使用Tab按钮
            topResponseBar.add(tabBar, BorderLayout.WEST);
            topResponseBar.add(statusBar, BorderLayout.EAST);
        }

        topResponseBar.revalidate();
        topResponseBar.repaint();
    }

    /**
     * 获取可见的Tab名称数组
     */
    private String[] getVisibleTabNames() {
        List<String> visibleNames = new ArrayList<>();
        for (int i = 0; i < tabButtons.length; i++) {
            if (tabButtons[i].isVisible()) {
                visibleNames.add(tabNames[i]);
            }
        }
        return visibleNames.toArray(new String[0]);
    }

    /**
     * 将实际Tab索引转换为可见Tab索引
     */
    private int getVisibleTabIndex(int actualIndex) {
        int visibleIndex = 0;
        for (int i = 0; i < actualIndex && i < tabButtons.length; i++) {
            if (tabButtons[i].isVisible()) {
                visibleIndex++;
            }
        }
        return visibleIndex;
    }

    /**
     * 将可见Tab索引转换为实际Tab索引
     */
    private int getActualTabIndex(int visibleIndex) {
        int count = 0;
        for (int i = 0; i < tabButtons.length; i++) {
            if (tabButtons[i].isVisible()) {
                if (count == visibleIndex) {
                    return i;
                }
                count++;
            }
        }
        return 0;
    }

}


