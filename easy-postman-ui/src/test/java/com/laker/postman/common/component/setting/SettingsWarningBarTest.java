package com.laker.postman.common.component.setting;

import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SettingsWarningBarTest {

    @Test
    public void shouldExposeConfiguredMessageAndInvokeActions() {
        AtomicInteger discardCount = new AtomicInteger();
        AtomicInteger saveCount = new AtomicInteger();

        SettingsWarningBar bar = new SettingsWarningBar(
                "Unsaved changes",
                "Discard",
                "Save now",
                discardCount::incrementAndGet,
                saveCount::incrementAndGet
        );

        assertEquals(bar.getMessageLabel().getText(), "Unsaved changes");
        assertTrue(bar.getDiscardButton().getText().contains("Discard"));
        assertTrue(bar.getSaveButton().getText().contains("Save now"));

        bar.getDiscardButton().doClick();
        bar.getSaveButton().doClick();

        assertEquals(discardCount.get(), 1);
        assertEquals(saveCount.get(), 1);
    }
}
