package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.http.okhttp.HttpClientRuntimeConfig;
import okhttp3.OkHttpClient;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
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

    private static PreparedRequest preparedRequest(String url) {
        PreparedRequest request = new PreparedRequest();
        request.url = url;
        request.followRedirects = true;
        request.sslVerificationEnabled = true;
        return request;
    }
}
