package com.laker.postman.common.component.tab;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Tab 拖拽排序处理器（IDEA 风格）
 *
 * <p>由于 {@link ClosableTabComponent} 覆盖了 Tab 头部区域，
 * tabbedPane 本身收不到 mouseDragged 事件，因此监听器必须装在每个 tabComponent 上。
 * 每创建一个 ClosableTabComponent，调用 {@link #attachTo} 注册一次即可。
 *
 * <p>拖拽过程：按下 → 移动超过阈值 → 显示蓝色竖线指示位置 → 松手完成排序。
 * "+" Tab 始终排在最后，不可拖动也不可成为拖入目标。
 */
@Slf4j
public class TabbedPaneDragHandler {

    // ── 回调，用于同步 RequestEditPanel 中的 previewTabIndex ─────────────────
    public interface IndexGetter { int get(); }
    public interface IndexSetter { void set(int value); }

    // ── 拖拽过程中的瞬态状态（集中管理，方便 reset）────────────────────────
    private static class DragState {
        int draggedTabIndex = -1;   // 被拖动的 Tab 索引
        int dropTargetIndex = -1;   // 当前悬停目标索引
        Point pressPointInPane;     // 按下时在 tabbedPane 坐标系的位置
        boolean active = false;     // 是否已超过阈值、进入真正拖拽

        boolean isReady() {
            return draggedTabIndex >= 0 && pressPointInPane != null;
        }

        void reset() {
            draggedTabIndex = -1;
            dropTargetIndex = -1;
            pressPointInPane = null;
            active = false;
        }
    }

    private static final int DRAG_THRESHOLD = 5; // 触发拖拽的最小像素位移

    private final JTabbedPane tabbedPane;
    private final IndexGetter previewIndexGetter;
    private final IndexSetter previewIndexSetter;

    private final DragState state = new DragState();

    // GlassPane 相关
    private TabDropIndicator dropIndicator;    // 拖拽指示线
    private Component savedGlassPane;          // 拖拽结束后恢复

    // ── 工厂方法 ─────────────────────────────────────────────────────────────

    private TabbedPaneDragHandler(JTabbedPane tabbedPane,
                                   IndexGetter previewIndexGetter,
                                   IndexSetter previewIndexSetter) {
        this.tabbedPane = tabbedPane;
        this.previewIndexGetter = previewIndexGetter;
        this.previewIndexSetter = previewIndexSetter;
    }

    /** 创建并绑定到指定 tabbedPane */
    public static TabbedPaneDragHandler install(JTabbedPane tabbedPane,
                                                 IndexGetter previewIndexGetter,
                                                 IndexSetter previewIndexSetter) {
        return new TabbedPaneDragHandler(tabbedPane, previewIndexGetter, previewIndexSetter);
    }

    // ── 公开方法：注册到 tabComponent ────────────────────────────────────────

    /**
     * 将拖拽监听器绑定到一个 tabComponent。
     * 在 {@link ClosableTabComponent} 构造器末尾调用。
     */
    public void attachTo(Component tabComponent) {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                onPress(e, tabComponent);
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                onDrag(e, tabComponent);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                onRelease();
            }
        };
        tabComponent.addMouseListener(adapter);
        tabComponent.addMouseMotionListener(adapter);
    }

    // ── 事件处理（私有）─────────────────────────────────────────────────────

    private void onPress(MouseEvent e, Component tabComponent) {
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        // 点击关闭按钮时不启动拖拽
        if (tabComponent instanceof ClosableTabComponent ctc && ctc.isInCloseButton(e.getX(), e.getY())) return;

        Point panePoint = toPanePoint(e.getPoint(), tabComponent);
        int idx = tabIndexAt(panePoint, tabComponent);
        if (idx < 0 || isPlusTab(idx)) return;

        state.draggedTabIndex = idx;
        state.pressPointInPane = panePoint;
        state.active = false;
    }

    private void onDrag(MouseEvent e, Component tabComponent) {
        if (!state.isReady()) return;
        Point panePoint = toPanePoint(e.getPoint(), tabComponent);

        // 超过阈值才真正进入拖拽
        if (!state.active) {
            if (panePoint.distance(state.pressPointInPane) < DRAG_THRESHOLD) return;
            state.active = true;
            tabbedPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            showDropIndicator();
        }

        int target = calcDropTarget(panePoint.x);
        if (target != state.dropTargetIndex) {
            state.dropTargetIndex = target;
            updateDropIndicator(target);
        }
    }

    private void onRelease() {
        try {
            if (state.active && state.dropTargetIndex >= 0
                    && state.dropTargetIndex != state.draggedTabIndex) {
                moveTab(state.draggedTabIndex, state.dropTargetIndex);
            }
        } finally {
            endDrag();
        }
    }

    // ── 坐标 & 索引工具 ──────────────────────────────────────────────────────

    /** 将 tabComponent 上的点转换到 tabbedPane 坐标系 */
    private Point toPanePoint(Point componentPoint, Component tabComponent) {
        return SwingUtilities.convertPoint(tabComponent, componentPoint, tabbedPane);
    }

    /** 根据坐标找 Tab 索引，找不到则用 indexOfTabComponent 兜底 */
    private int tabIndexAt(Point panePoint, Component tabComponent) {
        int idx = tabbedPane.indexAtLocation(panePoint.x, panePoint.y);
        if (idx < 0) idx = tabbedPane.indexOfTabComponent(tabComponent);
        return idx;
    }

    /** "+" Tab 始终是最后一个，不参与拖拽 */
    private boolean isPlusTab(int idx) {
        return idx == tabbedPane.getTabCount() - 1;
    }

    /**
     * 根据鼠标 X 坐标计算落点索引（不包含 "+" Tab）。
     * 鼠标在某 Tab 左半部分 → 落在该 Tab 前；右半部分 → 落在该 Tab 后。
     */
    private int calcDropTarget(int paneX) {
        int count = tabbedPane.getTabCount() - 1; // 排除 "+" Tab
        if (count <= 0) return -1;
        for (int i = 0; i < count; i++) {
            Rectangle b = tabbedPane.getBoundsAt(i);
            if (b != null && paneX < b.x + b.width / 2) return i;
        }
        return count - 1; // 鼠标在所有 Tab 右侧，落到最后
    }

    // ── GlassPane 指示线 ─────────────────────────────────────────────────────

    private void showDropIndicator() {
        JRootPane rootPane = SwingUtilities.getRootPane(tabbedPane);
        if (rootPane == null) return;
        savedGlassPane = rootPane.getGlassPane();
        dropIndicator = new TabDropIndicator();
        rootPane.setGlassPane(dropIndicator);
        dropIndicator.setVisible(true);
    }

    private void updateDropIndicator(int targetIdx) {
        if (dropIndicator == null) return;
        if (targetIdx < 0) { dropIndicator.clearLine(); return; }

        Rectangle b = tabbedPane.getBoundsAt(targetIdx);
        if (b == null) { dropIndicator.clearLine(); return; }

        // 拖动方向决定指示线贴左边还是右边
        int lineX = (targetIdx <= state.draggedTabIndex) ? b.x : (b.x + b.width);
        Point p = SwingUtilities.convertPoint(tabbedPane, lineX, b.y, dropIndicator);
        dropIndicator.show(p.x, p.y, b.height);
    }

    private void endDrag() {
        state.reset();
        tabbedPane.setCursor(Cursor.getDefaultCursor());

        if (dropIndicator != null) dropIndicator.setVisible(false);

        JRootPane rootPane = SwingUtilities.getRootPane(tabbedPane);
        if (rootPane != null && savedGlassPane != null) {
            rootPane.setGlassPane(savedGlassPane);
            savedGlassPane.setVisible(false);
        }
        dropIndicator = null;
        savedGlassPane = null;
    }

    // ── Tab 移动 ──────────────────────────────────────────────────────────────

    /**
     * 将 from 位置的 Tab 移动到 to 位置。
     * 完整保留 tabComponent（ClosableTabComponent）、content、title、icon、tooltip、enabled，
     * 并同步 selectedIndex 与 previewTabIndex。
     */
    private void moveTab(int from, int to) {
        int tabCount = tabbedPane.getTabCount();
        if (from == to || from < 0 || to < 0 || from >= tabCount || to >= tabCount) return;

        // 1. 读取 from 位置 Tab 的全部数据
        String title      = tabbedPane.getTitleAt(from);
        Icon icon         = tabbedPane.getIconAt(from);
        Component content = tabbedPane.getComponentAt(from);
        String tooltip    = tabbedPane.getToolTipTextAt(from);
        boolean enabled   = tabbedPane.isEnabledAt(from);
        Component tabComp = tabbedPane.getTabComponentAt(from);
        int selectedIndex = tabbedPane.getSelectedIndex();
        int previewIdx    = previewIndexGetter.get();

        // 2. 删除原位置，再插入目标位置
        tabbedPane.removeTabAt(from);
        int insertAt = Math.min(to, tabbedPane.getTabCount()); // 防止越界
        tabbedPane.insertTab(title, icon, content, tooltip, insertAt);
        tabbedPane.setEnabledAt(insertAt, enabled);
        if (tabComp != null) tabbedPane.setTabComponentAt(insertAt, tabComp);

        // 3. 同步 selectedIndex 和 previewTabIndex（索引因 remove+insert 发生了偏移）
        tabbedPane.setSelectedIndex(recalcIndex(selectedIndex, from, insertAt));
        if (previewIdx >= 0) previewIndexSetter.set(recalcIndex(previewIdx, from, insertAt));

        log.debug("Tab moved: {} → {} (insertAt={})", from, to, insertAt);
    }

    /**
     * 计算某个索引在 removeTabAt(from) + insertTab(insertAt) 之后的新位置。
     */
    private int recalcIndex(int idx, int removedFrom, int insertAt) {
        if (idx == removedFrom) return insertAt;               // 就是被移动的那个
        int afterRemove = (idx > removedFrom) ? idx - 1 : idx; // 模拟 remove 后的偏移
        return (afterRemove >= insertAt) ? afterRemove + 1 : afterRemove; // 模拟 insert 后的偏移
    }

    // ── 内部类：拖拽位置指示线（覆盖在 RootPane GlassPane 上）────────────────

    /**
     * 透明面板，绘制一条蓝色竖线 + 上下箭头，指示 Tab 将要插入的位置。
     * 颜色自动跟随 FlatLaf 主题。
     */
    static class TabDropIndicator extends JPanel {

        private int lineX = -1;
        private int lineY;
        private int lineHeight;

        TabDropIndicator() {
            setOpaque(false);
            setEnabled(false); // 不消费鼠标事件
        }

        void show(int x, int y, int height) {
            lineX = x; lineY = y; lineHeight = height;
            repaint();
        }

        void clearLine() {
            lineX = -1;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (lineX < 0) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 主题自适应颜色：优先取 FlatLaf 焦点色
            Color color = UIManager.getColor("Component.focusColor");
            if (color == null) color = UIManager.getColor("TabbedPane.underlineColor");
            if (color == null) color = new Color(60, 131, 197); // FlatLaf 默认蓝
            g2.setColor(color);

            // 竖线
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(lineX, lineY, lineX, lineY + lineHeight);

            // 顶部向下三角
            int a = 5;
            g2.fillPolygon(
                    new int[]{lineX - a, lineX + a, lineX},
                    new int[]{lineY, lineY, lineY + a * 2}, 3);
            // 底部向上三角
            int by = lineY + lineHeight;
            g2.fillPolygon(
                    new int[]{lineX - a, lineX + a, lineX},
                    new int[]{by, by, by - a * 2}, 3);

            g2.dispose();
        }
    }
}

