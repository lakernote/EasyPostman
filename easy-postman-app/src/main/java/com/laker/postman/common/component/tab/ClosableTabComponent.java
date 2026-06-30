package com.laker.postman.common.component.tab;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.panel.collections.editor.RequestEditorEmptyStatePanel;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.service.setting.ShortcutManager;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * 可关闭的 Tab 标题组件。
 *
 * <p>功能：
 * <ul>
 *   <li>显示请求方法/协议标识或业务图标 + 标题（超长自动截断）</li>
 *   <li>右侧绘制关闭按钮（hover 才可见，点击关闭 Tab）</li>
 *   <li>右上角小圆点：黄色=新建未保存、红色=已修改（脏）</li>
 *   <li>临时模式：标题斜体，表示可被复用的临时 Tab</li>
 *   <li>右键菜单：关闭当前 / 关闭其他 / 关闭右侧 / 关闭所有</li>
 *   <li>支持 Tab 拖拽排序（附加 TabbedPaneDragHandler 监听器）</li>
 * </ul>
 */
@Slf4j
public class ClosableTabComponent extends JPanel {

    // ── 布局常量 ─────────────────────────────────────────────────────────────
    public static final String ELLIPSIS = "...";
    private static final int MAX_TAB_WIDTH     = 160;
    private static final int MIN_TAB_WIDTH     = 80;
    private static final int TAB_HEIGHT        = 28;
    private static final int CLOSE_DIAMETER    = 11;   // 关闭按钮圆圈直径
    private static final int CLOSE_MARGIN      = 0;    // 关闭按钮距右边距
    private static final int CLOSE_TEXT_SPACING = 0;   // 关闭按钮与文字间距
    private static final int CLOSE_HIT_PADDING = 3;    // 扩大关闭按钮命中区域，提升易用性
    private static final int LABEL_LEFT_PAD    = 4;    // 标题 label 左内边距

    // ── 数据 ─────────────────────────────────────────────────────────────────
    private final JLabel label;
    private final String labelText;
    private final JTabbedPane tabbedPane;

    // ── 状态 ─────────────────────────────────────────────────────────────────
    private RequestTabMarkers markers = RequestTabMarkers.clean();
    private boolean hoverTab = false;            // 鼠标是否在整个 Tab 上
    private boolean hoverClose = false;          // 鼠标是否在关闭按钮上

    // ── 构造器 ───────────────────────────────────────────────────────────────

    public ClosableTabComponent(String title, RequestItemProtocolEnum protocol, boolean isRoot) {
        this(title, null, protocol, isRoot, false);
    }

    public static ClosableTabComponent forRequest(String title, HttpRequestItem item) {
        Objects.requireNonNull(item, "item");
        return forRequest(title, item.getMethod(), item.getProtocol());
    }

    public static ClosableTabComponent forRequest(String title, String method, RequestItemProtocolEnum protocol) {
        return new ClosableTabComponent(title, method, protocol, false, true);
    }

