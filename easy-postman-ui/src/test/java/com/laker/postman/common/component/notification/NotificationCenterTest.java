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
    public void bottomRightPlacementShouldReserveToolWindowStripes() {
        Rectangle anchor = new Rectangle(100, 80, 1000, 700);
        Rectangle screen = new Rectangle(0, 0, 1400, 1000);
        Dimension toastSize = new Dimension(ToastStyle.MAX_WIDTH, 80);

        Point point = ToastPlacement.calculate(anchor, screen, toastSize, NotificationPosition.BOTTOM_RIGHT, 0);

        assertTrue(point.x <= anchor.x + anchor.width - ToolWindowStripeMetrics.STRIPE_THICKNESS - toastSize.width);
        assertTrue(point.y <= anchor.y + anchor.height - ToolWindowStripeMetrics.STRIPE_THICKNESS - toastSize.height);
    }
}
