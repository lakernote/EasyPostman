package com.laker.postman.service.variable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prepares per-iteration data exposed through pm.iterationData and {{variable}}.
 */
public final class IterationDataRuntimeSupport {

    private static final Pattern BUILT_IN_PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\$[A-Za-z][A-Za-z0-9]*)}}");

    private IterationDataRuntimeSupport() {
    }

    public static Map<String, String> prepare(Map<String, String> sourceRow) {
        Map<String, String> prepared = new LinkedHashMap<>();
        if (sourceRow != null && !sourceRow.isEmpty()) {
            for (Map.Entry<String, String> entry : sourceRow.entrySet()) {
                if (entry.getKey() != null) {
                    String value = resolveBuiltInPlaceholders(entry.getValue());
                    prepared.put(entry.getKey(), value == null ? "" : value);
                }
            }
        }
        return prepared;
    }

    private static String resolveBuiltInPlaceholders(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        Matcher matcher = BUILT_IN_PLACEHOLDER_PATTERN.matcher(value);
        StringBuffer resolved = null;
        BuiltInFunctionService builtIns = BuiltInFunctionService.getInstance();

        while (matcher.find()) {
            String functionName = matcher.group(1);
            if (!builtIns.isBuiltInFunction(functionName)) {
                continue;
            }
            if (resolved == null) {
                resolved = new StringBuffer();
            }
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(builtIns.generate(functionName)));
        }

        if (resolved == null) {
            return value;
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }
}
