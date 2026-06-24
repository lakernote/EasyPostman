package com.laker.postman.common.component.tab;

import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class TabbedPaneDragHandlerTest extends AbstractSwingUiTest {

    @Test
    public void wrappedTabsDropTargetShouldUsePointerRow() throws Exception {
        BoundsBackedTabbedPane tabs = new BoundsBackedTabbedPane(
                new Rectangle(0, 0, 100, 30),
                new Rectangle(100, 0, 100, 30),
                new Rectangle(200, 0, 100, 30),
                new Rectangle(0, 30, 100, 30),
                new Rectangle(100, 30, 100, 30),
                new Rectangle(200, 30, 100, 30),
                new Rectangle(300, 30, 30, 30)
        );
        TabbedPaneDragHandler handler = TabbedPaneDragHandler.install(tabs, () -> -1, value -> {
        });

        Point secondRowFirstTabLeftHalf = new Point(10, 45);

        assertEquals(handler.dropSlotAt(secondRowFirstTabLeftHalf), 3);
    }

    @Test
    public void wrappedTabsDropTargetShouldUseRightHalfAsNextSlotInSameRow() {
        BoundsBackedTabbedPane tabs = new BoundsBackedTabbedPane(
                new Rectangle(0, 0, 100, 30),
                new Rectangle(100, 0, 100, 30),
                new Rectangle(200, 0, 100, 30),
                new Rectangle(0, 30, 100, 30),
                new Rectangle(100, 30, 100, 30),
                new Rectangle(200, 30, 100, 30),
                new Rectangle(300, 30, 30, 30)
        );
        TabbedPaneDragHandler handler = TabbedPaneDragHandler.install(tabs, () -> -1, value -> {
        });

        assertEquals(handler.dropSlotAt(new Point(75, 45)), 4);
    }

    @Test
    public void wrappedTabsDropTargetAfterLastNormalTabShouldStayBeforePlusTab() {
        BoundsBackedTabbedPane tabs = new BoundsBackedTabbedPane(
                new Rectangle(0, 0, 100, 30),
                new Rectangle(100, 0, 100, 30),
                new Rectangle(0, 30, 100, 30),
                new Rectangle(100, 30, 100, 30),
                new Rectangle(200, 30, 30, 30)
        );
        TabbedPaneDragHandler handler = TabbedPaneDragHandler.install(tabs, () -> -1, value -> {
        });

        assertEquals(handler.dropSlotAt(new Point(180, 45)), 4);
    }

    @Test
    public void movingTabForwardShouldConvertDropSlotAfterRemovalAndKeepPlusLast() {
        JTabbedPane tabs = titledTabbedPane("A", "B", "C", "D", "+");
        TabbedPaneDragHandler handler = TabbedPaneDragHandler.install(tabs, () -> -1, value -> {
        });

        handler.moveTabToSlot(0, 3);

        assertEquals(tabTitles(tabs), List.of("B", "C", "A", "D", "+"));
    }

    @Test
    public void movingTabBackwardShouldInsertAtDropSlotAndKeepPlusLast() {
        JTabbedPane tabs = titledTabbedPane("A", "B", "C", "D", "+");
        TabbedPaneDragHandler handler = TabbedPaneDragHandler.install(tabs, () -> -1, value -> {
        });

        handler.moveTabToSlot(3, 0);

        assertEquals(tabTitles(tabs), List.of("D", "A", "B", "C", "+"));
    }

    @Test
    public void movingTabToAdjacentSelfSlotShouldBeNoop() {
        JTabbedPane tabs = titledTabbedPane("A", "B", "C", "+");
        TabbedPaneDragHandler handler = TabbedPaneDragHandler.install(tabs, () -> -1, value -> {
        });

        handler.moveTabToSlot(1, 2);

        assertEquals(tabTitles(tabs), List.of("A", "B", "C", "+"));
    }

    private static JTabbedPane titledTabbedPane(String... titles) {
        JTabbedPane tabs = new JTabbedPane();
        for (String title : titles) {
            tabs.addTab(title, new JPanel());
        }
        return tabs;
    }

    private static List<String> tabTitles(JTabbedPane tabs) {
        List<String> titles = new ArrayList<>();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            titles.add(tabs.getTitleAt(i));
        }
        return titles;
    }

    private static final class BoundsBackedTabbedPane extends JTabbedPane {
        private final Rectangle[] bounds;

        private BoundsBackedTabbedPane(Rectangle... bounds) {
            this.bounds = bounds;
            for (int i = 0; i < bounds.length; i++) {
                addTab(i == bounds.length - 1 ? "+" : "Tab " + i, new JPanel());
            }
        }

        @Override
        public Rectangle getBoundsAt(int index) {
            return new Rectangle(bounds[index]);
        }
    }
}
