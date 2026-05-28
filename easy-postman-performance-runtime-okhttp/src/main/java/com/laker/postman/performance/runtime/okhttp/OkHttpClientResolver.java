package com.laker.postman.performance.runtime.okhttp;

import com.laker.postman.performance.core.request.PerformanceOutboundRequest;
import okhttp3.CookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class OkHttpClientResolver {
    private static final String HTTP_VERSION_HTTP_1_1 = "HTTP_1_1";
    private static final String HTTP_VERSION_HTTP_2 = "HTTP_2";

    private final OkHttpClient baseClient;

    OkHttpClientResolver(OkHttpClient baseClient) {
        this.baseClient = baseClient == null ? new OkHttpClient() : baseClient;
    }

    OkHttpClient clientFor(PerformanceOutboundRequest request) {
        Integer timeoutMs = request.getRequestTimeoutMs();
        boolean hasTimeout = timeoutMs != null && timeoutMs > 0;
        boolean hasFollowRedirectsOverride = request.getFollowRedirects() != null;
        boolean disableCookies = Boolean.FALSE.equals(request.getCookieJarEnabled());
        boolean hasProtocolOverride = isHttpVersionOverride(request.getHttpVersion());
        if (!hasTimeout && !hasFollowRedirectsOverride && !disableCookies && !hasProtocolOverride) {
            return baseClient;
        }
        OkHttpClient.Builder builder = baseClient.newBuilder();
        if (hasTimeout) {
            Duration timeout = Duration.ofMillis(timeoutMs);
            builder.connectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .writeTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .callTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        if (hasFollowRedirectsOverride) {
            boolean followRedirects = !Boolean.FALSE.equals(request.getFollowRedirects());
            builder.followRedirects(followRedirects)
                    .followSslRedirects(followRedirects);
        }
        if (disableCookies) {
            builder.cookieJar(CookieJar.NO_COOKIES);
        }
        if (HTTP_VERSION_HTTP_1_1.equals(request.getHttpVersion())) {
            builder.protocols(List.of(Protocol.HTTP_1_1));
        } else if (HTTP_VERSION_HTTP_2.equals(request.getHttpVersion())) {
            builder.protocols(List.of(Protocol.HTTP_2, Protocol.HTTP_1_1));
        }
        return builder.build();
    }

    private static boolean isHttpVersionOverride(String httpVersion) {
        return HTTP_VERSION_HTTP_1_1.equals(httpVersion) || HTTP_VERSION_HTTP_2.equals(httpVersion);
    }
}
