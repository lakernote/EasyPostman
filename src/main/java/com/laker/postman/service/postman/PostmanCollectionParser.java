package com.laker.postman.service.postman;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.model.*;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.*;

/**
 * Postman Collection解析器
 * 负责解析Postman Collection格式的请求（v2.1格式）
 */
@Slf4j
public class PostmanCollectionParser {

    // 常量定义
    private static final String GROUP = "group";
    private static final String REQUEST = "request";

    /**
     * 私有构造函数，防止实例化
     */
    private PostmanCollectionParser() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 解析完整的 Postman Collection JSON，返回根节点
     *
     * @param json Postman Collection JSON 字符串
     * @return 集合根节点，如果解析失败返回 null
     */
    public static DefaultMutableTreeNode parsePostmanCollection(String json) {
        try {
            JSONObject postmanRoot = JSONUtil.parseObj(json);
            if (postmanRoot.containsKey("info") && postmanRoot.containsKey("item")) {
                // 解析 collection 名称
                String collectionName = postmanRoot.getJSONObject("info").getStr("name", "Postman");
                RequestGroup collectionGroup = new RequestGroup(collectionName);

                // 解析集合级别的认证
                if (postmanRoot.containsKey("auth")) {
                    parseAuthToGroup(postmanRoot.getJSONObject("auth"), collectionGroup);
                }

                // 解析集合级别的脚本
                if (postmanRoot.containsKey("event")) {
                    parseEventsToGroup(postmanRoot.getJSONArray("event"), collectionGroup);
                }

                JSONArray items = postmanRoot.getJSONArray("item");
                DefaultMutableTreeNode collectionNode = new DefaultMutableTreeNode(new Object[]{GROUP, collectionGroup});
                // 递归解析树结构
                List<DefaultMutableTreeNode> children = parsePostmanItemsToTree(items);
                for (DefaultMutableTreeNode child : children) {
                    collectionNode.add(child);
                }
                return collectionNode;
            }
        } catch (Exception e) {
            // 返回 null 表示解析失败
            log.error("解析Postman Collection失败", e);
            return null;
        }
        return null;
    }

    /**
     * 递归解析Postman集合为树结构，返回标准分组/请求节点列表
     *
     * @param items Postman collection 的 item 数组
     * @return 树节点列表
     */
    private static List<DefaultMutableTreeNode> parsePostmanItemsToTree(JSONArray items) {
        List<DefaultMutableTreeNode> nodeList = new ArrayList<>();
        for (Object obj : items) {
            JSONObject item = (JSONObject) obj;

            // 判断是文件夹还是请求
            // 文件夹：有 item 字段
            // 请求：有 request 字段
            if (item.containsKey("item")) {
                // 文件夹节点
                String folderName = item.getStr("name", "default group");
                RequestGroup group = new RequestGroup(folderName);

                // 解析分组级别的认证
                JSONObject auth = item.getJSONObject("auth");
                if (auth != null) {
                    parseAuthToGroup(auth, group);
                }

                // 解析分组级别的脚本
                JSONArray events = item.getJSONArray("event");
                if (events != null && !events.isEmpty()) {
                    parseEventsToGroup(events, group);
                }

                DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(new Object[]{GROUP, group});

                // 递归处理子节点
                JSONArray children = item.getJSONArray("item");
                List<DefaultMutableTreeNode> childNodes = parsePostmanItemsToTree(children);
                for (DefaultMutableTreeNode child : childNodes) {
                    folderNode.add(child);
                }
                nodeList.add(folderNode);
            } else if (item.containsKey(REQUEST)) {
                // 纯请求节点
                HttpRequestItem req = parsePostmanSingleItem(item);
                DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(new Object[]{REQUEST, req});

                // 为响应创建子节点（类似 RequestsPersistence 的逻辑）
                if (req.getResponse() != null && !req.getResponse().isEmpty()) {
                    for (SavedResponse savedResp : req.getResponse()) {
                        DefaultMutableTreeNode responseNode = new DefaultMutableTreeNode(
                                new Object[]{"response", savedResp}
                        );
                        requestNode.add(responseNode);
                    }
                }

                nodeList.add(requestNode);
            }
        }
        return nodeList;
    }

