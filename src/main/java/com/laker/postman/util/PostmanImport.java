package com.laker.postman.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.model.Environment;
import com.laker.postman.model.HttpRequestItem;

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
                        Map<String, String> formData = new LinkedHashMap<>();
                        for (Object o : arr) {
                            JSONObject oObj = (JSONObject) o;
                            if (!oObj.getBool("disabled", false)) {
                                formData.put(oObj.getStr("key", ""), oObj.getStr("value", ""));
                            }
                        }
                        req.setFormData(formData);
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
}