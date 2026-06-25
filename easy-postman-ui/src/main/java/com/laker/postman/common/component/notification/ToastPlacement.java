package com.laker.postman.common.component.notification;

import com.laker.postman.common.component.ToolWindowStripeMetrics;
import com.laker.postman.model.NotificationPosition;
import lombok.experimental.UtilityClass;

import javax.swing.RootPaneContainer;
import java.awt.*;

@UtilityClass
class ToastPlacement {
    private static final int MARGIN = 24;
    private static final int TOOL_WINDOW_RESERVED_GAP = 8;
    private static final int RIGHT_RESERVED_WIDTH = ToolWindowStripeMetrics.STRIPE_THICKNESS + TOOL_WINDOW_RESERVED_GAP;
    private static final int BOTTOM_RESERVED_HEIGHT = ToolWindowStripeMetrics.STRIPE_THICKNESS + TOOL_WINDOW_RESERVED_GAP;
    private static final int SLIDE_DISTANCE = 16;
    private static final int MIN_ANCHOR_SIZE = 120;

    static Point calculate(Window parentWindow, Dimension windowSize, NotificationPosition position, int stackOffset) {
        Rectangle usableScreen = usableScreenBounds(graphicsConfiguration(parentWindow));
        Rectangle anchor = anchorBounds(parentWindow, usableScreen);
        return calculate(anchor, usableScreen, windowSize, position, stackOffset);
    }

    static Point calculate(Rectangle anchor, Rectangle usableScreen, Dimension windowSize,
                           NotificationPosition position, int stackOffset) {
        Rectangle safeAnchor = reserveAppToolWindowStripes(usableAnchor(anchor) ? anchor : usableScreen);
        int x;
        int y;
        switch (position) {
            case TOP_RIGHT:
                x = safeAnchor.x + safeAnchor.width - windowSize.width - MARGIN;
                y = safeAnchor.y + MARGIN + stackOffset;
                break;
            case TOP_CENTER:
                x = safeAnchor.x + (safeAnchor.width - windowSize.width) / 2;
                y = safeAnchor.y + MARGIN + stackOffset;
                break;
            case TOP_LEFT:
                x = safeAnchor.x + MARGIN;
                y = safeAnchor.y + MARGIN + stackOffset;
                break;
            case BOTTOM_CENTER:
                x = safeAnchor.x + (safeAnchor.width - windowSize.width) / 2;
                y = safeAnchor.y + safeAnchor.height - windowSize.height - MARGIN - stackOffset;
                break;
            case BOTTOM_LEFT:
                x = safeAnchor.x + MARGIN;
                y = safeAnchor.y + safeAnchor.height - windowSize.height - MARGIN - stackOffset;
                break;
            case CENTER:
                x = safeAnchor.x + (safeAnchor.width - windowSize.width) / 2;
                y = safeAnchor.y + (safeAnchor.height - windowSize.height) / 2 + stackOffset;
                break;
            case BOTTOM_RIGHT:
            default:
                x = safeAnchor.x + safeAnchor.width - windowSize.width - MARGIN;
                y = safeAnchor.y + safeAnchor.height - windowSize.height - MARGIN - stackOffset;
                break;
        }

        int minX = usableScreen.x + MARGIN;
        int minY = usableScreen.y + MARGIN;
        int maxX = usableScreen.x + usableScreen.width - windowSize.width - MARGIN;
        int maxY = usableScreen.y + usableScreen.height - windowSize.height - MARGIN;
        return new Point(clamp(x, minX, maxX), clamp(y, minY, maxY));
    }

    static Point slideStart(Point targetPosition, Dimension windowSize, NotificationPosition position) {
        Point point = new Point(targetPosition);
        switch (position) {
            case BOTTOM_RIGHT:
            case BOTTOM_CENTER:
            case BOTTOM_LEFT:
                point.y += windowSize.height + SLIDE_DISTANCE;
                break;
            default:
                point.y -= windowSize.height + SLIDE_DISTANCE;
                break;
        }
        return point;
    }

    private Rectangle anchorBounds(Window parentWindow, Rectangle fallbackBounds) {
        if (parentWindow == null) {
            return fallbackBounds;
        }
        Rectangle anchor = contentPaneScreenBounds(parentWindow);
        if (anchor == null) {
            anchor = parentWindow.getBounds();
        }
        return usableAnchor(anchor) ? anchor : fallbackBounds;
    }

    private Rectangle contentPaneScreenBounds(Window parentWindow) {
        if (!(parentWindow instanceof RootPaneContainer rootPaneContainer)) {
            return null;
        }
        Container contentPane = rootPaneContainer.getContentPane();
        if (contentPane == null || !contentPane.isShowing()) {
            return null;
        }
        try {
            Point location = contentPane.getLocationOnScreen();
            Dimension size = contentPane.getSize();
            return new Rectangle(location.x, location.y, size.width, size.height);
        } catch (IllegalComponentStateException ignored) {
            return null;
        }
    }

    private Rectangle reserveAppToolWindowStripes(Rectangle bounds) {
        int rightInset = insetWithin(bounds.width, RIGHT_RESERVED_WIDTH);
        int bottomInset = insetWithin(bounds.height, BOTTOM_RESERVED_HEIGHT);
        return new Rectangle(
                bounds.x,
                bounds.y,
                Math.max(1, bounds.width - rightInset),
                Math.max(1, bounds.height - bottomInset)
        );
    }

    private int insetWithin(int availableSize, int desiredInset) {
        return availableSize > MIN_ANCHOR_SIZE ? Math.min(desiredInset, availableSize - MIN_ANCHOR_SIZE) : 0;
    }

    private boolean usableAnchor(Rectangle anchor) {
        return anchor != null && anchor.width > 1 && anchor.height > 1;
    }

    private GraphicsConfiguration graphicsConfiguration(Window parentWindow) {
        GraphicsConfiguration graphicsConfiguration = parentWindow != null
                ? parentWindow.getGraphicsConfiguration()
                : defaultGraphicsConfiguration();
        return graphicsConfiguration != null ? graphicsConfiguration : defaultGraphicsConfiguration();
    }

    private GraphicsConfiguration defaultGraphicsConfiguration() {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice screenDevice = graphicsEnvironment.getDefaultScreenDevice();
        return screenDevice.getDefaultConfiguration();
    }

    private Rectangle usableScreenBounds(GraphicsConfiguration graphicsConfiguration) {
        if (graphicsConfiguration == null) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            return new Rectangle(0, 0, screenSize.width, screenSize.height);
        }
        Rectangle screen = graphicsConfiguration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);
        return new Rectangle(
                screen.x + insets.left,
                screen.y + insets.top,
                screen.width - insets.left - insets.right,
                screen.height - insets.top - insets.bottom
        );
    }

    private int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
