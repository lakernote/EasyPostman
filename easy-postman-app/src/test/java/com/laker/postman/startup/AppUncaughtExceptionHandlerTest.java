package com.laker.postman.startup;

import org.testng.annotations.Test;
import org.testng.SkipException;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AppUncaughtExceptionHandlerTest {
    @Test
    public void shouldNotShowSwingDialogWhenHeadless() throws Throwable {
        if (!GraphicsEnvironment.isHeadless()) {
            throw new SkipException("Only verifies the headless branch; desktop runs would show a real dialog");
        }

        AppUncaughtExceptionHandler handler = new AppUncaughtExceptionHandler();
        Method notifyUser = AppUncaughtExceptionHandler.class.getDeclaredMethod("notifyUser");
        notifyUser.setAccessible(true);

        try {
            notifyUser.invoke(handler);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
