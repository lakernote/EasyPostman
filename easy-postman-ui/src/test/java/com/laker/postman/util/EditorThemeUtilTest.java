package com.laker.postman.util;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.laker.postman.common.component.StandardEditorTokenPainter;
import com.laker.postman.common.component.ViewportClippedTokenPainter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.lang.reflect.Field;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class EditorThemeUtilTest {
    private LookAndFeel previousLookAndFeel;

    @BeforeMethod
    public void rememberLookAndFeel() {
        previousLookAndFeel = UIManager.getLookAndFeel();
        EditorThemeUtil.clearConfiguredThemeResources();
        EditorThemeUtil.clearConfiguredEditorFontApplier();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        EditorThemeUtil.clearConfiguredThemeResources();
        EditorThemeUtil.clearConfiguredEditorFontApplier();
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
    public void loadThemeShouldApplyIdeaLikeDarkEditorSurface() {
        FlatDarkLaf.setup();
        RSyntaxTextArea textArea = new RSyntaxTextArea();

        EditorThemeUtil.loadTheme(textArea);

        assertEquals(textArea.getBackground(), new Color(0x1E, 0x1F, 0x22));
        assertEquals(textArea.getCaretColor(), new Color(0xCE, 0xD0, 0xD6));
        assertEquals(textArea.getSelectionColor(), new Color(0x21, 0x42, 0x83));
    }

    @Test
    public void loadThemeShouldApplyRefinedLightEditorSurface() {
        FlatLightLaf.setup();
        RSyntaxTextArea textArea = new RSyntaxTextArea();

        EditorThemeUtil.loadTheme(textArea);

        assertEquals(textArea.getBackground(), Color.WHITE);
        assertEquals(textArea.getCaretColor(), new Color(0x11, 0x18, 0x27));
        assertEquals(textArea.getSelectionColor(), new Color(0xA6, 0xD2, 0xFF));
    }

    @Test
    public void loadThemeShouldInvokeConfiguredEditorFontApplierAfterTheme() {
        FlatLightLaf.setup();
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        Font expectedFont = new Font(Font.SERIF, Font.BOLD, 19);
        EditorThemeUtil.configureEditorFontApplier(area -> area.setFont(expectedFont));

        EditorThemeUtil.loadTheme(textArea);

        assertEquals(textArea.getFont(), expectedFont);
    }

    @Test
    public void loadThemeShouldInstallSharedStandardTokenPainter() throws Exception {
        RSyntaxTextArea textArea = new RSyntaxTextArea();

        EditorThemeUtil.loadTheme(textArea);

        Field field = RSyntaxTextArea.class.getDeclaredField("tokenPainter");
        field.setAccessible(true);
        assertEquals(field.get(textArea).getClass(), StandardEditorTokenPainter.class);
    }

    @Test
    public void shouldInstallViewportClippedSpecializationOnDemand() throws Exception {
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        EditorThemeUtil.loadTheme(textArea);

        EditorThemeUtil.installViewportClippedTokenPainter(textArea);

        Field field = RSyntaxTextArea.class.getDeclaredField("tokenPainter");
        field.setAccessible(true);
        assertEquals(field.get(textArea).getClass(), ViewportClippedTokenPainter.class);
    }

    @Test
    public void themeReloadShouldPreserveViewportClippedSpecialization() throws Exception {
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        EditorThemeUtil.loadTheme(textArea);
        EditorThemeUtil.installViewportClippedTokenPainter(textArea);

        EditorThemeUtil.loadTheme(textArea);

        Field field = RSyntaxTextArea.class.getDeclaredField("tokenPainter");
        field.setAccessible(true);
        assertEquals(field.get(textArea).getClass(), ViewportClippedTokenPainter.class);
    }

    @Test
    public void scrollPaneThemeShouldUseIdeaLikeDarkEditorChrome() {
        FlatDarkLaf.setup();
        RTextScrollPane scrollPane = new RTextScrollPane(new RSyntaxTextArea());

        EditorThemeUtil.applyScrollPaneChrome(scrollPane);

        Gutter gutter = scrollPane.getGutter();
        assertEquals(scrollPane.getViewport().getBackground(), new Color(0x1E, 0x1F, 0x22));
        assertEquals(gutter.getBackground(), new Color(0x1C, 0x1D, 0x20));
        assertEquals(gutter.getBorderColor(), new Color(0x2B, 0x2D, 0x30));
        assertEquals(gutter.getLineNumberColor(), new Color(0x6F, 0x73, 0x7A));
        assertEquals(gutter.getCurrentLineNumberColor(), new Color(0xAE, 0xB6, 0xC2));
        assertEditorScrollPaneBorderless(scrollPane);
        assertEditorScrollBarsBlendWith(scrollPane, new Color(0x1E, 0x1F, 0x22));
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
        assertEquals(gutter.getBorderColor(), new Color(0xE5, 0xE7, 0xEB));
        assertEquals(gutter.getLineNumberColor(), new Color(0x8A, 0x8F, 0x98));
        assertEquals(gutter.getCurrentLineNumberColor(), new Color(0x6B, 0x72, 0x80));
        assertEditorScrollPaneBorderless(scrollPane);
        assertEditorScrollBarsBlendWith(scrollPane, Color.WHITE);
    }

    private void assertEditorScrollPaneBorderless(RTextScrollPane scrollPane) {
        assertTrue(scrollPane.getBorder() instanceof EmptyBorder);
    }

    private void assertEditorScrollBarsBlendWith(RTextScrollPane scrollPane, Color expectedBackground) {
        assertEquals(scrollPane.getVerticalScrollBarPolicy(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        assertEquals(scrollPane.getHorizontalScrollBarPolicy(), ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        assertEquals(scrollPane.getVerticalScrollBar().getBackground(), expectedBackground);
        assertEquals(scrollPane.getHorizontalScrollBar().getBackground(), expectedBackground);
    }
}
