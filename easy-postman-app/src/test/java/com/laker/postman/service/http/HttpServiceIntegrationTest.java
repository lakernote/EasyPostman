package com.laker.postman.service.http;

import com.laker.postman.model.AuthType;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpFormData;
import com.laker.postman.model.HttpFormUrlencoded;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.TransportAuth;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import okhttp3.HttpUrl;
import okhttp3.Protocol;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import okio.Buffer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.SkipException;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URL;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLHandshakeException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class HttpServiceIntegrationTest {

    private MockWebServer server;

    @AfterMethod
    public void tearDown() throws IOException {
        if (server != null) {
            server.shutdown();
            server = null;
        }
        OkHttpClientManager.clearClientCache();
        CookieService.clearAllCookies();
    }

    @Test
    public void shouldSendGetRequestAndReadPlainTextResponse() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/plain; charset=utf-8")
                .setBody("pong"));

        PreparedRequest request = createRequest("GET", serverUrl("/ping"));
        request.headersList.add(new HttpHeader(true, "Accept", "*/*"));
        request.headersList.add(new HttpHeader(true, "User-Agent", "EasyPostman/Test"));

        HttpResponse response = HttpService.sendRequest(request, null);
        RecordedRequest recordedRequest = server.takeRequest();

        assertEquals(response.code, 200);
        assertEquals(response.body, "pong");
        assertEquals(recordedRequest.getMethod(), "GET");
        assertEquals(recordedRequest.getHeader("Accept"), "*/*");
        assertEquals(recordedRequest.getHeader("User-Agent"), "EasyPostman/Test");
    }

    @Test
    public void shouldSendJsonPostBodyToServer() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody("{\"ok\":true}"));

        PreparedRequest request = createRequest("POST", serverUrl("/echo"));
        request.body = "{\"chatId\":1,\"text\":\"hello\"}";
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json; charset=utf-8"));
        request.headersList.add(new HttpHeader(true, "Accept", "application/json"));

        HttpResponse response = HttpService.sendRequest(request, null);
        RecordedRequest recordedRequest = server.takeRequest();

        assertEquals(response.code, 200);
        assertEquals(response.body, "{\"ok\":true}");
        assertEquals(recordedRequest.getMethod(), "POST");
        assertEquals(recordedRequest.getHeader("Content-Type"), "application/json; charset=utf-8");
        assertEquals(recordedRequest.getBody().readUtf8(), request.body);
    }

    @Test
    public void shouldCleanJsonCommentsBeforeSendingApplicationJsonBody() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody("{\"ok\":true}"));

        PreparedRequest request = createRequest("POST", serverUrl("/json5"));
        request.body = "{\n  // comment\n  \"chatId\": 1,\n  \"text\": \"hello\" // trailing comment\n}";
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json; charset=utf-8"));

        HttpService.sendRequest(request, null);
        RecordedRequest recordedRequest = server.takeRequest();

        assertEquals(recordedRequest.getBody().readUtf8(), "{\"chatId\":1,\"text\":\"hello\"}");
    }

    @Test
    public void shouldSkipInvalidHeaderNamesWhenBuildingRequest() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("ok"));

        PreparedRequest request = createRequest("GET", serverUrl("/headers"));
        request.headersList.add(new HttpHeader(true, "Valid-Header", "value"));
        request.headersList.add(new HttpHeader(true, "Bad:Header", "should-skip"));
        request.headersList.add(new HttpHeader(true, "", "also-skip"));

        HttpService.sendRequest(request, null);
        RecordedRequest recordedRequest = server.takeRequest();

        assertEquals(recordedRequest.getHeader("Valid-Header"), "value");
        assertEquals(recordedRequest.getHeader("Bad:Header"), null);
    }

    @Test
    public void shouldRetryWithDigestAuthorizationAfterChallenge() throws Exception {
        server = createServer();
        String realm = "testrealm@host.com";
        String nonce = "dcd98b7102dd2f0e8b11d0f600bfb0c093";
        String opaque = "5ccc069c403ebaf9f0171e9517f40e41";
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest recordedRequest) {
                String authorization = recordedRequest.getHeader("Authorization");
                if (authorization == null) {
                    return digestChallenge(realm, nonce, opaque, false).setBody("unauthorized");
                }
                if (isValidDigestAuthorization(recordedRequest, realm, nonce, opaque, "Mufasa", "Circle Of Life")) {
                    return new MockResponse().setResponseCode(200).setBody("ok");
                }
                return new MockResponse().setResponseCode(401).setBody("bad digest");
            }
        });

        PreparedRequest request = createRequest("GET", serverUrl("/digest"));
        request.transportAuth = new TransportAuth(AuthType.DIGEST.getConstant(), "Mufasa", "Circle Of Life");

        HttpResponse response = HttpService.sendRequest(request, null);
        RecordedRequest firstRequest = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest retryRequest = server.takeRequest(1, TimeUnit.SECONDS);

        assertEquals(response.code, 200);
        assertNotNull(firstRequest);
        assertNotNull(retryRequest);
        assertEquals(firstRequest.getHeader("Authorization"), null);
        String authorization = retryRequest.getHeader("Authorization");
        assertNotNull(authorization);
        assertTrue(authorization.startsWith("Digest "));
        assertTrue(authorization.contains("username=\"Mufasa\""));
        assertTrue(authorization.contains("realm=\"testrealm@host.com\""));
        assertTrue(authorization.contains("nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\""));
        assertTrue(authorization.contains("uri=\"/digest\""));
        assertTrue(authorization.contains("qop=auth"));
        assertTrue(authorization.contains("opaque=\"5ccc069c403ebaf9f0171e9517f40e41\""));
        assertTrue(authorization.contains("response=\""));
    }

    @Test
    public void shouldRetryDigestAuthorizationWhenServerMarksNonceStale() throws Exception {
        server = createServer();
        String realm = "testrealm@host.com";
        String opaque = "stale-opaque";
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest recordedRequest) {
                String authorization = recordedRequest.getHeader("Authorization");
                if (authorization == null) {
                    return digestChallenge(realm, "old-nonce", opaque, false).setBody("unauthorized");
                }
                if (authorization.contains("nonce=\"old-nonce\"")
                        && isValidDigestAuthorization(recordedRequest, realm, "old-nonce", opaque, "Mufasa", "Circle Of Life")) {
                    return digestChallenge(realm, "new-nonce", opaque, true).setBody("stale");
                }
                if (isValidDigestAuthorization(recordedRequest, realm, "new-nonce", opaque, "Mufasa", "Circle Of Life")) {
                    return new MockResponse().setResponseCode(200).setBody("ok");
                }
                return new MockResponse().setResponseCode(401).setBody("bad digest");
            }
        });

        PreparedRequest request = createRequest("GET", serverUrl("/digest-stale"));
        request.transportAuth = new TransportAuth(AuthType.DIGEST.getConstant(), "Mufasa", "Circle Of Life");

        HttpResponse response = HttpService.sendRequest(request, null);
        RecordedRequest firstRequest = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest staleRequest = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest refreshedRequest = server.takeRequest(1, TimeUnit.SECONDS);

        assertEquals(response.code, 200);
        assertNotNull(firstRequest);
        assertNotNull(staleRequest);
        assertNotNull(refreshedRequest);
        assertEquals(firstRequest.getHeader("Authorization"), null);
        assertTrue(staleRequest.getHeader("Authorization").contains("nonce=\"old-nonce\""));
        assertTrue(refreshedRequest.getHeader("Authorization").contains("nonce=\"new-nonce\""));
    }

    @Test
    public void shouldNotSendDisabledHeaders() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("ok"));

        PreparedRequest request = createRequest("GET", serverUrl("/disabled-header"));
        request.headersList.add(new HttpHeader(false, "X-Disabled", "skip-me"));
        request.headersList.add(new HttpHeader(true, "X-Enabled", "keep-me"));

        HttpService.sendRequest(request, null);
        RecordedRequest recordedRequest = server.takeRequest();

        assertEquals(recordedRequest.getHeader("X-Disabled"), null);
        assertEquals(recordedRequest.getHeader("X-Enabled"), "keep-me");
    }

    @Test
    public void shouldSendDuplicateHeadersAsSeparateValues() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("ok"));

        PreparedRequest request = createRequest("GET", serverUrl("/duplicate-headers"));
        request.headersList.add(new HttpHeader(true, "X-Trace", "one"));
        request.headersList.add(new HttpHeader(true, "X-Trace", "two"));

        HttpService.sendRequest(request, null);
        RecordedRequest recordedRequest = server.takeRequest();

        assertEquals(recordedRequest.getHeaders().values("X-Trace"), List.of("one", "two"));
    }

    @Test
    public void shouldSendEmptyBodyForPostWithoutBodyContent() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("ok"));

        PreparedRequest request = createRequest("POST", serverUrl("/empty-post"));

        HttpService.sendRequest(request, null);
        RecordedRequest recordedRequest = server.takeRequest();

        assertEquals(recordedRequest.getMethod(), "POST");
        assertEquals(recordedRequest.getBodySize(), 0L);
    }

    @Test
    public void shouldSendFormUrlencodedRequestBody() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("ok"));

        PreparedRequest request = createRequest("POST", serverUrl("/form"));
        request.urlencodedList = new ArrayList<>();
        request.urlencodedList.add(new HttpFormUrlencoded(true, "name", "Alice"));
        request.urlencodedList.add(new HttpFormUrlencoded(true, "city", "Shanghai"));

        HttpService.sendRequest(request, null);
        RecordedRequest recordedRequest = server.takeRequest();

        assertEquals(recordedRequest.getMethod(), "POST");
        assertEquals(recordedRequest.getHeader("Content-Type"), "application/x-www-form-urlencoded");
        assertEquals(recordedRequest.getBody().readUtf8(), "name=Alice&city=Shanghai");
    }

    @Test
    public void shouldSkipDisabledFormUrlencodedFields() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("ok"));

        PreparedRequest request = createRequest("POST", serverUrl("/form-disabled"));
        request.urlencodedList = new ArrayList<>();
        request.urlencodedList.add(new HttpFormUrlencoded(false, "disabled", "skip"));
        request.urlencodedList.add(new HttpFormUrlencoded(true, "enabled", "keep"));

        HttpService.sendRequest(request, null);
        RecordedRequest recordedRequest = server.takeRequest();

        assertEquals(recordedRequest.getBody().readUtf8(), "enabled=keep");
    }

    @Test
    public void shouldSendMultipartRequestBody() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("ok"));

        PreparedRequest request = createRequest("POST", serverUrl("/multipart"));
        request.isMultipart = true;
        request.formDataList = new ArrayList<>();
        request.formDataList.add(new HttpFormData(true, "field1", HttpFormData.TYPE_TEXT, "value1"));

        HttpService.sendRequest(request, null);
        RecordedRequest recordedRequest = server.takeRequest();
        String body = recordedRequest.getBody().readUtf8();

        assertEquals(recordedRequest.getMethod(), "POST");
        assertTrue(recordedRequest.getHeader("Content-Type").startsWith("multipart/form-data; boundary="));
        assertTrue(body.contains("name=\"field1\""));
        assertTrue(body.contains("value1"));
    }

    @Test
    public void shouldSkipDisabledMultipartParts() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("ok"));

        PreparedRequest request = createRequest("POST", serverUrl("/multipart-disabled"));
        request.isMultipart = true;
        request.formDataList = new ArrayList<>();
        request.formDataList.add(new HttpFormData(false, "hidden", HttpFormData.TYPE_TEXT, "skip"));
        request.formDataList.add(new HttpFormData(true, "visible", HttpFormData.TYPE_TEXT, "keep"));

        HttpService.sendRequest(request, null);
        RecordedRequest recordedRequest = server.takeRequest();
        String body = recordedRequest.getBody().readUtf8();

        assertFalse(body.contains("name=\"hidden\""));
        assertTrue(body.contains("name=\"visible\""));
        assertTrue(body.contains("keep"));
    }

    @Test
    public void shouldSendMultipartPlaceholderForMissingFile() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("ok"));

        PreparedRequest request = createRequest("POST", serverUrl("/multipart-file"));
        request.isMultipart = true;
        request.formDataList = new ArrayList<>();
        request.formDataList.add(new HttpFormData(true, "upload", HttpFormData.TYPE_FILE, "/path/does/not/exist.txt"));

        HttpService.sendRequest(request, null);
        RecordedRequest recordedRequest = server.takeRequest();
        String body = recordedRequest.getBody().readUtf8();

        assertTrue(body.contains("name=\"upload\""));
        assertTrue(body.contains("filename=\"\""));
    }

    @Test
    public void shouldDecompressBrotliResponseBody() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/plain; charset=utf-8")
                .addHeader("Content-Encoding", "br")
                .setBody(new Buffer().write(Base64.getDecoder().decode("DwOAcG9uZy1icgM="))));

        PreparedRequest request = createRequest("GET", serverUrl("/brotli"));
        request.headersList.add(new HttpHeader(true, "Accept-Encoding", "br"));

        HttpResponse response = HttpService.sendRequest(request, null);

        assertEquals(response.code, 200);
        assertEquals(response.body, "pong-br");
        assertEquals(response.headers.get("Content-Encoding").get(0), "br");
    }

    @Test
    public void shouldKeepMultipleSetCookieHeaders() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Set-Cookie", "a=1; Path=/")
                .addHeader("Set-Cookie", "b=2; Path=/")
                .setBody("ok"));

        PreparedRequest request = createRequest("GET", serverUrl("/multi-cookie"));

        HttpResponse response = HttpService.sendRequest(request, null);

        assertNotNull(response.headers);
        assertEquals(response.headers.get("Set-Cookie"), List.of("a=1; Path=/", "b=2; Path=/"));
    }

    @Test
    public void shouldDecompressGzipResponseBody() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/plain; charset=utf-8")
                .addHeader("Content-Encoding", "gzip")
                .setBody(new Buffer().write(gzip("pong-gzip"))));

        PreparedRequest request = createRequest("GET", serverUrl("/gzip"));
        request.headersList.add(new HttpHeader(true, "Accept-Encoding", "gzip"));

        HttpResponse response = HttpService.sendRequest(request, null);

        assertEquals(response.code, 200);
        assertEquals(response.body, "pong-gzip");
        assertNotNull(response.headers);
        assertEquals(response.headers.get("Content-Encoding").get(0), "gzip");
    }

    @Test
    public void shouldSendHttpsRequestWithLenientSslVerification() throws Exception {
        server = createHttpsServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/plain; charset=utf-8")
                .setBody("secure-pong"));

        PreparedRequest request = createRequest("GET", serverUrl("/secure-ping"));
        request.sslVerificationEnabled = false;
        request.headersList.add(new HttpHeader(true, "Accept", "*/*"));

        HttpResponse response = HttpService.sendRequest(request, null);
        RecordedRequest recordedRequest = server.takeRequest();

        assertEquals(response.code, 200);
        assertEquals(response.body, "secure-pong");
        assertNotNull(recordedRequest.getHandshake());
        assertTrue(recordedRequest.getRequestUrl().isHttps());
    }

    @Test
    public void shouldUseHttp2WhenRequestedAndServerSupportsIt() throws Exception {
        server = createHttpsServer(List.of(Protocol.HTTP_2, Protocol.HTTP_1_1));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/plain; charset=utf-8")
                .setBody("h2-ok"));

        PreparedRequest request = createRequest("GET", serverUrl("/h2"));
        request.sslVerificationEnabled = false;
        request.httpVersion = HttpRequestItem.HTTP_VERSION_HTTP_2;

        HttpResponse response = HttpService.sendRequest(request, null);
        RecordedRequest recordedRequest = server.takeRequest();

        assertEquals(response.code, 200);
        assertEquals(response.protocol, Protocol.HTTP_2.toString());
        assertNotNull(recordedRequest.getHandshake());
    }

    @Test
    public void shouldUseHttp11WhenRequested() throws Exception {
        server = createHttpsServer(List.of(Protocol.HTTP_2, Protocol.HTTP_1_1));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/plain; charset=utf-8")
                .setBody("h1-ok"));

        PreparedRequest request = createRequest("GET", serverUrl("/h1"));
        request.sslVerificationEnabled = false;
        request.httpVersion = HttpRequestItem.HTTP_VERSION_HTTP_1_1;

        HttpResponse response = HttpService.sendRequest(request, null);
        RecordedRequest recordedRequest = server.takeRequest();

        assertEquals(response.code, 200);
        assertEquals(response.protocol, Protocol.HTTP_1_1.toString());
        assertNotNull(recordedRequest.getHandshake());
    }

    @Test
    public void shouldFailHttpsRequestWhenStrictSslVerificationRejectsSelfSignedCertificate() throws Exception {
        server = createHttpsServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/plain; charset=utf-8")
                .setBody("should-not-reach"));

        PreparedRequest request = createRequest("GET", serverUrl("/strict-secure-ping"));
        request.sslVerificationEnabled = true;

        SSLHandshakeException exception = expectThrows(SSLHandshakeException.class,
                () -> HttpService.sendRequest(request, null));

        assertNotNull(exception.getMessage());
        assertFalse(exception.getMessage().isBlank());
        assertEquals(server.getRequestCount(), 0);
    }

    @Test
    public void shouldHandleHeadResponseWithoutBody() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/plain; charset=utf-8")
                .addHeader("Content-Length", "4"));

        PreparedRequest request = createRequest("HEAD", serverUrl("/head"));

        HttpResponse response = HttpService.sendRequest(request, null);
        RecordedRequest recordedRequest = server.takeRequest();

        assertEquals(recordedRequest.getMethod(), "HEAD");
        assertEquals(response.code, 200);
        assertEquals(response.body, "");
        assertEquals(response.bodySize, 0L);
    }

    @Test
    public void shouldHandle204ResponseWithoutBody() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(204)
                .addHeader("Content-Type", "text/plain; charset=utf-8"));

        PreparedRequest request = createRequest("GET", serverUrl("/no-content"));

        HttpResponse response = HttpService.sendRequest(request, null);

        assertEquals(response.code, 204);
        assertEquals(response.body, "");
        assertEquals(response.bodySize, 0L);
    }

    @Test
    public void shouldHandle304ResponseWithoutBody() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(304)
                .addHeader("ETag", "\"etag-1\""));

        PreparedRequest request = createRequest("GET", serverUrl("/not-modified"));

        HttpResponse response = HttpService.sendRequest(request, null);

        assertEquals(response.code, 304);
        assertEquals(response.body, "");
        assertEquals(response.bodySize, 0L);
        assertEquals(response.headers.get("ETag"), List.of("\"etag-1\""));
    }

    @Test
    public void shouldThrowWhenServerClosesConnectionBeforeSendingResponseHeaders() throws Exception {
        server = createServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST);
            }
        });

        PreparedRequest request = createRequest("POST", serverUrl("/api/system/ping"));
        request.body = "{\"chatId\":1,\"text\":\"hello\"}";
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json; charset=utf-8"));
        request.headersList.add(new HttpHeader(true, "Accept", "*/*"));
        request.headersList.add(new HttpHeader(true, "Connection", "keep-alive"));

        IOException exception = expectThrows(IOException.class, () -> HttpService.sendRequest(request, null));
        RecordedRequest recordedRequest = server.takeRequest();

        assertEquals(recordedRequest.getMethod(), "POST");
        assertEquals(recordedRequest.getBody().readUtf8(), request.body);
        assertNotNull(exception.getMessage());
        assertFalse(exception.getMessage().isBlank());
    }

    @Test
    public void shouldReturnRedirectResponseWithoutFollowingWhenDisabled() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(302)
                .addHeader("Location", serverUrl("/target"))
                .setBody("redirect"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("target"));

        PreparedRequest request = createRequest("GET", serverUrl("/start"));
        request.followRedirects = false;

        HttpResponse response = RedirectHandler.executeWithRedirects(request, 10, null);
        RecordedRequest recordedRequest = server.takeRequest();

        assertEquals(response.code, 302);
        assertEquals(recordedRequest.getPath(), "/start");
        assertEquals(server.getRequestCount(), 1);
    }

    @Test
    public void shouldTransformPostToGetOn302Redirect() throws Exception {
        PreparedRequest request = createRequest("POST", "http://127.0.0.1/start");
        request.body = "{\"hello\":\"world\"}";
        request.isMultipart = true;
        request.formDataList = new ArrayList<>();
        request.urlencodedList = new ArrayList<>();
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json; charset=utf-8"));

        PreparedRequest redirectedRequest = RedirectHandler.prepareRedirectRequest(request, "http://127.0.0.1/redirected", 302, false);

        assertEquals(redirectedRequest.method, "GET");
        assertEquals(redirectedRequest.url, "http://127.0.0.1/redirected");
        assertEquals(redirectedRequest.body, null);
        assertFalse(redirectedRequest.isMultipart);
        assertEquals(redirectedRequest.formDataList, null);
        assertEquals(redirectedRequest.urlencodedList, null);
        assertEquals(findHeaderValue(redirectedRequest.headersList, "Content-Type"), null);
    }

    @Test
    public void shouldPreserveHeadMethodOn302Redirect() throws Exception {
        PreparedRequest request = createRequest("HEAD", "http://127.0.0.1/start");
        request.body = "ignored";
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json; charset=utf-8"));

        PreparedRequest redirectedRequest = RedirectHandler.prepareRedirectRequest(request, "http://127.0.0.1/redirected", 302, false);

        assertEquals(redirectedRequest.method, "HEAD");
        assertEquals(redirectedRequest.url, "http://127.0.0.1/redirected");
        assertEquals(redirectedRequest.body, null);
        assertEquals(findHeaderValue(redirectedRequest.headersList, "Content-Type"), null);
    }

    @Test
    public void shouldPreserveMethodBodyAndContentTypeOn307Redirect() throws Exception {
        PreparedRequest request = createRequest("POST", "http://127.0.0.1/start");
        request.body = "{\"hello\":\"world\"}";
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json; charset=utf-8"));
        request.headersList.add(new HttpHeader(true, "Authorization", "Bearer token"));

        PreparedRequest redirectedRequest = RedirectHandler.prepareRedirectRequest(request, "http://127.0.0.1/upload", 307, false);

        assertEquals(redirectedRequest.method, "POST");
        assertEquals(redirectedRequest.url, "http://127.0.0.1/upload");
        assertEquals(redirectedRequest.body, request.body);
        assertEquals(findHeaderValue(redirectedRequest.headersList, "Content-Type"), "application/json; charset=utf-8");
        assertEquals(findHeaderValue(redirectedRequest.headersList, "Authorization"), "Bearer token");
    }

    @Test
    public void shouldRemoveMultipartContentTypeOn307Redirect() throws Exception {
        PreparedRequest request = createRequest("POST", "http://127.0.0.1/start");
        request.isMultipart = true;
        request.formDataList = new ArrayList<>();
        request.formDataList.add(new HttpFormData(true, "file", HttpFormData.TYPE_TEXT, "value"));
        request.headersList.add(new HttpHeader(true, "Content-Type", "multipart/form-data"));

        PreparedRequest redirectedRequest = RedirectHandler.prepareRedirectRequest(request, "http://127.0.0.1/upload", 307, false);

        assertTrue(redirectedRequest.isMultipart);
        assertEquals(findHeaderValue(redirectedRequest.headersList, "Content-Type"), null);
    }

    @Test
    public void shouldRemoveMultipartContentTypeOn308Redirect() throws Exception {
        PreparedRequest request = createRequest("POST", "http://127.0.0.1/start");
        request.isMultipart = true;
        request.formDataList = new ArrayList<>();
        request.formDataList.add(new HttpFormData(true, "file", HttpFormData.TYPE_TEXT, "value"));
        request.headersList.add(new HttpHeader(true, "Content-Type", "multipart/form-data"));

        PreparedRequest redirectedRequest = RedirectHandler.prepareRedirectRequest(request, "http://127.0.0.1/upload", 308, false);

        assertTrue(redirectedRequest.isMultipart);
        assertEquals(findHeaderValue(redirectedRequest.headersList, "Content-Type"), null);
    }

    @Test
    public void shouldStripAuthorizationAndCookieOnCrossDomainRedirect() throws Exception {
        List<HttpHeader> headers = new ArrayList<>();
        headers.add(new HttpHeader(true, "Authorization", "Bearer token"));
        headers.add(new HttpHeader(true, "Cookie", "session=abc"));
        headers.add(new HttpHeader(true, "Content-Type", "application/json"));
        headers.add(new HttpHeader(true, "Host", "127.0.0.1"));

        @SuppressWarnings("unchecked")
        List<HttpHeader> cleanedHeaders = RedirectHandler.cleanHeadersList(headers, true, false, false);

        assertEquals(findHeaderValue(cleanedHeaders, "Authorization"), null);
        assertEquals(findHeaderValue(cleanedHeaders, "Cookie"), null);
        assertEquals(findHeaderValue(cleanedHeaders, "Content-Type"), null);
        assertEquals(findHeaderValue(cleanedHeaders, "Host"), null);
    }

    @Test
    public void shouldPreserveSensitiveHeadersOnSameOriginRedirect() throws Exception {
        List<HttpHeader> headers = new ArrayList<>();
        headers.add(new HttpHeader(true, "Authorization", "Bearer token"));
        headers.add(new HttpHeader(true, "Cookie", "session=abc"));
        headers.add(new HttpHeader(true, "Content-Type", "application/json"));

        @SuppressWarnings("unchecked")
        List<HttpHeader> cleanedHeaders = RedirectHandler.cleanHeadersList(headers, false, true, false);

        assertEquals(findHeaderValue(cleanedHeaders, "Authorization"), "Bearer token");
        assertEquals(findHeaderValue(cleanedHeaders, "Cookie"), "session=abc");
        assertEquals(findHeaderValue(cleanedHeaders, "Content-Type"), "application/json");
    }

    @Test
    public void shouldRemoveMultipartContentTypeWhenPreservingBody() throws Exception {
        List<HttpHeader> headers = new ArrayList<>();
        headers.add(new HttpHeader(true, "Content-Type", "multipart/form-data"));
        headers.add(new HttpHeader(true, "Authorization", "Bearer token"));

        @SuppressWarnings("unchecked")
        List<HttpHeader> cleanedHeaders = RedirectHandler.cleanHeadersList(headers, false, true, true);

        assertEquals(findHeaderValue(cleanedHeaders, "Content-Type"), null);
        assertEquals(findHeaderValue(cleanedHeaders, "Authorization"), "Bearer token");
    }

    @Test
    public void shouldTreatSchemeChangeAsCrossOriginRedirect() throws Exception {
        assertTrue(RedirectHandler.isCrossOrigin(new URL("http://example.com/api"), new URL("https://example.com/api")));
    }

    @Test
    public void shouldTreatPortChangeAsCrossOriginRedirect() throws Exception {
        assertTrue(RedirectHandler.isCrossOrigin(new URL("https://example.com/api"), new URL("https://example.com:8443/api")));
    }

    @Test
    public void shouldPersistAndSendCookiesWhenCookieJarEnabled() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Set-Cookie", "session=abc123; Path=/")
                .setBody("cookie-set"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("cookie-echo"));

        PreparedRequest firstRequest = createRequest("GET", serverUrl("/cookie/set"));
        firstRequest.cookieJarEnabled = true;
        PreparedRequest secondRequest = createRequest("GET", serverUrl("/cookie/check"));
        secondRequest.cookieJarEnabled = true;

        HttpService.sendRequest(firstRequest, null);
        HttpService.sendRequest(secondRequest, null);
        server.takeRequest();
        RecordedRequest cookieRequest = server.takeRequest();

        assertEquals(cookieRequest.getHeader("Cookie"), "session=abc123");
    }

    @Test
    public void shouldNotSendCookiesWhenCookieJarDisabled() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Set-Cookie", "session=abc123; Path=/")
                .setBody("cookie-set"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("cookie-check"));

        PreparedRequest firstRequest = createRequest("GET", serverUrl("/cookie/set"));
        firstRequest.cookieJarEnabled = true;
        PreparedRequest secondRequest = createRequest("GET", serverUrl("/cookie/check"));
        secondRequest.cookieJarEnabled = false;

        HttpService.sendRequest(firstRequest, null);
        HttpService.sendRequest(secondRequest, null);
        server.takeRequest();
        RecordedRequest cookieRequest = server.takeRequest();

        assertEquals(cookieRequest.getHeader("Cookie"), null);
    }

    @Test
    public void shouldRespectRequestTimeoutWhenServerDoesNotRespond() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

        PreparedRequest request = createRequest("GET", serverUrl("/hang"));
        request.requestTimeoutMs = 200;

        InterruptedIOException exception = expectThrows(InterruptedIOException.class, () -> HttpService.sendRequest(request, null));

        assertNotNull(exception.getMessage());
        assertFalse(exception.getMessage().isBlank());
    }

    @Test
    public void shouldMarkIncompleteTextResponseWhenConnectionDropsDuringBodyRead() throws Exception {
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/plain; charset=utf-8")
                .setBody("partial-body")
                .throttleBody(1, 1, TimeUnit.MILLISECONDS)
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));

        PreparedRequest request = createRequest("GET", serverUrl("/partial"));

        HttpResponse response = HttpService.sendRequest(request, null);

        assertEquals(response.code, 200);
        assertEquals(response.body, I18nUtil.getMessage(MessageKeys.RESPONSE_INCOMPLETE, "unexpected end of stream"));
        assertEquals(response.bodySize, 0L);
        assertEquals(response.filePath, null);
    }

    @Test
    public void shouldMarkIncompleteBinaryResponseWhenConnectionDropsDuringBodyRead() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            throw new SkipException("Binary download path requires a graphics environment");
        }
        server = createServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/octet-stream")
                .setBody(new Buffer().writeUtf8("partial-binary"))
                .throttleBody(1, 1, TimeUnit.MILLISECONDS)
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));

        PreparedRequest request = createRequest("GET", serverUrl("/partial-binary"));

        HttpResponse response = HttpService.sendRequest(request, null);

        assertEquals(response.code, 200);
        assertEquals(response.body, I18nUtil.getMessage(MessageKeys.RESPONSE_INCOMPLETE, "unexpected end of stream"));
        assertEquals(response.bodySize, 0L);
        assertEquals(response.filePath, null);
    }

    private MockWebServer createServer() throws IOException {
        MockWebServer created = new MockWebServer();
        created.start();
        return created;
    }

    private MockWebServer createHttpsServer() throws IOException {
        return createHttpsServer(List.of(Protocol.HTTP_2, Protocol.HTTP_1_1));
    }

    private MockWebServer createHttpsServer(List<Protocol> protocols) throws IOException {
        // okhttp-tls is only test infrastructure here: it lets the test build a throwaway
        // self-signed certificate so MockWebServer can speak real HTTPS on localhost.
        HeldCertificate localhostCertificate = new HeldCertificate.Builder()
                .addSubjectAlternativeName("localhost")
                .addSubjectAlternativeName("127.0.0.1")
                .commonName("localhost")
                .build();
        HandshakeCertificates serverCertificates = new HandshakeCertificates.Builder()
                .heldCertificate(localhostCertificate)
                .build();
        MockWebServer created = new MockWebServer();
        created.setProtocols(protocols);
        created.useHttps(serverCertificates.sslSocketFactory(), false);
        created.start();
        HttpUrl serverUrl = created.url("/");
        assertTrue(serverUrl.isHttps());
        return created;
    }

    private String serverUrl(String path) {
        return server.url(path).toString();
    }

    private PreparedRequest createRequest(String method, String url) {
        PreparedRequest request = new PreparedRequest();
        request.method = method;
        request.url = url;
        request.headersList = new ArrayList<>();
        request.collectBasicInfo = false;
        request.collectEventInfo = false;
        request.enableNetworkLog = false;
        return request;
    }

    private byte[] gzip(String value) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(value.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }

    private String findHeaderValue(List<HttpHeader> headers, String key) {
        for (HttpHeader header : headers) {
            if (header.getKey() != null && header.getKey().equalsIgnoreCase(key)) {
                return header.getValue();
            }
        }
        return null;
    }

    private MockResponse digestChallenge(String realm, String nonce, String opaque, boolean stale) {
        String challenge = "Digest realm=\"" + realm + "\", " +
                "qop=\"auth\", " +
                "nonce=\"" + nonce + "\", " +
                "opaque=\"" + opaque + "\"";
        if (stale) {
            challenge += ", stale=true";
        }
        return new MockResponse()
                .setResponseCode(401)
                .addHeader("WWW-Authenticate", challenge);
    }

    private boolean isValidDigestAuthorization(RecordedRequest request,
                                               String realm,
                                               String nonce,
                                               String opaque,
                                               String username,
                                               String password) {
        Map<String, String> attributes = parseDigestAuthorization(request.getHeader("Authorization"));
        if (!username.equals(attributes.get("username"))
                || !realm.equals(attributes.get("realm"))
                || !nonce.equals(attributes.get("nonce"))
                || !opaque.equals(attributes.get("opaque"))
                || !"auth".equals(attributes.get("qop"))
                || request.getPath() == null
                || !request.getPath().equals(attributes.get("uri"))) {
            return false;
        }

        String nc = attributes.get("nc");
        String cnonce = attributes.get("cnonce");
        if (nc == null || cnonce == null) {
            return false;
        }

        String ha1 = md5Hex(username + ":" + realm + ":" + password);
        String ha2 = md5Hex(request.getMethod() + ":" + attributes.get("uri"));
        String expected = md5Hex(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":auth:" + ha2);
        return expected.equals(attributes.get("response"));
    }

    private Map<String, String> parseDigestAuthorization(String authorization) {
        Map<String, String> attributes = new HashMap<>();
        if (authorization == null || !authorization.regionMatches(true, 0, "Digest ", 0, "Digest ".length())) {
            return attributes;
        }

        for (String token : authorization.substring("Digest ".length()).split(", ")) {
            String[] pair = token.split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            String value = pair[1];
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            attributes.put(pair[0], value);
        }
        return attributes;
    }

    private String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
