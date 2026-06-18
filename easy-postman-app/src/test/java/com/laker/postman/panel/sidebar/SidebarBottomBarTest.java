package com.laker.postman.panel.sidebar;

import com.laker.postman.common.component.ToolWindowStripeMetrics;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class SidebarBottomBarTest extends AbstractSwingUiTest {

    @Test
    public void bottomBarIconActionsShouldUseCenteredSidebarActionCells() {
        SidebarBottomBar bottomBar = noopBottomBar();

        List<JLabel> labels = collectIconLabels(bottomBar.leftPanel(), bottomBar.rightPanel());
        Dimension actionSize = ToolWindowStripeMetrics.actionSize();

        assertEquals(SidebarBottomBar.STRIPE_THICKNESS, ToolWindowStripeMetrics.STRIPE_THICKNESS);
        assertEquals(SidebarBottomBar.BOTTOM_BAR_ACTION_SIZE, ToolWindowStripeMetrics.ACTION_SIZE);
        assertEquals(labels.size(), 5);
        for (JLabel label : labels) {
            assertEquals(label.getPreferredSize(), actionSize);
            assertEquals(label.getMinimumSize(), actionSize);
            assertEquals(label.getMaximumSize(), actionSize);
            assertEquals(label.getHorizontalAlignment(), SwingConstants.CENTER);
            assertEquals(label.getVerticalAlignment(), SwingConstants.CENTER);
            Insets insets = label.getBorder().getBorderInsets(label);
            assertEquals(insets.top, 0);
            assertEquals(insets.left, 0);
            assertEquals(insets.bottom, 0);
            assertEquals(insets.right, 0);
        }
    }

    private static List<JLabel> collectIconLabels(JPanel... panels) {
        List<JLabel> labels = new ArrayList<>();
        for (JPanel panel : panels) {
            for (Component component : panel.getComponents()) {
                if (component instanceof JLabel label && label.getIcon() != null) {
                    labels.add(label);
                }
            }
        }
        return labels;
    }

    private static SidebarBottomBar noopBottomBar() {
        return new SidebarBottomBar(
                false,
                () -> {
                },
                () -> {
                },
                () -> {
                },
                () -> {
                },
                () -> {
                }
        );
    }
}
