package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.service.http.HttpRequestFactory;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

@Slf4j
public class RequestCollectionsFactory {

    // 创建默认请求组和测试请求
    public static void createDefaultRequestGroups(DefaultMutableTreeNode rootTreeNode, DefaultTreeModel treeModel) {
        try {
            DefaultMutableTreeNode defaultGroupNode = new DefaultMutableTreeNode(new Object[]{"group", "Default Group"});
            rootTreeNode.add(defaultGroupNode);
            // Example: Environment variable + script
            HttpRequestItem example = HttpRequestFactory.createDefaultRequest();
            example.setName("Environment Variable + Script Example");
            example.setMethod("GET");
            example.setUrl("{{baseUrl}}?q=lakernote");
            example.getParams().put("q", "lakernote");
            example.setPrescript("console.log('This is a pre-request script');");
            example.setPostscript("""
                    console.log('This is a post-request script');
                    pm.test('Response status is 200', function () {
                        pm.response.to.have.status(200);
                    });
                    pm.test('Response contains lakernote', function () {
                        pm.expect(pm.response.text()).to.include('lakernote');
                    });
                    """);
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{"request", example}));

            // GET Example
            HttpRequestItem getExample = HttpRequestFactory.createDefaultRequest();
            getExample.setName("GET Example");
            getExample.setMethod("GET");
            getExample.setUrl("https://httpbin.org/get");
            getExample.getHeaders().put("Accept", "application/json");
            getExample.getParams().put("q", "easytools");
            getExample.getParams().put("lang", "en");
            getExample.getParams().put("page", "1");
            getExample.getParams().put("size", "10");
            getExample.getParams().put("sort", "desc");
            getExample.getParams().put("filter", "active");
            getExample.setPostscript("""
                    pm.test('Response status is 200', function () {
                        pm.response.to.have.status(200);
                    });""");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{"request", getExample}));

            // POST JSON Example
            HttpRequestItem postJson = HttpRequestFactory.createDefaultRequest();
            postJson.setName("POST-JSON Example");
            postJson.setMethod("POST");
            postJson.setUrl("https://httpbin.org/post");
            postJson.getHeaders().put("Content-Type", "application/json");
            postJson.setBody("""
                    {
                        "key1": "value1",
                        "key2": "value2",
                        "key3": "value3"
                    }""");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{"request", postJson}));

            // POST form-data Example
            HttpRequestItem postFormData = HttpRequestFactory.createDefaultRequest();
            postFormData.setName("POST-form-data Example");
            postFormData.setMethod("POST");
            postFormData.setUrl("https://httpbin.org/post");
            postFormData.getHeaders().put("Content-Type", "multipart/form-data");
            postFormData.getFormData().put("key1", "value1");
            postFormData.getFormData().put("key2", "value2");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{"request", postFormData}));

            // POST x-www-form-urlencoded Example
            HttpRequestItem postUrl = HttpRequestFactory.createDefaultRequest();
            postUrl.setName("POST-x-www-form-urlencoded Example");
            postUrl.setMethod("POST");
            postUrl.setUrl("https://httpbin.org/post");
            postUrl.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
            postUrl.getUrlencoded().put("key1", "value1");
            postUrl.getUrlencoded().put("key2", "value2");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{"request", postUrl}));

            // PUT Example
            HttpRequestItem put = HttpRequestFactory.createDefaultRequest();
            put.setName("PUT Example");
            put.setMethod("PUT");
            put.setUrl("https://httpbin.org/put");
            put.getHeaders().put("Content-Type", "application/json");
            put.setBody("{\"update\":true}");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{"request", put}));

            // DELETE Example
            HttpRequestItem delete = HttpRequestFactory.createDefaultRequest();
            delete.setName("DELETE Example");
            delete.setMethod("DELETE");
            delete.setUrl("https://httpbin.org/delete");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{"request", delete}));

            // WebSocket Example
            HttpRequestItem websocket = HttpRequestFactory.createDefaultWebSocketRequest();
            websocket.setName("WebSocket Example");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{"request", websocket}));

            // Redirect Example
            HttpRequestItem redirect = HttpRequestFactory.createDefaultRedirectRequest();
            redirect.setName("Redirect Example");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{"request", redirect}));

            treeModel.reload();
        } catch (Exception ex) {
            log.error("Failed to create default request groups", ex);
        }
    }
}