package com.laker.postman.panel;

import com.laker.postman.model.HttpHeader;
import com.laker.postman.panel.collections.right.request.sub.EasyRequestHttpHeadersPanel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test for EasyRequestHttpHeadersPanel to ensure default headers are properly initialized
 */
public class EasyRequestHttpHeadersPanelTest {

    @Test
    public void testDefaultHeadersInitialization() {
        // Create a new panel
        EasyRequestHttpHeadersPanel panel = new EasyRequestHttpHeadersPanel();

        // Get the headers list
        List<HttpHeader> headers = panel.getHeadersList();

        // Should have 4 default headers
        Assert.assertEquals(headers.size(), 4, "Should have 4 default headers");

        // Verify each default header has a value (not empty)
        for (HttpHeader header : headers) {
            Assert.assertNotNull(header.getKey(), "Header key should not be null");
            Assert.assertFalse(header.getKey().trim().isEmpty(), "Header key should not be empty");
            Assert.assertNotNull(header.getValue(), "Header value should not be null");
            Assert.assertFalse(header.getValue().trim().isEmpty(), "Header value should not be empty for: " + header.getKey());
        }

        // Verify specific default headers
        boolean hasUserAgent = false;
        boolean hasAccept = false;
        boolean hasAcceptEncoding = false;
        boolean hasConnection = false;

        for (HttpHeader header : headers) {
            switch (header.getKey()) {
                case "User-Agent":
                    hasUserAgent = true;
                    Assert.assertTrue(header.getValue().startsWith("EasyPostman/"),
                        "User-Agent should start with 'EasyPostman/'");
                    break;
                case "Accept":
                    hasAccept = true;
                    Assert.assertEquals(header.getValue(), "*/*", "Accept should be '*/*'");
                    break;
                case "Accept-Encoding":
                    hasAcceptEncoding = true;
                    Assert.assertEquals(header.getValue(), "gzip, deflate, br",
                        "Accept-Encoding should be 'gzip, deflate, br'");
                    break;
                case "Connection":
                    hasConnection = true;
                    Assert.assertEquals(header.getValue(), "keep-alive",
                        "Connection should be 'keep-alive'");
                    break;
            }
        }

        Assert.assertTrue(hasUserAgent, "Should have User-Agent header");
        Assert.assertTrue(hasAccept, "Should have Accept header");
        Assert.assertTrue(hasAcceptEncoding, "Should have Accept-Encoding header");
        Assert.assertTrue(hasConnection, "Should have Connection header");
    }

    @Test
    public void testSetHeadersListWithEmptyList() {
        // Create a new panel
        EasyRequestHttpHeadersPanel panel = new EasyRequestHttpHeadersPanel();

        // Set headers list to empty
        panel.setHeadersList(new ArrayList<>());

        // Get the headers list - should be re-initialized with default headers
        List<HttpHeader> headers = panel.getHeadersList();

        // Should have 4 default headers
        Assert.assertEquals(headers.size(), 4, "Should have 4 default headers after setting empty list");

        // Verify each default header has a value
        for (HttpHeader header : headers) {
            Assert.assertFalse(header.getKey().trim().isEmpty(), "Header key should not be empty");
            Assert.assertFalse(header.getValue().trim().isEmpty(),
                "Header value should not be empty for: " + header.getKey());
        }
    }
}
