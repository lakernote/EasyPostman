package com.laker.postman;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.laker.postman.util.SystemUtil;

import java.io.File;
import java.util.UUID;

public class RequestGenerator {

    private static final String[] METHODS = {"GET", "POST", "PUT", "DELETE"};
    private static final String[] ENDPOINTS = {
            "https://httpbin.org/get",
            "https://httpbin.org/post",
            "https://httpbin.org/put",
            "https://httpbin.org/delete"
    };

    public static void main(String[] args) {
        // 构建根对象
        JSONArray jsonArray = new JSONArray();
        JSONObject group1 = new JSONObject();
        group1.set("type", "group");
        group1.set("name", "Default Group");

        JSONArray children = new JSONArray();

        for (int i = 1; i <= 500; i++) {
            children.add(generateRequest(i));
        }

        group1.set("children", children);
        jsonArray.add(group1);
        // 写入文件
        FileUtil.writeUtf8String(jsonArray.toStringPretty(), new File(SystemUtil.getUserHomeEasyPostmanPath() + "collections.json"));
        System.out.println("✅ 已生成 500 个请求，保存为 collections.json");
    }

    private static JSONObject generateRequest(int i) {
        int methodIndex = (i - 1) % METHODS.length;
        String method = METHODS[methodIndex];
        String url = ENDPOINTS[methodIndex];

        // 创建 request 对象
        JSONObject request = new JSONObject();
        request.set("type", "request");

        JSONObject data = new JSONObject();
        data.set("id", UUID.randomUUID().toString().replace("-", ""));
        data.set("name", "Request " + i);
        data.set("url", "  " + url + "  ?reqId=" + i);
        data.set("method", method);
        data.set("protocol", "HTTP");

        // headers
        JSONObject headers = new JSONObject();
        headers.set("User-Agent", "EasyPostman Client");
        headers.set("Accept", "*/*");
        if ("POST".equals(method) || "PUT".equals(method)) {
            headers.set("Content-Type", "application/json");
        }
        data.set("headers", headers);

        // body
        String body = "";
        if ("POST".equals(method) || "PUT".equals(method)) {
            body = String.format("{\"id\": %d, \"data\": \"example_data_%d\"}", i, i);
        }
        data.set("body", body);

        // params
        JSONObject params = new JSONObject();
        params.set("reqId", String.valueOf(i));
        data.set("params", params);

        // 其他空对象
        data.set("formData", new JSONObject());
        data.set("formFiles", new JSONObject());
        data.set("urlencoded", new JSONObject());
        data.set("extractorRules", new JSONArray());

        data.set("autoExtractVariables", true);
        data.set("authType", "No Auth");
        data.set("authUsername", "");
        data.set("authPassword", "");
        data.set("authToken", "");

        data.set("prescript", "");

        // postscript 测试状态码
        String postscript = String.format(
                "pm.test('Status 200 for Request %d', function () { pm.response.to.have.status(200); });",
                i
        );
        data.set("postscript", postscript);

        data.set("isFollowRedirects", true);
        data.set("newRequest", false);

        request.set("data", data);

        return request;
    }
}