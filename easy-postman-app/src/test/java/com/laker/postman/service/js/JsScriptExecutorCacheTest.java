package com.laker.postman.service.js;

import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

public class JsScriptExecutorCacheTest {

    @Test(description = "identical scripts should reuse the cached GraalVM Source")
    public void identicalScriptsShouldReuseCachedSource() throws Exception {
        clearScriptSourceCache();
        String script = "const result = 40 + 2;";

        JsScriptExecutor.executeScript(script, Map.of(), null);
        Object firstSource = scriptSourceCache().get(script);
        JsScriptExecutor.executeScript(script, Map.of(), null);
        Object secondSource = scriptSourceCache().get(script);

        assertEquals(scriptSourceCache().size(), 1);
        assertSame(secondSource, firstSource);
    }

    @Test(description = "cached script sources should still use fresh Java bindings on each execution")
    public void cachedSourceShouldUseFreshBindings() throws Exception {
        clearScriptSourceCache();
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
        assertEquals(scriptSourceCache().size(), 1);
    }

    private static void clearScriptSourceCache() throws Exception {
        Map<String, Object> cache = scriptSourceCache();
        synchronized (cache) {
            cache.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> scriptSourceCache() {
        try {
            Field field = JsScriptExecutor.class.getDeclaredField("SCRIPT_SOURCE_CACHE");
            field.setAccessible(true);
            return (Map<String, Object>) field.get(null);
        } catch (ReflectiveOperationException e) {
            fail("Missing JS script source cache field", e);
            return Map.of();
        }
    }
}
