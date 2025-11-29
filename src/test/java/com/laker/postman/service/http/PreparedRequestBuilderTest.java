package com.laker.postman.service.http;

import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
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

    private HttpHeader createHeader(String key, String value, boolean enabled) {
        HttpHeader header = new HttpHeader();
        header.setKey(key);
        header.setValue(value);
        header.setEnabled(enabled);
        return header;
    }
}

