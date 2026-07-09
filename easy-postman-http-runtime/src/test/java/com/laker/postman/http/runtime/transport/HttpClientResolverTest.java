package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.config.HttpRuntimeSettings;
import com.laker.postman.http.runtime.config.HttpRuntimeSettingsProvider;
import com.laker.postman.http.runtime.model.HttpCaptureProfile;
import com.laker.postman.http.runtime.model.HttpCaptureProfiles;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.okhttp.OkHttpClientManager;
import com.laker.postman.request.model.HttpRequestProxyPolicy;
import okhttp3.OkHttpClient;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class HttpClientResolverTest {

    @Test
    public void requestTimeoutShouldApplyToWholeCallTimeout() {
        PreparedRequest request = new PreparedRequest();
        request.url = "https://api.example.com/data";
        request.requestTimeoutMs = 1000;
        HttpClientResolver resolver = new HttpClientResolver();

        OkHttpClient client = resolver.resolveClient(request, ignored -> new OkHttpClient());

        assertEquals(client.connectTimeoutMillis(), 1000);
        assertEquals(client.readTimeoutMillis(), 1000);
        assertEquals(client.writeTimeoutMillis(), 1000);
        assertEquals(client.callTimeoutMillis(), 1000);
    }

    @Test
    public void websocketPingIntervalShouldApplyToResolvedClient() {
        PreparedRequest request = new PreparedRequest();
        request.url = "wss://api.example.com/socket";
        request.webSocketPingIntervalMs = 15000;
        HttpClientResolver resolver = new HttpClientResolver();

        OkHttpClient client = resolver.resolveClient(request, ignored -> new OkHttpClient());

        assertEquals(client.pingIntervalMillis(), 15000);
    }

    @Test
    public void websocketPingIntervalCanBeDisabled() {
        PreparedRequest request = new PreparedRequest();
        request.url = "wss://api.example.com/socket";
        request.webSocketPingIntervalMs = 0;
        HttpClientResolver resolver = new HttpClientResolver();

        OkHttpClient client = resolver.resolveClient(request, ignored -> new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .build());

        assertEquals(client.pingIntervalMillis(), 0);
    }

    @Test
    public void requestNoProxyPolicyShouldForceDirectClientWhenGlobalProxyEnabled() {
        try {
            HttpRuntimeSettingsProvider.set(manualProxySettings(true));
            PreparedRequest request = requestWithProxyPolicy(HttpRequestProxyPolicy.NO_PROXY);

            OkHttpClient client = new HttpClientResolver().resolveDefaultBaseClient(request);

            assertEquals(client.proxy(), Proxy.NO_PROXY);
        } finally {
            HttpRuntimeSettingsProvider.reset();
            OkHttpClientManager.clearClientCache();
        }
    }

    @Test
    public void functionalDiagnosticShouldInstallRequestSnapshotInterceptorWithoutNetworkLog() {
        PreparedRequest request = requestWithProxyPolicy(HttpRequestProxyPolicy.DEFAULT);
        HttpCaptureProfiles.apply(request, HttpCaptureProfile.FUNCTIONAL_DIAGNOSTIC);

        OkHttpClient client = new HttpClientResolver().resolveClient(request, ignored -> new OkHttpClient());

        assertTrue(hasNetworkInterceptor(client, RequestSnapshotNetworkInterceptor.class));
        assertFalse(HttpCaptureProfiles.resolve(request).emitNetworkLog());
    }

    @Test
    public void performanceMetricsShouldNotInstallRequestSnapshotInterceptor() {
        PreparedRequest request = requestWithProxyPolicy(HttpRequestProxyPolicy.DEFAULT);
        HttpCaptureProfiles.apply(request, HttpCaptureProfile.PERFORMANCE_METRICS);

        OkHttpClient client = new HttpClientResolver().resolveClient(request, ignored -> new OkHttpClient());

        assertFalse(hasNetworkInterceptor(client, RequestSnapshotNetworkInterceptor.class));
    }

    @Test
    public void requestUseProxyPolicyShouldUseConfiguredProxyWhenGlobalProxyDisabled() {
        try {
            HttpRuntimeSettingsProvider.set(manualProxySettings(false));
            PreparedRequest request = requestWithProxyPolicy(HttpRequestProxyPolicy.USE_PROXY);

            OkHttpClient client = new HttpClientResolver().resolveDefaultBaseClient(request);

            assertEquals(client.proxy().type(), Proxy.Type.HTTP);
        } finally {
            HttpRuntimeSettingsProvider.reset();
            OkHttpClientManager.clearClientCache();
        }
    }

    @Test
    public void requestUseProxyPolicyShouldForceDirectClientWhenManualProxyIsIncomplete() {
        ProxySelector originalSelector = ProxySelector.getDefault();

        try {
            ProxySelector.setDefault(fakeSystemProxySelector());
            HttpRuntimeSettingsProvider.set(manualProxySettings(false, ""));
            PreparedRequest request = requestWithProxyPolicy(HttpRequestProxyPolicy.USE_PROXY);

            OkHttpClient client = new HttpClientResolver().resolveDefaultBaseClient(request);

            assertEquals(client.proxy(), Proxy.NO_PROXY);
        } finally {
            ProxySelector.setDefault(originalSelector);
            HttpRuntimeSettingsProvider.reset();
            OkHttpClientManager.clearClientCache();
        }
    }

    @Test
    public void requestUseProxyPolicyShouldForceDirectClientWhenManualProxyPortIsInvalid() {
        ProxySelector originalSelector = ProxySelector.getDefault();

        try {
            ProxySelector.setDefault(fakeSystemProxySelector());
            HttpRuntimeSettingsProvider.set(manualProxySettings(false, "127.0.0.1", -1));
            PreparedRequest request = requestWithProxyPolicy(HttpRequestProxyPolicy.USE_PROXY);

            OkHttpClient client = new HttpClientResolver().resolveDefaultBaseClient(request);

            assertEquals(client.proxy(), Proxy.NO_PROXY);
        } finally {
            ProxySelector.setDefault(originalSelector);
            HttpRuntimeSettingsProvider.reset();
            OkHttpClientManager.clearClientCache();
        }
    }

    @Test
    public void proxyConfigurationFailureShouldForceDirectClientThroughDefaultResolver() {
        ProxySelector originalSelector = ProxySelector.getDefault();

        try {
            ProxySelector.setDefault(fakeSystemProxySelector());
            HttpRuntimeSettingsProvider.set(new HttpRuntimeSettings() {
                @Override
                public boolean isProxyEnabled() {
                    return false;
                }

                @Override
                public String getProxyMode() {
                    return PROXY_MODE_MANUAL;
                }

                @Override
                public String getProxyHost() {
                    return "127.0.0.1";
                }

                @Override
                public int getProxyPort() {
                    throw new IllegalStateException("broken proxy port");
                }
            });

            PreparedRequest request = requestWithProxyPolicy(HttpRequestProxyPolicy.USE_PROXY);
            OkHttpClient client = new HttpClientResolver().resolveDefaultBaseClient(request);

            assertEquals(client.proxy(), Proxy.NO_PROXY);
        } finally {
            ProxySelector.setDefault(originalSelector);
            HttpRuntimeSettingsProvider.reset();
            OkHttpClientManager.clearClientCache();
        }
    }

    private static PreparedRequest requestWithProxyPolicy(HttpRequestProxyPolicy proxyPolicy) {
        PreparedRequest request = new PreparedRequest();
        request.url = "https://api.example.com/data";
        request.proxyPolicy = proxyPolicy;
        request.sslVerificationEnabled = true;
        return request;
    }

    private static HttpRuntimeSettings manualProxySettings(boolean proxyEnabled) {
        return manualProxySettings(proxyEnabled, "127.0.0.1");
    }

    private static HttpRuntimeSettings manualProxySettings(boolean proxyEnabled, String proxyHost) {
        return manualProxySettings(proxyEnabled, proxyHost, 8080);
    }

    private static HttpRuntimeSettings manualProxySettings(boolean proxyEnabled, String proxyHost, int proxyPort) {
        return new HttpRuntimeSettings() {
            @Override
            public boolean isProxyEnabled() {
                return proxyEnabled;
            }

            @Override
            public String getProxyMode() {
                return PROXY_MODE_MANUAL;
            }

            @Override
            public String getProxyType() {
                return PROXY_TYPE_HTTP;
            }

            @Override
            public String getProxyHost() {
                return proxyHost;
            }

            @Override
            public int getProxyPort() {
                return proxyPort;
            }
        };
    }

    private static ProxySelector fakeSystemProxySelector() {
        return new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                return List.of(new Proxy(
                        Proxy.Type.HTTP,
                        InetSocketAddress.createUnresolved("127.0.0.1", 8888)
                ));
            }

            @Override
            public void connectFailed(URI uri, java.net.SocketAddress sa, java.io.IOException ioe) {
            }
        };
    }

    private static boolean hasNetworkInterceptor(OkHttpClient client, Class<?> interceptorType) {
        return client.networkInterceptors().stream().anyMatch(interceptorType::isInstance);
    }
}
