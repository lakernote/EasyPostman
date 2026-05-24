package com.laker.postman.panel.performance.assertion;

import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class AssertionTypeTest {

    @Test
    public void shouldResolveLegacyStorageValuesToEnums() {
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
    public void shouldHaveEnglishAndChineseLabelsForAllTypes() {
        ResourceBundle en = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        ResourceBundle zh = ResourceBundle.getBundle("messages", Locale.CHINESE);

        assertEquals(en.getString(MessageKeys.PERFORMANCE_ASSERTION_TYPE_RESPONSE_CODE), "Response Code");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_ASSERTION_TYPE_CONTAINS), "Contains");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_ASSERTION_TYPE_JSON_PATH), "JSONPath");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_ASSERTION_TYPE_RESPONSE_CODE), "响应码");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_ASSERTION_TYPE_CONTAINS), "包含内容");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_ASSERTION_TYPE_JSON_PATH), "JSONPath");
    }
}
