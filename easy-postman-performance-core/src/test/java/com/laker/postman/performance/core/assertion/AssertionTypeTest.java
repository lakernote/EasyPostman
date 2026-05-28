package com.laker.postman.performance.core.assertion;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class AssertionTypeTest {

    @Test
    public void shouldResolveStorageValuesToEnums() {
        assertEquals(AssertionType.fromStorageValue("Response Code"), AssertionType.RESPONSE_CODE);
        assertEquals(AssertionType.fromStorageValue("Contains"), AssertionType.CONTAINS);
        assertEquals(AssertionType.fromStorageValue("JSONPath"), AssertionType.JSON_PATH);
        assertEquals(AssertionType.fromStorageValue("unknown"), AssertionType.RESPONSE_CODE);
    }

    @Test
    public void shouldDeclareWhichAssertionTypesNeedResponseBody() {
        assertFalse(AssertionType.RESPONSE_CODE.requiresResponseBody());
        assertTrue(AssertionType.CONTAINS.requiresResponseBody());
        assertTrue(AssertionType.JSON_PATH.requiresResponseBody());
    }

    @Test
    public void toStringShouldStayHeadlessSafeAndStorageStable() {
        assertEquals(AssertionType.RESPONSE_CODE.toString(), "Response Code");
        assertEquals(AssertionType.JSON_PATH.toString(), "JSONPath");
    }

}
