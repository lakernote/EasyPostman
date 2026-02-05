package com.laker.postman.service.postman;

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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.*;

/**
 * Postman Collection解析器
 * 负责解析Postman Collection格式的请求（v2.1格式）
 * 此类只负责解析，不涉及 UI 层的 TreeNode 组装
 */
@Slf4j
@UtilityClass
public class PostmanCollectionParser {

    // 常量定义
    private static final String KEY_EVENT = "event";
    private static final String KEY_DISABLED = "disabled";
    private static final String KEY_VALUE = "value";
    private static final String KEY_HEADER = "header";
    private static final String MODE_FORMDATA = "formdata";
    private static final String MODE_URLENCODED = "urlencoded";


    /**
     * 解析完整的 Postman Collection JSON，返回解析结果
     *
     * @param json Postman Collection JSON 字符串
     * @return 解析结果，如果解析失败返回 null
     */
    public static CollectionParseResult parsePostmanCollection(String json) {
        try {
            JSONObject postmanRoot = JSONUtil.parseObj(json);
            if (postmanRoot.containsKey("info") && postmanRoot.containsKey("item")) {
                // 解析 collection 名称和描述
                JSONObject info = postmanRoot.getJSONObject("info");
                String collectionName = info.getStr("name", "Postman");
                RequestGroup collectionGroup = new RequestGroup(collectionName);

                // 解析集合描述
                String description = info.getStr("description");
                if (description != null && !description.isEmpty()) {
                    collectionGroup.setDescription(description);
                }

                // 解析集合级别的认证
                if (postmanRoot.containsKey("auth")) {
                    AuthParserUtil.parsePostmanAuthToGroup(postmanRoot.getJSONObject("auth"), collectionGroup);
                }

                // 解析集合级别的脚本
                if (postmanRoot.containsKey(KEY_EVENT)) {
                    parseEventsToGroup(postmanRoot.getJSONArray(KEY_EVENT), collectionGroup);
                }

                // 解析集合级别的变量
                if (postmanRoot.containsKey("variable")) {
                    collectionGroup.setVariables(parseVariables(postmanRoot.getJSONArray("variable")));
                }

                JSONArray items = postmanRoot.getJSONArray("item");
                CollectionParseResult result = new CollectionParseResult(collectionGroup);

                // 递归解析树结构
                List<CollectionNode> children = parsePostmanItems(items);
                for (CollectionNode child : children) {
                    result.addChild(child);
                }
                return result;
            }
        } catch (Exception e) {
            // 返回 null 表示解析失败
            log.error("解析Postman Collection失败", e);
            return null;
        }
        return null;
    }

    /**
     * 递归解析Postman集合，返回节点列表
     *
     * @param items Postman collection 的 item 数组
     * @return 节点列表
     */
    private static List<CollectionNode> parsePostmanItems(JSONArray items) {
        List<CollectionNode> nodeList = new ArrayList<>();
        for (Object obj : items) {
            JSONObject item = (JSONObject) obj;

            // 判断是文件夹还是请求
            // 文件夹：有 item 字段
            // 请求：有 request 字段
            if (item.containsKey("item")) {
                // 文件夹节点
                String folderName = item.getStr("name", "default group");
                RequestGroup group = new RequestGroup(folderName);

                // 解析文件夹描述
                String description = item.getStr("description");
                if (description != null && !description.isEmpty()) {
                    group.setDescription(description);
                }

                // 解析分组级别的认证
                JSONObject auth = item.getJSONObject("auth");
                if (auth != null) {
                    AuthParserUtil.parsePostmanAuthToGroup(auth, group);
                }

                // 解析分组级别的脚本
                JSONArray events = item.getJSONArray("event");
                if (events != null && !events.isEmpty()) {
                    parseEventsToGroup(events, group);
                }

                CollectionNode folderNode = new CollectionNode(NodeType.GROUP, group);

                // 递归处理子节点
                JSONArray children = item.getJSONArray("item");
                List<CollectionNode> childNodes = parsePostmanItems(children);
                for (CollectionNode child : childNodes) {
                    folderNode.addChild(child);
                }
                nodeList.add(folderNode);
            } else if (item.containsKey("request")) {
                // 纯请求节点
                HttpRequestItem req = parsePostmanSingleItem(item);
                CollectionNode requestNode = new CollectionNode(NodeType.REQUEST, req);

                nodeList.add(requestNode);
            }
        }
        return nodeList;
    }


