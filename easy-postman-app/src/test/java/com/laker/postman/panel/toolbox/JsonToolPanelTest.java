package com.laker.postman.panel.toolbox;

import com.laker.postman.test.AbstractSwingUiTest;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.Test;

import javax.swing.*;
import java.lang.reflect.Field;

import static org.testng.Assert.assertEquals;

public class JsonToolPanelTest extends AbstractSwingUiTest {

    @Test
    public void shouldUseViewportClippedTokenPainterForJsonEditors() throws Exception {
        JsonToolPanel[] holder = new JsonToolPanel[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new JsonToolPanel());

        assertViewportClippedTokenPainter(getTextArea(holder[0], "inputArea"));
        assertViewportClippedTokenPainter(getTextArea(holder[0], "outputArea"));
    }

    private void assertViewportClippedTokenPainter(RSyntaxTextArea textArea) throws Exception {
        Field field = RSyntaxTextArea.class.getDeclaredField("tokenPainter");
        field.setAccessible(true);
        Object tokenPainter = field.get(textArea);
        assertEquals(tokenPainter.getClass().getName(),
                "com.laker.postman.common.component.ViewportClippedTokenPainter");
    }

    private RSyntaxTextArea getTextArea(JsonToolPanel panel, String fieldName) throws Exception {
        Field field = JsonToolPanel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (RSyntaxTextArea) field.get(panel);
    }
}
