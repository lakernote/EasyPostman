package com.laker.postman.plugin.kafka;

final class KafkaClassLoaderSupport {

    private KafkaClassLoaderSupport() {
    }

    static <T, E extends Throwable> T withPluginContextClassLoader(ThrowingSupplier<T, E> action) throws E {
        Thread currentThread = Thread.currentThread();
        ClassLoader previous = currentThread.getContextClassLoader();
        ClassLoader pluginClassLoader = KafkaClassLoaderSupport.class.getClassLoader();
        if (previous == pluginClassLoader) {
            return action.get();
        }
        currentThread.setContextClassLoader(pluginClassLoader);
        try {
            return action.get();
        } finally {
            currentThread.setContextClassLoader(previous);
        }
    }

    @FunctionalInterface
    interface ThrowingSupplier<T, E extends Throwable> {
        T get() throws E;
    }
}