    /**
     * 解析Postman的events到RequestGroup
     */
    private static void parseEventsToGroup(JSONArray events, RequestGroup group) {
        if (events == null || events.isEmpty()) {
            return;
        }

        ScriptPair scripts = parseEventScripts(events);
        if (scripts.preScript != null) {
            group.setPrescript(scripts.preScript);
        }
        if (scripts.postScript != null) {
            group.setPostscript(scripts.postScript);
        }
    }

    /**
     * 从events数组中解析前置和后置脚本
     */
    private static ScriptPair parseEventScripts(JSONArray events) {
        StringBuilder testScript = new StringBuilder();
        StringBuilder preScript = new StringBuilder();

        for (Object e : events) {
            JSONObject eObj = (JSONObject) e;
            String listen = eObj.getStr("listen");
            JSONObject script = eObj.getJSONObject("script");
            if (script != null) {
                JSONArray exec = script.getJSONArray("exec");
                if (exec != null) {
                    for (Object line : exec) {
                        if ("test".equals(listen)) {
                            testScript.append(line).append("\n");
                        } else if ("prerequest".equals(listen)) {
                            preScript.append(line).append("\n");
                        }
                    }
                }
            }
        }

        return new ScriptPair(
            preScript.isEmpty() ? null : preScript.toString(),
            testScript.isEmpty() ? null : testScript.toString()
        );
    }

    /**
     * 脚本对，包含前置和后置脚本
     */
    private static class ScriptPair {
        final String preScript;
        final String postScript;

        ScriptPair(String preScript, String postScript) {
            this.preScript = preScript;
            this.postScript = postScript;
        }
    }

    /**
     * 解析Postman的query参数数组
     */
    private static List<HttpParam> parseQueryParams(JSONArray queryArr) {
        if (queryArr == null || queryArr.isEmpty()) {
            return new ArrayList<>();
        }

        List<HttpParam> paramsList = new ArrayList<>();
        for (Object q : queryArr) {
            JSONObject qObj = (JSONObject) q;
            boolean enabled = !qObj.getBool(KEY_DISABLED, false);
            paramsList.add(new HttpParam(enabled, qObj.getStr("key", ""), qObj.getStr(KEY_VALUE, "")));
        }
        return paramsList;
    }

    /**
     * 解析Postman的header数组
     */
    private static List<HttpHeader> parseHeaders(JSONArray headers) {
        if (headers == null || headers.isEmpty()) {
            return new ArrayList<>();
        }

        List<HttpHeader> headersList = new ArrayList<>();
        for (Object h : headers) {
            JSONObject hObj = (JSONObject) h;
            boolean enabled = !hObj.getBool(KEY_DISABLED, false);
            headersList.add(new HttpHeader(enabled, hObj.getStr("key", ""), hObj.getStr(KEY_VALUE, "")));
        }
        return headersList;
    }

    /**
     * 解析Postman的variable数组
     */
    private static List<Variable> parseVariables(JSONArray variables) {
        if (variables == null || variables.isEmpty()) {
            return new ArrayList<>();
        }

        List<Variable> variableList = new ArrayList<>();
        for (Object v : variables) {
            JSONObject vObj = (JSONObject) v;
            boolean enabled = !vObj.getBool(KEY_DISABLED, false);
            String key = vObj.getStr("key", "");
            String value = vObj.getStr(KEY_VALUE, "");
            variableList.add(new Variable(enabled, key, value));
        }
        return variableList;
    }

    /**
     * 解析Postman的formdata数组
     */
    private static List<HttpFormData> parseFormData(JSONArray arr) {
        if (arr == null || arr.isEmpty()) {
            return new ArrayList<>();
        }

        List<HttpFormData> formDataList = new ArrayList<>();
        for (Object o : arr) {
            JSONObject oObj = (JSONObject) o;
            String formType = oObj.getStr("type", "text");
            String key = oObj.getStr("key", "");
            boolean enabled = !oObj.getBool(KEY_DISABLED, false);
            if ("file".equals(formType)) {
                formDataList.add(new HttpFormData(enabled, key, HttpFormData.TYPE_FILE, oObj.getStr("src", "")));
            } else {
                formDataList.add(new HttpFormData(enabled, key, HttpFormData.TYPE_TEXT, oObj.getStr(KEY_VALUE, "")));
            }
        }
        return formDataList;
    }

