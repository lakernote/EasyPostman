package com.laker.postman.service.js;

import com.laker.postman.model.Environment;
import com.laker.postman.service.js.api.PostmanApiContext;
import com.laker.postman.service.variable.ExecutionContextScope;
import com.laker.postman.service.variable.ExecutionVariableContext;
import com.laker.postman.service.variable.VariablesService;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
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

    @Test(description = "plain pm variable scripts should not load lazy built-in libraries")
    public void plainPostmanVariableScriptShouldNotLoadLazyBuiltinLibraries() throws Exception {
        JsContextPool replacementPool = new JsContextPool(1);
        JsContextPool previousPool = getStaticField("contextPool", JsContextPool.class);
        int previousPoolSize = getStaticIntField("contextPoolSize");
        int previousTimeoutMs = getStaticIntField("contextAcquireTimeoutMs");
        JsLibraryLoader.clearCache();

        try {
            setStaticField("contextPool", replacementPool);
            setStaticField("contextPoolSize", 1);
            setStaticField("contextAcquireTimeoutMs", 1000);

            ExecutionVariableContext executionContext = new ExecutionVariableContext();
            try (ExecutionContextScope ignored = ExecutionContextScope.open(executionContext)) {
                PostmanApiContext pm = new PostmanApiContext(new Environment("test"));
                pm.info.setWebSocketSendInfo(17, 700, "WS Send");
                Map<String, Object> bindings = new HashMap<>();
                bindings.put("pm", pm);

                JsScriptExecutor.executeScript("""
                        pm.variables.set('i', pm.info.wsSendIndex);
                        pm.variables.set('a', Math.random() < 0.5 ? 'T' : 'F');
                        """, bindings, null);

                assertEquals(VariablesService.getInstance().get("i"), "17");
                assertTrue(Set.of("T", "F").contains(VariablesService.getInstance().get("a")));
            }

            assertFalse(JsLibraryLoader.isBuiltinLibraryCachedForTests("lodash"));
            assertFalse(JsLibraryLoader.isBuiltinLibraryCachedForTests("crypto-js"));
            assertFalse(JsLibraryLoader.isBuiltinLibraryCachedForTests("moment"));

            StringBuilder sink = new StringBuilder();
            Map<String, Object> lodashBindings = new HashMap<>();
            lodashBindings.put("sink", sink);
            JsScriptExecutor.executeScript("sink.append(_.uniq([1, 1, 2]).join(','));", lodashBindings, null);

            assertEquals(sink.toString(), "1,2");
            assertTrue(JsLibraryLoader.isBuiltinLibraryCachedForTests("lodash"));
        } finally {
            setStaticField("contextPool", previousPool);
            setStaticField("contextPoolSize", previousPoolSize);
            setStaticField("contextAcquireTimeoutMs", previousTimeoutMs);
            replacementPool.shutdown();
            JsLibraryLoader.clearCache();
        }
    }

    @Test(description = "run-scoped script executor should not initialize the shared JS pool")
    public void pooledScriptExecutorShouldNotInitializeSharedContextPool() throws Exception {
        JsContextPool previousPool = getStaticField("contextPool", JsContextPool.class);
        int previousPoolSize = getStaticIntField("contextPoolSize");
        int previousTimeoutMs = getStaticIntField("contextAcquireTimeoutMs");
        setStaticField("contextPool", null);
        setStaticField("contextPoolSize", 0);
        setStaticField("contextAcquireTimeoutMs", 0);

        try (JsScriptExecutor.PooledScriptExecutor ignored = new JsScriptExecutor.PooledScriptExecutor(1, 1000)) {
            assertEquals(getStaticField("contextPool", JsContextPool.class), null);
        } finally {
            JsContextPool createdPool = getStaticField("contextPool", JsContextPool.class);
            if (createdPool != null && createdPool != previousPool) {
                createdPool.shutdown();
            }
            setStaticField("contextPool", previousPool);
            setStaticField("contextPoolSize", previousPoolSize);
            setStaticField("contextAcquireTimeoutMs", previousTimeoutMs);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> sourceCache() throws Exception {
        Field cacheField = JsScriptExecutor.class.getDeclaredField("SCRIPT_SOURCE_CACHE");
        cacheField.setAccessible(true);
        return (Map<Object, Object>) cacheField.get(null);
    }

    private static int getStaticIntField(String fieldName) throws Exception {
        return (Integer) staticField(fieldName).get(null);
    }

    private static <T> T getStaticField(String fieldName, Class<T> type) throws Exception {
        return type.cast(staticField(fieldName).get(null));
    }

    private static void setStaticField(String fieldName, Object value) throws Exception {
        staticField(fieldName).set(null, value);
    }

    private static Field staticField(String fieldName) throws Exception {
        Field field = JsScriptExecutor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }
}
