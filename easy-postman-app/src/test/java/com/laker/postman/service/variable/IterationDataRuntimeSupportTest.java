package com.laker.postman.service.variable;

import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class IterationDataRuntimeSupportTest {

    @Test
    public void shouldCopySourceRowWithoutAddingIterationIndexToDataVariables() {
        Map<String, String> sourceRow = new LinkedHashMap<>();
        sourceRow.put("userId", "123");

        Map<String, String> prepared = IterationDataRuntimeSupport.prepare(sourceRow);

        assertFalse(prepared.containsKey("i"));
        assertEquals(prepared.get("userId"), "123");
        assertFalse(sourceRow.containsKey("i"));
    }

    @Test
    public void shouldKeepExplicitIColumnValueForBackwardCompatibility() {
        Map<String, String> sourceRow = new LinkedHashMap<>();
        sourceRow.put("i", "custom-index");

        Map<String, String> prepared = IterationDataRuntimeSupport.prepare(sourceRow);

        assertEquals(prepared.get("i"), "custom-index");
    }

    @Test
    public void shouldResolveRandomTfPlaceholderOnceForPreparedRow() {
        Map<String, String> sourceRow = new LinkedHashMap<>();
        sourceRow.put("a", "{{$randomTF}}");

        Map<String, String> prepared = IterationDataRuntimeSupport.prepare(sourceRow);

        assertTrue(Set.of("T", "F").contains(prepared.get("a")));
        assertEquals(sourceRow.get("a"), "{{$randomTF}}");
    }
}
