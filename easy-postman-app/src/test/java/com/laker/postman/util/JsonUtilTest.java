package com.laker.postman.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class JsonUtilTest {

    @Test(description = "转义和反转义应保持多行文本内容不变")
    public void testEscapeAndUnescapeJsonStringContent_RoundTrip() {
        String original = "{\n  \"name\": \"demo\",\n  \"enabled\": true\n}";

        String escaped = JsonUtil.escapeJsonStringContent(original);
        String unescaped = JsonUtil.unescapeJsonStringContent(escaped);

        assertEquals(unescaped, original);
    }

    @Test(description = "反转义不应把路径中的字面量 n 和 t 误转成换行或制表符")
    public void testUnescapeJsonStringContent_PathLikeText() {
        String escaped = "C:\\\\new\\\\temp\\\\tab";

        String unescaped = JsonUtil.unescapeJsonStringContent(escaped);

        assertEquals(unescaped, "C:\\new\\temp\\tab");
    }

    @Test(description = "反转义应正确处理字面量反斜杠加控制字符")
    public void testUnescapeJsonStringContent_LiteralEscapedSequences() {
        String escaped = "\\\\n \\\\t \\\\r";

        String unescaped = JsonUtil.unescapeJsonStringContent(escaped);

        assertEquals(unescaped, "\\n \\t \\r");
    }

    @Test(description = "反转义应支持 unicode 序列")
    public void testUnescapeJsonStringContent_Unicode() {
        String escaped = "\\u4f60\\u597d";

        String unescaped = JsonUtil.unescapeJsonStringContent(escaped);

        assertEquals(unescaped, "你好");
    }
}
