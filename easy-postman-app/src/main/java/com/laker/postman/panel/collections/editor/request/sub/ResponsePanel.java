package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.script.model.TestResult;
import com.laker.postman.request.model.RequestItemProtocolEnum;


import com.laker.postman.common.component.LoadingOverlay;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.service.setting.SettingManager;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 响应部分面板，包含响应体、响应头、测试结果、网络日志、耗时等
 */
public class ResponsePanel extends JPanel {

    private final ResponseStatusBar statusBar;
    private final ResponseHeadersPanel responseHeadersPanel;
    private final ResponseBodyPanel responseBodyPanel;
    @Getter
    private final NetworkLogPanel networkLogPanel;
    private final TimelinePanel timelinePanel;
    private final JEditorPane testsPane;
    private final JButton[] tabButtons;
    private final JPanel tabBar; // 保存tabBar引用，用于切换
    private final JPanel topResponseBar; // 保存topResponseBar引用
    private final JPanel cardPanel;
    private final String[] tabNames;
    private final ResponseTabBadgeController tabBadgeController;
    private final ResponseTabNavigationController tabNavigationController;
    @Getter
    private final RequestItemProtocolEnum protocol;
    @Getter
    private final WebSocketResponsePanel webSocketResponsePanel;
    @Getter
    private final SSEResponsePanel sseResponsePanel;
    private final LoadingOverlay loadingOverlay;
    private boolean hasResponseData = false;

    public ResponsePanel(RequestItemProtocolEnum protocol, boolean enableSaveButton) {
        this.protocol = protocol;
        setLayout(new BorderLayout());
        tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusBar = new ResponseStatusBar();

        // 根据协议类型初始化相应的面板（使用TabBarBuilder简化）
        if (protocol.isWebSocketProtocol()) {
            // WebSocket 专用布局
            TabBarBuilder.TabConfig tabConfig = TabBarBuilder.createWebSocketTabs();
            tabNames = tabConfig.tabNames;
            tabButtons = TabBarBuilder.createModernTabButtons(tabNames);
            TabBarBuilder.addButtonsToTabBar(tabBar, tabButtons, tabConfig.initialVisibility);

            topResponseBar = new JPanel(new BorderLayout());
            topResponseBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()));
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
            // SSE: 使用事件流和响应头
            TabBarBuilder.TabConfig tabConfig = TabBarBuilder.createSSETabs();
            tabNames = tabConfig.tabNames;
            tabButtons = TabBarBuilder.createModernTabButtons(tabNames);
            TabBarBuilder.addButtonsToTabBar(tabBar, tabButtons, tabConfig.initialVisibility);

