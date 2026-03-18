package com.example;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContext;

import java.util.concurrent.atomic.AtomicInteger;

public class TestRuntimePlugin implements EasyPostmanPlugin {

    private static final AtomicInteger LOAD_COUNT = new AtomicInteger();
    private static final AtomicInteger START_COUNT = new AtomicInteger();
    private static final AtomicInteger STOP_COUNT = new AtomicInteger();

    public static void reset() {
        LOAD_COUNT.set(0);
        START_COUNT.set(0);
        STOP_COUNT.set(0);
    }

    public static int getLoadCount() {
        return LOAD_COUNT.get();
    }

    public static int getStartCount() {
        return START_COUNT.get();
    }

    public static int getStopCount() {
        return STOP_COUNT.get();
    }

    @Override
    public void onLoad(PluginContext context) {
        LOAD_COUNT.incrementAndGet();
        context.registerScriptApi("testRuntime", Object::new);
    }

    @Override
    public void onStart() {
        START_COUNT.incrementAndGet();
    }

    @Override
    public void onStop() {
        STOP_COUNT.incrementAndGet();
    }
}