    private ClosableTabComponent(String title, String method, RequestItemProtocolEnum protocol,
                                 boolean isRoot, boolean requestTab) {
        this.tabbedPane = UiSingletonFactory.getInstance(RequestEditorPanel.class).getTabbedPane();
        String safeTitle = title == null ? "" : title;

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        setToolTipText(safeTitle);

        // 计算 Tab 宽度并创建 label
        LabelPresentation labelPresentation = buildLabel(safeTitle, method, protocol, isRoot, requestTab);
        label = labelPresentation.label();
        labelText = labelPresentation.text();
        add(label, BorderLayout.CENTER);

        // 右键菜单
        setComponentPopupMenu(buildPopupMenu());

        // 鼠标：hover 显示关闭按钮、关闭按钮点击、选中 Tab
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                updateHoverState(e.getX(), e.getY());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { updateHoverState(e.getX(), e.getY()); }
            @Override public void mouseExited(MouseEvent e)  { updateHoverState(-1, -1); }
            @Override public void mousePressed(MouseEvent e) { handleMousePress(e); }
        });

        // 拖拽排序支持
        TabbedPaneDragHandler handler = UiSingletonFactory.getInstance(RequestEditorPanel.class).getDragHandler();
        if (handler != null) handler.attachTo(this);
    }

    // ── 构造辅助：构建 label ─────────────────────────────────────────────────

    private LabelPresentation buildLabel(String title, String method, RequestItemProtocolEnum protocol,
                                         boolean isRoot, boolean requestTab) {
        Font labelFont = FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1);
        FontMetrics fm = getFontMetrics(labelFont);
        RequestTabDisplayMetadata.Badge badge = requestTab
                ? RequestTabDisplayMetadata.badgeFor(method, protocol)
                : null;
        Icon icon = requestTab ? null : resolveIcon(protocol, isRoot);
        int closeSpace = CLOSE_DIAMETER + CLOSE_TEXT_SPACING + CLOSE_MARGIN;
        int iconSpace  = icon == null ? 0 : 20;
        int badgeSpace = badge == null ? 0 : fm.stringWidth(badge.text() + " ");
        int padding    = 20;

        int tabWidth = Math.max(
                Math.min(fm.stringWidth(title) + badgeSpace + iconSpace + closeSpace + padding, MAX_TAB_WIDTH),
                MIN_TAB_WIDTH);
        setPreferredSize(new Dimension(tabWidth, TAB_HEIGHT));

        // 文字超长时截断
        String displayTitle = truncate(title, fm, tabWidth - badgeSpace - iconSpace - closeSpace - padding);
        String renderedText = RequestTabDisplayMetadata.labelText(badge, displayTitle);

        // label 本身不消费鼠标事件（避免遮挡关闭按钮）
        JLabel lbl = new JLabel(renderedText) {
            @Override public boolean contains(int x, int y) { return false; }
        };
        lbl.setFont(labelFont);
        lbl.setIcon(icon);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, LABEL_LEFT_PAD, 0, closeSpace + CLOSE_TEXT_SPACING));
        lbl.setHorizontalAlignment(SwingConstants.LEFT);
        lbl.setVerticalAlignment(SwingConstants.CENTER);
        return new LabelPresentation(lbl, renderedText);
    }

    private static String truncate(String title, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(title) <= maxWidth) return title;
        int len = title.length();
        while (len > 0 && fm.stringWidth(title.substring(0, len) + ELLIPSIS) > maxWidth) len--;
        return title.substring(0, len) + ELLIPSIS;
    }

    private static Icon resolveIcon(RequestItemProtocolEnum protocol, boolean isRoot) {
        if (protocol == RequestItemProtocolEnum.SAVED_RESPONSE) {
            return IconUtil.createThemed("icons/save-response.svg", 24, 24);
        }
        if (protocol != null) {
            return null;
        }
        return new FlatSVGIcon(isRoot ? "icons/collection.svg" : "icons/group.svg", 18, 18);
    }

    private record LabelPresentation(JLabel label, String text) {
    }

    // ── 鼠标事件处理 ─────────────────────────────────────────────────────────

    private void updateHoverState(int x, int y) {
        boolean nextHoverTab = contains(x, y);
        boolean nextHoverClose = nextHoverTab && isInCloseButton(x, y);
        if (hoverTab != nextHoverTab || hoverClose != nextHoverClose) {
            hoverTab = nextHoverTab;
            hoverClose = nextHoverClose;
            repaint();
        }
    }

    private void handleMousePress(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        int idx = tabbedPane.indexOfTabComponent(this);
        if (idx < 0) return;

        if (isInCloseButton(e.getX(), e.getY())) {
            // 点击关闭按钮：先选中再关闭
            tabbedPane.setSelectedIndex(idx);
            UiSingletonFactory.getInstance(RequestEditorPanel.class).closeCurrentTab();
        } else {
            // 普通点击：选中并在左侧树中定位
            tabbedPane.setSelectedIndex(idx);
            locateInTree(idx);
        }
    }

    private void locateInTree(int idx) {
        Component comp = tabbedPane.getComponentAt(idx);
        if (!(comp instanceof RequestEditSubPanel subPanel)) return;
        HttpRequestItem req = subPanel.getCurrentRequest();
        if (req == null || req.getId() == null) return;
        SwingUtilities.invokeLater(() -> {
            try {
                UiSingletonFactory.getInstance(CollectionTreePanel.class)
                        .locateAndSelectRequest(req.getId());
            } catch (Exception ex) {
                log.error("定位请求节点时出错", ex);
            }
        });
    }

    // ── 关闭按钮区域 ─────────────────────────────────────────────────────────

    private Rectangle closeButtonBounds() {
        int x = getWidth() - CLOSE_DIAMETER - CLOSE_MARGIN;
        int y = (getHeight() - CLOSE_DIAMETER) / 2;
        return new Rectangle(x, y, CLOSE_DIAMETER, CLOSE_DIAMETER);
    }

    /** 判断坐标是否在关闭按钮上（供自身和 TabbedPaneDragHandler 使用） */
    public boolean isInCloseButton(int x, int y) {
        Rectangle hitBounds = closeButtonBounds();
        hitBounds.grow(CLOSE_HIT_PADDING, CLOSE_HIT_PADDING);
        return hitBounds.contains(x, y);
    }

    // ── 绘制 ─────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Rectangle cb = closeButtonBounds();
        int x = cb.x;
        int y = cb.y;
        int r = CLOSE_DIAMETER;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (hoverClose) {
            g2.setColor(closeButtonBg());
            g2.fillOval(x, y, r, r);
            g2.setColor(closeButtonFg());
            g2.setStroke(new BasicStroke(1.5f));
            int pad = 2;
            g2.drawLine(x + pad, y + pad, x + r - pad, y + r - pad);
            g2.drawLine(x + r - pad, y + pad, x + pad, y + r - pad);
        } else if (hoverTab) {
            g2.setColor(closeButtonFg());
            g2.setStroke(new BasicStroke(1.5f));
            int pad = 2;
            g2.drawLine(x + pad, y + pad, x + r - pad, y + r - pad);
            g2.drawLine(x + r - pad, y + pad, x + pad, y + r - pad);
        } else if (markers.isNewRequest()) {
            g2.setColor(new Color(255, 204, 0, 180));   // 黄点
            g2.fillOval(x, y, r, r);
        } else if (markers.isDirty()) {
            g2.setColor(dirtyColor());                   // 红点
            g2.fillOval(x, y, r, r);
        }
        g2.dispose();
    }

    // ── 主题适配色 ───────────────────────────────────────────────────────────

    private Color closeButtonBg() {
        Color hoverColor = UIManager.getColor("TabbedPane.hoverColor");
        if (hoverColor != null) {
            return ModernColors.withAlpha(hoverColor, 160);
        }
        return ModernColors.withAlpha(ModernColors.getHoverBackgroundColor(), 160);
    }

    private Color closeButtonFg() {
        return closeButtonForegroundColor();
    }

    private Color dirtyColor() {
        return dirtyDotColor();
    }

    static Color closeButtonForegroundColor() {
        return ModernColors.getTextPrimary();
    }

    static Color dirtyDotColor() {
        return ModernColors.withAlpha(ModernColors.getError(), 180);
    }

    // ── 右键菜单 ─────────────────────────────────────────────────────────────

    private JPopupMenu buildPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);
        RequestEditorPanel editPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);

        JMenuItem closeCurrent = menuItem(MessageKeys.TAB_CLOSE_CURRENT, ShortcutManager.CLOSE_CURRENT_TAB,
                () -> { int i = tabbedPane.indexOfTabComponent(this); if (i >= 0) { tabbedPane.setSelectedIndex(i); editPanel.closeCurrentTab(); } });

        JMenuItem closeOthers = menuItem(MessageKeys.TAB_CLOSE_OTHERS, ShortcutManager.CLOSE_OTHER_TABS,
                () -> { int i = tabbedPane.indexOfTabComponent(this); if (i >= 0) { tabbedPane.setSelectedIndex(i); editPanel.closeOtherTabs(); } });

        JMenuItem closeRight = new JMenuItem(I18nUtil.getMessage(MessageKeys.TAB_CLOSE_RIGHT));
        closeRight.addActionListener(e -> closeTabsToTheRight());

        JMenuItem closeAll = menuItem(MessageKeys.TAB_CLOSE_ALL, ShortcutManager.CLOSE_ALL_TABS,
                editPanel::closeAllTabs);

        menu.add(closeCurrent);
        menu.addSeparator();
        menu.add(closeOthers);
        menu.add(closeRight);
        menu.addSeparator();
        menu.add(closeAll);
        return menu;
    }

    private JMenuItem menuItem(String msgKey, String shortcutKey, Runnable action) {
        JMenuItem item = new JMenuItem(I18nUtil.getMessage(msgKey));
        KeyStroke ks = ShortcutManager.getKeyStroke(shortcutKey);
        if (ks != null) item.setAccelerator(ks);
        item.addActionListener(e -> action.run());
        return item;
    }

    private void closeTabsToTheRight() {
        int thisIdx = tabbedPane.indexOfTabComponent(this);
        if (thisIdx < 0) return;
        RequestEditorPanel editPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        for (int i = tabbedPane.getTabCount() - 1; i > thisIdx; i--) {
            if (!(tabbedPane.getComponentAt(i) instanceof RequestEditorEmptyStatePanel)) {
                editPanel.removeTabAtWithCleanup(i);
            }
        }
    }

    // ── Marker state ─────────────────────────────────────────────────────────

    public RequestTabMarkers getMarkers() {
        return markers;
    }

    public void updateMarkers(UnaryOperator<RequestTabMarkers> markerUpdater) {
        if (markerUpdater == null) {
            return;
        }
        applyMarkers(markerUpdater.apply(markers));
    }

    private void applyMarkers(RequestTabMarkers markers) {
        this.markers = markers == null ? RequestTabMarkers.clean() : markers;
        label.setText(labelText);
        label.setFont(FontsUtil.getDefaultFontWithOffset(this.markers.isPreviewMode() ? Font.ITALIC : Font.PLAIN, -1));
        repaint();
    }
}
