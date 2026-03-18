package com.laker.postman.model.script;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 支持 pm.expect(xxx) 断言的链式断言对象
 */
public class Expectation {
    private final Object actual;
    private boolean negated = false;

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

    public Expectation(Object actual) {
        this.actual = actual;
    }

    /**
     * 获取 not 链式属性，用于反转断言
     */
    public Expectation getNot() {
        Expectation negatedExpectation = new Expectation(actual);
        negatedExpectation.negated = !this.negated;
        return negatedExpectation;
    }

    public void include(Object expected) {
        boolean result = actual != null && expected != null && actual.toString().contains(expected.toString());
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_INCLUDE_FAILED : MessageKeys.EXPECTATION_INCLUDE_FAILED,
                    expected, actual));
        }
    }

    public void eql(Object expected) {
        boolean result = Objects.equals(actual, expected);
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_EQL_FAILED : MessageKeys.EXPECTATION_EQL_FAILED,
                    expected, actual));
        }
    }

    public void equal(Object expected) {
        eql(expected); // equal() is an alias for eql() in Chai.js
    }

    public void property(String property) {
        boolean hasProperty;
        if (actual instanceof Map) {
            hasProperty = ((Map<?, ?>) actual).containsKey(property);
        } else {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_PROPERTY_NOT_MAP));
        }

        if (negated ? hasProperty : !hasProperty) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_PROPERTY_FOUND : MessageKeys.EXPECTATION_PROPERTY_NOT_FOUND,
                    property));
        }
    }

    public void match(String regex) {
        boolean result = actual != null && Pattern.compile(regex).matcher(actual.toString()).find();
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_MATCH_REGEX_FAILED : MessageKeys.EXPECTATION_MATCH_REGEX_FAILED,
                    regex, actual));
        }
    }

    public void match(Pattern pattern) {
        boolean result = actual != null && pattern.matcher(actual.toString()).find();
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_MATCH_PATTERN_FAILED : MessageKeys.EXPECTATION_MATCH_PATTERN_FAILED,
                    pattern, actual));
        }
    }

    // Handle JavaScript RegExp objects
    public void match(Object jsRegExp) {
        if (jsRegExp != null) {
            try {
                // Convert JavaScript RegExp to string and extract the pattern part
                String regExpStr = jsRegExp.toString();
                // JS RegExp toString format is typically /pattern/flags
                if (regExpStr.startsWith("/") && regExpStr.lastIndexOf("/") > 0) {
                    String patternStr = regExpStr.substring(1, regExpStr.lastIndexOf("/"));
                    // Create a Java Pattern (ignoring flags for simplicity)
                    Pattern pattern = Pattern.compile(patternStr);
                    match(pattern);
                    return;
                }
            } catch (Exception e) {
                // Fall through to the error below
            }
        }
        throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_MATCH_JSREGEXP_FAILED, jsRegExp, actual));
    }

    public void below(Number max) {
        if (!(actual instanceof Number)) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_A_NUMBER, actual));
        }
        boolean result = ((Number) actual).doubleValue() < max.doubleValue();
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_BELOW_FAILED : MessageKeys.EXPECTATION_BELOW_FAILED,
                    max, actual));
        }
    }

    public void above(Number min) {
        if (!(actual instanceof Number)) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_A_NUMBER, actual));
        }
        boolean result = ((Number) actual).doubleValue() > min.doubleValue();
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_ABOVE_FAILED : MessageKeys.EXPECTATION_ABOVE_FAILED,
                    min, actual));
        }
    }

    public void least(Number min) {
        if (!(actual instanceof Number)) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_A_NUMBER, actual));
        }
        boolean result = ((Number) actual).doubleValue() >= min.doubleValue();
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_LEAST_FAILED : MessageKeys.EXPECTATION_LEAST_FAILED,
                    min, actual));
        }
    }

    public void most(Number max) {
        if (!(actual instanceof Number)) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_A_NUMBER, actual));
        }
        boolean result = ((Number) actual).doubleValue() <= max.doubleValue();
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_MOST_FAILED : MessageKeys.EXPECTATION_MOST_FAILED,
                    max, actual));
        }
    }

    public void within(Number min, Number max) {
        if (!(actual instanceof Number)) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_NOT_A_NUMBER, actual));
        }
        double value = ((Number) actual).doubleValue();
        boolean result = value >= min.doubleValue() && value <= max.doubleValue();
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_WITHIN_FAILED : MessageKeys.EXPECTATION_WITHIN_FAILED,
                    min, max, actual));
        }
    }

    public void length(int expectedLength) {
        int actualLength = getLength(actual);
        boolean result = actualLength == expectedLength;
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_LENGTH_FAILED : MessageKeys.EXPECTATION_LENGTH_FAILED,
                    expectedLength, actualLength));
        }
    }

    private int getLength(Object obj) {
        if (obj == null) {
            return 0;
        } else if (obj instanceof String) {
            return ((String) obj).length();
        } else if (obj instanceof Collection) {
            return ((Collection<?>) obj).size();
        } else if (obj instanceof Map) {
            return ((Map<?, ?>) obj).size();
        } else if (obj.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(obj);
        } else {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_NO_LENGTH_PROPERTY, obj));
        }
    }

    // Type checking methods
    public void ok() {
        boolean result = isTruthy(actual);
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_OK_FAILED : MessageKeys.EXPECTATION_OK_FAILED,
                    actual));
        }
    }

    private boolean isTruthy(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof Number) return ((Number) obj).doubleValue() != 0;
        if (obj instanceof String) return !((String) obj).isEmpty();
        if (obj instanceof Collection) return !((Collection<?>) obj).isEmpty();
        if (obj instanceof Map) return !((Map<?, ?>) obj).isEmpty();
        return true;
    }

    // Alias for ok()
    public void exist() {
        boolean result = actual != null;
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_EXIST_FAILED : MessageKeys.EXPECTATION_EXIST_FAILED));
        }
    }

    public void empty() {
        boolean result = isEmpty(actual);
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_EMPTY_FAILED : MessageKeys.EXPECTATION_EMPTY_FAILED,
                    actual));
        }
    }

    private boolean isEmpty(Object obj) {
        if (obj == null) return true;
        if (obj instanceof String) return ((String) obj).isEmpty();
        if (obj instanceof Collection) return ((Collection<?>) obj).isEmpty();
        if (obj instanceof Map) return ((Map<?, ?>) obj).isEmpty();
        if (obj.getClass().isArray()) return java.lang.reflect.Array.getLength(obj) == 0;
        return false;
    }

    // Type assertion using string type names
    public void a(String type) {
        String actualType = getTypeName(actual);
        boolean result = type.equalsIgnoreCase(actualType);
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_TYPE_FAILED : MessageKeys.EXPECTATION_TYPE_FAILED,
                    type, actualType));
        }
    }

    public void an(String type) {
        a(type);  // Alias for grammatical correctness
    }

    // Boolean assertions
    public void getTrue() {
        boolean result = Boolean.TRUE.equals(actual);
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_TRUE_FAILED : MessageKeys.EXPECTATION_TRUE_FAILED,
                    actual));
        }
    }

    public void getFalse() {
        boolean result = Boolean.FALSE.equals(actual);
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_FALSE_FAILED : MessageKeys.EXPECTATION_FALSE_FAILED,
                    actual));
        }
    }

    // Null/undefined assertions
    public void getNull() {
        boolean result = actual == null;
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_NULL_FAILED : MessageKeys.EXPECTATION_NULL_FAILED));
        }
    }

    public void getUndefined() {
        // In JavaScript context, undefined is similar to null in Java
        getNull();
    }

    // NaN assertion
    public void getNaN() {
        boolean result = (actual instanceof Double && ((Double) actual).isNaN()) ||
                (actual instanceof Float && ((Float) actual).isNaN());
        if (negated ? result : !result) {
            throw new AssertionError(I18nUtil.getMessage(
                    negated ? MessageKeys.EXPECTATION_NOT_NAN_FAILED : MessageKeys.EXPECTATION_NAN_FAILED,
                    actual));
        }
    }

    private String getTypeName(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "string";
        if (obj instanceof Number) return "number";
        if (obj instanceof Boolean) return "boolean";
        if (obj instanceof Collection) return "array";
        if (obj instanceof Map) return "object";
        if (obj.getClass().isArray()) return "array";
        return "object";
    }
}