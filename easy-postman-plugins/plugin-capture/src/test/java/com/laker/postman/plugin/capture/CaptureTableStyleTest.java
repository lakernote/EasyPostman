package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class CaptureTableStyleTest {

    @Test
    public void shouldClassifyDurationSeverity() {
        assertEquals(CaptureTableStyle.durationTone(999), CaptureTableStyle.Tone.NORMAL);
        assertEquals(CaptureTableStyle.durationTone(1000), CaptureTableStyle.Tone.WARNING);
        assertEquals(CaptureTableStyle.durationTone(3000), CaptureTableStyle.Tone.FAILURE);
    }

    @Test
    public void shouldClassifyHttpMethodGroups() {
        assertEquals(CaptureTableStyle.methodTone("GET"), CaptureTableStyle.Tone.INFO);
        assertEquals(CaptureTableStyle.methodTone("POST"), CaptureTableStyle.Tone.WARNING);
        assertEquals(CaptureTableStyle.methodTone("PUT"), CaptureTableStyle.Tone.PRIMARY);
        assertEquals(CaptureTableStyle.methodTone("TLS"), CaptureTableStyle.Tone.FAILURE);
    }
}
