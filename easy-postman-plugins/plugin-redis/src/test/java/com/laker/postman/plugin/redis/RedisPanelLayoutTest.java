package com.laker.postman.plugin.redis;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static com.laker.postman.plugin.redis.RedisI18n.t;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.common.component.connection.ConnectionToolbarUi;
import com.laker.postman.util.I18nUtil;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import org.testng.annotations.Test;
import sun.misc.Unsafe;

public class RedisPanelLayoutTest {

    @Test
    public void templateComboShouldUseFixedMaxEasyComboBox() throws Exception {
        RedisPanel panel = buildActionBarOnlyPanel();
        JComboBox<String> templateCombo = stringCombo(panel, "templateCombo");

        assertTrue(templateCombo instanceof EasyComboBox<?>,
                "template selector should use EasyComboBox so localized item widths are measured consistently");
        assertFixedMaxWidth(templateCombo);
    }

    @Test
    public void commandComboShouldUseFixedMaxEasyComboBox() throws Exception {
        RedisPanel panel = buildActionBarOnlyPanel();
        JComboBox<String> commandCombo = stringCombo(panel, "commandCombo");

        assertTrue(commandCombo instanceof EasyComboBox<?>,
                "command selector should use EasyComboBox so the longest command remains visible");
        assertFixedMaxWidth(commandCombo);
    }

    @Test
    public void authRowShouldStayNearContentWidthInWideContainer() throws Exception {
        RedisPanel panel = new RedisPanel();
        invoke(panel, "setAuthOptionsVisible", true);
        panel.setSize(new Dimension(1800, 900));
        layoutRecursively(panel);

        JPanel authRow = fieldValue(panel, "authRow", JPanel.class);

        assertNotNull(authRow);
        assertTrue(authRow.getWidth() > 0, "auth row should be laid out");
        assertTrue(authRow.getWidth() <= authRow.getPreferredSize().width + 16,
                "auth row width " + authRow.getWidth()
                        + " should stay near preferred width " + authRow.getPreferredSize().width);
    }

    @Test
    public void connectionActionsShouldUseIconOnlyToolbarButtonsWithTooltips() {
        Locale originalLocale = I18nUtil.currentLocale();
        try {
            assertConnectionActionsUseIconOnlyButtons(Locale.CHINESE);
            assertConnectionActionsUseIconOnlyButtons(Locale.ENGLISH);
        } finally {
            I18nUtil.setLocale(originalLocale);
        }
    }

    @Test
    public void hostFieldShouldStayCompactAcrossLocales() throws Exception {
        Locale originalLocale = I18nUtil.currentLocale();
        try {
            assertHostFieldUsesCompactWidth(Locale.CHINESE);
            assertHostFieldUsesCompactWidth(Locale.ENGLISH);
        } finally {
            I18nUtil.setLocale(originalLocale);
        }
    }

    @Test
    public void actionBarShouldUseCompactActionsAcrossLocales() throws Exception {
        Locale originalLocale = I18nUtil.currentLocale();
        try {
            assertActionBarUsesCompactActions(Locale.CHINESE);
            assertActionBarUsesCompactActions(Locale.ENGLISH);
        } finally {
            I18nUtil.setLocale(originalLocale);
        }
    }

    @Test
    public void leftSidebarTabsShouldUseCompactLocalizedTitlesAcrossLocales() {
        Locale originalLocale = I18nUtil.currentLocale();
        try {
            assertLeftSidebarTabsUseCompactTitle(Locale.CHINESE);
            assertLeftSidebarTabsUseCompactTitle(Locale.ENGLISH);
        } finally {
            I18nUtil.setLocale(originalLocale);
        }
    }

    private static void assertConnectionActionsUseIconOnlyButtons(Locale locale) {
        I18nUtil.setLocale(locale);
        RedisPanel panel = layoutPanel(1320);

        assertIconOnlyToolbarButton(panel, t(MessageKeys.TOOLBOX_REDIS_CONNECT));
        assertIconOnlyToolbarButton(panel, t(MessageKeys.TOOLBOX_REDIS_DISCONNECT));
    }

    private static void assertHostFieldUsesCompactWidth(Locale locale) throws Exception {
        I18nUtil.setLocale(locale);
        RedisPanel panel = layoutPanel(1320);

        JTextField hostField = fieldValue(panel, "hostField", JTextField.class);

        assertTrue(hostField.getWidth() > 0, locale + " host field should be laid out");
        assertTrue(hostField.getWidth() <= 220,
                locale + " Redis host field should stay compact because port and DB have separate controls: "
                        + hostField.getWidth());
    }

    private static void assertActionBarUsesCompactActions(Locale locale) throws Exception {
        I18nUtil.setLocale(locale);
        RedisPanel panel = layoutPanel(1320);

        assertIconOnlyToolbarButton(panel, t(MessageKeys.TOOLBOX_REDIS_LOAD_TEMPLATE));

        JButton executeButton = fieldValue(panel, "executeBtn", JButton.class);
        assertEquals(executeButton.getText(), t(MessageKeys.TOOLBOX_REDIS_EXECUTE_SHORT),
                locale + " execute button should use compact text");
        assertEquals(executeButton.getToolTipText(), t(MessageKeys.TOOLBOX_REDIS_EXECUTE),
                locale + " execute button tooltip should keep full action");
        assertCompactPrimaryActionButton(executeButton, locale);
    }

