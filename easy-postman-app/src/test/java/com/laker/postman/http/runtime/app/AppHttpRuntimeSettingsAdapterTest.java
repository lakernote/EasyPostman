package com.laker.postman.http.runtime.app;

import com.laker.postman.service.setting.SettingManager;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Properties;

import static org.testng.Assert.assertEquals;

public class AppHttpRuntimeSettingsAdapterTest {

    @Test
    public void shouldExposeBlankProxyPortAsIncompleteManualProxy() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            props.setProperty("proxy_port", "");

            assertEquals(new AppHttpRuntimeSettingsAdapter().getProxyPort(), 0);
        } finally {
            props.clear();
            props.putAll(backup);
        }
    }

    @Test
    public void shouldExposeInvalidProxyPortAsIncompleteManualProxy() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            props.setProperty("proxy_port", "not-a-port");

            assertEquals(new AppHttpRuntimeSettingsAdapter().getProxyPort(), 0);
        } finally {
            props.clear();
            props.putAll(backup);
        }
    }

    private static Properties getSettingsProperties() throws Exception {
        Field propsField = SettingManager.class.getDeclaredField("props");
        propsField.setAccessible(true);
        return (Properties) propsField.get(null);
    }
}
