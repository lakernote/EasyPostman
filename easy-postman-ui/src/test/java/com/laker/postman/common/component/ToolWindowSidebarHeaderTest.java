package com.laker.postman.common.component;

import com.laker.postman.common.component.button.ClearButton;
import com.laker.postman.common.component.button.RefreshButton;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import java.awt.Dimension;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class ToolWindowSidebarHeaderTest {
    private Map<String, Object> previousTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousTokens = new HashMap<>();
        previousTokens.put("defaultFont", UIManager.get("defaultFont"));
    }

    @AfterMethod
    public void restoreThemeTokens() {
        previousTokens.forEach(UIManager::put);
    }

    @Test
    public void shouldUseConsistentSidebarHeaderInsetsAndActionSizes() {
        RefreshButton refreshButton = new RefreshButton();
        ClearButton clearButton = new ClearButton();

        ToolWindowSidebarHeader header = new ToolWindowSidebarHeader("Items", refreshButton, clearButton);

        assertTrue(!header.isOpaque());
        assertTrue(header.getBorder() instanceof CompoundBorder);
        assertEquals(header.getInsets().top, ToolWindowSidebarHeader.VERTICAL_PADDING);
        assertEquals(header.getInsets().left, ToolWindowSidebarHeader.HORIZONTAL_PADDING);
        assertEquals(header.getInsets().bottom, ToolWindowSidebarHeader.VERTICAL_PADDING + 1);
        assertEquals(header.getInsets().right, ToolWindowSidebarHeader.ACTION_GAP);

        assertEquals(header.getComponentCount(), 2);
        assertEquals(header.getTitleLabel().getText(), "Items");
        JPanel actionPanel = (JPanel) header.getComponent(1);
        assertSame(actionPanel.getComponent(0), refreshButton);
        assertSame(actionPanel.getComponent(2), clearButton);
        assertActionSize(refreshButton);
        assertActionSize(clearButton);
    }

    @Test
    public void shouldTrackConfiguredUiFontSizeForTitle() {
        UIManager.put("defaultFont", new Font(Font.DIALOG, Font.PLAIN, 16));

        ToolWindowSidebarHeader header = new ToolWindowSidebarHeader("Items");

        assertEquals(header.getTitleLabel().getFont().getSize(), 15);
        assertEquals(header.getTitleLabel().getFont().getStyle(), Font.BOLD);
    }

    private static void assertActionSize(AbstractButton button) {
        Dimension size = new Dimension(
                ToolWindowSidebarHeader.ACTION_SIZE,
                ToolWindowSidebarHeader.ACTION_SIZE
        );
        assertEquals(button.getPreferredSize(), size);
        assertEquals(button.getMinimumSize(), size);
        assertEquals(button.getMaximumSize(), size);
    }
}
