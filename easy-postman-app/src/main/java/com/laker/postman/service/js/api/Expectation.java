package com.laker.postman.service.js.api;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.graalvm.polyglot.Value;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 支持 pm.expect(xxx) 断言的链式断言对象。
 * <p>
 * 这里实现的是 Postman / Chai 常用子集，重点覆盖内置 snippet 和用户脚本高频写法。
 */
public class Expectation {
    private final Object actual;
    private final boolean negated;

    public final Expectation to = this;
    public final Expectation be = this;
    public final Expectation have = this;
    public final Expectation been = this;
    public final Expectation is = this;
    public final Expectation that = this;
    public final Expectation which = this;
    public final Expectation and = this;
    public final Expectation has = this;
    public final Expectation with = this;
    public final Expectation at = this;
    public final Expectation of = this;
    public final Expectation same = this;
    public final Expectation a = this;
    public final Expectation an = this;
    public final Expectation deep = this;
    public final Expectation own = this;
    public final Expectation nested = this;
    public final Expectation all = this;
    public final Expectation any = this;
    public Expectation not;

    public Expectation(Object actual) {
        this(actual, false, true);
    }

    private Expectation(Object actual, boolean negated, boolean createNegatedMirror) {
        this.actual = actual;
        this.negated = negated;
        if (createNegatedMirror) {
            this.not = new Expectation(actual, !negated, false);
            this.not.not = this;
        }
    }

    public Expectation getNot() {
        return not;
    }

