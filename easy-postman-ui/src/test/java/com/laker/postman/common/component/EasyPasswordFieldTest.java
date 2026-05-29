package com.laker.postman.common.component;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class EasyPasswordFieldTest {

    @Test
    public void shouldExposePasswordTextAndRevealButtonFlag() {
        EasyPasswordField field = new EasyPasswordField(12, "Password");

        field.setPasswordText("secret");
        field.setShowRevealButton(false);

        assertEquals(field.getPasswordText(), "secret");
        assertFalse(field.isShowRevealButton());
    }
}
