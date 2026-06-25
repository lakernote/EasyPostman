package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class CaptureValueFormatTest {

    @Test
    public void shouldFormatByteCountsForTableDisplay() {
        assertEquals(CaptureValueFormat.bytes(0), "0 B");
        assertEquals(CaptureValueFormat.bytes(153), "153 B");
        assertEquals(CaptureValueFormat.bytes(85_407), "83.4 KB");
        assertEquals(CaptureValueFormat.bytes(1_048_576), "1.0 MB");
    }

    @Test
    public void shouldFormatDurationsForTableDisplay() {
        assertEquals(CaptureValueFormat.duration(0), "0 ms");
        assertEquals(CaptureValueFormat.duration(984), "984 ms");
        assertEquals(CaptureValueFormat.duration(1_078), "1.08 s");
        assertEquals(CaptureValueFormat.duration(12_345), "12.3 s");
    }
}
