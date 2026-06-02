package com.laker.postman.http.runtime.okhttp;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.HttpHeader;
import lombok.experimental.UtilityClass;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Sink;
import okio.Timeout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class OkHttpRequestSnapshotCapture {
    private static final long MAX_CAPTURE_BODY_BYTES = 2 * 1024;

    public static void capture(PreparedRequest preparedRequest, Request request, boolean captureBody) {
        if (preparedRequest == null || request == null) {
            return;
        }
        preparedRequest.sentHeadersList = toHttpHeaders(request);
        if (captureBody) {
            preparedRequest.sentRequestBody = snapshotBody(request.body());
        }
    }

    private static List<HttpHeader> toHttpHeaders(Request request) {
        Headers headers = request.headers();
        List<HttpHeader> result = new ArrayList<>(headers != null ? headers.size() + 1 : 1);
        if (headers != null) {
            for (int i = 0; i < headers.size(); i++) {
                result.add(new HttpHeader(true, headers.name(i), headers.value(i)));
            }
        }
        if (findHeaderValue(result, "Host") == null) {
            result.add(new HttpHeader(true, "Host", hostHeader(request)));
        }
        return result;
    }

    private static String findHeaderValue(List<HttpHeader> headers, String key) {
        for (HttpHeader header : headers) {
            if (header.getKey() != null && header.getKey().equalsIgnoreCase(key)) {
                return header.getValue();
            }
        }
        return null;
    }

    private static String hostHeader(Request request) {
        String host = request.url().host();
        int port = request.url().port();
        if (host.indexOf(':') != -1 && !host.startsWith("[") && !host.endsWith("]")) {
            host = "[" + host + "]";
        }
        int defaultPort = "https".equals(request.url().scheme()) ? 443 : 80;
        return port == defaultPort ? host : host + ":" + port;
    }

    private static String snapshotBody(RequestBody body) {
        if (body == null) {
            return null;
        }
        MediaType contentType = body.contentType();
        String description = bodyDescription(contentType);
        if (description != null) {
            return description;
        }
        // one-shot/duplex 请求体无法安全重复读取，避免日志预览破坏真实发送。
        if (body.isDuplex()) {
            return "[duplex request body omitted]";
        }
        if (body.isOneShot()) {
            return "[one-shot request body omitted]";
        }
        long contentLength = safeContentLength(body);
        try {
            TruncatingBodySink sink = new TruncatingBodySink(MAX_CAPTURE_BODY_BYTES, contentLength);
            BufferedSink bufferedSink = Okio.buffer(sink);
            try {
                body.writeTo(bufferedSink);
                bufferedSink.flush();
            } catch (BodyPreviewLimitReachedException ignored) {
                // 只预览前 2KB，避免大请求体为了日志被完整遍历。
            }
            return sink.snapshot();
        } catch (Exception e) {
            return "[读取请求体失败: " + e.getMessage() + "]";
        }
    }

    private static long safeContentLength(RequestBody body) {
        try {
            return body.contentLength();
        } catch (Exception ignored) {
            return -1L;
        }
    }

    private static String bodyDescription(MediaType contentType) {
        if (contentType == null) {
            return null;
        }
        String type = contentType.type().toLowerCase();
        String subtype = contentType.subtype().toLowerCase();
        if ("multipart".equals(type)) {
            return "[multipart/form-data] (see form files)";
        }
        if ("application".equals(type) && (subtype.contains("octet-stream") || subtype.contains("binary"))) {
            return "[binary/octet-stream]";
        }
        if ("image".equals(type) || "audio".equals(type) || "video".equals(type)) {
            return "[" + type + "/" + subtype + "]";
        }
        return null;
    }

    private static final class TruncatingBodySink implements Sink {
        private final Buffer prefix = new Buffer();
        private final long limitBytes;
        private final long declaredLength;
        private long totalBytes;
        private long remainingBytes;

        private TruncatingBodySink(long limitBytes, long declaredLength) {
            this.limitBytes = Math.max(0, limitBytes);
            this.declaredLength = declaredLength;
            this.remainingBytes = this.limitBytes;
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            totalBytes += byteCount;
            long bytesToKeep = Math.min(byteCount, remainingBytes);
            if (bytesToKeep > 0) {
                prefix.write(source, bytesToKeep);
                remainingBytes -= bytesToKeep;
            }
            long bytesToSkip = byteCount - bytesToKeep;
            if (bytesToSkip > 0) {
                source.skip(bytesToSkip);
            }
            if (remainingBytes == 0 && isKnownOrPotentiallyTruncated()) {
                throw new BodyPreviewLimitReachedException();
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public Timeout timeout() {
            return Timeout.NONE;
        }

        @Override
        public void close() {
        }

        private String snapshot() {
            String text = prefix.readUtf8();
            if (!isKnownOrPotentiallyTruncated() && totalBytes <= limitBytes) {
                return text;
            }
            return text + "\n\n[Truncated request body: " + describeTotalBytes()
                    + " bytes, showing first " + (limitBytes / 1024) + "KB]";
        }

        private boolean isKnownOrPotentiallyTruncated() {
            return declaredLength > limitBytes || (declaredLength < 0 && totalBytes >= limitBytes);
        }

        private String describeTotalBytes() {
            if (declaredLength >= 0) {
                return String.valueOf(declaredLength);
            }
            return "at least " + totalBytes;
        }
    }

    private static final class BodyPreviewLimitReachedException extends IOException {
    }
}
