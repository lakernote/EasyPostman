package com.laker.postman.service.collections;

import com.laker.postman.model.HttpFormData;
import com.laker.postman.model.HttpFormUrlencoded;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpParam;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.service.http.HttpRequestFactory;
import com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel;
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
    public static final String HTTPS_HTTPBIN_ORG = "https://httpbin.org";
    public static final String HTTPS_HTTPBIN_ORG_POST = HTTPS_HTTPBIN_ORG + "/post";

    private DefaultRequestsFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static void create(DefaultMutableTreeNode rootTreeNode, DefaultTreeModel treeModel) {
        try {
            createEnglishScriptExamples(rootTreeNode);
            createChineseScriptExamples(rootTreeNode);
            createBasicHttpExamples(rootTreeNode);
            treeModel.reload();
        } catch (Exception ex) {
            log.error("Failed to create default request groups", ex);
        }
    }

    private static void createEnglishScriptExamples(DefaultMutableTreeNode rootTreeNode) {
        RequestGroup group = new RequestGroup("Script Examples (EN)");
        group.setDescription("""
                Run request 1 first, then request 2.
                
                Request 1 sets environment variables in script.
                Request 2 reuses them in query params, headers, body, and test script.
                Request 2 also creates temporary variables for the current request.
                """);
        DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(new Object[]{"group", group});
        rootTreeNode.add(groupNode);

        HttpRequestItem setup = HttpRequestFactory.createDefaultRequest();
        setup.setName("1. Setup Variables");
        setup.setDescription("Creates shared environment variables and one temporary variable in the pre-request script.");
        setup.setMethod("GET");
        setup.setUrl("{{baseUrl}}/anything/setup");
        setup.setHeadersList(withExtraHeaders(setup,
                new HttpHeader(true, "X-Setup-Trace", "{{setupTraceId}}")));
        setup.setParamsList(List.of(
                new HttpParam(true, "tenant", "{{tenantId}}")
        ));
        setup.setPrescript("""
                pm.environment.set('baseUrl', 'https://httpbin.org');
                pm.environment.set('tenantId', 'team-alpha');
                pm.environment.set('userId', 'user-1001');
                pm.environment.set('keyword', 'easy-postman');
                
                pm.variables.set('setupTraceId', 'setup-' + Date.now());
                console.log('Environment ready:', JSON.stringify(pm.environment.toObject()));
                """);
        setup.setPostscript("""
                var jsonData = pm.response.json();
                pm.test('Setup request is successful', function () {
                    pm.response.to.have.status(200);
                    pm.expect(jsonData.args.tenant).to.eql(pm.environment.get('tenantId'));
                });
                """);
        addRequestNode(groupNode, setup);

        HttpRequestItem reuse = HttpRequestFactory.createDefaultRequest();
        reuse.setName("2. Reuse Variables");
        reuse.setDescription("Reuses environment variables in params, headers, body, and script. Temporary variables are created in this request and reused immediately.");
        reuse.setMethod("POST");
        reuse.setUrl(HTTPS_HTTPBIN_ORG_POST);
        reuse.setBodyType(RequestBodyPanel.BODY_TYPE_RAW);
        reuse.setHeadersList(withExtraHeaders(reuse,
                new HttpHeader(true, CONTENT_TYPE, APPLICATION_JSON),
                new HttpHeader(true, "X-Tenant-Id", "{{tenantId}}"),
                new HttpHeader(true, "X-Trace-Id", "{{traceId}}")));
        reuse.setParamsList(List.of(
                new HttpParam(true, "keyword", "{{keyword}}"),
                new HttpParam(true, "trace", "{{traceId}}")
        ));
        reuse.setBody("""
                {
                  "tenantId": "{{tenantId}}",
                  "userId": "{{userId}}",
                  "traceId": "{{traceId}}",
                  "note": "{{note}}"
                }""");
        reuse.setPrescript("""
                pm.variables.set('traceId', 'trace-' + Date.now());
                pm.variables.set('note', 'temporary variables work in body, header, and params');
                
                pm.request.headers.upsert({
                    key: 'X-Script-Flag',
                    value: 'ready'
                });
                """);
        reuse.setPostscript("""
                var jsonData = pm.response.json();
                pm.test('Reuse request is successful', function () {
                    pm.response.to.have.status(200);
                });
                pm.test('Environment variables are reused', function () {
                    pm.expect(jsonData.json.tenantId).to.eql(pm.environment.get('tenantId'));
                    pm.expect(jsonData.json.userId).to.eql(pm.environment.get('userId'));
                    pm.expect(jsonData.args.keyword).to.eql(pm.environment.get('keyword'));
                });
                pm.test('Temporary variables are reused in the current request', function () {
                    pm.expect(jsonData.args.trace).to.eql(pm.variables.get('traceId'));
                    pm.expect(jsonData.json.traceId).to.eql(pm.variables.get('traceId'));
                    pm.expect(jsonData.json.note).to.eql(pm.variables.get('note'));
                    pm.expect(jsonData.headers['X-Trace-Id']).to.eql(pm.variables.get('traceId'));
                });
                console.log('traceId =', pm.variables.get('traceId'));
                """);
        addRequestNode(groupNode, reuse);
    }

    private static void createChineseScriptExamples(DefaultMutableTreeNode rootTreeNode) {
        RequestGroup group = new RequestGroup("脚本示例（中文）");
        group.setDescription("""
                先执行请求 1，再执行请求 2。
                
                请求 1 在脚本里设置环境变量。
                请求 2 在请求参数、请求头、请求体和测试脚本里复用这些变量。
                请求 2 还会创建当前请求可用的临时变量。
                """);
        DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(new Object[]{"group", group});
        rootTreeNode.add(groupNode);

        HttpRequestItem setup = HttpRequestFactory.createDefaultRequest();
        setup.setName("1. 设置变量");
        setup.setDescription("在前置脚本中设置共享环境变量，并演示一个临时变量。");
        setup.setMethod("GET");
        setup.setUrl("{{baseUrl}}/anything/setup-cn");
        setup.setHeadersList(withExtraHeaders(setup,
                new HttpHeader(true, "X-Setup-Trace", "{{setupTraceId}}")));
        setup.setParamsList(List.of(
                new HttpParam(true, "tenant", "{{tenantId}}")
        ));
        setup.setPrescript("""
                pm.environment.set('baseUrl', 'https://httpbin.org');
                pm.environment.set('tenantId', 'team-cn');
                pm.environment.set('userId', 'user-cn-1001');
                pm.environment.set('keyword', 'easy-postman-cn');
                
                pm.variables.set('setupTraceId', 'setup-cn-' + Date.now());
                console.log('环境变量已准备好:', JSON.stringify(pm.environment.toObject()));
                """);
        setup.setPostscript("""
                var jsonData = pm.response.json();
                pm.test('设置变量请求成功', function () {
                    pm.response.to.have.status(200);
                    pm.expect(jsonData.args.tenant).to.eql(pm.environment.get('tenantId'));
                });
                """);
        addRequestNode(groupNode, setup);

        HttpRequestItem reuse = HttpRequestFactory.createDefaultRequest();
        reuse.setName("2. 复用变量");
        reuse.setDescription("在请求参数、请求头、请求体和脚本里复用环境变量；同时创建并使用当前请求的临时变量。");
        reuse.setMethod("POST");
        reuse.setUrl(HTTPS_HTTPBIN_ORG_POST);
        reuse.setBodyType(RequestBodyPanel.BODY_TYPE_RAW);
        reuse.setHeadersList(withExtraHeaders(reuse,
                new HttpHeader(true, CONTENT_TYPE, APPLICATION_JSON),
                new HttpHeader(true, "X-Tenant-Id", "{{tenantId}}"),
                new HttpHeader(true, "X-Trace-Id", "{{traceId}}")));
        reuse.setParamsList(List.of(
                new HttpParam(true, "keyword", "{{keyword}}"),
                new HttpParam(true, "trace", "{{traceId}}")
        ));
        reuse.setBody("""
                {
                  "tenantId": "{{tenantId}}",
                  "userId": "{{userId}}",
                  "traceId": "{{traceId}}",
                  "note": "{{note}}"
                }""");
        reuse.setPrescript("""
                pm.variables.set('traceId', 'trace-cn-' + Date.now());
                pm.variables.set('note', '临时变量可用于请求体、请求头和请求参数');
                
                pm.request.headers.upsert({
                    key: 'X-Script-Flag',
                    value: 'ready-cn'
                });
                """);
        reuse.setPostscript("""
                var jsonData = pm.response.json();
                pm.test('复用变量请求成功', function () {
                    pm.response.to.have.status(200);
                });
                pm.test('环境变量复用成功', function () {
                    pm.expect(jsonData.json.tenantId).to.eql(pm.environment.get('tenantId'));
                    pm.expect(jsonData.json.userId).to.eql(pm.environment.get('userId'));
                    pm.expect(jsonData.args.keyword).to.eql(pm.environment.get('keyword'));
                });
                pm.test('临时变量在当前请求中复用成功', function () {
                    pm.expect(jsonData.args.trace).to.eql(pm.variables.get('traceId'));
                    pm.expect(jsonData.json.traceId).to.eql(pm.variables.get('traceId'));
                    pm.expect(jsonData.json.note).to.eql(pm.variables.get('note'));
                    pm.expect(jsonData.headers['X-Trace-Id']).to.eql(pm.variables.get('traceId'));
                });
                console.log('当前 traceId =', pm.variables.get('traceId'));
                """);
        addRequestNode(groupNode, reuse);
    }

    private static void createBasicHttpExamples(DefaultMutableTreeNode rootTreeNode) {
        RequestGroup defaultGroup = new RequestGroup("Basic HTTP Examples");
        defaultGroup.setDescription("""
                General-purpose examples for common request types.
                These requests are independent from the official script examples.
                """);
        DefaultMutableTreeNode defaultGroupNode = new DefaultMutableTreeNode(new Object[]{"group", defaultGroup});
        rootTreeNode.add(defaultGroupNode);

        HttpRequestItem getExample = HttpRequestFactory.createDefaultRequest();
        getExample.setName("GET Example");
        getExample.setMethod("GET");
        getExample.setUrl(HTTPS_HTTPBIN_ORG + "/get?q=easytools&lang=en&page=1&size=10&sort=desc&filter=active");
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
        addRequestNode(defaultGroupNode, getExample);

        HttpRequestItem postJson = HttpRequestFactory.createDefaultRequest();
        postJson.setName("POST JSON Example");
        postJson.setMethod("POST");
        postJson.setUrl(HTTPS_HTTPBIN_ORG_POST);
        postJson.setBodyType(RequestBodyPanel.BODY_TYPE_RAW);
        postJson.setHeadersList(withExtraHeaders(postJson,
                new HttpHeader(true, CONTENT_TYPE, APPLICATION_JSON)));
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
        addRequestNode(defaultGroupNode, postJson);

        HttpRequestItem postFormData = HttpRequestFactory.createDefaultRequest();
        postFormData.setName("POST form-data Example");
        postFormData.setMethod("POST");
        postFormData.setUrl(HTTPS_HTTPBIN_ORG_POST);
        postFormData.setHeadersList(withExtraHeaders(postFormData,
                new HttpHeader(true, CONTENT_TYPE, "multipart/form-data")));
        postFormData.setFormDataList(List.of(
                new HttpFormData(true, "key1", HttpFormData.TYPE_TEXT, "value1"),
                new HttpFormData(true, "key2", HttpFormData.TYPE_TEXT, "value2")
        ));
        postFormData.setPostscript("""
                pm.test('Form-data value check', function () {
                    var jsonData = pm.response.json();
                    pm.expect(jsonData.form.key1).to.eql("value1");
                });
                """);
        addRequestNode(defaultGroupNode, postFormData);

        HttpRequestItem postUrl = HttpRequestFactory.createDefaultRequest();
        postUrl.setName("POST x-www-form-urlencoded Example");
        postUrl.setMethod("POST");
        postUrl.setUrl(HTTPS_HTTPBIN_ORG_POST);
        postUrl.setHeadersList(withExtraHeaders(postUrl,
                new HttpHeader(true, CONTENT_TYPE, "application/x-www-form-urlencoded")));
        postUrl.setUrlencodedList(List.of(
                new HttpFormUrlencoded(true, "key1", "value1"),
                new HttpFormUrlencoded(true, "key2", "value2")
        ));
        postUrl.setPostscript("""
                pm.test('URL encoded value check', function () {
                    var jsonData = pm.response.json();
                    pm.expect(jsonData.form.key1).to.eql("value1");
                });
                """);
        addRequestNode(defaultGroupNode, postUrl);

        HttpRequestItem put = HttpRequestFactory.createDefaultRequest();
        put.setName("PUT Example");
        put.setMethod("PUT");
        put.setUrl(HTTPS_HTTPBIN_ORG + "/put");
        put.setBodyType(RequestBodyPanel.BODY_TYPE_RAW);
        put.setHeadersList(withExtraHeaders(put,
                new HttpHeader(true, CONTENT_TYPE, APPLICATION_JSON)));
        put.setBody("{\"update\":true}");
        addRequestNode(defaultGroupNode, put);

        HttpRequestItem delete = HttpRequestFactory.createDefaultRequest();
        delete.setName("DELETE Example");
        delete.setMethod("DELETE");
        delete.setUrl(HTTPS_HTTPBIN_ORG + "/delete");
        addRequestNode(defaultGroupNode, delete);

        HttpRequestItem redirect = HttpRequestFactory.createDefaultRedirectRequest();
        redirect.setName("Redirect Example");
        addRequestNode(defaultGroupNode, redirect);

        HttpRequestItem websocket = HttpRequestFactory.createDefaultWebSocketRequest();
        websocket.setName("WebSocket Example");
        addRequestNode(defaultGroupNode, websocket);

        HttpRequestItem sse = HttpRequestFactory.createDefaultSseRequest();
        addRequestNode(defaultGroupNode, sse);
    }

    private static List<HttpHeader> withExtraHeaders(HttpRequestItem item, HttpHeader... extraHeaders) {
        List<HttpHeader> headers = new ArrayList<>(item.getHeadersList());
        headers.addAll(List.of(extraHeaders));
        return headers;
    }

    private static void addRequestNode(DefaultMutableTreeNode parentNode, HttpRequestItem item) {
        parentNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, item}));
    }
}
