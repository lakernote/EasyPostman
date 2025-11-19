package com.laker.postman.service.httpfile;

import java.util.Base64;

import javax.swing.tree.DefaultMutableTreeNode;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.model.RequestItemProtocolEnum;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BASIC;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BEARER;

/**
 * HttpFileParser 单元测试类
 * 测试 HTTP 文件解析器的各种功能
 */
public class HttpFileParserTest {

    @Test(description = "测试解析空内容")
    public void testParseEmptyContent() {
        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(null);
        assertNull(result, "null 内容应该返回 null");

        result = HttpFileParser.parseHttpFile("");
        assertNull(result, "空字符串应该返回 null");

        result = HttpFileParser.parseHttpFile("   ");
        assertNull(result, "空白字符串应该返回 null");
    }

    @Test(description = "测试解析简单的 GET 请求")
    public void testParseSimpleGetRequest() {
        String content = "GET https://api.example.com/users";
        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);

        assertNotNull(result, "应该成功解析");
        assertEquals(result.getChildCount(), 1, "应该有一个请求节点");

        DefaultMutableTreeNode requestNode = (DefaultMutableTreeNode) result.getChildAt(0);
        Object[] userObject = (Object[]) requestNode.getUserObject();
        assertEquals(userObject[0], "request", "节点类型应该是 request");

