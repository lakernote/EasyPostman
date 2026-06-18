package com.laker.postman.panel.collections.editor.request;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.variable.VariableParser;
import com.laker.postman.variable.VariableSegment;
import com.laker.postman.variable.VariableType;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UtilityClass
class RequestVariableUsageScanner {
    static List<RequestVariableUsage> scan(HttpRequestItem request) {
        return scan(request, RequestVariableCatalog.allByType(request));
    }

    static List<RequestVariableUsage> scan(HttpRequestItem request,
                                           Map<VariableType, List<RequestVariableUsage>> variablesByType) {
        if (request == null) {
            return List.of();
        }

        Set<String> names = new LinkedHashSet<>();
        collect(names, request.getUrl());
        collectParams(names, request.getPathVariablesList());
        collectParams(names, request.getParamsList());
        collectHeaders(names, request.getHeadersList());
        collect(names, request.getAuthUsername());
        collect(names, request.getAuthPassword());
        collect(names, request.getAuthToken());
        collect(names, request.getAuthApiKeyName());
        collect(names, request.getAuthApiKeyValue());
        collect(names, request.getBody());
        collectFormData(names, request.getFormDataList());
        collectUrlencoded(names, request.getUrlencodedList());
        collect(names, request.getPrescript());
        collect(names, request.getPostscript());

        List<RequestVariableUsage> usages = new ArrayList<>();
        for (String name : names) {
            usages.add(RequestVariableCatalog.resolveUsage(name, variablesByType));
        }
        return usages;
    }

    private static void collect(Set<String> names, String value) {
        if (CharSequenceUtil.isBlank(value)) {
            return;
        }
        for (VariableSegment segment : VariableParser.getVariableSegments(value)) {
            if (CharSequenceUtil.isNotBlank(segment.name)) {
                names.add(segment.name.trim());
            }
        }
    }

    private static void collectParams(Set<String> names, List<HttpParam> params) {
        if (params == null) {
            return;
        }
        for (HttpParam param : params) {
            if (param != null && param.isEnabled()) {
                collect(names, param.getKey());
                collect(names, param.getValue());
            }
        }
    }

    private static void collectHeaders(Set<String> names, List<HttpHeader> headers) {
        if (headers == null) {
            return;
        }
        for (HttpHeader header : headers) {
            if (header != null && header.isEnabled()) {
                collect(names, header.getKey());
                collect(names, header.getValue());
            }
        }
    }

    private static void collectFormData(Set<String> names, List<HttpFormData> formDataList) {
        if (formDataList == null) {
            return;
        }
        for (HttpFormData formData : formDataList) {
            if (formData != null && formData.isEnabled()) {
                collect(names, formData.getKey());
                collect(names, formData.getValue());
            }
        }
    }

    private static void collectUrlencoded(Set<String> names, List<HttpFormUrlencoded> urlencodedList) {
        if (urlencodedList == null) {
            return;
        }
        for (HttpFormUrlencoded item : urlencodedList) {
            if (item != null && item.isEnabled()) {
                collect(names, item.getKey());
                collect(names, item.getValue());
            }
        }
    }
}
