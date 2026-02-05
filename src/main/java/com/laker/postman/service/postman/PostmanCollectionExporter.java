package com.laker.postman.service.postman;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.laker.postman.model.*;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Arrays;
import java.util.UUID;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.*;

/**
 * Postman Collection导出器
 * 负责将内部数据结构导出为Postman Collection格式（v2.1格式）
 */
@UtilityClass
public class PostmanCollectionExporter {

    /**
     * 从树节点递归构建 Postman Collection v2.1 JSON
     *
     * @param groupNode 分组节点
     * @param groupName 分组名（如果根节点有 RequestGroup，会使用其名称）
     * @return Postman Collection v2.1 JSONObject
     */
    public static JSONObject buildPostmanCollectionFromTreeNode(DefaultMutableTreeNode groupNode, String groupName) {
        JSONObject collection = new JSONObject();
        JSONObject info = new JSONObject();
        info.put("_postman_id", UUID.randomUUID().toString());

        // 检查根节点是否有 RequestGroup 对象
        Object userObj = groupNode.getUserObject();
        RequestGroup rootGroup = null;
        if (userObj instanceof Object[] obj && "group".equals(obj[0])) {
            Object groupData = obj[1];
            if (groupData instanceof RequestGroup group) {
                rootGroup = group;
                groupName = group.getName(); // 使用 RequestGroup 的名称
            }
        }

        info.put("name", groupName);
        info.put("schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json");

        // 导出集合描述
        if (rootGroup != null && rootGroup.getDescription() != null && !rootGroup.getDescription().isEmpty()) {
            info.put("description", rootGroup.getDescription());
        }

        collection.put("info", info);

        // 导出集合级别的认证（从根节点）
        if (rootGroup != null && rootGroup.hasAuth()) {
            JSONObject auth = new JSONObject();
            if (AUTH_TYPE_BASIC.equals(rootGroup.getAuthType())) {
                auth.put("type", "basic");
                JSONArray arr = new JSONArray();
                arr.add(new JSONObject().put("key", "username").put("value", rootGroup.getAuthUsername()));
                arr.add(new JSONObject().put("key", "password").put("value", rootGroup.getAuthPassword()));
                auth.put("basic", arr);
            } else if (AUTH_TYPE_BEARER.equals(rootGroup.getAuthType())) {
                auth.put("type", "bearer");
                JSONArray arr = new JSONArray();
                arr.add(new JSONObject().put("key", "token").put("value", rootGroup.getAuthToken()));
                auth.put("bearer", arr);
            }
            collection.put("auth", auth);
        }

        // 导出集合级别的脚本（从根节点）
        if (rootGroup != null && (rootGroup.hasPreScript() || rootGroup.hasPostScript())) {
            JSONArray events = new JSONArray();
            if (rootGroup.hasPreScript()) {
                JSONObject pre = new JSONObject();
                pre.put("listen", "prerequest");
                JSONObject script = new JSONObject();
                script.put("type", "text/javascript");
                script.put("exec", Arrays.asList(rootGroup.getPrescript().split("\n")));
                pre.put("script", script);
                events.add(pre);
            }
            if (rootGroup.hasPostScript()) {
                JSONObject post = new JSONObject();
                post.put("listen", "test");
                JSONObject script = new JSONObject();
                script.put("type", "text/javascript");
                script.put("exec", Arrays.asList(rootGroup.getPostscript().split("\n")));
                post.put("script", script);
                events.add(post);
            }
            collection.put("event", events);
        }

        // 导出集合级别的变量（从根节点）
        if (rootGroup != null && rootGroup.hasVariables()) {
            JSONArray variables = new JSONArray();
            for (Variable variable : rootGroup.getVariables()) {
                JSONObject varObj = new JSONObject();
                varObj.put("key", variable.getKey());
                varObj.put("value", variable.getValue());
                if (!variable.isEnabled()) {
                    varObj.put("disabled", true);
                }
                variables.add(varObj);
            }
            collection.put("variable", variables);
        }

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
                    JSONObject folder = new JSONObject();

                    // 处理分组对象
                    Object groupData = obj2[1];
                    if (groupData instanceof RequestGroup group) {
                        // 新格式：使用RequestGroup对象
                        folder.put("name", group.getName());

                        // 导出分组描述
                        if (group.getDescription() != null && !group.getDescription().isEmpty()) {
                            folder.put("description", group.getDescription());
                        }

                        // 导出分组级别的认证
                        if (group.hasAuth()) {
                            JSONObject auth = new JSONObject();
                            if (AUTH_TYPE_BASIC.equals(group.getAuthType())) {
                                auth.put("type", "basic");
                                JSONArray arr = new JSONArray();
                                arr.add(new JSONObject().put("key", "username").put("value", group.getAuthUsername()));
                                arr.add(new JSONObject().put("key", "password").put("value", group.getAuthPassword()));
                                auth.put("basic", arr);
                            } else if (AUTH_TYPE_BEARER.equals(group.getAuthType())) {
                                auth.put("type", "bearer");
                                JSONArray arr = new JSONArray();
                                arr.add(new JSONObject().put("key", "token").put("value", group.getAuthToken()));
                                auth.put("bearer", arr);
                            }
                            folder.put("auth", auth);
                        }

                        // 导出分组级别的脚本
                        JSONArray events = new JSONArray();
                        if (group.hasPreScript()) {
                            JSONObject pre = new JSONObject();
                            pre.put("listen", "prerequest");
                            JSONObject script = new JSONObject();
                            script.put("type", "text/javascript");
                            script.put("exec", Arrays.asList(group.getPrescript().split("\n")));
                            pre.put("script", script);
                            events.add(pre);
                        }
                        if (group.hasPostScript()) {
                            JSONObject post = new JSONObject();
                            post.put("listen", "test");
                            JSONObject script = new JSONObject();
                            script.put("type", "text/javascript");
                            script.put("exec", Arrays.asList(group.getPostscript().split("\n")));
                            post.put("script", script);
                            events.add(post);
                        }
                        if (!events.isEmpty()) {
                            folder.put("event", events);
                        }
                    } else if (groupData instanceof String subGroupName) {
                        // 旧格式兼容：字符串名称
                        folder.put("name", subGroupName);
                    }

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

        // 导出请求描述
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            request.put("description", item.getDescription());
        }

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
        if (item.getParamsList() != null && !item.getParamsList().isEmpty()) {
            JSONArray queryArr = url.containsKey("query") ? url.getJSONArray("query") : new JSONArray();
            for (HttpParam param : item.getParamsList()) {
                if (param.isEnabled()) {
                    JSONObject q = new JSONObject();
                    q.put("key", param.getKey());
                    q.put("value", param.getValue());
                    queryArr.add(q);
                }
            }
            url.put("query", queryArr);
        }
        request.put("url", url);
        // headers
        if (item.getHeadersList() != null && !item.getHeadersList().isEmpty()) {
            JSONArray headerArr = new JSONArray();
            for (HttpHeader header : item.getHeadersList()) {
                if (header.isEnabled()) {
                    JSONObject h = new JSONObject();
                    h.put("key", header.getKey());
                    h.put("value", header.getValue());
                    headerArr.add(h);
                }
            }
            request.put("header", headerArr);
        }
        // body
        if (item.getBody() != null && !item.getBody().isEmpty()) {
            JSONObject body = new JSONObject();
            body.put("mode", "raw");
            body.put("raw", item.getBody());
            request.put("body", body);
        } else if (item.getFormDataList() != null && !item.getFormDataList().isEmpty()) {
            JSONObject body = new JSONObject();
            body.put("mode", "formdata");
            JSONArray arr = new JSONArray();
            for (HttpFormData formData : item.getFormDataList()) {
                if (formData.isEnabled()) {
                    JSONObject o = new JSONObject();
                    o.put("key", formData.getKey());
                    if (formData.isText()) {
                        o.put("value", formData.getValue());
                        o.put("type", "text");
                    } else if (formData.isFile()) {
                        o.put("src", formData.getValue());
                        o.put("type", "file");
                    }
                    arr.add(o);
                }
            }
            body.put("formdata", arr);
            request.put("body", body);
        }
        // urlencoded
        else if (item.getUrlencodedList() != null && !item.getUrlencodedList().isEmpty()) {
            JSONObject body = new JSONObject();
            body.put("mode", "urlencoded");
            JSONArray arr = new JSONArray();
            for (HttpFormUrlencoded encoded : item.getUrlencodedList()) {
                if (encoded.isEnabled()) {
                    JSONObject o = new JSONObject();
                    o.put("key", encoded.getKey());
                    o.put("value", encoded.getValue());
                    o.put("type", "text");
                    arr.add(o);
                }
            }
            body.put("urlencoded", arr);
            request.put("body", body);
        }
        // auth
        if (item.getAuthType() != null && !AUTH_TYPE_NONE.equals(item.getAuthType())) {
            JSONObject auth = new JSONObject();
            if (AUTH_TYPE_BASIC.equals(item.getAuthType())) {
                auth.put("type", "basic");
                JSONArray arr = new JSONArray();
                arr.add(new JSONObject().put("key", "username").put("value", item.getAuthUsername()));
                arr.add(new JSONObject().put("key", "password").put("value", item.getAuthPassword()));
                auth.put("basic", arr);
            } else if (AUTH_TYPE_BEARER.equals(item.getAuthType())) {
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

        // 导出 response（Postman 的 Examples/Saved Responses）
        if (item.getResponse() != null && !item.getResponse().isEmpty()) {
            JSONArray responsesArray = new JSONArray();
            for (SavedResponse savedResp : item.getResponse()) {
                JSONObject respJson = toPostmanResponse(savedResp);
                responsesArray.add(respJson);
            }
            postmanItem.put("response", responsesArray);
        }

        return postmanItem;
    }

    /**
     * 将 SavedResponse 转换为 Postman 响应格式
     */
    private static JSONObject toPostmanResponse(SavedResponse savedResp) {
        JSONObject respJson = new JSONObject();

        // 基本信息
        respJson.put("name", savedResp.getName());
        respJson.put("status", savedResp.getStatus());
        respJson.put("code", savedResp.getCode());

        if (savedResp.getPreviewLanguage() != null && !savedResp.getPreviewLanguage().isEmpty()) {
            respJson.put("_postman_previewlanguage", savedResp.getPreviewLanguage());
        }

        // 响应头
        if (savedResp.getHeaders() != null && !savedResp.getHeaders().isEmpty()) {
            JSONArray headersArray = new JSONArray();
            for (HttpHeader header : savedResp.getHeaders()) {
                JSONObject headerObj = new JSONObject();
                headerObj.put("key", header.getKey());
                headerObj.put("value", header.getValue());
                headersArray.add(headerObj);
            }
            respJson.put("header", headersArray);
        }

        // Cookies (如果有)
        if (savedResp.getCookies() != null && !savedResp.getCookies().isEmpty()) {
            JSONArray cookiesArray = new JSONArray();
            // Postman cookie 格式可以在这里添加
            respJson.put("cookie", cookiesArray);
        }

        // 响应体
        if (savedResp.getBody() != null) {
            respJson.put("body", savedResp.getBody());
        }

        // 原始请求信息
        if (savedResp.getOriginalRequest() != null) {
            SavedResponse.OriginalRequest origReq = savedResp.getOriginalRequest();
            JSONObject originalRequest = new JSONObject();

            originalRequest.put("method", origReq.getMethod());

            // URL
            JSONObject url = new JSONObject();
            url.put("raw", origReq.getUrl());

            // 解析 URL 的各个部分
            try {
                java.net.URI uri = new java.net.URI(origReq.getUrl());
                if (uri.getScheme() != null) {
                    url.put("protocol", uri.getScheme());
                }
                if (uri.getHost() != null) {
                    String[] hostParts = uri.getHost().split("\\.");
                    url.put("host", Arrays.asList(hostParts));
                }
                if (uri.getPath() != null && !uri.getPath().isEmpty()) {
                    String[] pathParts = uri.getPath().replaceFirst("^/", "").split("/");
                    url.put("path", Arrays.asList(pathParts));
                }
            } catch (Exception ignore) {
            }

            // Query 参数
            if (origReq.getParams() != null && !origReq.getParams().isEmpty()) {
                JSONArray queryArray = new JSONArray();
                for (HttpParam param : origReq.getParams()) {
                    JSONObject paramObj = new JSONObject();
                    paramObj.put("key", param.getKey());
                    paramObj.put("value", param.getValue());
                    if (!param.isEnabled()) {
                        paramObj.put("disabled", true);
                    }
                    queryArray.add(paramObj);
                }
                url.put("query", queryArray);
            }

            originalRequest.put("url", url);

            // 请求头
            if (origReq.getHeaders() != null && !origReq.getHeaders().isEmpty()) {
                JSONArray headersArray = new JSONArray();
                for (HttpHeader header : origReq.getHeaders()) {
                    JSONObject headerObj = new JSONObject();
                    headerObj.put("key", header.getKey());
                    headerObj.put("value", header.getValue());
                    if (!header.isEnabled()) {
                        headerObj.put("disabled", true);
                    }
                    headersArray.add(headerObj);
                }
                originalRequest.put("header", headersArray);
            }

            // 请求体
            if (origReq.getBodyType() != null && !origReq.getBodyType().isEmpty()) {
                JSONObject body = new JSONObject();
                body.put("mode", origReq.getBodyType());

                if ("raw".equals(origReq.getBodyType()) && origReq.getBody() != null) {
                    body.put("raw", origReq.getBody());
                } else if ("formdata".equals(origReq.getBodyType()) && origReq.getFormDataList() != null) {
                    JSONArray formDataArray = new JSONArray();
                    for (HttpFormData formData : origReq.getFormDataList()) {
                        JSONObject formDataObj = new JSONObject();
                        formDataObj.put("key", formData.getKey());
                        if (formData.isText()) {
                            formDataObj.put("value", formData.getValue());
                            formDataObj.put("type", "text");
                        } else if (formData.isFile()) {
                            formDataObj.put("src", formData.getValue());
                            formDataObj.put("type", "file");
                        }
                        if (!formData.isEnabled()) {
                            formDataObj.put("disabled", true);
                        }
                        formDataArray.add(formDataObj);
                    }
                    body.put("formdata", formDataArray);
                } else if ("urlencoded".equals(origReq.getBodyType()) && origReq.getUrlencodedList() != null) {
                    JSONArray urlencodedArray = new JSONArray();
                    for (HttpFormUrlencoded encoded : origReq.getUrlencodedList()) {
                        JSONObject encodedObj = new JSONObject();
                        encodedObj.put("key", encoded.getKey());
                        encodedObj.put("value", encoded.getValue());
                        if (!encoded.isEnabled()) {
                            encodedObj.put("disabled", true);
                        }
                        urlencodedArray.add(encodedObj);
                    }
                    body.put("urlencoded", urlencodedArray);
                }

                originalRequest.put("body", body);
            }

            respJson.put("originalRequest", originalRequest);
        }

        return respJson;
    }
}

