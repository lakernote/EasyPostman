package com.laker.postman.service.postman;

import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.model.Environment;
import com.laker.postman.model.HttpRequestItem;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

/**
 * Postman导入工具类
 */
public class PostmanImport {

    /**
     * 解析Postman环境变量JSON，转换为Environment列表
     */
    public static List<Environment> parsePostmanEnvironments(String json) {
        List<Environment> result = new ArrayList<>();
        // 兼容单个对象或数组
        if (JSONUtil.isTypeJSONArray(json)) {
            JSONArray arr = JSONUtil.parseArray(json);
            for (Object obj : arr) {
                parseSingleEnvObj((JSONObject) obj, result);
            }
        } else {
            JSONObject envObj = JSONUtil.parseObj(json);
            parseSingleEnvObj(envObj, result);
        }
        return result;
    }

    private static void parseSingleEnvObj(JSONObject envObj, List<Environment> result) {
        String name = envObj.getStr("name", "未命名环境");
        Environment env = new Environment(name);
        env.setId(UUID.randomUUID().toString());
        JSONArray values = envObj.getJSONArray("values");
        if (values != null) {
            for (Object v : values) {
                JSONObject vObj = (JSONObject) v;
                String key = vObj.getStr("key", "");
                String value = vObj.getStr("value", "");
                boolean enabled = vObj.getBool("enabled", true);
                if (!key.isEmpty() && enabled) {
                    env.addVariable(key, value);
                }
            }
        }
        result.add(env);
    }

