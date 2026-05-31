package com.laker.postman.plugin.api;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PluginContributionSupportTest {

    @Test
    public void shouldAddNeutralScriptApiCompletions() {
        CollectingSink sink = new CollectingSink();

        PluginContributionSupport.addScriptApiCompletions(
                sink,
                "redis",
                "Redis plugin API",
                "execute",
                "",
                null,
                "query"
        );

        assertTrue(sink.items().contains(new ScriptCompletionItem(
                ScriptCompletionKind.BASIC,
                "pm.plugin",
                "pm.plugin",
                "pm.plugin(alias)"
        )));
        assertTrue(sink.items().contains(new ScriptCompletionItem(
                ScriptCompletionKind.BASIC,
                "pm.plugin(\"redis\")",
                "pm.plugin(\"redis\")",
                "Redis plugin API"
        )));
        assertTrue(sink.items().contains(new ScriptCompletionItem(
                ScriptCompletionKind.BASIC,
                "pm.plugin(\"redis\").execute",
                "pm.plugin(\"redis\").execute",
                "pm.plugin(\"redis\").execute(options)"
        )));
        assertTrue(sink.items().contains(new ScriptCompletionItem(
                ScriptCompletionKind.BASIC,
                "pm.redis.query",
                "pm.redis.query",
                "pm.redis.query(options)"
        )));
        assertEquals(sink.items().size(), 7);
    }

    @Test
    public void shouldAddNeutralSnippetCompletion() {
        CollectingSink sink = new CollectingSink();

        PluginContributionSupport.addSnippetCompletion(
                sink,
                "redis.get",
                "pm.plugin(\"redis\").execute({});",
                "Redis query + assert"
        );

        assertEquals(sink.items(), List.of(new ScriptCompletionItem(
                ScriptCompletionKind.SHORTHAND,
                "redis.get",
                "pm.plugin(\"redis\").execute({});",
                "Redis query + assert"
        )));
    }

    @Test
    public void shouldIgnoreBlankCompletionInputs() {
        CollectingSink sink = new CollectingSink();

        sink.basic("", "blank");
        sink.shorthand(" ", "replacement", "blank");
        PluginContributionSupport.addScriptApiCompletions(sink, " ", "Blank alias", "method");
        PluginContributionSupport.addSnippetCompletion(sink, null, "replacement", "null input");

        assertTrue(sink.items().isEmpty());
    }

    private static final class CollectingSink implements ScriptCompletionSink {
        private final List<ScriptCompletionItem> items = new ArrayList<>();

        @Override
        public void add(ScriptCompletionItem item) {
            if (item != null) {
                items.add(item);
            }
        }

        private List<ScriptCompletionItem> items() {
            return items;
        }
    }
}
