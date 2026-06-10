package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.config.HttpRuntimeSettings;
import com.laker.postman.http.runtime.config.HttpRuntimeSettingsProvider;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.okhttp.OkHttpClientManager;
import com.laker.postman.request.model.HttpRequestProxyPolicy;
import okhttp3.OkHttpClient;
import org.testng.annotations.Test;

import java.net.Proxy;

import static org.testng.Assert.assertEquals;

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

    private static PreparedRequest requestWithProxyPolicy(HttpRequestProxyPolicy proxyPolicy) {
        PreparedRequest request = new PreparedRequest();
        request.url = "https://api.example.com/data";
        request.proxyPolicy = proxyPolicy;
        request.sslVerificationEnabled = true;
        return request;
    }

    private static HttpRuntimeSettings manualProxySettings(boolean proxyEnabled) {
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
                return "127.0.0.1";
            }

            @Override
            public int getProxyPort() {
                return 8080;
            }
        };
    }
}
