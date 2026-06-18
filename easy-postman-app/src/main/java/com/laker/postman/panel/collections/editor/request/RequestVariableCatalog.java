package com.laker.postman.panel.collections.editor.request;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.collection.CollectionInheritance;
import com.laker.postman.collection.model.CollectionRequestContext;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.model.Variable;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.GlobalVariablesService;
import com.laker.postman.service.collections.ActiveCollectionRequestRepository;
import com.laker.postman.service.collections.CollectionRequestRepository;
import com.laker.postman.service.variable.BuiltInFunctionService;
import com.laker.postman.service.variable.EnvironmentVariableService;
import com.laker.postman.service.variable.IterationDataVariableService;
import com.laker.postman.service.variable.VariablesService;
import com.laker.postman.variable.VariableType;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
class RequestVariableCatalog {
    private static final CollectionRequestRepository REQUEST_REPOSITORY = new ActiveCollectionRequestRepository();

    static Map<VariableType, List<RequestVariableUsage>> allByType() {
        return allByType(null);
    }

    static Map<VariableType, List<RequestVariableUsage>> allByType(HttpRequestItem request) {
        Map<VariableType, List<RequestVariableUsage>> result = new EnumMap<>(VariableType.class);
        putAll(result, VariableType.VARIABLE, VariablesService.getInstance().getAll());
        putAll(result, VariableType.ITERATION_DATA, IterationDataVariableService.getInstance().getAll());
        putAll(result, VariableType.GROUP, groupVariablesFor(request));
        putAll(result, VariableType.ENVIRONMENT, EnvironmentVariableService.getInstance().getAll());
        putAll(result, VariableType.GLOBAL, GlobalVariablesService.getInstance().getAll());
        putAll(result, VariableType.BUILT_IN, BuiltInFunctionService.getInstance().getAll());
        return result;
    }

    static RequestVariableUsage resolveUsage(String name, Map<VariableType, List<RequestVariableUsage>> variablesByType) {
        if (CharSequenceUtil.isBlank(name)) {
            return new RequestVariableUsage("", null, null);
        }

        String normalizedName = name.trim();
        if (variablesByType != null) {
            for (VariableType type : orderedTypes()) {
                List<RequestVariableUsage> usages = variablesByType.get(type);
                if (usages == null) {
                    continue;
                }
                for (RequestVariableUsage usage : usages) {
                    if (normalizedName.equals(usage.name())) {
                        String value = type == VariableType.BUILT_IN
                                ? BuiltInFunctionService.getInstance().get(normalizedName)
                                : usage.value();
                        return new RequestVariableUsage(normalizedName, value, type);
                    }
                }
            }
        }
        return new RequestVariableUsage(normalizedName, null, null);
    }

    private static Map<String, String> groupVariablesFor(HttpRequestItem request) {
        if (request == null || CharSequenceUtil.isBlank(request.getId())) {
            return Map.of();
        }
        return REQUEST_REPOSITORY.findRequestContextById(request.getId())
                .map(CollectionRequestContext::getGroupChain)
                .map(RequestVariableCatalog::groupVariablesFromChain)
                .orElseGet(Map::of);
    }

    private static Map<String, String> groupVariablesFromChain(List<RequestGroup> groupChain) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Variable variable : CollectionInheritance.mergeGroupVariables(groupChain)) {
            if (variable != null && variable.isEnabled() && CharSequenceUtil.isNotBlank(variable.getKey())) {
                result.put(variable.getKey().trim(), variable.getValue());
            }
        }
        return result;
    }

    private static List<VariableType> orderedTypes() {
        return List.of(
                VariableType.VARIABLE,
                VariableType.ITERATION_DATA,
                VariableType.GROUP,
                VariableType.ENVIRONMENT,
                VariableType.GLOBAL,
                VariableType.BUILT_IN
        );
    }

    private static void putAll(Map<VariableType, List<RequestVariableUsage>> result,
                               VariableType type,
                               Map<String, String> variables) {
        List<RequestVariableUsage> usages = toUsages(type, variables);
        if (!usages.isEmpty()) {
            result.put(type, usages);
        }
    }

    private static List<RequestVariableUsage> toUsages(VariableType type, Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return List.of();
        }

        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            if (CharSequenceUtil.isNotBlank(key)) {
                normalized.put(key.trim(), entry.getValue());
            }
        }

        List<RequestVariableUsage> usages = new ArrayList<>();
        for (Map.Entry<String, String> entry : normalized.entrySet()) {
            usages.add(new RequestVariableUsage(entry.getKey(), entry.getValue(), type));
        }
        return usages;
    }
}
