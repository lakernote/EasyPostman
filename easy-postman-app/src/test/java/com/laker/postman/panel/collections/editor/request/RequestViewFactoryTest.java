package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.request.model.RequestItemProtocolEnum;


import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.common.constants.ThemeColors;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
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
            assertSame(components.reqTabs.getComponentAt(0), components.paramsTabPanel);
            assertTrue(hasTab(components.reqTabs, I18nUtil.getMessage(MessageKeys.TAB_PARAMS)));
        });
    }

    @Test
    public void paramsTabShouldShowQueryParamsBeforePathVariables() throws Exception {
        withHiddenRequestEditorTabs(List.of(), () -> {
            RequestViewComponents components = createView(RequestItemProtocolEnum.HTTP);
            JPanel paramsContent = paramsScrollContent(components);

            assertSame(paramsContent.getComponent(0), components.paramsPanel);
            assertSame(paramsContent.getComponent(2), components.pathVariablesPanel);
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

    @Test
    public void requestEditSubPanelShouldUseCardSurfaceBackgroundInsideToolWindow() throws Exception {
        Object previousSurface = UIManager.get(ThemeColors.SURFACE);
        Color surface = new Color(245, 247, 250);
        UIManager.put(ThemeColors.SURFACE, surface);

        try {
            RequestEditSubPanel panel = createRequestEditSubPanel(RequestItemProtocolEnum.HTTP);

            assertEquals(panel.getBackground(), surface);
        } finally {
            UIManager.put(ThemeColors.SURFACE, previousSurface);
        }
    }

    @Test
    public void requestResponseSplitShouldUseSubtleSeparatorAndCardSurfaces() throws Exception {
        Object previousBackground = UIManager.get(ThemeColors.BACKGROUND);
        Object previousSurface = UIManager.get(ThemeColors.SURFACE);
        Object previousSeparator = UIManager.get(ThemeColors.TAB_SEPARATOR);
        Color background = new Color(233, 234, 238);
        Color surface = new Color(255, 255, 255);
        Color separator = new Color(229, 231, 235);
        UIManager.put(ThemeColors.BACKGROUND, background);
        UIManager.put(ThemeColors.SURFACE, surface);
        UIManager.put(ThemeColors.TAB_SEPARATOR, separator);

        try {
            RequestViewComponents components = createView(RequestItemProtocolEnum.HTTP);

            assertNotNull(components.splitPane);
            assertEquals(components.splitPane.getDividerSize(), 4);
            assertEquals(components.splitPane.getBackground(), background);
            assertTrue(components.requestLinePanel.isOpaque());
            assertEquals(components.requestLinePanel.getBackground(), surface);
            assertTrue(components.reqTabs.isOpaque());
            assertEquals(components.reqTabs.getBackground(), surface);
            for (int i = 0; i < components.reqTabs.getTabCount(); i++) {
                Component tabContent = components.reqTabs.getComponentAt(i);
                assertTrue(tabContent instanceof JComponent, tabContent.getClass().getSimpleName());
                assertEquals(tabContent.getBackground(), surface, tabContent.getClass().getSimpleName());
            }
            assertTrue(components.responsePanel.isOpaque());
            assertEquals(components.responsePanel.getBackground(), surface);
            assertDividerPaintsSingleSeparatorLine(components.splitPane, surface, separator);
        } finally {
            UIManager.put(ThemeColors.BACKGROUND, previousBackground);
            UIManager.put(ThemeColors.SURFACE, previousSurface);
            UIManager.put(ThemeColors.TAB_SEPARATOR, previousSeparator);
        }
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
        JTabbedPane[] holder = new JTabbedPane[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = findTabbedPaneContaining(
                panel,
                I18nUtil.getMessage(MessageKeys.REQUEST_DOCS_TAB_TITLE)
        ));
        assertNotNull(holder[0]);
        return holder[0];
    }

    private JTabbedPane findTabbedPaneContaining(Component component, String tabTitle) {
        if (component instanceof JTabbedPane tabs && hasTab(tabs, tabTitle)) {
            return tabs;
        }
        if (!(component instanceof Container container)) {
            return null;
        }
        for (Component child : container.getComponents()) {
            JTabbedPane tabs = findTabbedPaneContaining(child, tabTitle);
            if (tabs != null) {
                return tabs;
            }
        }
        return null;
    }

    private boolean hasTab(JTabbedPane tabs, String title) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (title.equals(tabs.getTitleAt(i))) {
                return true;
            }
        }
        return false;
    }

    private JPanel paramsScrollContent(RequestViewComponents components) {
        assertTrue(components.paramsTabPanel.getComponent(0) instanceof JScrollPane);
        JScrollPane scrollPane = (JScrollPane) components.paramsTabPanel.getComponent(0);
        assertTrue(scrollPane.isWheelScrollingEnabled());
        assertTrue(scrollPane.getViewport().getView() instanceof JPanel);
        return (JPanel) scrollPane.getViewport().getView();
    }

    private void assertDividerPaintsSingleSeparatorLine(JSplitPane splitPane, Color surface, Color separator) {
        BasicSplitPaneDivider divider = ((BasicSplitPaneUI) splitPane.getUI()).getDivider();
        int dividerSize = splitPane.getDividerSize();
        int width = splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT ? dividerSize : 80;
        int height = splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT ? 80 : dividerSize;
        divider.setSize(width, height);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            divider.paint(graphics);
        } finally {
            graphics.dispose();
        }

        if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            int x = Math.max(0, width / 2);
            assertEquals(new Color(image.getRGB(0, 10), true), surface);
            assertEquals(new Color(image.getRGB(x, 10), true), separator);
        } else {
            int y = Math.max(0, height / 2);
            assertEquals(new Color(image.getRGB(10, 0), true), surface);
            assertEquals(new Color(image.getRGB(10, y), true), separator);
        }
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
