package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.okhttp.HttpClientRuntimeConfig;
import com.laker.postman.http.runtime.okhttp.OkHttpClientManager;
import com.laker.postman.http.runtime.ssl.SSLConfigurationUtil;
import com.laker.postman.request.model.HttpRequestProxyPolicy;
import okhttp3.ConnectionPool;
import okhttp3.CookieJar;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static com.laker.postman.request.util.HttpUrlUtil.extractBaseUri;

public final class ScopedHttpBaseClientProvider implements HttpBaseClientProvider {
    private final Supplier<HttpClientRuntimeConfig> configSupplier;
    private final Supplier<String> cookieScopeSupplier;
    private final Map<ClientKey, OkHttpClient> baseClients = new ConcurrentHashMap<>();
    private final Map<ScopedClientKey, OkHttpClient> scopedClients = new ConcurrentHashMap<>();
    private final CookieJar customCookieJar;
    private final ScopedCookieJarStore scopedCookieJarStore;

    public ScopedHttpBaseClientProvider(Supplier<HttpClientRuntimeConfig> configSupplier) {
        this(configSupplier, (Supplier<String>) null);
    }

    public ScopedHttpBaseClientProvider(Supplier<HttpClientRuntimeConfig> configSupplier,
                                        Supplier<String> cookieScopeSupplier) {
        this.configSupplier = configSupplier == null ? HttpClientRuntimeConfig::defaults : configSupplier;
        this.cookieScopeSupplier = cookieScopeSupplier;
        this.customCookieJar = null;
        this.scopedCookieJarStore = new ScopedCookieJarStore();
    }

    public ScopedHttpBaseClientProvider(Supplier<HttpClientRuntimeConfig> configSupplier, CookieJar cookieJar) {
        this.configSupplier = configSupplier == null ? HttpClientRuntimeConfig::defaults : configSupplier;
        this.cookieScopeSupplier = null;
        this.customCookieJar = cookieJar == null ? CookieJar.NO_COOKIES : cookieJar;
        this.scopedCookieJarStore = null;
    }

    @Override
    public OkHttpClient getBaseClient(PreparedRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        String baseUri = extractBaseUri(request.url);
        boolean followRedirects = request.followRedirects;
        SSLConfigurationUtil.SSLVerificationMode sslMode = HttpClientResolver.DEFAULT.resolveSslVerificationMode(request);
        HttpClientRuntimeConfig config = resolveConfig();
        HttpRequestProxyPolicy proxyPolicy = HttpRequestProxyPolicy.normalize(request.proxyPolicy);
        ClientKey key = new ClientKey(
                baseUri,
                followRedirects,
                sslMode,
                config,
                OkHttpClientManager.runtimeSettingsCacheKey(baseUri, proxyPolicy),
                proxyPolicy
        );
        if (customCookieJar != null) {
            return baseClients.computeIfAbsent(key, this::createCustomCookieClient);
        }
        String cookieScope = ScopedCookieJarStore.normalizeScope(resolveCookieScope());
        return scopedClients.computeIfAbsent(new ScopedClientKey(key, cookieScope), this::createScopedCookieClient);
    }

    public void clear() {
        shutdownClients();
        baseClients.clear();
        scopedClients.clear();
        clearCookies();
    }

    public void clearCookies() {
        if (scopedCookieJarStore != null) {
            scopedCookieJarStore.clear();
        }
    }

    private HttpClientRuntimeConfig resolveConfig() {
        HttpClientRuntimeConfig config = configSupplier.get();
        return config == null ? HttpClientRuntimeConfig.defaults() : config;
    }

    private OkHttpClient createCustomCookieClient(ClientKey key) {
        return OkHttpClientManager.createClientForRuntimeConfig(
                key.baseUri,
                key.followRedirects,
                key.sslMode,
                key.config,
                customCookieJar,
                key.proxyPolicy
        );
    }

    private OkHttpClient createScopedCookieClient(ScopedClientKey scopedKey) {
        OkHttpClient baseClient = baseClients.computeIfAbsent(scopedKey.clientKey, this::createBaseClientWithoutCookies);
        return baseClient.newBuilder()
                .cookieJar(scopedCookieJarStore.cookieJarForScope(scopedKey.cookieScope))
                .build();
    }

    private OkHttpClient createBaseClientWithoutCookies(ClientKey key) {
        return OkHttpClientManager.createClientForRuntimeConfig(
                key.baseUri,
                key.followRedirects,
                key.sslMode,
                key.config,
                CookieJar.NO_COOKIES,
                key.proxyPolicy
        );
    }

    private void shutdownClients() {
        Set<Dispatcher> dispatchers = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<ConnectionPool> connectionPools = Collections.newSetFromMap(new IdentityHashMap<>());
        shutdownClients(baseClients.values(), dispatchers, connectionPools);
        shutdownClients(scopedClients.values(), dispatchers, connectionPools);
    }

    private void shutdownClients(Iterable<OkHttpClient> clients,
                                 Set<Dispatcher> dispatchers,
                                 Set<ConnectionPool> connectionPools) {
        for (OkHttpClient client : clients) {
            Dispatcher dispatcher = client.dispatcher();
            if (dispatchers.add(dispatcher)) {
                dispatcher.cancelAll();
                dispatcher.executorService().shutdown();
            }
            ConnectionPool connectionPool = client.connectionPool();
            if (connectionPools.add(connectionPool)) {
                connectionPool.evictAll();
            }
        }
    }

    private String resolveCookieScope() {
        return cookieScopeSupplier == null ? null : cookieScopeSupplier.get();
    }

    private record ClientKey(String baseUri,
                             boolean followRedirects,
                             SSLConfigurationUtil.SSLVerificationMode sslMode,
                             HttpClientRuntimeConfig config,
                             String runtimeSettingsCacheKey,
                             HttpRequestProxyPolicy proxyPolicy) {
    }

    private record ScopedClientKey(ClientKey clientKey, String cookieScope) {
    }
}
