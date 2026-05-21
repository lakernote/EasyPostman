package com.laker.postman.panel.performance.result;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.jfree.chart.ChartPanel;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceTrendPanelTest extends AbstractSwingUiTest {

    @Test
    public void shouldUseProtocolTabsAndDefaultToSeparateChartsWithHttpVirtualUsers() {
        PerformanceTrendPanel panel = SingletonFactory.getInstance(PerformanceTrendPanel.class);
        JTabbedPane tabs = findFirst(panel, JTabbedPane.class);
        assertEquals(tabs.getTabCount(), 3);
        assertEquals(tabs.getTitleAt(0), PerformanceProtocol.HTTP.getDisplayName());
        assertEquals(tabs.getTitleAt(1), PerformanceProtocol.WEBSOCKET.getDisplayName());
        assertEquals(tabs.getTitleAt(2), PerformanceProtocol.SSE.getDisplayName());

        Component httpTab = tabs.getComponentAt(0);

        JToggleButton separateButton = findToggleButton(
                httpTab,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_SEPARATE_CHARTS)
        );
        JToggleButton combinedButton = findToggleButton(
                httpTab,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_COMBINED_CHART)
        );

        assertTrue(separateButton.isSelected());
        assertFalse(combinedButton.isSelected());
        assertEquals(countVisibleCharts(httpTab), 4);
        assertTrue(hasCheckBox(
                httpTab,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_VIRTUAL_USERS)
        ));

        combinedButton.doClick();

        assertTrue(combinedButton.isSelected());
        assertFalse(separateButton.isSelected());
        assertEquals(countVisibleCharts(httpTab), 1);
        assertFalse(findAll(httpTab, JCheckBox.class).isEmpty());
    }

    @Test
    public void shouldStretchSeparateChartGridWhenViewportIsTallerThanPreferredHeight() {
        PerformanceTrendPanel panel = SingletonFactory.getInstance(PerformanceTrendPanel.class);
        JTabbedPane tabs = findFirst(panel, JTabbedPane.class);
        Component httpTab = tabs.getComponentAt(0);
        JScrollPane splitScrollPane = findFirst(httpTab, JScrollPane.class);
        JViewport viewport = splitScrollPane.getViewport();
        viewport.setExtentSize(new Dimension(1600, 900));

        Component view = viewport.getView();

        assertTrue(((Scrollable) view).getScrollableTracksViewportHeight());
    }

    private static boolean hasCheckBox(Component root, String text) {
        for (JCheckBox checkBox : findAll(root, JCheckBox.class)) {
            if (text.equals(checkBox.getText())) {
                return true;
            }
        }
        return false;
    }

    private static int countVisibleCharts(Component root) {
        int count = 0;
        for (ChartPanel chartPanel : findAll(root, ChartPanel.class)) {
            if (isEffectivelyVisible(chartPanel, root)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isEffectivelyVisible(Component component, Component root) {
        Component current = component;
        while (current != null) {
            if (!current.isVisible()) {
                return false;
            }
            if (current == root) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static JToggleButton findToggleButton(Component root, String text) {
        for (JToggleButton button : findAll(root, JToggleButton.class)) {
            if (text.equals(button.getText())) {
                return button;
            }
        }
        throw new AssertionError("Toggle button not found: " + text);
    }

    private static <T extends Component> T findFirst(Component root, Class<T> type) {
        List<T> components = findAll(root, type);
        if (components.isEmpty()) {
            throw new AssertionError("Component not found: " + type.getName());
        }
        return components.get(0);
    }

    private static <T extends Component> List<T> findAll(Component root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        collect(root, type, matches);
        return matches;
    }

    private static <T extends Component> void collect(Component component, Class<T> type, List<T> matches) {
        if (type.isInstance(component)) {
            matches.add(type.cast(component));
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collect(child, type, matches);
            }
        }
    }
}