    public Expectation include(Object expected) {
        boolean result = containsValue(actual, expected);
        assertResult(
                result,
                I18nUtil.getMessage(MessageKeys.EXPECTATION_INCLUDE_FAILED, expected, actual),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_INCLUDE_FAILED, expected, actual)
        );
        return this;
    }

    public Expectation contain(Object expected) {
        return include(expected);
    }

    public Expectation eql(Object expected) {
        boolean result = deepEquals(actual, expected);
        assertResult(
                result,
                I18nUtil.getMessage(MessageKeys.EXPECTATION_EQL_FAILED, expected, actual),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_EQL_FAILED, expected, actual)
        );
        return this;
    }

    public Expectation equal(Object expected) {
        return eql(expected);
    }

    public Expectation property(String property) {
        PropertyLookup lookup = lookupProperty(actual, property);
        assertResult(
                lookup.found(),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_PROPERTY_NOT_FOUND, property),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_PROPERTY_FOUND, property)
        );
        return new Expectation(lookup.value());
    }

    public Expectation property(String property, Object expected) {
        PropertyLookup lookup = lookupProperty(actual, property);
        boolean result = lookup.found() && deepEquals(lookup.value(), expected);
        assertResult(
                result,
                "expected property '" + property + "' to equal " + printable(expected),
                "expected property '" + property + "' not to equal " + printable(expected)
        );
        return new Expectation(lookup.value());
    }

    public Expectation jsonSchema(Object schema) {
        boolean result = validateJsonSchema(actual, schema);
        assertResult(
                result,
                "expected value to match JSON schema",
                "expected value not to match JSON schema"
        );
        return this;
    }

    public Expectation match(String regex) {
        boolean result = actual != null && Pattern.compile(regex).matcher(String.valueOf(normalizeValue(actual))).find();
        assertResult(
                result,
                I18nUtil.getMessage(MessageKeys.EXPECTATION_MATCH_REGEX_FAILED, regex, actual),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_MATCH_REGEX_FAILED, regex, actual)
        );
        return this;
    }

    public Expectation match(Pattern pattern) {
        boolean result = actual != null && pattern.matcher(String.valueOf(normalizeValue(actual))).find();
        assertResult(
                result,
                I18nUtil.getMessage(MessageKeys.EXPECTATION_MATCH_PATTERN_FAILED, pattern, actual),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_MATCH_PATTERN_FAILED, pattern, actual)
        );
        return this;
    }

    public Expectation match(Object jsRegExp) {
        if (jsRegExp != null) {
            try {
                String regExpStr = jsRegExp.toString();
                if (regExpStr.startsWith("/") && regExpStr.lastIndexOf("/") > 0) {
                    String patternStr = regExpStr.substring(1, regExpStr.lastIndexOf("/"));
                    return match(Pattern.compile(patternStr));
                }
            } catch (Exception ignored) {
                // Fall through to the assertion error below.
            }
        }
        throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_MATCH_JSREGEXP_FAILED, jsRegExp, actual));
    }

    public Expectation below(Number max) {
        Number value = asNumber(actual);
        if (value == null) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_A_NUMBER, actual));
        }
        assertResult(
                value.doubleValue() < max.doubleValue(),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_BELOW_FAILED, max, actual),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_BELOW_FAILED, max, actual)
        );
        return this;
    }

    public Expectation above(Number min) {
        Number value = asNumber(actual);
        if (value == null) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_A_NUMBER, actual));
        }
        assertResult(
                value.doubleValue() > min.doubleValue(),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_ABOVE_FAILED, min, actual),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_ABOVE_FAILED, min, actual)
        );
        return this;
    }

    public Expectation least(Number min) {
        Number value = asNumber(actual);
        if (value == null) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_A_NUMBER, actual));
        }
        assertResult(
                value.doubleValue() >= min.doubleValue(),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_LEAST_FAILED, min, actual),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_LEAST_FAILED, min, actual)
        );
        return this;
    }

    public Expectation most(Number max) {
        Number value = asNumber(actual);
        if (value == null) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_A_NUMBER, actual));
        }
        assertResult(
                value.doubleValue() <= max.doubleValue(),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_MOST_FAILED, max, actual),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_MOST_FAILED, max, actual)
        );
        return this;
    }

    public Expectation within(Number min, Number max) {
        Number value = asNumber(actual);
        if (value == null) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_A_NUMBER, actual));
        }
        double doubleValue = value.doubleValue();
        assertResult(
                doubleValue >= min.doubleValue() && doubleValue <= max.doubleValue(),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_WITHIN_FAILED, min, max, actual),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_WITHIN_FAILED, min, max, actual)
        );
        return this;
    }

    public Expectation closeTo(Number expected, Number delta) {
        Number value = asNumber(actual);
        if (value == null) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_A_NUMBER, actual));
        }
        boolean result = Math.abs(value.doubleValue() - expected.doubleValue()) <= delta.doubleValue();
        assertResult(
                result,
                "expected " + printable(actual) + " to be close to " + expected + " +/- " + delta,
                "expected " + printable(actual) + " not to be close to " + expected + " +/- " + delta
        );
        return this;
    }

    public Expectation length(int expectedLength) {
        int actualLength = getLength(actual);
        assertResult(
                actualLength == expectedLength,
                I18nUtil.getMessage(MessageKeys.EXPECTATION_LENGTH_FAILED, expectedLength, actualLength),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_LENGTH_FAILED, expectedLength, actualLength)
        );
        return this;
    }

    public Expectation lengthOf(int expectedLength) {
        return length(expectedLength);
    }

    public Expectation ok() {
        assertResult(
                isTruthy(actual),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_OK_FAILED, actual),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_OK_FAILED, actual)
        );
        return this;
    }

    public Expectation getOk() {
        return ok();
    }

    public Expectation exist() {
        assertResult(
                normalizeValue(actual) != null,
                I18nUtil.getMessage(MessageKeys.EXPECTATION_EXIST_FAILED),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_EXIST_FAILED)
        );
        return this;
    }

    public Expectation getExist() {
        return exist();
    }

    public Expectation empty() {
        assertResult(
                isEmpty(actual),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_EMPTY_FAILED, actual),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_EMPTY_FAILED, actual)
        );
        return this;
    }

    public Expectation getEmpty() {
        return empty();
    }

    public Expectation a(String type) {
        String actualType = getTypeName(actual);
        assertResult(
                type.equalsIgnoreCase(actualType),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_TYPE_FAILED, type, actualType),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_TYPE_FAILED, type, actualType)
        );
        return this;
    }

    public Expectation an(String type) {
        return a(type);
    }

    public Expectation string(String substring) {
        return include(substring);
    }

    public Expectation keys(Object expectedKeys) {
        Set<String> actualKeys = extractKeys(actual);
        List<Object> expected = toList(expectedKeys);
        boolean result = !expected.isEmpty();
        for (Object key : expected) {
            if (!actualKeys.contains(String.valueOf(normalizeValue(key)))) {
                result = false;
                break;
            }
        }
        assertResult(
                result,
                "expected keys " + actualKeys + " to include " + expected,
                "expected keys " + actualKeys + " not to include " + expected
        );
        return this;
    }

    public Expectation members(Object expectedMembers) {
        List<Object> actualMembers = toList(actual);
        List<Object> expected = toList(expectedMembers);
        boolean result = actualMembers.size() == expected.size();
        if (result) {
            for (Object item : expected) {
                if (!listContainsDeep(actualMembers, item)) {
                    result = false;
                    break;
                }
            }
        }
        assertResult(
                result,
                "expected members " + actualMembers + " to equal " + expected,
                "expected members " + actualMembers + " not to equal " + expected
        );
        return this;
    }

    public Expectation oneOf(Object candidates) {
        boolean result = listContainsDeep(toList(candidates), actual);
        assertResult(
                result,
                "expected " + printable(actual) + " to be one of " + printable(candidates),
                "expected " + printable(actual) + " not to be one of " + printable(candidates)
        );
        return this;
    }

    public Expectation respondTo(String methodName) {
        boolean result = hasExecutableMember(actual, methodName);
        assertResult(
                result,
                "expected " + printable(actual) + " to respond to " + methodName,
                "expected " + printable(actual) + " not to respond to " + methodName
        );
        return this;
    }

    public Expectation satisfy(Value predicate) {
        if (predicate == null || !predicate.canExecute()) {
            throw new AssertionError("satisfy requires a predicate function");
        }
        Object result = normalizeValue(predicate.execute(actual));
        assertResult(
                isTruthy(result),
                "expected " + printable(actual) + " to satisfy predicate",
                "expected " + printable(actual) + " not to satisfy predicate"
        );
        return this;
    }

    public Expectation change(Value fn, Object target, String property) {
        return compareMutation(fn, target, property, MutationExpectation.CHANGE);
    }

    public Expectation change(Object target, String property) {
        return compareMutation(asExecutable(actual), target, property, MutationExpectation.CHANGE);
    }

    public Expectation increase(Value fn, Object target, String property) {
        return compareMutation(fn, target, property, MutationExpectation.INCREASE);
    }

    public Expectation increase(Object target, String property) {
        return compareMutation(asExecutable(actual), target, property, MutationExpectation.INCREASE);
    }

    public Expectation decrease(Value fn, Object target, String property) {
        return compareMutation(fn, target, property, MutationExpectation.DECREASE);
    }

    public Expectation decrease(Object target, String property) {
        return compareMutation(asExecutable(actual), target, property, MutationExpectation.DECREASE);
    }

    public Expectation getTrue() {
        assertResult(
                Boolean.TRUE.equals(normalizeValue(actual)),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_TRUE_FAILED, actual),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_TRUE_FAILED, actual)
        );
        return this;
    }

    public Expectation getFalse() {
        assertResult(
                Boolean.FALSE.equals(normalizeValue(actual)),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_FALSE_FAILED, actual),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_FALSE_FAILED, actual)
        );
        return this;
    }

    public Expectation getNull() {
        assertResult(
                normalizeValue(actual) == null,
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NULL_FAILED),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_NULL_FAILED)
        );
        return this;
    }

    public Expectation getUndefined() {
        return getNull();
    }

    public Expectation getNaN() {
        Object normalized = normalizeValue(actual);
        boolean result = (normalized instanceof Double doubleValue && doubleValue.isNaN())
                || (normalized instanceof Float floatValue && floatValue.isNaN());
        assertResult(
                result,
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NAN_FAILED, actual),
                I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_NAN_FAILED, actual)
        );
        return this;
    }

    private Expectation compareMutation(Value fn, Object target, String property, MutationExpectation expectation) {
        if (fn == null || !fn.canExecute()) {
            throw new AssertionError(expectation.methodName + " requires a function");
        }
        PropertyLookup before = lookupProperty(target, property);
        fn.executeVoid();
        PropertyLookup after = lookupProperty(target, property);

        boolean result;
        if (expectation == MutationExpectation.CHANGE) {
            result = !deepEquals(before.value(), after.value());
        } else {
            Number beforeNumber = asNumber(before.value());
            Number afterNumber = asNumber(after.value());
            if (beforeNumber == null || afterNumber == null) {
                throw new AssertionError(expectation.methodName + " requires numeric property values");
            }
            result = expectation == MutationExpectation.INCREASE
                    ? afterNumber.doubleValue() > beforeNumber.doubleValue()
                    : afterNumber.doubleValue() < beforeNumber.doubleValue();
        }

        assertResult(
                result,
                "expected function to " + expectation.methodName + " property '" + property + "'",
                "expected function not to " + expectation.methodName + " property '" + property + "'"
        );
        return this;
    }

    private enum MutationExpectation {
        CHANGE("change"),
        INCREASE("increase"),
        DECREASE("decrease");

        private final String methodName;

        MutationExpectation(String methodName) {
            this.methodName = methodName;
        }
    }

    private void assertResult(boolean result, String positiveMessage, String negativeMessage) {
        if (negated ? result : !result) {
            throw new AssertionError(negated ? negativeMessage : positiveMessage);
        }
    }

    private boolean validateJsonSchema(Object value, Object rawSchema) {
        Object schemaObject = normalizeValue(rawSchema);
        if (!(schemaObject instanceof Map<?, ?> schema)) {
            return false;
        }

        Object type = schema.get("type");
        if (type != null && !schemaTypeMatches(value, String.valueOf(normalizeValue(type)))) {
            return false;
        }

        Object required = schema.get("required");
        if (required != null) {
            for (Object key : toList(required)) {
                if (!lookupProperty(value, String.valueOf(normalizeValue(key))).found()) {
                    return false;
                }
            }
        }

        Object propertiesObject = schema.get("properties");
        Object normalizedProperties = normalizeValue(propertiesObject);
        if (normalizedProperties instanceof Map<?, ?> properties) {
            for (Map.Entry<?, ?> entry : properties.entrySet()) {
                String key = String.valueOf(entry.getKey());
                PropertyLookup property = lookupProperty(value, key);
                if (!property.found()) {
                    continue;
                }
                Object propertySchemaObject = normalizeValue(entry.getValue());
                if (propertySchemaObject instanceof Map<?, ?> propertySchema) {
                    Object propertyType = propertySchema.get("type");
                    if (propertyType != null
                            && !schemaTypeMatches(property.value(), String.valueOf(normalizeValue(propertyType)))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean schemaTypeMatches(Object value, String expectedType) {
        String actualType = getTypeName(value);
        return switch (expectedType.toLowerCase()) {
            case "integer" -> {
                Number number = asNumber(value);
                yield number != null && Math.floor(number.doubleValue()) == number.doubleValue();
            }
            case "number" -> asNumber(value) != null;
            case "object" -> "object".equals(actualType);
            case "array" -> "array".equals(actualType);
            case "string" -> "string".equals(actualType);
            case "boolean" -> "boolean".equals(actualType);
            case "null" -> normalizeValue(value) == null;
            default -> true;
        };
    }

    private PropertyLookup lookupProperty(Object source, String property) {
        if (property == null || property.isBlank()) {
            return new PropertyLookup(false, null);
        }

        PropertyLookup direct = lookupDirectProperty(source, property);
        if (direct.found() || !property.contains(".")) {
            return direct;
        }

        Object current = source;
        for (String segment : property.split("\\.")) {
            PropertyLookup part = lookupDirectProperty(current, segment);
            if (!part.found()) {
                return new PropertyLookup(false, null);
            }
            current = part.value();
        }
        return new PropertyLookup(true, current);
    }

    private PropertyLookup lookupDirectProperty(Object source, String property) {
        Object normalized = normalizeValue(source);
        if (normalized == null) {
            return new PropertyLookup(false, null);
        }

        if (normalized instanceof Map<?, ?> map) {
            if (map.containsKey(property)) {
                return new PropertyLookup(true, map.get(property));
            }
            return new PropertyLookup(false, null);
        }
        if (normalized instanceof JSONObject jsonObject) {
            if (jsonObject.containsKey(property)) {
                return new PropertyLookup(true, jsonObject.get(property));
            }
            return new PropertyLookup(false, null);
        }
        if (normalized instanceof List<?> list && isInteger(property)) {
            int index = Integer.parseInt(property);
            return index >= 0 && index < list.size()
                    ? new PropertyLookup(true, list.get(index))
                    : new PropertyLookup(false, null);
        }
        if (normalized instanceof JSONArray jsonArray && isInteger(property)) {
            int index = Integer.parseInt(property);
            return index >= 0 && index < jsonArray.size()
                    ? new PropertyLookup(true, jsonArray.get(index))
                    : new PropertyLookup(false, null);
        }
        if (normalized.getClass().isArray() && isInteger(property)) {
            int index = Integer.parseInt(property);
            return index >= 0 && index < Array.getLength(normalized)
                    ? new PropertyLookup(true, Array.get(normalized, index))
                    : new PropertyLookup(false, null);
        }

        String getterName = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        try {
            Method method = normalized.getClass().getMethod(getterName);
            return new PropertyLookup(true, method.invoke(normalized));
        } catch (Exception ignored) {
            return new PropertyLookup(false, null);
        }
    }

    private record PropertyLookup(boolean found, Object value) {
    }

    private Object normalizeValue(Object value) {
        if (!(value instanceof Value jsValue)) {
            return value;
        }

        try {
            if (jsValue.isNull()) {
                return null;
            }
            if (jsValue.isHostObject()) {
                return jsValue.asHostObject();
            }
            if (jsValue.hasArrayElements()) {
                List<Object> values = new ArrayList<>();
                for (long i = 0; i < jsValue.getArraySize(); i++) {
                    values.add(normalizeValue(jsValue.getArrayElement(i)));
                }
                return values;
            }
            if (jsValue.hasMembers()) {
                Map<String, Object> values = new LinkedHashMap<>();
                for (String key : jsValue.getMemberKeys()) {
                    values.put(key, normalizeValue(jsValue.getMember(key)));
                }
                return values;
            }
            if (jsValue.isBoolean()) {
                return jsValue.asBoolean();
            }
            if (jsValue.isNumber()) {
                if (jsValue.fitsInLong()) {
                    return jsValue.asLong();
                }
                return jsValue.asDouble();
            }
            if (jsValue.isString()) {
                return jsValue.asString();
            }
        } catch (Exception ignored) {
            // Fall back to the raw value below.
        }
        return value;
    }

    private boolean containsValue(Object container, Object expected) {
        Object normalized = normalizeValue(container);
        Object normalizedExpected = normalizeValue(expected);
        if (normalized == null || normalizedExpected == null) {
            return false;
        }
        if (normalized instanceof String value) {
            return value.contains(String.valueOf(normalizedExpected));
        }
        if (normalized instanceof Map<?, ?> map) {
            return map.containsKey(String.valueOf(normalizedExpected))
                    || map.values().stream().anyMatch(value -> deepEquals(value, normalizedExpected));
        }
        if (normalized instanceof JSONObject jsonObject) {
            return jsonObject.containsKey(String.valueOf(normalizedExpected))
                    || jsonObject.values().stream().anyMatch(value -> deepEquals(value, normalizedExpected));
        }
        if (normalized instanceof Collection<?> collection) {
            return collection.stream().anyMatch(value -> deepEquals(value, normalizedExpected));
        }
        if (normalized instanceof JSONArray jsonArray) {
            for (Object value : jsonArray) {
                if (deepEquals(value, normalizedExpected)) {
                    return true;
                }
            }
            return false;
        }
        if (normalized.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(normalized); i++) {
                if (deepEquals(Array.get(normalized, i), normalizedExpected)) {
                    return true;
                }
            }
            return false;
        }
        return String.valueOf(normalized).contains(String.valueOf(normalizedExpected));
    }

    private boolean deepEquals(Object left, Object right) {
        Object normalizedLeft = normalizeValue(left);
        Object normalizedRight = normalizeValue(right);
        if (normalizedLeft instanceof Number leftNumber && normalizedRight instanceof Number rightNumber) {
            return Double.compare(leftNumber.doubleValue(), rightNumber.doubleValue()) == 0;
        }
        if (normalizedLeft instanceof Map<?, ?> leftMap && normalizedRight instanceof Map<?, ?> rightMap) {
            if (leftMap.size() != rightMap.size()) {
                return false;
            }
            for (Map.Entry<?, ?> entry : leftMap.entrySet()) {
                if (!rightMap.containsKey(entry.getKey()) || !deepEquals(entry.getValue(), rightMap.get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        if (normalizedLeft instanceof Collection<?> leftCollection && normalizedRight instanceof Collection<?> rightCollection) {
            if (leftCollection.size() != rightCollection.size()) {
                return false;
            }
            var leftIterator = leftCollection.iterator();
            var rightIterator = rightCollection.iterator();
            while (leftIterator.hasNext() && rightIterator.hasNext()) {
                if (!deepEquals(leftIterator.next(), rightIterator.next())) {
                    return false;
                }
            }
            return true;
        }
        return Objects.equals(normalizedLeft, normalizedRight);
    }

    private Number asNumber(Object value) {
        Object normalized = normalizeValue(value);
        if (normalized instanceof Number number) {
            return number;
        }
        if (normalized instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private int getLength(Object obj) {
        Object normalized = normalizeValue(obj);
        if (normalized == null) {
            return 0;
        }
        if (normalized instanceof String text) {
            return text.length();
        }
        if (normalized instanceof Collection<?> collection) {
            return collection.size();
        }
        if (normalized instanceof Map<?, ?> map) {
            return map.size();
        }
        if (normalized instanceof JSONObject jsonObject) {
            return jsonObject.size();
        }
        if (normalized instanceof JSONArray jsonArray) {
            return jsonArray.size();
        }
        if (normalized.getClass().isArray()) {
            return Array.getLength(normalized);
        }
        throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_NO_LENGTH_PROPERTY, obj));
    }

    private boolean isTruthy(Object obj) {
        Object normalized = normalizeValue(obj);
        if (normalized == null) {
            return false;
        }
        if (normalized instanceof Boolean value) {
            return value;
        }
        if (normalized instanceof Number value) {
            return value.doubleValue() != 0;
        }
        if (normalized instanceof String value) {
            return !value.isEmpty();
        }
        if (normalized instanceof Collection<?> value) {
            return !value.isEmpty();
        }
        if (normalized instanceof Map<?, ?> value) {
            return !value.isEmpty();
        }
        if (normalized instanceof JSONObject value) {
            return !value.isEmpty();
        }
        if (normalized instanceof JSONArray value) {
            return !value.isEmpty();
        }
        return true;
    }

    private boolean isEmpty(Object obj) {
        Object normalized = normalizeValue(obj);
        if (normalized == null) {
            return true;
        }
        if (normalized instanceof String value) {
            return value.isEmpty();
        }
        if (normalized instanceof Collection<?> value) {
            return value.isEmpty();
        }
        if (normalized instanceof Map<?, ?> value) {
            return value.isEmpty();
        }
        if (normalized instanceof JSONObject value) {
            return value.isEmpty();
        }
        if (normalized instanceof JSONArray value) {
            return value.isEmpty();
        }
        if (normalized.getClass().isArray()) {
            return Array.getLength(normalized) == 0;
        }
        return false;
    }

    private String getTypeName(Object obj) {
        Object normalized = normalizeValue(obj);
        if (normalized == null) {
            return "null";
        }
        if (normalized instanceof String) {
            return "string";
        }
        if (normalized instanceof Number) {
            return "number";
        }
        if (normalized instanceof Boolean) {
            return "boolean";
        }
        if (normalized instanceof Collection<?> || normalized instanceof JSONArray || normalized.getClass().isArray()) {
            return "array";
        }
        if (normalized instanceof Map<?, ?> || normalized instanceof JSONObject) {
            return "object";
        }
        return "object";
    }

    private Set<String> extractKeys(Object value) {
        Object normalized = normalizeValue(value);
        Set<String> keys = new LinkedHashSet<>();
        if (normalized instanceof Map<?, ?> map) {
            map.keySet().forEach(key -> keys.add(String.valueOf(key)));
        } else if (normalized instanceof JSONObject jsonObject) {
            jsonObject.keySet().forEach(key -> keys.add(String.valueOf(key)));
        }
        return keys;
    }

    private List<Object> toList(Object value) {
        Object normalized = normalizeValue(value);
        List<Object> values = new ArrayList<>();
        if (normalized == null) {
            return values;
        }
        if (normalized instanceof Collection<?> collection) {
            values.addAll(collection);
            return values;
        }
        if (normalized instanceof JSONArray jsonArray) {
            for (Object item : jsonArray) {
                values.add(item);
            }
            return values;
        }
        if (normalized.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(normalized); i++) {
                values.add(Array.get(normalized, i));
            }
            return values;
        }
        values.add(normalized);
        return values;
    }

    private boolean listContainsDeep(List<Object> values, Object expected) {
        return values.stream().anyMatch(value -> deepEquals(value, expected));
    }

    private boolean hasExecutableMember(Object value, String methodName) {
        if (value instanceof Value jsValue && jsValue.hasMembers() && jsValue.hasMember(methodName)) {
            return jsValue.getMember(methodName).canExecute();
        }
        Object normalized = normalizeValue(value);
        if (normalized == null || methodName == null || methodName.isBlank()) {
            return false;
        }
        if (normalized instanceof Map<?, ?> map) {
            Object member = map.get(methodName);
            return member instanceof Value jsValue ? jsValue.canExecute() : member != null;
        }
        for (Method method : normalized.getClass().getMethods()) {
            if (method.getName().equals(methodName)) {
                return true;
            }
        }
        return false;
    }

    private Value asExecutable(Object value) {
        if (value instanceof Value jsValue && jsValue.canExecute()) {
            return jsValue;
        }
        throw new AssertionError("expected value to be an executable function");
    }

    private boolean isInteger(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String printable(Object value) {
        return String.valueOf(normalizeValue(value));
    }
}
