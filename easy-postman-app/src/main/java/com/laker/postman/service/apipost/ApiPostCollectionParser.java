package com.laker.postman.service.apipost;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.model.*;
import com.laker.postman.service.common.AuthParserUtil;
import com.laker.postman.service.common.CollectionNode;
import com.laker.postman.service.common.CollectionParseResult;
import com.laker.postman.service.common.NodeType;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.*;
import static com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel.*;

/**
 * ApiPost Collection解析器
 * 负责解析ApiPost导出的JSON格式
 */
@Slf4j
@UtilityClass
public class ApiPostCollectionParser {

    private static final String KEY_PARAMETER = "parameter";
    private static final String KEY_IS_CHECKED = "is_checked";
    private static final String KEY_VALUE = "value";

    public static CollectionParseResult parseApiPostCollection(String json) {
        try {
            JSONObject apipostRoot = JSONUtil.parseObj(json);
            if (!apipostRoot.containsKey("apis") || !apipostRoot.containsKey("name")) {
                return null;
            }

            String projectName = apipostRoot.getStr("name", "ApiPost");
            JSONArray apis = apipostRoot.getJSONArray("apis");
            if (apis == null || apis.isEmpty()) {
                return null;
            }

            Map<String, CollectionNode> idToNodeMap = new HashMap<>();
            for (Object apiObj : apis) {
                JSONObject api = (JSONObject) apiObj;
                if (!"folder".equals(api.getStr("target_type", ""))) {
                    continue;
                }

                RequestGroup group = new RequestGroup(api.getStr("name", "未命名文件夹"));
                group.setDescription(api.getStr("description", ""));
                JSONObject request = api.getJSONObject("request");
                if (request != null) {
                    JSONObject auth = request.getJSONObject("auth");
                    if (auth != null) {
                        AuthParserUtil.parseApiPostAuthToGroup(auth, group);
                    }
                }
                idToNodeMap.put(api.getStr("target_id"), new CollectionNode(NodeType.GROUP, group));
            }

            List<CollectionNode> topLevelNodes = new ArrayList<>();
            for (Object apiObj : apis) {
                JSONObject api = (JSONObject) apiObj;
                String targetId = api.getStr("target_id");
                String parentId = api.getStr("parent_id", "0");
                String targetType = api.getStr("target_type", "");

                CollectionNode currentNode = null;
                if ("folder".equals(targetType)) {
                    currentNode = idToNodeMap.get(targetId);
                } else if ("api".equals(targetType)) {
                    HttpRequestItem req = parseApiPostApi(api);
                    if (req != null) {
                        currentNode = new CollectionNode(NodeType.REQUEST, req);
                    }
                }

                if (currentNode == null) {
                    continue;
                }

                if ("0".equals(parentId)) {
                    topLevelNodes.add(currentNode);
                } else {
                    CollectionNode parentNode = idToNodeMap.get(parentId);
                    if (parentNode != null) {
                        parentNode.addChild(currentNode);
                    } else {
                        topLevelNodes.add(currentNode);
                    }
                }
            }

            RequestGroup collectionGroup = new RequestGroup(projectName);
            collectionGroup.setDescription(apipostRoot.getStr("intro", ""));

            JSONObject global = apipostRoot.getJSONObject("global");
            if (global != null) {
                JSONObject globalParam = global.getJSONObject("global_param");
                if (globalParam != null) {
                    JSONObject auth = globalParam.getJSONObject("auth");
                    if (auth != null) {
                        AuthParserUtil.parseApiPostAuthToGroup(auth, collectionGroup);
                    }
                }
            }

            CollectionParseResult result = new CollectionParseResult(collectionGroup);
            for (CollectionNode node : topLevelNodes) {
                result.addChild(node);
            }
            parseApiPostEnvironments(global, result, projectName);
            return result;
        } catch (Exception e) {
            log.error("解析Apipost Collection失败", e);
            return null;
        }
    }

