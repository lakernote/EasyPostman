package com.laker.postman.panel.sidebar;

import org.testng.annotations.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SidebarBottomBarTest {

    @Test
    public void bottomBarItemsShouldLeaveMoreSpaceBelowWindowEdge() {
        SidebarBottomBar bottomBar = noopBottomBar();

        List<JLabel> labels = collectLabels(bottomBar.leftPanel(), bottomBar.rightPanel());

        assertTrue(labels.size() >= 6);
        for (JLabel label : labels) {
            Insets insets = label.getBorder().getBorderInsets(label);
            assertEquals(insets.top, 2);
            assertEquals(insets.bottom, 6);
        }
    }

    private static List<JLabel> collectLabels(JPanel... panels) {
        List<JLabel> labels = new ArrayList<>();
        for (JPanel panel : panels) {
            for (Component component : panel.getComponents()) {
                if (component instanceof JLabel label) {
                    labels.add(label);
                }
            }
        }
        return labels;
    }

    private static SidebarBottomBar noopBottomBar() {
        return new SidebarBottomBar(
                false,
                () -> {
                },
                () -> {
                },
                () -> {
                },
                () -> {
                },
                () -> {
                }
        );
    }
}
