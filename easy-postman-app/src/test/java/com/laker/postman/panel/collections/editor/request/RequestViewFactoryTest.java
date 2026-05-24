package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.model.RequestEditSubPanelType;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import javax.swing.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class RequestViewFactoryTest extends AbstractSwingUiTest {

    @Test
    public void shouldHideRequestEditorTabsFromSettings() throws Exception {
        withHiddenRequestEditorTabs(List.of(
                SettingManager.REQUEST_EDITOR_TAB_DOCS,
                SettingManager.REQUEST_EDITOR_TAB_AUTH,
                SettingManager.REQUEST_EDITOR_TAB_SETTINGS
        ), () -> {
            RequestViewComponents components = createView(RequestItemProtocolEnum.HTTP);

            assertFalse(hasTab(components.reqTabs, I18nUtil.getMessage(MessageKeys.REQUEST_DOCS_TAB_TITLE)));
            assertFalse(hasTab(components.reqTabs, I18nUtil.getMessage(MessageKeys.TAB_AUTHORIZATION)));
            assertFalse(hasTab(components.reqTabs, I18nUtil.getMessage(MessageKeys.TAB_SETTINGS)));
            assertTrue(hasTab(components.reqTabs, I18nUtil.getMessage(MessageKeys.TAB_PARAMS)));
            assertTrue(hasTab(components.reqTabs, I18nUtil.getMessage(MessageKeys.TAB_REQUEST_HEADERS)));
            assertTrue(hasTab(components.reqTabs, I18nUtil.getMessage(MessageKeys.TAB_REQUEST_BODY)));
            assertTrue(hasTab(components.reqTabs, I18nUtil.getMessage(MessageKeys.TAB_SCRIPTS)));
        });
    }

    @Test
    public void shouldFallbackToParamsTabWhenAllRequestEditorTabsAreHidden() throws Exception {
        withHiddenRequestEditorTabs(List.of(
                SettingManager.REQUEST_EDITOR_TAB_DOCS,
                SettingManager.REQUEST_EDITOR_TAB_PARAMS,
                SettingManager.REQUEST_EDITOR_TAB_AUTH,
                SettingManager.REQUEST_EDITOR_TAB_HEADERS,
                SettingManager.REQUEST_EDITOR_TAB_BODY,
                SettingManager.REQUEST_EDITOR_TAB_SCRIPTS,
                SettingManager.REQUEST_EDITOR_TAB_SETTINGS
        ), () -> {
            RequestViewComponents components = createView(RequestItemProtocolEnum.HTTP);

            assertEquals(components.reqTabs.getTabCount(), 1);
            assertSame(components.reqTabs.getComponentAt(0), components.paramsPanel);
            assertTrue(hasTab(components.reqTabs, I18nUtil.getMessage(MessageKeys.TAB_PARAMS)));
        });
    }

    @Test
    public void shouldRefreshInitializedRequestEditorTabsVisibility() throws Exception {
        withHiddenRequestEditorTabs(List.of(), () -> {
            RequestEditSubPanel panel = createRequestEditSubPanel(RequestItemProtocolEnum.HTTP);
            JTabbedPane tabs = getRequestTabs(panel);
            assertTrue(hasTab(tabs, I18nUtil.getMessage(MessageKeys.REQUEST_DOCS_TAB_TITLE)));

            SettingManager.setHiddenRequestEditorTabs(List.of(
                    SettingManager.REQUEST_EDITOR_TAB_DOCS,
                    SettingManager.REQUEST_EDITOR_TAB_BODY
            ));
            SwingUtilities.invokeAndWait(panel::updateRequestEditorTabsVisibility);

            assertFalse(hasTab(tabs, I18nUtil.getMessage(MessageKeys.REQUEST_DOCS_TAB_TITLE)));
            assertFalse(hasTab(tabs, I18nUtil.getMessage(MessageKeys.TAB_REQUEST_BODY)));
            assertTrue(hasTab(tabs, I18nUtil.getMessage(MessageKeys.TAB_PARAMS)));
            assertTrue(hasTab(tabs, I18nUtil.getMessage(MessageKeys.TAB_REQUEST_HEADERS)));
        });
    }

    private RequestViewComponents createView(RequestItemProtocolEnum protocol) throws Exception {
        RequestViewComponents[] holder = new RequestViewComponents[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = RequestViewFactory.create(
                protocol,
                RequestEditSubPanelType.NORMAL,
                e -> {
                }
        ));
        return holder[0];
    }

    private RequestEditSubPanel createRequestEditSubPanel(RequestItemProtocolEnum protocol) throws Exception {
        RequestEditSubPanel[] holder = new RequestEditSubPanel[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new RequestEditSubPanel("tab-visibility-test", protocol));
        return holder[0];
    }

    private JTabbedPane getRequestTabs(RequestEditSubPanel panel) throws Exception {
        Field reqTabsField = RequestEditSubPanel.class.getDeclaredField("reqTabs");
        reqTabsField.setAccessible(true);
        return (JTabbedPane) reqTabsField.get(panel);
    }

    private boolean hasTab(JTabbedPane tabs, String title) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (title.equals(tabs.getTitleAt(i))) {
                return true;
            }
        }
        return false;
    }

    private void withHiddenRequestEditorTabs(List<String> hiddenTabs, CheckedRunnable runnable) throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        Path configPath = Path.of(ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        boolean configExisted = Files.exists(configPath);
        String originalConfig = configExisted ? Files.readString(configPath) : null;

        try {
            SettingManager.setHiddenRequestEditorTabs(hiddenTabs);
            runnable.run();
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

    private static void restoreConfig(Path configPath, boolean configExisted, String originalConfig) throws Exception {
        if (configExisted) {
            Files.writeString(configPath, originalConfig);
        } else {
            Files.deleteIfExists(configPath);
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
