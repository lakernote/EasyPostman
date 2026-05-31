package com.laker.postman.performance.execution;

import com.laker.postman.model.PreparedRequest;
import com.laker.postman.http.runtime.okhttp.HttpClientRuntimeConfig;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;

public class DefaultPerformanceNetworkRuntimeTest {

    @Test
    public void shouldCreateRunScopedHttpClientsWithConfiguredDispatcherLimits() {
        DefaultPerformanceNetworkRuntime runtime = new DefaultPerformanceNetworkRuntime(
                () -> new HttpClientRuntimeConfig(7, 11, 13, 17)
        );

        OkHttpClient client = runtime.getBaseClient(preparedRequest("http://example.test/api"));

        assertEquals(client.dispatcher().getMaxRequests(), 13);
        assertEquals(client.dispatcher().getMaxRequestsPerHost(), 17);
    }

    @Test
    public void shouldReuseRunScopedBaseClientForSameRequestConfig() {
        DefaultPerformanceNetworkRuntime runtime = new DefaultPerformanceNetworkRuntime(
                () -> new HttpClientRuntimeConfig(7, 11, 13, 17)
        );
        PreparedRequest request = preparedRequest("http://example.test/api");

        assertSame(runtime.getBaseClient(request), runtime.getBaseClient(request));
    }

    @Test
    public void shouldUseIsolatedCookieJarPerRuntime() {
        DefaultPerformanceNetworkRuntime firstRuntime = new DefaultPerformanceNetworkRuntime(
                () -> new HttpClientRuntimeConfig(7, 11, 13, 17)
        );
        DefaultPerformanceNetworkRuntime secondRuntime = new DefaultPerformanceNetworkRuntime(
                () -> new HttpClientRuntimeConfig(7, 11, 13, 17)
        );
        PreparedRequest request = preparedRequest("http://example.test/api");

        OkHttpClient firstClient = firstRuntime.getBaseClient(request);
        OkHttpClient secondClient = secondRuntime.getBaseClient(request);

        assertNotSame(firstClient.cookieJar(), secondClient.cookieJar());
    }

    @Test
    public void shouldClearCookiesAtRunBoundary() {
        DefaultPerformanceNetworkRuntime runtime = new DefaultPerformanceNetworkRuntime(
                () -> new HttpClientRuntimeConfig(7, 11, 13, 17)
        );
        PreparedRequest request = preparedRequest("http://example.test/api");
        OkHttpClient client = runtime.getBaseClient(request);
        HttpUrl url = HttpUrl.get("http://example.test/api");
        Cookie cookie = new Cookie.Builder()
                .domain("example.test")
                .path("/")
                .name("sid")
                .value("one")
                .build();

        client.cookieJar().saveFromResponse(url, List.of(cookie));
        assertEquals(client.cookieJar().loadForRequest(url).size(), 1);

        runtime.endRun();

        assertTrue(client.cookieJar().loadForRequest(url).isEmpty());
    }

    @Test
    public void shouldSnapshotHttpClientConfigForRun() {
        AtomicReference<HttpClientRuntimeConfig> configRef = new AtomicReference<>(
                new HttpClientRuntimeConfig(7, 11, 13, 17)
        );
        DefaultPerformanceNetworkRuntime runtime = new DefaultPerformanceNetworkRuntime(configRef::get);
        runtime.beginRun();

        OkHttpClient firstRunClient = runtime.getBaseClient(preparedRequest("http://first.example.test/api"));
        configRef.set(new HttpClientRuntimeConfig(19, 23, 29, 31));
        OkHttpClient sameRunClient = runtime.getBaseClient(preparedRequest("http://second.example.test/api"));

        assertEquals(firstRunClient.dispatcher().getMaxRequests(), 13);
        assertEquals(sameRunClient.dispatcher().getMaxRequests(), 13);

        runtime.endRun();

        OkHttpClient nextRunClient = runtime.getBaseClient(preparedRequest("http://third.example.test/api"));
        assertEquals(nextRunClient.dispatcher().getMaxRequests(), 29);
    }

    private static PreparedRequest preparedRequest(String url) {
        PreparedRequest request = new PreparedRequest();
        request.url = url;
        request.followRedirects = true;
        request.sslVerificationEnabled = true;
        return request;
    }
}
