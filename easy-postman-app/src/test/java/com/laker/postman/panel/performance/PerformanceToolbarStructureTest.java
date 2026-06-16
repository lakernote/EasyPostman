package com.laker.postman.panel.performance;

import com.laker.postman.common.component.MemoryLabel;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertTrue;

public class PerformanceToolbarStructureTest extends AbstractSwingUiTest {

    @Test
    public void workerEndpointInputShouldLiveOutsidePrimaryToolbarRow() {
        PerformancePanelViewFactory.ToolbarSection toolbarSection = toolbarSection();
        JTextField workerEndpointsField = toolbarSection.workerEndpointsField();
        Container workerEndpointRow = workerEndpointsField.getParent();

        try {
            assertNotSame(toolbarSection.remoteModeCheckBox().getParent(), workerEndpointRow);
            assertFalse(workerEndpointRow.isVisible());

            toolbarSection.remoteModeCheckBox().doClick();

            assertTrue(workerEndpointRow.isVisible());
            assertTrue(workerEndpointsField.isVisible());
        } finally {
            MemoryLabel memoryLabel = findFirst(toolbarSection.topPanel(), MemoryLabel.class);
            if (memoryLabel != null) {
                memoryLabel.stopAutoRefresh();
            }
        }
    }

    @Test
    public void primaryToolbarControlsShouldNotStretchAcrossTheWholeRow() {
        PerformancePanelViewFactory.ToolbarSection toolbarSection = toolbarSection();
        JComponent topPanel = toolbarSection.topPanel();
        topPanel.setSize(new Dimension(1600, 96));
        layoutRecursively(topPanel);

        Container remotePanel = toolbarSection.remoteModeCheckBox().getParent();
        Container primaryPanel = remotePanel.getParent();

        try {
            int extraWidth = primaryPanel.getWidth() - primaryPanel.getPreferredSize().width;
            assertTrue(extraWidth <= 32, "primary toolbar controls should stay compact, extra width: " + extraWidth);
        } finally {
            MemoryLabel memoryLabel = findFirst(toolbarSection.topPanel(), MemoryLabel.class);
            if (memoryLabel != null) {
                memoryLabel.stopAutoRefresh();
            }
        }
    }

    @Test
    public void workerEndpointInputShouldStayCompactWhenRemoteIsEnabled() {
        PerformancePanelViewFactory.ToolbarSection toolbarSection = toolbarSection();
        JComponent topPanel = toolbarSection.topPanel();
        JTextField workerEndpointsField = toolbarSection.workerEndpointsField();

        try {
            toolbarSection.remoteModeCheckBox().doClick();
            topPanel.setSize(new Dimension(1600, 120));
            layoutRecursively(topPanel);

            assertTrue(
                    workerEndpointsField.getParent().getWidth() <= 620,
                    "worker endpoint row should stay compact, actual width: "
                            + workerEndpointsField.getParent().getWidth()
            );
            assertTrue(
                    workerEndpointsField.getWidth() <= 520,
                    "worker endpoint field should stay compact, actual width: "
                            + workerEndpointsField.getWidth()
            );
        } finally {
            MemoryLabel memoryLabel = findFirst(toolbarSection.topPanel(), MemoryLabel.class);
            if (memoryLabel != null) {
                memoryLabel.stopAutoRefresh();
            }
        }
    }

    @Test
    public void workerEndpointChineseLabelShouldBeLocalizedAndShort() {
        ResourceBundle zhMessages = ResourceBundle.getBundle("messages", Locale.SIMPLIFIED_CHINESE);

        assertEquals(zhMessages.getString("performance.remote.workers.label"), "节点");
    }

    @Test
    public void workerEndpointInputPreferredWidthShouldStayCompact() {
        PerformancePanelViewFactory.ToolbarSection toolbarSection = toolbarSection();
        try {
            assertTrue(
                    toolbarSection.workerEndpointsField().getPreferredSize().width <= 440,
                    "worker endpoint preferred width should avoid an oversized error outline"
            );
        } finally {
            MemoryLabel memoryLabel = findFirst(toolbarSection.topPanel(), MemoryLabel.class);
            if (memoryLabel != null) {
                memoryLabel.stopAutoRefresh();
            }
        }
    }

    private static PerformancePanelViewFactory.ToolbarSection toolbarSection() {
        return new PerformancePanelViewFactory().createToolbarSection(
                () -> {
                },
                () -> {
                },
                () -> {
                },
                () -> {
                },
                false,
                "127.0.0.1:19090",
                ignored -> {
                },
                ignored -> {
                }
        );
    }

    private static void layoutRecursively(Component component) {
        if (component instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layoutRecursively(child);
            }
        }
    }

    private static <T> T findFirst(java.awt.Component component, Class<T> type) {
        if (type.isInstance(component)) {
            return type.cast(component);
        }
        if (component instanceof Container container) {
            for (java.awt.Component child : container.getComponents()) {
                T result = findFirst(child, type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
