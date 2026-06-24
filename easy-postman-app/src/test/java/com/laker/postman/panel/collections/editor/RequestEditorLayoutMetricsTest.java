package com.laker.postman.panel.collections.editor;

import com.laker.postman.common.component.tab.PlusTabComponent;
import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.service.setting.SettingManager;
import org.testng.annotations.Test;

import java.awt.Insets;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.LookAndFeel;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.formdev.flatlaf.FlatLightLaf;

import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_ALIGN_LEADING;
import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_TAB_ALIGNMENT;
import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_TAB_AREA_ALIGNMENT;
import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_TAB_WIDTH_MODE;
import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_TAB_WIDTH_MODE_PREFERRED;
import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_HAS_FULL_BORDER;
import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_SHOW_CONTENT_SEPARATOR;
import static org.testng.Assert.assertEquals;

public class RequestEditorLayoutMetricsTest {

    @Test
    public void requestEditorWorkspaceShouldKeepOneConsistentCardInset() {
        assertEquals(RequestEditorPanel.EDITOR_WORKSPACE_INSETS, new Insets(6, 6, 6, 6));
    }

    @Test
    public void requestEditorTabsShouldUseCompactIdeaLikeHeight() {
        assertEquals(RequestEditorPanel.REQUEST_TAB_HEIGHT, 34);
        assertEquals(RequestEditorPanel.REQUEST_TAB_INSETS, new Insets(2, 5, 2, 5));
        assertEquals(RequestEditorPanel.REQUEST_TAB_AREA_INSETS, new Insets(0, 0, 0, 5));
    }

    @Test
    public void requestEditorTabsShouldUseSingleContentSeparatorWithoutFullBorder() {
        UiSingletonPanel.setFactoryCreationAllowed(true);
        try {
            RequestEditorPanel panel = new RequestEditorPanel();
            panel.initUI();

            assertEquals(panel.getTabbedPane().getClientProperty(TABBED_PANE_HAS_FULL_BORDER), Boolean.FALSE);
            assertEquals(panel.getTabbedPane().getClientProperty(TABBED_PANE_SHOW_CONTENT_SEPARATOR), Boolean.TRUE);
            assertEquals(panel.getTabbedPane().getClientProperty(TABBED_PANE_TAB_WIDTH_MODE),
                    TABBED_PANE_TAB_WIDTH_MODE_PREFERRED);
            assertEquals(panel.getTabbedPane().getClientProperty(TABBED_PANE_TAB_AREA_ALIGNMENT),
                    TABBED_PANE_ALIGN_LEADING);
            assertEquals(panel.getTabbedPane().getClientProperty(TABBED_PANE_TAB_ALIGNMENT),
                    TABBED_PANE_ALIGN_LEADING);
        } finally {
            UiSingletonPanel.setFactoryCreationAllowed(false);
        }
    }

