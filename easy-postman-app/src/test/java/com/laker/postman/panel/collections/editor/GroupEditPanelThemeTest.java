package com.laker.postman.panel.collections.editor;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.common.constants.ThemeColors;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class GroupEditPanelThemeTest extends AbstractSwingUiTest {

    @Test
    public void groupEditPanelShouldUseCardSurfaceInsideToolWindow() throws Exception {
        Object previousSurface = UIManager.get(ThemeColors.SURFACE);
        Color surface = new Color(255, 255, 255);
        UIManager.put(ThemeColors.SURFACE, surface);

        try {
            GroupEditPanel panel = createGroupEditPanel();

            assertTrue(panel.isOpaque());
            assertEquals(panel.getBackground(), surface);
            JTabbedPane tabs = findTabbedPane(panel);
            assertNotNull(tabs);
            assertTrue(tabs.isOpaque());
            assertEquals(tabs.getBackground(), surface);
        } finally {
            UIManager.put(ThemeColors.SURFACE, previousSurface);
        }
    }

    private GroupEditPanel createGroupEditPanel() throws Exception {
        GroupEditPanel[] holder = new GroupEditPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            RequestGroup group = new RequestGroup("Group");
            holder[0] = new GroupEditPanel(new DefaultMutableTreeNode(group), group, () -> {
            });
        });
        return holder[0];
    }

    private JTabbedPane findTabbedPane(Component component) {
        if (component instanceof JTabbedPane tabs) {
            return tabs;
        }
        if (!(component instanceof Container container)) {
            return null;
        }
        for (Component child : container.getComponents()) {
            JTabbedPane tabs = findTabbedPane(child);
            if (tabs != null) {
                return tabs;
            }
        }
        return null;
    }
}
