package com.laker.postman.plugin.redis;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import com.laker.postman.common.component.EasyComboBox;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
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

    private static RedisPanel buildActionBarOnlyPanel() throws Exception {
        RedisPanel panel = redisPanelWithoutConstructor();
        Method buildActionBar = RedisPanel.class.getDeclaredMethod("buildActionBar");
        buildActionBar.setAccessible(true);
        buildActionBar.invoke(panel);
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
