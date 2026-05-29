package com.laker.postman.util;

import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;

public class CommonI18nTest {

    @Test
    public void allCommonMessageKeysShouldResolve() throws IllegalAccessException {
        for (Field field : CommonMessageKeys.class.getFields()) {
            if (field.getType() != String.class || !Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String key = (String) field.get(null);
            String value = CommonI18n.get(key);
            assertFalse(value.startsWith("!"), field.getName() + " is missing from common message bundles");
            assertNotEquals(value, key);
        }
    }

    @Test
    public void defaultI18nShouldFallbackToCommonBundle() {
        assertNotEquals(I18nUtil.getMessage(CommonMessageKeys.BUTTON_SAVE), CommonMessageKeys.BUTTON_SAVE);
        assertFalse(I18nUtil.getMessage(CommonMessageKeys.GENERAL_ERROR).startsWith("!"));
    }
}
