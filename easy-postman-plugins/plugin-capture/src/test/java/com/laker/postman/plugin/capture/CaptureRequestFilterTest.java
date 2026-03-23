package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CaptureRequestFilterTest {

    @Test
    public void shouldKeepLegacyHostFilterBehavior() {
        CaptureRequestFilter filter = CaptureRequestFilter.parse("api.example.com");

        assertTrue(filter.matches("api.example.com", "/orders", "https://api.example.com/orders", Map.of()));
        assertTrue(filter.matches("v1.api.example.com", "/orders", "https://v1.api.example.com/orders", Map.of()));
        assertFalse(filter.matches("static.example.com", "/orders", "https://static.example.com/orders", Map.of()));
    }

    @Test
    public void shouldRequireAllSpecifiedDimensions() {
        CaptureRequestFilter filter = CaptureRequestFilter.parse("host:api.example.com query:debug=true path:/orders");

        assertTrue(filter.matches(
                "api.example.com",
                "/v1/orders?debug=true&sort=desc",
                "https://api.example.com/v1/orders?debug=true&sort=desc",
                Map.of()));
        assertFalse(filter.matches(
                "api.example.com",
                "/v1/orders?debug=false",
                "https://api.example.com/v1/orders?debug=false",
                Map.of()));
        assertFalse(filter.matches(
                "api.example.com",
                "/v1/users?debug=true",
                "https://api.example.com/v1/users?debug=true",
                Map.of()));
    }

    @Test
    public void shouldSupportExcludeRules() {
        CaptureRequestFilter filter = CaptureRequestFilter.parse("host:example.com !query:token=secret");

        assertTrue(filter.matches(
                "example.com",
                "/api/users?token=public",
                "https://example.com/api/users?token=public",
                Map.of()));
        assertFalse(filter.matches(
                "example.com",
                "/api/users?token=secret",
                "https://example.com/api/users?token=secret",
                Map.of()));
    }

    @Test
    public void shouldSupportWildcardAndRegexRules() {
        CaptureRequestFilter wildcardFilter = CaptureRequestFilter.parse("path:*/orders/*");
        CaptureRequestFilter regexFilter = CaptureRequestFilter.parse("regex:https://api\\.example\\.com/.+[?&](token|session)=");

        assertTrue(wildcardFilter.matches(
                "api.example.com",
                "/v1/orders/123?debug=true",
                "https://api.example.com/v1/orders/123?debug=true",
                Map.of()));
        assertFalse(wildcardFilter.matches(
                "api.example.com",
                "/v1/users/123",
                "https://api.example.com/v1/users/123",
                Map.of()));

        assertTrue(regexFilter.matches(
                "api.example.com",
                "/v1/orders?token=abc",
                "https://api.example.com/v1/orders?token=abc",
                Map.of()));
        assertFalse(regexFilter.matches(
                "api.example.com",
                "/v1/orders?trace=abc",
                "https://api.example.com/v1/orders?trace=abc",
                Map.of()));
    }

    @Test
    public void shouldSupportSchemeAndResourceTypeAliases() {
        CaptureRequestFilter httpsFilter = CaptureRequestFilter.parse("https");
        CaptureRequestFilter imageFilter = CaptureRequestFilter.parse("image");
        CaptureRequestFilter jsonFilter = CaptureRequestFilter.parse("json");
        CaptureRequestFilter apiFilter = CaptureRequestFilter.parse("type:api");

        assertTrue(httpsFilter.matches("chatgpt.com", "/assets/app.js", "https://chatgpt.com/assets/app.js", Map.of()));
        assertFalse(httpsFilter.matches("example.com", "/app.js", "http://example.com/app.js", Map.of()));

        assertTrue(imageFilter.matches(
                "static.example.com",
                "/logo.png",
                "https://static.example.com/logo.png",
                Map.of("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")));
        assertFalse(imageFilter.matches(
                "api.example.com",
                "/v1/users",
                "https://api.example.com/v1/users",
                Map.of("Accept", "application/json")));

        assertTrue(jsonFilter.matches(
                "api.example.com",
                "/v1/users",
                "https://api.example.com/v1/users",
                Map.of("Accept", "application/json")));
        assertTrue(apiFilter.matches(
                "api.example.com",
                "/api/users/list",
                "https://api.example.com/api/users/list",
                Map.of()));
        assertFalse(apiFilter.matches(
                "www.example.com",
                "/index.html",
                "https://www.example.com/index.html",
                Map.of("Accept", "text/html")));
    }

    @Test
    public void shouldMitmAllHostsWhenOnlyQueryRulesExist() {
        CaptureRequestFilter filter = CaptureRequestFilter.parse("query:debug=true");

        assertTrue(filter.shouldMitmHost("api.example.com"));
        assertTrue(filter.shouldMitmHost("static.example.com"));
    }

    @Test
    public void shouldRespectHostRulesWhenDecidingMitm() {
        CaptureRequestFilter filter = CaptureRequestFilter.parse("host:api.example.com !host:static.example.com query:debug=true");

        assertTrue(filter.shouldMitmHost("api.example.com"));
        assertFalse(filter.shouldMitmHost("static.example.com"));
        assertFalse(filter.shouldMitmHost("other.example.com"));
    }

    @Test
    public void shouldSupportPartialHostSegmentMatching() {
        CaptureRequestFilter filter = CaptureRequestFilter.parse("echo.websocket");

        assertTrue(filter.matches(
                "echo.websocket.org",
                "/",
                "https://echo.websocket.org/",
                Map.of()));
        assertTrue(filter.shouldMitmHost("echo.websocket.org"));
    }
}
