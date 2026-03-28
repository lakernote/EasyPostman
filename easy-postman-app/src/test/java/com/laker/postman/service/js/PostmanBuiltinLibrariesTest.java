package com.laker.postman.service.js;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.SystemUtil;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.*;

/**
 * Postman 内置库完整测试套件
 * <p>
 * 测试所有内置 JavaScript 库的功能，确保与 Postman 兼容
 * 包括: CryptoJS, Lodash, Moment, atob/btoa
 * </p>
 *
 * @author laker
 */
@Slf4j
public class PostmanBuiltinLibrariesTest {

    private Context context;
    private HttpServer httpServer;
    private boolean originalRemoteRequireEnabled;
    private boolean originalAllowHttpRemoteRequire;
    private String originalAllowedRemoteHosts;
    private int originalRemoteConnectTimeoutMs;
    private int originalRemoteReadTimeoutMs;
    private int originalRemoteMaxBytes;
    private String originalEasyPostmanDataDir;

    @BeforeClass
    public void rememberSettings() throws IOException {
        Path tempDataDir = Files.createTempDirectory("easy-postman-js-settings-");
        originalEasyPostmanDataDir = System.getProperty("easyPostman.data.dir");
        System.setProperty("easyPostman.data.dir", tempDataDir.toString());
        SystemUtil.resetForTests();

        originalRemoteRequireEnabled = SettingManager.isRemoteJsRequireEnabled();
        originalAllowHttpRemoteRequire = SettingManager.isInsecureRemoteJsRequireEnabled();
        originalAllowedRemoteHosts = SettingManager.getRemoteJsRequireAllowedHosts();
        originalRemoteConnectTimeoutMs = SettingManager.getRemoteJsRequireConnectTimeoutMs();
        originalRemoteReadTimeoutMs = SettingManager.getRemoteJsRequireReadTimeoutMs();
        originalRemoteMaxBytes = SettingManager.getRemoteJsRequireMaxBytes();
    }

    @AfterClass
    public void restoreSettings() {
        restoreScriptSettings();
        if (originalEasyPostmanDataDir == null) {
            System.clearProperty("easyPostman.data.dir");
        } else {
            System.setProperty("easyPostman.data.dir", originalEasyPostmanDataDir);
        }
    }

    @BeforeMethod
    public void setUp() {
        log.info("初始化 JavaScript 执行环境...");
        resetScriptSettingsToDefaults();
        context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> false)
                .option("engine.WarnInterpreterOnly", "false")
                .build();

        // 注入所有 polyfill（包括 Java 实现的 atob, btoa 等函数）
        JsPolyfillInjector.injectAll(context);

