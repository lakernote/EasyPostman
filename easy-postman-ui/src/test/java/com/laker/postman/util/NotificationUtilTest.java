package com.laker.postman.util;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;

public class NotificationUtilTest {
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

        assertEquals(NotificationUtil.NotificationType.SUCCESS.getColor(), success);
    }

    @Test
    public void toastHeaderTitleShouldUseNotificationTypeColor() throws Exception {
        Color success = new Color(19, 88, 21);
        UIManager.put(ThemeColors.SUCCESS, success);

        assertEquals(NotificationUtil.toastTitleColor(NotificationUtil.NotificationType.SUCCESS), success);
    }
}