    /**
     * 解析Postman的urlencoded数组
     */
    private static List<HttpFormUrlencoded> parseUrlencoded(JSONArray arr) {
        if (arr == null || arr.isEmpty()) {
            return new ArrayList<>();
        }

        List<HttpFormUrlencoded> urlencodedList = new ArrayList<>();
        for (Object o : arr) {
            JSONObject oObj = (JSONObject) o;
            boolean enabled = !oObj.getBool(KEY_DISABLED, false);
            urlencodedList.add(new HttpFormUrlencoded(enabled, oObj.getStr("key", ""), oObj.getStr(KEY_VALUE, "")));
        }
        return urlencodedList;
    }

    /**
     * 解析单个Postman请求item为HttpRequestItem
     */
    private static HttpRequestItem parsePostmanSingleItem(JSONObject item) {
        HttpRequestItem req = new HttpRequestItem();
        req.setId(UUID.randomUUID().toString());
        req.setName(item.getStr("name", "未命名请求"));
        JSONObject request = item.getJSONObject("request");
        if (request != null) {
            // 解析请求描述
            String description = request.getStr("description");
            if (description != null && !description.isEmpty()) {
                req.setDescription(description);
            }

            req.setMethod(request.getStr("method", "GET"));
            // url
            Object urlObj = request.get("url");
            if (urlObj instanceof JSONObject urlJson) {
                req.setUrl(urlJson.getStr("raw", ""));
                // 解析query参数
                JSONArray queryArr = urlJson.getJSONArray("query");
                List<HttpParam> params = parseQueryParams(queryArr);
                if (!params.isEmpty()) {
                    req.setParamsList(params);
                }
            } else if (urlObj instanceof String urlStr) {
                req.setUrl(urlStr);
            }
            // headers
            JSONArray headers = request.getJSONArray(KEY_HEADER);
            List<HttpHeader> headersList = parseHeaders(headers);
            if (headersList != null && !headersList.isEmpty()) {
                req.setHeadersList(headersList);
            }
            // auth
            JSONObject auth = request.getJSONObject("auth");
            if (auth != null) {
                String authType = auth.getStr("type", "");
                if ("basic".equals(authType)) {
                    req.setAuthType(AUTH_TYPE_BASIC);
                    JSONArray basicArr = auth.getJSONArray("basic");
                    String username = null, password = null;
                    if (basicArr != null) {
                        for (Object o : basicArr) {
                            JSONObject oObj = (JSONObject) o;
                            if ("username".equals(oObj.getStr("key"))) username = oObj.getStr("value", "");
                            if ("password".equals(oObj.getStr("key"))) password = oObj.getStr("value", "");
                        }
                    }
                    req.setAuthUsername(username);
                    req.setAuthPassword(password);
                } else if ("bearer".equals(authType)) {
                    req.setAuthType(AUTH_TYPE_BEARER);
                    JSONArray bearerArr = auth.getJSONArray("bearer");
                    if (bearerArr != null && !bearerArr.isEmpty()) {
                        for (Object o : bearerArr) {
                            JSONObject oObj = (JSONObject) o;
                            if ("token".equals(oObj.getStr("key"))) {
                                req.setAuthToken(oObj.getStr("value", ""));
                            }
                        }
                    }
                } else {
                    req.setAuthType(AUTH_TYPE_NONE);
                }
            }
            // body
            JSONObject body = request.getJSONObject("body");
            if (body != null) {
                String mode = body.getStr("mode", "");
                if ("raw".equals(mode)) {
                    req.setBody(body.getStr("raw", ""));
                } else if (MODE_FORMDATA.equals(mode)) {
                    List<HttpFormData> formDataList = parseFormData(body.getJSONArray(MODE_FORMDATA));
                    if (!formDataList.isEmpty()) {
                        req.setFormDataList(formDataList);
                    }
                } else if (MODE_URLENCODED.equals(mode)) {
                    List<HttpFormUrlencoded> urlencodedList = parseUrlencoded(body.getJSONArray(MODE_URLENCODED));
                    if (!urlencodedList.isEmpty()) {
                        req.setUrlencodedList(urlencodedList);
                    }
                }
            }
        }
        // event（如test脚本、pre-request脚本）
        JSONArray events = item.getJSONArray("event");
        if (events != null && !events.isEmpty()) {
            ScriptPair scripts = parseEventScripts(events);
            if (scripts.preScript != null) {
                req.setPrescript(scripts.preScript);
            }
            if (scripts.postScript != null) {
                req.setPostscript(scripts.postScript);
            }
        }

        // 解析 response（Postman 的 Examples/Saved Responses）
        JSONArray responses = item.getJSONArray("response");
        if (responses != null && !responses.isEmpty()) {
            List<SavedResponse> savedResponsesList = new ArrayList<>();
            for (Object respObj : responses) {
                JSONObject respJson = (JSONObject) respObj;
                SavedResponse savedResponse = parsePostmanResponse(respJson);
                if (savedResponse != null) {
                    savedResponsesList.add(savedResponse);
                }
            }
            req.setResponse(savedResponsesList);
        }

        return req;
    }

