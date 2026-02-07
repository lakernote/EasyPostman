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

    // ==================== Tab索引常量 ====================
    private static final int TAB_INDEX_RESPONSE_BODY = 0;
    private static final int TAB_INDEX_RESPONSE_HEADERS = 1;
    private static final int TAB_INDEX_TESTS = 2;
    private static final int TAB_INDEX_LOG = 5;

    // ==================== UI组件 ====================
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


        // 根据协议类型初始化相应的面板（使用TabBarBuilder简化）
        if (protocol.isWebSocketProtocol()) {
            // WebSocket 专用布局
            TabBarBuilder.TabConfig tabConfig = TabBarBuilder.createWebSocketTabs();
            tabNames = tabConfig.tabNames;
            tabButtons = createModernTabButtons(tabNames);
            TabBarBuilder.addButtonsToTabBar(tabBar, tabButtons, tabConfig.initialVisibility);
            // 初始化第一个可见tab为选中状态
            initializeFirstSelectedTab(tabButtons);

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
            TabBarBuilder.TabConfig tabConfig = TabBarBuilder.createSSETabs();
            tabNames = tabConfig.tabNames;
            tabButtons = createModernTabButtons(tabNames);
            TabBarBuilder.addButtonsToTabBar(tabBar, tabButtons, tabConfig.initialVisibility);
            // 初始化第一个可见tab为选中状态
            initializeFirstSelectedTab(tabButtons);

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
            TabBarBuilder.TabConfig tabConfig = TabBarBuilder.createHttpTabs();
            tabNames = tabConfig.tabNames;
            tabButtons = createModernTabButtons(tabNames);
            TabBarBuilder.addButtonsToTabBar(tabBar, tabButtons, tabConfig.initialVisibility);
            // 初始化第一个可见tab为选中状态
            initializeFirstSelectedTab(tabButtons);

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

        // 使用TabBarBuilder绑定tab事件
        TabBarBuilder.bindTabActions(tabButtons, tabNames, cardPanel, this::onTabSelected);

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

    // ==================== Tab相关辅助方法 ====================

    /**
     * 创建现代化的Tab按钮数组
     */
    private JButton[] createModernTabButtons(String[] names) {
        JButton[] buttons = new JButton[names.length];
        for (int i = 0; i < names.length; i++) {
            buttons[i] = new ModernTabButton(names[i], i);
        }
        return buttons;
    }

    /**
     * 初始化第一个可见的tab为选中状态
     */
    private void initializeFirstSelectedTab(JButton[] buttons) {
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].isVisible() && buttons[i] instanceof ModernTabButton modernTabButton) {
                modernTabButton.updateSelectedIndex(i);
                selectedTabIndex = i;
                break;
            }
        }
    }

    /**
     * Tab选中回调
     */
    private void onTabSelected(int tabIndex) {
        selectedTabIndex = tabIndex;
        // 更新所有ModernTabButton的选中状态
        for (JButton btn : tabButtons) {
            if (btn instanceof ModernTabButton modernTabButton) {
                modernTabButton.updateSelectedIndex(selectedTabIndex);
            } else {
                // 兼容旧的TabButton
                btn.repaint();
            }
        }
    }

    /**
     * 自定义LayoutManager，用于确保遮罩层覆盖整个cardPanel
     */
    private static class OverlayLayout implements LayoutManager2 {

        public OverlayLayout() {
            // 默认构造函数
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
            // 此布局不需要根据名称添加组件
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            // 此布局不需要移除组件逻辑
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
            // 此布局不需要约束条件
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
            // 此布局不需要缓存，无需失效逻辑
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
        if (tabButtons.length > TAB_INDEX_RESPONSE_HEADERS) {
            JButton headersBtn = tabButtons[TAB_INDEX_RESPONSE_HEADERS];
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

    /**
     * 设置响应大小显示
     * 重构后使用Helper类简化代码逻辑
     */
    public void setResponseSize(long bytes, HttpEventInfo httpEventInfo) {
        // 使用Helper类计算大小信息
        ResponseSizeCalculator.SizeInfo sizeInfo = ResponseSizeCalculator.calculate(bytes, httpEventInfo);

        // 更新标签显示
        updateSizeLabel(sizeInfo);

        // 添加tooltip（如果有httpEventInfo）
        if (httpEventInfo != null) {
            attachSizeTooltip(bytes, httpEventInfo, sizeInfo);
        }
    }

    /**
     * 更新响应大小标签
     */
    private void updateSizeLabel(ResponseSizeCalculator.SizeInfo sizeInfo) {
        responseSizeLabel.setText(sizeInfo.getDisplayText());
        responseSizeLabel.setForeground(sizeInfo.getNormalColor());
        responseSizeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        responseSizeLabel.setToolTipText(null);

        // 移除旧的监听器
        for (MouseListener listener : responseSizeLabel.getMouseListeners()) {
            responseSizeLabel.removeMouseListener(listener);
        }
    }

    /**
     * 添加响应大小的tooltip和鼠标悬停效果
     */
    private void attachSizeTooltip(long bytes, HttpEventInfo httpEventInfo, ResponseSizeCalculator.SizeInfo sizeInfo) {
        // 使用Helper类生成tooltip HTML
        String tooltip = ResponseTooltipBuilder.buildSizeTooltip(bytes, httpEventInfo, sizeInfo);

        // 添加鼠标监听器实现悬停效果
        responseSizeLabel.addMouseListener(new MouseAdapter() {
            private Timer showTimer;
            private Timer hideTimer;

            @Override
            public void mouseEntered(MouseEvent e) {
                // 悬停时改变颜色
                responseSizeLabel.setForeground(sizeInfo.getHoverColor());

                if (hideTimer != null) {
                    hideTimer.stop();
                }

                showTimer = new Timer(400, evt -> EasyPostmanStyleTooltip.showTooltip(responseSizeLabel, tooltip));
                showTimer.setRepeats(false);
                showTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 恢复原色
                responseSizeLabel.setForeground(sizeInfo.getNormalColor());

                if (showTimer != null) {
                    showTimer.stop();
                }

                hideTimer = new Timer(200, evt -> EasyPostmanStyleTooltip.hideTooltip());
                hideTimer.setRepeats(false);
                hideTimer.start();
            }
        });
    }

    public void setTestResults(List<TestResult> testResults) {
        if (testsPane == null) return; // 防止 NPE
        String html = HttpHtmlRenderer.renderTestResults(testResults);
        testsPane.setText(html);
        testsPane.setCaretPosition(0);
        // 动态设置Tests按钮文本和颜色
        if (tabButtons.length > TAB_INDEX_TESTS) {
            JButton testsBtn = tabButtons[TAB_INDEX_TESTS];
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
     * 切换Tab按钮显示（HTTP或SSE）
     *
     * @param type "http" 显示HTTP相关tabs，"sse" 显示SSE相关tabs
     */
    public void switchTabButtonHttpOrSse(String type) {
        if ("http".equals(type)) {
            showHttpTabs();
        } else {
            showSSETabs();
        }
    }

    /**
     * 显示HTTP相关的tabs
     */
    private void showHttpTabs() {
        tabButtons[TAB_INDEX_RESPONSE_BODY].setVisible(true);
        tabButtons[TAB_INDEX_RESPONSE_BODY].doClick();
        tabButtons[TAB_INDEX_LOG].setVisible(false);
    }

    /**
     * 显示SSE相关的tabs
     */
    private void showSSETabs() {
        tabButtons[TAB_INDEX_RESPONSE_BODY].setVisible(false);
        tabButtons[TAB_INDEX_LOG].setVisible(true);
        tabButtons[TAB_INDEX_LOG].doClick();
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


