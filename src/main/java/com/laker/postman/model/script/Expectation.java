package com.laker.postman.model.script;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 支持 pm.expect(xxx) 断言的链式断言对象
 */
public class Expectation {
    private final Object actual;
    public final Expectation to = this;
    public final Expectation be = this;
    public final Expectation have = this;

    public Expectation(Object actual) {
        this.actual = actual;
    }

    public void include(Object expected) {
        if (actual == null || expected == null || !actual.toString().contains(expected.toString())) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_INCLUDE_FAILED, expected, actual));
        }
    }

    public void eql(Object expected) {
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_EQL_FAILED, expected, actual));
        }
    }

    public void equal(Object expected) {
        eql(expected); // equal() is an alias for eql() in Chai.js
    }

    public void property(String property) {
        if (actual instanceof Map) {
            if (!((Map<?, ?>) actual).containsKey(property)) {
                throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_PROPERTY_NOT_FOUND, property));
            }
        } else {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_PROPERTY_NOT_MAP));
        }
    }

    public void match(String regex) {
        if (actual == null || !Pattern.compile(regex).matcher(actual.toString()).find()) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_MATCH_REGEX_FAILED, regex, actual));
        }
    }

    public void match(Pattern pattern) {
        if (actual == null || !pattern.matcher(actual.toString()).find()) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_MATCH_PATTERN_FAILED, pattern, actual));
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
        if (!(actual instanceof Number) || ((Number) actual).doubleValue() >= max.doubleValue()) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.EXPECTATION_BELOW_FAILED, max, actual));
        }
    }
}