package com.laker.postman.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class I18nUtilPluginBundleTest {

    @Test
    public void shouldLoadPluginSpecificBundleWithoutHostMessagesBundle() {
        String message = I18nUtil.getMessage(
                "plugin-test-messages",
                I18nUtilPluginBundleTest.class.getClassLoader(),
                "hello",
                "Codex"
        );

        assertEquals(message, "Hello Codex");
    }
}
