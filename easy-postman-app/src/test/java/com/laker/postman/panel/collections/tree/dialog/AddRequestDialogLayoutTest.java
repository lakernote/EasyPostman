package com.laker.postman.panel.collections.tree.dialog;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class AddRequestDialogLayoutTest {

    @Test
    public void addRequestDialogShouldReserveHeightForProtocolCardsAndFooter() {
        assertEquals(AddRequestDialog.DIALOG_HEIGHT, 280,
                "Add request dialog should stay compact while leaving room for protocol cards above the fixed footer");
    }
}
