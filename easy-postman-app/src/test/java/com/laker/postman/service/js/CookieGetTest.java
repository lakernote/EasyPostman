package com.laker.postman.service.js;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.script.Cookie;
import com.laker.postman.model.script.PostmanApiContext;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.*;


/**
 * 测试 pm.cookies.get() 能够正确获取响应中的 Cookie
 */
public class CookieGetTest {

    private Context context;
    private PostmanApiContext pm;

    @BeforeMethod
    public void setUp() {
        context = Context.newBuilder("js")
                .allowAllAccess(true)
                .build();
        pm = new PostmanApiContext(null);
    }

    @AfterMethod
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void testGetResponseCookieWorks() {
        // 创建包含 Set-Cookie 头的响应
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new HashMap<>();
        response.headers.put("Set-Cookie", Arrays.asList(
                "JSESSIONID=ABC123; Path=/; HttpOnly",
                "token=XYZ789; Domain=example.com; Secure"
        ));

        // 设置响应
        pm.setResponse(response);

        // 测试 pm.getResponseCookie() - 应该可以工作
        Cookie jsessionId = pm.getResponseCookie("JSESSIONID");
        assertNotNull(jsessionId, "pm.getResponseCookie('JSESSIONID') should return a Cookie");
        assertEquals("JSESSIONID", jsessionId.name);
        assertEquals("ABC123", jsessionId.value);
        assertEquals("/", jsessionId.path);
        assertTrue(jsessionId.httpOnly);

        Cookie token = pm.getResponseCookie("token");
        assertNotNull(token, "pm.getResponseCookie('token') should return a Cookie");
        assertEquals("token", token.name);
        assertEquals("XYZ789", token.value);
        assertEquals("example.com", token.domain);
        assertTrue(token.secure);
    }

    @Test
    public void testCookiesGetNowWorks() {
        // 创建包含 Set-Cookie 头的响应
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new HashMap<>();
        response.headers.put("Set-Cookie", Arrays.asList(
                "JSESSIONID=ABC123; Path=/; HttpOnly",
                "token=XYZ789; Domain=example.com; Secure"
        ));

        // 设置响应（会自动填充 cookies）
        pm.setResponse(response);

        // 测试 pm.cookies.get() - 现在应该可以工作了！
        Cookie jsessionId = pm.cookies.get("JSESSIONID");
        assertNotNull(jsessionId, "pm.cookies.get('JSESSIONID') should now return a Cookie after fix");
        assertEquals("JSESSIONID", jsessionId.name);
        assertEquals("ABC123", jsessionId.value);

        Cookie token = pm.cookies.get("token");
        assertNotNull(token, "pm.cookies.get('token') should now return a Cookie after fix");
        assertEquals("token", token.name);
        assertEquals("XYZ789", token.value);
    }

    @Test
    public void testCookiesAllWorks() {
        // 创建包含多个 Cookie 的响应
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new HashMap<>();
        response.headers.put("Set-Cookie", Arrays.asList(
                "JSESSIONID=ABC123; Path=/",
                "token=XYZ789; Domain=example.com",
                "user_id=12345; Secure"
        ));

        pm.setResponse(response);

        // 测试 pm.cookies.all()
        Cookie[] allCookies = pm.cookies.all();
        assertNotNull(allCookies);
        assertEquals(3, allCookies.length, "Should have 3 cookies");

        // 验证所有 Cookie 都被填充了
        Set<String> cookieNames = new HashSet<>();
        for (Cookie cookie : allCookies) {
            cookieNames.add(cookie.name);
        }
        assertTrue(cookieNames.contains("JSESSIONID"));
        assertTrue(cookieNames.contains("token"));
        assertTrue(cookieNames.contains("user_id"));
    }

    @Test
    public void testCookiesHasWorks() {
        // 创建响应
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new HashMap<>();
        response.headers.put("Set-Cookie", Collections.singletonList("JSESSIONID=ABC123"));

        pm.setResponse(response);

        // 测试 pm.cookies.has()
        assertTrue(pm.cookies.has("JSESSIONID"), "pm.cookies.has('JSESSIONID') should return true");
        assertFalse(pm.cookies.has("nonexistent"), "pm.cookies.has('nonexistent') should return false");
    }

    @Test
    public void testJavaScriptCookiesGet() {
        // 创建响应
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new HashMap<>();
        response.headers.put("Set-Cookie", Arrays.asList(
                "JSESSIONID=ABC123; Path=/; HttpOnly",
                "token=XYZ789"
        ));

        pm.setResponse(response);

        // 在 JavaScript 中测试
        context.getBindings("js").putMember("pm", pm);

        // 测试 pm.cookies.get()
        Value result = context.eval("js",
                "var cookie = pm.cookies.get('JSESSIONID');" +
                        "cookie ? cookie.value : null"
        );
        assertNotNull(result);
        assertFalse(result.isNull(), "pm.cookies.get('JSESSIONID') should return a valid cookie");
        assertEquals("ABC123", result.asString());

        // 测试 pm.cookies.all()
        Value allResult = context.eval("js",
                "var allCookies = pm.cookies.all();" +
                        "allCookies.length"
        );
        assertEquals(2, allResult.asInt(), "Should have 2 cookies");

        // 测试 pm.cookies.has()
        Value hasResult = context.eval("js", "pm.cookies.has('JSESSIONID')");
        assertTrue(hasResult.asBoolean());
    }

    @Test
    public void testScriptComparison() {
        // 模拟用户报告的场景
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new HashMap<>();
        response.headers.put("Set-Cookie", Collections.singletonList("JSESSIONID=SESSION123"));

        pm.setResponse(response);
        context.getBindings("js").putMember("pm", pm);

        // 方法1: pm.getResponseCookie() - 一直可以工作
        Value method1 = context.eval("js",
                "var jsessionId = pm.getResponseCookie('JSESSIONID');" +
                        "jsessionId ? jsessionId.value : null"
        );
        assertNotNull(method1);
        assertEquals("SESSION123", method1.asString(), "pm.getResponseCookie() should work");

        // 方法2: pm.cookies.get() - 现在也应该可以工作了
        Value method2 = context.eval("js",
                "var cookie = pm.cookies.get('JSESSIONID');" +
                        "cookie ? cookie.value : null"
        );
        assertNotNull(method2);
        assertEquals("SESSION123", method2.asString(), "pm.cookies.get() should now work after fix");

        // 两种方法应该返回相同的值
        assertEquals(method1.asString(), method2.asString(),
                "Both methods should return the same cookie value");
    }
}

