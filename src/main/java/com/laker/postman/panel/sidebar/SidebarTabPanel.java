package com.laker.postman.panel.sidebar;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.SidebarTab;
import com.laker.postman.model.TabInfo;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.panel.sidebar.cookie.CookieManagerDialog;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.SystemUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;


/**
 * 左侧标签页面板
 */
@Slf4j
public class SidebarTabPanel extends SingletonBasePanel {

    // Constants
    private static final String ICON_LABEL_NAME = "iconLabel";
    private static final String TITLE_LABEL_NAME = "titleLabel";
    private static final String BUTTON_FOREGROUND_KEY = "Button.foreground";

    @Getter
    private JTabbedPane tabbedPane;
    @Getter
    private transient List<TabInfo> tabInfos;
    private JPanel consoleContainer;
    private JPanel bottomLeftPanel;  // 底部栏左侧面板缓存
    private JPanel bottomRightPanel; // 底部栏右侧面板缓存
    private JLabel consoleLabel;
    private JLabel sidebarToggleLabel; // 侧边栏展开/收起按钮
    private JLabel cookieLabel;
    private JLabel versionLabel;
    private ConsolePanel consolePanel;
    private boolean sidebarExpanded = false; // 侧边栏展开状态
    private CookieManagerDialog cookieManagerDialog; // Cookie管理器对话框实例
    private int lastSelectedIndex = -1; // 记录上一次选中的索引，用于优化颜色更新

    // 字体缓存，避免重复创建
    private Font normalFont;      // PLAIN 12 - Tab文本和版本号共用
    private Font boldFont;        // BOLD 12 - Tab文本选中态和底部栏共用
    private Font bottomBarFont;   // BOLD 12 - 底部栏字体（与boldFont相同，为了语义清晰保留）

    // 自适应宽度缓存
    private int calculatedExpandedTabWidth = -1; // 计算后的展开状态tab宽度

    // 性能优化：缓存绘制时使用的颜色对象，避免重复创建
    private transient Color cachedBgColor;
    private transient GradientPaint cachedGradient;
    private transient int lastIndicatorHeight = -1; // 用于判断是否需要重新创建渐变

