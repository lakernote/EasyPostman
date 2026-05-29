package com.laker.postman.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class TimeDisplayUtilTest {

    @Test
    public void shouldFormatElapsedTimeByScale() {
        assertEquals(TimeDisplayUtil.formatElapsedTime(999), "999 ms");
        assertEquals(TimeDisplayUtil.formatElapsedTime(1500), "1.50 s");
        assertEquals(TimeDisplayUtil.formatElapsedTime(65_000), "1m 5s");
    }
}
