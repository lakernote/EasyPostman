package com.laker.postman.common.component;

import com.formdev.flatlaf.FlatDarkLaf;
import com.laker.postman.common.constants.ThemeColors;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class MarkdownEditorPanelThemeTest extends AbstractSwingUiTest {
    private LookAndFeel previousLookAndFeel;
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeState() {
        previousLookAndFeel = UIManager.getLookAndFeel();
        previousThemeTokens = remember(ThemeColors.HOVER_BACKGROUND, ThemeColors.ERROR_DARK);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (previousLookAndFeel != null) {
            UIManager.setLookAndFeel(previousLookAndFeel);
        }
        restore(previousThemeTokens);
    }

    @Test
    public void previewInlineStylesShouldUseSemanticTokensInDarkTheme() throws Exception {
        FlatDarkLaf.setup();
        UIManager.put(ThemeColors.HOVER_BACKGROUND, new Color(1, 2, 3));
        UIManager.put(ThemeColors.ERROR_DARK, new Color(11, 12, 13));

        String[] styles = new String[2];
        SwingUtilities.invokeAndWait(() -> {
            MarkdownEditorPanel panel = new MarkdownEditorPanel();
            styles[0] = invokeString(panel, "getTableHeaderStyle");
            styles[1] = invokeString(panel, "getInlineCodeStyle");
        });

        assertTrue(styles[0].contains("background-color:#010203"));
        assertTrue(styles[1].contains("background-color:#010203"));
        assertTrue(styles[1].contains("color:#0b0c0d"));
    }

    @Test
    public void editorAndPreviewShouldUseInnerSplitWithoutNestedToolWindowCards() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MarkdownEditorPanel panel = new MarkdownEditorPanel();
            JSplitPane splitPane = (JSplitPane) readField(panel, "splitPane");

            assertEquals(splitPane.getDividerSize(), AppToolWindowChrome.DIVIDER_SIZE);
            assertSame(splitPane.getLeftComponent(), readField(panel, "editorPanelRef"));
            assertSame(splitPane.getRightComponent(), readField(panel, "previewPanelRef"));
        });
    }

    private static String invokeString(MarkdownEditorPanel panel, String methodName) {
        try {
            Method method = MarkdownEditorPanel.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            return (String) method.invoke(panel);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static Object readField(MarkdownEditorPanel panel, String fieldName) {
        try {
            java.lang.reflect.Field field = MarkdownEditorPanel.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(panel);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
