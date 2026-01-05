package com.laker.postman.service.js;

import org.graalvm.polyglot.Context;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * CryptoJS 功能测试 - 使用 TestNG
 */
public class CryptoJSTest {

    @Test(description = "使用 CryptoJS（自动加载）")
    public void testDirectUsage() {
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .build()) {

            // 注入库
            JsLibraryLoader.injectBuiltinLibraries(context);

            // 测试 AES 加密
            String script = """
                    var encrypted = CryptoJS.AES.encrypt('Hello World', 'secret key 123');
                    encrypted.toString();
                    """;

            String result = context.eval("js", script).asString();
            assertNotNull(result, "加密结果不应为空");
            assertFalse(result.isEmpty(), "加密结果不应为空字符串");
            System.out.println("✅ 加密成功: " + result);
        }
    }

    @Test(description = "原始脚本（完整功能）")
    public void testOriginalScript() {
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .out(System.out)
                .err(System.err)
                .build()) {

            // 注入库和 polyfill
            JsLibraryLoader.injectBuiltinLibraries(context);
            JsPolyfillInjector.injectAll(context);

            // 您的原始脚本（简化版）
            String script = """
                    var secret_key = "1234567890123456";
                    var app_code = "myapp";
                    var app_secret = "mysecret";
                    var password = "mypassword";
                    
                    // 测试 btoa
                    var authz_header = "Basic " + btoa(app_code + ":" + app_secret);
                    console.log("Authorization: " + authz_header);
                    
                    // 测试 CryptoJS
                    var key = CryptoJS.enc.Latin1.parse(secret_key);
                    var iv = key;
                    
                    var encrypted = CryptoJS.AES.encrypt(
                        password,
                        key,
                        {
                            iv: iv,
                            mode: CryptoJS.mode.CBC,
                            padding: CryptoJS.pad.ZeroPadding
                        }
                    );
                    
                    console.log("Encrypted: " + encrypted.toString());
                    
                    // 测试 encodeURIComponent
                    var encrypted_password = encodeURIComponent(encrypted.toString());
                    console.log("Encrypted & Encoded: " + encrypted_password);
                    
                    encrypted_password;
                    """;

            String result = context.eval("js", script).asString();
            assertNotNull(result, "最终结果不应为空");
            assertFalse(result.isEmpty(), "最终结果不应为空字符串");
            assertTrue(result.contains("%"), "URL 编码后应包含 %");
            System.out.println("✅ 最终结果: " + result);
        }
    }
}

