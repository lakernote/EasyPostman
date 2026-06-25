package com.laker.postman.common.component.notification;

import com.laker.postman.common.constants.ThemeColors;
import com.laker.postman.common.component.ToolWindowStripeMetrics;
import com.laker.postman.model.NotificationPosition;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class NotificationCenterTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.SUCCESS, ThemeColors.NOTIFICATION_BACKGROUND);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void notificationTypeColorShouldFollowThemeTokens() {
        Color success = new Color(9, 8, 7);
        UIManager.put(ThemeColors.SUCCESS, success);

        assertEquals(NotificationCenter.NotificationType.SUCCESS.getColor(), success);
    }

    @Test
    public void toastHeaderTitleShouldUseNotificationTypeColor() throws Exception {
        Color success = new Color(19, 88, 21);
        UIManager.put(ThemeColors.SUCCESS, success);

        assertEquals(NotificationCenter.toastTitleColor(NotificationCenter.NotificationType.SUCCESS), success);
    }

    @Test
    public void shortToastWidthShouldUseCompactMinimumInsteadOfMaxWidth() {
        Font titleFont = new Font(Font.DIALOG, Font.BOLD, 13);
        Font bodyFont = new Font(Font.DIALOG, Font.PLAIN, 13);

        int width = ToastStyle.preferredWidth("成功", "设置保存成功", titleFont, bodyFont);

        assertEquals(width, ToastStyle.MIN_WIDTH);
        assertTrue(width < ToastStyle.MAX_WIDTH);
    }

    @Test
    public void longToastWidthShouldCapAtMaxWidth() {
        Font titleFont = new Font(Font.DIALOG, Font.BOLD, 13);
        Font bodyFont = new Font(Font.DIALOG, Font.PLAIN, 13);

        int width = ToastStyle.preferredWidth(
                "错误",
                "Request execution failed because the upstream service returned an invalid gateway response.",
                titleFont,
                bodyFont
        );

        assertEquals(width, ToastStyle.MAX_WIDTH);
    }

    @Test
    public void collapsedToastBodyShouldNotEmbedExpandActionText() {
        String message = String.join("\n", "line 1", "line 2", "line 3", "line 4", "line 5");

        String displayText = ToastTextFormatter.displayText(message, false);

        assertEquals(displayText, "line 1\nline 2\nline 3\nline 4\u2026");
    }

    @Test
    public void collapsedSingleLineToastBodyShouldRespectLengthLimit() {
        String message = "HTTP 403 Forbidden: " + "x".repeat(ToastStyle.COLLAPSED_MAX_LENGTH + 40);

        String displayText = ToastTextFormatter.displayText(message, false);

        assertTrue(displayText.endsWith("\u2026"));
        assertTrue(displayText.length() <= ToastStyle.COLLAPSED_MAX_LENGTH + 1);
    }

    @Test
    public void toastSurfaceShouldReadNotificationBackgroundToken() {
        Color background = new Color(250, 251, 252);
        UIManager.put(ThemeColors.NOTIFICATION_BACKGROUND, background);

        assertEquals(ToastStyle.surfaceColor(), background);
    }

    @Test
    public void toastCardShouldNotPaintAStatusStripe() {
        Color background = new Color(250, 251, 252);
        UIManager.put(ThemeColors.NOTIFICATION_BACKGROUND, background);
        UIManager.put(ThemeColors.SUCCESS, new Color(22, 178, 96));
        JPanel card = ToastStyle.createCardPanel(NotificationCenter.NotificationType.SUCCESS);
        card.setSize(80, 40);

        BufferedImage image = new BufferedImage(80, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        card.paint(graphics);
        graphics.dispose();

        assertEquals(new Color(image.getRGB(2, 20), true), background);
    }

    @Test
    public void toastBorderShouldRemainVisibleOnLightTheme() {
        assertTrue(ToastStyle.borderColor().getAlpha() >= 120);
    }

    @Test
    public void toastCloseButtonShouldBeDiscoverableBeforeHover() {
        JButton closeButton = ToastStyle.createCloseButton(() -> {
        });

        assertTrue(iconHasVisiblePixels(closeButton.getIcon()));
    }

    @Test
    public void toastActionButtonShouldUseButtonSemantics() {
        boolean[] clicked = {false};
        JButton actionButton = ToastStyle.createLinkButton("Show details", () -> clicked[0] = true);

        actionButton.doClick();

        assertTrue(clicked[0]);
        assertEquals(actionButton.getCursor().getType(), Cursor.HAND_CURSOR);
        assertTrue(!actionButton.isContentAreaFilled());
    }

    @Test
    public void toastHoverTargetsShouldIncludeBodyTextArea() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/common/component/notification/ToastWindow.java"));

        assertTrue(source.contains("bodyLabel, bodyText"),
                "Toast body text area should pause auto-close on hover after wrapping it in a scroll pane");
    }

    @Test
    public void bottomRightPlacementShouldReserveToolWindowStripes() {
        Rectangle anchor = new Rectangle(100, 80, 1000, 700);
        Rectangle screen = new Rectangle(0, 0, 1400, 1000);
        Dimension toastSize = new Dimension(ToastStyle.MAX_WIDTH, 80);

        Point point = ToastPlacement.calculate(anchor, screen, toastSize, NotificationPosition.BOTTOM_RIGHT, 0);

        assertTrue(point.x <= anchor.x + anchor.width - ToolWindowStripeMetrics.STRIPE_THICKNESS - toastSize.width);
        assertTrue(point.y <= anchor.y + anchor.height - ToolWindowStripeMetrics.STRIPE_THICKNESS - toastSize.height);
    }

    private static boolean iconHasVisiblePixels(Icon icon) {
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        icon.paintIcon(null, graphics, 0, 0);
        graphics.dispose();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xff) > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
