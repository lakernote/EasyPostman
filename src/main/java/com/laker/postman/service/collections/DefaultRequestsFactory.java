package com.laker.postman.service.collections;

import com.laker.postman.model.*;
import com.laker.postman.service.http.HttpRequestFactory;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DefaultRequestsFactory {

    public static final String REQUEST = "request";
    public static final String APPLICATION_JSON = "application/json";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String HTTPS_HTTPBIN_ORG_POST = "https://httpbin.org/post";

    private DefaultRequestsFactory() {
        throw new IllegalStateException("Utility class");
    }

    // 创建默认请求组和测试请求
    public static void create(DefaultMutableTreeNode rootTreeNode, DefaultTreeModel treeModel) {
        try {
            RequestGroup defaultGroup = new RequestGroup("Default Group");
            DefaultMutableTreeNode defaultGroupNode = new DefaultMutableTreeNode(new Object[]{"group", defaultGroup});
            rootTreeNode.add(defaultGroupNode);
            // Example: Environment variable + script
            HttpRequestItem example = HttpRequestFactory.createDefaultRequest();
            example.setName("Environment Variable + Script Example");
            example.setMethod("GET");
            example.setUrl("{{baseUrl}}?q=lakernote");
            example.setParamsList(List.of(
                    new HttpParam(true, "q", "lakernote")
            ));
            example.setPrescript("console.log('This is a pre-request script');");
            example.setPostscript("""
                    console.log('This is a post-request script');
                    pm.test('Response status is 200', function () {
                        pm.response.to.have.status(200);
                    });
                    """);
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, example}));

            // GET Example
            HttpRequestItem getExample = HttpRequestFactory.createDefaultRequest();
            getExample.setName("GET Example");
            getExample.setMethod("GET");
            getExample.setUrl("https://httpbin.org/get?q=easytools&lang=en&page=1&size=10&sort=desc&filter=active");
            getExample.setParamsList(List.of(
                    new HttpParam(true, "q", "easytools"),
                    new HttpParam(true, "lang", "en"),
                    new HttpParam(true, "page", "1"),
                    new HttpParam(true, "size", "10"),
                    new HttpParam(true, "sort", "desc"),
                    new HttpParam(true, "filter", "active")
            ));
            getExample.setPostscript("""
                    pm.test('JSON value check', function () {
                        var jsonData = pm.response.json();
                        pm.expect(jsonData.args.size).to.eql("10");
                    });
                    """);
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, getExample}));

            // POST JSON Example
            HttpRequestItem postJson = HttpRequestFactory.createDefaultRequest();
            postJson.setName("POST-JSON Example");
            postJson.setMethod("POST");
            postJson.setUrl(HTTPS_HTTPBIN_ORG_POST);
            List<HttpHeader> postJsonHeaders = new ArrayList<>(postJson.getHeadersList());
            postJsonHeaders.add(new HttpHeader(true, CONTENT_TYPE, APPLICATION_JSON));
            postJson.setHeadersList(postJsonHeaders);
            postJson.setBody("""
                    {
                        "key1": "value1",
                        "key2": "value2",
                        "key3": "value3"
                    }""");
            postJson.setPostscript("""
                    pm.test('JSON value check', function () {
                        var jsonData = pm.response.json();
                        pm.expect(jsonData.json.key1).to.eql("value1");
                    });
                    """);
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, postJson}));

            // POST form-data Example
            HttpRequestItem postFormData = HttpRequestFactory.createDefaultRequest();
            postFormData.setName("POST-form-data Example");
            postFormData.setMethod("POST");
            postFormData.setUrl(HTTPS_HTTPBIN_ORG_POST);
            List<HttpHeader> formDataHeaders = new ArrayList<>(postFormData.getHeadersList());
            formDataHeaders.add(new HttpHeader(true, CONTENT_TYPE, "multipart/form-data"));
            postFormData.setHeadersList(formDataHeaders);
            postFormData.setFormDataList(List.of(
                    new HttpFormData(true, "key1", HttpFormData.TYPE_TEXT, "value1"),
                    new HttpFormData(true, "key2", HttpFormData.TYPE_TEXT, "value2")
            ));
            postFormData.setPostscript("""
                    pm.test('JSON value check', function () {
                        var jsonData = pm.response.json();
                        pm.expect(jsonData.form.key1).to.eql("value1");
                    });
                    """);
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, postFormData}));

            // POST x-www-form-urlencoded Example
            HttpRequestItem postUrl = HttpRequestFactory.createDefaultRequest();
            postUrl.setName("POST-x-www-form-urlencoded Example");
            postUrl.setMethod("POST");
            postUrl.setUrl(HTTPS_HTTPBIN_ORG_POST);
            List<HttpHeader> urlEncodedHeaders = new ArrayList<>(postUrl.getHeadersList());
            urlEncodedHeaders.add(new HttpHeader(true, CONTENT_TYPE, "application/x-www-form-urlencoded"));
            postUrl.setHeadersList(urlEncodedHeaders);
            postUrl.setUrlencodedList(List.of(
                    new HttpFormUrlencoded(true, "key1", "value1"),
                    new HttpFormUrlencoded(true, "key2", "value2")
            ));
            postUrl.setPostscript("""
                    pm.test('JSON value check', function () {
                        var jsonData = pm.response.json();
                        pm.expect(jsonData.form.key1).to.eql("value1");
                    });
                    """);
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, postUrl}));

            // PUT Example
            HttpRequestItem put = HttpRequestFactory.createDefaultRequest();
            put.setName("PUT Example");
            put.setMethod("PUT");
            put.setUrl("https://httpbin.org/put");
            List<HttpHeader> putHeaders = new ArrayList<>(put.getHeadersList());
            putHeaders.add(new HttpHeader(true, CONTENT_TYPE, APPLICATION_JSON));
            put.setHeadersList(putHeaders);
            put.setBody("{\"update\":true}");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, put}));

            // DELETE Example
            HttpRequestItem delete = HttpRequestFactory.createDefaultRequest();
            delete.setName("DELETE Example");
            delete.setMethod("DELETE");
            delete.setUrl("https://httpbin.org/delete");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, delete}));

            // Redirect Example
            HttpRequestItem redirect = HttpRequestFactory.createDefaultRedirectRequest();
            redirect.setName("Redirect Example");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, redirect}));

            // WebSocket Example
            HttpRequestItem websocket = HttpRequestFactory.createDefaultWebSocketRequest();
            websocket.setName("WebSocket Example");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, websocket}));

            // SSE Example
            HttpRequestItem sse = HttpRequestFactory.createDefaultSseRequest();
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, sse}));

            treeModel.reload();
        } catch (Exception ex) {
            log.error("Failed to create default request groups", ex);
        }
    }
}