    private static HttpRequestItem parseApiPostApi(JSONObject api) {
        try {
            HttpRequestItem req = new HttpRequestItem();
            req.setId(UUID.randomUUID().toString());
            req.setName(api.getStr("name", "未命名接口"));
            req.setMethod(api.getStr("method", "GET"));
            req.setUrl(api.getStr("url", ""));
            req.setDescription(api.getStr("description", ""));

            JSONObject request = api.getJSONObject("request");
            if (request != null) {
                parseAuth(request, req);
                parseHeaders(request, req);
                parseQuery(request, req);
                parseCookies(request, req);
                parseBody(request, req);
                parseApiPostScripts(request, req);
            }

            JSONObject response = api.getJSONObject("response");
            if (response != null) {
                JSONArray examples = response.getJSONArray("example");
                if (examples != null && !examples.isEmpty()) {
                    List<SavedResponse> savedResponsesList = new ArrayList<>();
                    for (Object exampleObj : examples) {
                        SavedResponse savedResponse = parseApiPostResponse((JSONObject) exampleObj, req);
                        if (savedResponse != null) {
                            savedResponsesList.add(savedResponse);
                        }
                    }
                    req.setResponse(savedResponsesList);
                }
            }
            return req;
        } catch (Exception e) {
            log.error("解析Apipost API失败", e);
            return null;
        }
    }

    private static void parseAuth(JSONObject request, HttpRequestItem req) {
        JSONObject auth = request.getJSONObject("auth");
        if (auth == null) {
            return;
        }

        String authType = auth.getStr("type", "");
        if ("inherit".equals(authType)) {
            req.setAuthType(AUTH_TYPE_INHERIT);
            return;
        }
        if ("noauth".equals(authType)) {
            req.setAuthType(AUTH_TYPE_NONE);
            return;
        }
        if ("basic".equals(authType)) {
            req.setAuthType(AUTH_TYPE_BASIC);
            JSONObject basic = auth.getJSONObject("basic");
            if (basic != null) {
                req.setAuthUsername(basic.getStr("username", ""));
                req.setAuthPassword(basic.getStr("password", ""));
            }
            return;
        }
        if ("bearer".equals(authType)) {
            req.setAuthType(AUTH_TYPE_BEARER);
            JSONObject bearer = auth.getJSONObject("bearer");
            if (bearer != null) {
                req.setAuthToken(bearer.getStr("key", ""));
            }
            return;
        }
        req.setAuthType(AUTH_TYPE_NONE);
    }

    private static void parseHeaders(JSONObject request, HttpRequestItem req) {
        JSONObject header = request.getJSONObject("header");
        if (header == null) {
            return;
        }
        JSONArray parameters = header.getJSONArray(KEY_PARAMETER);
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        List<HttpHeader> headersList = new ArrayList<>();
        for (Object paramObj : parameters) {
            JSONObject param = (JSONObject) paramObj;
            boolean enabled = param.getInt(KEY_IS_CHECKED, 1) == 1;
            headersList.add(new HttpHeader(enabled, param.getStr("key", ""), param.getStr(KEY_VALUE, "")));
        }
        req.setHeadersList(headersList);
    }

    private static void parseQuery(JSONObject request, HttpRequestItem req) {
        JSONObject query = request.getJSONObject("query");
        if (query == null) {
            return;
        }
        JSONArray parameters = query.getJSONArray(KEY_PARAMETER);
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        List<HttpParam> paramsList = new ArrayList<>();
        for (Object paramObj : parameters) {
            JSONObject param = (JSONObject) paramObj;
            boolean enabled = param.getInt(KEY_IS_CHECKED, 1) == 1;
            paramsList.add(new HttpParam(enabled, param.getStr("key", ""), param.getStr(KEY_VALUE, "")));
        }
        req.setParamsList(paramsList);
    }

    private static void parseCookies(JSONObject request, HttpRequestItem req) {
        JSONObject cookie = request.getJSONObject("cookie");
        if (cookie == null) {
            return;
        }
        JSONArray parameters = cookie.getJSONArray(KEY_PARAMETER);
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        List<String> cookiePairs = new ArrayList<>();
        for (Object paramObj : parameters) {
            JSONObject param = (JSONObject) paramObj;
            if (param.getInt(KEY_IS_CHECKED, 1) == 1) {
                cookiePairs.add(param.getStr("key", "") + "=" + param.getStr(KEY_VALUE, ""));
            }
        }
        if (cookiePairs.isEmpty()) {
            return;
        }

        if (req.getHeadersList() == null) {
            req.setHeadersList(new ArrayList<>());
        }

        for (HttpHeader header : req.getHeadersList()) {
            if ("Cookie".equalsIgnoreCase(header.getKey())) {
                String currentValue = header.getValue();
                header.setValue(currentValue.isEmpty() ? String.join("; ", cookiePairs) : currentValue + "; " + String.join("; ", cookiePairs));
                return;
            }
        }
        req.getHeadersList().add(new HttpHeader(true, "Cookie", String.join("; ", cookiePairs)));
    }

