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
}