    /**
     * 解析 Postman 的单个响应（Example/Saved Response）
     */
    private static SavedResponse parsePostmanResponse(JSONObject respJson) {
        SavedResponse savedResponse = new SavedResponse();

        // 基本信息
        savedResponse.setId(respJson.getStr("_postman_previewlanguage", UUID.randomUUID().toString()));
        savedResponse.setName(respJson.getStr("name", "Response"));
        savedResponse.setTimestamp(System.currentTimeMillis());

        // 状态信息
        savedResponse.setCode(respJson.getInt("code", 200));
        savedResponse.setStatus(respJson.getStr("status", "OK"));
        savedResponse.setPreviewLanguage(respJson.getStr("_postman_previewlanguage", "text"));

        // 响应头
        JSONArray headers = respJson.getJSONArray(KEY_HEADER);
        List<HttpHeader> headersList = parseHeaders(headers);
        if (!headersList.isEmpty()) {
            savedResponse.setHeaders(headersList);
        }

        // 响应体
        savedResponse.setBody(respJson.getStr("body", ""));

        // 原始请求信息
        JSONObject originalRequest = respJson.getJSONObject("originalRequest");
        if (originalRequest != null) {
            SavedResponse.OriginalRequest origReq = new SavedResponse.OriginalRequest();
            origReq.setMethod(originalRequest.getStr("method", "GET"));

            // 解析 URL
            Object urlObj = originalRequest.get("url");
            if (urlObj instanceof JSONObject urlJson) {
                origReq.setUrl(urlJson.getStr("raw", ""));

                // 解析 query 参数
                List<HttpParam> params = parseQueryParams(urlJson.getJSONArray("query"));
                if (!params.isEmpty()) {
                    origReq.setParams(params);
                }
            } else if (urlObj instanceof String urlStr) {
                origReq.setUrl(urlStr);
            }

            // 解析请求头
            List<HttpHeader> reqHeaders = parseHeaders(originalRequest.getJSONArray(KEY_HEADER));
            if (!reqHeaders.isEmpty()) {
                origReq.setHeaders(reqHeaders);
            }

            // 解析请求体
            JSONObject body = originalRequest.getJSONObject("body");
            if (body != null) {
                String mode = body.getStr("mode", "");
                origReq.setBodyType(mode);

                if ("raw".equals(mode)) {
                    origReq.setBody(body.getStr("raw", ""));
                } else if (MODE_FORMDATA.equals(mode)) {
                    List<HttpFormData> formDataList = parseFormData(body.getJSONArray(MODE_FORMDATA));
                    if (!formDataList.isEmpty()) {
                        origReq.setFormDataList(formDataList);
                    }
                } else if (MODE_URLENCODED.equals(mode)) {
                    List<HttpFormUrlencoded> urlencodedList = parseUrlencoded(body.getJSONArray(MODE_URLENCODED));
                    if (!urlencodedList.isEmpty()) {
                        origReq.setUrlencodedList(urlencodedList);
                    }
                }
            }

            savedResponse.setOriginalRequest(origReq);
        }

        return savedResponse;
    }
}
