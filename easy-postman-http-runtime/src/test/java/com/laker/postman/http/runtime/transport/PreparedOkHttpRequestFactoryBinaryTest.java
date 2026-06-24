package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.RequestBodyTypes;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class PreparedOkHttpRequestFactoryBinaryTest {

    @Test
    public void binaryBodyShouldSendFileBytesInsteadOfPathText() throws Exception {
        Path file = Files.createTempFile("easy-postman-binary-body", ".bin");
        byte[] payload = new byte[]{0, 1, 2, 3, (byte) 0xFE, (byte) 0xFF};
        Files.write(file, payload);

        PreparedRequest request = new PreparedRequest();
        request.url = "https://api.example.com/upload";
        request.method = "PUT";
        request.bodyType = RequestBodyTypes.BODY_TYPE_BINARY;
        request.body = file.toString();
        request.headersList = new ArrayList<>();
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/octet-stream"));

        Request okRequest = PreparedOkHttpRequestFactory.build(request);

        assertNotNull(okRequest.body());
        assertEquals(readRequestBody(okRequest.body()), payload);
        assertEquals(okRequest.body().contentType().toString(), "application/octet-stream");
    }

    @Test
    public void binaryBodyShouldDetectContentTypeWhenHeaderIsAbsent() throws Exception {
        Path file = Files.createTempFile("easy-postman-binary-body", ".png");
        Files.write(file, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        PreparedRequest request = new PreparedRequest();
        request.url = "https://api.example.com/upload";
        request.method = "POST";
        request.bodyType = RequestBodyTypes.BODY_TYPE_BINARY;
        request.body = file.toString();
        request.headersList = new ArrayList<>();

        Request okRequest = PreparedOkHttpRequestFactory.build(request);

        assertNotNull(okRequest.body());
        assertEquals(okRequest.body().contentType().toString(), "image/png");
    }

    private static byte[] readRequestBody(RequestBody body) throws IOException {
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        return buffer.readByteArray();
    }
}
