package com.laker.postman.performance.core.extractor;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ExtractorTypeTest {

    @Test
    public void shouldResolveLegacyStorageValuesToEnums() {
        assertEquals(ExtractorType.fromStorageValue("JSONPath"), ExtractorType.JSON_PATH);
        assertEquals(ExtractorType.fromStorageValue("Regex"), ExtractorType.REGEX);
        assertEquals(ExtractorType.fromStorageValue("unknown"), ExtractorType.JSON_PATH);
    }

    @Test
    public void shouldDeclareWhichExtractorTypesNeedResponseBody() {
        assertTrue(ExtractorType.JSON_PATH.requiresResponseBody());
        assertTrue(ExtractorType.REGEX.requiresResponseBody());
        assertFalse(ExtractorType.HEADER.requiresResponseBody());
        assertFalse(ExtractorType.COOKIE.requiresResponseBody());
    }

    @Test
    public void toStringShouldStayHeadlessSafeAndStorageStable() {
        assertEquals(ExtractorType.JSON_PATH.toString(), "JSONPath");
        assertEquals(ExtractorType.HEADER.toString(), "Header");
    }
}