            topResponseBar = new JPanel(new BorderLayout());
            topResponseBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()));
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
            tabButtons = TabBarBuilder.createModernTabButtons(tabNames);
            TabBarBuilder.addButtonsToTabBar(tabBar, tabButtons, tabConfig.initialVisibility);

            topResponseBar = new JPanel(new BorderLayout());
            topResponseBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()));
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
        tabBadgeController = new ResponseTabBadgeController(tabButtons);
        tabNavigationController = new ResponseTabNavigationController(
                topResponseBar,
                tabBar,
                statusBar,
                cardPanel,
                tabButtons,
                tabNames
        );
        tabNavigationController.initializeFirstVisibleTab();

        // 检查初始布局状态，决定使用 tabBar 还是下拉框
        boolean isVertical = SettingManager.isLayoutVertical();
        tabNavigationController.installInitialLayout(!isVertical);

        // 使用TabBarBuilder绑定tab事件
        tabNavigationController.bindTabActions();

        // 默认所有按钮不可用
        setResponseTabButtonsEnable(false);

        // 初始化加载遮罩层
        loadingOverlay = new LoadingOverlay();

        // loading overlay 只覆盖 cardPanel，不遮顶部 tab/status bar。
        // 这样请求执行中仍然可以切换响应标签、查看状态信息。
        JLayeredPane cardLayeredPane = new JLayeredPane();
        cardLayeredPane.setLayout(new ResponseCardOverlayLayout());
        cardLayeredPane.add(cardPanel, JLayeredPane.DEFAULT_LAYER);
        cardLayeredPane.add(loadingOverlay, JLayeredPane.PALETTE_LAYER);

        add(topResponseBar, BorderLayout.NORTH);
        add(cardLayeredPane, BorderLayout.CENTER);
    }

    public void setResponseTabButtonsEnable(boolean enable) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setResponseTabButtonsEnable(enable));
            return;
        }
        if (tabNavigationController.enabledStateMatches(enable)) {
            return;
        }
        tabNavigationController.setEnabled(enable);
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
        tabBadgeController.updateResponseHeadersCount(resp.headers != null ? resp.headers.size() : 0);
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
        statusBar.setStatus(code);
    }

    public void setResponseTime(long ms) {
        statusBar.setResponseTime(ms);
    }

    /**
     * 设置响应大小显示（带完整响应对象，可读取 Content-Encoding）
     */
    public void setResponseSize(long bytes, HttpResponse httpResponse) {
        statusBar.setResponseSize(bytes, httpResponse);
    }

    public void setTestResults(List<TestResult> testResults) {
        if (testsPane == null) return; // 防止 NPE
        String html = HttpHtmlRenderer.renderTestResults(testResults);
        testsPane.setText(html);
        testsPane.setCaretPosition(0);
        tabBadgeController.updateTestResults(testResults);
    }

    public void clearAll() {
        hasResponseData = false;
        // 清空状态栏
        statusBar.clear();

        responseHeadersPanel.setHeaders(new LinkedHashMap<>());
        tabBadgeController.updateResponseHeadersCount(0);
        if (protocol.isWebSocketProtocol()) {
            webSocketResponsePanel.clearMessages();
        }

        if (protocol.isSseProtocol()) {
            sseResponsePanel.clearMessages();
        }
        if (protocol.isHttpProtocol()) {
            responseBodyPanel.setBodyText(null);
            clearTiming();
            networkLogPanel.clearLog();
            networkLogPanel.clearAllDetails();
            sseResponsePanel.clearMessages();
        }

        if (testsPane != null) {
            setTestResults(new ArrayList<>());
        }
    }

    public void clearInFlightRequestDetails() {
        if (networkLogPanel != null) {
            networkLogPanel.clearLog();
            networkLogPanel.clearAllDetails();
        }
        clearTiming();
    }

    public boolean hasResponseData() {
        return hasResponseData;
    }

    public void markResponseDataLoaded() {
        hasResponseData = true;
    }

    /**
     * 切换Tab按钮显示（HTTP或SSE）
     *
     * @param type "http" 显示HTTP相关tabs，"sse" 显示SSE相关tabs
     */
    public void switchTabButtonHttpOrSse(String type) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> switchTabButtonHttpOrSse(type));
            return;
        }
        if (!protocol.isHttpProtocol()) {
            return;
        }
        tabNavigationController.switchHttpOrSse(type);
    }

    private void clearTiming() {
        if (timelinePanel == null) {
            return;
        }
        timelinePanel.setStages(new ArrayList<>());
        timelinePanel.setHttpEventInfo(null);
    }

    /**
     * 显示加载遮罩
     */
    public void showLoadingOverlay() {
        if (loadingOverlay == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            loadingOverlay.showLoading();
        } else {
            SwingUtilities.invokeLater(loadingOverlay::showLoading);
        }
    }

    /**
     * 隐藏加载遮罩
     */
    public void hideLoadingOverlay() {
        if (loadingOverlay == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            // 请求完成后的 UI 刷新本来就在 EDT 上。
            // 这里必须立即隐藏，否则会被排到大响应渲染之后，看起来像“转圈卡住不转”。
            loadingOverlay.hideLoading();
        } else {
            SwingUtilities.invokeLater(loadingOverlay::hideLoading);
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
        tabNavigationController.switchToTab(tabIndex);
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
        tabNavigationController.updateLayoutOrientation(isVertical);
    }

}
