package com.laker.postman.performance.runtime;

import com.laker.postman.performance.core.controller.ConditionData;
import com.laker.postman.performance.core.controller.ConditionExpressionEvaluator;
import com.laker.postman.performance.core.controller.WhileData;
import com.laker.postman.performance.core.plan.PerformanceConditionController;
import com.laker.postman.performance.core.plan.PerformanceWhileController;
import com.laker.postman.service.variable.ExecutionContextScope;
import com.laker.postman.service.variable.ExecutionVariableContext;
import com.laker.postman.service.variable.VariableResolver;

final class PerformanceConditionEvaluator {

    private PerformanceConditionEvaluator() {
    }

    static boolean evaluate(PerformanceConditionController conditionController,
                            ExecutionVariableContext iterationContext) {
        try (ExecutionContextScope ignored = ExecutionContextScope.open(iterationContext)) {
            ConditionData conditionData = conditionController.getConditionData();
            return ConditionExpressionEvaluator.evaluate(
                    conditionData == null ? null : conditionData.expression,
                    APP_VARIABLE_LOOKUP
            );
        }
    }

    static boolean evaluate(PerformanceWhileController whileController,
                            ExecutionVariableContext iterationContext) {
        try (ExecutionContextScope ignored = ExecutionContextScope.open(iterationContext)) {
            WhileData whileData = whileController.getWhileData();
            return ConditionExpressionEvaluator.evaluate(
                    whileData == null ? null : whileData.expression,
                    APP_VARIABLE_LOOKUP
            );
        }
    }

    private static final ConditionExpressionEvaluator.VariableLookup APP_VARIABLE_LOOKUP =
            new ConditionExpressionEvaluator.VariableLookup() {
                @Override
                public String resolve(String variableName) {
                    return VariableResolver.resolveVariable(variableName);
                }

                @Override
                public boolean isDefined(String variableName) {
                    return VariableResolver.isVariableDefined(variableName);
                }
            };
}
