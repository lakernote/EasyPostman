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

    @Test
    public void shouldClassifySourceAndByteTones() {
        assertEquals(CaptureTableStyle.sourceTone("codex · PID 25562"), CaptureTableStyle.Tone.INFO);
        assertEquals(CaptureTableStyle.sourceTone("127.0.0.1:53211"), CaptureTableStyle.Tone.NORMAL);
        assertEquals(CaptureTableStyle.bytesTone(0), CaptureTableStyle.Tone.MUTED);
        assertEquals(CaptureTableStyle.bytesTone(120 * 1024), CaptureTableStyle.Tone.WARNING);
        assertEquals(CaptureTableStyle.bytesTone(2 * 1024 * 1024), CaptureTableStyle.Tone.FAILURE);
    }
}