    /**
     * 解析单个Postman请求item为HttpRequestItem
     */
    public static HttpRequestItem parsePostmanSingleItem(JSONObject item) {
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
                    Map<String, String> params = new LinkedHashMap<>();
                    for (Object q : queryArr) {
                        JSONObject qObj = (JSONObject) q;
                        if (!qObj.getBool("disabled", false)) {
                            params.put(qObj.getStr("key", ""), qObj.getStr("value", ""));
                        }
                    }
                    req.setParams(params);
                }
            } else if (urlObj instanceof String urlStr) {
                req.setUrl(urlStr);
            }
            // headers
            JSONArray headers = request.getJSONArray("header");
            if (headers != null && !headers.isEmpty()) {
                Map<String, String> headerMap = new LinkedHashMap<>();
                for (Object h : headers) {
                    JSONObject hObj = (JSONObject) h;
                    if (!hObj.getBool("disabled", false)) {
                        headerMap.put(hObj.getStr("key", ""), hObj.getStr("value", ""));
                    }
                }
                req.setHeaders(headerMap);
            }
            // auth
            JSONObject auth = request.getJSONObject("auth");
            if (auth != null) {
                String authType = auth.getStr("type", "");
                if ("basic".equals(authType)) {
                    req.setAuthType("basic");
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
                    req.setAuthType("bearer");
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
                    req.setAuthType("none");
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
                        Map<String, String> formData = new LinkedHashMap<>();
                        Map<String, String> formFiles = new LinkedHashMap<>();
                        for (Object o : arr) {
                            JSONObject oObj = (JSONObject) o;
                            String formType = oObj.getStr("type", "text");
                            String key = oObj.getStr("key", "");
                            if (!oObj.getBool("disabled", false)) {
                                if ("file".equals(formType)) {
                                    formFiles.put(key, oObj.getStr("src", ""));
                                } else {
                                    formData.put(key, oObj.getStr("value", ""));
                                }
                            }
                        }
                        req.setFormData(formData);
                        req.setFormFiles(formFiles);
                    }
                } else if ("urlencoded".equals(mode)) {
                    JSONArray arr = body.getJSONArray("urlencoded");
                    if (arr != null && !arr.isEmpty()) {
                        Map<String, String> urlencoded = new LinkedHashMap<>();
                        for (Object o : arr) {
                            JSONObject oObj = (JSONObject) o;
                            if (!oObj.getBool("disabled", false)) {
                                urlencoded.put(oObj.getStr("key", ""), oObj.getStr("value", ""));
                            }
                        }
                        req.setUrlencoded(urlencoded);
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
        return req;
    }

    // ========== Postman Collection 导出相关 ==========

    /**
     * 从树节点递归构建 Postman Collection v2.1 JSON
     *
     * @param groupNode 分组节点
     * @param groupName 分组名
     * @return Postman Collection v2.1 JSONObject
     */
    public static JSONObject buildPostmanCollectionFromTreeNode(DefaultMutableTreeNode groupNode, String groupName) {
        JSONObject collection = new JSONObject();
        JSONObject info = new JSONObject();
        info.put("_postman_id", UUID.randomUUID().toString());
        info.put("name", groupName);
        info.put("schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json");
        collection.put("info", info);
        collection.put("item", buildPostmanItemsFromNode(groupNode));
        return collection;
    }

    /**
     * 递归构建 Postman items 数组
     */
    private static JSONArray buildPostmanItemsFromNode(DefaultMutableTreeNode node) {
        JSONArray items = new JSONArray();
        for (int i = 0; i < node.getChildCount(); i++) {
            javax.swing.tree.DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof Object[] obj2) {
                if ("group".equals(obj2[0])) {
                    String subGroupName = String.valueOf(obj2[1]);
                    JSONObject folder = new JSONObject();
                    folder.put("name", subGroupName);
                    folder.put("item", buildPostmanItemsFromNode(child));
                    items.add(folder);
                } else if ("request".equals(obj2[0])) {
                    items.add(toPostmanItem((com.laker.postman.model.HttpRequestItem) obj2[1]));
                }
            }
        }
        return items;
    }

    /**
     * HttpRequestItem 转 Postman 单请求 item
     */
    public static JSONObject toPostmanItem(HttpRequestItem item) {
        JSONObject postmanItem = new JSONObject();
        postmanItem.put("name", item.getName());
        JSONObject request = new JSONObject();
        request.put("method", item.getMethod());
        // url
        JSONObject url = new JSONObject();
        String rawUrl = item.getUrl();
        url.put("raw", rawUrl);
        try {
            java.net.URI uri = new java.net.URI(rawUrl);
            // protocol
            if (uri.getScheme() != null) {
                url.put("protocol", uri.getScheme());
            }
            // host
            if (uri.getHost() != null) {
                String[] hostParts = uri.getHost().split("\\.");
                url.put("host", Arrays.asList(hostParts));
            }
            // path
            if (uri.getPath() != null && !uri.getPath().isEmpty()) {
                String[] pathParts = uri.getPath().replaceFirst("^/", "").split("/");
                url.put("path", Arrays.asList(pathParts));
            }
            // query
            if (uri.getQuery() != null && !uri.getQuery().isEmpty()) {
                JSONArray queryArr = new JSONArray();
                String[] pairs = uri.getQuery().split("&");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    JSONObject q = new JSONObject();
                    q.put("key", kv[0]);
                    q.put("value", kv.length > 1 ? kv[1] : "");
                    queryArr.add(q);
                }
                url.put("query", queryArr);
            }
        } catch (Exception ignore) {
        }
        // 额外合并 params
        if (item.getParams() != null && !item.getParams().isEmpty()) {
            JSONArray queryArr = url.containsKey("query") ? url.getJSONArray("query") : new JSONArray();
            for (Map.Entry<String, String> entry : item.getParams().entrySet()) {
                JSONObject q = new JSONObject();
                q.put("key", entry.getKey());
                q.put("value", entry.getValue());
                queryArr.add(q);
            }
            url.put("query", queryArr);
        }
        request.put("url", url);
        // headers
        if (item.getHeaders() != null && !item.getHeaders().isEmpty()) {
            JSONArray headerArr = new JSONArray();
            for (Map.Entry<String, String> entry : item.getHeaders().entrySet()) {
                JSONObject h = new JSONObject();
                h.put("key", entry.getKey());
                h.put("value", entry.getValue());
                headerArr.add(h);
            }
            request.put("header", headerArr);
        }
        // body
        if (item.getBody() != null && !item.getBody().isEmpty()) {
            JSONObject body = new JSONObject();
            body.put("mode", "raw");
            body.put("raw", item.getBody());
            request.put("body", body);
        } else if (item.getFormData() != null && !item.getFormData().isEmpty()) {
            JSONObject body = new JSONObject();
            body.put("mode", "formdata");
            JSONArray arr = new JSONArray();
            for (Map.Entry<String, String> entry : item.getFormData().entrySet()) {
                JSONObject o = new JSONObject();
                o.put("key", entry.getKey());
                o.put("value", entry.getValue());
                o.put("type", "text");
                arr.add(o);
            }
            if (item.getFormFiles() != null && !item.getFormFiles().isEmpty()) {
                for (Map.Entry<String, String> entry : item.getFormFiles().entrySet()) {
                    JSONObject o = new JSONObject();
                    o.put("key", entry.getKey());
                    o.put("src", entry.getValue());
                    o.put("type", "file");
                    arr.add(o);
                }
            }
            body.put("formdata", arr);
            request.put("body", body);
        }
        // urlencoded
        else if (MapUtil.isNotEmpty(item.getUrlencoded())) {
            JSONObject body = new JSONObject();
            body.put("mode", "urlencoded");
            JSONArray arr = new JSONArray();
            for (Map.Entry<String, String> entry : item.getUrlencoded().entrySet()) {
                JSONObject o = new JSONObject();
                o.put("key", entry.getKey());
                o.put("value", entry.getValue());
                o.put("type", "text");
                arr.add(o);
            }
            body.put("urlencoded", arr);
            request.put("body", body);
        }
        // auth
        if (item.getAuthType() != null && !"none".equals(item.getAuthType())) {
            JSONObject auth = new JSONObject();
            if ("basic".equals(item.getAuthType())) {
                auth.put("type", "basic");
                JSONArray arr = new JSONArray();
                arr.add(new JSONObject().put("key", "username").put("value", item.getAuthUsername()));
                arr.add(new JSONObject().put("key", "password").put("value", item.getAuthPassword()));
                auth.put("basic", arr);
            } else if ("bearer".equals(item.getAuthType())) {
                auth.put("type", "bearer");
                JSONArray arr = new JSONArray();
                arr.add(new JSONObject().put("key", "token").put("value", item.getAuthToken()));
                auth.put("bearer", arr);
            }
            request.put("auth", auth);
        }
        postmanItem.put("request", request);
        // 脚本 event
        JSONArray events = new JSONArray();
        if (item.getPrescript() != null && !item.getPrescript().isEmpty()) {
            JSONObject pre = new JSONObject();
            pre.put("listen", "prerequest");
            JSONObject script = new JSONObject();
            script.put("type", "text/javascript");
            script.put("exec", Arrays.asList(item.getPrescript().split("\n")));
            pre.put("script", script);
            events.add(pre);
        }
        if (item.getPostscript() != null && !item.getPostscript().isEmpty()) {
            JSONObject post = new JSONObject();
            post.put("listen", "test");
            JSONObject script = new JSONObject();
            script.put("type", "text/javascript");
            script.put("exec", Arrays.asList(item.getPostscript().split("\n")));
            post.put("script", script);
            events.add(post);
        }
        if (!events.isEmpty()) {
            postmanItem.put("event", events);
        }
        return postmanItem;
    }

    /**
     * 导出单个环境为Postman环境变量JSON字符串
     */
    public static String toPostmanEnvironmentJson(Environment env) {
        JSONObject obj = new JSONObject();
        obj.put("id", UUID.randomUUID().toString());
        obj.put("name", env.getName());
        JSONArray values = new JSONArray();
        if (env.getVariables() != null) {
            for (Map.Entry<String, String> entry : env.getVariables().entrySet()) {
                JSONObject v = new JSONObject();
                v.put("key", entry.getKey());
                v.put("value", entry.getValue());
                v.put("enabled", true);
                values.add(v);
            }
        }
        obj.put("values", values);
        obj.put("_postman_variable_scope", "environment");
        obj.put("_postman_exported_at", java.time.ZonedDateTime.now().toString());
        obj.put("_postman_exported_using", "EasyPostman");
        return obj.toStringPretty();
    }
}