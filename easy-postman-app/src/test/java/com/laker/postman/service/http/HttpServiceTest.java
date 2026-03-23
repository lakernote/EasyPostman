package com.laker.postman.service.http;

import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.setting.SettingManager;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Properties;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class HttpServiceTest {

    @Test
    public void shouldIsolateConnectionPoolWhenRequestSslModeDiffersFromGlobal() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            props.setProperty("proxy_enabled", "false");
            props.setProperty("ssl_verification_enabled", "true");
            props.setProperty("proxy_ssl_verification_disabled", "false");

            PreparedRequest request = new PreparedRequest();
            request.url = "https://api.example.com/data";
            request.sslVerificationEnabled = false;

            assertTrue(HttpService.shouldIsolateConnectionPool(request));
        } finally {
            props.clear();
            props.putAll(backup);
        }
    }

    @Test
    public void shouldNotIsolateConnectionPoolWhenRequestSslModeMatchesGlobal() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            props.setProperty("proxy_enabled", "false");
            props.setProperty("ssl_verification_enabled", "true");
            props.setProperty("proxy_ssl_verification_disabled", "false");

            PreparedRequest request = new PreparedRequest();
            request.url = "https://api.example.com/data";
            request.sslVerificationEnabled = true;

            assertFalse(HttpService.shouldIsolateConnectionPool(request));
        } finally {
            props.clear();
            props.putAll(backup);
        }
    }

    @Test
    public void shouldUseSecureDefaultPortForWss() {
        assertTrue(HttpService.resolveSecurePort("wss", -1) == 443);
        assertTrue(HttpService.resolveSecurePort("https", -1) == 443);
    }

    private static Properties getSettingsProperties() throws Exception {
        Field propsField = SettingManager.class.getDeclaredField("props");
        propsField.setAccessible(true);
        return (Properties) propsField.get(null);
    }
}
