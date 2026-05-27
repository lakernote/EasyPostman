package com.laker.postman.panel.performance;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class PerformancePanelViewFactoryTest {

    @Test
    public void resultControlsShouldSwitchLinkedResultTabs() {
        PerformancePanelViewFactory viewFactory = new PerformancePanelViewFactory();
        AtomicBoolean trendEnabled = new AtomicBoolean(true);
        AtomicBoolean reportRealtime = new AtomicBoolean(false);
        AtomicBoolean compactDetails = new AtomicBoolean(false);
        AtomicInteger reportRefreshCount = new AtomicInteger();
        AtomicInteger saveAllCount = new AtomicInteger();
        AtomicInteger saveConfigCount = new AtomicInteger();

        PerformancePanelViewFactory.ResultSection resultSection = viewFactory.createResultSection(
                true,
                false,
                false,
                null,
                compactDetails::set,
                trendEnabled::set,
                reportRealtime::set,
                reportRefreshCount::incrementAndGet,
                saveAllCount::incrementAndGet,
                saveConfigCount::incrementAndGet
        );

        assertEquals(resultSection.resultTabbedPane().getSelectedIndex(), PerformancePanelViewFactory.RESULT_TAB_TREND);
        assertTrue(resultSection.trendButton().isSelected());
        assertTrue(resultSection.resultTabbedPane().getUI().getClass().getSimpleName().contains("HiddenResultTabs"));
        assertFalse(hasText(resultSection.resultPanel(), "结果视图"));
        assertFalse(hasText(resultSection.resultPanel(), "Result View"));
        Container resultSwitcher = resultSection.trendButton().getParent();
        assertSame(resultSwitcher.getComponent(0), resultSection.trendButton());
        assertSame(resultSwitcher.getComponent(1), resultSection.reportButton());
        assertSame(resultSwitcher.getComponent(2), resultSection.resultTableButton());

        JCheckBox compactDetailsCheckBox = resultSection.efficientCheckBox();
        assertEquals(compactDetailsCheckBox.getText(), I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_DETAIL_COMPACT));
        assertFalse(compactDetailsCheckBox.isSelected());
        compactDetailsCheckBox.doClick();
        assertTrue(compactDetails.get());
        assertEquals(saveAllCount.get(), 1);
        assertEquals(saveConfigCount.get(), 1);

        resultSection.trendButton().doClick();
        assertEquals(resultSection.resultTabbedPane().getSelectedIndex(), PerformancePanelViewFactory.RESULT_TAB_TREND);

        resultSection.trendCheckBox().doClick();
        assertFalse(trendEnabled.get());
        assertEquals(resultSection.resultTabbedPane().getSelectedIndex(), PerformancePanelViewFactory.RESULT_TAB_TABLE);
        assertEquals(saveConfigCount.get(), 2);

        resultSection.trendButton().doClick();
        resultSection.trendCheckBox().doClick();
        assertTrue(trendEnabled.get());
        assertEquals(resultSection.resultTabbedPane().getSelectedIndex(), PerformancePanelViewFactory.RESULT_TAB_TREND);
        assertEquals(saveConfigCount.get(), 3);

        resultSection.reportButton().doClick();
        assertEquals(resultSection.resultTabbedPane().getSelectedIndex(), PerformancePanelViewFactory.RESULT_TAB_REPORT);
        assertEquals(reportRefreshCount.get(), 1);

        resultSection.reportRefreshModeBox().setSelectedIndex(1);
        assertTrue(reportRealtime.get());
        assertEquals(resultSection.resultTabbedPane().getSelectedIndex(), PerformancePanelViewFactory.RESULT_TAB_REPORT);
        assertEquals(reportRefreshCount.get(), 2);
        assertEquals(saveConfigCount.get(), 4);
    }

    @Test
    public void topToolbarShouldOnlyContainExecutionControls() {
        PerformancePanelViewFactory viewFactory = new PerformancePanelViewFactory();

        PerformancePanelViewFactory.ToolbarSection toolbarSection = viewFactory.createToolbarSection(
                () -> {
                }
        );

        assertEquals(countCheckBoxes(toolbarSection.topPanel()), 0);
        assertNotNull(toolbarSection.runBtn());
        assertNotNull(toolbarSection.stopBtn());
        assertNotNull(toolbarSection.refreshBtn());
    }

    private static int countCheckBoxes(Component component) {
        int count = component instanceof JCheckBox ? 1 : 0;
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                count += countCheckBoxes(child);
            }
        }
        return count;
    }

    private static boolean hasText(Component component, String text) {
        if (component instanceof JLabel label && text.equals(label.getText())) {
            return true;
        }
        if (component instanceof AbstractButton button && text.equals(button.getText())) {
            return true;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (hasText(child, text)) {
                    return true;
                }
            }
        }
        return false;
    }
}
