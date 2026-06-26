package com.laker.postman.panel.sidebar;

import com.laker.postman.common.component.ToolWindowStripeMetrics;
import com.laker.postman.plugin.api.StatusBarActionContribution;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

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

    @Test
    public void bottomBarShouldRenderPluginStatusBarActions() {
        StatusBarActionContribution contribution = new StatusBarActionContribution(
                "capture-shortcut",
                "抓包",
                "icons/global-variables.svg",
                StatusBarActionContribution.TARGET_TOOLBOX,
                "capture",
                200
        );
        AtomicReference<StatusBarActionContribution> invoked = new AtomicReference<>();
        SidebarBottomBar bottomBar = noopBottomBar(List.of(contribution), invoked::set);

        List<JLabel> labels = collectIconLabels(bottomBar.leftPanel(), bottomBar.rightPanel());
        JLabel actionLabel = labels.stream()
                .filter(label -> "抓包".equals(label.getToolTipText()))
                .findFirst()
                .orElseThrow();

        assertEquals(labels.size(), 6);
        assertEquals(actionLabel.getAccessibleContext().getAccessibleName(), "抓包");
        click(actionLabel);
        assertSame(invoked.get(), contribution);
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
        return noopBottomBar(List.of(), ignored -> {
        });
    }

    private static SidebarBottomBar noopBottomBar(List<StatusBarActionContribution> statusBarActions,
                                                  Consumer<StatusBarActionContribution> handler) {
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
                },
                statusBarActions,
                handler
        );
    }

    private static void click(JLabel label) {
        MouseEvent event = new MouseEvent(
                label,
                MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),
                0,
                1,
                1,
                1,
                false
        );
        for (var listener : label.getMouseListeners()) {
            listener.mousePressed(event);
        }
    }
}
