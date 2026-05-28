package com.laker.postman.service.http;

import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.http.okhttp.HttpClientRuntimeConfig;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.service.http.ssl.SSLConfigurationUtil;
import okhttp3.CookieJar;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static com.laker.postman.service.http.HttpRequestUtil.extractBaseUri;

public final class ScopedHttpBaseClientProvider implements HttpBaseClientProvider {
    private final Supplier<HttpClientRuntimeConfig> configSupplier;
    private final Map<String, OkHttpClient> clients = new ConcurrentHashMap<>();
    private final CookieJar cookieJar;
    private final CookieManager cookieManager;

    public ScopedHttpBaseClientProvider(Supplier<HttpClientRuntimeConfig> configSupplier) {
        this(configSupplier, new CookieManager(null, CookiePolicy.ACCEPT_ALL));
    }

    private ScopedHttpBaseClientProvider(Supplier<HttpClientRuntimeConfig> configSupplier, CookieManager cookieManager) {
        this(configSupplier, new JavaNetCookieJar(cookieManager), cookieManager);
    }

    public ScopedHttpBaseClientProvider(Supplier<HttpClientRuntimeConfig> configSupplier, CookieJar cookieJar) {
        this(configSupplier, cookieJar, null);
    }

    private ScopedHttpBaseClientProvider(Supplier<HttpClientRuntimeConfig> configSupplier,
                                         CookieJar cookieJar,
                                         CookieManager cookieManager) {
        this.configSupplier = configSupplier == null ? HttpClientRuntimeConfig::defaults : configSupplier;
        this.cookieJar = cookieJar;
        this.cookieManager = cookieManager;
    }

    @Override
    public OkHttpClient getBaseClient(PreparedRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        String baseUri = extractBaseUri(request.url);
        boolean followRedirects = request.followRedirects;
        SSLConfigurationUtil.SSLVerificationMode sslMode = HttpService.resolveSslVerificationMode(request);
        HttpClientRuntimeConfig config = resolveConfig();
        String key = cacheKey(
                baseUri,
                followRedirects,
                sslMode,
                config,
                OkHttpClientManager.runtimeSettingsCacheKey(baseUri)
        );
        return clients.computeIfAbsent(key, ignored ->
                OkHttpClientManager.createClientForRuntimeConfig(baseUri, followRedirects, sslMode, config, cookieJar)
        );
    }

    public void clear() {
        for (OkHttpClient client : clients.values()) {
            client.dispatcher().cancelAll();
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
        clients.clear();
        clearCookies();
    }

    public void clearCookies() {
        if (cookieManager != null) {
            cookieManager.getCookieStore().removeAll();
        }
    }

    private HttpClientRuntimeConfig resolveConfig() {
        HttpClientRuntimeConfig config = configSupplier.get();
        return config == null ? HttpClientRuntimeConfig.defaults() : config;
    }

    private static String cacheKey(String baseUri,
                                   boolean followRedirects,
                                   SSLConfigurationUtil.SSLVerificationMode sslMode,
                                   HttpClientRuntimeConfig config,
                                   String runtimeSettingsCacheKey) {
        return baseUri + "|" + followRedirects + "|" + sslMode + "|"
                + config.maxIdleConnections() + "|"
                + config.keepAliveDurationSeconds() + "|"
                + config.maxRequests() + "|"
                + config.maxRequestsPerHost() + "|"
                + runtimeSettingsCacheKey;
    }
}