    private static void assertLeftSidebarTabsUseCompactTitle(Locale locale) {
        I18nUtil.setLocale(locale);
        RedisPanel panel = layoutPanel(1320);

        JTabbedPane leftTabs = findTabbedPaneWithTitle(panel, t(MessageKeys.TOOLBOX_REDIS_HISTORY));
        assertNotNull(leftTabs, locale + " left tabs should be present");
        assertEquals(leftTabs.getTitleAt(0), t(MessageKeys.TOOLBOX_REDIS_KEYS_TAB),
                locale + " keys tab should use compact title");
        assertEquals(leftTabs.getToolTipTextAt(0), t(MessageKeys.TOOLBOX_REDIS_KEYS_MANAGEMENT),
                locale + " keys tab tooltip should keep full title");
        assertTrue(leftTabs.getBounds().width <= 260,
                locale + " left tabs should stay within sidebar width instead of expanding for long text");
    }

    private static RedisPanel buildActionBarOnlyPanel() throws Exception {
        RedisPanel panel = redisPanelWithoutConstructor();
        Method buildActionBar = RedisPanel.class.getDeclaredMethod("buildActionBar");
        buildActionBar.setAccessible(true);
        buildActionBar.invoke(panel);
        return panel;
    }

    private static RedisPanel layoutPanel(int width) {
        RedisPanel panel = new RedisPanel();
        panel.setSize(new Dimension(width, 760));
        layoutRecursively(panel);
        return panel;
    }

    private static RedisPanel redisPanelWithoutConstructor() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        return (RedisPanel) unsafe.allocateInstance(RedisPanel.class);
    }

    @SuppressWarnings("unchecked")
    private static JComboBox<String> stringCombo(RedisPanel panel, String fieldName) throws Exception {
        Field field = RedisPanel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (JComboBox<String>) field.get(panel);
    }

    private static void assertFixedMaxWidth(JComboBox<String> comboBox) {
        int shortestIndex = 0;
        int longestIndex = 0;
        int shortestWidth = Integer.MAX_VALUE;
        int longestWidth = -1;
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            int width = renderedWidth(comboBox, comboBox.getItemAt(i), i);
            if (width < shortestWidth) {
                shortestWidth = width;
                shortestIndex = i;
            }
            if (width > longestWidth) {
                longestWidth = width;
                longestIndex = i;
            }
        }

        comboBox.setSelectedIndex(shortestIndex);
        int compactSelectionWidth = comboBox.getPreferredSize().width;
        comboBox.setSelectedIndex(longestIndex);
        int longestSelectionWidth = comboBox.getPreferredSize().width;

        assertEquals(compactSelectionWidth, longestSelectionWidth,
                "fixed-max combo should not resize when selecting shorter localized values");
    }

    private static void assertIconOnlyToolbarButton(RedisPanel panel, String tooltip) {
        JButton button = findButtonByTooltip(panel, tooltip);
        assertNotNull(button, "button not found: " + tooltip);
        assertTrue(button.getText() == null || button.getText().isBlank(),
                tooltip + " should use icon-only text");
        assertNotNull(button.getIcon(), tooltip + " should expose an SVG icon");
        assertTrue(FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON.equals(
                        button.getClientProperty(FlatClientProperties.BUTTON_TYPE)),
                tooltip + " should reuse the shared toolbar button style");
        Dimension expectedSize = new Dimension(ConnectionToolbarUi.TOOLBAR_BUTTON_SIZE,
                ConnectionToolbarUi.TOOLBAR_BUTTON_SIZE);
        assertEquals(button.getPreferredSize(), expectedSize,
                tooltip + " should use the shared toolbar button preferred size");
        assertEquals(button.getMinimumSize(), expectedSize,
                tooltip + " should use the shared toolbar button minimum size");
        assertEquals(button.getMaximumSize(), expectedSize,
                tooltip + " should use the shared toolbar button maximum size");
    }

    private static void assertCompactPrimaryActionButton(JButton button, Locale locale) {
        assertEquals(button.getClass().getName(),
                "com.laker.postman.common.component.button.CompactPrimaryButton",
                locale + " execute button should use the shared compact primary action component");
        assertEquals(button.getPreferredSize().height, 30,
                locale + " compact primary action should not use the larger regular primary height");
        assertEquals(button.getHeight(), 30,
                locale + " compact primary action should keep its rendered height");
        String style = String.valueOf(button.getClientProperty(FlatClientProperties.STYLE));
        assertTrue(style.contains("arc: 6"), locale + " compact primary action should be a rounded rectangle");
        assertTrue(style.contains("margin: 2,8,2,8"),
                locale + " compact primary action should use compact margins");
    }

    private static JButton findButtonByTooltip(Component component, String tooltip) {
        if (component instanceof JButton button && tooltip.equals(button.getToolTipText())) {
            return button;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton found = findButtonByTooltip(child, tooltip);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JTabbedPane findTabbedPaneWithTitle(Component component, String title) {
        if (component instanceof JTabbedPane tabbedPane) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (title.equals(tabbedPane.getTitleAt(i))) {
                    return tabbedPane;
                }
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JTabbedPane found = findTabbedPaneWithTitle(child, title);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static int renderedWidth(JComboBox<String> comboBox, String value, int index) {
        @SuppressWarnings("unchecked")
        ListCellRenderer<? super String> renderer = (ListCellRenderer<? super String>) comboBox.getRenderer();
        Component component = renderer.getListCellRendererComponent(new JList<>(), value, index, false, false);
        if (component instanceof JLabel label) {
            return label.getPreferredSize().width;
        }
        return component.getPreferredSize().width;
    }

    private static void invoke(Object instance, String methodName, boolean value) throws Exception {
        Method method = instance.getClass().getDeclaredMethod(methodName, boolean.class);
        method.setAccessible(true);
        method.invoke(instance, value);
    }

    private static <T> T fieldValue(Object instance, String fieldName, Class<T> type) throws Exception {
        Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(instance));
    }

    private static void layoutRecursively(Component component) {
        if (component instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layoutRecursively(child);
            }
        }
    }
}
