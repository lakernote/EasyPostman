package com.laker.postman.http.runtime.transport;

import okhttp3.CookieJar;
import okhttp3.JavaNetCookieJar;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ScopedCookieJarStore {
    static final String DEFAULT_SCOPE = "default";

    private final Map<String, CookieManager> cookieManagers = new ConcurrentHashMap<>();
    private final Map<String, CookieJar> cookieJars = new ConcurrentHashMap<>();

    CookieJar cookieJarForScope(String scope) {
        String normalizedScope = normalizeScope(scope);
        return cookieJars.computeIfAbsent(normalizedScope, ignored ->
                new JavaNetCookieJar(cookieManagerForScope(normalizedScope))
        );
    }

    void clear() {
        cookieManagers.values().forEach(cookieManager -> cookieManager.getCookieStore().removeAll());
        cookieManagers.clear();
        cookieJars.clear();
    }

    private CookieManager cookieManagerForScope(String scope) {
        return cookieManagers.computeIfAbsent(scope, ignored -> new CookieManager(null, CookiePolicy.ACCEPT_ALL));
    }

    static String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return DEFAULT_SCOPE;
        }
        return scope;
    }
}