        HttpRequestItem request = (HttpRequestItem) userObject[1];
        assertEquals(request.getMethod(), "GET");
        assertEquals(request.getUrl(), "https://api.example.com/users");
        assertNotNull(request.getName(), "应该有请求名称");
    }

    @Test(description = "测试解析带名称的请求")
    public void testParseRequestWithName() {
        String content = """
                ### 获取用户列表
                GET https://api.example.com/users
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);
        assertEquals(result.getChildCount(), 1);

        DefaultMutableTreeNode requestNode = (DefaultMutableTreeNode) result.getChildAt(0);
        HttpRequestItem request = (HttpRequestItem) ((Object[]) requestNode.getUserObject())[1];
        assertEquals(request.getName(), "获取用户列表");
        assertEquals(request.getMethod(), "GET");
        assertEquals(request.getUrl(), "https://api.example.com/users");
    }

    @Test(description = "测试解析多个请求")
    public void testParseMultipleRequests() {
        String content = """
                ### 获取用户列表
                GET https://api.example.com/users

                ### 创建用户
                POST https://api.example.com/users
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);
        assertEquals(result.getChildCount(), 2, "应该有两个请求节点");

        // 验证第一个请求
        DefaultMutableTreeNode requestNode1 = (DefaultMutableTreeNode) result.getChildAt(0);
        HttpRequestItem request1 = (HttpRequestItem) ((Object[]) requestNode1.getUserObject())[1];
        assertEquals(request1.getName(), "获取用户列表");
        assertEquals(request1.getMethod(), "GET");

        // 验证第二个请求
        DefaultMutableTreeNode requestNode2 = (DefaultMutableTreeNode) result.getChildAt(1);
        HttpRequestItem request2 = (HttpRequestItem) ((Object[]) requestNode2.getUserObject())[1];
        assertEquals(request2.getName(), "创建用户");
        assertEquals(request2.getMethod(), "POST");
    }

    @Test(description = "测试解析 POST 请求带 JSON body")
    public void testParsePostRequestWithJsonBody() {
        String content = """
                ### 创建用户
                POST https://api.example.com/users
                Content-Type: application/json

                {
                  "name": "John Doe",
                  "email": "john@example.com"
                }
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);
        assertEquals(result.getChildCount(), 1);

        HttpRequestItem request = getRequestFromNode(result, 0);
        assertEquals(request.getMethod(), "POST");
        assertEquals(request.getUrl(), "https://api.example.com/users");
        assertEquals(request.getBodyType(), "raw");
        assertTrue(request.getBody().contains("John Doe"));
        assertTrue(request.getBody().contains("john@example.com"));

        // 验证 Content-Type 头部
        assertNotNull(request.getHeadersList());
        boolean hasContentType = request.getHeadersList().stream()
                .anyMatch(h -> "Content-Type".equals(h.getKey()) && 
                             h.getValue().contains("application/json"));
        assertTrue(hasContentType, "应该有 Content-Type 头部");
    }

    @Test(description = "测试解析带多个请求头的请求")
    public void testParseRequestWithMultipleHeaders() {
        String content = """
                ### API 请求
                GET https://api.example.com/data
                Accept: application/json
                User-Agent: EasyPostman/1.0
                X-Custom-Header: custom-value
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);

        HttpRequestItem request = getRequestFromNode(result, 0);
        assertNotNull(request.getHeadersList());
        assertEquals(request.getHeadersList().size(), 3, "应该有 3 个请求头");

        // 验证请求头
        assertTrue(hasHeader(request, "Accept", "application/json"));
        assertTrue(hasHeader(request, "User-Agent", "EasyPostman/1.0"));
        assertTrue(hasHeader(request, "X-Custom-Header", "custom-value"));
    }

    @Test(description = "测试解析 Basic 认证")
    public void testParseBasicAuth() {
        String content = """
                ### 需要认证的请求
                GET https://api.example.com/protected
                Authorization: Basic dXNlcm5hbWU6cGFzc3dvcmQ=
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);

        HttpRequestItem request = getRequestFromNode(result, 0);
        assertEquals(request.getAuthType(), AUTH_TYPE_BASIC);
        
        // 验证 Base64 解码
        String decoded = new String(Base64.getDecoder().decode("dXNlcm5hbWU6cGFzc3dvcmQ="));
        String[] parts = decoded.split(":", 2);
        assertEquals(request.getAuthUsername(), parts[0]);
        assertEquals(request.getAuthPassword(), parts[1]);
    }

    @Test(description = "测试解析 Basic 认证（变量格式）")
    public void testParseBasicAuthWithVariables() {
        String content = """
                ### 需要认证的请求
                GET https://api.example.com/protected
                Authorization: Basic {{username}} {{password}}
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);

        HttpRequestItem request = getRequestFromNode(result, 0);
        assertEquals(request.getAuthType(), AUTH_TYPE_BASIC);
        assertEquals(request.getAuthUsername(), "{{username}}");
        assertEquals(request.getAuthPassword(), "{{password}}");
    }

    @Test(description = "测试解析 Bearer 认证")
    public void testParseBearerAuth() {
        String content = """
                ### 需要 Bearer Token 的请求
                GET https://api.example.com/protected
                Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);

        HttpRequestItem request = getRequestFromNode(result, 0);
        assertEquals(request.getAuthType(), AUTH_TYPE_BEARER);
        assertEquals(request.getAuthToken(), "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
    }

    @Test(description = "测试解析 urlencoded body")
    public void testParseUrlencodedBody() {
        String content = """
                ### 提交表单
                POST https://api.example.com/submit
                Content-Type: application/x-www-form-urlencoded

                name=John+Doe&email=john%40example.com&age=30
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);

        HttpRequestItem request = getRequestFromNode(result, 0);
        assertEquals(request.getMethod(), "POST");
        assertEquals(request.getBodyType(), "urlencoded");
        assertNotNull(request.getUrlencodedList());
        assertEquals(request.getUrlencodedList().size(), 3, "应该有 3 个 urlencoded 字段");

        // 验证字段值
        assertTrue(hasUrlencodedField(request, "name", "John+Doe"));
        assertTrue(hasUrlencodedField(request, "email", "john%40example.com"));
        assertTrue(hasUrlencodedField(request, "age", "30"));
    }

    @Test(description = "测试解析各种 HTTP 方法")
    public void testParseVariousHttpMethods() {
        String content = """
                ### GET 请求
                GET https://api.example.com/get

                ### POST 请求
                POST https://api.example.com/post

                ### PUT 请求
                PUT https://api.example.com/put

                ### DELETE 请求
                DELETE https://api.example.com/delete

                ### PATCH 请求
                PATCH https://api.example.com/patch

                ### HEAD 请求
                HEAD https://api.example.com/head

                ### OPTIONS 请求
                OPTIONS https://api.example.com/options
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);
        assertEquals(result.getChildCount(), 7, "应该有 7 个请求");

        assertEquals(getRequestFromNode(result, 0).getMethod(), "GET");
        assertEquals(getRequestFromNode(result, 1).getMethod(), "POST");
        assertEquals(getRequestFromNode(result, 2).getMethod(), "PUT");
        assertEquals(getRequestFromNode(result, 3).getMethod(), "DELETE");
        assertEquals(getRequestFromNode(result, 4).getMethod(), "PATCH");
        assertEquals(getRequestFromNode(result, 5).getMethod(), "HEAD");
        assertEquals(getRequestFromNode(result, 6).getMethod(), "OPTIONS");
    }

    @Test(description = "测试解析 POST 请求带多行 body")
    public void testParsePostRequestWithMultilineBody() {
        String content = """
                ### 创建复杂对象
                POST https://api.example.com/complex
                Content-Type: application/json

                {
                  "name": "John",
                  "address": {
                    "street": "123 Main St",
                    "city": "New York"
                  },
                  "tags": ["tag1", "tag2"]
                }
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);

        HttpRequestItem request = getRequestFromNode(result, 0);
        assertEquals(request.getMethod(), "POST");
        assertEquals(request.getBodyType(), "raw");
        assertNotNull(request.getBody());
        assertTrue(request.getBody().contains("\"name\": \"John\""));
        assertTrue(request.getBody().contains("\"street\": \"123 Main St\""));
        assertTrue(request.getBody().contains("\"tags\":"));
    }

    @Test(description = "测试解析没有名称的请求（使用 URL 作为名称）")
    public void testParseRequestWithoutName() {
        String content = """
                ###
                GET https://api.example.com/users/profile
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);

        HttpRequestItem request = getRequestFromNode(result, 0);
        // 如果没有名称，应该使用 URL 的路径部分作为名称
        assertNotNull(request.getName());
        assertFalse(request.getName().isEmpty());
    }

    @Test(description = "测试解析 SSE 请求")
    public void testParseSSERequest() {
        String content = """
                ### SSE 流式请求
                GET https://api.example.com/events
                Accept: text/event-stream
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);

        HttpRequestItem request = getRequestFromNode(result, 0);
        assertEquals(request.getProtocol(), RequestItemProtocolEnum.SSE);
        assertTrue(hasHeader(request, "Accept", "text/event-stream"));
    }

    @Test(description = "测试解析 WebSocket 请求")
    public void testParseWebSocketRequest() {
        String content = """
                ### WebSocket 连接
                GET wss://echo-websocket.hoppscotch.io/
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);

        HttpRequestItem request = getRequestFromNode(result, 0);
        assertEquals(request.getProtocol(), RequestItemProtocolEnum.WEBSOCKET);
        assertEquals(request.getUrl(), "wss://echo-websocket.hoppscotch.io/");
    }

    @Test(description = "测试解析包含空行的请求")
    public void testParseRequestWithEmptyLines() {
        String content = """
                ### 带空行的请求
                POST https://api.example.com/data
                Content-Type: application/json


                {
                  "data": "value"
                }
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);

        HttpRequestItem request = getRequestFromNode(result, 0);
        assertEquals(request.getMethod(), "POST");
        assertNotNull(request.getBody());
        assertTrue(request.getBody().contains("\"data\": \"value\""));
    }

    @Test(description = "测试解析没有分隔符的单个请求")
    public void testParseSingleRequestWithoutSeparator() {
        String content = "GET https://api.example.com/users";
        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);

        assertNotNull(result);
        assertEquals(result.getChildCount(), 1);

        HttpRequestItem request = getRequestFromNode(result, 0);
        assertEquals(request.getMethod(), "GET");
        assertEquals(request.getUrl(), "https://api.example.com/users");
    }

    @Test(description = "测试解析带查询参数的 URL")
    public void testParseRequestWithQueryParams() {
        String content = """
                ### 搜索请求
                GET https://api.example.com/search?q=test&limit=10&page=1
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);

        HttpRequestItem request = getRequestFromNode(result, 0);
        assertEquals(request.getUrl(), "https://api.example.com/search?q=test&limit=10&page=1");
    }

    @Test(description = "测试解析分组节点")
    public void testParseGroupNode() {
        String content = """
                ### 第一个请求
                GET https://api.example.com/one

                ### 第二个请求
                POST https://api.example.com/two
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);

        // 验证根节点是分组节点
        Object[] rootUserObject = (Object[]) result.getUserObject();
        assertEquals(rootUserObject[0], "group");
        assertTrue(rootUserObject[1] instanceof RequestGroup);

        RequestGroup group = (RequestGroup) rootUserObject[1];
        assertNotNull(group.getName());
        assertTrue(group.getName().startsWith("HTTP Import"));
    }

    @Test(description = "测试解析无效请求（没有 URL）")
    public void testParseInvalidRequestWithoutUrl() {
        String content = """
                ### 无效请求
                GET
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        // 没有有效 URL 的请求不应该被添加到结果中
        assertNull(result, "没有有效 URL 的请求应该返回 null");
    }

    @Test(description = "测试解析带特殊字符的请求头值")
    public void testParseHeaderWithSpecialCharacters() {
        String content = """
                ### 特殊字符测试
                GET https://api.example.com/test
                X-Custom: value:with:colons
                X-Another: value with spaces
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);

        HttpRequestItem request = getRequestFromNode(result, 0);
        assertTrue(hasHeader(request, "X-Custom", "value:with:colons"));
        assertTrue(hasHeader(request, "X-Another", "value with spaces"));
    }

    @Test(description = "测试解析 DELETE 请求带 body")
    public void testParseDeleteRequestWithBody() {
        String content = """
                ### 删除资源
                DELETE https://api.example.com/resource/123
                Content-Type: application/json

                {
                  "reason": "No longer needed"
                }
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);

        HttpRequestItem request = getRequestFromNode(result, 0);
        assertEquals(request.getMethod(), "DELETE");
        assertNotNull(request.getBody());
        assertTrue(request.getBody().contains("\"reason\""));
    }

    @Test(description = "测试解析 PUT 请求")
    public void testParsePutRequest() {
        String content = """
                ### 更新资源
                PUT https://api.example.com/resource/123
                Content-Type: application/json

                {
                  "name": "Updated Name"
                }
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);

        HttpRequestItem request = getRequestFromNode(result, 0);
        assertEquals(request.getMethod(), "PUT");
        assertEquals(request.getBodyType(), "raw");
        assertTrue(request.getBody().contains("Updated Name"));
    }

    @Test(description = "测试解析 urlencoded body 带空值")
    public void testParseUrlencodedWithEmptyValues() {
        String content = """
                ### 表单提交
                POST https://api.example.com/form
                Content-Type: application/x-www-form-urlencoded

                name=John&email=&age=30&empty=
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);

        HttpRequestItem request = getRequestFromNode(result, 0);
        assertEquals(request.getBodyType(), "urlencoded");
        assertTrue(hasUrlencodedField(request, "name", "John"));
        assertTrue(hasUrlencodedField(request, "email", ""));
        assertTrue(hasUrlencodedField(request, "age", "30"));
        assertTrue(hasUrlencodedField(request, "empty", ""));
    }

    @Test(description = "测试解析复杂场景：多个请求带各种配置")
    public void testParseComplexScenario() {
        String content = """
                ### 获取用户
                GET https://api.example.com/users
                Accept: application/json
                Authorization: Bearer token123

                ### 创建用户
                POST https://api.example.com/users
                Content-Type: application/json
                Authorization: Basic dXNlcm5hbWU6cGFzc3dvcmQ=

                {
                  "name": "John",
                  "email": "john@example.com"
                }

                ### 提交表单
                POST https://api.example.com/form
                Content-Type: application/x-www-form-urlencoded

                name=John&email=john@example.com
                """;

        DefaultMutableTreeNode result = HttpFileParser.parseHttpFile(content);
        assertNotNull(result);
        assertEquals(result.getChildCount(), 3, "应该有 3 个请求");

        // 验证第一个请求（GET with Bearer）
        HttpRequestItem req1 = getRequestFromNode(result, 0);
        assertEquals(req1.getMethod(), "GET");
        assertEquals(req1.getAuthType(), AUTH_TYPE_BEARER);
        assertEquals(req1.getAuthToken(), "token123");

        // 验证第二个请求（POST with JSON and Basic Auth）
        HttpRequestItem req2 = getRequestFromNode(result, 1);
        assertEquals(req2.getMethod(), "POST");
        assertEquals(req2.getAuthType(), AUTH_TYPE_BASIC);
        assertEquals(req2.getBodyType(), "raw");
        assertTrue(req2.getBody().contains("John"));

        // 验证第三个请求（POST with urlencoded）
        HttpRequestItem req3 = getRequestFromNode(result, 2);
        assertEquals(req3.getMethod(), "POST");
        assertEquals(req3.getBodyType(), "urlencoded");
        assertTrue(hasUrlencodedField(req3, "name", "John"));
    }

    // 辅助方法：从节点获取 HttpRequestItem
    private HttpRequestItem getRequestFromNode(DefaultMutableTreeNode root, int index) {
        DefaultMutableTreeNode requestNode = (DefaultMutableTreeNode) root.getChildAt(index);
        Object[] userObject = (Object[]) requestNode.getUserObject();
        return (HttpRequestItem) userObject[1];
    }

    // 辅助方法：检查请求是否有指定的请求头
    private boolean hasHeader(HttpRequestItem request, String key, String value) {
        if (request.getHeadersList() == null) {
            return false;
        }
        return request.getHeadersList().stream()
                .anyMatch(h -> key.equals(h.getKey()) && value.equals(h.getValue()));
    }

    // 辅助方法：检查请求是否有指定的 urlencoded 字段
    private boolean hasUrlencodedField(HttpRequestItem request, String key, String value) {
        if (request.getUrlencodedList() == null) {
            return false;
        }
        return request.getUrlencodedList().stream()
                .anyMatch(f -> key.equals(f.getKey()) && value.equals(f.getValue()));
    }
}
