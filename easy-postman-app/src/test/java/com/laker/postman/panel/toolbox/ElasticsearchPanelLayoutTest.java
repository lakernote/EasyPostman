package com.laker.postman.panel.toolbox;

import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ElasticsearchPanelLayoutTest extends AbstractSwingUiTest {

    @Test
    public void authRowShouldStayNearContentWidthInWideContainer() throws Exception {
        ElasticsearchPanel panel = new ElasticsearchPanel();
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
