package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.common.component.ToolWindowStripeMetrics;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RequestSideAssistantPanelLayoutTest extends AbstractSwingUiTest {

    @Test
    public void collapsedAssistantToolbarShouldUseCenteredSidebarActionCells() {
        RequestSideAssistantPanel panel = new RequestSideAssistantPanel(() -> null);
        Dimension actionSize = ToolWindowStripeMetrics.actionSize();

        assertEquals(panel.getPreferredSize().width, ToolWindowStripeMetrics.STRIPE_THICKNESS);
        assertEquals(panel.getMinimumSize().width, ToolWindowStripeMetrics.STRIPE_THICKNESS);

        JPanel toolWindowArea = (JPanel) panel.getComponent(0);
        BorderLayout layout = (BorderLayout) toolWindowArea.getLayout();
        Component toolbar = layout.getLayoutComponent(BorderLayout.EAST);

        assertEquals(toolbar.getPreferredSize().width, ToolWindowStripeMetrics.STRIPE_THICKNESS);
        assertEquals(toolbar.getMinimumSize().width, ToolWindowStripeMetrics.STRIPE_THICKNESS);
        Insets toolbarInsets = ((JPanel) toolbar).getBorder().getBorderInsets(toolbar);
        assertEquals(toolbarInsets.top, 4);
        assertEquals(toolbarInsets.bottom, 4);

        List<JLabel> labels = collectIconLabels((JPanel) toolbar);
        assertEquals(labels.size(), 2);
        for (JLabel label : labels) {
            assertEquals(label.getPreferredSize(), actionSize);
            assertEquals(label.getMinimumSize(), actionSize);
            assertEquals(label.getMaximumSize(), actionSize);
            assertEquals(label.getHorizontalAlignment(), SwingConstants.CENTER);
            assertEquals(label.getVerticalAlignment(), SwingConstants.CENTER);
            Insets insets = label.getBorder().getBorderInsets(label);
            assertEquals(insets.left, 0);
            assertEquals(insets.right, 8);
        }
    }

    @Test
    public void selectedAssistantToolBackgroundShouldFollowShiftedIconCenter() {
        RequestSideAssistantPanel panel = new RequestSideAssistantPanel(() -> null);
        JLabel label = collectIconLabels(findToolbar(panel)).get(0);
        Dimension actionSize = ToolWindowStripeMetrics.actionSize();
        label.setSize(actionSize);

        MouseEvent mousePressed = new MouseEvent(
                label,
                MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),
                0,
                actionSize.width / 2,
                actionSize.height / 2,
                1,
                false,
                MouseEvent.BUTTON1
        );
        for (var listener : label.getMouseListeners()) {
            listener.mousePressed(mousePressed);
        }

        Rectangle selectedBackground = paintPrimaryBackgroundBounds(label, actionSize);

        assertTrue(selectedBackground.x <= 1);
        assertTrue(selectedBackground.width <= 27);
        assertTrue(selectedBackground.getCenterX() < actionSize.width / 2.0);
    }

    private static List<JLabel> collectIconLabels(JPanel panel) {
        List<JLabel> labels = new ArrayList<>();
        for (Component component : panel.getComponents()) {
            if (component instanceof JLabel label && label.getIcon() != null) {
                labels.add(label);
            }
        }
        assertTrue(labels.size() <= 2);
        return labels;
    }

    private static JPanel findToolbar(RequestSideAssistantPanel panel) {
        JPanel toolWindowArea = (JPanel) panel.getComponent(0);
        BorderLayout layout = (BorderLayout) toolWindowArea.getLayout();
        return (JPanel) layout.getLayoutComponent(BorderLayout.EAST);
    }

    private static Rectangle paintPrimaryBackgroundBounds(JLabel label, Dimension size) {
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            label.paint(graphics);
        } finally {
            graphics.dispose();
        }

        Color primary = ModernColors.getPrimary();
        Rectangle bounds = null;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color pixel = new Color(image.getRGB(x, y), true);
                if (isPrimary(pixel, primary)) {
                    if (bounds == null) {
                        bounds = new Rectangle(x, y, 1, 1);
                    } else {
                        bounds.add(x, y);
                    }
                }
            }
        }
        assertTrue(bounds != null);
        return bounds;
    }

    private static boolean isPrimary(Color pixel, Color primary) {
        return pixel.getAlpha() > 200
                && Math.abs(pixel.getRed() - primary.getRed()) <= 2
                && Math.abs(pixel.getGreen() - primary.getGreen()) <= 2
                && Math.abs(pixel.getBlue() - primary.getBlue()) <= 2;
    }
}
