package com.laker.postman.service.js;

import com.laker.postman.model.Environment;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.script.Cookie;
import com.laker.postman.model.script.PostmanApiContext;
import org.graalvm.polyglot.Context;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Test for pm.getResponseCookie() functionality
 */
public class PostmanCookieTest {

    @Test
    public void testGetResponseCookie() {
        Environment env = new Environment();
        env.setName("Test");

        PostmanApiContext pm = new PostmanApiContext(env);

        // Set up a mock response with Set-Cookie header
        HttpResponse mockResponse = new HttpResponse();
        mockResponse.code = 200;
        mockResponse.headers = new HashMap<>();
        List<String> setCookies = new ArrayList<>();
        setCookies.add("JSESSIONID=ABC123; Path=/; HttpOnly");
        setCookies.add("token=xyz789; Domain=example.com; Secure");
        mockResponse.headers.put("Set-Cookie", setCookies);

        pm.setResponse(mockResponse);

        // Test getting JSESSIONID cookie
        Cookie jsessionId = pm.getResponseCookie("JSESSIONID");
        assertNotNull(jsessionId, "JSESSIONID cookie should exist");
        assertEquals(jsessionId.name, "JSESSIONID");
        assertEquals(jsessionId.value, "ABC123");
        assertEquals(jsessionId.path, "/");
        assertTrue(jsessionId.httpOnly);

        // Test getting token cookie
        Cookie token = pm.getResponseCookie("token");
        assertNotNull(token, "token cookie should exist");
        assertEquals(token.name, "token");
        assertEquals(token.value, "xyz789");
        assertEquals(token.domain, "example.com");
        assertTrue(token.secure);

        // Test getting non-existent cookie
        Cookie nonExistent = pm.getResponseCookie("nonexistent");
        assertNull(nonExistent, "Non-existent cookie should return null");
    }

    @Test
    public void testGetResponseCookieInJavaScript() throws Exception {
        Environment env = new Environment();
        env.setName("Test");

        PostmanApiContext pm = new PostmanApiContext(env);

        // Set up a mock response with Set-Cookie header
        HttpResponse mockResponse = new HttpResponse();
        mockResponse.code = 200;
        mockResponse.headers = new HashMap<>();
        List<String> setCookies = new ArrayList<>();
        setCookies.add("JSESSIONID=ABC123DEF456; Path=/api; Secure; HttpOnly");
        mockResponse.headers.put("Set-Cookie", setCookies);

        pm.setResponse(mockResponse);

        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .option("js.ecmascript-version", "2021")
                .build()) {

            // Bind pm object to JavaScript context
            context.getBindings("js").putMember("pm", pm);

            // Test 1: pm.getResponseCookie should be accessible
            context.eval("js", "var cookie = pm.getResponseCookie('JSESSIONID')");

            // Test 2: Get cookie and access its properties
            context.eval("js", "var jsessionId = pm.getResponseCookie('JSESSIONID')");

            // Verify cookie name
            String cookieName = context.eval("js", "jsessionId.name").asString();
            assertEquals(cookieName, "JSESSIONID");

            // Verify cookie value
            String cookieValue = context.eval("js", "jsessionId.value").asString();
            assertEquals(cookieValue, "ABC123DEF456");

            // Verify cookie path
            String cookiePath = context.eval("js", "jsessionId.path").asString();
            assertEquals(cookiePath, "/api");

            // Verify cookie is secure
            Boolean isSecure = context.eval("js", "jsessionId.secure").asBoolean();
            assertTrue(isSecure);

            // Verify cookie is httpOnly
            Boolean isHttpOnly = context.eval("js", "jsessionId.httpOnly").asBoolean();
            assertTrue(isHttpOnly);

            // Test 3: Use cookie in environment variable (common use case)
            context.eval("js",
                    "var cookie = pm.getResponseCookie('JSESSIONID');" +
                            "if (cookie) {" +
                            "    pm.environment.set('session_id', cookie.value);" +
                            "}");

            // Verify the environment variable was set
            assertEquals(env.get("session_id"), "ABC123DEF456");
        }
    }

    @Test
    public void testGetResponseCookieComplexParsing() {
        Environment env = new Environment();
        env.setName("Test");

        PostmanApiContext pm = new PostmanApiContext(env);

        // Test complex cookie with all attributes
        HttpResponse mockResponse = new HttpResponse();
        mockResponse.code = 200;
        mockResponse.headers = new HashMap<>();
        List<String> setCookies = new ArrayList<>();
        setCookies.add("session=abcd1234; Domain=.example.com; Path=/; Expires=Wed, 21 Oct 2025 07:28:00 GMT; Max-Age=3600; Secure; HttpOnly; SameSite=Strict");
        mockResponse.headers.put("Set-Cookie", setCookies);

        pm.setResponse(mockResponse);

        Cookie cookie = pm.getResponseCookie("session");
        assertNotNull(cookie);
        assertEquals(cookie.name, "session");
        assertEquals(cookie.value, "abcd1234");
        assertEquals(cookie.domain, ".example.com");
        assertEquals(cookie.path, "/");
        assertEquals(cookie.expires, "Wed, 21 Oct 2025 07:28:00 GMT");
        assertEquals(cookie.maxAge, Integer.valueOf(3600));
        assertTrue(cookie.secure);
        assertTrue(cookie.httpOnly);
        assertEquals(cookie.sameSite, "Strict");
    }

    @Test
    public void testGetResponseCookieWithNullResponse() {
        Environment env = new Environment();
        env.setName("Test");

        PostmanApiContext pm = new PostmanApiContext(env);

        // No response set
        Cookie cookie = pm.getResponseCookie("anything");
        assertNull(cookie, "Should return null when no response is set");
    }
}

