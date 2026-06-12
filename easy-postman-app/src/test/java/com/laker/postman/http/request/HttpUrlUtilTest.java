package com.laker.postman.http.request;

import com.laker.postman.request.util.HttpUrlUtil;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class HttpUrlUtilTest {

    @Test
    public void shouldDecodeOnlyQueryPartForDisplay() {
        String displayUrl = HttpUrlUtil.decodeQueryForDisplay(
                "https://example.com/a%2Fb?filter=%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87&lang=en#frag"
        );

        assertEquals(
                displayUrl,
                "https://example.com/a%2Fb?filter=测试中文&lang=en#frag"
        );
    }

    @Test
    public void shouldKeepPlusSignWhenDecodingComponent() {
        assertEquals(HttpUrlUtil.decodeComponent("a+b%20c"), "a+b c");
    }

    @Test
    public void shouldExtractPostmanStylePathVariablesFromUrlPathOnly() {
        var variables = HttpUrlUtil.extractPathVariables(
                "https://api.example.com:8443/users/:userId/orders/:order_id?q=:queryIgnored#/:fragmentIgnored"
        );

        assertEquals(variables.size(), 2);
        assertEquals(variables.get(0).getKey(), "userId");
        assertEquals(variables.get(1).getKey(), "order_id");
    }

    @Test
    public void shouldExposePathVariableSegmentsForUrlHighlighting() {
        String url = "https://api.example.com/users/:userId/orders/:order_id?q=:queryIgnored";

        var segments = HttpUrlUtil.extractPathVariableSegments(url);

        assertEquals(segments.size(), 2);
        assertEquals(segments.get(0).name(), "userId");
        assertEquals(url.substring(segments.get(0).startIndex(), segments.get(0).endIndex()), ":userId");
        assertEquals(segments.get(1).name(), "order_id");
        assertEquals(url.substring(segments.get(1).startIndex(), segments.get(1).endIndex()), ":order_id");
    }

    @Test
    public void shouldReplacePostmanStylePathVariablesWithoutTouchingSchemePortQueryOrFragment() {
        String resolved = HttpUrlUtil.replacePathVariables(
                "https://api.example.com:8443/users/:userId?q=:queryIgnored#/:fragmentIgnored",
                List.of(new com.laker.postman.request.model.HttpParam(true, "userId", "42"))
        );

        assertEquals(
                resolved,
                "https://api.example.com:8443/users/42?q=:queryIgnored#/:fragmentIgnored"
        );
    }
}
