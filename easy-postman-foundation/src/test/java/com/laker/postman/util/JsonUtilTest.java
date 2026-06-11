package com.laker.postman.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class JsonUtilTest {

    @Test(description = "JSON 格式化应保留 JSON5/JSONC 风格注释")
    public void prettyPrintShouldPreserveJsonComments() {
        String formatted = JsonUtil.toJsonPrettyStr("""
                {
                  // request metadata
                  "url": "https://example.com/api", // keep inline
                  "nested": {
                    "enabled": true // keep tail
                  }
                }""");

        assertEquals(formatted, """
                {
                    // request metadata
                    "url": "https://example.com/api", // keep inline
                    "nested": {
                        "enabled": true // keep tail
                    }
                }""");
    }

    @Test(description = "URL 字符串里的双斜杠不应被误认为注释")
    public void prettyPrintShouldNotTreatUrlStringAsComment() {
        String formatted = JsonUtil.toJsonPrettyStr("""
                {"url":"https://example.com/a//b"}""");

        assertEquals(formatted, """
                {
                    "url": "https://example.com/a//b"
                }""");
    }
}
