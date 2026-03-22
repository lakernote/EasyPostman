package com.laker.postman.service.http.okhttp;

import cn.hutool.json.JSONUtil;
import com.laker.postman.model.TrustedCertificateEntry;
import com.laker.postman.service.setting.SettingManager;
import okhttp3.OkHttpClient;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

public class OkHttpClientManagerTest {

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

            String firstKey = getProxyConfigKey();

            Thread.sleep(1100L);
            Files.writeString(trustFile, "second-version", StandardCharsets.US_ASCII);

            String secondKey = getProxyConfigKey();
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

    private static Properties getSettingsProperties() throws Exception {
        Field propsField = SettingManager.class.getDeclaredField("props");
        propsField.setAccessible(true);
        return (Properties) propsField.get(null);
    }

    private static String getProxyConfigKey() throws Exception {
        Method method = OkHttpClientManager.class.getDeclaredMethod("getProxyConfigKey");
        method.setAccessible(true);
        return (String) method.invoke(null);
    }
}
