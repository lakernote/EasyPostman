package com.laker.postman.common.component.notification;

import com.laker.postman.common.constants.ThemeColors;
import com.laker.postman.common.component.ToolWindowStripeMetrics;
import com.laker.postman.model.NotificationPosition;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
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
    public void bottomRightPlacementShouldReserveToolWindowStripes() {
        Rectangle anchor = new Rectangle(100, 80, 1000, 700);
        Rectangle screen = new Rectangle(0, 0, 1400, 1000);
        Dimension toastSize = new Dimension(ToastStyle.MAX_WIDTH, 80);

        Point point = ToastPlacement.calculate(anchor, screen, toastSize, NotificationPosition.BOTTOM_RIGHT, 0);

        assertTrue(point.x <= anchor.x + anchor.width - ToolWindowStripeMetrics.STRIPE_THICKNESS - toastSize.width);
        assertTrue(point.y <= anchor.y + anchor.height - ToolWindowStripeMetrics.STRIPE_THICKNESS - toastSize.height);
    }
}
