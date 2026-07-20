package com.laker.postman.collection;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.model.Variable;
import com.laker.postman.request.model.AuthType;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.util.JsonUtil;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class CollectionInheritance {
    private static final String SEPARATOR = "\n\n";
    private static final String SECTION_COMMENT_TEMPLATE = "// === %s ===\n\n";

    public static HttpRequestItem apply(HttpRequestItem item, List<RequestGroup> groupChain) {
        if (item == null || groupChain == null || groupChain.isEmpty()) {
            return item;
        }

        HttpRequestItem mergedItem = JsonUtil.deepCopy(item, HttpRequestItem.class);
        if (mergedItem == null) {
            return item;
        }

        applyAuthInheritance(mergedItem, groupChain);
        mergedItem.setPrescript(mergePreScripts(groupChain, item.getPrescript()));
        mergedItem.setPostscript(mergePostScripts(item.getPostscript(), groupChain));
        mergedItem.setHeadersList(mergeHeaders(collectGroupHeaders(groupChain), safeList(item.getHeadersList())));
        return mergedItem;
    }

    public static List<Variable> mergeGroupVariables(List<RequestGroup> groupChain) {
        return mergeGroupVariables(groupChain, false);
    }

    /**
     * Merges only enabled collection/folder variables for executable request scopes.
     * The existing merge method intentionally remains unchanged for callers that need
     * the complete editable variable list, including disabled rows.
     */
    public static List<Variable> mergeEnabledGroupVariables(List<RequestGroup> groupChain) {
        return mergeGroupVariables(groupChain, true);
    }

    private static List<Variable> mergeGroupVariables(List<RequestGroup> groupChain, boolean enabledOnly) {
        if (groupChain == null || groupChain.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Variable> mergedMap = new LinkedHashMap<>();
        for (RequestGroup group : groupChain) {
            if (group == null || group.getVariables() == null) {
                continue;
            }
            for (Variable variable : group.getVariables()) {
                if (variable != null
                        && (!enabledOnly || variable.isEnabled())
                        && variable.getKey() != null
                        && !variable.getKey().trim().isEmpty()) {
                    mergedMap.put(variable.getKey(), variable);
                }
            }
        }
        return new ArrayList<>(mergedMap.values());
    }

    public static List<HttpHeader> mergeHeaders(List<HttpHeader> groupHeaders, List<HttpHeader> requestHeaders) {
        boolean hasGroupHeaders = groupHeaders != null && !groupHeaders.isEmpty();
        boolean hasRequestHeaders = requestHeaders != null && !requestHeaders.isEmpty();
        if (!hasGroupHeaders && !hasRequestHeaders) {
            return new ArrayList<>();
        }
        if (!hasGroupHeaders) {
            return new ArrayList<>(requestHeaders);
        }
        if (!hasRequestHeaders) {
            return new ArrayList<>(groupHeaders);
        }

        int estimatedSize = groupHeaders.size() + requestHeaders.size();
        Map<String, HttpHeader> mergedMap = new LinkedHashMap<>((int) (estimatedSize / 0.75) + 1);
        for (HttpHeader header : groupHeaders) {
            putHeaderIfValid(mergedMap, header);
        }
        for (HttpHeader header : requestHeaders) {
            putHeaderIfValid(mergedMap, header);
        }
        return new ArrayList<>(mergedMap.values());
    }

    private static void applyAuthInheritance(HttpRequestItem item, List<RequestGroup> groupChain) {
        if (!AuthType.INHERIT.getConstant().equals(item.getAuthType())) {
            return;
        }

        for (int i = groupChain.size() - 1; i >= 0; i--) {
            RequestGroup group = groupChain.get(i);
            if (group == null) {
                continue;
            }
            String groupAuthType = group.getAuthType();
            if (groupAuthType == null || AuthType.INHERIT.getConstant().equals(groupAuthType)) {
                continue;
            }
            if (AuthType.NONE.getConstant().equals(groupAuthType)) {
                item.setAuthType(AuthType.NONE.getConstant());
                return;
            }
            item.setAuthType(group.getAuthType());
            item.setAuthUsername(group.getAuthUsername());
            item.setAuthPassword(group.getAuthPassword());
            item.setAuthToken(group.getAuthToken());
            item.setAuthApiKeyName(group.getAuthApiKeyName());
            item.setAuthApiKeyValue(group.getAuthApiKeyValue());
            item.setAuthApiKeyPlacement(group.getAuthApiKeyPlacement());
            return;
        }
    }

    private static String mergePreScripts(List<RequestGroup> groupChain, String requestScript) {
        List<ScriptPart> scripts = new ArrayList<>();
        for (RequestGroup group : groupChain) {
            if (group != null && hasText(group.getPrescript())) {
                scripts.add(new ScriptPart(group.getName() + " PreScript", group.getPrescript()));
            }
        }
        if (hasText(requestScript)) {
            scripts.add(new ScriptPart("请求级脚本", requestScript));
        }
        return mergeScripts(scripts);
    }

    private static String mergePostScripts(String requestScript, List<RequestGroup> groupChain) {
        List<ScriptPart> scripts = new ArrayList<>();
        if (hasText(requestScript)) {
            scripts.add(new ScriptPart("请求级脚本", requestScript));
        }
        for (int i = groupChain.size() - 1; i >= 0; i--) {
            RequestGroup group = groupChain.get(i);
            if (group != null && hasText(group.getPostscript())) {
                scripts.add(new ScriptPart(group.getName() + " PostScript", group.getPostscript()));
            }
        }
        return mergeScripts(scripts);
    }

    private static String mergeScripts(List<ScriptPart> scripts) {
        List<ScriptPart> validScripts = scripts.stream()
                .filter(script -> script != null && hasText(script.content()))
                .toList();
        if (validScripts.isEmpty()) {
            return null;
        }
        if (validScripts.size() == 1) {
            return validScripts.get(0).content();
        }

        StringBuilder merged = new StringBuilder();
        for (int i = 0; i < validScripts.size(); i++) {
            ScriptPart script = validScripts.get(i);
            if (hasText(script.source())) {
                merged.append(String.format(SECTION_COMMENT_TEMPLATE, script.source()));
            }
            merged.append(script.content());
            if (i < validScripts.size() - 1) {
                merged.append(SEPARATOR);
            }
        }
        return merged.toString();
    }

    private static List<HttpHeader> collectGroupHeaders(List<RequestGroup> groupChain) {
        List<HttpHeader> headers = new ArrayList<>();
        for (RequestGroup group : groupChain) {
            if (group != null && group.getHeaders() != null && !group.getHeaders().isEmpty()) {
                headers.addAll(group.getHeaders());
            }
        }
        return headers;
    }

    private static void putHeaderIfValid(Map<String, HttpHeader> mergedMap, HttpHeader header) {
        if (header != null && header.getKey() != null && !header.getKey().trim().isEmpty()) {
            mergedMap.put(header.getKey().toLowerCase(), header);
        }
    }

    private static <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private record ScriptPart(String source, String content) {
    }
}
