package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.PreparedRequest;
import okhttp3.OkHttpClient;
import org.testng.annotations.Test;

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
}
