package com.laker.postman.service.curl;

import com.laker.postman.model.CurlRequest;
import com.laker.postman.model.PreparedRequest;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import java.util.LinkedHashMap;

import static org.testng.Assert.*;

/**
 * CurlParser 单元测试类
 * 测试 cURL 命令解析器的各种功能
 */
public class CurlParserTest {

    @Test(description = "解析简单的GET请求")
    public void testParseSimpleGetRequest() {
        String curl = "curl https://api.example.com/users";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "GET");
        assertTrue(result.headers.isEmpty());
        assertNull(result.body);
        assertTrue(result.params.isEmpty());
        assertFalse(result.followRedirects);
    }

    @Test(description = "解析带查询参数的GET请求")
    public void testParseGetRequestWithQueryParams() {
        String curl = "curl 'https://api.example.com/users?page=1&limit=10&sort=name'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users?page=1&limit=10&sort=name");
        assertEquals(result.method, "GET");
        assertEquals(result.params.get("page"), "1");
        assertEquals(result.params.get("limit"), "10");
        assertEquals(result.params.get("sort"), "name");
    }

    @Test(description = "解析POST请求带JSON数据")
    public void testParsePostRequestWithJsonData() {
        String curl = "curl -X POST https://api.example.com/users " +
                     "-H 'Content-Type: application/json' " +
                     "-d '{\"name\":\"John\",\"email\":\"john@example.com\"}'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "POST");
        assertEquals(result.headers.get("Content-Type"), "application/json");
        assertEquals(result.body, "{\"name\":\"John\",\"email\":\"john@example.com\"}");
    }

    @Test(description = "解析带多个请求头的请求")
    public void testParseRequestWithMultipleHeaders() {
        String curl = "curl -X GET https://api.example.com/users " +
                     "-H 'Authorization: Bearer token123' " +
                     "-H 'Accept: application/json' " +
                     "-H 'User-Agent: MyApp/1.0'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "GET");
        assertEquals(result.headers.get("Authorization"), "Bearer token123");
        assertEquals(result.headers.get("Accept"), "application/json");
        assertEquals(result.headers.get("User-Agent"), "MyApp/1.0");
    }

    @Test(description = "解析带Cookie的请求")
    public void testParseRequestWithCookie() {
        String curl = "curl https://api.example.com/users -b 'session=abc123; lang=en'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.headers.get("Cookie"), "session=abc123; lang=en");
    }

    @Test(description = "解析带重定向标志的请求")
    public void testParseRequestWithRedirectFlag() {
        String curl = "curl -L https://api.example.com/users";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertTrue(result.followRedirects);

        // 测试长格式
        curl = "curl --location https://api.example.com/users";
        result = CurlParser.parse(curl);
        assertTrue(result.followRedirects);
    }

    @Test(description = "解析表单数据请求")
    public void testParseFormDataRequest() {
        String curl = "curl -X POST https://api.example.com/upload " +
                     "-F 'name=John' " +
                     "-F 'email=john@example.com' " +
                     "-F 'file=@/path/to/file.txt'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/upload");
        assertEquals(result.method, "POST");
        assertEquals(result.headers.get("Content-Type"), "multipart/form-data");
        assertEquals(result.formData.get("name"), "John");
        assertEquals(result.formData.get("email"), "john@example.com");
        assertEquals(result.formFiles.get("file"), "/path/to/file.txt");
    }

    @Test(description = "解析带转义字符的数据")
    public void testParseDataWithEscapeCharacters() {
        String curl = "curl -X POST https://api.example.com/data " +
                     "-d '{\"message\":\"Hello\\nWorld\\t!\",\"path\":\"C:\\\\Users\"}'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.method, "POST");
        assertTrue(result.body.contains("Hello\nWorld\t!"));
        assertTrue(result.body.contains("C:\\Users"));
    }

    @Test(description = "解析多行cURL命令")
    public void testParseMultiLineCurlCommand() {
        String curl = """
                curl -X POST \\
                  https://api.example.com/users \\
                  -H 'Content-Type: application/json' \\
                  -d '{"name":"John"}'
                """;
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "POST");
        assertEquals(result.headers.get("Content-Type"), "application/json");
        assertEquals(result.body, "{\"name\":\"John\"}");
    }

    @Test(description = "解析带URL参数的请求")
    public void testParseRequestWithUrlParameter() {
        String curl = "curl --url https://api.example.com/users -X GET";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "GET");
    }

    @Test(description = "解析不同的数据参数格式")
    public void testParseDataParameterVariants() {
        // 测试 -d
        String curl1 = "curl -d 'test data' https://api.example.com";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertEquals(result1.method, "POST");
        assertEquals(result1.body, "test data");

        // 测试 --data
        String curl2 = "curl --data 'test data' https://api.example.com";
        CurlRequest result2 = CurlParser.parse(curl2);
        assertEquals(result2.method, "POST");
        assertEquals(result2.body, "test data");

        // 测试 --data-raw
        String curl3 = "curl --data-raw 'test data' https://api.example.com";
        CurlRequest result3 = CurlParser.parse(curl3);
        assertEquals(result3.method, "POST");
        assertEquals(result3.body, "test data");

        // 测试 --data-binary
        String curl4 = "curl --data-binary 'test data' https://api.example.com";
        CurlRequest result4 = CurlParser.parse(curl4);
        assertEquals(result4.method, "POST");
        assertEquals(result4.body, "test data");
    }

    @Test(description = "解析带引号的复杂命令")
    public void testParseComplexQuotedCommand() {
        String curl = "curl -X POST 'https://api.example.com/users' " +
                     "-H \"Content-Type: application/json\" " +
                     "-H 'Authorization: Bearer token' " +
                     "-d $'{\"data\": \"test\"}'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "POST");
        assertEquals(result.headers.get("Content-Type"), "application/json");
        assertEquals(result.headers.get("Authorization"), "Bearer token");
        assertEquals(result.body, "{\"data\": \"test\"}");
    }

    @Test(description = "测试默认方法推断")
    public void testDefaultMethodInference() {
        // 没有数据和方法的请求应该默认为GET
        String curl1 = "curl https://api.example.com/users";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertEquals(result1.method, "GET");

        // 有数据但没有指定方法的请求应该默认为POST
        String curl2 = "curl https://api.example.com/users -d 'data'";
        CurlRequest result2 = CurlParser.parse(curl2);
        assertEquals(result2.method, "POST");

        // 有表单数据但没有指定方法的请求应该默认为POST
        String curl3 = "curl https://api.example.com/users -F 'name=test'";
        CurlRequest result3 = CurlParser.parse(curl3);
        assertEquals(result3.method, "POST");
    }

    @Test(description = "测试空值和边界情况")
    public void testNullAndEdgeCases() {
        // 空字符串
        CurlRequest result1 = CurlParser.parse("");
        assertEquals(result1.method, "GET");
        assertNull(result1.url);

        // 只有curl命令
        CurlRequest result2 = CurlParser.parse("curl");
        assertEquals(result2.method, "GET");
        assertNull(result2.url);

        // null输入
        CurlRequest result3 = CurlParser.parse(null);
        assertEquals(result3.method, "GET");
        assertNull(result3.url);
    }

    @Test(description = "测试PreparedRequest转cURL功能")
    public void testToCurlMethod() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "POST";
        preparedRequest.url = "https://api.example.com/users";
        preparedRequest.headers = new LinkedHashMap<>();
        preparedRequest.headers.put("Content-Type", "application/json");
        preparedRequest.headers.put("Authorization", "Bearer token");
        preparedRequest.body = "{\"name\":\"John\"}";

        String curlCommand = CurlParser.toCurl(preparedRequest);

        assertTrue(curlCommand.startsWith("curl"));
        assertTrue(curlCommand.contains("-X POST"));
        assertTrue(curlCommand.contains("\"https://api.example.com/users\""));
        assertTrue(curlCommand.contains("-H \"Content-Type: application/json\""));
        assertTrue(curlCommand.contains("-H \"Authorization: Bearer token\""));
        assertTrue(curlCommand.contains("--data"));
        assertTrue(curlCommand.contains("'{\"name\":\"John\"}'"));
    }

    @Test(description = "测试PreparedRequest转cURL - 表单数据")
    public void testToCurlWithFormData() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "POST";
        preparedRequest.url = "https://api.example.com/upload";
        preparedRequest.formData = new LinkedHashMap<>();
        preparedRequest.formData.put("name", "John");
        preparedRequest.formData.put("email", "john@example.com");
        preparedRequest.formFiles = new LinkedHashMap<>();
        preparedRequest.formFiles.put("file", "/path/to/file.txt");

        String curlCommand = CurlParser.toCurl(preparedRequest);

        assertTrue(curlCommand.contains("-F 'name=John'"));
        assertTrue(curlCommand.contains("-F 'email=john@example.com'"));
        assertTrue(curlCommand.contains("-F 'file=@/path/to/file.txt'"));
    }

    @Test(description = "测试PreparedRequest转cURL - GET请求")
    public void testToCurlGetRequest() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "GET";
        preparedRequest.url = "https://api.example.com/users";
        preparedRequest.headers = new LinkedHashMap<>();
        preparedRequest.headers.put("Accept", "application/json");

        String curlCommand = CurlParser.toCurl(preparedRequest);

        assertTrue(curlCommand.startsWith("curl"));
        assertFalse(curlCommand.contains("-X GET")); // GET方法不应该显式指定
        assertTrue(curlCommand.contains("\"https://api.example.com/users\""));
        assertTrue(curlCommand.contains("-H \"Accept: application/json\""));
    }

    @Test(description = "测试Shell参数转义")
    public void testShellArgumentEscaping() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.method = "POST";
        preparedRequest.url = "https://api.example.com/users";
        preparedRequest.body = "data with 'single quotes' and \"double quotes\"";

        String curlCommand = CurlParser.toCurl(preparedRequest);

        // 应该正确转义引号
        assertTrue(curlCommand.contains("\"data with 'single quotes' and \\\"double quotes\\\"\""));
    }

    @DataProvider(name = "getRequestFormats")
    public Object[][] getRequestFormats() {
        return new Object[][]{
            {"curl https://example.com"},
            {"curl -X GET https://example.com"},
            {"curl --request GET https://example.com"},
            {"curl https://example.com -X GET"}
        };
    }

    @Test(description = "测试不同格式的GET请求", dataProvider = "getRequestFormats")
    public void testVariousGetRequestFormats(String curlCommand) {
        CurlRequest result = CurlParser.parse(curlCommand);
        assertEquals(result.url, "https://example.com");
        assertEquals(result.method, "GET");
    }

    @Test(description = "测试请求头分割逻辑")
    public void testHeaderSplitting() {
        String curl = "curl https://api.example.com -H 'Content-Type:application/json' " +
                     "-H 'Authorization: Bearer token:with:colons'";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.headers.get("Content-Type"), "application/json");
        assertEquals(result.headers.get("Authorization"), "Bearer token:with:colons");
    }

    @Test(description = "测试查询参数解析边界情况")
    public void testQueryParamEdgeCases() {
        // 没有值的参数
        String curl1 = "curl 'https://api.example.com?flag&key=value'";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertEquals(result1.params.get("flag"), "");
        assertEquals(result1.params.get("key"), "value");

        // 空值参数
        String curl2 = "curl 'https://api.example.com?empty=&key=value'";
        CurlRequest result2 = CurlParser.parse(curl2);
        assertEquals(result2.params.get("empty"), "");
        assertEquals(result2.params.get("key"), "value");
    }

    @Test(description = "测试复杂的表单字段解析")
    public void testComplexFormFieldParsing() {
        String curl = "curl -X POST https://api.example.com/upload " +
                     "-F 'field_without_value' " +
                     "-F 'normal_field=value' " +
                     "-F 'file_field=@/path/file.txt'";
        CurlRequest result = CurlParser.parse(curl);

        // 没有等号的字段应该被忽略
        assertFalse(result.formData.containsKey("field_without_value"));
        assertEquals(result.formData.get("normal_field"), "value");
        assertEquals(result.formFiles.get("file_field"), "/path/file.txt");
    }

    @Test(description = "测试URL和方法的不同位置")
    public void testUrlAndMethodPositions() {
        // URL在前，方法在后
        String curl1 = "curl https://api.example.com -X POST";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertEquals(result1.url, "https://api.example.com");
        assertEquals(result1.method, "POST");

        // 方法在前，URL在后
        String curl2 = "curl -X POST https://api.example.com";
        CurlRequest result2 = CurlParser.parse(curl2);
        assertEquals(result2.url, "https://api.example.com");
        assertEquals(result2.method, "POST");
    }

    @Test(description = "测试Cookie参数的不同格式")
    public void testCookieParameterFormats() {
        // 短格式
        String curl1 = "curl -b 'session=123' https://api.example.com";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertEquals(result1.headers.get("Cookie"), "session=123");

        // 长格式
        String curl2 = "curl --cookie 'session=123' https://api.example.com";
        CurlRequest result2 = CurlParser.parse(curl2);
        assertEquals(result2.headers.get("Cookie"), "session=123");
    }

    @Test(description = "测试请求头参数的不同格式")
    public void testHeaderParameterFormats() {
        // 短格式
        String curl1 = "curl -H 'Content-Type: application/json' https://api.example.com";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertEquals(result1.headers.get("Content-Type"), "application/json");

        // 长格式
        String curl2 = "curl --header 'Content-Type: application/json' https://api.example.com";
        CurlRequest result2 = CurlParser.parse(curl2);
        assertEquals(result2.headers.get("Content-Type"), "application/json");
    }

    @Test(description = "测试HTTP方法大小写转换")
    public void testHttpMethodCaseConversion() {
        String curl = "curl -X post https://api.example.com";
        CurlRequest result = CurlParser.parse(curl);
        assertEquals(result.method, "POST"); // 应该转换为大写
    }

    @Test(description = "测试转义字符处理")
    public void testEscapeCharacterHandling() {
        String curl = "curl -d 'line1\\nline2\\tindented\\\\backslash\\\"quote' https://api.example.com";
        CurlRequest result = CurlParser.parse(curl);

        String expectedBody = "line1\nline2\tindented\\backslash\"quote";
        assertEquals(result.body, expectedBody);
    }

    @Test(description = "测试PreparedRequest为null的情况")
    public void testToCurlWithNullPreparedRequest() {
        // 测试各种null情况
        PreparedRequest preparedRequest = new PreparedRequest();

        String curlCommand = CurlParser.toCurl(preparedRequest);
        assertEquals(curlCommand, "curl");

        // 测试部分null
        preparedRequest.url = "https://api.example.com";
        curlCommand = CurlParser.toCurl(preparedRequest);
        assertTrue(curlCommand.contains("\"https://api.example.com\""));
    }

    @Test(description = "测试复合场景 - 完整的cURL命令")
    public void testCompleteScenario() {
        String curl = "curl -L -X POST 'https://api.example.com/users?active=true' " +
                     "-H 'Content-Type: application/json' " +
                     "-H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9' " +
                     "-H 'User-Agent: EasyPostman/2.1.0' " +
                     "-b 'session=abc123; csrf_token=xyz789' " +
                     "-d '{\"name\":\"测试用户\",\"age\":25,\"active\":true}'";

        CurlRequest result = CurlParser.parse(curl);

        // 验证基本属性
        assertEquals(result.url, "https://api.example.com/users?active=true");
        assertEquals(result.method, "POST");
        assertTrue(result.followRedirects);

        // 验证请求头
        assertEquals(result.headers.get("Content-Type"), "application/json");
        assertEquals(result.headers.get("Authorization"), "Bearer eyJhbGciOiJIUzI1NiJ9");
        assertEquals(result.headers.get("User-Agent"), "EasyPostman/2.1.0");
        assertEquals(result.headers.get("Cookie"), "session=abc123; csrf_token=xyz789");

        // 验证请求体
        assertEquals(result.body, "{\"name\":\"测试用户\",\"age\":25,\"active\":true}");

        // 验证查询参数
        assertEquals(result.params.get("active"), "true");
    }

    @Test(description = "测试往返转换一致性")
    public void testRoundTripConsistency() {
        // 创建一个PreparedRequest
        PreparedRequest original = new PreparedRequest();
        original.method = "PUT";
        original.url = "https://api.example.com/users/123";
        original.headers = new LinkedHashMap<>();
        original.headers.put("Content-Type", "application/json");
        original.headers.put("Accept", "application/json");
        original.body = "{\"name\":\"Updated Name\"}";

        // 转换为cURL命令
        String curlCommand = CurlParser.toCurl(original);

        // 再解析回CurlRequest
        CurlRequest parsed = CurlParser.parse(curlCommand);

        // 验证往返转换的一致性
        assertEquals(parsed.method, original.method);
        assertEquals(parsed.url, original.url);
        assertEquals(parsed.headers.get("Content-Type"), original.headers.get("Content-Type"));
        assertEquals(parsed.headers.get("Accept"), original.headers.get("Accept"));
        assertEquals(parsed.body, original.body);
    }
}
