package com.laker.postman.panel.collections.editor;

import com.formdev.flatlaf.ui.FlatTabbedPaneUI;

import javax.swing.JTabbedPane;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * Keeps wrapped request tabs in insertion order when read left-to-right and top-to-bottom.
 * Swing's wrapped tab layout is designed for property pages and stretches rows to fill the tab area.
 * Request editor tabs use fixed-size custom tab components, so top-wrapped rows must keep their
 * natural bounds to avoid sparse rows and overlap near the right edge.
 */
final class RequestEditorTabbedPaneUi extends FlatTabbedPaneUI {
    @Override
    protected LayoutManager createLayoutManager() {
        if (tabPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT) {
            return super.createLayoutManager();
        }
        return new FlatTabbedPaneLayout() {
            @Override
            public void calculateLayoutInfo() {
                super.calculateLayoutInfo();
                if (tabPane.getTabPlacement() == TOP && tabPane.getTabLayoutPolicy() == JTabbedPane.WRAP_TAB_LAYOUT) {
                    compactTopWrappedTabs();
                }
            }

            @Override
            public void layoutContainer(Container parent) {
                super.layoutContainer(parent);
                if (tabPane.getTabPlacement() == TOP && tabPane.getTabLayoutPolicy() == JTabbedPane.WRAP_TAB_LAYOUT) {
                    compactTopWrappedTabs();
                    layoutTopWrappedTabComponents();
                }
            }

            @Override
            protected void normalizeTabRuns(int tabPlacement, int tabCount, int start, int max) {
                if (tabPlacement != TOP) {
                    super.normalizeTabRuns(tabPlacement, tabCount, start, max);
                }
            }

            @Override
            protected void padSelectedTab(int tabPlacement, int selectedIndex) {
                if (tabPlacement != TOP) {
                    super.padSelectedTab(tabPlacement, selectedIndex);
                }
            }
        };
    }

    private void compactTopWrappedTabs() {
        int tabCount = tabPane.getTabCount();
        if (tabCount == 0 || rects == null || rects.length < tabCount) {
            return;
        }

        Insets insets = tabPane.getInsets();
        Insets tabAreaInsets = getTabAreaInsets(TOP);
        int startX = insets.left + tabAreaInsets.left;
        int maxX = Math.max(startX, tabPane.getWidth() - insets.right - tabAreaInsets.right);
        int x = startX;
        int y = insets.top + tabAreaInsets.top;
        int rowHeight = 0;
        int rowCount = 1;
        FontMetrics metrics = getFontMetrics();

        for (int i = 0; i < tabCount; i++) {
            int width = Math.max(0, calculateTabWidth(TOP, i, metrics));
            int height = Math.max(1, calculateTabHeight(TOP, i, metrics.getHeight()));
            if (x > startX && x + width > maxX) {
                x = startX;
                y += Math.max(rowHeight, height);
                rowHeight = 0;
                rowCount++;
            }

            rects[i].setBounds(x, y, width, height);
            x += width;
            rowHeight = Math.max(rowHeight, height);
        }
        runCount = Math.max(runCount, rowCount);
    }

    private void layoutTopWrappedTabComponents() {
        int tabCount = tabPane.getTabCount();
        for (int i = 0; i < tabCount; i++) {
            Component component = tabPane.getTabComponentAt(i);
            if (component == null || component.getParent() == null) {
                continue;
            }

            Rectangle tabBounds = rects[i];
            Insets tabInsets = getTabInsets(TOP, i);
            Dimension preferredSize = component.getPreferredSize();
            int availableWidth = tabBounds.width - tabInsets.left - tabInsets.right;
            int availableHeight = tabBounds.height - tabInsets.top - tabInsets.bottom;
            int x = tabBounds.x + tabInsets.left + ((availableWidth - preferredSize.width) / 2);
            int y = tabBounds.y + tabInsets.top + ((availableHeight - preferredSize.height) / 2);
            x -= component.getParent().getX();
            y -= component.getParent().getY();
            component.setBounds(x, y, preferredSize.width, preferredSize.height);
        }
    }
}
