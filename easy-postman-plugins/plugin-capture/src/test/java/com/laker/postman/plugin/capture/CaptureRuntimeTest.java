package com.laker.postman.plugin.capture;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.assertNull;

public class CaptureRuntimeTest {

    @AfterMethod(alwaysRun = true)
    public void resetRuntime() throws Exception {
        setProxyService(null);
    }

    @Test
    public void shouldNotInitializeProxyServiceWhenStoppingUnusedRuntime() throws Exception {
        setProxyService(null);

        CaptureRuntime.stopQuietly();

        assertNull(readProxyService());
    }

    private static CaptureProxyService readProxyService() throws Exception {
        Field field = CaptureRuntime.class.getDeclaredField("proxyService");
        field.setAccessible(true);
        return (CaptureProxyService) field.get(null);
    }

    private static void setProxyService(CaptureProxyService value) throws Exception {
        Field field = CaptureRuntime.class.getDeclaredField("proxyService");
        field.setAccessible(true);
        field.set(null, value);
    }
}
