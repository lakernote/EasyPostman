package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CaptureQuickFilterTokensTest {

    @Test
    public void shouldNotTreatNegatedQuickFilterAsSelected() {
        List<String> tokens = CaptureQuickFilterTokens.parse("!https json");

        assertFalse(CaptureQuickFilterTokens.hasIncludedToken(tokens, "https"));
        assertTrue(CaptureQuickFilterTokens.hasIncludedToken(tokens, "json"));
    }

    @Test
    public void shouldKeepNegatedAliasWhenRemovingPositiveSelection() {
        List<String> tokens = new ArrayList<>(CaptureQuickFilterTokens.parse("!https https scheme:https"));

        CaptureQuickFilterTokens.removeToken(tokens, "https", false);

        assertEquals(tokens, List.of("!https"));
    }

    @Test
    public void shouldReplaceNegatedAliasWhenSelectingPositiveQuickFilter() {
        List<String> tokens = new ArrayList<>(CaptureQuickFilterTokens.parse("!https"));

        CaptureQuickFilterTokens.removeToken(tokens, "https", true);
        tokens.add("https");

        assertEquals(tokens, List.of("https"));
        assertTrue(CaptureQuickFilterTokens.hasIncludedToken(tokens, "https"));
    }
}
