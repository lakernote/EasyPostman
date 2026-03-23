package com.laker.postman.plugin.capture;

final class CaptureRuntime {
    private static final CaptureProxyService PROXY_SERVICE = new CaptureProxyService();

    private CaptureRuntime() {
    }

    static CaptureProxyService proxyService() {
        return PROXY_SERVICE;
    }

    static void stopQuietly() {
        try {
            if (PROXY_SERVICE.isRunning() || PROXY_SERVICE.isSystemProxySynced()) {
                PROXY_SERVICE.stop();
            }
        } catch (Exception ignored) {
            // Runtime shutdown should not be blocked by best-effort proxy cleanup.
        }
    }
}
