package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;

public class SystemProxyServiceTest {

    @Test
    public void shouldSkipWindowsRestoreWhenNoSnapshotWasCaptured() throws Exception {
        SystemProxyService service = new SystemProxyService();
        Method restoreWindowsSnapshot = SystemProxyService.class.getDeclaredMethod("restoreWindowsSnapshot");
        restoreWindowsSnapshot.setAccessible(true);

        restoreWindowsSnapshot.invoke(service);

        assertFalse(readBooleanField(service, "active"));
        assertNull(readObjectField(service, "windowsSnapshot"));
    }

    private static boolean readBooleanField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(target);
    }

    private static Object readObjectField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
