package com.laker.postman.service.postman;

import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.laker.postman.model.HttpRequestItem;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.*;

/**
 * Postman Collection导出器
 * 负责将内部数据结构导出为Postman Collection格式（v2.1格式）
 */
public class PostmanCollectionExporter {

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
        return postmanItem;
    }
}

