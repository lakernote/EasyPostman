package com.laker.postman.service.js;

import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class JsScriptExecutorCacheTest {

    @Test(description = "user script Source cache should be bounded and reuse identical scripts")
    public void shouldBoundAndReuseUserScriptSourceCache() throws Exception {
        Map<?, ?> cache = sourceCache();
        cache.clear();

        Method sourceFactory = JsScriptExecutor.class.getDeclaredMethod("getCachedScriptSource", String.class);
        sourceFactory.setAccessible(true);

        try {
            Object first = sourceFactory.invoke(null, "sink.append(value);");
            Object second = sourceFactory.invoke(null, "sink.append(value);");
            assertSame(second, first);

            for (int i = 0; i < 600; i++) {
                sourceFactory.invoke(null, "var value" + i + " = " + i + ";");
            }
            assertTrue(cache.size() <= 512);
        } finally {
            cache.clear();
        }
    }

    @Test(description = "repeated script executions should still use fresh Java bindings")
    public void repeatedExecutionShouldUseFreshBindings() throws Exception {
        String script = "sink.append(value);";

        StringBuilder firstSink = new StringBuilder();
        Map<String, Object> firstBindings = new HashMap<>();
        firstBindings.put("sink", firstSink);
        firstBindings.put("value", "first");
        JsScriptExecutor.executeScript(script, firstBindings, null);

        StringBuilder secondSink = new StringBuilder();
        Map<String, Object> secondBindings = new HashMap<>();
        secondBindings.put("sink", secondSink);
        secondBindings.put("value", "second");
        JsScriptExecutor.executeScript(script, secondBindings, null);

        assertEquals(firstSink.toString(), "first");
        assertEquals(secondSink.toString(), "second");
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> sourceCache() throws Exception {
        Field cacheField = JsScriptExecutor.class.getDeclaredField("SCRIPT_SOURCE_CACHE");
        cacheField.setAccessible(true);
        return (Map<Object, Object>) cacheField.get(null);
    }
}
