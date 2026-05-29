package com.laker.postman.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class HttpHeaderConstantsTest {

    @Test
    public void shouldReturnCommonValuesCaseInsensitively() {
        assertTrue(HttpHeaderConstants.getCommonValuesForHeader("content-type").contains("application/json"));
        assertTrue(HttpHeaderConstants.getCommonValuesForHeader("ACCEPT").contains("*/*"));
    }

    @Test
    public void shouldReturnEmptyListForUnknownHeaders() {
        assertEquals(HttpHeaderConstants.getCommonValuesForHeader("X-Unknown-Header").size(), 0);
    }
}
