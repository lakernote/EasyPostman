package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.config.HttpRequestRuntimeSettingsResolver;
import com.laker.postman.http.runtime.config.HttpRuntimeSettingsProvider;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.okhttp.DigestAuthenticator;
import com.laker.postman.http.runtime.okhttp.OkHttpClientManager;
import com.laker.postman.http.runtime.okhttp.OkHttpExchangeEventListener;
import com.laker.postman.http.runtime.ssl.SSLConfigurationUtil;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.TransportAuth;
import okhttp3.CookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.laker.postman.request.util.HttpUrlUtil.extractBaseUri;

public final class HttpClientResolver {
    static final HttpClientResolver DEFAULT = new HttpClientResolver();

    public OkHttpClient resolveClient(PreparedRequest request, HttpBaseClientProvider baseClientProvider) {
        OkHttpClient baseClient = baseClientProvider == null
                ? resolveDefaultBaseClient(request)
                : baseClientProvider.getBaseClient(request);
        return buildDynamicClient(baseClient, request, request.requestTimeoutMs);
    }

    OkHttpClient resolveDefaultBaseClient(PreparedRequest request) {
        String baseUri = extractBaseUri(request.url);
        boolean isolateSslConfiguration = shouldIsolateConnectionPool(request);
        return isolateSslConfiguration
                ? OkHttpClientManager.createClientForSslMode(
                        baseUri,
                        request.followRedirects,
                        resolveSslVerificationMode(request)
                )
                : OkHttpClientManager.getClient(baseUri, request.followRedirects);
    }

    boolean shouldIsolateConnectionPool(PreparedRequest preparedRequest) {
        if (preparedRequest == null) {
            return false;
        }

        URI uri;
        try {
            uri = URI.create(preparedRequest.url);
        } catch (Exception e) {
            return false;
        }

        String scheme = uri.getScheme();
        boolean secureScheme = "https".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme);
        if (!secureScheme) {
            return false;
        }

        return resolveSslVerificationMode(preparedRequest) != resolveGlobalSslVerificationMode(preparedRequest.url);
    }

    SSLConfigurationUtil.SSLVerificationMode resolveSslVerificationMode(PreparedRequest preparedRequest) {
        if (preparedRequest == null) {
            return resolveGlobalSslVerificationMode();
        }

        boolean proxySslDisabled = HttpRequestRuntimeSettingsResolver
                .isProxySslVerificationForcedDisabled(preparedRequest.url);
        return (!preparedRequest.sslVerificationEnabled || proxySslDisabled)
                ? SSLConfigurationUtil.SSLVerificationMode.LENIENT
                : SSLConfigurationUtil.SSLVerificationMode.STRICT;
    }

    int resolveSecurePort(String scheme, int port) {
        if (port != -1) {
            return port;
        }
        return ("https".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme)) ? 443 : 80;
    }

    private OkHttpClient buildDynamicClient(OkHttpClient baseClient,
                                            PreparedRequest preparedRequest,
                                            int timeoutMs) {
        OkHttpClient.Builder builder = baseClient.newBuilder();
        boolean needEventListener = preparedRequest.collectBasicInfo
                || preparedRequest.collectMetricsInfo
                || preparedRequest.collectEventInfo
                || preparedRequest.enableNetworkLog;
        if (CookieHeaderMergeNetworkInterceptor.hasEnabledExplicitCookieHeader(preparedRequest)) {
            builder.addNetworkInterceptor(new CookieHeaderMergeNetworkInterceptor(preparedRequest));
        }
        if (preparedRequest.enableNetworkLog) {
            builder.addNetworkInterceptor(new RequestSnapshotNetworkInterceptor(preparedRequest));
        }
        builder.addNetworkInterceptor(new CompressionDecompressNetworkInterceptor());

        applyRequestSettings(builder, preparedRequest);

        if (needEventListener) {
            builder.eventListenerFactory(call -> new OkHttpExchangeEventListener(preparedRequest));
        }

        if (timeoutMs > 0) {
            builder.connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .callTimeout(timeoutMs * 3L, TimeUnit.MILLISECONDS);
        }
        return builder.build();
    }

    private void applyRequestSettings(OkHttpClient.Builder builder,
                                      PreparedRequest preparedRequest) {
        if (!preparedRequest.cookieJarEnabled) {
            builder.cookieJar(CookieJar.NO_COOKIES);
        }

        applyDigestAuthenticator(builder, preparedRequest);

        String httpVersion = preparedRequest.httpVersion != null
                ? preparedRequest.httpVersion
                : HttpRequestItem.HTTP_VERSION_AUTO;
        if (HttpRequestItem.HTTP_VERSION_HTTP_1_1.equals(httpVersion)) {
            builder.protocols(List.of(Protocol.HTTP_1_1));
        } else if (HttpRequestItem.HTTP_VERSION_HTTP_2.equals(httpVersion)) {
            builder.protocols(List.of(Protocol.HTTP_2, Protocol.HTTP_1_1));
        }
    }

    private void applyDigestAuthenticator(OkHttpClient.Builder builder,
                                          PreparedRequest preparedRequest) {
        TransportAuth auth = preparedRequest != null ? preparedRequest.transportAuth : null;
        if (auth == null || !auth.isDigest()) {
            return;
        }
        if (isBlank(auth.username) || containsUnresolvedPlaceholder(auth.username)
                || containsUnresolvedPlaceholder(auth.password)) {
            return;
        }
        builder.authenticator(new DigestAuthenticator(
                auth.username,
                auth.password == null ? "" : auth.password
        ));
    }

    private SSLConfigurationUtil.SSLVerificationMode resolveGlobalSslVerificationMode() {
        return resolveGlobalSslVerificationMode(null);
    }

    private SSLConfigurationUtil.SSLVerificationMode resolveGlobalSslVerificationMode(String url) {
        boolean proxySslDisabled = false;
        if (url != null && !url.isBlank()) {
            proxySslDisabled = HttpRequestRuntimeSettingsResolver.isProxySslVerificationForcedDisabled(url);
        }
        return (HttpRuntimeSettingsProvider.get().isRequestSslVerificationDisabled() || proxySslDisabled)
                ? SSLConfigurationUtil.SSLVerificationMode.LENIENT
                : SSLConfigurationUtil.SSLVerificationMode.STRICT;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean containsUnresolvedPlaceholder(String value) {
        return value != null && value.contains("{{") && value.contains("}}");
    }
}
