package com.laker.postman.common.component.tab;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.service.setting.ShortcutManager;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * 可关闭的 Tab 标题组件。
 *
 * <p>功能：
 * <ul>
 *   <li>显示协议图标 + 标题（超长自动截断）</li>
 *   <li>右侧绘制关闭按钮（hover 才可见，点击关闭 Tab）</li>
 *   <li>右上角小圆点：黄色=新建未保存、红色=已修改（脏）</li>
 *   <li>预览模式：标题斜体，表示临时 Tab</li>
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
    private static final int LABEL_LEFT_PAD    = 4;    // 标题 label 左内边距

    // ── 数据 ─────────────────────────────────────────────────────────────────
    private final JLabel label;
    private final String rawTitle;           // 原始完整标题（用于 dirty/newRequest 重置）
    private final JTabbedPane tabbedPane;

    // ── 状态 ─────────────────────────────────────────────────────────────────
    @Getter private boolean dirty      = false; // 红点：内容已修改
    @Getter private boolean newRequest = false; // 黄点：新建未保存
    @Getter private boolean previewMode = false; // 斜体：临时预览 Tab
    private boolean hoverClose = false;          // 鼠标是否在关闭按钮上

    // ── 构造器 ───────────────────────────────────────────────────────────────

    public ClosableTabComponent(String title, RequestItemProtocolEnum protocol) {
        this(title, protocol, false);
    }

    public ClosableTabComponent(String title, RequestItemProtocolEnum protocol, boolean isRoot) {
        this.tabbedPane = SingletonFactory.getInstance(RequestEditPanel.class).getTabbedPane();
        this.rawTitle = title;

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        setToolTipText(title);

        // 计算 Tab 宽度并创建 label
        label = buildLabel(title, protocol, isRoot);
        add(label, BorderLayout.CENTER);

        // 右键菜单
        setComponentPopupMenu(buildPopupMenu());

        // 鼠标：hover 显示关闭按钮、关闭按钮点击、选中 Tab
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                setHoverClose(true);
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e)  { setHoverClose(false); }
            @Override public void mousePressed(MouseEvent e) { handleMousePress(e); }
        });

        // 拖拽排序支持
        TabbedPaneDragHandler handler = SingletonFactory.getInstance(RequestEditPanel.class).getDragHandler();
        if (handler != null) handler.attachTo(this);
    }

    // ── 构造辅助：构建 label ─────────────────────────────────────────────────

    private JLabel buildLabel(String title, RequestItemProtocolEnum protocol, boolean isRoot) {
        FontMetrics fm = getFontMetrics(getFont());
        int closeSpace = CLOSE_DIAMETER + CLOSE_TEXT_SPACING + CLOSE_MARGIN;
        int iconSpace  = 20;
        int padding    = 20;

        int tabWidth = Math.max(
                Math.min(fm.stringWidth(title) + iconSpace + closeSpace + padding, MAX_TAB_WIDTH),
                MIN_TAB_WIDTH);
        setPreferredSize(new Dimension(tabWidth, TAB_HEIGHT));

        // 文字超长时截断
        String displayTitle = truncate(title, fm, tabWidth - iconSpace - closeSpace - padding);

        // label 本身不消费鼠标事件（避免遮挡关闭按钮）
        JLabel lbl = new JLabel(displayTitle) {
            @Override public boolean contains(int x, int y) { return false; }
        };
        lbl.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        lbl.setIcon(resolveIcon(protocol, isRoot));
        lbl.setBorder(BorderFactory.createEmptyBorder(0, LABEL_LEFT_PAD, 0, closeSpace + CLOSE_TEXT_SPACING));
        lbl.setHorizontalAlignment(SwingConstants.LEFT);
        lbl.setVerticalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    private static String truncate(String title, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(title) <= maxWidth) return title;
        int len = title.length();
        while (len > 0 && fm.stringWidth(title.substring(0, len) + ELLIPSIS) > maxWidth) len--;
        return title.substring(0, len) + ELLIPSIS;
    }

    private static Icon resolveIcon(RequestItemProtocolEnum protocol, boolean isRoot) {
        if (protocol != null) return protocol.getIcon();
        return new FlatSVGIcon(isRoot ? "icons/collection.svg" : "icons/group.svg", 18, 18);
    }

    // ── 鼠标事件处理 ─────────────────────────────────────────────────────────

    private void setHoverClose(boolean hover) {
        if (hoverClose != hover) { hoverClose = hover; repaint(); }
    }

    private void handleMousePress(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        int idx = tabbedPane.indexOfTabComponent(this);
        if (idx < 0) return;

        if (isInCloseButton(e.getX(), e.getY())) {
            // 点击关闭按钮：先选中再关闭
            tabbedPane.setSelectedIndex(idx);
            SingletonFactory.getInstance(RequestEditPanel.class).closeCurrentTab();
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
                SingletonFactory.getInstance(RequestCollectionsLeftPanel.class)
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
        return closeButtonBounds().contains(x, y);
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
            // 关闭按钮：背景圆 + X 线
            g2.setColor(closeButtonBg());
            g2.fillOval(x, y, r, r);
            g2.setColor(closeButtonFg());
            g2.setStroke(new BasicStroke(1.5f));
            int pad = 2;
            g2.drawLine(x + pad, y + pad, x + r - pad, y + r - pad);
            g2.drawLine(x + r - pad, y + pad, x + pad, y + r - pad);
        } else if (newRequest) {
            g2.setColor(new Color(255, 204, 0, 180));   // 黄点
            g2.fillOval(x, y, r, r);
        } else if (dirty) {
            g2.setColor(dirtyColor());                   // 红点
            g2.fillOval(x, y, r, r);
        }
        g2.dispose();
    }

    // ── 主题适配色 ───────────────────────────────────────────────────────────

    private boolean isDark() { return FlatLaf.isLafDark(); }

    private Color closeButtonBg() {
        Color base = tabbedPane.getBackground();
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), isDark() ? 200 : 180);
    }

    private Color closeButtonFg() { return isDark() ? Color.WHITE : Color.BLACK; }

    private Color dirtyColor() {
        return isDark() ? new Color(239, 83, 80, 180) : new Color(209, 47, 47, 131);
    }

    // ── 右键菜单 ─────────────────────────────────────────────────────────────

    private JPopupMenu buildPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);

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
        for (int i = tabbedPane.getTabCount() - 1; i > thisIdx; i--) {
            if (!(tabbedPane.getComponentAt(i) instanceof PlusPanel)) tabbedPane.remove(i);
        }
    }

    // ── 公开状态 setter ──────────────────────────────────────────────────────

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        label.setText(rawTitle);
        repaint();
    }

    public void setNewRequest(boolean newRequest) {
        this.newRequest = newRequest;
        label.setText(rawTitle);
        repaint();
    }

    /** 预览模式用斜体提示这是临时 Tab */
    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
        label.setFont(FontsUtil.getDefaultFontWithOffset(previewMode ? Font.ITALIC : Font.PLAIN, -1));
        repaint();
    }
}
