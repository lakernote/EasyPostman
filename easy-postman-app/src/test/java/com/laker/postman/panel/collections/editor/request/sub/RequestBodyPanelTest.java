package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.request.model.RequestItemProtocolEnum;


import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;

import static org.testng.Assert.assertFalse;

public class RequestBodyPanelTest extends AbstractSwingUiTest {

    @Test
    public void shouldDisableMatchedBracketPopupForRequestEditor() throws Exception {
        RequestBodyPanel[] holder = new RequestBodyPanel[1];

        SwingUtilities.invokeAndWait(() -> holder[0] = new RequestBodyPanel(RequestItemProtocolEnum.HTTP));

        assertFalse(holder[0].getBodyArea().getShowMatchedBracketPopup());
    }
}
