package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.request.model.RequestItemProtocolEnum;


import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class RequestBodyPanelTest extends AbstractSwingUiTest {

    @Test
    public void shouldDisableMatchedBracketPopupForRequestEditor() throws Exception {
        RequestBodyPanel[] holder = new RequestBodyPanel[1];

        SwingUtilities.invokeAndWait(() -> holder[0] = new RequestBodyPanel(RequestItemProtocolEnum.HTTP));

        assertFalse(holder[0].getBodyArea().getShowMatchedBracketPopup());
    }

    @Test
    public void rawBodyShouldPreserveUserWhitespace() throws Exception {
        RequestBodyPanel[] holder = new RequestBodyPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new RequestBodyPanel(RequestItemProtocolEnum.HTTP);
            holder[0].getBodyArea().setText("  {\"a\":1}\n");
        });

        assertEquals(holder[0].getRawBody(), "  {\"a\":1}\n");
    }

    @Test
    public void programmaticRawBodyLoadShouldNotBeUndoable() throws Exception {
        RequestBodyPanel[] holder = new RequestBodyPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new RequestBodyPanel(RequestItemProtocolEnum.HTTP);
            holder[0].setRawBodyText("{\"loaded\":true}");
            triggerUndo(holder[0]);
        });

        assertEquals(holder[0].getRawBody(), "{\"loaded\":true}");
    }

    @Test
    public void userEditAfterProgrammaticRawBodyLoadShouldRemainUndoable() throws Exception {
        RequestBodyPanel[] holder = new RequestBodyPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new RequestBodyPanel(RequestItemProtocolEnum.HTTP);
            holder[0].setRawBodyText("{\"loaded\":true}");
            holder[0].getBodyArea().append("\n");
            triggerUndo(holder[0]);
        });

        assertEquals(holder[0].getRawBody(), "{\"loaded\":true}");
    }

    private static void triggerUndo(RequestBodyPanel panel) {
        Action action = panel.getBodyArea().getActionMap().get("Undo");
        action.actionPerformed(new ActionEvent(panel.getBodyArea(), ActionEvent.ACTION_PERFORMED, "Undo"));
    }
}
