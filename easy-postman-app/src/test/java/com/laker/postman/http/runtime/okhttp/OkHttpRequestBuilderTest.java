package com.laker.postman.http.runtime.okhttp;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;


import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Objects;

import static org.testng.Assert.*;

/**
 * OkHttpRequestBuilder 单元测试
 */
public class OkHttpRequestBuilderTest {

    private PreparedRequest request;

    @BeforeMethod
    public void setUp() {
        request = new PreparedRequest();
        request.url = "https://api.example.com/test";
        request.method = "POST";
        request.headersList = new ArrayList<>();
    }

    // ==================== 基础请求测试 ====================

    @Test(description = "测试 GET 请求构建")
    public void testBuildGetRequest() {
        request.method = "GET";
        request.headersList.add(new HttpHeader(true, "Accept", "application/json"));

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertNotNull(okRequest);
        assertEquals(okRequest.url().toString(), request.url);
        assertEquals(okRequest.method(), "GET");
        assertNull(okRequest.body(), "GET 请求不应该有 body");
        assertEquals(okRequest.header("Accept"), "application/json");
    }

    @Test(description = "测试 POST 请求构建")
    public void testBuildPostRequest() throws IOException {
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = "{\"name\":\"test\"}";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertNotNull(okRequest);
        assertEquals(okRequest.method(), "POST");
        assertNotNull(okRequest.body());

        String bodyContent = readRequestBody(okRequest.body());
        assertEquals(bodyContent, "{\"name\":\"test\"}");
    }

    @Test(description = "测试 PUT 请求构建")
    public void testBuildPutRequest() {
        request.method = "PUT";
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = "{\"id\":1,\"name\":\"updated\"}";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertEquals(okRequest.method(), "PUT");
        assertNotNull(okRequest.body());
    }

    @Test(description = "测试 DELETE 请求构建")
    public void testBuildDeleteRequest() {
        request.method = "DELETE";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertEquals(okRequest.method(), "DELETE");
        assertNotNull(okRequest.body(), "DELETE 请求应该有空 body");
    }

    @Test(description = "测试 HEAD 请求构建")
    public void testBuildHeadRequest() {
        request.method = "HEAD";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertEquals(okRequest.method(), "HEAD");
        assertNull(okRequest.body(), "HEAD 请求不应该有 body");
    }

    // ==================== JSON5 注释去除测试 ====================

    @Test(description = "测试 JSON5 单行注释去除")
    public void testJson5SingleLineComments() throws IOException {
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = """
                {
                  // 这是用户名
                  "username": "test",
                  // 这是密码
                  "password": "123456"
                }
                """;

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);
        String bodyContent = readRequestBody(okRequest.body());

