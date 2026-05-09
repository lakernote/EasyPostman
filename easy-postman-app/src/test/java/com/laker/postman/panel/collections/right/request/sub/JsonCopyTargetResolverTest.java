package com.laker.postman.panel.collections.right.request.sub;

import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class JsonCopyTargetResolverTest {

    @Test
    public void shouldResolvePropertyKeyAndStringValueAtKeyOffset() {
        String json = """
                {
                    "data": "line1\\nline2",
                    "ok": true
                }
                """;

        JsonCopyTargetResolver.CopyTarget target = resolveAt(json, "\"data\"");

        assertEquals(target.key(), "data");
        assertEquals(target.value(), "line1\nline2");
    }

    @Test
    public void shouldResolveNestedPropertyInsteadOfParentValue() {
        String json = """
                {
                    "data": {
                        "id": 123,
                        "name": "demo"
                    }
                }
                """;

        JsonCopyTargetResolver.CopyTarget target = resolveAt(json, "\"id\"");

        assertEquals(target.key(), "id");
        assertEquals(target.value(), "123");
    }

    @Test
    public void shouldResolveArrayElementValueWithoutKey() {
        String json = """
                {
                    "items": [
                        "alpha",
                        "beta"
                    ]
                }
                """;

        JsonCopyTargetResolver.CopyTarget target = resolveAt(json, "\"alpha\"");

        assertNull(target.key());
        assertEquals(target.value(), "alpha");
    }

    @Test
    public void shouldReturnEmptyForInvalidJson() {
        Optional<JsonCopyTargetResolver.CopyTarget> target = JsonCopyTargetResolver.resolve("{\"data\":", 2);

        assertFalse(target.isPresent());
    }

    private JsonCopyTargetResolver.CopyTarget resolveAt(String json, String marker) {
        int offset = json.indexOf(marker) + 1;
        Optional<JsonCopyTargetResolver.CopyTarget> target = JsonCopyTargetResolver.resolve(json, offset);
        assertTrue(target.isPresent());
        return target.get();
    }
}
