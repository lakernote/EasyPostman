package com.laker.postman.plugin.capture;

final class CaptureRuntime {
    private static volatile CaptureProxyService proxyService;

    private CaptureRuntime() {
    }

    static CaptureProxyService proxyService() {
        CaptureProxyService current = proxyService;
        if (current != null) {
            return current;
        }
        synchronized (CaptureRuntime.class) {
            current = proxyService;
            if (current == null) {
                current = new CaptureProxyService();
                proxyService = current;
            }
            return current;
        }
    }

    static void stopQuietly() {
        CaptureProxyService current = proxyService;
        if (current == null) {
            return;
        }
        try {
            if (current.isRunning() || current.isSystemProxySynced()) {
                current.stop();
            }
        } catch (Exception ignored) {
            // Runtime shutdown should not be blocked by best-effort proxy cleanup.
        }
    }
}
