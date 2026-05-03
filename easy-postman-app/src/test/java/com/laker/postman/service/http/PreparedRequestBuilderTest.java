package com.laker.postman.service.http;

import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.AuthType;
import com.laker.postman.model.TransportAuth;
import com.laker.postman.service.setting.SettingManager;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;


/**
 * 测试 PreparedRequestBuilder，特别是 Authorization 头的处理
 */
public class PreparedRequestBuilderTest {

    @Test
    public void testBasicAuthorizationHeaderIsAdded() {
        // Arrange
        HttpRequestItem item = new HttpRequestItem();
        item.setId("test-1");
        item.setMethod("POST");
        item.setUrl("https://idbrokerbts.webex.com/idb/oauth2/v1/access_token");
        item.setAuthType("Basic Auth");  // 使用正确的常量
        item.setAuthUsername("testuser");
        item.setAuthPassword("testpass");

        List<HttpHeader> headers = new ArrayList<>();
        headers.add(createHeader("Content-Type", "application/json", true));
        item.setHeadersList(headers);

        // Act
        PreparedRequest req = PreparedRequestBuilder.build(item);

        // Assert
        assertNotNull(req);
        assertNotNull(req.headersList);

        // 验证 Authorization 头被添加
        boolean hasAuth = false;
        for (HttpHeader header : req.headersList) {
            if ("Authorization".equals(header.getKey()) && header.isEnabled()) {
                hasAuth = true;
                assertTrue(header.getValue().startsWith("Basic "));
                break;
            }
        }
        assertTrue(hasAuth, "Authorization header should be added for basic auth");
        assertNull(req.transportAuth, "Basic auth should not keep transport auth metadata");
    }

    @Test
    public void testBearerTokenHeaderIsAdded() {
        // Arrange
        HttpRequestItem item = new HttpRequestItem();
        item.setId("test-2");
        item.setMethod("GET");
        item.setUrl("https://api.example.com/data");
        item.setAuthType("Bearer Token");  // 使用正确的常量
        item.setAuthToken("my-secret-token");

        List<HttpHeader> headers = new ArrayList<>();
        headers.add(createHeader("Accept", "application/json", true));
        item.setHeadersList(headers);

        // Act
        PreparedRequest req = PreparedRequestBuilder.build(item);

        // Assert
        assertNotNull(req);
        assertNotNull(req.headersList);

        // 验证 Authorization 头被添加
        boolean hasAuth = false;
        for (HttpHeader header : req.headersList) {
            if ("Authorization".equals(header.getKey()) && header.isEnabled()) {
                hasAuth = true;
                assertEquals("Bearer my-secret-token", header.getValue());
                break;
            }
        }
        assertTrue(hasAuth, "Authorization header should be added for bearer token");
        assertNull(req.transportAuth, "Bearer auth should not keep transport auth metadata");
    }

    @Test
    public void testDigestAuthDoesNotAddPreviewAuthorizationHeader() {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("test-digest");
        item.setMethod("GET");
        item.setUrl("https://api.example.com/digest");
        item.setAuthType(AuthType.DIGEST.getConstant());
        item.setAuthUsername("digest-user");
        item.setAuthPassword("digest-pass");

        List<HttpHeader> headers = new ArrayList<>();
        headers.add(createHeader("Accept", "application/json", true));
        item.setHeadersList(headers);

        PreparedRequest req = PreparedRequestBuilder.build(item);

        assertNotNull(req);
        assertNotNull(req.transportAuth);
        assertEquals(req.transportAuth.type, AuthType.DIGEST.getConstant());
        assertEquals(req.transportAuth.username, "digest-user");
        assertEquals(req.transportAuth.password, "digest-pass");
        for (HttpHeader header : req.headersList) {
            assertNotEquals(header.getKey(), "Authorization",
                    "Digest auth should wait for WWW-Authenticate challenge before adding Authorization");
        }
    }

    @Test
    public void testPreparedRequestSimplifyClearsTransportAuth() {
        PreparedRequest req = new PreparedRequest();
        req.transportAuth = new TransportAuth(AuthType.DIGEST.getConstant(), "digest-user", "digest-pass");

        req.simplify();

        assertNull(req.transportAuth);
    }

    @Test
    public void testDigestTransportAuthIsSkippedWhenAuthorizationHeaderIsExplicit() {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("test-digest-explicit-auth");
        item.setMethod("GET");
        item.setUrl("https://api.example.com/digest");
        item.setAuthType(AuthType.DIGEST.getConstant());
        item.setAuthUsername("digest-user");
        item.setAuthPassword("digest-pass");

        List<HttpHeader> headers = new ArrayList<>();
        headers.add(createHeader("Authorization", "Custom Auth Value", true));
        item.setHeadersList(headers);

        PreparedRequest req = PreparedRequestBuilder.build(item);
        RequestFinalizer.finalizeForSend(req, item, true);

        assertNull(req.transportAuth, "Explicit Authorization should suppress Digest transport auth");
        assertEquals(findHeaderValue(req.headersList, "Authorization"), "Custom Auth Value");
    }

