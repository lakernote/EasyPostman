package com.laker.postman.service.http;

import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.service.setting.SettingManager;
import okhttp3.OkHttpClient;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertEquals;
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

    @Test
    public void shouldReuseBaseSslConfigurationWhenRequestSslModeMatchesGlobal() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            props.setProperty("proxy_enabled", "false");
            props.setProperty("ssl_verification_enabled", "true");
            props.setProperty("proxy_ssl_verification_disabled", "false");
            OkHttpClientManager.clearClientCache();

            PreparedRequest request = new PreparedRequest();
            request.url = "https://api.example.com/data";
            request.followRedirects = true;
            request.sslVerificationEnabled = true;

            OkHttpClient baseClient = OkHttpClientManager.getClient("https://api.example.com", true);
            OkHttpClient customClient = invokeBuildCustomClient(request);

            assertSame(customClient.sslSocketFactory(), baseClient.sslSocketFactory());
            assertSame(customClient.hostnameVerifier(), baseClient.hostnameVerifier());
        } finally {
            props.clear();
            props.putAll(backup);
            OkHttpClientManager.clearClientCache();
        }
    }

    @Test
    public void systemProxyBypassShouldNotForceSslIsolation() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        ProxySelector originalSelector = ProxySelector.getDefault();

        try {
            props.clear();
            props.setProperty("proxy_enabled", "true");
            props.setProperty("proxy_mode", SettingManager.PROXY_MODE_SYSTEM);
            props.setProperty("ssl_verification_enabled", "true");
            props.setProperty("proxy_ssl_verification_disabled", "true");

            ProxySelector.setDefault(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return List.of(Proxy.NO_PROXY);
                }

                @Override
                public void connectFailed(URI uri, java.net.SocketAddress sa, java.io.IOException ioe) {
                }
            });

            PreparedRequest request = new PreparedRequest();
            request.url = "https://bypass.example.com/data";
            request.sslVerificationEnabled = true;

            assertFalse(HttpService.shouldIsolateConnectionPool(request));
        } finally {
            ProxySelector.setDefault(originalSelector);
            props.clear();
            props.putAll(backup);
            OkHttpClientManager.clearClientCache();
        }
    }

    @Test
    public void strictRequestShouldNotReuseLenientSslComponents() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            props.setProperty("proxy_enabled", "false");
            props.setProperty("ssl_verification_enabled", "false");
            props.setProperty("proxy_ssl_verification_disabled", "false");
            OkHttpClientManager.clearClientCache();

            PreparedRequest request = new PreparedRequest();
            request.url = "https://api.example.com/data";
            request.followRedirects = true;
            request.sslVerificationEnabled = true;

            OkHttpClient lenientBaseClient = OkHttpClientManager.getClient("https://api.example.com", true);
            OkHttpClient strictClient = invokeBuildCustomClient(request);
            OkHttpClient strictDefaultClient = new OkHttpClient();

            assertEquals(
                    strictClient.hostnameVerifier().getClass().getName(),
                    strictDefaultClient.hostnameVerifier().getClass().getName()
            );
            assertEquals(
                    strictClient.sslSocketFactory().getClass().getName(),
                    strictDefaultClient.sslSocketFactory().getClass().getName()
            );
            assertFalse(
                    strictClient.hostnameVerifier().getClass().getName()
                            .equals(lenientBaseClient.hostnameVerifier().getClass().getName())
            );
            assertFalse(
                    strictClient.sslSocketFactory().getClass().getName()
                            .equals(lenientBaseClient.sslSocketFactory().getClass().getName())
            );
        } finally {
            props.clear();
            props.putAll(backup);
            OkHttpClientManager.clearClientCache();
        }
    }

    private static Properties getSettingsProperties() throws Exception {
        Field propsField = SettingManager.class.getDeclaredField("props");
        propsField.setAccessible(true);
        return (Properties) propsField.get(null);
    }

    private static OkHttpClient invokeBuildCustomClient(PreparedRequest request) throws Exception {
        Method method = HttpService.class.getDeclaredMethod("buildCustomClient", PreparedRequest.class);
        method.setAccessible(true);
        return (OkHttpClient) method.invoke(null, request);
    }
}