    private static void parseBody(JSONObject request, HttpRequestItem req) {
        JSONObject body = request.getJSONObject("body");
        if (body == null) {
            return;
        }

        String mode = body.getStr("mode", "");
        if ("urlencoded".equals(mode)) {
            req.setBodyType(BODY_TYPE_FORM_URLENCODED);
            JSONArray parameters = body.getJSONArray(KEY_PARAMETER);
            if (parameters != null) {
                List<HttpFormUrlencoded> urlencodedList = new ArrayList<>();
                for (Object paramObj : parameters) {
                    JSONObject param = (JSONObject) paramObj;
                    boolean enabled = param.getInt(KEY_IS_CHECKED, 1) == 1;
                    urlencodedList.add(new HttpFormUrlencoded(enabled, param.getStr("key", ""), param.getStr(KEY_VALUE, "")));
                }
                req.setUrlencodedList(urlencodedList);
            }
            return;
        }

        if ("formdata".equals(mode)) {
            req.setBodyType(BODY_TYPE_FORM_DATA);
            JSONArray parameters = body.getJSONArray(KEY_PARAMETER);
            if (parameters != null) {
                List<HttpFormData> formDataList = new ArrayList<>();
                for (Object paramObj : parameters) {
                    JSONObject param = (JSONObject) paramObj;
                    boolean enabled = param.getInt(KEY_IS_CHECKED, 1) == 1;
                    String fieldType = param.getStr("field_type", "string");
                    String key = param.getStr("key", "");
                    if ("file".equals(fieldType) || param.containsKey("file_name")) {
                        formDataList.add(new HttpFormData(enabled, key, HttpFormData.TYPE_FILE, param.getStr("file_name", "")));
                    } else {
                        formDataList.add(new HttpFormData(enabled, key, HttpFormData.TYPE_TEXT, param.getStr(KEY_VALUE, "")));
                    }
                }
                req.setFormDataList(formDataList);
            }
            return;
        }

        if ("raw".equals(mode)) {
            req.setBodyType(BODY_TYPE_RAW);
            req.setBody(body.getStr("raw", ""));
            return;
        }

        if ("binary".equals(mode)) {
            req.setBodyType(BODY_TYPE_RAW);
            req.setBody("");
        }
    }

    private static void parseApiPostScripts(JSONObject request, HttpRequestItem req) {
        String preScript = extractTaskScripts(request.get("pre_tasks"));
        if (!preScript.isBlank()) {
            req.setPrescript(preScript);
        }

        String postScript = extractTaskScripts(request.get("post_tasks"));
        if (!postScript.isBlank()) {
            req.setPostscript(postScript);
        }
    }

    private static String extractTaskScripts(Object tasksObject) {
        List<String> scripts = new ArrayList<>();
        collectTaskScripts(tasksObject, scripts);
        return String.join("\n", scripts).trim();
    }

    private static void collectTaskScripts(Object node, List<String> scripts) {
        if (node == null) {
            return;
        }
        if (node instanceof String text) {
            if (!text.isBlank()) {
                scripts.add(text.trim());
            }
            return;
        }
        if (node instanceof JSONArray array) {
            for (Object item : array) {
                collectTaskScripts(item, scripts);
            }
            return;
        }
        if (node instanceof JSONObject object) {
            for (String key : new String[]{"script", "code", "content", "value", "raw"}) {
                String text = object.getStr(key, "");
                if (!text.isBlank()) {
                    scripts.add(text.trim());
                    return;
                }
            }
            for (String key : object.keySet()) {
                collectTaskScripts(object.get(key), scripts);
            }
        }
    }

