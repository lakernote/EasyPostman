package com.laker.postman.service.setting;

import cn.hutool.json.JSONUtil;
import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.model.TrustedCertificateEntry;
import com.laker.postman.model.PreparedRequest;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SettingManagerTrustedCertificateTest {

    @Test
    public void shouldReadLegacySingleTrustMaterialAsListEntry() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            props.setProperty("custom_trust_material_path", "/tmp/legacy.pem");
            props.setProperty("custom_trust_material_password", "legacy-pass");

            List<TrustedCertificateEntry> entries = SettingManager.getCustomTrustMaterialEntries();

            assertEquals(entries.size(), 1);
            assertEquals(entries.get(0).getPath(), "/tmp/legacy.pem");
            assertEquals(entries.get(0).getPassword(), "legacy-pass");
            assertTrue(entries.get(0).isEnabled());
        } finally {
            props.clear();
            props.putAll(backup);
        }
    }

    @Test
    public void shouldPersistTrustedCertificateEntriesAsJsonList() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            TrustedCertificateEntry first = new TrustedCertificateEntry();
            first.setPath("/tmp/a.pem");
            TrustedCertificateEntry second = new TrustedCertificateEntry();
            second.setPath("/tmp/b.p12");
            second.setPassword("changeit");

            props.setProperty("custom_trust_material_entries", JSONUtil.toJsonStr(List.of(first, second)));
            props.setProperty("custom_trust_material_path", "/tmp/a.pem");
            props.setProperty("custom_trust_material_password", "");

            String json = props.getProperty("custom_trust_material_entries");
            assertTrue(JSONUtil.parseArray(json).size() == 2);
            assertEquals(SettingManager.getCustomTrustMaterialEntries().size(), 2);
            assertEquals(SettingManager.getCustomTrustMaterialPath(), "/tmp/a.pem");
        } finally {
            props.clear();
            props.putAll(backup);
        }
    }

    @Test
    public void shouldDisableRequestAndProxySslVerificationByDefaultWhenUnset() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();

            assertTrue(SettingManager.isRequestSslVerificationDisabled());
            assertTrue(SettingManager.isProxySslVerificationDisabled());
        } finally {
            props.clear();
            props.putAll(backup);
        }
    }

    @Test
    public void preparedRequestShouldDefaultToLenientSslVerification() {
        PreparedRequest request = new PreparedRequest();

        assertFalse(request.sslVerificationEnabled);
    }

    @Test
    public void shouldNormalizeSlowRequestThresholdToNonNegative() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        Path configPath = Path.of(ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        boolean configExisted = Files.exists(configPath);
        String originalConfig = configExisted ? Files.readString(configPath) : null;

        try {
            props.clear();
            props.setProperty("jmeter_slow_request_threshold", "-20");
            assertEquals(SettingManager.getJmeterSlowRequestThreshold(), 0);

            SettingManager.setJmeterSlowRequestThreshold(-50);
            assertEquals(props.getProperty("jmeter_slow_request_threshold"), "0");
        } finally {
            props.clear();
            props.putAll(backup);
            restoreConfig(configPath, configExisted, originalConfig);
        }
    }

    @Test
    public void shouldPreserveUpdateSourceWhenPersistingLastCheckTimeFromStaleSettings() throws Exception {
        Path tempFile = Files.createTempFile("easy-postman-settings", ".properties");
        Properties diskSettings = new Properties();
        diskSettings.setProperty("update_source_preference", "github");
        try (var output = Files.newOutputStream(tempFile)) {
            diskSettings.store(output, "test");
        }

        SettingManager.saveProperty(tempFile.toFile(), "last_update_check_time", "12345");

        Properties reloaded = new Properties();
        try (FileInputStream input = new FileInputStream(tempFile.toFile())) {
            reloaded.load(input);
        }

        assertEquals(reloaded.getProperty("update_source_preference"), "github");
        assertEquals(reloaded.getProperty("last_update_check_time"), "12345");
    }

    @Test
    public void shouldNotDiscardPendingInMemorySettingWhenSinglePropertySaveRefreshesSettings() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        Path configPath = Path.of(ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        boolean configExisted = Files.exists(configPath);
        String originalConfig = configExisted ? Files.readString(configPath) : null;

        try {
            Files.createDirectories(configPath.getParent());
            Properties diskSettings = new Properties();
            diskSettings.setProperty("jmeter_slow_request_threshold", "0");
            try (var output = Files.newOutputStream(configPath)) {
                diskSettings.store(output, "test");
            }

            props.clear();
            props.setProperty("proxy_enabled", "true");

            invokeSetAndSaveProperty("last_update_check_time", "12345");

            assertEquals(props.getProperty("proxy_enabled"), "true");
            assertEquals(props.getProperty("last_update_check_time"), "12345");
        } finally {
            props.clear();
            props.putAll(backup);
            restoreConfig(configPath, configExisted, originalConfig);
        }
    }

    private static Properties getSettingsProperties() throws Exception {
        Field propsField = SettingManager.class.getDeclaredField("props");
        propsField.setAccessible(true);
        return (Properties) propsField.get(null);
    }

    private static void invokeSetAndSaveProperty(String key, String value) throws Exception {
        Method method = SettingManager.class.getDeclaredMethod("setAndSaveProperty", String.class, String.class);
        method.setAccessible(true);
        method.invoke(null, key, value);
    }

    private static void restoreConfig(Path configPath, boolean configExisted, String originalConfig) throws Exception {
        if (configExisted) {
            Files.writeString(configPath, originalConfig);
        } else {
            Files.deleteIfExists(configPath);
        }
    }
}
