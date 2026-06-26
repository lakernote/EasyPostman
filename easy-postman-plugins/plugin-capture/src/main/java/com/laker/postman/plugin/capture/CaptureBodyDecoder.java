package com.laker.postman.plugin.capture;

import com.laker.postman.util.HttpHeaderConstants;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

@UtilityClass
class CaptureBodyDecoder {

    static DecodedBody decodeForPreview(byte[] body, Map<String, String> headers, int limit) {
        if (body == null || body.length == 0) {
            return DecodedBody.raw(new byte[0]);
        }
        String encoding = supportedEncoding(headers);
        if (encoding.isBlank()) {
            return DecodedBody.raw(body);
        }
        try {
            byte[] decoded = decode(body, encoding, limit);
            return DecodedBody.decoded(decoded, encoding);
        } catch (IOException | IllegalArgumentException ignored) {
            return DecodedBody.decodeFailed(body, encoding);
        }
    }

    private static byte[] decode(byte[] body, String encoding, int limit) throws IOException {
        if ("gzip".equals(encoding) || "x-gzip".equals(encoding)) {
            try (InputStream input = new GZIPInputStream(new ByteArrayInputStream(body))) {
                return readLimited(input, limit);
            }
        }
        if ("deflate".equals(encoding)) {
            return decodeDeflate(body, limit);
        }
        return body;
    }

    private static byte[] decodeDeflate(byte[] body, int limit) throws IOException {
        try {
            return inflate(body, limit, false);
        } catch (IOException zlibFailed) {
            return inflate(body, limit, true);
        }
    }

    private static byte[] inflate(byte[] body, int limit, boolean nowrap) throws IOException {
        try (InputStream input = new InflaterInputStream(new ByteArrayInputStream(body), new Inflater(nowrap))) {
            return readLimited(input, limit);
        }
    }

    private static byte[] readLimited(InputStream input, int limit) throws IOException {
        int effectiveLimit = Math.max(0, limit);
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(effectiveLimit, 8192));
        byte[] buffer = new byte[8192];
        while (output.size() < effectiveLimit) {
            int read;
            try {
                read = input.read(buffer);
            } catch (EOFException e) {
                if (output.size() > 0) {
                    break;
                }
                throw e;
            }
            if (read < 0) {
                break;
            }
            int writeLength = Math.min(read, effectiveLimit - output.size());
            output.write(buffer, 0, writeLength);
        }
        return output.toByteArray();
    }

    private static String supportedEncoding(Map<String, String> headers) {
        String encodingHeader = headerValueIgnoreCase(headers, HttpHeaderConstants.CONTENT_ENCODING);
        if (encodingHeader.isBlank()) {
            return "";
        }
        String[] encodings = encodingHeader.split(",");
        if (encodings.length != 1) {
            return "";
        }
        String encoding = encodings[0].trim().toLowerCase(Locale.ROOT);
        if ("gzip".equals(encoding) || "x-gzip".equals(encoding) || "deflate".equals(encoding)) {
            return encoding;
        }
        return "";
    }

    private static String headerValueIgnoreCase(Map<String, String> headers, String name) {
        if (headers == null || headers.isEmpty()) {
            return "";
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue() == null ? "" : entry.getValue();
            }
        }
        return "";
    }

    record DecodedBody(byte[] bytes, String encoding, boolean decoded, boolean decodeFailed) {
        static DecodedBody raw(byte[] bytes) {
            return new DecodedBody(bytes, "", false, false);
        }

        static DecodedBody decoded(byte[] bytes, String encoding) {
            return new DecodedBody(bytes, encoding, true, false);
        }

        static DecodedBody decodeFailed(byte[] bytes, String encoding) {
            return new DecodedBody(bytes, encoding, false, true);
        }
    }
}
