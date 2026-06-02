package com.laker.postman.startup;

import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AppUncaughtExceptionHandlerTest {

    @Test
    public void shouldNotShowSwingDialogWhenHeadless() throws Throwable {
        String previousHeadless = System.getProperty("java.awt.headless");
        System.setProperty("java.awt.headless", "true");
        try {
            AppUncaughtExceptionHandler handler = new AppUncaughtExceptionHandler();
            Method notifyUser = AppUncaughtExceptionHandler.class.getDeclaredMethod("notifyUser");
            notifyUser.setAccessible(true);

            try {
                notifyUser.invoke(handler);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        } finally {
            if (previousHeadless == null) {
                System.clearProperty("java.awt.headless");
            } else {
                System.setProperty("java.awt.headless", previousHeadless);
            }
        }
    }
}
