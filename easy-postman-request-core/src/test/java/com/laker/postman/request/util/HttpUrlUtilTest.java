package com.laker.postman.request.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class HttpUrlUtilTest {

    @Test
    public void shouldDecodeReadableTextButPreserveReservedQueryEscapesForDisplay() {
        String url = "https://example.com/api?" +
                "nested=https%3A%2F%2Fstatic.example.com%2Fpath%2Fk%3Dv%26next%3D1" +
                "&keyword=%E4%B8%AD%E6%96%87";

        String displayUrl = HttpUrlUtil.decodeQueryForDisplay(url);

        assertEquals(displayUrl,
                "https://example.com/api?" +
                        "nested=https%3A%2F%2Fstatic.example.com%2Fpath%2Fk%3Dv%26next%3D1" +
                        "&keyword=中文");
    }
}
