package com.laker.postman.performance.core.extractor;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ExtractorTypeTest {

    @Test
    public void shouldResolveStorageValuesToEnums() {
        assertEquals(ExtractorType.fromStorageValue("JSONPath"), ExtractorType.JSON_PATH);
        assertEquals(ExtractorType.fromStorageValue("Regex"), ExtractorType.REGEX);
        assertEquals(ExtractorType.fromStorageValue("Response Field"), ExtractorType.RESPONSE_FIELD);
        assertEquals(ExtractorType.fromStorageValue("unknown"), ExtractorType.JSON_PATH);
    }

    @Test
    public void shouldDeclareWhichExtractorTypesNeedResponseBody() {
        assertTrue(ExtractorType.JSON_PATH.requiresResponseBody());
        assertTrue(ExtractorType.REGEX.requiresResponseBody());
        assertFalse(ExtractorType.HEADER.requiresResponseBody());
        assertFalse(ExtractorType.COOKIE.requiresResponseBody());
        assertFalse(ExtractorType.RESPONSE_FIELD.requiresResponseBody());
    }

    @Test
    public void toStringShouldStayHeadlessSafeAndStorageStable() {
        assertEquals(ExtractorType.JSON_PATH.toString(), "JSONPath");
        assertEquals(ExtractorType.HEADER.toString(), "Header");
        assertEquals(ExtractorType.RESPONSE_FIELD.toString(), "Response Field");
    }

    @Test
    public void responseFieldsShouldExposeStableStorageAndDefaultVariableNames() {
        assertEquals(ResponseField.fromStorageValue("Status Code"), ResponseField.STATUS_CODE);
        assertEquals(ResponseField.fromStorageValue("unknown"), ResponseField.STATUS_CODE);
        assertEquals(ResponseField.STATUS_CODE.getDefaultVariableName(), "statusCode");
        assertEquals(ResponseField.RESPONSE_TIME_MS.getDefaultVariableName(), "responseTimeMs");
    }
}
