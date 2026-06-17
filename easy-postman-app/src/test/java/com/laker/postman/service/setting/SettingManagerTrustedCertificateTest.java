package com.laker.postman.service.setting;

import cn.hutool.json.JSONUtil;
import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.certificate.TrustedCertificateEntry;
import com.laker.postman.http.runtime.model.PreparedRequest;
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

            String json = props.getProperty("custom_trust_material_entries");
            assertTrue(JSONUtil.parseArray(json).size() == 2);
            List<TrustedCertificateEntry> entries = SettingManager.getCustomTrustMaterialEntries();
            assertEquals(entries.size(), 2);
            assertEquals(entries.get(0).getPath(), "/tmp/a.pem");
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
            assertEquals(SettingManager.getPerformanceSlowRequestThreshold(), 0);

            props.setProperty("performance_slow_request_threshold", "-20");
            assertEquals(SettingManager.getPerformanceSlowRequestThreshold(), 0);

            SettingManager.setPerformanceSlowRequestThreshold(-50);
            assertEquals(props.getProperty("performance_slow_request_threshold"), "0");
        } finally {
            props.clear();
            props.putAll(backup);
            restoreConfig(configPath, configExisted, originalConfig);
        }
    }

    @Test
    public void shouldNormalizePerformanceConnectionAndDispatcherSettingsToPositiveDefaults() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        Path configPath = Path.of(ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        boolean configExisted = Files.exists(configPath);
        String originalConfig = configExisted ? Files.readString(configPath) : null;

        try {
            props.clear();
            props.setProperty("performance_max_idle_connections", "0");
            props.setProperty("performance_keep_alive_seconds", "0");
            props.setProperty("performance_max_requests", "0");
            props.setProperty("performance_max_requests_per_host", "0");
            props.setProperty("performance_js_context_acquire_timeout_ms", "0");

            assertEquals(SettingManager.getPerformanceMaxIdleConnections(), 100);
            assertEquals(SettingManager.getPerformanceKeepAliveSeconds(), 60L);
            assertEquals(SettingManager.getPerformanceMaxRequests(), 1000);
            assertEquals(SettingManager.getPerformanceMaxRequestsPerHost(), 1000);
            assertEquals(SettingManager.getPerformanceJsContextAcquireTimeoutMs(), 1_000);

            SettingManager.setPerformanceMaxIdleConnections(0);
            SettingManager.setPerformanceKeepAliveSeconds(0);
            SettingManager.setPerformanceMaxRequests(0);
            SettingManager.setPerformanceMaxRequestsPerHost(0);
            SettingManager.setPerformanceJsContextAcquireTimeoutMs(0);

            assertEquals(props.getProperty("performance_max_idle_connections"), "100");
            assertEquals(props.getProperty("performance_keep_alive_seconds"), "60");
            assertEquals(props.getProperty("performance_max_requests"), "1000");
            assertEquals(props.getProperty("performance_max_requests_per_host"), "1000");
            assertEquals(props.getProperty("performance_js_context_acquire_timeout_ms"), "1000");
        } finally {
            props.clear();
            props.putAll(backup);
            restoreConfig(configPath, configExisted, originalConfig);
        }
    }

    @Test
    public void shouldReadAndNormalizePerformanceResponseBodyPreviewLimit() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            assertEquals(SettingManager.getPerformanceResponseBodyPreviewLimitKb(), 64);

            props.setProperty("performance_response_body_preview_limit_kb", "4");
            assertEquals(SettingManager.getPerformanceResponseBodyPreviewLimitKb(), 4);

            props.setProperty("performance_response_body_preview_limit_kb", "0");
            assertEquals(SettingManager.getPerformanceResponseBodyPreviewLimitKb(), 64);

            props.setProperty("performance_response_body_preview_limit_kb", "2048");
            assertEquals(SettingManager.getPerformanceResponseBodyPreviewLimitKb(), 64);

            props.setProperty("performance_response_body_preview_limit_kb", "abc");
            assertEquals(SettingManager.getPerformanceResponseBodyPreviewLimitKb(), 64);
        } finally {
            props.clear();
            props.putAll(backup);
        }
    }

    @Test
    public void shouldPersistSanitizedPerformanceResponseBodyPreviewLimit() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        Path configPath = Path.of(ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        boolean configExisted = Files.exists(configPath);
        String originalConfig = configExisted ? Files.readString(configPath) : null;

        try {
            props.clear();

            SettingManager.setPerformanceResponseBodyPreviewLimitKb(4);
            assertEquals(props.getProperty("performance_response_body_preview_limit_kb"), "4");

            SettingManager.setPerformanceResponseBodyPreviewLimitKb(0);
            assertEquals(props.getProperty("performance_response_body_preview_limit_kb"), "64");
        } finally {
            props.clear();
            props.putAll(backup);
            restoreConfig(configPath, configExisted, originalConfig);
        }
    }

    @Test
    public void shouldReadAndNormalizePerformanceResultRowLimit() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            assertEquals(SettingManager.getPerformanceResultRowLimit(), 3_000);

            props.setProperty("performance_result_row_limit", "5000");
            assertEquals(SettingManager.getPerformanceResultRowLimit(), 5000);

            props.setProperty("performance_result_row_limit", "99");
            assertEquals(SettingManager.getPerformanceResultRowLimit(), 3_000);

            props.setProperty("performance_result_row_limit", "100001");
            assertEquals(SettingManager.getPerformanceResultRowLimit(), 3_000);

            props.setProperty("performance_result_row_limit", "abc");
            assertEquals(SettingManager.getPerformanceResultRowLimit(), 3_000);
        } finally {
            props.clear();
            props.putAll(backup);
        }
    }

    @Test
    public void shouldPersistSanitizedPerformanceResultRowLimit() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        Path configPath = Path.of(ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        boolean configExisted = Files.exists(configPath);
        String originalConfig = configExisted ? Files.readString(configPath) : null;

        try {
            props.clear();

            SettingManager.setPerformanceResultRowLimit(5000);
            assertEquals(props.getProperty("performance_result_row_limit"), "5000");

            SettingManager.setPerformanceResultRowLimit(99);
            assertEquals(props.getProperty("performance_result_row_limit"), "3000");
        } finally {
            props.clear();
            props.putAll(backup);
            restoreConfig(configPath, configExisted, originalConfig);
        }
    }

    @Test
    public void shouldReadAndNormalizeGitDiffLargeFileThreshold() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            assertEquals(SettingManager.getGitDiffLargeFileThresholdMb(), 2);
            assertEquals(SettingManager.getGitDiffLargeFileThresholdBytes(), 2L * 1024 * 1024);

            props.setProperty("git_diff_large_file_threshold_mb", "4");
            assertEquals(SettingManager.getGitDiffLargeFileThresholdMb(), 4);
            assertEquals(SettingManager.gitDiffLargeFileThresholdBytes(4), 4L * 1024 * 1024);

            props.setProperty("git_diff_large_file_threshold_mb", "0");
            assertEquals(SettingManager.getGitDiffLargeFileThresholdMb(), 2);

            props.setProperty("git_diff_large_file_threshold_mb", "65");
            assertEquals(SettingManager.getGitDiffLargeFileThresholdMb(), 2);

            props.setProperty("git_diff_large_file_threshold_mb", "abc");
            assertEquals(SettingManager.getGitDiffLargeFileThresholdMb(), 2);
        } finally {
            props.clear();
            props.putAll(backup);
        }
    }

    @Test
    public void shouldPersistSanitizedGitDiffLargeFileThreshold() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        Path configPath = Path.of(ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        boolean configExisted = Files.exists(configPath);
        String originalConfig = configExisted ? Files.readString(configPath) : null;

        try {
            props.clear();

            SettingManager.setGitDiffLargeFileThresholdMb(4);
            assertEquals(props.getProperty("git_diff_large_file_threshold_mb"), "4");

            SettingManager.setGitDiffLargeFileThresholdMb(65);
            assertEquals(props.getProperty("git_diff_large_file_threshold_mb"), "2");
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
            diskSettings.setProperty("performance_slow_request_threshold", "0");
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