    @Test
    public void testExistingAuthorizationHeaderIsNotOverridden() {
        // Arrange
        HttpRequestItem item = new HttpRequestItem();
        item.setId("test-3");
        item.setMethod("POST");
        item.setUrl("https://api.example.com/data");
        item.setAuthType("Basic Auth");  // 使用正确的常量
        item.setAuthUsername("testuser");
        item.setAuthPassword("testpass");

        List<HttpHeader> headers = new ArrayList<>();
        headers.add(createHeader("Authorization", "Custom Auth Value", true));
        headers.add(createHeader("Content-Type", "application/json", true));
        item.setHeadersList(headers);

        // Act
        PreparedRequest req = PreparedRequestBuilder.build(item);

        // Assert
        assertNotNull(req);
        assertNotNull(req.headersList);

        // 验证原有的 Authorization 头没有被覆盖
        int authCount = 0;
        for (HttpHeader header : req.headersList) {
            if ("Authorization".equals(header.getKey()) && header.isEnabled()) {
                authCount++;
                assertEquals("Custom Auth Value", header.getValue(),
                    "Existing Authorization header should not be overridden");
            }
        }
        assertEquals(1, authCount, "Should have exactly one Authorization header");
    }

    @Test
    public void testNoAuthTypeDoesNotAddAuthorizationHeader() {
        // Arrange
        HttpRequestItem item = new HttpRequestItem();
        item.setId("test-4");
        item.setMethod("GET");
        item.setUrl("https://api.example.com/data");
        // 不设置 authType

        List<HttpHeader> headers = new ArrayList<>();
        headers.add(createHeader("Accept", "application/json", true));
        item.setHeadersList(headers);

        // Act
        PreparedRequest req = PreparedRequestBuilder.build(item);

        // Assert
        assertNotNull(req);
        assertNotNull(req.headersList);

        // 验证没有添加 Authorization 头
        for (HttpHeader header : req.headersList) {
            assertNotEquals("Authorization", header.getKey(),
                "Should not add Authorization header when authType is not set");
        }
    }

    @Test
    public void testHeadersListIsNotModifiedDirectly() {
        // Arrange
        HttpRequestItem item = new HttpRequestItem();
        item.setId("test-5");
        item.setMethod("POST");
        item.setUrl("https://api.example.com/data");
        item.setAuthType("Basic Auth");  // 使用正确的常量
        item.setAuthUsername("user");
        item.setAuthPassword("pass");

        List<HttpHeader> originalHeaders = new ArrayList<>();
        originalHeaders.add(createHeader("Content-Type", "application/json", true));
        item.setHeadersList(originalHeaders);

        int originalSize = originalHeaders.size();

        // Act
        PreparedRequest req = PreparedRequestBuilder.build(item);

        // Assert
        // 原始的 headersList 不应该被修改
        assertEquals(originalSize, originalHeaders.size(),
            "Original headersList should not be modified");

        // 但 PreparedRequest 的 headersList 应该包含新添加的 Authorization 头
        assertTrue(req.headersList.size() > originalSize,
            "PreparedRequest headersList should contain additional Authorization header");
    }

    @Test
    public void testRequestLevelSettingsOverrideGlobalDefaultsExceptSslVerification() {
        boolean oldSslVerificationDisabled = SettingManager.isRequestSslVerificationDisabled();

        try {
            SettingManager.setRequestSslVerificationDisabled(true);

            HttpRequestItem item = new HttpRequestItem();
            item.setId("test-settings-override");
            item.setMethod("GET");
            item.setUrl("https://api.example.com/data");
            item.setFollowRedirects(Boolean.FALSE);
            item.setCookieJarEnabled(Boolean.FALSE);
            item.setHttpVersion(HttpRequestItem.HTTP_VERSION_HTTP_1_1);
            item.setRequestTimeoutMs(4321);

            PreparedRequest req = PreparedRequestBuilder.build(item);

            assertFalse(req.followRedirects);
            assertFalse(req.cookieJarEnabled);
            assertFalse(req.sslVerificationEnabled);
            assertEquals(req.httpVersion, HttpRequestItem.HTTP_VERSION_HTTP_1_1);
            assertEquals(req.requestTimeoutMs, 4321);
        } finally {
            SettingManager.setRequestSslVerificationDisabled(oldSslVerificationDisabled);
        }
    }

    @Test
    public void testRequestLevelSettingsFallBackToGlobalDefaults() {
        boolean oldFollowRedirects = SettingManager.isFollowRedirects();
        boolean oldSslVerificationDisabled = SettingManager.isRequestSslVerificationDisabled();
        int oldRequestTimeout = SettingManager.getRequestTimeout();

        try {
            SettingManager.setFollowRedirects(false);
            SettingManager.setRequestSslVerificationDisabled(true);
            SettingManager.setRequestTimeout(6789);

            HttpRequestItem item = new HttpRequestItem();
            item.setId("test-settings-fallback");
            item.setMethod("GET");
            item.setUrl("https://api.example.com/data");
            item.setHttpVersion(null);

            PreparedRequest req = PreparedRequestBuilder.build(item);

            assertFalse(req.followRedirects);
            assertTrue(req.cookieJarEnabled);
            assertFalse(req.sslVerificationEnabled);
            assertEquals(req.httpVersion, HttpRequestItem.HTTP_VERSION_AUTO);
            assertEquals(req.requestTimeoutMs, 6789);
        } finally {
            SettingManager.setFollowRedirects(oldFollowRedirects);
            SettingManager.setRequestSslVerificationDisabled(oldSslVerificationDisabled);
            SettingManager.setRequestTimeout(oldRequestTimeout);
        }
    }

    private HttpHeader createHeader(String key, String value, boolean enabled) {
        HttpHeader header = new HttpHeader();
        header.setKey(key);
        header.setValue(value);
        header.setEnabled(enabled);
        return header;
    }

    private String findHeaderValue(List<HttpHeader> headers, String key) {
        if (headers == null) {
            return null;
        }
        for (HttpHeader header : headers) {
            if (header != null && header.getKey() != null && header.getKey().equalsIgnoreCase(key)) {
                return header.getValue();
            }
        }
        return null;
    }
}
