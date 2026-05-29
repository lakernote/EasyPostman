package com.laker.postman.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class JsonPathUtilTest {

    @Test
    public void shouldExtractNestedArrayValueFromJsonString() {
        String json = """
                {
                  "users": [
                    {"name": "alice"},
                    {"name": "bob"}
                  ]
                }
                """;

        String value = JsonPathUtil.extractJsonPath(json, "$.users[1].name");

        assertEquals(value, "bob");
    }

    @Test
    public void shouldReturnNullForMissingPath() {
        String value = JsonPathUtil.extractJsonPath("{\"users\":[]}", "$.users[0].name");

        assertNull(value);
    }
}
