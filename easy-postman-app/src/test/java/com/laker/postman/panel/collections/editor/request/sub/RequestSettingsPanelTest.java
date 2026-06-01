package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.request.edit.HttpRequestEditorContentSummary;
import com.laker.postman.request.edit.HttpRequestEditorDraft;
import com.laker.postman.request.edit.HttpRequestSettingsDraft;
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

        assertNull(panel.collectSettings().getRequestTimeoutMs());
    }

    @Test
    public void shouldPreserveInheritedSettingsWhenGlobalDefaultsChange() {
        boolean oldFollowRedirects = SettingManager.isFollowRedirects();

        try {
            SettingManager.setFollowRedirects(true);

            RequestSettingsPanel panel = new RequestSettingsPanel();
            panel.populate(HttpRequestSettingsDraft.builder().followRedirects(null).build());

            SettingManager.setFollowRedirects(false);

            HttpRequestSettingsDraft saved = panel.collectSettings();

            assertNull(saved.getFollowRedirects());
            assertFalse(hasSettingsContent(saved));
        } finally {
            SettingManager.setFollowRedirects(oldFollowRedirects);
        }
    }

    @Test
    public void shouldAllowExplicitFollowRedirectsOverrideToReturnToDefault() throws Exception {
        RequestSettingsPanel panel = new RequestSettingsPanel();
        panel.populate(HttpRequestSettingsDraft.builder().followRedirects(Boolean.FALSE).build());
        selectBooleanSetting(panel, "followRedirectsComboBox", null);

        HttpRequestSettingsDraft saved = panel.collectSettings();

        assertNull(saved.getFollowRedirects());
        assertFalse(hasSettingsContent(saved));
    }

    @Test
    public void shouldAllowInheritedFollowRedirectsToBeSavedAsExplicitValue() throws Exception {
        boolean oldFollowRedirects = SettingManager.isFollowRedirects();

        try {
            SettingManager.setFollowRedirects(true);

            RequestSettingsPanel panel = new RequestSettingsPanel();
            panel.populate(HttpRequestSettingsDraft.builder().followRedirects(null).build());
            selectBooleanSetting(panel, "followRedirectsComboBox", Boolean.TRUE);

            HttpRequestSettingsDraft saved = panel.collectSettings();

            assertTrue(saved.getFollowRedirects());
            assertTrue(hasSettingsContent(saved));
        } finally {
            SettingManager.setFollowRedirects(oldFollowRedirects);
        }
    }

    @Test
    public void shouldKeepExplicitOverrideWhenUserChangesInheritedValueAfterGlobalChange() throws Exception {
        boolean oldFollowRedirects = SettingManager.isFollowRedirects();

        try {
            SettingManager.setFollowRedirects(true);

            RequestSettingsPanel panel = new RequestSettingsPanel();
            panel.populate(HttpRequestSettingsDraft.builder().followRedirects(null).build());

            SettingManager.setFollowRedirects(false);
            selectBooleanSetting(panel, "followRedirectsComboBox", Boolean.FALSE);

            HttpRequestSettingsDraft saved = panel.collectSettings();

            assertFalse(saved.getFollowRedirects());
            assertTrue(hasSettingsContent(saved));
        } finally {
            SettingManager.setFollowRedirects(oldFollowRedirects);
        }
    }

    @Test
    public void shouldRebaselineFollowRedirectsAfterSave() throws Exception {
        boolean oldFollowRedirects = SettingManager.isFollowRedirects();

        try {
            SettingManager.setFollowRedirects(false);

            RequestSettingsPanel panel = new RequestSettingsPanel();
            panel.populate(HttpRequestSettingsDraft.builder().followRedirects(null).build());

            selectBooleanSetting(panel, "followRedirectsComboBox", Boolean.TRUE);
            panel.collectSettings();
            panel.rebaseline();

            selectBooleanSetting(panel, "followRedirectsComboBox", Boolean.FALSE);
            HttpRequestSettingsDraft savedAgain = panel.collectSettings();

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

    private static boolean hasSettingsContent(HttpRequestSettingsDraft settings) {
        return HttpRequestEditorContentSummary.from(HttpRequestEditorDraft.builder()
                .followRedirects(settings.getFollowRedirects())
                .cookieJarEnabled(settings.getCookieJarEnabled())
                .httpVersion(settings.getHttpVersion())
                .requestTimeoutMs(settings.getRequestTimeoutMs())
                .build()).isHasSettings();
    }

}