    @Override
    protected void initUI() {
        // 初始化字体缓存
        normalFont = FontsUtil.getDefaultFont(Font.PLAIN);  // Tab文本和版本号共用
        boldFont = FontsUtil.getDefaultFont(Font.BOLD);     // Tab选中态共用
        bottomBarFont = boldFont; // 底部栏使用相同的 BOLD 12 字体

        // 先读取侧边栏展开状态
        sidebarExpanded = SettingManager.isSidebarExpanded();
        setLayout(new BorderLayout());

        // 1. 创建标签页
        tabbedPane = createModernTabbedPane();
        tabInfos = new ArrayList<>();

        // 使用枚举初始化所有 Tab，简化代码并集中管理
        for (SidebarTab tab : SidebarTab.values()) {
            tabInfos.add(tab.toTabInfo());
        }

        // Add tabs to the JTabbedPane
        for (int i = 0; i < tabInfos.size(); i++) {
            TabInfo info = tabInfos.get(i);
            tabbedPane.addTab(info.title, new JPanel());
            // 设置自定义 tab 组件以实现图标在上、文本在下的布局
            tabbedPane.setTabComponentAt(i, createTabComponent(info.title, info.icon));
        }

        // 默认设置选中第一个标签
        tabbedPane.setSelectedIndex(0);

        // 2. 控制台日志区和底部栏
        consoleContainer = new JPanel(new BorderLayout());
        consoleContainer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ModernColors.getDividerBorderColor()));
        consoleContainer.setOpaque(false);
        initBottomBar();
        consolePanel = SingletonFactory.getInstance(ConsolePanel.class);
        // 注册关闭按钮事件
        consolePanel.setCloseAction(e -> setConsoleExpanded(false));
        setConsoleExpanded(false);
    }

    private void setConsoleExpanded(boolean expanded) {
        JSplitPane splitPane;
        removeAll();
        if (expanded) {
            consoleContainer.removeAll();
            consoleContainer.add(consolePanel, BorderLayout.CENTER);
            splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, consoleContainer);
            splitPane.setDividerSize(2);
            splitPane.setBorder(null);
            splitPane.setOneTouchExpandable(true);
            splitPane.setResizeWeight(1.0);
            splitPane.setMinimumSize(new Dimension(0, 10));
            tabbedPane.setMinimumSize(new Dimension(0, 30));
            consoleContainer.setMinimumSize(new Dimension(0, 30));
            add(splitPane, BorderLayout.CENTER);
            revalidate();
            repaint();
            SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(splitPane.getHeight() - 300));
        } else {
            add(tabbedPane, BorderLayout.CENTER);
            consoleContainer.removeAll();
            // 使用缓存的面板，避免重复创建
            consoleContainer.add(bottomLeftPanel, BorderLayout.WEST);
            consoleContainer.add(bottomRightPanel, BorderLayout.EAST);
            add(consoleContainer, BorderLayout.SOUTH);
            revalidate();
            repaint();
        }
    }

    /**
     * 初始化底部栏，包括控制台标签和版本标签
     */
    private void initBottomBar() {
        createSidebarToggleLabel();
        createConsoleLabel();
        createCookieLabel();
        createVersionLabel();

        // 初始化底部栏面板，避免每次展开/收起时重复创建
        bottomLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottomLeftPanel.setOpaque(false);
        bottomLeftPanel.add(sidebarToggleLabel);
        bottomLeftPanel.add(consoleLabel);

        bottomRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomRightPanel.setOpaque(false);
        bottomRightPanel.add(cookieLabel);
        bottomRightPanel.add(versionLabel);
    }

    /**
     * 创建侧边栏展开/收起按钮
     */
    private void createSidebarToggleLabel() {
        FlatSVGIcon toggleIcon = new FlatSVGIcon("icons/sidebar-toggle.svg", 20, 20);
        toggleIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor(BUTTON_FOREGROUND_KEY)));
        sidebarToggleLabel = new JLabel(toggleIcon);
        sidebarToggleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sidebarToggleLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
        sidebarToggleLabel.setFocusable(true);
        sidebarToggleLabel.setEnabled(true);
        // 优化提示文本
        sidebarToggleLabel.setToolTipText(sidebarExpanded ? "Collapse sidebar" : "Expand sidebar");
        sidebarToggleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                toggleSidebarExpansion();
            }
        });
    }

    /**
     * 切换侧边栏展开/收起状态
     */
    private void toggleSidebarExpansion() {
        sidebarExpanded = !sidebarExpanded;
        SettingManager.setSidebarExpanded(sidebarExpanded);
        // 更新提示文本
        sidebarToggleLabel.setToolTipText(sidebarExpanded ? "Collapse sidebar" : "Expand sidebar");
        recreateTabbedPane();
    }

    /**
     * 创建控制台标签
     */
    private void createConsoleLabel() {
        consoleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CONSOLE_TITLE));
        FlatSVGIcon consoleIcon = new FlatSVGIcon("icons/console.svg", 20, 20);
        consoleIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor(BUTTON_FOREGROUND_KEY)));
        consoleLabel.setIcon(consoleIcon);
        consoleLabel.setFont(bottomBarFont); // 使用缓存的字体
        consoleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        consoleLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        consoleLabel.setFocusable(true);
        consoleLabel.setEnabled(true);
        consoleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                setConsoleExpanded(true);
            }
        });
    }

    /**
     * 创建 Cookie 标签
     */
    private void createCookieLabel() {
        cookieLabel = new JLabel(I18nUtil.getMessage(MessageKeys.COOKIES_TITLE));
        FlatSVGIcon cookieIcon = new FlatSVGIcon("icons/cookie.svg", 20, 20);
        cookieLabel.setIcon(cookieIcon);
        cookieLabel.setFont(bottomBarFont); // 使用缓存的字体
        cookieLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cookieLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        cookieLabel.setFocusable(true);
        cookieLabel.setEnabled(true);
        cookieLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showCookieManagerDialog();
            }
        });
    }

    /**
     * 显示 Cookie 管理器对话框
     */
    private void showCookieManagerDialog() {
        // 如果对话框已存在且可见，则将其置于前台
        if (cookieManagerDialog != null && cookieManagerDialog.isVisible()) {
            cookieManagerDialog.toFront();
            cookieManagerDialog.requestFocus();
            return;
        }

        // 创建新对话框
        Window window = SwingUtilities.getWindowAncestor(this);
        cookieManagerDialog = new CookieManagerDialog(window);
        cookieManagerDialog.setVisible(true);
    }

    /**
     * 创建版本号标签
     */
    private void createVersionLabel() {
        versionLabel = new JLabel(SystemUtil.getCurrentVersion());
        versionLabel.setFont(normalFont); // 使用缓存的字体（与normalFont共用）
        versionLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 12));
        versionLabel.setToolTipText("EasyPostman version");
    }

    @Override
    public void updateUI() {
        super.updateUI();

        // 1. 更新字体缓存（字体切换时）
        normalFont = FontsUtil.getDefaultFont(Font.PLAIN);
        boldFont = FontsUtil.getDefaultFont(Font.BOLD);
        bottomBarFont = boldFont;

        // 2. 清除颜色和渐变缓存（主题切换时）
        cachedBgColor = null;
        cachedGradient = null;
        lastIndicatorHeight = -1;

        // 3. 清除宽度缓存（字体改变会影响文本宽度）
        calculatedExpandedTabWidth = -1;

        // 4. 重新应用自定义的 TabbedPane UI（super.updateUI() 会重置它）
        if (tabbedPane != null) {
            // 重新创建并设置自定义 UI
            recreateTabbedPaneUI();
        }

        // 5. 更新底部栏组件
        if (consoleLabel != null) {
            consoleLabel.setText(I18nUtil.getMessage(MessageKeys.CONSOLE_TITLE));
            consoleLabel.setFont(bottomBarFont);
            // 更新图标颜色
            FlatSVGIcon consoleIcon = new FlatSVGIcon("icons/console.svg", 20, 20);
            consoleIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor(BUTTON_FOREGROUND_KEY)));
            consoleLabel.setIcon(consoleIcon);
        }

        if (cookieLabel != null) {
            cookieLabel.setText(I18nUtil.getMessage(MessageKeys.COOKIES_TITLE));
            cookieLabel.setFont(bottomBarFont);
            // 更新图标
            FlatSVGIcon cookieIcon = new FlatSVGIcon("icons/cookie.svg", 20, 20);
            cookieLabel.setIcon(cookieIcon);
        }

        if (versionLabel != null) {
            versionLabel.setFont(normalFont);
        }

        if (sidebarToggleLabel != null) {
            // 更新图标颜色
            FlatSVGIcon toggleIcon = new FlatSVGIcon("icons/sidebar-toggle.svg", 20, 20);
            toggleIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor(BUTTON_FOREGROUND_KEY)));
            sidebarToggleLabel.setIcon(toggleIcon);
            // 更新提示文本
            sidebarToggleLabel.setToolTipText(sidebarExpanded ? "Collapse sidebar" : "Expand sidebar");
        }

        // 6. 重新创建所有 tab 组件以更新字体和文本
        if (tabbedPane != null && tabInfos != null) {
            int currentSelectedIndex = tabbedPane.getSelectedIndex();
            for (int i = 0; i < tabInfos.size(); i++) {
                // 从 SidebarTab 枚举重新获取标题（支持语言切换）
                if (i < SidebarTab.values().length) {
                    SidebarTab sidebarTab = SidebarTab.values()[i];
                    TabInfo info = tabInfos.get(i);
                    // 更新标题（语言切换）
                    info.title = I18nUtil.getMessage(sidebarTab.getTitleKey());
                    // 根据是否选中使用对应的图标
                    boolean isSelected = (i == currentSelectedIndex);
                    Icon iconToUse = isSelected ? sidebarTab.getSelectedIcon() : sidebarTab.getIcon();
                    // 重新创建 tab 组件
                    tabbedPane.setTabComponentAt(i, createTabComponent(info.title, iconToUse));
                }
            }
            // 延迟执行颜色初始化，确保所有 UI 组件都创建完成
            SwingUtilities.invokeLater(() -> {
                initializeAllTabColors();
                // 再次强制刷新，确保选中状态正确显示
                tabbedPane.repaint();
            });
        }
    }

    /**
     * 重新创建 TabbedPane 的自定义 UI
     * 用于在 updateUI() 后恢复自定义的选中状态视觉效果
     */
    private void recreateTabbedPaneUI() {
        tabbedPane.setForeground(ModernColors.getTextPrimary());
        tabbedPane.setFont(new Font(tabbedPane.getFont().getName(), Font.PLAIN, 14));

        // 应用自定义 UI
        applyCustomTabbedPaneUI(tabbedPane);

        // 强制刷新 UI,确保自定义渲染立即生效
        tabbedPane.revalidate();
        tabbedPane.repaint();
    }

    @Override
    protected void registerListeners() {
        tabbedPane.addChangeListener(e -> {
            handleTabChange();
            updateTabTextColors(); // 更新所有 tab 的文字颜色
        });
        // 懒加载第一个tab
        SwingUtilities.invokeLater(() -> {
            ensureTabComponentLoaded(0);
            initializeAllTabColors(); // 初始化时更新所有 tab 的颜色
        });

        // 注册快捷键
        registerTabShortcuts();
    }

    /**
     * 注册 Tab 快捷键
     * Ctrl/Cmd + 1-7 切换到对应的 Tab
     */
    private void registerTabShortcuts() {
        int modifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(); // Mac用Cmd，Windows用Ctrl

        // 为每个 Tab 注册快捷键 Ctrl/Cmd + 数字
        for (int i = 0; i < Math.min(tabInfos.size(), 9); i++) {
            final int tabIndex = i;
            int keyCode = KeyEvent.VK_1 + i; // VK_1 到 VK_9

            // 注册快捷键到输入映射
            String actionKey = "switchToTab" + (i + 1);
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke(keyCode, modifier), actionKey
            );
            getActionMap().put(actionKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (tabIndex < tabbedPane.getTabCount()) {
                        tabbedPane.setSelectedIndex(tabIndex);
                    }
                }
            });
        }
    }

    /**
     * 初始化所有 tab 的图标和文字颜色（在首次加载时调用，展开和收起状态都需要）
     */
    private void initializeAllTabColors() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            updateSingleTabTextColor(i, i == selectedIndex);
        }
        lastSelectedIndex = selectedIndex;
    }

    /**
     * 更新所有 tab 的图标和文字颜色（展开和收起状态都支持）
     * 优化：只更新当前和之前选中的 tab，避免遍历所有 tab
     */
    private void updateTabTextColors() {
        int selectedIndex = tabbedPane.getSelectedIndex();

        // 更新之前选中的 tab（如果存在）
        if (lastSelectedIndex >= 0 && lastSelectedIndex < tabbedPane.getTabCount()) {
            updateSingleTabTextColor(lastSelectedIndex, false);
        }

        // 更新当前选中的 tab
        if (selectedIndex >= 0 && selectedIndex < tabbedPane.getTabCount()) {
            updateSingleTabTextColor(selectedIndex, true);
        }

        // 记录当前选中的索引
        lastSelectedIndex = selectedIndex;
    }

    /**
     * 更新单个 tab 的图标和文字颜色（支持展开和收起状态）
     */
    private void updateSingleTabTextColor(int tabIndex, boolean isSelected) {
        Component tabComponent = tabbedPane.getTabComponentAt(tabIndex);
        if (!(tabComponent instanceof JPanel panel)) return;

        // 获取对应的 SidebarTab 枚举
        if (tabIndex < 0 || tabIndex >= SidebarTab.values().length) return;
        SidebarTab sidebarTab = SidebarTab.values()[tabIndex];

        updateTabIcon(panel, sidebarTab, isSelected);
        updateTabTitle(panel, isSelected);
    }

    /**
     * 更新tab图标
     */
    private void updateTabIcon(JPanel panel, SidebarTab sidebarTab, boolean isSelected) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JLabel label && ICON_LABEL_NAME.equals(label.getName())) {
                label.setIcon(isSelected ? sidebarTab.getSelectedIcon() : sidebarTab.getIcon());
                break;
            }
        }
    }

    /**
     * 更新tab标题
     */
    private void updateTabTitle(JPanel panel, boolean isSelected) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JLabel label && TITLE_LABEL_NAME.equals(label.getName())) {
                if (isSelected) {
                    label.setForeground(ModernColors.PRIMARY);
                    label.setFont(boldFont);
                } else {
                    label.setForeground(ModernColors.getTextSecondary());
                    label.setFont(normalFont);
                }
                break;
            }
        }
    }

    // Tab切换时才加载真正的面板内容
    private void handleTabChange() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        ensureTabComponentLoaded(selectedIndex); // 懒加载当前选中的tab内容
        // Check if the selected tab is the environment tab
        if (selectedIndex >= 0 && selectedIndex < tabInfos.size()) {
            TabInfo info = tabInfos.get(selectedIndex);
            if (info.title.equals(I18nUtil.getMessage(MessageKeys.MENU_ENVIRONMENTS))) {
                Component comp = tabbedPane.getComponentAt(selectedIndex);
                if (comp instanceof EnvironmentPanel environmentPanel) {
                    environmentPanel.refreshUI();
                }
            }
        }
    }

    private void ensureTabComponentLoaded(int index) {
        if (index < 0 || index >= tabInfos.size()) return;
        TabInfo info = tabInfos.get(index);
        Component comp = tabbedPane.getComponentAt(index);
        if (comp == null || comp.getClass() == JPanel.class) {
            JPanel realPanel = info.getPanel(); // 懒加载真正的面板内容
            tabbedPane.setComponentAt(index, realPanel);
        }
    }

    /**
     * 创建现代化标签页
     */
    private JTabbedPane createModernTabbedPane() {
        JTabbedPane pane = new JTabbedPane(SwingConstants.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT) {
            @Override
            public void updateUI() {
                // 先调用父类的 updateUI，这会重置 UI
                super.updateUI();
                // 立即重新应用我们的自定义 UI
                applyCustomTabbedPaneUI(this);
            }
        };
        pane.setForeground(ModernColors.getTextPrimary());
        pane.setFont(new Font(pane.getFont().getName(), Font.PLAIN, 14));

        // 自定义标签页UI
        applyCustomTabbedPaneUI(pane);

        return pane;
    }

    /**
     * 应用自定义的 TabbedPane UI
     * 提取为独立方法，便于在 updateUI() 时重用
     */
    private void applyCustomTabbedPaneUI(JTabbedPane pane) {
        pane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                // 增加 tab 区域的上下边距，让 tab 之间有更多空间
                tabAreaInsets = new Insets(8, 6, 8, 0);
                contentBorderInsets = new Insets(0, 0, 0, 0);
                // tab 之间的间距
                tabInsets = new Insets(2, 2, 2, 2);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                              int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                // 高质量渲染提示，确保清晰锐利
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                if (isSelected) {
                    // 选中状态：绘制清晰的渐变指示条 + 淡雅背景
                    int leftMargin = 2;        // 左边距，距离边缘更近
                    int verticalMargin = 6;    // 上下边距，让指示条稍短一些
                    int indicatorWidth = 4;    // 指示条宽度
                    int indicatorRadius = 2;   // 圆角半径，减小到2px避免模糊

                    // 1. 绘制淡雅背景（使用缓存的颜色对象）
                    if (cachedBgColor == null) {
                        cachedBgColor = new Color(
                                ModernColors.PRIMARY.getRed(),
                                ModernColors.PRIMARY.getGreen(),
                                ModernColors.PRIMARY.getBlue(),
                                25  // 稍微增加透明度，让背景更明显
                        );
                    }
                    g2.setColor(cachedBgColor);
                    g2.fillRect(x, y, w, h);

                    // 2. 绘制实心渐变指示条（使用缓存的渐变对象）
                    int indicatorX = x + leftMargin;
                    int indicatorY = y + verticalMargin;
                    int indicatorHeight = h - verticalMargin * 2;

                    // 只有在高度变化时才重新创建渐变对象，使用更饱和的颜色
                    if (cachedGradient == null || lastIndicatorHeight != indicatorHeight) {
                        cachedGradient = new GradientPaint(
                                0, 0, ModernColors.PRIMARY,  // 顶部：标准蓝（更清晰）
                                0, indicatorHeight, ModernColors.PRIMARY_LIGHT  // 底部：亮蓝
                        );
                        lastIndicatorHeight = indicatorHeight;
                    }

                    g2.setPaint(cachedGradient);
                    g2.translate(indicatorX, indicatorY);
                    // 使用fillRoundRect绘制实心指示条，清晰锐利
                    g2.fillRoundRect(0, 0, indicatorWidth, indicatorHeight, indicatorRadius, indicatorRadius);
                    g2.translate(-indicatorX, -indicatorY);
                }
                // 不绘制悬停效果，避免卡顿

                g2.dispose();
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
                // 不绘制任何边框
            }

            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                                               int tabIndex, Rectangle iconRect, Rectangle textRect,
                                               boolean isSelected) {
                // 不绘制焦点指示器（虚线框）
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // 不绘制内容区边框和分隔线
            }

            @Override
            protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
                // 根据展开状态调整宽度
                if (sidebarExpanded) {
                    return calculateExpandedTabWidth();
                } else {
                    return 48;
                }
            }

            @Override
            protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
                // 根据展开状态调整高度
                if (sidebarExpanded) {
                    return 72;
                } else {
                    return 64;
                }
            }
        });
    }

    /**
     * 创建自定义 Tab 组件（图标在上，文本在下）
     */
    private Component createTabComponent(String title, Icon icon) {
        JPanel panel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                if (sidebarExpanded) {
                    // 展开状态：使用计算出的最佳宽度，基于最长文本
                    size.width = calculateExpandedTabWidth();
                } else {
                    // 收起状态：紧凑的固定宽度，仅容纳图标
                    size.width = 38;
                }
                return size;
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        if (sidebarExpanded) {
            // 展开状态：图标在上，文本在下
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            iconLabel.setName(ICON_LABEL_NAME); // 标记为图标标签

            JLabel titleLabel = new JLabel(title);
            titleLabel.setName(TITLE_LABEL_NAME); // 标记为文字标签
            // 根据当前是否选中设置初始颜色
            int currentIndex = getTabIndexByTitle(title);
            boolean isCurrentlySelected = currentIndex >= 0 && tabbedPane.getSelectedIndex() == currentIndex;

            if (isCurrentlySelected) {
                titleLabel.setFont(boldFont);
                titleLabel.setForeground(ModernColors.PRIMARY);
            } else {
                titleLabel.setFont(normalFont);
                titleLabel.setForeground(ModernColors.getTextSecondary());
            }
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(Box.createVerticalStrut(5));
            panel.add(iconLabel);
            panel.add(Box.createVerticalStrut(4));
            panel.add(titleLabel);
            panel.add(Box.createVerticalStrut(5));

            panel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        } else {
            // 收起状态：只显示图标，居中，增加上下左右间距
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            iconLabel.setName(ICON_LABEL_NAME); // 标记为图标标签
            // 设置 tooltip 在 panel 上而非 label 上
            panel.setToolTipText(title);

            panel.add(Box.createVerticalGlue());
            panel.add(iconLabel);
            panel.add(Box.createVerticalGlue());

            // 增加边距，尤其是上下间距，让图标之间不会挨在一起
            panel.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        }

        // 只为 panel 添加一个鼠标监听器
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int tabIndex = getTabIndexByTitle(title);
                if (tabIndex >= 0) {
                    tabbedPane.setSelectedIndex(tabIndex);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                panel.setCursor(Cursor.getDefaultCursor());
            }
        });

        return panel;
    }

    /**
     * 根据标题获取 tab 索引
     */
    private int getTabIndexByTitle(String title) {
        for (int i = 0; i < tabInfos.size(); i++) {
            if (tabInfos.get(i).title.equals(title)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 计算展开状态下的最佳tab宽度
     * 基于所有tab标题中最长的文本
     */
    private int calculateExpandedTabWidth() {
        if (calculatedExpandedTabWidth > 0) {
            return calculatedExpandedTabWidth;
        }

        int maxWidth = 0;
        JLabel tempLabel = new JLabel();
        tempLabel.setFont(boldFont); // 使用粗体字体计算，因为选中时会变粗

        for (TabInfo info : tabInfos) {
            tempLabel.setText(info.title);
            int textWidth = tempLabel.getPreferredSize().width;
            maxWidth = Math.max(maxWidth, textWidth);
        }

        // 计算总宽度：左右边距(10+10) + 文本宽度
        calculatedExpandedTabWidth = maxWidth + 20;

        // 设置最小和最大宽度限制
        calculatedExpandedTabWidth = Math.max(calculatedExpandedTabWidth, 40); // 最小70px
        calculatedExpandedTabWidth = Math.min(calculatedExpandedTabWidth, 120); // 最大120px

        return calculatedExpandedTabWidth;
    }

    /**
     * 更新侧边栏展开/收起状态
     * 从设置对话框调用，用于实时更新UI
     */
    public void updateSidebarExpansion() {
        boolean newExpanded = SettingManager.isSidebarExpanded();
        if (this.sidebarExpanded != newExpanded) {
            this.sidebarExpanded = newExpanded;
            calculatedExpandedTabWidth = -1; // 重置宽度缓存
            // 重新创建 TabbedPane 以应用新的展开状态
            recreateTabbedPane();
        }
    }

    /**
     * 重新创建标签页以应用新的展开/收起状态
     */
    private void recreateTabbedPane() {
        int selectedIndex = tabbedPane.getSelectedIndex();

        // 保存所有已加载的面板
        List<Component> loadedPanels = new ArrayList<>();
        for (int i = 0; i < tabInfos.size(); i++) {
            Component comp = tabbedPane.getComponentAt(i);
            loadedPanels.add(comp);
        }

        // 移除旧的 TabbedPane
        Component consoleComp = null;
        LayoutManager layout = getLayout();
        if (layout instanceof BorderLayout borderLayout) {
            consoleComp = borderLayout.getLayoutComponent(BorderLayout.SOUTH);
            remove(tabbedPane);
        }

        // 创建新的 TabbedPane
        tabbedPane = createModernTabbedPane();

        // 恢复所有 tab - 重用 tabInfos 中缓存的图标
        for (int i = 0; i < tabInfos.size(); i++) {
            TabInfo info = tabInfos.get(i);
            tabbedPane.addTab(info.title, loadedPanels.get(i));
            // 设置自定义 tab 组件 - icon 从 tabInfos 获取，已经缓存
            tabbedPane.setTabComponentAt(i, createTabComponent(info.title, info.icon));
        }

        // 恢复选中的索引
        if (selectedIndex >= 0 && selectedIndex < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(selectedIndex);
        }

        // 重新添加组件
        add(tabbedPane, BorderLayout.CENTER);
        if (consoleComp != null) {
            add(consoleComp, BorderLayout.SOUTH);
        }

        // 重新注册监听器
        tabbedPane.addChangeListener(e -> {
            handleTabChange();
            updateTabTextColors();
        });

        // 重置 lastSelectedIndex 避免 bug
        lastSelectedIndex = -1;

        // 更新初始文字颜色
        initializeAllTabColors();

        // 刷新UI
        revalidate();
        repaint();
    }
}

