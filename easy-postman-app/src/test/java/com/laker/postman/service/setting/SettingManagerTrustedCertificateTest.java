package com.laker.postman.service.setting;

import cn.hutool.json.JSONUtil;
import com.laker.postman.model.TrustedCertificateEntry;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
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

    private static Properties getSettingsProperties() throws Exception {
        Field propsField = SettingManager.class.getDeclaredField("props");
        propsField.setAccessible(true);
        return (Properties) propsField.get(null);
    }
}