        assertFalse(bodyContent.contains("//"), "单行注释应该被去除");
        assertFalse(bodyContent.contains("这是用户名"), "注释内容应该被去除");
        assertTrue(bodyContent.contains("username"), "字段应该保留");
        assertTrue(bodyContent.contains("test"), "值应该保留");
    }

    @Test(description = "测试 JSON5 多行注释去除")
    public void testJson5MultiLineComments() throws IOException {
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = """
                {
                  /* 这是一个
                     多行注释 */
                  "name": "John",
                  /* 年龄信息 */
                  "age": 30
                }
                """;

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);
        String bodyContent = readRequestBody(okRequest.body());

        assertFalse(bodyContent.contains("/*"), "多行注释开始符应该被去除");
        assertFalse(bodyContent.contains("*/"), "多行注释结束符应该被去除");
        assertFalse(bodyContent.contains("多行注释"), "注释内容应该被去除");
        assertTrue(bodyContent.contains("name"), "字段应该保留");
        assertTrue(bodyContent.contains("John"), "值应该保留");
    }

    @Test(description = "测试 JSON5 混合注释去除")
    public void testJson5MixedComments() throws IOException {
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = """
                {
                  // 用户信息
                  "user": {
                    /* 姓名 */
                    "name": "Alice",
                    // 邮箱
                    "email": "alice@example.com"
                  },
                  /* 设置项
                     包含多个字段 */
                  "settings": {
                    "theme": "dark" // 主题设置
                  }
                }
                """;

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);
        String bodyContent = readRequestBody(okRequest.body());

        assertFalse(bodyContent.contains("//"), "单行注释应该被去除");
        assertFalse(bodyContent.contains("/*"), "多行注释应该被去除");
        assertTrue(bodyContent.contains("user"), "字段应该保留");
        assertTrue(bodyContent.contains("Alice"), "值应该保留");
        assertTrue(bodyContent.contains("settings"), "字段应该保留");
    }

    @Test(description = "测试无 Content-Type 时默认按 JSON 处理并去除注释")
    public void testDefaultJsonWithComments() throws IOException {
        // 不设置 Content-Type，应该默认为 JSON
        request.body = """
                {
                  // 测试字段
                  "test": "value"
                }
                """;

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);
        String bodyContent = readRequestBody(okRequest.body());

        assertFalse(bodyContent.contains("//"), "默认 JSON 格式应该去除注释");
        assertTrue(bodyContent.contains("test"), "字段应该保留");
        assertTrue(bodyContent.contains("value"), "值应该保留");
        assertNotNull(okRequest.body());
        assertEquals(Objects.requireNonNull(okRequest.body().contentType()).toString(), "application/json; charset=utf-8");
    }

    @Test(description = "测试普通 JSON 不受影响")
    public void testNormalJsonNotAffected() throws IOException {
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = """
                {
                  "name": "Bob",
                  "age": 25,
                  "active": true
                }
                """;

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);
        String bodyContent = readRequestBody(okRequest.body());

        assertTrue(bodyContent.contains("name"), "字段应该保留");
        assertTrue(bodyContent.contains("Bob"), "值应该保留");
        assertTrue(bodyContent.contains("25"), "值应该保留");
    }

    @Test(description = "测试非 JSON Content-Type 不处理注释")
    public void testNonJsonContentTypeNotProcessed() throws IOException {
        request.headersList.add(new HttpHeader(true, "Content-Type", "text/plain"));
        request.body = "// This is not JSON\nSome text content";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);
        String bodyContent = readRequestBody(okRequest.body());

        assertEquals(bodyContent, request.body, "非 JSON 内容应该保持原样");
        assertTrue(bodyContent.contains("//"), "非 JSON 内容的注释符不应被去除");
    }

    @Test(description = "测试 JSON Content-Type 变体")
    public void testJsonContentTypeVariants() throws IOException {
        String[] jsonContentTypes = {
                "application/json",
                "application/json; charset=utf-8",
                "application/json;charset=UTF-8",
                "Application/JSON",
                "APPLICATION/JSON"
        };

        for (String contentType : jsonContentTypes) {
            PreparedRequest req = new PreparedRequest();
            req.url = "https://api.example.com/test";
            req.method = "POST";
            req.headersList = new ArrayList<>();
            req.headersList.add(new HttpHeader(true, "Content-Type", contentType));
            req.body = "{ // comment\n\"test\": \"value\" }";

            Request okRequest = OkHttpRequestBuilder.buildRequest(req);
            String bodyContent = readRequestBody(okRequest.body());

            assertFalse(bodyContent.contains("//"),
                    "Content-Type: " + contentType + " 应该去除注释");
        }
    }

    // ==================== Headers 测试 ====================

    @Test(description = "测试添加多个 Headers")
    public void testMultipleHeaders() {
        request.headersList.add(new HttpHeader(true, "Accept", "application/json"));
        request.headersList.add(new HttpHeader(true, "Authorization", "Bearer token123"));
        request.headersList.add(new HttpHeader(true, "X-Custom-Header", "custom-value"));

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertEquals(okRequest.header("Accept"), "application/json");
        assertEquals(okRequest.header("Authorization"), "Bearer token123");
        assertEquals(okRequest.header("X-Custom-Header"), "custom-value");
    }

    @Test(description = "测试 null Header 值")
    public void testNullHeaderValue() {
        request.headersList.add(new HttpHeader(true, "X-Test", null));

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        // OkHttp returns empty string for non-existent headers, and null values are skipped
        String headerValue = okRequest.header("X-Test");
        assertTrue(headerValue == null || headerValue.isEmpty(), "null 值的 header 应该被跳过或返回空字符串");
    }

    @Test(description = "测试非法 Header 名称被跳过")
    public void testInvalidHeaderNames() {
        request.headersList.add(new HttpHeader(true, "Valid-Header", "value1"));
        request.headersList.add(new HttpHeader(true, "Invalid:Header", "value2")); // 包含冒号，非法
        request.headersList.add(new HttpHeader(true, "", "value3")); // 空名称，非法
        request.headersList.add(new HttpHeader(true, null, "value4")); // null 名称，非法

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertEquals(okRequest.header("Valid-Header"), "value1");
        assertNull(okRequest.header("Invalid:Header"), "非法 header 应该被跳过");
    }

    // ==================== 空 Body 测试 ====================

    @Test(description = "测试空 Body")
    public void testEmptyBody() throws IOException {
        request.body = "";
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertNotNull(okRequest.body());
        assertEquals(readRequestBody(okRequest.body()), "", "空 body 应该是空字符串");
    }

    @Test(description = "测试 null Body")
    public void testNullBody() throws IOException {
        request.body = null;
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertNotNull(okRequest.body());
        assertEquals(readRequestBody(okRequest.body()), "", "null body 应该是空字符串");
    }

    // ==================== Multipart Form Data 测试 ====================

    @Test(description = "测试构建 Multipart 请求")
    public void testBuildMultipartRequest() {
        request.formDataList = new ArrayList<>();
        request.formDataList.add(new HttpFormData(true, "field1", HttpFormData.TYPE_TEXT, "value1"));
        request.formDataList.add(new HttpFormData(true, "field2", HttpFormData.TYPE_TEXT, "value2"));

        Request okRequest = OkHttpRequestBuilder.buildMultipartRequest(request);

        assertNotNull(okRequest);
        assertEquals(okRequest.method(), "POST");
        assertNotNull(okRequest.body());
        assertTrue(Objects.requireNonNull(okRequest.body().contentType()).toString().contains("multipart/form-data"));
    }

    @Test(description = "测试 Multipart 空值字段")
    public void testMultipartWithNullValue() {
        request.formDataList = new ArrayList<>();
        request.formDataList.add(new HttpFormData(true, "field1", HttpFormData.TYPE_TEXT, null));
        request.formDataList.add(new HttpFormData(true, "field2", HttpFormData.TYPE_TEXT, "value2"));

        Request okRequest = OkHttpRequestBuilder.buildMultipartRequest(request);

        assertNotNull(okRequest.body());
    }

    @Test(description = "测试 Multipart 空键被跳过")
    public void testMultipartWithEmptyKey() {
        request.formDataList = new ArrayList<>();
        request.formDataList.add(new HttpFormData(true, "", HttpFormData.TYPE_TEXT, "value1"));
        request.formDataList.add(new HttpFormData(true, "field2", HttpFormData.TYPE_TEXT, "value2"));

        Request okRequest = OkHttpRequestBuilder.buildMultipartRequest(request);

        assertNotNull(okRequest.body());
    }

    @Test(description = "测试 Multipart 文件上传")
    public void testMultipartWithFiles() throws IOException {
        // 创建临时测试文件
        File tempFile = Files.createTempFile("test", ".txt").toFile();
        tempFile.deleteOnExit();
        Files.writeString(tempFile.toPath(), "test content");

        request.formDataList = new ArrayList<>();
        request.formDataList.add(new HttpFormData(true, "file", HttpFormData.TYPE_FILE, tempFile.getAbsolutePath()));

        Request okRequest = OkHttpRequestBuilder.buildMultipartRequest(request);

        assertNotNull(okRequest.body());
        assertTrue(Objects.requireNonNull(okRequest.body().contentType()).toString().contains("multipart/form-data"));
    }

    @Test(description = "测试 Multipart 不存在的文件被忽略")
    public void testMultipartWithNonExistentFile() {
        // 添加一个有效的表单字段，确保 multipart 有至少一个部分
        request.formDataList = new ArrayList<>();
        request.formDataList.add(new HttpFormData(true, "field1", HttpFormData.TYPE_TEXT, "value1"));
        request.formDataList.add(new HttpFormData(true, "file", HttpFormData.TYPE_FILE, "/path/to/nonexistent/file.txt"));

        Request okRequest = OkHttpRequestBuilder.buildMultipartRequest(request);

        assertNotNull(okRequest.body());
        // 不存在的文件应该被忽略，但表单数据应该存在
    }

    // ==================== URL Encoded Form 测试 ====================

    @Test(description = "测试构建 URL Encoded Form 请求")
    public void testBuildFormRequest() {
        request.urlencodedList = new ArrayList<>();
        request.urlencodedList.add(new HttpFormUrlencoded(true, "username", "test"));
        request.urlencodedList.add(new HttpFormUrlencoded(true, "password", "123456"));

        Request okRequest = OkHttpRequestBuilder.buildFormRequest(request);

        assertNotNull(okRequest);
        assertEquals(okRequest.method(), "POST");
        assertNotNull(okRequest.body());
        assertEquals(okRequest.header("Content-Type"),
                "application/x-www-form-urlencoded; charset=UTF-8");
    }

    @Test(description = "测试 Form 请求保留用户指定的 Content-Type")
    public void testFormRequestWithCustomContentType() {
        request.urlencodedList = new ArrayList<>();
        request.urlencodedList.add(new HttpFormUrlencoded(true, "field", "value"));
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/x-www-form-urlencoded; charset=ISO-8859-1"));

        Request okRequest = OkHttpRequestBuilder.buildFormRequest(request);

        assertEquals(okRequest.header("Content-Type"),
                "application/x-www-form-urlencoded; charset=ISO-8859-1",
                "应该保留用户指定的 Content-Type");
    }

    @Test(description = "测试 Form 请求 null 值处理")
    public void testFormRequestWithNullValue() {
        request.urlencodedList = new ArrayList<>();
        request.urlencodedList.add(new HttpFormUrlencoded(true, "field1", null));
        request.urlencodedList.add(new HttpFormUrlencoded(true, "field2", "value2"));

        Request okRequest = OkHttpRequestBuilder.buildFormRequest(request);

        assertNotNull(okRequest.body());
    }

    @Test(description = "测试 Form 请求空键被跳过")
    public void testFormRequestWithEmptyKey() {
        request.urlencodedList = new ArrayList<>();
        request.urlencodedList.add(new HttpFormUrlencoded(true, "", "value1"));
        request.urlencodedList.add(new HttpFormUrlencoded(true, "field2", "value2"));

        Request okRequest = OkHttpRequestBuilder.buildFormRequest(request);

        assertNotNull(okRequest.body());
    }

    // ==================== 边界情况测试 ====================

    @Test(description = "测试没有 headers 的请求")
    public void testRequestWithoutHeaders() {
        request.headersList = null;
        request.body = "{\"test\":\"value\"}";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertNotNull(okRequest);
        // 应该使用默认的 JSON Content-Type
        assertNotNull(okRequest.body());
        assertEquals(Objects.requireNonNull(okRequest.body().contentType()).toString(), "application/json; charset=utf-8");
    }

    @Test(description = "测试空 headers List")
    public void testRequestWithEmptyHeaders() {
        request.headersList = new ArrayList<>();
        request.body = "{\"test\":\"value\"}";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertNotNull(okRequest);
    }

    @Test(description = "测试方法名大小写")
    public void testMethodCaseInsensitive() {
        request.method = "post";
        request.body = "{\"test\":\"value\"}";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertEquals(okRequest.method(), "POST", "方法名应该转为大写");
    }

    @Test(description = "测试 PATCH 方法")
    public void testPatchMethod() {
        request.method = "PATCH";
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = "{\"status\":\"updated\"}";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertEquals(okRequest.method(), "PATCH");
        assertNotNull(okRequest.body());
    }

    @Test(description = "测试 OPTIONS 方法")
    public void testOptionsMethod() {
        request.method = "OPTIONS";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertEquals(okRequest.method(), "OPTIONS");
        assertNotNull(okRequest.body());
    }

    @Test(description = "测试无效 JSON 不会导致崩溃")
    public void testInvalidJsonWithComments() throws IOException {
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = "{ // comment\n invalid json here }";

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertNotNull(okRequest);
        assertNotNull(okRequest.body());
        // 如果 JSON 无效，应该保留原始内容
        String bodyContent = readRequestBody(okRequest.body());
        assertNotNull(bodyContent);
    }

    @Test(description = "测试特殊字符的 JSON")
    public void testJsonWithSpecialCharacters() throws IOException {
        request.headersList.add(new HttpHeader(true, "Content-Type", "application/json"));
        request.body = """
                {
                  "text": "Hello\\nWorld\\t!",
                  "unicode": "中文测试",
                  "emoji": "😀"
                }
                """;

        Request okRequest = OkHttpRequestBuilder.buildRequest(request);

        assertNotNull(okRequest);
        String bodyContent = readRequestBody(okRequest.body());
        assertTrue(bodyContent.contains("中文测试"));
    }

    // ==================== 辅助方法 ====================

    /**
     * 读取 RequestBody 内容
     */
    private String readRequestBody(RequestBody body) throws IOException {
        if (body == null) {
            return "";
        }
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        return buffer.readUtf8();
    }
}

