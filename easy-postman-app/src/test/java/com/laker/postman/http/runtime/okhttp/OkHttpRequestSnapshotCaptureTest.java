package com.laker.postman.http.runtime.okhttp;

import com.laker.postman.http.runtime.model.PreparedRequest;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSink;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class OkHttpRequestSnapshotCaptureTest {

    @Test
    public void shouldStopCapturingRequestBodyAfterPreviewLimit() {
        PreparedRequest preparedRequest = new PreparedRequest();
        AtomicInteger chunksWritten = new AtomicInteger();
        RequestBody body = new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.get("text/plain; charset=utf-8");
            }

            @Override
            public long contentLength() {
                return 1024L * 1024L;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                for (int i = 0; i < 1024; i++) {
                    chunksWritten.incrementAndGet();
                    sink.writeUtf8("x".repeat(1024));
                }
            }
        };
        Request request = new Request.Builder()
                .url("http://example.test/upload")
                .post(body)
                .build();

        OkHttpRequestSnapshotCapture.capture(preparedRequest, request, true);

        assertNotNull(preparedRequest.sentRequestBody);
        assertTrue(chunksWritten.get() < 1024, "Snapshot capture should stop before traversing the full body");
        assertTrue(preparedRequest.sentRequestBody.contains("Truncated request body"));
        assertTrue(preparedRequest.sentRequestBody.contains("1048576 bytes"));
    }
}
