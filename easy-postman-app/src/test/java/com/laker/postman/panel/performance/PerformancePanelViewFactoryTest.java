package com.laker.postman.panel.performance;

import com.laker.postman.common.component.MemoryLabel;
import com.laker.postman.common.component.placeholder.PerformanceTrendPlaceholderPanel;
import com.laker.postman.common.component.button.ExportButton;
import com.laker.postman.common.component.button.HelpButton;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.panel.performance.result.LazyPerformanceTrendPanel;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class PerformancePanelViewFactoryTest extends AbstractSwingUiTest {

    @Test
    public void treeSectionShouldUseThemeAwareTreeSurface() {
        PerformancePanelViewFactory viewFactory = new PerformancePanelViewFactory();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");

        PerformancePanelViewFactory.TreeSection treeSection = viewFactory.createTreeSection(new DefaultTreeModel(root));

        JTree tree = treeSection.tree();
        assertEquals(tree.getBackground(), uiColor("Tree.background", ModernColors.getCardBackgroundColor()));
        assertEquals(tree.getForeground(), uiColor("Tree.foreground", ModernColors.getTextPrimary()));
    }

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
    public void resultSectionShouldKeepTrendChartsLazyUntilSamplesArrive() {
        PerformancePanelViewFactory viewFactory = new PerformancePanelViewFactory();

        PerformancePanelViewFactory.ResultSection resultSection = viewFactory.createResultSection(
                true,
                false,
                true,
                null,
                ignored -> {
                },
                ignored -> {
                },
                ignored -> {
                },
                () -> {
                },
                () -> {
                },
                () -> {
                }
        );

        Component trendComponent = resultSection.resultTabbedPane().getComponentAt(PerformancePanelViewFactory.RESULT_TAB_TREND);

        assertTrue(trendComponent instanceof LazyPerformanceTrendPanel);
        LazyPerformanceTrendPanel lazyTrendPanel = (LazyPerformanceTrendPanel) trendComponent;
        assertFalse(lazyTrendPanel.isTrendPanelCreated());
        assertTrue(hasComponent(lazyTrendPanel, PerformanceTrendPlaceholderPanel.class));
    }

    @Test
    public void topToolbarShouldExposeRunRefreshAndExportControls() {
        PerformancePanelViewFactory viewFactory = new PerformancePanelViewFactory();
        AtomicInteger exportCount = new AtomicInteger();
        AtomicInteger usageHelpCount = new AtomicInteger();
        AtomicInteger remoteToggleCount = new AtomicInteger();

        PerformancePanelViewFactory.ToolbarSection toolbarSection = viewFactory.createToolbarSection(
                exportCount::incrementAndGet,
                usageHelpCount::incrementAndGet,
                () -> {
                },
                false,
                "127.0.0.1:19090",
                ignored -> remoteToggleCount.incrementAndGet(),
                ignored -> remoteToggleCount.incrementAndGet()
        );

        assertEquals(countCheckBoxes(toolbarSection.topPanel()), 1);
        assertEquals(countTextFields(toolbarSection.topPanel()), 1);
        assertNotNull(toolbarSection.runBtn());
        assertNotNull(toolbarSection.stopBtn());
        assertNotNull(toolbarSection.refreshBtn());
        assertNotNull(toolbarSection.exportBtn());
        assertNotNull(toolbarSection.usageHelpBtn());
        assertNotNull(toolbarSection.remoteModeCheckBox());
        assertNotNull(toolbarSection.workerEndpointsField());
        assertNotNull(toolbarSection.progressLabel());
        assertNotNull(toolbarSection.limitLabel());
        assertEquals(toolbarSection.progressLabel().getText(), "0/0");
        assertEquals(toolbarSection.limitLabel().getText(), "");
        assertFalse(toolbarSection.limitLabel().isVisible());
        assertNotNull(toolbarSection.progressLabel().getIcon());
        assertNull(toolbarSection.limitLabel().getIcon());
        assertFalse(toolbarSection.workerEndpointsField().isEditable());
        assertEquals(toolbarSection.workerEndpointsField().getForeground(), ModernColors.getTextDisabled());
        MemoryLabel memoryLabel = findFirst(toolbarSection.topPanel(), MemoryLabel.class);
        assertNotNull(memoryLabel);
        assertTrue(memoryLabel.getText().contains("/"));
        assertFalse(memoryLabel.getText().contains(" / "));
        assertFalse(memoryLabel.getText().matches(".*\\d+\\.\\d{2,}(MB|GB).*"));
        memoryLabel.stopAutoRefresh();
        assertTrue(hasComponent(toolbarSection.topPanel(), ExportButton.class));
        assertTrue(hasComponent(toolbarSection.topPanel(), HelpButton.class));
        assertFalse(toolbarSection.remoteModeCheckBox().isSelected());
        assertEquals(toolbarSection.workerEndpointsField().getText(), "127.0.0.1:19090");

        toolbarSection.exportBtn().doClick();
        assertEquals(exportCount.get(), 1);
        toolbarSection.usageHelpBtn().doClick();
        assertEquals(usageHelpCount.get(), 1);
        toolbarSection.remoteModeCheckBox().doClick();
        assertEquals(remoteToggleCount.get(), 1);
        assertTrue(toolbarSection.workerEndpointsField().isEditable());
        assertEquals(toolbarSection.workerEndpointsField().getForeground(), ModernColors.getTextPrimary());
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

    private static Color uiColor(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return color == null ? fallback : color;
    }

    private static int countTextFields(Component component) {
        int count = component instanceof JTextField ? 1 : 0;
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                count += countTextFields(child);
            }
        }
        return count;
    }

    private static boolean hasComponent(Component component, Class<?> type) {
        if (type.isInstance(component)) {
            return true;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (hasComponent(child, type)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static <T> T findFirst(Component component, Class<T> type) {
        if (type.isInstance(component)) {
            return type.cast(component);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                T result = findFirst(child, type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
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
