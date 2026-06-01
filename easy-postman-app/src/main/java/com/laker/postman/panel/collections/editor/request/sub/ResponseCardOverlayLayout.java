package com.laker.postman.panel.collections.editor.request.sub;

import java.awt.*;

/**
 * 响应卡片遮罩布局。
 * <p>
 * cardPanel 和 loadingOverlay 需要完全重叠，独立布局类比嵌在 ResponsePanel 里更容易复用和理解。
 */
final class ResponseCardOverlayLayout implements LayoutManager2 {

    @Override
    public void addLayoutComponent(String name, Component comp) {
        // 此布局不需要根据名称添加组件。
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        // 此布局不需要移除组件逻辑。
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
            int width = parent.getWidth();
            int height = parent.getHeight();
            for (Component component : parent.getComponents()) {
                component.setBounds(0, 0, width, height);
            }
        }
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        // 此布局不需要约束条件。
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
        // 此布局不需要缓存，无需失效逻辑。
    }
}
