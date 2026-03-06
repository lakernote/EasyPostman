package com.laker.postman.model.script;

import org.graalvm.polyglot.Value;

import java.util.*;

/**
 * Script API option helpers.
 */
final class ScriptOptionUtil {

    private ScriptOptionUtil() {
    }

    static Map<String, Object> toMap(Object value) {
        Object normalized = normalize(value);
        if (normalized == null) {
            return new LinkedHashMap<>();
        }
        if (normalized instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey());
                result.put(key, entry.getValue());
            }
            return result;
        }
        throw new IllegalArgumentException("Options must be an object");
    }

    static Object get(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key != null && map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    static String getString(Map<String, Object> map, String defaultValue, String... keys) {
        Object value = get(map, keys);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    static String getRequiredString(Map<String, Object> map, String... keys) {
        String value = getString(map, null, keys);
        if (isBlank(value)) {
            String keyHint = (keys != null && keys.length > 0) ? keys[0] : "value";
            throw new IllegalArgumentException("Missing required option: " + keyHint);
        }
        return value;
    }

    static int getInt(Map<String, Object> map, int defaultValue, String... keys) {
        Object value = get(map, keys);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignore) {
            return defaultValue;
        }
    }

    static long getLong(Map<String, Object> map, long defaultValue, String... keys) {
        Object value = get(map, keys);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignore) {
            return defaultValue;
        }
    }

    static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    static Object normalize(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Value polyglotValue) {
            if (polyglotValue.isNull()) {
                return null;
            }
            if (polyglotValue.isBoolean()) {
                return polyglotValue.asBoolean();
            }
            if (polyglotValue.isNumber()) {
                if (polyglotValue.fitsInInt()) return polyglotValue.asInt();
                if (polyglotValue.fitsInLong()) return polyglotValue.asLong();
                if (polyglotValue.fitsInDouble()) return polyglotValue.asDouble();
                return polyglotValue.asDouble();
            }
            if (polyglotValue.isString()) {
                return polyglotValue.asString();
            }
            if (polyglotValue.hasArrayElements()) {
                int size = (int) polyglotValue.getArraySize();
                List<Object> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(normalize(polyglotValue.getArrayElement(i)));
                }
                return list;
            }
            if (polyglotValue.hasMembers()) {
                Map<String, Object> map = new LinkedHashMap<>();
                for (String key : polyglotValue.getMemberKeys()) {
                    map.put(key, normalize(polyglotValue.getMember(key)));
                }
                return map;
            }
            return polyglotValue.toString();
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey());
                converted.put(key, normalize(entry.getValue()));
            }
            return converted;
        }

        if (value instanceof Collection<?> collection) {
            List<Object> converted = new ArrayList<>(collection.size());
            for (Object item : collection) {
                converted.add(normalize(item));
            }
            return converted;
        }

        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            List<Object> converted = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                converted.add(normalize(java.lang.reflect.Array.get(value, i)));
            }
            return converted;
        }

        return value;
    }
}