    /**
     * 解析Postman的auth到RequestGroup
     */
    private static void parseAuthToGroup(JSONObject auth, RequestGroup group) {
        String authType = auth.getStr("type", "");
        if ("basic".equals(authType)) {
            group.setAuthType(AUTH_TYPE_BASIC);
            JSONArray basicArr = auth.getJSONArray("basic");
            String username = null, password = null;
            if (basicArr != null) {
                for (Object o : basicArr) {
                    JSONObject oObj = (JSONObject) o;
                    if ("username".equals(oObj.getStr("key"))) username = oObj.getStr("value", "");
                    if ("password".equals(oObj.getStr("key"))) password = oObj.getStr("value", "");
                }
            }
            group.setAuthUsername(username);
            group.setAuthPassword(password);
        } else if ("bearer".equals(authType)) {
            group.setAuthType(AUTH_TYPE_BEARER);
            JSONArray bearerArr = auth.getJSONArray("bearer");
            if (bearerArr != null && !bearerArr.isEmpty()) {
                for (Object o : bearerArr) {
                    JSONObject oObj = (JSONObject) o;
                    if ("token".equals(oObj.getStr("key"))) {
                        group.setAuthToken(oObj.getStr("value", ""));
                    }
                }
            }
        }
    }

    /**
     * 解析Postman的events到RequestGroup
     */
    private static void parseEventsToGroup(JSONArray events, RequestGroup group) {
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
        if (!preScript.isEmpty()) {
            group.setPrescript(preScript.toString());
        }
        if (!testScript.isEmpty()) {
            group.setPostscript(testScript.toString());
        }
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
            req.setMethod(request.getStr("method", "GET"));
            // url
            Object urlObj = request.get("url");
            if (urlObj instanceof JSONObject urlJson) {
                req.setUrl(urlJson.getStr("raw", ""));
                // 解析query参数
                JSONArray queryArr = urlJson.getJSONArray("query");
                if (queryArr != null) {
                    List<HttpParam> paramsList = new ArrayList<>();
                    for (Object q : queryArr) {
                        JSONObject qObj = (JSONObject) q;
                        boolean enabled = !qObj.getBool("disabled", false);
                        paramsList.add(new HttpParam(enabled, qObj.getStr("key", ""), qObj.getStr("value", "")));
                    }
                    req.setParamsList(paramsList);
                }
            } else if (urlObj instanceof String urlStr) {
                req.setUrl(urlStr);
            }
            // headers
            JSONArray headers = request.getJSONArray("header");
            if (headers != null && !headers.isEmpty()) {
                List<HttpHeader> headersList = new ArrayList<>();
                for (Object h : headers) {
                    JSONObject hObj = (JSONObject) h;
                    boolean enabled = !hObj.getBool("disabled", false);
                    headersList.add(new HttpHeader(enabled, hObj.getStr("key", ""), hObj.getStr("value", "")));
                }
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
                } else if ("formdata".equals(mode)) {
                    JSONArray arr = body.getJSONArray("formdata");
                    if (arr != null && !arr.isEmpty()) {
                        List<HttpFormData> formDataList = new ArrayList<>();
                        for (Object o : arr) {
                            JSONObject oObj = (JSONObject) o;
                            String formType = oObj.getStr("type", "text");
                            String key = oObj.getStr("key", "");
                            boolean enabled = !oObj.getBool("disabled", false);
                            if ("file".equals(formType)) {
                                formDataList.add(new HttpFormData(enabled, key, HttpFormData.TYPE_FILE, oObj.getStr("src", "")));
                            } else {
                                formDataList.add(new HttpFormData(enabled, key, HttpFormData.TYPE_TEXT, oObj.getStr("value", "")));
                            }
                        }
                        req.setFormDataList(formDataList);
                    }
                } else if ("urlencoded".equals(mode)) {
                    JSONArray arr = body.getJSONArray("urlencoded");
                    if (arr != null && !arr.isEmpty()) {
                        List<HttpFormUrlencoded> urlencodedList = new ArrayList<>();
                        for (Object o : arr) {
                            JSONObject oObj = (JSONObject) o;
                            boolean enabled = !oObj.getBool("disabled", false);
                            urlencodedList.add(new HttpFormUrlencoded(enabled, oObj.getStr("key", ""), oObj.getStr("value", "")));
                        }
                        req.setUrlencodedList(urlencodedList);
                    }
                }
            }
        }
        // event（如test脚本、pre-request脚本）
        JSONArray events = item.getJSONArray("event");
        if (events != null && !events.isEmpty()) {
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
            if (!preScript.isEmpty()) {
                req.setPrescript(preScript.toString());
            }
            if (!testScript.isEmpty()) {
                req.setPostscript(testScript.toString());
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
        JSONArray headers = respJson.getJSONArray("header");
        if (headers != null && !headers.isEmpty()) {
            List<HttpHeader> headersList = new ArrayList<>();
            for (Object h : headers) {
                JSONObject hObj = (JSONObject) h;
                headersList.add(new HttpHeader(true, hObj.getStr("key", ""), hObj.getStr("value", "")));
            }
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
                JSONArray queryArr = urlJson.getJSONArray("query");
                if (queryArr != null) {
                    List<HttpParam> paramsList = new ArrayList<>();
                    for (Object q : queryArr) {
                        JSONObject qObj = (JSONObject) q;
                        boolean enabled = !qObj.getBool("disabled", false);
                        paramsList.add(new HttpParam(enabled, qObj.getStr("key", ""), qObj.getStr("value", "")));
                    }
                    origReq.setParams(paramsList);
                }
            } else if (urlObj instanceof String urlStr) {
                origReq.setUrl(urlStr);
            }

            // 解析请求头
            JSONArray reqHeaders = originalRequest.getJSONArray("header");
            if (reqHeaders != null && !reqHeaders.isEmpty()) {
                List<HttpHeader> headersList = new ArrayList<>();
                for (Object h : reqHeaders) {
                    JSONObject hObj = (JSONObject) h;
                    boolean enabled = !hObj.getBool("disabled", false);
                    headersList.add(new HttpHeader(enabled, hObj.getStr("key", ""), hObj.getStr("value", "")));
                }
                origReq.setHeaders(headersList);
            }

            // 解析请求体
            JSONObject body = originalRequest.getJSONObject("body");
            if (body != null) {
                String mode = body.getStr("mode", "");
                origReq.setBodyType(mode);

                if ("raw".equals(mode)) {
                    origReq.setBody(body.getStr("raw", ""));
                } else if ("formdata".equals(mode)) {
                    JSONArray arr = body.getJSONArray("formdata");
                    if (arr != null && !arr.isEmpty()) {
                        List<HttpFormData> formDataList = new ArrayList<>();
                        for (Object o : arr) {
                            JSONObject oObj = (JSONObject) o;
                            String formType = oObj.getStr("type", "text");
                            String key = oObj.getStr("key", "");
                            boolean enabled = !oObj.getBool("disabled", false);
                            if ("file".equals(formType)) {
                                formDataList.add(new HttpFormData(enabled, key, HttpFormData.TYPE_FILE, oObj.getStr("src", "")));
                            } else {
                                formDataList.add(new HttpFormData(enabled, key, HttpFormData.TYPE_TEXT, oObj.getStr("value", "")));
                            }
                        }
                        origReq.setFormDataList(formDataList);
                    }
                } else if ("urlencoded".equals(mode)) {
                    JSONArray arr = body.getJSONArray("urlencoded");
                    if (arr != null && !arr.isEmpty()) {
                        List<HttpFormUrlencoded> urlencodedList = new ArrayList<>();
                        for (Object o : arr) {
                            JSONObject oObj = (JSONObject) o;
                            boolean enabled = !oObj.getBool("disabled", false);
                            urlencodedList.add(new HttpFormUrlencoded(enabled, oObj.getStr("key", ""), oObj.getStr("value", "")));
                        }
                        origReq.setUrlencodedList(urlencodedList);
                    }
                }
            }

            savedResponse.setOriginalRequest(origReq);
        }

        return savedResponse;
    }
}
