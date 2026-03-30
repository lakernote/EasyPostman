package com.laker.postman.service.http.okhttp;

import cn.hutool.json.JSONUtil;
import com.laker.postman.model.TrustedCertificateEntry;
import com.laker.postman.service.setting.SettingManager;
import okhttp3.OkHttpClient;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class OkHttpClientManagerTest {

    @Test
    public void defaultProxySettingsShouldDisableProxyWhenUnset() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();

            assertFalse(SettingManager.isProxyEnabled());
            assertEquals(SettingManager.getProxyMode(), SettingManager.PROXY_MODE_MANUAL);
            assertTrue(SettingManager.isManualProxyMode());
            assertEquals(SettingManager.getProxyPortText(), "");
        } finally {
            props.clear();
            props.putAll(backup);
        }
    }

    @Test
    public void customTrustCacheKeyShouldChangeWhenTrustMaterialFileChanges() throws Exception {
        Path trustFile = Files.createTempFile("easy-postman-trust-material", ".pem");
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            Files.writeString(trustFile, "first", StandardCharsets.US_ASCII);

            props.setProperty("proxy_enabled", "false");
            props.setProperty("ssl_verification_enabled", "true");
            props.setProperty("proxy_ssl_verification_disabled", "false");
            props.setProperty("custom_trust_material_enabled", "true");
            TrustedCertificateEntry entry = new TrustedCertificateEntry();
            entry.setPath(trustFile.toString());
            entry.setPassword("");
            props.setProperty("custom_trust_material_entries", JSONUtil.toJsonStr(java.util.List.of(entry)));
            props.setProperty("custom_trust_material_path", trustFile.toString());
            props.setProperty("custom_trust_material_password", "");

            String firstKey = getProxyConfigKey("https://localhost:8443");

            Thread.sleep(1100L);
            Files.writeString(trustFile, "second-version", StandardCharsets.US_ASCII);

            String secondKey = getProxyConfigKey("https://localhost:8443");
            assertNotEquals(secondKey, firstKey);
        } finally {
            props.clear();
            props.putAll(backup);
            Files.deleteIfExists(trustFile);
            OkHttpClientManager.clearClientCache();
        }
    }

    @Test
    public void httpClientShouldIgnoreBrokenCustomTrustMaterial() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            props.setProperty("proxy_enabled", "false");
            props.setProperty("ssl_verification_enabled", "true");
            props.setProperty("proxy_ssl_verification_disabled", "false");
            props.setProperty("custom_trust_material_enabled", "true");
            TrustedCertificateEntry entry = new TrustedCertificateEntry();
            entry.setPath("/tmp/does-not-exist.pem");
            props.setProperty("custom_trust_material_entries", JSONUtil.toJsonStr(java.util.List.of(entry)));

            OkHttpClient client = OkHttpClientManager.getClient("https://localhost:8443", true);

            assertNotNull(client);
        } finally {
            props.clear();
            props.putAll(backup);
            OkHttpClientManager.clearClientCache();
        }
    }

    @Test
    public void proxySslBypassShouldNotAffectDirectHttpsRequests() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            props.setProperty("proxy_enabled", "false");
            props.setProperty("ssl_verification_enabled", "true");
            props.setProperty("proxy_ssl_verification_disabled", "false");

            OkHttpClient baseline = OkHttpClientManager.getClient("https://localhost:8443", true);
            String baselineHostnameVerifier = baseline.hostnameVerifier().getClass().getName();
            String baselineSocketFactory = baseline.sslSocketFactory().getClass().getName();

            OkHttpClientManager.clearClientCache();
            props.setProperty("proxy_ssl_verification_disabled", "true");

            OkHttpClient directClient = OkHttpClientManager.getClient("https://localhost:8443", true);

            assertEquals(directClient.hostnameVerifier().getClass().getName(), baselineHostnameVerifier);
            assertEquals(directClient.sslSocketFactory().getClass().getName(), baselineSocketFactory);
        } finally {
            props.clear();
            props.putAll(backup);
            OkHttpClientManager.clearClientCache();
        }
    }

    @Test
    public void disabledProxyShouldForceDirectConnectionsEvenWhenSystemProxyExists() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        ProxySelector originalSelector = ProxySelector.getDefault();

        try {
            props.clear();
            props.setProperty("proxy_enabled", "false");
            props.setProperty("ssl_verification_enabled", "true");
            props.setProperty("proxy_ssl_verification_disabled", "false");

            ProxySelector.setDefault(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return List.of(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("127.0.0.1", 8888)));
                }

                @Override
                public void connectFailed(URI uri, java.net.SocketAddress sa, java.io.IOException ioe) {
                }
            });

            OkHttpClient client = OkHttpClientManager.getClient("https://example.com", true);

            assertEquals(client.proxy(), Proxy.NO_PROXY);
        } finally {
            ProxySelector.setDefault(originalSelector);
            props.clear();
            props.putAll(backup);
            OkHttpClientManager.clearClientCache();
        }
    }

    @Test
    public void systemProxyModeShouldResolveProxyFromDefaultSelector() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        ProxySelector originalSelector = ProxySelector.getDefault();

        try {
            props.clear();
            props.setProperty("proxy_enabled", "true");
            props.setProperty("proxy_mode", SettingManager.PROXY_MODE_SYSTEM);
            props.setProperty("ssl_verification_enabled", "true");
            props.setProperty("proxy_ssl_verification_disabled", "false");

            Proxy expectedProxy = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("127.0.0.1", 9090));
            ProxySelector.setDefault(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    if ("example.com".equalsIgnoreCase(uri.getHost())) {
                        return List.of(expectedProxy);
                    }
                    return List.of(Proxy.NO_PROXY);
                }

                @Override
                public void connectFailed(URI uri, java.net.SocketAddress sa, java.io.IOException ioe) {
                }
            });

            Proxy selectedProxy = OkHttpClientManager.selectSystemProxy("https://example.com");
            assertEquals(selectedProxy.type(), Proxy.Type.HTTP);
            InetSocketAddress address = (InetSocketAddress) selectedProxy.address();
            assertEquals(address.getHostString(), "127.0.0.1");
            assertEquals(address.getPort(), 9090);

            String proxyKey = getProxyConfigKey("https://example.com");
            assertTrue(proxyKey.contains("proxy:system:HTTP:127.0.0.1:9090"));

            OkHttpClient client = OkHttpClientManager.getClient("https://example.com", true);
            List<Proxy> proxies = client.proxySelector().select(URI.create("https://example.com"));
            assertEquals(proxies.size(), 1);
            assertEquals(proxies.get(0), expectedProxy);
        } finally {
            ProxySelector.setDefault(originalSelector);
            props.clear();
            props.putAll(backup);
            OkHttpClientManager.clearClientCache();
        }
    }

    @Test
    public void systemProxyCacheKeyShouldChangeWhenSelectedProxyChanges() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        ProxySelector originalSelector = ProxySelector.getDefault();

        try {
            props.clear();
            props.setProperty("proxy_enabled", "true");
            props.setProperty("proxy_mode", SettingManager.PROXY_MODE_SYSTEM);
            props.setProperty("ssl_verification_enabled", "true");
            props.setProperty("proxy_ssl_verification_disabled", "false");

            ProxySelector.setDefault(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return List.of(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("127.0.0.1", 8080)));
                }

                @Override
                public void connectFailed(URI uri, java.net.SocketAddress sa, java.io.IOException ioe) {
                }
            });
            String firstKey = getProxyConfigKey("https://example.com");

            ProxySelector.setDefault(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return List.of(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("127.0.0.1", 9090)));
                }

                @Override
                public void connectFailed(URI uri, java.net.SocketAddress sa, java.io.IOException ioe) {
                }
            });
            String secondKey = getProxyConfigKey("https://example.com");

            assertNotEquals(secondKey, firstKey);
        } finally {
            ProxySelector.setDefault(originalSelector);
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

    private static String getProxyConfigKey(String baseUri) throws Exception {
        Method method = OkHttpClientManager.class.getDeclaredMethod("getProxyConfigKey", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, baseUri);
    }
}
