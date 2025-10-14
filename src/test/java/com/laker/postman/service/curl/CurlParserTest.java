package com.laker.postman.service.curl;

import com.laker.postman.model.CurlRequest;
import com.laker.postman.model.PreparedRequest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

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

    // ===================
    // Windows CMD 格式测试
    // ===================

    @Test(description = "测试Windows CMD格式的多行cURL命令 - 使用^续行符")
    public void testWindowsCmdMultiLineCurl() {
        String curl = """
                curl -X POST ^
                  "https://api.example.com/users" ^
                  -H "Content-Type: application/json" ^
                  -d "{\\"name\\":\\"John\\"}"
                """;
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "POST");
        assertEquals(result.headers.get("Content-Type"), "application/json");
        assertEquals(result.body, "{\"name\":\"John\"}");
    }

    @Test(description = "测试Windows CMD格式的POST请求带JSON数据")
    public void testWindowsCmdPostRequestWithJsonData() {
        String curl = "curl -X POST \"https://api.example.com/users\" " +
                "-H \"Content-Type: application/json\" " +
                "-d \"{\\\"name\\\":\\\"John\\\",\\\"email\\\":\\\"john@example.com\\\"}\"";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "POST");
        assertEquals(result.headers.get("Content-Type"), "application/json");
        assertEquals(result.body, "{\"name\":\"John\",\"email\":\"john@example.com\"}");
    }

    @Test(description = "测试Windows CMD格式的表单数据请求")
    public void testWindowsCmdFormDataRequest() {
        String curl = "curl -X POST \"https://api.example.com/upload\" " +
                "-F \"name=John\" " +
                "-F \"email=john@example.com\" " +
                "-F \"file=@C:\\\\path\\\\to\\\\file.txt\"";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/upload");
        assertEquals(result.method, "POST");
        assertEquals(result.headers.get("Content-Type"), "multipart/form-data");
        assertEquals(result.formData.get("name"), "John");
        assertEquals(result.formData.get("email"), "john@example.com");
        assertEquals(result.formFiles.get("file"), "C:\\path\\to\\file.txt");
    }

    @Test(description = "测试Windows CMD格式的复杂多行命令")
    public void testWindowsCmdComplexMultiLineCommand() {
        String curl = """
                curl -L ^
                  -X POST ^
                  "https://api.example.com/users?active=true" ^
                  -H "Content-Type: application/json" ^
                  -H "Authorization: Bearer token123" ^
                  -H "User-Agent: EasyPostman/2.1.0" ^
                  -b "session=abc123; csrf_token=xyz789" ^
                  -d "{\\"name\\":\\"测试用户\\",\\"age\\":25,\\"active\\":true}"
                """;

        CurlRequest result = CurlParser.parse(curl);

        // 验证基本属性
        assertEquals(result.url, "https://api.example.com/users?active=true");
        assertEquals(result.method, "POST");
        assertTrue(result.followRedirects);

        // 验证请求头
        assertEquals(result.headers.get("Content-Type"), "application/json");
        assertEquals(result.headers.get("Authorization"), "Bearer token123");
        assertEquals(result.headers.get("User-Agent"), "EasyPostman/2.1.0");
        assertEquals(result.headers.get("Cookie"), "session=abc123; csrf_token=xyz789");

        // 验证请求体
        assertEquals(result.body, "{\"name\":\"测试用户\",\"age\":25,\"active\":true}");

        // 验证查询参数
        assertEquals(result.params.get("active"), "true");
    }

    @Test(description = "测试Windows CMD格式的双引号转义")
    public void testWindowsCmdDoubleQuoteEscaping() {
        String curl = "curl -X POST \"https://api.example.com/data\" " +
                "-d \"{\\\"message\\\":\\\"Hello World!\\\",\\\"path\\\":\\\"C:\\\\\\\\Users\\\"}\"";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.method, "POST");
        assertTrue(result.body.contains("Hello World!"));
        assertTrue(result.body.contains("C:\\Users"));
    }

    @Test(description = "测试Windows CMD格式的路径处理")
    public void testWindowsCmdPathHandling() {
        String curl = "curl -X POST \"https://api.example.com/upload\" " +
                "-F \"file=@C:\\\\Users\\\\Admin\\\\Documents\\\\test.txt\" " +
                "-F \"config=@D:\\\\config\\\\app.json\"";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.formFiles.get("file"), "C:\\Users\\Admin\\Documents\\test.txt");
        assertEquals(result.formFiles.get("config"), "D:\\config\\app.json");
    }

    @Test(description = "测试Windows CMD格式的Cookie处理")
    public void testWindowsCmdCookieHandling() {
        String curl = "curl \"https://api.example.com/users\" " +
                "-b \"session=abc123; lang=en; theme=dark\"";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.headers.get("Cookie"), "session=abc123; lang=en; theme=dark");
    }

    @Test(description = "测试Windows CMD格式的URL参数")
    public void testWindowsCmdUrlParameter() {
        String curl = "curl --url \"https://api.example.com/users\" -X GET";
        CurlRequest result = CurlParser.parse(curl);

        assertEquals(result.url, "https://api.example.com/users");
        assertEquals(result.method, "GET");
    }

    @Test(description = "测试Windows CMD格式的边界情况")
    public void testWindowsCmdEdgeCases() {
        // 测试简单的续行符
        String curl1 = "curl \"https://api.example.com\"";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertEquals(result1.url, "https://api.example.com");

        // 测试带续行符的命令
        String curl2 = """
                curl ^
                  "https://api.example.com" ^
                  -X GET
                """;
        CurlRequest result2 = CurlParser.parse(curl2);
        assertEquals(result2.url, "https://api.example.com");
        assertEquals(result2.method, "GET");
    }

    @Test(description = "测试Windows CMD格式的数据参数变体")
    public void testWindowsCmdDataParameterVariants() {
        // 测试 --data-raw
        String curl1 = "curl --data-raw \"test data\" \"https://api.example.com\"";
        CurlRequest result1 = CurlParser.parse(curl1);
        assertEquals(result1.method, "POST");
        assertEquals(result1.body, "test data");

        // 测试 --data-binary
        String curl2 = "curl --data-binary \"binary data\" \"https://api.example.com\"";
        CurlRequest result2 = CurlParser.parse(curl2);
        assertEquals(result2.method, "POST");
        assertEquals(result2.body, "binary data");
    }

    @Test(description = "测试Windows CMD格式的完整场景")
    public void testWindowsCmdCompleteScenario() {
        String curl = """
                curl -L ^
                  -X PUT ^
                  "https://api.example.com/users/123?timestamp=1640995200" ^
                  -H "Content-Type: application/json" ^
                  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.token.signature" ^
                  -H "Accept: application/json" ^
                  -H "User-Agent: EasyPostman/2.1.0 (Windows NT 10.0)" ^
                  -b "JSESSIONID=1234567890ABCDEF; Path=/; Secure" ^
                  -d "{\\"id\\":123,\\"name\\":\\"Updated User\\",\\"email\\":\\"updated@example.com\\",\\"active\\":true}"
                """;

        CurlRequest result = CurlParser.parse(curl);

        // 验证基本属性
        assertEquals(result.url, "https://api.example.com/users/123?timestamp=1640995200");
        assertEquals(result.method, "PUT");
        assertTrue(result.followRedirects);

        // 验证请求头
        assertEquals(result.headers.get("Content-Type"), "application/json");
        assertEquals(result.headers.get("Authorization"), "Bearer eyJhbGciOiJIUzI1NiJ9.token.signature");
        assertEquals(result.headers.get("Accept"), "application/json");
        assertEquals(result.headers.get("User-Agent"), "EasyPostman/2.1.0 (Windows NT 10.0)");
        assertEquals(result.headers.get("Cookie"), "JSESSIONID=1234567890ABCDEF; Path=/; Secure");

        // 验证请求体
        String expectedBody = "{\"id\":123,\"name\":\"Updated User\",\"email\":\"updated@example.com\",\"active\":true}";
        assertEquals(result.body, expectedBody);

        // 验证查询参数
        assertEquals(result.params.get("timestamp"), "1640995200");
    }

    @Test(description = "解析WebSocket连接的curl命令")
    public void testParseWebSocketCurlCommand() {
        String curl = "curl 'wss://echo-websocket.hoppscotch.io/' " +
                "-H 'Upgrade: websocket' " +
                "-H 'Origin: https://hoppscotch.io' " +
                "-H 'Cache-Control: no-cache' " +
                "-H 'Accept-Language: zh-CN,zh;q=0.9' " +
                "-H 'Pragma: no-cache' " +
                "-H 'Connection: Upgrade' " +
                "-H 'Sec-WebSocket-Key: ISD3FRCDNdAPiU+0dTuSXg==' " +
                "-H 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36' " +
                "-H 'Sec-WebSocket-Version: 13' " +
                "-H 'Sec-WebSocket-Extensions: permessage-deflate; client_max_window_bits'";

        CurlRequest result = CurlParser.parse(curl);

        // 验证URL和基本信息解析正确
        assertEquals(result.url, "wss://echo-websocket.hoppscotch.io/");
        assertEquals(result.method, "GET"); // WebSocket握手使用GET方法

        // 验证受限制的WebSocket头部已被过滤掉
        assertNull(result.headers.get("Upgrade"));
        assertNull(result.headers.get("Connection"));
        assertNull(result.headers.get("Sec-WebSocket-Key"));
        assertNull(result.headers.get("Sec-WebSocket-Version"));
        assertNull(result.headers.get("Sec-WebSocket-Extensions"));

        // 验证非受限制的头部正常保留
        assertEquals(result.headers.get("Origin"), "https://hoppscotch.io");
        assertEquals(result.headers.get("Cache-Control"), "no-cache");
        assertEquals(result.headers.get("Accept-Language"), "zh-CN,zh;q=0.9");
        assertEquals(result.headers.get("Pragma"), "no-cache");
        assertEquals(result.headers.get("User-Agent"), "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36");
    }

    @Test(description = "测试WebSocket请求的受限头部过滤")
    public void testWebSocketRestrictedHeaderFiltering() {
        String curl = "curl 'wss://echo-websocket.hoppscotch.io/' " +
                "-H 'Upgrade: websocket' " +
                "-H 'Origin: https://hoppscotch.io' " +
                "-H 'Connection: Upgrade' " +
                "-H 'Sec-WebSocket-Key: ISD3FRCDNdAPiU+0dTuSXg==' " +
                "-H 'User-Agent: Mozilla/5.0' " +
                "-H 'Sec-WebSocket-Version: 13' " +
                "-H 'Sec-WebSocket-Extensions: permessage-deflate' " +
                "-H 'Accept-Language: zh-CN,zh;q=0.9'";

        CurlRequest result = CurlParser.parse(curl);

        // 验证URL和基本信息解析正确
        assertEquals(result.url, "wss://echo-websocket.hoppscotch.io/");
        assertEquals(result.method, "GET");

        // 验证受限制的WebSocket头部已被过滤掉
        assertNull(result.headers.get("Upgrade"));
        assertNull(result.headers.get("Connection"));
        assertNull(result.headers.get("Sec-WebSocket-Key"));
        assertNull(result.headers.get("Sec-WebSocket-Version"));
        assertNull(result.headers.get("Sec-WebSocket-Extensions"));

        // 验证非受限制的头部正常保留
        assertEquals(result.headers.get("Origin"), "https://hoppscotch.io");
        assertEquals(result.headers.get("User-Agent"), "Mozilla/5.0");
        assertEquals(result.headers.get("Accept-Language"), "zh-CN,zh;q=0.9");
    }

    @Test(description = "测试普通HTTP请求不受WebSocket头部过滤影响")
    public void testHttpRequestWithWebSocketHeaders() {
        String curl = "curl 'https://api.example.com/test' " +
                "-H 'Sec-WebSocket-Key: test-key' " +
                "-H 'Sec-WebSocket-Version: 13' " +
                "-H 'Connection: keep-alive' " +
                "-H 'User-Agent: TestAgent'";

        CurlRequest result = CurlParser.parse(curl);

        // 验证普通HTTP请求允许设置这些头部
        assertEquals(result.url, "https://api.example.com/test");
        assertEquals(result.method, "GET");
        assertEquals(result.headers.get("Sec-WebSocket-Key"), "test-key");
        assertEquals(result.headers.get("Sec-WebSocket-Version"), "13");
        assertEquals(result.headers.get("Connection"), "keep-alive");
        assertEquals(result.headers.get("User-Agent"), "TestAgent");
    }

    @Test(description = "解析用户提供的复杂 curl 示例")
    public void testParseProvidedCurlExample() {
        // Use Java 17 text block for the multi-line curl command
        var curl = """
                curl 'http://127.0.0.1:8801/app-api/v1/ChangeClothes/getChangeClothesList' \
                  -H 'Accept: */*' \
                  -H 'Accept-Language: zh-CN,zh;q=0.9' \
                  -H 'Authorization: bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX25hbWUiOiIxNTMyNjUyMjY1MyIsInNjb3BlIjpbImFsbCJdLCJleHAiOjE4MjA5MjMxMTcsImp0aSI6IlAwenZRQk5yT05RUF9zeU85ZFFjeU9qLU1icyIsImNsaWVudF9pZCI6Im1hbGwtd2VhcHAiLCJtZW1iZXJJZCI6IjdxNXIxajN6eGU4YTMzYjE5OHh2em15NSIsInVzZXJuYW1lIjoiMTUzMjY1MjI2NTMifQ.i12RSQz59sA9kjEcuXE0ljH11SJDSljaMHThzEnAdBTkUw3lt629f7bzBeD5P00xXBib8TrT2YI_Y0XLK36EMcQ3MUXhQlr_osKQgYpEWlypF4xOTnda_9fPPbjMWZvGUeDul86hX_S0Lxw1Fr9HhSbrL71pHmRtrh2KGYysbWIrXLoGxQKKg4-TuTSm-HNSLk_eX_Ob4aP1HXEhIiCEdG37b92BI5HBwmsMLl1Ir4IRmyjaos277wbL4NAr7ufBmvtduA6QeoK_PtoE5_iMRUe89WyCoCgMFa4PY2NEJteehuQ-stUyoDE9Y3g5nuJESi0xrg-WQjDbfUe2xNPZ_w' \
                  -H 'Cache-Control: no-cache' \
                  -H 'Connection: keep-alive' \
                  -H 'Content-Type: application/json' \
                  -H 'Origin: http://127.0.0.1:8801' \
                  -H 'Pragma: no-cache' \
                  -H 'Referer: http://127.0.0.1:8801/doc.html' \
                  -H 'Request-Origion: Knife4j' \
                  -H 'Sec-Fetch-Dest: empty' \
                  -H 'Sec-Fetch-Mode: cors' \
                  -H 'Sec-Fetch-Site: same-origin' \
                  -H 'User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36' \
                  -H 'knife4j-gateway-code: ROOT' \
                  -H 'sec-ch-ua: "Google Chrome";v="141", "Not?A_Brand";v="8", "Chromium";v="141"' \
                  -H 'sec-ch-ua-mobile: ?0' \
                  -H 'sec-ch-ua-platform: "Windows"' \
                  --data-raw $'{\\n  "id": "",\\n  "memberId": "",\\n  "pageNum": 1,\\n  "pageSize": 10\\n}'
                """;

        var req = CurlParser.parse(curl);

        assertNotNull(req);
        assertEquals(req.url, "http://127.0.0.1:8801/app-api/v1/ChangeClothes/getChangeClothesList");
        // 有 body 的请求默认应为 POST
        assertEquals(req.method, "POST");
        // 常见头部检查
        assertEquals(req.headers.get("Content-Type"), "application/json");
        assertTrue(req.headers.containsKey("Authorization"));
        assertEquals(req.headers.get("Request-Origion"), "Knife4j");

        // 请求体应包含关键字段（unescape 后）
        assertNotNull(req.body);
        assertTrue(!req.body.contains("n"));
        assertTrue(req.body.contains("\"pageNum\": 1"));
        assertTrue(req.body.contains("\"pageSize\": 10"));
        assertTrue(req.body.contains("\"id\": \"\""));
        assertTrue(req.body.contains("\"memberId\": \"\""));
    }

    @Test(description = "测试真实的 $'...' 格式的 cURL 命令")
    public void testParseRealCurlWithDollarQuote() {
        String curl = "curl 'http://127.0.0.1:8801/app-api/v1/ChangeClothes/getChangeClothesList' \\\n" +
                "  -H 'Accept: */*' \\\n" +
                "  -H 'Accept-Language: zh-CN,zh;q=0.9' \\\n" +
                "  -H 'Authorization: bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9' \\\n" +
                "  -H 'Cache-Control: no-cache' \\\n" +
                "  -H 'Connection: keep-alive' \\\n" +
                "  -H 'Content-Type: application/json' \\\n" +
                "  -H 'Origin: http://127.0.0.1:8801' \\\n" +
                "  -H 'Pragma: no-cache' \\\n" +
                "  -H 'Referer: http://127.0.0.1:8801/doc.html' \\\n" +
                "  -H 'Request-Origion: Knife4j' \\\n" +
                "  -H 'User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' \\\n" +
                "  --data-raw $'{\\n  \"id\": \"\",\\n  \"memberId\": \"\",\\n  \"pageNum\": 1,\\n  \"pageSize\": 10\\n}'";

        CurlRequest result = CurlParser.parse(curl);

        // 验证 URL
        assertEquals(result.url, "http://127.0.0.1:8801/app-api/v1/ChangeClothes/getChangeClothesList");

        // 验证方法
        assertEquals(result.method, "POST");

        // 验证 headers
        assertEquals(result.headers.get("Accept"), "*/*");
        assertEquals(result.headers.get("Content-Type"), "application/json");
        assertEquals(result.headers.get("Authorization"), "bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9");

        // 验证 body - 应该包含真正的换行符
        assertNotNull(result.body);
        assertTrue(result.body.contains("\n"), "Body should contain newline characters");
        assertTrue(result.body.contains("\"id\":"), "Body should contain id field");
        assertTrue(result.body.contains("\"memberId\":"), "Body should contain memberId field");
        assertTrue(result.body.contains("\"pageNum\": 1"), "Body should contain pageNum field");
        assertTrue(result.body.contains("\"pageSize\": 10"), "Body should contain pageSize field");

        // 验证 body 不包含字面的 \n
        assertTrue(!result.body.contains("\\n"), "Body should not contain literal \\n");
    }
}
