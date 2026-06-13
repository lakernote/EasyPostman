package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.request.model.HttpParam;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class RequestUrlEditorSupportTest {

    @Test
    public void shouldMergePathVariablesFromUrlAndPreserveExistingValues() {
        List<HttpParam> merged = RequestUrlEditorSupport.mergePathVariablesFromUrl(
                "https://api.example.com/users/:userId/orders/:orderId?q=:ignored",
                List.of(
                        new HttpParam(true, "userId", "42", "User id"),
                        new HttpParam(true, "removed", "old")
                )
        );

        assertEquals(merged.size(), 2);
        assertEquals(merged.get(0).getKey(), "userId");
        assertEquals(merged.get(0).getValue(), "42");
        assertEquals(merged.get(0).getDescription(), "User id");
        assertEquals(merged.get(1).getKey(), "orderId");
        assertEquals(merged.get(1).getValue(), "");
    }

    @Test
    public void shouldPreserveQueryParamDescriptionsWhenUrlValuesChange() {
        List<HttpParam> merged = RequestUrlEditorSupport.mergeUrlParamsWithCurrentTableMetadata(
                "https://httpbin.org/get?q=easytools&lang=en&page=1",
                List.of(
                        new HttpParam(true, "q", "easypostman", "Search keyword"),
                        new HttpParam(true, "lang", "en", "Language"),
                        new HttpParam(true, "page", "1", "Page number")
                )
        );

        assertEquals(merged.size(), 3);
        assertEquals(merged.get(0).getKey(), "q");
        assertEquals(merged.get(0).getValue(), "easytools");
        assertEquals(merged.get(0).getDescription(), "Search keyword");
        assertEquals(merged.get(1).getDescription(), "Language");
        assertEquals(merged.get(2).getDescription(), "Page number");
    }

    @Test
    public void shouldPreserveDuplicateQueryParamDescriptionsInOrder() {
        List<HttpParam> merged = RequestUrlEditorSupport.mergeUrlParamsWithCurrentTableMetadata(
                "https://example.com/search?tag=java&tag=swing",
                List.of(
                        new HttpParam(true, "tag", "old-java", "Primary tag"),
                        new HttpParam(true, "tag", "old-swing", "Secondary tag")
                )
        );

        assertEquals(merged.size(), 2);
        assertEquals(merged.get(0).getValue(), "java");
        assertEquals(merged.get(0).getDescription(), "Primary tag");
        assertEquals(merged.get(1).getValue(), "swing");
        assertEquals(merged.get(1).getDescription(), "Secondary tag");
    }

    @Test
    public void shouldKeepDisabledQueryParamsWhenUrlIsReparsed() {
        List<HttpParam> merged = RequestUrlEditorSupport.mergeUrlParamsWithCurrentTableMetadata(
                "https://example.com/search?q=new",
                List.of(
                        new HttpParam(true, "q", "old", "Search keyword"),
                        new HttpParam(false, "debug", "1", "Local debug switch")
                )
        );

        assertEquals(merged.size(), 2);
        assertEquals(merged.get(0).getKey(), "q");
        assertEquals(merged.get(0).getValue(), "new");
        assertEquals(merged.get(0).getDescription(), "Search keyword");
        assertEquals(merged.get(1).getKey(), "debug");
        assertEquals(merged.get(1).getValue(), "1");
        assertEquals(merged.get(1).getDescription(), "Local debug switch");
        assertEquals(merged.get(1).isEnabled(), false);
    }
}
