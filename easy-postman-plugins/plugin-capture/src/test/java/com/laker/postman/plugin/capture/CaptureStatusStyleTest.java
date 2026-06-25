package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class CaptureStatusStyleTest {

    @Test
    public void shouldClassifyStatusCodesByHttpRange() {
        assertEquals(CaptureStatusStyle.toneFor(200), CaptureStatusStyle.Tone.SUCCESS);
        assertEquals(CaptureStatusStyle.toneFor("302 Found"), CaptureStatusStyle.Tone.REDIRECT);
        assertEquals(CaptureStatusStyle.toneFor(405), CaptureStatusStyle.Tone.CLIENT_ERROR);
        assertEquals(CaptureStatusStyle.toneFor(503), CaptureStatusStyle.Tone.FAILURE);
    }

    @Test
    public void shouldTreatTlsProxyFailureAsFailure() {
        assertEquals(CaptureStatusStyle.toneFor(495), CaptureStatusStyle.Tone.FAILURE);
    }

    @Test
    public void shouldTreatBlankOrInvalidStatusAsPending() {
        assertEquals(CaptureStatusStyle.toneFor(""), CaptureStatusStyle.Tone.PENDING);
        assertEquals(CaptureStatusStyle.toneFor(null), CaptureStatusStyle.Tone.PENDING);
        assertEquals(CaptureStatusStyle.toneFor("pending"), CaptureStatusStyle.Tone.PENDING);
    }
}
