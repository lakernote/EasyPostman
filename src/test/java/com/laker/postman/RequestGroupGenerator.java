package com.laker.postman;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.laker.postman.util.SystemUtil;

import java.io.File;
import java.util.UUID;

public class RequestGroupGenerator {

    private static final String[] METHODS = {"GET", "POST", "PUT", "DELETE"};
    private static final String[] ENDPOINTS = {
            "https://httpbin.org/get",
            "https://httpbin.org/post",
            "https://httpbin.org/put",
            "https://httpbin.org/delete"
    };
    public static final int INT = 1000;

    public static void main(String[] args) {
        JSONArray jsonArray = new JSONArray();
        int groupCount = 100;
        int requestsPerGroup = INT / groupCount;
        int requestIndex = 1;
        for (int g = 1; g <= groupCount; g++) {
            JSONObject group = new JSONObject();
            group.set("type", "group");
            group.set("name", "Group " + g);
            JSONArray children = new JSONArray();
            for (int j = 0; j < requestsPerGroup; j++) {
                if (requestIndex > INT) break;
                children.add(generateRequest(requestIndex));
                requestIndex++;
            }
            group.set("children", children);
            jsonArray.add(group);
        }
        // 写入文件
        FileUtil.writeUtf8String(jsonArray.toStringPretty(), new File(SystemUtil.getUserHomeEasyPostmanPath() + "collections.json"));
        System.out.println("✅ 已生成 " + INT + " 个请求，分为 " + groupCount + " 个分组，保存为 collections.json");
    }

    private static JSONObject generateRequest(int i) {
        int methodIndex = (i - 1) % METHODS.length;
        String method = METHODS[methodIndex];
        String url = ENDPOINTS[methodIndex];

        JSONObject request = new JSONObject();
        request.set("type", "request");

        JSONObject data = new JSONObject();
        data.set("id", UUID.randomUUID().toString().replace("-", ""));
        data.set("name", "Request " + i);
        data.set("url", url + "?reqId=" + i);
        data.set("method", method);
        data.set("protocol", "HTTP");

        JSONObject headers = new JSONObject();
        headers.set("User-Agent", "EasyPostman Client");
        headers.set("Accept", "*/*");
        if ("POST".equals(method) || "PUT".equals(method)) {
            headers.set("Content-Type", "application/json");
        }
        data.set("headers", headers);

        String body = "";
        if ("POST".equals(method) || "PUT".equals(method)) {
            body = String.format("{\"id\": %d, \"data\": \"example_data_%d\"}", i, i);
        }
        data.set("body", body);

        JSONObject params = new JSONObject();
        params.set("reqId", String.valueOf(i));
        data.set("params", params);

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

