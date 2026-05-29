package com.laker.postman.common.component.table;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;

public class ImprovedTableRowTransferHandlerThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.BORDER_MEDIUM);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void dragImageBorderShouldUseSemanticBorderMediumColor() {
        Color border = new Color(31, 32, 33);
        UIManager.put(ThemeColors.BORDER_MEDIUM, border);

        DefaultTableModel model = new DefaultTableModel(new Object[][]{{"a"}}, new Object[]{"name"});
        JTable table = new JTable(model);
        table.setSelectionBackground(new Color(0, 0, 0, 0));
        table.setSize(80, table.getRowHeight());
        table.setRowSelectionInterval(0, 0);

        ExposedHandler handler = new ExposedHandler(model);
        handler.exposeCreateTransferable(table);

        Image dragImage = handler.getDragImage();
        assertNotNull(dragImage);
        BufferedImage bufferedImage = (BufferedImage) dragImage;
        Color pixel = new Color(bufferedImage.getRGB(0, 0), true);
        assertClose(pixel.getRed(), border.getRed());
        assertClose(pixel.getGreen(), border.getGreen());
        assertClose(pixel.getBlue(), border.getBlue());
    }

    private static class ExposedHandler extends ImprovedTableRowTransferHandler {
        private ExposedHandler(DefaultTableModel model) {
            super(model, null);
        }

        private void exposeCreateTransferable(JComponent component) {
            createTransferable(component);
        }
    }

    private void assertClose(int actual, int expected) {
        assertTrue(Math.abs(actual - expected) <= 1, "expected " + actual + " to be within 1 of " + expected);
    }
}
