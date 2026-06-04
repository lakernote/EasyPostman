package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.app.AppHttpRuntimeBootstrap;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.okhttp.HttpClientRuntimeConfig;
import com.laker.postman.http.runtime.okhttp.OkHttpClientManager;
import com.laker.postman.service.setting.SettingManager;
import okhttp3.CookieJar;
import okhttp3.OkHttpClient;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertTrue;

public class ScopedHttpBaseClientProviderTest {

    @BeforeMethod
    public void configureRuntimeAdapters() {
        AppHttpRuntimeBootstrap.configure();
    }

    @Test
    public void shouldNotReuseRunScopedClientWhenProxySettingsChange() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        ScopedHttpBaseClientProvider provider = new ScopedHttpBaseClientProvider(
                () -> new HttpClientRuntimeConfig(7, 11, 13, 17)
        );

        try {
            props.clear();
            props.setProperty("proxy_enabled", "false");
            props.setProperty("ssl_verification_enabled", "true");
            props.setProperty("proxy_ssl_verification_disabled", "false");
            PreparedRequest request = preparedRequest("http://example.test/api");

            OkHttpClient directClient = provider.getBaseClient(request);
            assertEquals(directClient.proxy(), Proxy.NO_PROXY);

            props.setProperty("proxy_enabled", "true");
            props.setProperty("proxy_mode", SettingManager.PROXY_MODE_MANUAL);
            props.setProperty("proxy_type", SettingManager.PROXY_TYPE_HTTP);
            props.setProperty("proxy_host", "127.0.0.1");
            props.setProperty("proxy_port", "9090");
            props.setProperty("proxy_ssl_verification_disabled", "false");

            OkHttpClient proxiedClient = provider.getBaseClient(request);

            assertNotSame(proxiedClient, directClient);
            assertEquals(proxiedClient.proxy().type(), Proxy.Type.HTTP);
            InetSocketAddress proxyAddress = (InetSocketAddress) proxiedClient.proxy().address();
            assertEquals(proxyAddress.getHostString(), "127.0.0.1");
            assertEquals(proxyAddress.getPort(), 9090);
        } finally {
            provider.clear();
            props.clear();
            props.putAll(backup);
            OkHttpClientManager.clearClientCache();
        }
    }

    @Test
    public void shouldNotReuseRunScopedClientWhenProxyPasswordChanges() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        ScopedHttpBaseClientProvider provider = new ScopedHttpBaseClientProvider(
                () -> new HttpClientRuntimeConfig(7, 11, 13, 17)
        );

        try {
            props.clear();
            props.setProperty("proxy_enabled", "true");
            props.setProperty("proxy_mode", SettingManager.PROXY_MODE_MANUAL);
            props.setProperty("proxy_type", SettingManager.PROXY_TYPE_HTTP);
            props.setProperty("proxy_host", "127.0.0.1");
            props.setProperty("proxy_port", "9090");
            props.setProperty("proxy_username", "worker");
            props.setProperty("proxy_password", "first");
            props.setProperty("ssl_verification_enabled", "true");
            props.setProperty("proxy_ssl_verification_disabled", "false");
            PreparedRequest request = preparedRequest("http://example.test/api");

            OkHttpClient firstClient = provider.getBaseClient(request);

            props.setProperty("proxy_password", "second");
            OkHttpClient secondClient = provider.getBaseClient(request);

            assertNotSame(secondClient, firstClient);
        } finally {
            provider.clear();
            props.clear();
            props.putAll(backup);
            OkHttpClientManager.clearClientCache();
        }
    }

    @Test
    public void clearShouldShutdownScopedDispatcherExecutor() {
        ScopedHttpBaseClientProvider provider = new ScopedHttpBaseClientProvider(
                () -> new HttpClientRuntimeConfig(7, 11, 13, 17)
        );
        OkHttpClient client = provider.getBaseClient(preparedRequest("http://example.test/api"));
        ExecutorService dispatcherExecutor = client.dispatcher().executorService();

        provider.clear();

        assertTrue(dispatcherExecutor.isShutdown());
    }

    @Test
    public void nullCustomCookieJarShouldUseNoCookiesInsteadOfScopedStore() {
        ScopedHttpBaseClientProvider provider = new ScopedHttpBaseClientProvider(
                () -> new HttpClientRuntimeConfig(7, 11, 13, 17),
                (CookieJar) null
        );

        OkHttpClient client = provider.getBaseClient(preparedRequest("http://example.test/api"));

        assertEquals(client.cookieJar(), CookieJar.NO_COOKIES);
    }

    private static PreparedRequest preparedRequest(String url) {
        PreparedRequest request = new PreparedRequest();
        request.url = url;
        request.followRedirects = true;
        request.sslVerificationEnabled = true;
        return request;
    }

    private static Properties getSettingsProperties() throws Exception {
        Field propsField = SettingManager.class.getDeclaredField("props");
        propsField.setAccessible(true);
        return (Properties) propsField.get(null);
    }
}