    private static void parseApiPostEnvironments(JSONObject global, CollectionParseResult result, String projectName) {
        if (global == null) {
            return;
        }

        JSONArray envs = global.getJSONArray("envs");
        if (envs == null || envs.isEmpty()) {
            return;
        }

        for (int i = 0; i < envs.size(); i++) {
            JSONObject envObj = envs.getJSONObject(i);
            if (envObj == null) {
                continue;
            }

            String envName = envObj.getStr("name", "").isBlank()
                    ? projectName + " - Env " + (i + 1)
                    : envObj.getStr("name");
            Environment env = new Environment(envName);
            env.setId(UUID.randomUUID().toString());

            JSONArray serverList = envObj.getJSONArray("server_list");
            if (serverList != null) {
                for (Object serverObj : serverList) {
                    if (serverObj instanceof JSONObject server) {
                        String uri = server.getStr("uri", "");
                        if (!uri.isBlank()) {
                            env.set("baseUrl", uri);
                            break;
                        }
                    }
                }
            }

            Object envVarList = envObj.get("env_var_list");
            if (envVarList instanceof JSONObject envMap) {
                for (String key : envMap.keySet()) {
                    Object rawValue = envMap.get(key);
                    env.set(key, rawValue == null ? "" : String.valueOf(rawValue));
                }
            } else if (envVarList instanceof JSONArray envArray) {
                for (Object variableObj : envArray) {
                    if (variableObj instanceof JSONObject variable) {
                        String key = variable.getStr("key", "");
                        if (!key.isBlank()) {
                            env.set(key, variable.getStr("value", ""));
                        }
                    }
                }
            }

            if (!env.getVariableList().isEmpty()) {
                result.addEnvironment(env);
            }
        }
    }

    private static int parseStatusCode(String codeStr) {
        try {
            return Integer.parseInt(codeStr);
        } catch (NumberFormatException e) {
            log.warn("Invalid status code: {}, using 200 as default", codeStr);
            return 200;
        }
    }

    private static SavedResponse parseApiPostResponse(JSONObject exampleJson, HttpRequestItem request) {
        try {
            SavedResponse savedResponse = new SavedResponse();
            savedResponse.setId(UUID.randomUUID().toString());

            JSONObject expect = exampleJson.getJSONObject("expect");
            if (expect != null) {
                savedResponse.setName(expect.getStr("name", "Response"));
                savedResponse.setCode(parseStatusCode(expect.getStr("code", "200")));
                savedResponse.setStatus("OK");
                savedResponse.setPreviewLanguage(expect.getStr("content_type", "json"));
            } else {
                savedResponse.setName("Response");
                savedResponse.setCode(200);
                savedResponse.setStatus("OK");
                savedResponse.setPreviewLanguage("json");
            }

            savedResponse.setTimestamp(System.currentTimeMillis());

            JSONArray headers = exampleJson.getJSONArray("headers");
            if (headers != null && !headers.isEmpty()) {
                List<HttpHeader> headersList = new ArrayList<>();
                for (Object h : headers) {
                    JSONObject hObj = (JSONObject) h;
                    headersList.add(new HttpHeader(true, hObj.getStr("key", ""), hObj.getStr(KEY_VALUE, "")));
                }
                savedResponse.setHeaders(headersList);
            }

            savedResponse.setBody(exampleJson.getStr("raw", ""));

            SavedResponse.OriginalRequest origReq = new SavedResponse.OriginalRequest();
            origReq.setMethod(request.getMethod());
            origReq.setUrl(request.getUrl());
            origReq.setHeaders(request.getHeadersList() != null ? new ArrayList<>(request.getHeadersList()) : new ArrayList<>());
            origReq.setParams(request.getParamsList() != null ? new ArrayList<>(request.getParamsList()) : new ArrayList<>());
            origReq.setBodyType(request.getBodyType());
            origReq.setBody(request.getBody());
            origReq.setFormDataList(request.getFormDataList() != null ? new ArrayList<>(request.getFormDataList()) : new ArrayList<>());
            origReq.setUrlencodedList(request.getUrlencodedList() != null ? new ArrayList<>(request.getUrlencodedList()) : new ArrayList<>());
            savedResponse.setOriginalRequest(origReq);
            return savedResponse;
        } catch (Exception e) {
            log.error("解析Apipost响应失败", e);
            return null;
        }
    }
}
