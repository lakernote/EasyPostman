package com.laker.postman.panel.sidebar;

import org.testng.annotations.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SidebarTabComponentFactoryTest {

    @Test
    public void collapsedTabShouldCenterIconWithBorderLayout() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Collections", new JPanel());
        tabbedPane.setSelectedIndex(0);
        SidebarTabComponentFactory factory = new SidebarTabComponentFactory(
                tabbedPane,
                () -> false,
                ignored -> 0,
                () -> 80,
                () -> SidebarTabMetrics.collapsedWidth(20),
                () -> new Font(Font.SANS_SERIF, Font.PLAIN, 12),
                () -> new Font(Font.SANS_SERIF, Font.BOLD, 12)
        );

        Component component = factory.create(
                SidebarTab.COLLECTIONS,
                "Collections",
                SidebarTab.COLLECTIONS.getIcon()
        );

        assertTrue(component instanceof JPanel);
        JPanel panel = (JPanel) component;
        assertEquals(panel.getPreferredSize().height, SidebarTabMetrics.collapsedHeight(SidebarTab.COLLECTIONS.getIcon().getIconHeight()));
        assertTrue(panel.getLayout() instanceof BorderLayout);
        Component iconLabel = findIconLabel(panel);
        assertEquals(((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER), iconLabel);
        assertTrue(iconLabel instanceof JLabel);
        assertEquals(((JLabel) iconLabel).getHorizontalAlignment(), SwingConstants.CENTER);
        assertEquals(((JLabel) iconLabel).getVerticalAlignment(), SwingConstants.CENTER);

        Border border = panel.getBorder();
        Insets insets = border.getBorderInsets(panel);
        assertEquals(insets.left, SidebarTabMetrics.collapsedIconPaddingLeft());
        assertEquals(insets.right, SidebarTabMetrics.collapsedIconPaddingRight());
    }

    private Component findIconLabel(JComponent component) {
        for (Component child : component.getComponents()) {
            if ("iconLabel".equals(child.getName())) {
                return child;
            }
        }
        return null;
    }
}
