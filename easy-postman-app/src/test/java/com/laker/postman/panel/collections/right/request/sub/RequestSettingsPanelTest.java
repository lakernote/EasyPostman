package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.service.setting.SettingManager;
import org.testng.annotations.Test;

import javax.swing.*;
import java.lang.reflect.Field;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class RequestSettingsPanelTest {

    @Test
    public void shouldRejectOversizedTimeoutValue() throws Exception {
        RequestSettingsPanel panel = new RequestSettingsPanel();
        setRequestTimeoutText(panel, "999999999999");

        assertNotNull(panel.validateSettings());
    }

    @Test
    public void shouldNotThrowWhenApplyingOversizedTimeoutValue() throws Exception {
        RequestSettingsPanel panel = new RequestSettingsPanel();
        setRequestTimeoutText(panel, "999999999999");

        HttpRequestItem item = new HttpRequestItem();
        panel.applyTo(item);

        assertNull(item.getRequestTimeoutMs());
    }

    @Test
    public void shouldPreserveInheritedSettingsWhenGlobalDefaultsChange() throws Exception {
        boolean oldFollowRedirects = SettingManager.isFollowRedirects();
        boolean oldSslVerificationDisabled = SettingManager.isRequestSslVerificationDisabled();

        try {
            SettingManager.setFollowRedirects(true);
            SettingManager.setRequestSslVerificationDisabled(false);

            HttpRequestItem original = new HttpRequestItem();
            original.setFollowRedirects(null);
            original.setSslVerificationEnabled(null);

            RequestSettingsPanel panel = new RequestSettingsPanel();
            panel.populate(original);

            SettingManager.setFollowRedirects(false);
            SettingManager.setRequestSslVerificationDisabled(true);

            HttpRequestItem saved = new HttpRequestItem();
            panel.applyTo(saved);

            assertNull(saved.getFollowRedirects());
            assertNull(saved.getSslVerificationEnabled());
            assertFalse(panel.hasCustomSettings());
        } finally {
            SettingManager.setFollowRedirects(oldFollowRedirects);
            SettingManager.setRequestSslVerificationDisabled(oldSslVerificationDisabled);
        }
    }

    @Test
    public void shouldKeepExplicitOverrideWhenUserChangesInheritedValueAfterGlobalChange() throws Exception {
        boolean oldFollowRedirects = SettingManager.isFollowRedirects();

        try {
            SettingManager.setFollowRedirects(true);

            HttpRequestItem original = new HttpRequestItem();
            original.setFollowRedirects(null);

            RequestSettingsPanel panel = new RequestSettingsPanel();
            panel.populate(original);

            SettingManager.setFollowRedirects(false);
            setSwitchSelected(panel, "followRedirectsSwitch", false);

            HttpRequestItem saved = new HttpRequestItem();
            panel.applyTo(saved);

            assertFalse(saved.getFollowRedirects());
            assertTrue(panel.hasCustomSettings());
        } finally {
            SettingManager.setFollowRedirects(oldFollowRedirects);
        }
    }

    @Test
    public void shouldRebaselineFollowRedirectsAfterSave() throws Exception {
        boolean oldFollowRedirects = SettingManager.isFollowRedirects();

        try {
            SettingManager.setFollowRedirects(false);

            HttpRequestItem original = new HttpRequestItem();
            original.setFollowRedirects(null);

            RequestSettingsPanel panel = new RequestSettingsPanel();
            panel.populate(original);

            setSwitchSelected(panel, "followRedirectsSwitch", true);
            HttpRequestItem saved = new HttpRequestItem();
            panel.applyTo(saved);
            panel.rebaseline(saved);

            setSwitchSelected(panel, "followRedirectsSwitch", false);
            HttpRequestItem savedAgain = new HttpRequestItem();
            panel.applyTo(savedAgain);

            assertFalse(savedAgain.getFollowRedirects());
        } finally {
            SettingManager.setFollowRedirects(oldFollowRedirects);
        }
    }

    private static void setRequestTimeoutText(RequestSettingsPanel panel, String value) throws Exception {
        Field field = RequestSettingsPanel.class.getDeclaredField("requestTimeoutField");
        field.setAccessible(true);
        JTextField timeoutField = (JTextField) field.get(panel);
        timeoutField.setText(value);
    }

    private static void setSwitchSelected(RequestSettingsPanel panel, String fieldName, boolean selected) throws Exception {
        Field field = RequestSettingsPanel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        AbstractButton button = (AbstractButton) field.get(panel);
        button.setSelected(selected);
    }
}
