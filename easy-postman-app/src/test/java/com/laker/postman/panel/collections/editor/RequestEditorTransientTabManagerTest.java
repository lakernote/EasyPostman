package com.laker.postman.panel.collections.editor;

import org.testng.annotations.Test;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class RequestEditorTransientTabManagerTest {

    @Test
    public void shouldStopTrackingTransientTabWhenMatchingPanelIsPinned() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabbedPane = new JTabbedPane();
            JComponent transientPanel = new JPanel();
            tabbedPane.addTab("Transient", transientPanel);
            tabbedPane.setTabComponentAt(0, new JPanel());

            RequestEditorTransientTabManager manager = new RequestEditorTransientTabManager(
                    tabbedPane,
                    index -> false,
                    () -> {
                    },
                    component -> {
                    }
            );
            setField(manager, "transientTab", transientPanel);
            setField(manager, "transientTabIndex", 0);

            assertTrue(manager.pinIfTransient(transientPanel));
            assertEquals(manager.transientTabIndex(), -1);
            assertSame(tabbedPane.getComponentAt(0), transientPanel);
        });
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
