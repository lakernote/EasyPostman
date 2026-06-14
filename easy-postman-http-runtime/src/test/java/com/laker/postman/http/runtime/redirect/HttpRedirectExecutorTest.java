package com.laker.postman.http.runtime.redirect;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.transport.HttpCallTracker;
import com.laker.postman.http.runtime.transport.HttpExchangeOptions;
import com.laker.postman.http.runtime.transport.HttpTransport;
import com.laker.postman.http.runtime.transport.RealtimeConnectionHandle;
import com.laker.postman.http.runtime.transport.RealtimeConnectionOptions;
import com.laker.postman.http.runtime.transport.RealtimeWebSocketConnection;
import okhttp3.Call;
import okhttp3.WebSocketListener;
import okhttp3.sse.EventSourceListener;
import org.testng.annotations.Test;

import static org.testng.Assert.assertSame;

public class HttpRedirectExecutorTest {

    @Test
    public void shouldPassCallTrackerToUnderlyingHttpTransport() throws Exception {
        CapturingTransport transport = new CapturingTransport();
        HttpRedirectExecutor executor = new HttpRedirectExecutor(transport);
        PreparedRequest request = new PreparedRequest();
        request.method = "GET";
        request.url = "https://example.test/no-redirect";
        HttpCallTracker tracker = new HttpCallTracker() {
            @Override
            public void onCallStarted(Call call) {
            }
        };

        executor.executeWithRedirects(request, 0, null, tracker);

        assertSame(transport.options.resolvedCallTracker(), tracker);
    }

    private static final class CapturingTransport implements HttpTransport {
        private HttpExchangeOptions options;

        @Override
        public HttpResponse execute(PreparedRequest request, HttpExchangeOptions options) {
            this.options = options;
            HttpResponse response = new HttpResponse();
            response.code = 200;
            response.body = "ok";
            return response;
        }

        @Override
        public RealtimeConnectionHandle openSse(PreparedRequest request,
                                                EventSourceListener listener,
                                                RealtimeConnectionOptions options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RealtimeWebSocketConnection openWebSocket(PreparedRequest request,
                                                        WebSocketListener listener,
                                                        RealtimeConnectionOptions options) {
            throw new UnsupportedOperationException();
        }
    }
}
