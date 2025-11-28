package com.laker.postman.service.js;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;

/**
 * 脚本代码片段管理器
 * 管理自动补全的代码片段和提示信息
 */
@UtilityClass
public class ScriptSnippetManager {

    /**
     * 创建自动补全提供器
     */
    public static CompletionProvider createCompletionProvider() {
        DefaultCompletionProvider provider = new DefaultCompletionProvider();

        // 添加基础对象补全
        addBasicCompletions(provider);

        // 添加代码片段补全
        addSnippetCompletions(provider);

        return provider;
    }

    /**
     * 添加基础对象补全
     */
    private static void addBasicCompletions(DefaultCompletionProvider provider) {
        // ========== 核心对象 ==========
        provider.addCompletion(new BasicCompletion(provider, "pm",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM)));
        provider.addCompletion(new BasicCompletion(provider, "postman",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_POSTMAN)));
        provider.addCompletion(new BasicCompletion(provider, "request",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_REQUEST)));
        provider.addCompletion(new BasicCompletion(provider, "response",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_RESPONSE)));
        provider.addCompletion(new BasicCompletion(provider, "env",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_ENV)));

        // ========== pm 对象方法 ==========
        provider.addCompletion(new BasicCompletion(provider, "pm.test",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_TEST)));
        provider.addCompletion(new BasicCompletion(provider, "pm.expect",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_EXPECT)));
        provider.addCompletion(new BasicCompletion(provider, "pm.generateUUID",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_GENERATE_UUID)));
        provider.addCompletion(new BasicCompletion(provider, "pm.getTimestamp",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_GET_TIMESTAMP)));
        provider.addCompletion(new BasicCompletion(provider, "pm.setVariable",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_SET_VARIABLE)));
        provider.addCompletion(new BasicCompletion(provider, "pm.getVariable",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_GET_VARIABLE)));
        provider.addCompletion(new BasicCompletion(provider, "pm.getResponseCookie",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_GET_RESPONSE_COOKIE)));

        // ========== pm.environment 方法 ==========
        provider.addCompletion(new BasicCompletion(provider, "pm.environment.set",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_ENV_SET)));
        provider.addCompletion(new BasicCompletion(provider, "pm.environment.get",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_ENV_GET)));
        provider.addCompletion(new BasicCompletion(provider, "pm.environment.has",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_ENV_HAS)));
        provider.addCompletion(new BasicCompletion(provider, "pm.environment.unset",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_ENV_UNSET)));
        provider.addCompletion(new BasicCompletion(provider, "pm.environment.clear",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_ENV_CLEAR)));

        // ========== pm.variables 方法 ==========
        provider.addCompletion(new BasicCompletion(provider, "pm.variables.set",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_VAR_SET)));
        provider.addCompletion(new BasicCompletion(provider, "pm.variables.get",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_VAR_GET)));
        provider.addCompletion(new BasicCompletion(provider, "pm.variables.has",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_VAR_HAS)));
        provider.addCompletion(new BasicCompletion(provider, "pm.variables.unset",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_VAR_UNSET)));
        provider.addCompletion(new BasicCompletion(provider, "pm.variables.clear",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_VAR_CLEAR)));

        // ========== pm.request 方法 ==========
        provider.addCompletion(new BasicCompletion(provider, "pm.request.url",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_REQUEST_URL)));
        provider.addCompletion(new BasicCompletion(provider, "pm.request.method",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_REQUEST_METHOD)));
        provider.addCompletion(new BasicCompletion(provider, "pm.request.headers",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_REQUEST_HEADERS)));
        provider.addCompletion(new BasicCompletion(provider, "pm.request.body",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_REQUEST_BODY)));
        provider.addCompletion(new BasicCompletion(provider, "pm.request.params",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_REQUEST_PARAMS)));
        provider.addCompletion(new BasicCompletion(provider, "pm.request.formData",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_REQUEST_FORMDATA)));
        provider.addCompletion(new BasicCompletion(provider, "pm.request.urlencoded",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_REQUEST_URLENCODED)));
        provider.addCompletion(new BasicCompletion(provider, "pm.request.formFiles",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_REQUEST_FORMFILES)));

        // ========== pm.response 方法 ==========
        provider.addCompletion(new BasicCompletion(provider, "pm.response.code",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_CODE)));
        provider.addCompletion(new BasicCompletion(provider, "pm.response.status",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_STATUS)));
        provider.addCompletion(new BasicCompletion(provider, "pm.response.headers",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_HEADERS)));
        provider.addCompletion(new BasicCompletion(provider, "pm.response.json",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_JSON)));
        provider.addCompletion(new BasicCompletion(provider, "pm.response.text",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_TEXT)));
        provider.addCompletion(new BasicCompletion(provider, "pm.response.responseTime",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_TIME)));
        provider.addCompletion(new BasicCompletion(provider, "pm.response.to.have.status",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_TO_HAVE_STATUS)));
        provider.addCompletion(new BasicCompletion(provider, "pm.response.to.have.header",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_RESPONSE_TO_HAVE_HEADER)));

        // ========== pm.cookies 方法 ==========
        provider.addCompletion(new BasicCompletion(provider, "pm.cookies.get",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_COOKIES_GET)));
        provider.addCompletion(new BasicCompletion(provider, "pm.cookies.has",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_COOKIES_HAS)));
        provider.addCompletion(new BasicCompletion(provider, "pm.cookies.all",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_COOKIES_ALL)));
        provider.addCompletion(new BasicCompletion(provider, "pm.cookies.jar",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_PM_COOKIES_JAR)));

        // ========== 响应相关（旧版兼容） ==========
        provider.addCompletion(new BasicCompletion(provider, "responseBody",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_RESPONSE_BODY)));
        provider.addCompletion(new BasicCompletion(provider, "responseHeaders",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_RESPONSE_HEADERS)));
        provider.addCompletion(new BasicCompletion(provider, "status",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_STATUS)));
        provider.addCompletion(new BasicCompletion(provider, "statusCode",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_STATUS_CODE)));

        // ========== 环境变量方法（旧版兼容） ==========
        provider.addCompletion(new BasicCompletion(provider, "setEnvironmentVariable",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SET_ENV)));
        provider.addCompletion(new BasicCompletion(provider, "getEnvironmentVariable",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_GET_ENV)));

        // ========== Console 对象 ==========
        provider.addCompletion(new BasicCompletion(provider, "console.log",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_CONSOLE_LOG)));
        provider.addCompletion(new BasicCompletion(provider, "console.warn",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_CONSOLE_WARN)));
        provider.addCompletion(new BasicCompletion(provider, "console.error",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_CONSOLE_ERROR)));
        provider.addCompletion(new BasicCompletion(provider, "console.info",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_CONSOLE_INFO)));

        // ========== JavaScript 内置对象 ==========
        provider.addCompletion(new BasicCompletion(provider, "JSON.parse",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_JSON_PARSE)));
        provider.addCompletion(new BasicCompletion(provider, "JSON.stringify",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_JSON_STRINGIFY)));
        provider.addCompletion(new BasicCompletion(provider, "Date.now",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_DATE_NOW)));
        provider.addCompletion(new BasicCompletion(provider, "Math.random",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MATH_RANDOM)));

        // ========== 编码/解码函数 ==========
        provider.addCompletion(new BasicCompletion(provider, "btoa",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_BTOA)));
        provider.addCompletion(new BasicCompletion(provider, "atob",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_ATOB)));
        provider.addCompletion(new BasicCompletion(provider, "encodeURIComponent",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_ENCODE_URI)));
        provider.addCompletion(new BasicCompletion(provider, "decodeURIComponent",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_DECODE_URI)));
        provider.addCompletion(new BasicCompletion(provider, "encodeURI",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_ENCODE_URI_FULL)));
        provider.addCompletion(new BasicCompletion(provider, "decodeURI",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_DECODE_URI_FULL)));

        // ========== 加密函数 ==========
        provider.addCompletion(new BasicCompletion(provider, "MD5",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_MD5)));
        provider.addCompletion(new BasicCompletion(provider, "SHA256",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SHA256)));

        // ========== JavaScript关键字 ==========
        provider.addCompletion(new BasicCompletion(provider, "if",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_IF)));
        provider.addCompletion(new BasicCompletion(provider, "else",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_ELSE)));
        provider.addCompletion(new BasicCompletion(provider, "for",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_FOR)));
        provider.addCompletion(new BasicCompletion(provider, "while",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_WHILE)));
        provider.addCompletion(new BasicCompletion(provider, "function",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_FUNCTION)));
        provider.addCompletion(new BasicCompletion(provider, "return",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_RETURN)));
        provider.addCompletion(new BasicCompletion(provider, "var",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_VAR)));
        provider.addCompletion(new BasicCompletion(provider, "let",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_LET)));
        provider.addCompletion(new BasicCompletion(provider, "const",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_CONST)));
    }

    /**
     * 添加代码片段补全
     */
    private static void addSnippetCompletions(DefaultCompletionProvider provider) {
        // ========== 环境变量操作 ==========
        provider.addCompletion(new ShorthandCompletion(provider,
                "pm.environment.set",
                "pm.environment.set('key', 'value');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_SET_ENV)));
        provider.addCompletion(new ShorthandCompletion(provider,
                "pm.environment.get",
                "pm.environment.get('key');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_GET_ENV)));

        // ========== 局部变量操作 ==========
        provider.addCompletion(new ShorthandCompletion(provider,
                "pm.variables.set",
                "pm.variables.set('key', 'value');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_VAR_SET)));
        provider.addCompletion(new ShorthandCompletion(provider,
                "pm.variables.get",
                "pm.variables.get('key');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_VAR_GET)));

        // ========== UUID 和时间戳 ==========
        provider.addCompletion(new ShorthandCompletion(provider,
                "pm.generateUUID",
                "pm.environment.set('uuid', pm.generateUUID());",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_GENERATE_UUID)));
        provider.addCompletion(new ShorthandCompletion(provider,
                "pm.getTimestamp",
                "pm.environment.set('timestamp', pm.getTimestamp());",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_GET_TIMESTAMP)));

        // ========== 断言测试 ==========
        provider.addCompletion(new ShorthandCompletion(provider,
                "pm.test",
                "pm.test('Test name', function() {\n    pm.expect(pm.response.code).to.equal(200);\n});",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_TEST)));
        provider.addCompletion(new ShorthandCompletion(provider,
                "status200",
                "pm.test('Status code is 200', function() {\n    pm.response.to.have.status(200);\n});",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_STATUS_200)));
        provider.addCompletion(new ShorthandCompletion(provider,
                "responseTime",
                "pm.test('Response time is less than 1000ms', function() {\n    pm.expect(pm.response.responseTime).to.be.below(1000);\n});",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_RESPONSE_TIME)));
        provider.addCompletion(new ShorthandCompletion(provider,
                "bodyContains",
                "pm.test('Body contains string', function() {\n    pm.expect(pm.response.text()).to.include('success');\n});",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_BODY_CONTAINS)));
        provider.addCompletion(new ShorthandCompletion(provider,
                "jsonValue",
                "pm.test('JSON value check', function() {\n    var jsonData = pm.response.json();\n    pm.expect(jsonData.code).to.eql(0);\n});",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_JSON_VALUE)));

        // ========== 提取数据 ==========
        provider.addCompletion(new ShorthandCompletion(provider,
                "extractJson",
                "var jsonData = pm.response.json();\npm.environment.set('token', jsonData.token);",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_EXTRACT_JSON)));
        provider.addCompletion(new ShorthandCompletion(provider,
                "extractHeader",
                "var token = pm.response.headers.get('X-Token');\npm.environment.set('token', token);",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_EXTRACT_HEADER)));

        // ========== Cookie 操作 ==========
        provider.addCompletion(new ShorthandCompletion(provider,
                "getCookie",
                "var cookie = pm.cookies.get('cookieName');\nif (cookie) {\n    console.log('Cookie value:', cookie.value);\n}",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_GET_COOKIE)));
        provider.addCompletion(new ShorthandCompletion(provider,
                "setCookie",
                "var jar = pm.cookies.jar();\njar.set(pm.request.url, 'cookieName', 'cookieValue');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_SET_COOKIE)));

        // ========== 动态修改请求 ==========
        provider.addCompletion(new ShorthandCompletion(provider,
                "addHeader",
                "pm.request.headers.add({\n    key: 'X-Custom-Header',\n    value: 'CustomValue'\n});",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_ADD_HEADER)));
        provider.addCompletion(new ShorthandCompletion(provider,
                "addParam",
                "pm.request.params.add({\n    key: 'timestamp',\n    value: Date.now()\n});",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_ADD_PARAM)));

        // ========== 编码/解码函数 ==========
        provider.addCompletion(new ShorthandCompletion(provider,
                "btoa",
                "var encoded = btoa('String');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_BTOA)));
        provider.addCompletion(new ShorthandCompletion(provider,
                "atob",
                "var decoded = atob('Base64');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_ATOB)));
        provider.addCompletion(new ShorthandCompletion(provider,
                "encodeURIComponent",
                "var encoded = encodeURIComponent('String');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_ENCODE_URI)));
        provider.addCompletion(new ShorthandCompletion(provider,
                "decodeURIComponent",
                "var decoded = decodeURIComponent('String');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_DECODE_URI)));

        // ========== 加密函数 ==========
        provider.addCompletion(new ShorthandCompletion(provider,
                "MD5",
                "var hash = MD5('Message').toString();",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_MD5)));
        provider.addCompletion(new ShorthandCompletion(provider,
                "SHA256",
                "var hash = SHA256('Message').toString();",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_SHA256)));

        // ========== 控制台输出 ==========
        provider.addCompletion(new ShorthandCompletion(provider,
                "console.log",
                "console.log('Message');",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_CONSOLE_LOG)));

        // ========== JSON操作 ==========
        provider.addCompletion(new ShorthandCompletion(provider,
                "JSON.parse(responseBody)",
                "var jsonData = JSON.parse(responseBody);",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_JSON_PARSE)));
        provider.addCompletion(new ShorthandCompletion(provider,
                "JSON.stringify",
                "var jsonString = JSON.stringify(obj);",
                I18nUtil.getMessage(MessageKeys.AUTOCOMPLETE_SNIPPET_JSON_STRINGIFY)));
    }

}