        log.info("所有内置库加载完成");
    }

    @AfterMethod
    public void tearDown() {
        stopHttpServer();
        if (context != null) {
            context.close();
            log.info("JavaScript 执行环境已关闭");
        }

        JsLibraryLoader.clearCache();
        restoreScriptSettings();
    }

    // ==================== CryptoJS 测试 ====================

    @Test(description = "测试 CryptoJS - MD5 哈希")
    public void testCryptoJS_MD5() {
        String script = "CryptoJS.MD5('Hello World').toString();";
        Value result = context.eval("js", script);

        String hash = result.asString();
        assertEquals(hash, "b10a8db164e0754105b7a99be72e3fe5", "MD5 哈希值不正确");
        log.info("✓ CryptoJS MD5 测试通过: {}", hash);
    }

    @Test(description = "测试 CryptoJS - SHA256 哈希")
    public void testCryptoJS_SHA256() {
        String script = "CryptoJS.SHA256('Hello World').toString();";
        Value result = context.eval("js", script);

        String hash = result.asString();
        assertEquals(hash, "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e");
        log.info("✓ CryptoJS SHA256 测试通过");
    }

    @Test(description = "测试 CryptoJS - AES 加密解密")
    public void testCryptoJS_AES() {
        String script = """
                var message = 'Secret Message';
                var key = 'my-secret-key';
                var encrypted = CryptoJS.AES.encrypt(message, key).toString();
                var decrypted = CryptoJS.AES.decrypt(encrypted, key).toString(CryptoJS.enc.Utf8);
                JSON.stringify({ encrypted: encrypted, decrypted: decrypted, match: message === decrypted });
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        assertTrue(json.contains("\"match\":true"), "AES 加密解密失败");
        assertTrue(json.contains("\"decrypted\":\"Secret Message\""), "解密后的消息不匹配");
        log.info("✓ CryptoJS AES 加密解密测试通过");
    }

    @Test(description = "测试 CryptoJS - HMAC-SHA256 签名")
    public void testCryptoJS_HMAC() {
        String script = """
                var message = 'POST/api/users{"userId":123}';
                var secret = 'api-secret-key';
                var signature = CryptoJS.HmacSHA256(message, secret).toString();
                signature;
                """;

        Value result = context.eval("js", script);
        String signature = result.asString();

        assertNotNull(signature);
        assertEquals(signature.length(), 64, "HMAC-SHA256 签名长度应为 64");
        log.info("✓ CryptoJS HMAC-SHA256 测试通过: {}", signature);
    }

    @Test(description = "测试 CryptoJS - Base64 编码")
    public void testCryptoJS_Base64() {
        String script = """
                var text = 'Hello, World!';
                var encoded = CryptoJS.enc.Base64.stringify(CryptoJS.enc.Utf8.parse(text));
                var decoded = CryptoJS.enc.Base64.parse(encoded).toString(CryptoJS.enc.Utf8);
                JSON.stringify({ encoded: encoded, decoded: decoded, match: text === decoded });
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        assertTrue(json.contains("\"match\":true"));
        assertTrue(json.contains("SGVsbG8sIFdvcmxkIQ=="));
        log.info("✓ CryptoJS Base64 测试通过");
    }

    // ==================== Lodash 测试 ====================

    @Test(description = "测试 Lodash - 数组去重")
    public void testLodash_Uniq() {
        String script = """
                var arr = [1, 2, 2, 3, 3, 3, 4, 5];
                var unique = _.uniq(arr);
                JSON.stringify(unique);
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "[1,2,3,4,5]");
        log.info("✓ Lodash uniq 测试通过");
    }

    @Test(description = "测试 Lodash - 数组过滤")
    public void testLodash_Filter() {
        String script = """
                var numbers = [1, 2, 3, 4, 5, 6];
                var evens = _.filter(numbers, n => n % 2 === 0);
                JSON.stringify(evens);
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "[2,4,6]");
        log.info("✓ Lodash filter 测试通过");
    }

    @Test(description = "测试 Lodash - 数组映射")
    public void testLodash_Map() {
        String script = """
                var numbers = [1, 2, 3];
                var doubled = _.map(numbers, n => n * 2);
                JSON.stringify(doubled);
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "[2,4,6]");
        log.info("✓ Lodash map 测试通过");
    }

    @Test(description = "测试 Lodash - 对象操作")
    public void testLodash_Pick() {
        String script = """
                var obj = { name: 'John', age: 30, city: 'NY', country: 'USA' };
                var picked = _.pick(obj, ['name', 'age']);
                JSON.stringify(picked);
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        assertTrue(json.contains("\"name\":\"John\""));
        assertTrue(json.contains("\"age\":30"));
        assertFalse(json.contains("city"));
        log.info("✓ Lodash pick 测试通过");
    }

    @Test(description = "测试 Lodash - 数组分组")
    public void testLodash_GroupBy() {
        String script = """
                var numbers = [6.1, 4.2, 6.3, 4.5];
                var grouped = _.groupBy(numbers, Math.floor);
                JSON.stringify(grouped);
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        assertTrue(json.contains("\"4\""));
        assertTrue(json.contains("\"6\""));
        log.info("✓ Lodash groupBy 测试通过");
    }

    // ==================== Moment 测试 ====================

    @Test(description = "测试 Moment - 日期格式化")
    public void testMoment_Format() {
        String script = """
                var date = moment('2024-01-15 10:30:45');
                var formatted = date.format('YYYY-MM-DD HH:mm:ss');
                formatted;
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "2024-01-15 10:30:45");
        log.info("✓ Moment 格式化测试通过");
    }

    @Test(description = "测试 Moment - 日期加减")
    public void testMoment_Add() {
        String script = """
                var date = moment('2024-01-15');
                var nextWeek = date.add(7, 'days').format('YYYY-MM-DD');
                nextWeek;
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "2024-01-22");
        log.info("✓ Moment 日期加减测试通过");
    }

    @Test(description = "测试 Moment - 日期减法")
    public void testMoment_Subtract() {
        String script = """
                var date = moment('2024-01-15');
                var lastMonth = date.subtract(1, 'months').format('YYYY-MM-DD');
                lastMonth;
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "2023-12-15");
        log.info("✓ Moment 日期减法测试通过");
    }

    @Test(description = "测试 Moment - 日期比较")
    public void testMoment_Compare() {
        String script = """
                var date1 = moment('2024-01-01');
                var date2 = moment('2024-12-31');
                var isBefore = date1.isBefore(date2);
                var isAfter = date1.isAfter(date2);
                JSON.stringify({ isBefore: isBefore, isAfter: isAfter });
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        assertTrue(json.contains("\"isBefore\":true"));
        assertTrue(json.contains("\"isAfter\":false"));
        log.info("✓ Moment 日期比较测试通过");
    }

    @Test(description = "测试 Moment - 日期解析")
    public void testMoment_Parse() {
        String script = """
                var date = moment('15/01/2024', 'DD/MM/YYYY');
                var formatted = date.format('YYYY-MM-DD');
                formatted;
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "2024-01-15");
        log.info("✓ Moment 日期解析测试通过");
    }


    // ==================== atob/btoa 测试（Java 实现）====================

    @Test(description = "测试 btoa - Base64 编码（Java 实现）")
    public void testBtoa_Encode() {
        String script = "btoa('Hello, World!');";
        Value result = context.eval("js", script);

        assertEquals(result.asString(), "SGVsbG8sIFdvcmxkIQ==");
        log.info("✓ btoa 编码测试通过");
    }

    @Test(description = "测试 atob - Base64 解码")
    public void testAtob_Decode() {
        String script = "atob('SGVsbG8sIFdvcmxkIQ==');";
        Value result = context.eval("js", script);

        assertEquals(result.asString(), "Hello, World!");
        log.info("✓ atob 解码测试通过");
    }

    @Test(description = "测试 atob/btoa - 往返编解码")
    public void testAtobBtoa_RoundTrip() {
        String script = """
                var original = 'The quick brown fox jumps over the lazy dog';
                var encoded = btoa(original);
                var decoded = atob(encoded);
                JSON.stringify({ match: original === decoded, original: original, decoded: decoded });
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        assertTrue(json.contains("\"match\":true"));
        log.info("✓ atob/btoa 往返测试通过");
    }

    // ==================== require() 测试 ====================

    @Test(description = "测试 require() - 加载 CryptoJS")
    public void testRequire_CryptoJS() {
        String script = """
                var CryptoJS = require('crypto-js');
                var hash = CryptoJS.MD5('test').toString();
                hash;
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "098f6bcd4621d373cade4e832627b4f6");
        log.info("✓ require('crypto-js') 测试通过");
    }

    @Test(description = "测试 require() - 加载 Lodash")
    public void testRequire_Lodash() {
        String script = """
                var _ = require('lodash');
                var arr = _.uniq([1, 2, 2, 3]);
                JSON.stringify(arr);
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "[1,2,3]");
        log.info("✓ require('lodash') 测试通过");
    }

    @Test(description = "测试 require() - 加载 Moment")
    public void testRequire_Moment() {
        String script = """
                var moment = require('moment');
                var date = moment('2024-01-15').format('YYYY-MM-DD');
                date;
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "2024-01-15");
        log.info("✓ require('moment') 测试通过");
    }

    @Test(description = "测试 require() - 加载本地绝对路径脚本")
    public void testRequire_LocalFile() throws IOException {
        Path scriptFile = Files.createTempFile("easy-postman-local-lib-", ".js");
        Files.writeString(scriptFile, """
                module.exports = {
                    version: '1.0.0',
                    sign: function(input) {
                        return input + '-signed';
                    }
                };
                """, StandardCharsets.UTF_8);

        Value result = context.eval("js", "require('" + toJsString(scriptFile.toAbsolutePath().normalize().toString()) + "').sign('token');");
        assertEquals(result.asString(), "token-signed");
    }

    @Test(description = "测试 require() - 本地脚本内部支持相对路径依赖")
    public void testRequire_LocalFileRelativeDependency() throws IOException {
        Path tempDir = Files.createTempDirectory("easy-postman-local-module-");
        Path childModule = tempDir.resolve("child.js");
        Path parentModule = tempDir.resolve("parent.js");

        Files.writeString(childModule, "module.exports = { value: 'child-module' };", StandardCharsets.UTF_8);
        Files.writeString(parentModule, """
                var child = require('./child.js');
                module.exports = { value: 'parent:' + child.value };
                """, StandardCharsets.UTF_8);

        Value result = context.eval("js", "require('" + toJsString(parentModule.toAbsolutePath().normalize().toString()) + "').value;");
        assertEquals(result.asString(), "parent:child-module");
    }

    @Test(description = "测试 require() - 远程脚本默认禁用")
    public void testRequire_RemoteDisabledByDefault() {
        Value result = context.eval("js", """
                try {
                    require('https://example.com/remote-lib.js');
                    'no-error';
                } catch (e) {
                    String(e.message);
                }
                """);
        assertTrue(result.asString().contains("Remote JavaScript require() is disabled"));
    }

    @Test(description = "测试 require() - 远程脚本启用后可加载")
    public void testRequire_RemoteEnabledWithExplicitOptIn() throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/remote-lib.js", exchange -> {
            byte[] body = "module.exports = { value: 'remote-ok' };".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/javascript; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        httpServer.start();

        SettingManager.setRemoteJsRequireEnabled(true);
        SettingManager.setInsecureRemoteJsRequireEnabled(true);
        SettingManager.setRemoteJsRequireConnectTimeoutMs(1000);
        SettingManager.setRemoteJsRequireReadTimeoutMs(1000);
        SettingManager.setRemoteJsRequireMaxBytes(65536);

        int port = httpServer.getAddress().getPort();
        Value result = context.eval("js", "require('http://127.0.0.1:" + port + "/remote-lib.js').value;");
        assertEquals(result.asString(), "remote-ok");
    }

    @Test(description = "测试 Context 池复用时 require() 缓存会在每次执行后清空")
    public void testContextPoolClearsRequireCacheBetweenRuns() throws Exception {
        Path scriptFile = Files.createTempFile("easy-postman-cache-reset-", ".js");
        Files.writeString(scriptFile, "module.exports = { version: 'v1' };", StandardCharsets.UTF_8);

        JsContextPool pool = new JsContextPool(1);
        String normalizedPath = scriptFile.toAbsolutePath().normalize().toString();
        try {
            JsContextPool.PooledContext firstBorrow = pool.borrowContext(1000);
            Value firstResult = firstBorrow.getContext().eval("js", "require('" + toJsString(normalizedPath) + "').version;");
            assertEquals(firstResult.asString(), "v1");
            pool.returnContext(firstBorrow);

            Files.writeString(scriptFile, "module.exports = { version: 'v2' };", StandardCharsets.UTF_8);

            JsContextPool.PooledContext secondBorrow = pool.borrowContext(1000);
            Value secondResult = secondBorrow.getContext().eval("js", "require('" + toJsString(normalizedPath) + "').version;");
            assertEquals(secondResult.asString(), "v2");
            pool.returnContext(secondBorrow);
        } finally {
            pool.shutdown();
        }
    }

    // ==================== 综合场景测试 ====================

    @Test(description = "Postman 典型场景 - API 请求签名")
    public void testPostmanScenario_APISignature() {
        String script = """
                // 1. 生成请求 ID（使用随机数）
                var requestId = 'req_' + Math.random().toString(36).substring(2, 15);
                
                // 2. 获取时间戳
                var timestamp = moment().unix();
                
                // 3. 构造签名数据
                var signData = {
                    requestId: requestId,
                    timestamp: timestamp,
                    method: 'POST',
                    path: '/api/users'
                };
                
                // 4. 使用 Lodash 排序并拼接
                var signString = _.map(
                    _.sortBy(_.toPairs(signData), 0),
                    pair => pair[0] + '=' + pair[1]
                ).join('&');
                
                // 5. 生成 HMAC-SHA256 签名
                var signature = CryptoJS.HmacSHA256(signString, 'api-secret').toString();
                
                // 6. 返回结果
                JSON.stringify({
                    requestId: requestId,
                    timestamp: timestamp,
                    signature: signature,
                    hasValidRequestId: requestId.startsWith('req_'),
                    hasValidSignature: signature.length === 64
                });
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        assertTrue(json.contains("\"hasValidRequestId\":true"));
        assertTrue(json.contains("\"hasValidSignature\":true"));
        log.info("✓ API 签名场景测试通过");
    }

    @Test(description = "Postman 典型场景 - 响应验证")
    public void testPostmanScenario_ResponseValidation() {
        String script = """
                // 模拟 API 响应数据
                var response = {
                    status: 'success',
                    data: {
                        userId: 12345,
                        username: 'john_doe',
                        email: 'john@example.com',
                        createdAt: '2024-01-15T10:30:00Z',
                        roles: ['user', 'admin']
                    }
                };
                
                // 使用 Lodash 验证必需字段
                var requiredFields = ['userId', 'username', 'email'];
                var hasAllFields = _.every(requiredFields, field => _.has(response.data, field));
                
                // 使用 Moment 验证时间
                var createdDate = moment(response.data.createdAt);
                var isValidDate = createdDate.isValid();
                
                // 验证基本类型
                var isValidStatus = response.status === 'success';
                var isValidUserId = typeof response.data.userId === 'number';
                var isValidRoles = Array.isArray(response.data.roles) && response.data.roles.length === 2;
                
                JSON.stringify({
                    hasAllFields: hasAllFields,
                    isValidDate: isValidDate,
                    isValidStatus: isValidStatus,
                    isValidUserId: isValidUserId,
                    isValidRoles: isValidRoles,
                    allPassed: hasAllFields && isValidDate && isValidStatus && isValidUserId && isValidRoles
                });
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        assertTrue(json.contains("\"allPassed\":true"));
        log.info("✓ 响应验证场景测试通过");
    }

    @Test(description = "综合测试 - 所有库可用性检查")
    public void testAllLibrariesAvailable() {
        String script = """
                var libraries = {
                    // JavaScript 库
                    cryptojs: typeof CryptoJS !== 'undefined',
                    lodash: typeof _ !== 'undefined',
                    moment: typeof moment !== 'undefined',
                
                    // Java 实现的函数
                    atob: typeof atob === 'function',
                    btoa: typeof btoa === 'function',
                    MD5: typeof MD5 === 'function',
                    SHA256: typeof SHA256 === 'function',
                    timestamp: typeof timestamp === 'function',
                
                    // 其他
                    require: typeof require === 'function'
                };
                
                JSON.stringify(libraries);
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        // 验证所有库都已加载
        assertTrue(json.contains("\"cryptojs\":true"), "CryptoJS 未加载");
        assertTrue(json.contains("\"lodash\":true"), "Lodash 未加载");
        assertTrue(json.contains("\"moment\":true"), "Moment 未加载");
        assertTrue(json.contains("\"atob\":true"), "atob 未加载");
        assertTrue(json.contains("\"btoa\":true"), "btoa 未加载");
        assertTrue(json.contains("\"MD5\":true"), "MD5 未加载");
        assertTrue(json.contains("\"SHA256\":true"), "SHA256 未加载");
        assertTrue(json.contains("\"timestamp\":true"), "timestamp 未加载");
        assertTrue(json.contains("\"require\":true"), "require 未加载");

        log.info("✓ 所有库可用性检查通过: {}", json);
    }

    @Test(description = "性能测试 - 库加载时间")
    public void testLibraryLoadingPerformance() {
        long startTime = System.currentTimeMillis();

        Context testContext = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> false)
                .option("engine.WarnInterpreterOnly", "false")
                .build();

        JsLibraryLoader.injectBuiltinLibraries(testContext);
        JsLibraryLoader.injectRequireFunction(testContext);

        long loadTime = System.currentTimeMillis() - startTime;
        testContext.close();

        assertTrue(loadTime < 1000, "库加载时间过长: " + loadTime + "ms");
        log.info("✓ 库加载性能测试通过，耗时: {}ms", loadTime);
    }

    private void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
    }

    private void restoreScriptSettings() {
        SettingManager.setRemoteJsRequireEnabled(originalRemoteRequireEnabled);
        SettingManager.setInsecureRemoteJsRequireEnabled(originalAllowHttpRemoteRequire);
        SettingManager.setRemoteJsRequireAllowedHosts(originalAllowedRemoteHosts);
        SettingManager.setRemoteJsRequireConnectTimeoutMs(originalRemoteConnectTimeoutMs);
        SettingManager.setRemoteJsRequireReadTimeoutMs(originalRemoteReadTimeoutMs);
        SettingManager.setRemoteJsRequireMaxBytes(originalRemoteMaxBytes);
    }

    private void resetScriptSettingsToDefaults() {
        SettingManager.setRemoteJsRequireEnabled(false);
        SettingManager.setInsecureRemoteJsRequireEnabled(false);
        SettingManager.setRemoteJsRequireAllowedHosts("");
        SettingManager.setRemoteJsRequireConnectTimeoutMs(3000);
        SettingManager.setRemoteJsRequireReadTimeoutMs(5000);
        SettingManager.setRemoteJsRequireMaxBytes(512 * 1024);
    }

    private String toJsString(String raw) {
        return raw.replace("\\", "\\\\").replace("'", "\\'");
    }

}
