package com.laker.postman.common.component;

import com.laker.postman.test.AbstractSwingUiTest;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;

import static org.testng.Assert.assertEquals;

public class MarkdownEditorPanelTest extends AbstractSwingUiTest {

    @Test
    public void programmaticMarkdownLoadShouldNotBeUndoable() throws Exception {
        MarkdownEditorPanel[] holder = new MarkdownEditorPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new MarkdownEditorPanel();
            holder[0].setText("loaded description");
            triggerUndo(readEditor(holder[0]));
        });

        assertEquals(holder[0].getText(), "loaded description");
    }

    @Test
    public void userEditAfterProgrammaticMarkdownLoadShouldRemainUndoable() throws Exception {
        MarkdownEditorPanel[] holder = new MarkdownEditorPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new MarkdownEditorPanel();
            holder[0].setText("loaded description");
            RSyntaxTextArea editor = readEditor(holder[0]);
            editor.append("\n");
            triggerUndo(editor);
        });

        assertEquals(holder[0].getText(), "loaded description");
    }

    private static void triggerUndo(RSyntaxTextArea editor) {
        Action action = editor.getActionMap().get("undo");
        action.actionPerformed(new ActionEvent(editor, ActionEvent.ACTION_PERFORMED, "undo"));
    }

    private static RSyntaxTextArea readEditor(MarkdownEditorPanel panel) {
        try {
            Field field = MarkdownEditorPanel.class.getDeclaredField("editorArea");
            field.setAccessible(true);
            return (RSyntaxTextArea) field.get(panel);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
