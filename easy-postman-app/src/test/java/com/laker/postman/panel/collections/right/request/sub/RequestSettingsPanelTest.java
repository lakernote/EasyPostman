package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.service.setting.SettingManager;
import org.testng.annotations.Test;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.Objects;

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
    public void shouldPreserveInheritedSettingsWhenGlobalDefaultsChange() {
        boolean oldFollowRedirects = SettingManager.isFollowRedirects();

        try {
            SettingManager.setFollowRedirects(true);

            HttpRequestItem original = new HttpRequestItem();
            original.setFollowRedirects(null);

            RequestSettingsPanel panel = new RequestSettingsPanel();
            panel.populate(original);

            SettingManager.setFollowRedirects(false);

            HttpRequestItem saved = new HttpRequestItem();
            panel.applyTo(saved);

            assertNull(saved.getFollowRedirects());
            assertFalse(panel.hasCustomSettings());
        } finally {
            SettingManager.setFollowRedirects(oldFollowRedirects);
        }
    }

    @Test
    public void shouldAllowExplicitFollowRedirectsOverrideToReturnToDefault() throws Exception {
        HttpRequestItem original = new HttpRequestItem();
        original.setFollowRedirects(Boolean.FALSE);

        RequestSettingsPanel panel = new RequestSettingsPanel();
        panel.populate(original);
        selectBooleanSetting(panel, "followRedirectsComboBox", null);

        HttpRequestItem saved = new HttpRequestItem();
        panel.applyTo(saved);

        assertNull(saved.getFollowRedirects());
        assertFalse(panel.hasCustomSettings());
    }

    @Test
    public void shouldAllowInheritedFollowRedirectsToBeSavedAsExplicitValue() throws Exception {
        boolean oldFollowRedirects = SettingManager.isFollowRedirects();

        try {
            SettingManager.setFollowRedirects(true);

            HttpRequestItem original = new HttpRequestItem();
            original.setFollowRedirects(null);

            RequestSettingsPanel panel = new RequestSettingsPanel();
            panel.populate(original);
            selectBooleanSetting(panel, "followRedirectsComboBox", Boolean.TRUE);

            HttpRequestItem saved = new HttpRequestItem();
            panel.applyTo(saved);

            assertTrue(saved.getFollowRedirects());
            assertTrue(panel.hasCustomSettings());
        } finally {
            SettingManager.setFollowRedirects(oldFollowRedirects);
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
            selectBooleanSetting(panel, "followRedirectsComboBox", Boolean.FALSE);

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

            selectBooleanSetting(panel, "followRedirectsComboBox", Boolean.TRUE);
            HttpRequestItem saved = new HttpRequestItem();
            panel.applyTo(saved);
            panel.rebaseline();

            selectBooleanSetting(panel, "followRedirectsComboBox", Boolean.FALSE);
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

    private static void selectBooleanSetting(RequestSettingsPanel panel, String fieldName, Boolean value) throws Exception {
        Field field = RequestSettingsPanel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        JComboBox<?> comboBox = (JComboBox<?>) field.get(panel);
        ComboBoxModel<?> model = comboBox.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            Object option = model.getElementAt(i);
            Field valueField = option.getClass().getDeclaredField("value");
            valueField.setAccessible(true);
            Object optionValue = valueField.get(option);
            if (Objects.equals(optionValue, value)) {
                comboBox.setSelectedIndex(i);
                return;
            }
        }
        throw new IllegalArgumentException("No combo option found for value: " + value);
    }

}
