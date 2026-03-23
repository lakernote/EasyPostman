package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CaptureRequestFilterTest {

    @Test
    public void shouldKeepLegacyHostFilterBehavior() {
        CaptureRequestFilter filter = CaptureRequestFilter.parse("api.example.com");

        assertTrue(filter.matches("api.example.com", "/orders", "https://api.example.com/orders"));
        assertTrue(filter.matches("v1.api.example.com", "/orders", "https://v1.api.example.com/orders"));
        assertFalse(filter.matches("static.example.com", "/orders", "https://static.example.com/orders"));
    }

    @Test
    public void shouldRequireAllSpecifiedDimensions() {
        CaptureRequestFilter filter = CaptureRequestFilter.parse("host:api.example.com query:debug=true path:/orders");

        assertTrue(filter.matches(
                "api.example.com",
                "/v1/orders?debug=true&sort=desc",
                "https://api.example.com/v1/orders?debug=true&sort=desc"));
        assertFalse(filter.matches(
                "api.example.com",
                "/v1/orders?debug=false",
                "https://api.example.com/v1/orders?debug=false"));
        assertFalse(filter.matches(
                "api.example.com",
                "/v1/users?debug=true",
                "https://api.example.com/v1/users?debug=true"));
    }

    @Test
    public void shouldSupportExcludeRules() {
        CaptureRequestFilter filter = CaptureRequestFilter.parse("host:example.com !query:token=secret");

        assertTrue(filter.matches(
                "example.com",
                "/api/users?token=public",
                "https://example.com/api/users?token=public"));
        assertFalse(filter.matches(
                "example.com",
                "/api/users?token=secret",
                "https://example.com/api/users?token=secret"));
    }

    @Test
    public void shouldSupportWildcardAndRegexRules() {
        CaptureRequestFilter wildcardFilter = CaptureRequestFilter.parse("path:*/orders/*");
        CaptureRequestFilter regexFilter = CaptureRequestFilter.parse("regex:https://api\\.example\\.com/.+[?&](token|session)=");

        assertTrue(wildcardFilter.matches(
                "api.example.com",
                "/v1/orders/123?debug=true",
                "https://api.example.com/v1/orders/123?debug=true"));
        assertFalse(wildcardFilter.matches(
                "api.example.com",
                "/v1/users/123",
                "https://api.example.com/v1/users/123"));

        assertTrue(regexFilter.matches(
                "api.example.com",
                "/v1/orders?token=abc",
                "https://api.example.com/v1/orders?token=abc"));
        assertFalse(regexFilter.matches(
                "api.example.com",
                "/v1/orders?trace=abc",
                "https://api.example.com/v1/orders?trace=abc"));
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
}
