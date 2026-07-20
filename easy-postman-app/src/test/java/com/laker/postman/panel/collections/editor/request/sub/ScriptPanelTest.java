package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.component.StandardEditorTokenPainter;
import com.laker.postman.test.AbstractSwingUiTest;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.Test;

import javax.swing.*;
import java.lang.reflect.Field;

import static org.testng.Assert.assertEquals;

public class ScriptPanelTest extends AbstractSwingUiTest {

    @Test
    public void programmaticScriptLoadShouldNotBeUndoable() throws Exception {
        ScriptPanel[] holder = new ScriptPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ScriptPanel();
            holder[0].setPrescript("pm.test('loaded', () => {});");
            readEditor(holder[0], "prescriptArea").undoLastAction();
        });

        assertEquals(holder[0].getPrescript(), "pm.test('loaded', () => {});");
    }

    @Test
    public void userEditAfterProgrammaticScriptLoadShouldRemainUndoable() throws Exception {
        ScriptPanel[] holder = new ScriptPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ScriptPanel();
            holder[0].setPrescript("pm.test('loaded', () => {});");
            RSyntaxTextArea editor = readEditor(holder[0], "prescriptArea");
            editor.append("\n");
            editor.undoLastAction();
        });

        assertEquals(holder[0].getPrescript(), "pm.test('loaded', () => {});");
    }

    @Test
    public void scriptEditorsShouldUseSharedFallbackAwareTokenPainter() throws Exception {
        ScriptPanel[] holder = new ScriptPanel[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new ScriptPanel());

        assertFallbackAwareTokenPainter(readEditor(holder[0], "prescriptArea"));
        assertFallbackAwareTokenPainter(readEditor(holder[0], "postscriptArea"));
    }

    private static void assertFallbackAwareTokenPainter(RSyntaxTextArea editor) throws Exception {
        Field field = RSyntaxTextArea.class.getDeclaredField("tokenPainter");
        field.setAccessible(true);
        assertEquals(field.get(editor).getClass(), StandardEditorTokenPainter.class);
    }

    private static RSyntaxTextArea readEditor(ScriptPanel panel, String fieldName) {
        try {
            Field field = ScriptPanel.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (RSyntaxTextArea) field.get(panel);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
