package com.laker.postman.performance.core.controller;

import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ConditionExpressionEvaluatorTest {

    @Test
    public void shouldEvaluateResolvedStringBooleanAndNumericComparisons() {
        ConditionExpressionEvaluator.VariableLookup lookup = lookup(Map.of(
                "status", "200",
                "tenant", "acme",
                "latency", "321",
                "method", "GET"
        ));

        assertTrue(ConditionExpressionEvaluator.evaluate(
                "{{status}} == 200 && {{tenant}} == 'acme' && {{latency}} < 1000",
                lookup
        ));
        assertTrue(ConditionExpressionEvaluator.evaluate(
                "({{status}} == 201 || {{status}} = 200) && !({{method}} != 'GET')",
                lookup
        ));
        assertFalse(ConditionExpressionEvaluator.evaluate("{{latency}} >= 1000", lookup));
    }

    @Test
    public void bareVariableShouldOnlyPassWhenResolvedTextIsTrue() {
        assertTrue(ConditionExpressionEvaluator.evaluate("{{shouldRun}}", lookup(Map.of("shouldRun", "true"))));
        assertTrue(ConditionExpressionEvaluator.evaluate("{{shouldRun}}", lookup(Map.of("shouldRun", "TRUE"))));
        assertFalse(ConditionExpressionEvaluator.evaluate("{{shouldRun}}", lookup(Map.of("shouldRun", "yes"))));
        assertFalse(ConditionExpressionEvaluator.evaluate("{{shouldRun}}", lookup(Map.of("shouldRun", "1"))));
    }

    @Test
    public void missingVariableShouldResolveAsEmptyAndSupportDefinedFunction() {
        ConditionExpressionEvaluator.VariableLookup lookup = lookup(Map.of("status", "200"));

        assertTrue(ConditionExpressionEvaluator.evaluate(
                "{{missing}} == '' && !defined('missing') && defined(\"status\")",
                lookup
        ));
        assertFalse(ConditionExpressionEvaluator.evaluate("defined('missing')", lookup));
    }

    @Test
    public void invalidExpressionShouldReturnFalseInsteadOfThrowing() {
        ConditionExpressionEvaluator.VariableLookup lookup = lookup(Map.of("status", "200", "latency", "abc"));

        assertFalse(ConditionExpressionEvaluator.evaluate("({{status}} == 200", lookup));
        assertFalse(ConditionExpressionEvaluator.evaluate("{{latency}} > 10", lookup));
        assertFalse(ConditionExpressionEvaluator.evaluate("", lookup));
    }

    private static ConditionExpressionEvaluator.VariableLookup lookup(Map<String, String> values) {
        return new ConditionExpressionEvaluator.VariableLookup() {
            @Override
            public String resolve(String variableName) {
                return values.get(variableName);
            }

            @Override
            public boolean isDefined(String variableName) {
                return values.containsKey(variableName);
            }
        };
    }
}
