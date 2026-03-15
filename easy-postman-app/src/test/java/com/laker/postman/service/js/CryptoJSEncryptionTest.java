package com.laker.postman.service.js;

import com.laker.postman.model.Environment;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpParam;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.script.PostmanApiContext;
import com.laker.postman.model.script.ScriptRequestAccessor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.testng.Assert.*;

/**
 * CryptoJS AES 加密测试 - 完整场景测试
 * 测试 pm.environment、pm.request、CryptoJS、btoa、encodeURIComponent 等功能的集成使用
 */
public class CryptoJSEncryptionTest {

    @Test(description = "测试完整的 CryptoJS AES 加密脚本场景")
    public void testCryptoJSEncryptionWithPmContext() {
        // 1. 准备环境变量
        Environment env = new Environment();
        env.setName("Test Environment");
        env.set("secret_key", "1234567890123456"); // 16字节密钥
        env.set("app_code", "myAppCode");
        env.set("app_secret", "myAppSecret");

        // 2. 准备请求对象
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.id = "test-request-001";
        preparedRequest.url = "https://api.example.com/login?username=testuser&password=mypassword123";
        preparedRequest.method = "POST";
        preparedRequest.headersList = new ArrayList<>();
        preparedRequest.paramsList = new ArrayList<>();

        // 添加查询参数
        HttpParam usernameParam = new HttpParam();
        usernameParam.setKey("username");
        usernameParam.setValue("testuser");
        usernameParam.setEnabled(true);
        preparedRequest.paramsList.add(usernameParam);

        HttpParam passwordParam = new HttpParam();
        passwordParam.setKey("password");
        passwordParam.setValue("mypassword123");
        passwordParam.setEnabled(true);
        preparedRequest.paramsList.add(passwordParam);

        // 3. 创建 PostmanApiContext
        PostmanApiContext pm = new PostmanApiContext(env);
        pm.request = new ScriptRequestAccessor(preparedRequest);

        // 4. 执行 JavaScript 脚本
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .option("js.ecmascript-version", "2021")
                .out(System.out)
                .err(System.err)
                .build()) {

            // 注入 CryptoJS 和 Polyfill
            JsLibraryLoader.injectBuiltinLibraries(context);
            JsPolyfillInjector.injectAll(context);

            // 注入 pm 对象
            context.getBindings("js").putMember("pm", pm);

            // 执行完整的加密脚本
            String script = """
                    // 从环境变量获取配置
                    var secret_key = pm.environment.get("secret_key");
                    var app_code = pm.environment.get("app_code");
                    var app_secret = pm.environment.get("app_secret");
                    
                    // 生成 Basic 认证头
                    var authz_header = "Basic " + btoa(app_code + ":" + app_secret);
                    
                    // 获取密码参数（使用 Postman 标准 API）
                    var password = pm.request.url.query.all()[1].value;
                    
                    // 解析密钥
                    var key = CryptoJS.enc.Latin1.parse(secret_key);
                    var iv = key;
                    
                    // AES 加密
                    var encrypted = CryptoJS.AES.encrypt(
                        password,
                        key, {
                            iv: iv,
                            mode: CryptoJS.mode.CBC,
                            padding: CryptoJS.pad.ZeroPadding
                        }
                    );
                    
                    // URL 编码
                    var encrypted_password = encodeURIComponent(encrypted.toString());
                    
                    // 输出日志
                    console.log("Original Password: " + password);
                    console.log("Encrypted Password: " + encrypted_password);
                    console.log("Authorization Header: " + authz_header);
                    
                    // 修改请求参数（使用 Postman 标准 API）
                    pm.request.url.query.all()[1].value = encrypted_password;
                    
                    // 添加认证头
                    pm.request.headers.add({
                        key: 'Authorization',
                        value: authz_header
                    });
                    
                    // 返回加密后的密码用于验证
                    encrypted_password;
                    """;

            Value result = context.eval("js", script);
            String encryptedPassword = result.asString();

            // 同步 JavaScript 对字段的修改回底层对象
            pm.request.url.query.sync();

            // 5. 验证结果
            assertNotNull(encryptedPassword, "加密后的密码不应为空");
            assertFalse(encryptedPassword.isEmpty(), "加密后的密码不应为空字符串");
            assertTrue(encryptedPassword.contains("%"), "URL 编码后应包含 %");
            assertNotEquals(encryptedPassword, "mypassword123", "密码应该已被加密");

            // 验证请求参数已被修改
            assertEquals(preparedRequest.paramsList.get(1).getValue(), encryptedPassword,
                    "请求中的密码参数应该已被更新为加密后的值");

            // 验证 Authorization 头已添加
            assertEquals(preparedRequest.headersList.size(), 1, "应该添加了一个请求头");
            HttpHeader authHeader = preparedRequest.headersList.get(0);
            assertEquals(authHeader.getKey(), "Authorization", "请求头名称应为 Authorization");
            assertTrue(authHeader.getValue().startsWith("Basic "), "应该是 Basic 认证");

            // 验证 Basic 认证头的内容
            String expectedAuth = "Basic bXlBcHBDb2RlOm15QXBwU2VjcmV0"; // Base64("myAppCode:myAppSecret")
            assertEquals(authHeader.getValue(), expectedAuth, "Basic 认证头应该正确编码");

            System.out.println("✅ 完整加密脚本测试通过!");
            System.out.println("   原始密码: mypassword123");
            System.out.println("   加密后: " + encryptedPassword);
            System.out.println("   认证头: " + authHeader.getValue());
        }
    }

    @Test(description = "测试 CryptoJS AES 加密的可逆性")
    public void testCryptoJSEncryptionDecryption() {
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .out(System.out)
                .err(System.err)
                .build()) {

            JsLibraryLoader.injectBuiltinLibraries(context);
            JsPolyfillInjector.injectAll(context);

            String script = """
                    var secret_key = "1234567890123456";
                    var password = "mypassword123";
                    
                    // 加密
                    var key = CryptoJS.enc.Latin1.parse(secret_key);
                    var iv = key;
                    
                    var encrypted = CryptoJS.AES.encrypt(
                        password,
                        key, {
                            iv: iv,
                            mode: CryptoJS.mode.CBC,
                            padding: CryptoJS.pad.ZeroPadding
                        }
                    );
                    
                    var encryptedStr = encrypted.toString();
                    console.log("Encrypted: " + encryptedStr);
                    
                    // 解密
                    var decrypted = CryptoJS.AES.decrypt(
                        encryptedStr,
                        key, {
                            iv: iv,
                            mode: CryptoJS.mode.CBC,
                            padding: CryptoJS.pad.ZeroPadding
                        }
                    );
                    
                    var decryptedStr = decrypted.toString(CryptoJS.enc.Utf8).replace(/\\0+$/, '');
                    console.log("Decrypted: " + decryptedStr);
                    
                    decryptedStr;
                    """;

            Value result = context.eval("js", script);
            String decrypted = result.asString();

            assertEquals(decrypted, "mypassword123", "解密后应该恢复原始密码");
            System.out.println("✅ 加密解密测试通过!");
        }
    }

    @Test(description = "测试不同密钥长度的 AES 加密")
    public void testDifferentKeyLengths() {
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .out(System.out)
                .err(System.err)
                .build()) {

            JsLibraryLoader.injectBuiltinLibraries(context);
            JsPolyfillInjector.injectAll(context);

            // 测试 128-bit (16字节)
            String script128 = """
                    var key = CryptoJS.enc.Latin1.parse("1234567890123456");
                    var encrypted = CryptoJS.AES.encrypt("test", key, {iv: key, mode: CryptoJS.mode.CBC});
                    encrypted.toString();
                    """;
            Value result128 = context.eval("js", script128);
            assertNotNull(result128.asString(), "128-bit 密钥加密应该成功");

            // 测试 192-bit (24字节)
            String script192 = """
                    var key = CryptoJS.enc.Latin1.parse("123456789012345678901234");
                    var encrypted = CryptoJS.AES.encrypt("test", key, {iv: key, mode: CryptoJS.mode.CBC});
                    encrypted.toString();
                    """;
            Value result192 = context.eval("js", script192);
            assertNotNull(result192.asString(), "192-bit 密钥加密应该成功");

            // 测试 256-bit (32字节)
            String script256 = """
                    var key = CryptoJS.enc.Latin1.parse("12345678901234567890123456789012");
                    var encrypted = CryptoJS.AES.encrypt("test", key, {iv: key, mode: CryptoJS.mode.CBC});
                    encrypted.toString();
                    """;
            Value result256 = context.eval("js", script256);
            assertNotNull(result256.asString(), "256-bit 密钥加密应该成功");

            System.out.println("✅ 不同密钥长度测试通过!");
        }
    }

    @Test(description = "测试 btoa 和 atob 功能")
    public void testBtoaAndAtob() {
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .build()) {

            JsPolyfillInjector.injectAll(context);

            // 测试 btoa
            String btoaScript = "btoa('myAppCode:myAppSecret')";
            Value encoded = context.eval("js", btoaScript);
            assertEquals(encoded.asString(), "bXlBcHBDb2RlOm15QXBwU2VjcmV0", "btoa 编码应该正确");

            // 测试 atob
            String atobScript = "atob('bXlBcHBDb2RlOm15QXBwU2VjcmV0')";
            Value decoded = context.eval("js", atobScript);
            assertEquals(decoded.asString(), "myAppCode:myAppSecret", "atob 解码应该正确");

            System.out.println("✅ btoa/atob 测试通过!");
        }
    }

    @Test(description = "测试 encodeURIComponent 和 decodeURIComponent")
    public void testEncodeDecodeURIComponent() {
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .build()) {

            JsPolyfillInjector.injectAll(context);

            String testString = "Hello World! 你好世界 #special@chars&=+";

            String encodeScript = String.format("encodeURIComponent('%s')",
                    testString.replace("'", "\\'"));
            Value encoded = context.eval("js", encodeScript);
            String encodedStr = encoded.asString();

            assertTrue(encodedStr.contains("%"), "编码后应包含 %");
            assertFalse(encodedStr.contains(" "), "空格应该被编码");
            assertFalse(encodedStr.contains("#"), "# 应该被编码");

            // 测试解码
            String decodeScript = String.format("decodeURIComponent('%s')", encodedStr);
            Value decoded = context.eval("js", decodeScript);
            assertEquals(decoded.asString(), testString, "解码后应恢复原始字符串");

            System.out.println("✅ encodeURIComponent/decodeURIComponent 测试通过!");
        }
    }

    @Test(description = "测试环境变量获取和设置")
    public void testEnvironmentVariables() {
        Environment env = new Environment();
        env.setName("Test Env");
        env.set("key1", "value1");
        env.set("key2", "value2");

        PostmanApiContext pm = new PostmanApiContext(env);

        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .build()) {

            context.getBindings("js").putMember("pm", pm);

            // 测试获取环境变量
            String getScript = "pm.environment.get('key1')";
            Value value1 = context.eval("js", getScript);
            assertEquals(value1.asString(), "value1", "应该能获取环境变量");

            // 测试设置环境变量
            context.eval("js", "pm.environment.set('key3', 'value3')");
            assertEquals(env.get("key3"), "value3", "应该能设置环境变量");

            System.out.println("✅ 环境变量测试通过!");
        }
    }

    @Test(description = "测试请求参数的访问和修改")
    public void testRequestParamsAccess() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.url = "https://example.com?param1=value1&param2=value2";
        preparedRequest.paramsList = new ArrayList<>();

        HttpParam param1 = new HttpParam();
        param1.setKey("param1");
        param1.setValue("value1");
        param1.setEnabled(true);
        preparedRequest.paramsList.add(param1);

        HttpParam param2 = new HttpParam();
        param2.setKey("param2");
        param2.setValue("value2");
        param2.setEnabled(true);
        preparedRequest.paramsList.add(param2);

        Environment env = new Environment();
        PostmanApiContext pm = new PostmanApiContext(env);
        pm.request = new ScriptRequestAccessor(preparedRequest);

        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .build()) {

            context.getBindings("js").putMember("pm", pm);

            // 测试访问参数（使用 Postman 标准 API）
            String accessScript = "pm.request.url.query.all()[0].value";
            Value value = context.eval("js", accessScript);
            assertEquals(value.asString(), "value1", "应该能访问第一个参数");

            // 测试修改参数（使用 Postman 标准 API）
            context.eval("js", "pm.request.url.query.all()[1].value = 'newvalue2'");

            // 同步修改
            pm.request.url.query.sync();

            assertEquals(preparedRequest.paramsList.get(1).getValue(), "newvalue2",
                    "参数值应该被修改");

            // 测试获取参数数量
            String countScript = "pm.request.url.query.all().length";
            Value count = context.eval("js", countScript);
            assertEquals(count.asInt(), 2, "应该有2个参数");

            System.out.println("✅ 请求参数访问测试通过!");
        }
    }

    @Test(description = "测试请求头的添加")
    public void testRequestHeadersAdd() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.headersList = new ArrayList<>();

        Environment env = new Environment();
        PostmanApiContext pm = new PostmanApiContext(env);
        pm.request = new ScriptRequestAccessor(preparedRequest);

        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .build()) {

            context.getBindings("js").putMember("pm", pm);

            // 添加请求头
            context.eval("js", """
                    pm.request.headers.add({
                        key: 'Authorization',
                        value: 'Bearer token123'
                    })
                    """);

            assertEquals(preparedRequest.headersList.size(), 1, "应该添加了一个请求头");
            assertEquals(preparedRequest.headersList.get(0).getKey(), "Authorization");
            assertEquals(preparedRequest.headersList.get(0).getValue(), "Bearer token123");

            // 添加多个请求头
            context.eval("js", """
                    pm.request.headers.add({
                        key: 'Content-Type',
                        value: 'application/json'
                    })
                    """);

            assertEquals(preparedRequest.headersList.size(), 2, "应该有2个请求头");

            System.out.println("✅ 请求头添加测试通过!");
        }
    }

    @Test(description = "测试用户提供的原始 Postman 脚本 - 完全使用 .value 属性")
    public void testUserOriginalPostmanScript() {
        // 模拟真实的 Postman 场景
        Environment env = new Environment();
        env.setName("Production");
        env.set("secret_key", "1234567890123456");
        env.set("app_code", "myAppCode");
        env.set("app_secret", "myAppSecret");

        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.url = "https://api.example.com/login?username=admin&password=mypassword123";
        preparedRequest.method = "POST";
        preparedRequest.headersList = new ArrayList<>();
        preparedRequest.paramsList = new ArrayList<>();

        // 添加查询参数
        HttpParam usernameParam = new HttpParam();
        usernameParam.setKey("username");
        usernameParam.setValue("admin");
        usernameParam.setEnabled(true);
        preparedRequest.paramsList.add(usernameParam);

        HttpParam passwordParam = new HttpParam();
        passwordParam.setKey("password");
        passwordParam.setValue("mypassword123");
        passwordParam.setEnabled(true);
        preparedRequest.paramsList.add(passwordParam);

        PostmanApiContext pm = new PostmanApiContext(env);
        pm.request = new ScriptRequestAccessor(preparedRequest);

        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .option("js.ecmascript-version", "2021")
                .out(System.out)
                .err(System.err)
                .build()) {

            JsLibraryLoader.injectBuiltinLibraries(context);
            JsPolyfillInjector.injectAll(context);
            context.getBindings("js").putMember("pm", pm);

            // 用户原始的 Postman 脚本（完全不修改，使用 .value 属性）
            String userScript = """
                    var secret_key = pm.environment.get("secret_key");
                    var app_code = pm.environment.get("app_code");
                    var app_secret = pm.environment.get("app_secret");
                    
                    var authz_header = "Basic " + btoa(app_code + ":" + app_secret);
                    var password = pm.request.url.query.all()[1].value;
                    
                    var key = CryptoJS.enc.Latin1.parse(secret_key);
                    var iv = key;
                    
                    // 加密
                    var encrypted = CryptoJS.AES.encrypt(
                      password,
                      key, {
                        iv: iv,
                        mode: CryptoJS.mode.CBC,
                        padding: CryptoJS.pad.ZeroPadding
                      }
                    );
                    var encrypted_password = encodeURIComponent(encrypted.toString());
                    
                    console.log(encrypted_password);
                    pm.request.url.query.all()[1].value = encrypted_password;
                    pm.request.headers.add(
                        {key: 'Authorization', value: authz_header }
                    );
                    
                    encrypted_password;
                    """;

            Value result = context.eval("js", userScript);
            String encryptedPassword = result.asString();

            // 同步 JavaScript 对字段的修改
            pm.request.url.query.sync();

            // 验证结果
            assertNotNull(encryptedPassword, "加密后的密码不应为空");
            assertTrue(encryptedPassword.contains("%"), "URL 编码后应包含 %");
            assertNotEquals(encryptedPassword, "mypassword123", "密码应该已被加密");

            // 验证密码参数已被修改
            assertEquals(preparedRequest.paramsList.get(1).getValue(), encryptedPassword,
                    "请求中的密码参数应该已被更新为加密后的值");

            // 验证认证头
            assertEquals(preparedRequest.headersList.size(), 1, "应该添加了一个请求头");
            HttpHeader authHeader = preparedRequest.headersList.get(0);
            assertEquals(authHeader.getKey(), "Authorization");
            assertTrue(authHeader.getValue().startsWith("Basic "));

            System.out.println("✅ 用户原始 Postman 脚本测试通过!");
            System.out.println("   加密后的密码: " + encryptedPassword);
            System.out.println("   认证头: " + authHeader.getValue());
        }
    }
}