    @Test
    public void wrappedRequestEditorTabsShouldPreserveAddedOrderInVisualRows() throws Exception {
        LookAndFeel previousLookAndFeel = UIManager.getLookAndFeel();
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
            tabs.setUI(new RequestEditorTabbedPaneUi());
            for (int i = 0; i < 22; i++) {
                tabs.addTab("Tab " + i, new JPanel());
            }
            tabs.addTab(RequestEditorPanel.PLUS_TAB, new JPanel());
            tabs.setSelectedIndex(3);
            tabs.setSize(700, 180);
            tabs.doLayout();

            assertEquals(visualTabOrder(tabs), sequentialTabOrder(tabs.getTabCount()));
        } finally {
            if (previousLookAndFeel != null) {
                UIManager.setLookAndFeel(previousLookAndFeel);
            }
        }
    }

    @Test
    public void wrappedRequestEditorTabsShouldKeepCustomTabComponentsInsideRows() throws Exception {
        LookAndFeel previousLookAndFeel = UIManager.getLookAndFeel();
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
            tabs.setUI(new RequestEditorTabbedPaneUi());
            tabs.putClientProperty("JTabbedPane.tabHeight", RequestEditorPanel.REQUEST_TAB_HEIGHT);
            tabs.putClientProperty("JTabbedPane.tabInsets", RequestEditorPanel.REQUEST_TAB_INSETS);
            tabs.putClientProperty(TABBED_PANE_TAB_WIDTH_MODE, TABBED_PANE_TAB_WIDTH_MODE_PREFERRED);
            tabs.putClientProperty(TABBED_PANE_TAB_AREA_ALIGNMENT, TABBED_PANE_ALIGN_LEADING);
            tabs.putClientProperty(TABBED_PANE_TAB_ALIGNMENT, TABBED_PANE_ALIGN_LEADING);
            for (int i = 0; i < 18; i++) {
                tabs.addTab("Tab " + i, new JPanel());
                tabs.setTabComponentAt(i, fixedTabComponent(150, 28, "HTTP Tab " + i));
            }
            tabs.addTab(RequestEditorPanel.PLUS_TAB, new JPanel());
            tabs.setTabComponentAt(tabs.getTabCount() - 1, fixedTabComponent(28, 28, "+"));
            tabs.setSelectedIndex(4);
            tabs.setSize(1_360, 180);
            tabs.doLayout();

            for (int i = 0; i < tabs.getTabCount(); i++) {
                Rectangle bounds = tabs.getBoundsAt(i);
                assertEquals(bounds.x >= 0, true, "tab " + i + " should not start before the row");
                assertEquals(bounds.x + bounds.width <= tabs.getWidth(), true,
                        "tab " + i + " should not overflow the row: " + bounds);
            }
            for (int i = 0; i < tabs.getTabCount(); i++) {
                Rectangle first = tabs.getBoundsAt(i);
                for (int j = i + 1; j < tabs.getTabCount(); j++) {
                    Rectangle second = tabs.getBoundsAt(j);
                    if (rowY(first) == rowY(second)) {
                        assertEquals(first.intersects(second), false,
                                "tabs " + i + " and " + j + " should not overlap: " + first + " / " + second);
                    }
                }
            }
            assertEquals(maxSameRowComponentGap(tabs) <= 16, true,
                    "wrapped request tabs should stay compact instead of stretching across the row");
            assertTabComponentsDoNotOverlap(tabs);
        } finally {
            if (previousLookAndFeel != null) {
                UIManager.setLookAndFeel(previousLookAndFeel);
            }
        }
    }

    @Test
    public void plusTabComponentShouldUseStableCompactSize() {
        PlusTabComponent plusTab = new PlusTabComponent();

        assertEquals(plusTab.getPreferredSize(), new Dimension(28, 28));
        assertEquals(plusTab.getMinimumSize(), new Dimension(28, 28));
        assertEquals(plusTab.getMaximumSize(), new Dimension(28, 28));
    }

    @Test
    public void requestEditorTabsShouldUseConfiguredLayoutPolicy() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        UiSingletonPanel.setFactoryCreationAllowed(true);
        try {
            props.remove("request_editor_tabs_multiline");
            RequestEditorPanel defaultPanel = new RequestEditorPanel();
            defaultPanel.initUI();
            assertEquals(defaultPanel.getTabbedPane().getTabLayoutPolicy(), JTabbedPane.SCROLL_TAB_LAYOUT);

            props.setProperty("request_editor_tabs_multiline", "true");
            RequestEditorPanel multiLinePanel = new RequestEditorPanel();
            multiLinePanel.initUI();
            assertEquals(multiLinePanel.getTabbedPane().getTabLayoutPolicy(), JTabbedPane.WRAP_TAB_LAYOUT);
        } finally {
            props.clear();
            props.putAll(backup);
            UiSingletonPanel.setFactoryCreationAllowed(false);
        }
    }

    @Test
    public void emptyStateShouldKeepCompactVerticalInsetInsideEditorCard() {
        assertEquals(RequestEditorEmptyStatePanel.EMPTY_STATE_INSETS, new Insets(12, 20, 12, 20));
    }

    private static Properties getSettingsProperties() throws Exception {
        Field propsField = SettingManager.class.getDeclaredField("props");
        propsField.setAccessible(true);
        return (Properties) propsField.get(null);
    }

    private static List<Integer> visualTabOrder(JTabbedPane tabs) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            indices.add(i);
        }
        indices.sort(Comparator
                .comparingInt((Integer index) -> rowY(tabs.getBoundsAt(index)))
                .thenComparingInt(index -> tabs.getBoundsAt(index).x));
        return indices;
    }

    private static int rowY(Rectangle bounds) {
        return bounds.y / Math.max(1, bounds.height);
    }

    private static List<Integer> sequentialTabOrder(int tabCount) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < tabCount; i++) {
            indices.add(i);
        }
        return indices;
    }

    private static JLabel fixedTabComponent(int width, int height, String text) {
        JLabel label = new JLabel(text);
        label.setPreferredSize(new Dimension(width, height));
        return label;
    }

    private static int maxSameRowComponentGap(JTabbedPane tabs) {
        List<Rectangle> componentBounds = tabComponentBoundsInTabbedPane(tabs);
        componentBounds.sort(Comparator
                .comparingInt(RequestEditorLayoutMetricsTest::rowY)
                .thenComparingInt(bounds -> bounds.x));
        int maxGap = 0;
        for (int i = 1; i < componentBounds.size(); i++) {
            Rectangle previous = componentBounds.get(i - 1);
            Rectangle current = componentBounds.get(i);
            if (rowY(previous) == rowY(current)) {
                maxGap = Math.max(maxGap, current.x - (previous.x + previous.width));
            }
        }
        return maxGap;
    }

    private static void assertTabComponentsDoNotOverlap(JTabbedPane tabs) {
        List<Rectangle> componentBounds = tabComponentBoundsInTabbedPane(tabs);
        for (int i = 0; i < componentBounds.size(); i++) {
            Rectangle first = componentBounds.get(i);
            for (int j = i + 1; j < componentBounds.size(); j++) {
                Rectangle second = componentBounds.get(j);
                if (rowY(first) == rowY(second)) {
                    assertEquals(first.intersects(second), false,
                            "tab components " + i + " and " + j + " should not overlap: "
                                    + first + " / " + second);
                }
            }
        }
    }

    private static List<Rectangle> tabComponentBoundsInTabbedPane(JTabbedPane tabs) {
        List<Rectangle> bounds = new ArrayList<>();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component component = tabs.getTabComponentAt(i);
            if (component != null) {
                bounds.add(SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), tabs));
            }
        }
        return bounds;
    }
}
