package com.laker.postman.util;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class EditorThemeUtilTest {
    private LookAndFeel previousLookAndFeel;

    @BeforeMethod
    public void rememberLookAndFeel() {
        previousLookAndFeel = UIManager.getLookAndFeel();
        EditorThemeUtil.clearConfiguredThemeResources();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        EditorThemeUtil.clearConfiguredThemeResources();
        if (previousLookAndFeel != null) {
            UIManager.setLookAndFeel(previousLookAndFeel);
        }
    }

    @Test
    public void themeResourcePathShouldFollowInstalledFlatLafTheme() {
        FlatDarkLaf.setup();
        assertEquals(EditorThemeUtil.themeResourcePath(), "/themes/easypostman-dark.xml");

        FlatLightLaf.setup();
        assertEquals(EditorThemeUtil.themeResourcePath(), "/themes/easypostman-light.xml");
    }

    @Test
    public void configuredThemeResourceShouldOverrideFlatLafDetection() {
        FlatDarkLaf.setup();

        EditorThemeUtil.configureThemeResources("themes/custom-light.xml", "fallback.xml");

        assertEquals(EditorThemeUtil.themeResourcePath(), "/themes/custom-light.xml");
    }

    @Test
    public void shouldFallBackToFlatLafDetectionWhenConfiguredPathIsBlank() {
        FlatLightLaf.setup();

        EditorThemeUtil.configureThemeResources(" ", " ");

        assertEquals(EditorThemeUtil.themeResourcePath(), "/themes/easypostman-light.xml");
    }

    @Test
    public void scrollPaneThemeShouldUseComfortableDarkEditorChrome() {
        FlatDarkLaf.setup();
        RTextScrollPane scrollPane = new RTextScrollPane(new RSyntaxTextArea());

        EditorThemeUtil.applyScrollPaneChrome(scrollPane);

        Gutter gutter = scrollPane.getGutter();
        assertEquals(scrollPane.getViewport().getBackground(), new Color(0x3A, 0x3D, 0x3F));
        assertEquals(gutter.getBackground(), new Color(0x38, 0x3B, 0x3D));
        assertEquals(gutter.getBorderColor(), new Color(0x45, 0x49, 0x4C));
        assertEquals(gutter.getLineNumberColor(), new Color(0x74, 0x7A, 0x82));
        assertEquals(gutter.getCurrentLineNumberColor(), new Color(0xA9, 0xB7, 0xC6));
        assertEditorScrollPaneBorderless(scrollPane);
    }

    @Test
    public void scrollPaneThemeShouldResetToLightEditorChrome() {
        RTextScrollPane scrollPane = new RTextScrollPane(new RSyntaxTextArea());

        FlatDarkLaf.setup();
        EditorThemeUtil.applyScrollPaneChrome(scrollPane);
        FlatLightLaf.setup();
        EditorThemeUtil.applyScrollPaneChrome(scrollPane);

        Gutter gutter = scrollPane.getGutter();
        assertEquals(scrollPane.getViewport().getBackground(), Color.WHITE);
        assertEquals(gutter.getBackground(), Color.WHITE);
        assertEquals(gutter.getBorderColor(), new Color(0xE0, 0xE0, 0xE0));
        assertEquals(gutter.getLineNumberColor(), new Color(0x99, 0x99, 0x99));
        assertEquals(gutter.getCurrentLineNumberColor(), new Color(0x66, 0x66, 0x66));
        assertEditorScrollPaneBorderless(scrollPane);
    }

    private void assertEditorScrollPaneBorderless(RTextScrollPane scrollPane) {
        assertTrue(scrollPane.getBorder() instanceof EmptyBorder);
    }
}
