package com.laker.postman.util;

import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;

public class UiI18nTest {

    @Test
    public void shouldLoadUiOwnedMessages() {
        String tableInfo = UiI18n.get(UiMessageKeys.TABLE_PAGE_INFO, "1", "20", "1", "3");
        assertFalse(tableInfo.startsWith("!"));
        assertFalse(tableInfo.contains("{0}"));

        String notificationAction = UiI18n.get(UiMessageKeys.NOTIFICATION_EXPAND);
        assertFalse(notificationAction.startsWith("!"));
        assertNotEquals(notificationAction, UiMessageKeys.NOTIFICATION_EXPAND);
    }

    @Test
    public void allUiMessageKeysShouldResolve() throws IllegalAccessException {
        for (Field field : UiMessageKeys.class.getFields()) {
            if (field.getType() != String.class || !Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String key = (String) field.get(null);
            assertFalse(UiI18n.get(key).startsWith("!"), field.getName() + " is missing from ui message bundles");
        }
    }
}
