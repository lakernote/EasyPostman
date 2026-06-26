package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class CaptureFilterExpressionsTest {

    @Test
    public void shouldBuildOnlyHostFilterFromCapturedHost() {
        assertEquals(CaptureFilterExpressions.onlyHost("HTTPS://ChatGPT.COM:443/ces/v1/m"), "host:chatgpt.com");
    }

    @Test
    public void shouldAppendExcludeHostFilter() {
        assertEquals(
                CaptureFilterExpressions.excludeHost("https image", "static.example.com"),
                "https image !host:static.example.com"
        );
    }

    @Test
    public void shouldBuildOnlyRequestFilter() {
        assertEquals(
                CaptureFilterExpressions.onlyRequest("POST", "collector.github.com", "/github/collect?x=1"),
                "method:POST host:collector.github.com path:/github/collect"
        );
    }

    @Test
    public void shouldBuildPathFiltersFromCapturedPath() {
        assertEquals(CaptureFilterExpressions.onlyPath("/github/collect?x=1"), "path:/github/collect");
        assertEquals(
                CaptureFilterExpressions.excludePath("https", "/github/collect?x=1"),
                "https !path:/github/collect"
        );
    }

    @Test
    public void shouldBuildSourceProcessFilters() {
        assertEquals(CaptureFilterExpressions.onlyPid(" 25562 "), "pid:25562");
        assertEquals(CaptureFilterExpressions.excludePid("https", "25562"), "https !pid:25562");
        assertEquals(CaptureFilterExpressions.onlyProcess("Google Chrome Helper"), "process:\"Google Chrome Helper\"");
        assertEquals(
                CaptureFilterExpressions.excludeProcess("api", "Google Chrome Helper"),
                "api !process:\"Google Chrome Helper\""
        );
    }

    @Test
    public void shouldWrapOrExpressionWhenAppendingExcludeHost() {
        assertEquals(
                CaptureFilterExpressions.excludeHost("a.com or b.com json", "pinned.example.com"),
                "(a.com or b.com json) !host:pinned.example.com"
        );
    }
}
