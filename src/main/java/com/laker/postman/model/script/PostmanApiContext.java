package com.laker.postman.model.script;

import com.laker.postman.model.*;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.http.HttpService;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Value;

import java.util.*;

/**
 * Postman 脚本 API 上下文 (pm 对象)
 * <p>
 * 该类模拟 Postman 中的 <code>pm</code> 对象，为 PreRequest/PostRequest 脚本提供完整的 API 支持。
 * 在脚本执行时，该对象会被注入到 JavaScript 引擎中，供脚本代码访问。
 * </p>
 *
 * <h3>主要功能模块：</h3>
 * <ul>
 *   <li><b>环境变量管理</b>: pm.environment.set/get - 操作当前激活的环境变量</li>
 *   <li><b>全局变量管理</b>: pm.globals.set/get - 操作全局变量（实际使用环境变量实现）</li>
 *   <li><b>临时变量管理</b>: pm.variables.set/get - 操作请求生命周期内的临时变量</li>
 *   <li><b>Cookie 操作</b>: pm.cookies.get/set/delete - 管理请求的 Cookie</li>
 *   <li><b>测试断言</b>: pm.test() - 执行测试断言并记录结果</li>
 *   <li><b>请求访问</b>: pm.request - 访问和修改当前请求信息</li>
 *   <li><b>响应访问</b>: pm.response - 访问响应数据（仅后置脚本可用）</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // PreRequest Script
 * pm.environment.set("timestamp", Date.now());
 * pm.variables.set("requestId", pm.uuid());
 * pm.request.headers.add({key: "X-Request-ID", value: pm.variables.get("requestId")});
 *
 * // PostRequest Script
 * pm.test("Status code is 200", function() {
 *     pm.expect(pm.response.code).to.equal(200);
 * });
 * pm.environment.set("token", pm.response.json().access_token);
 * }</pre>
 *
 * @author laker
 * @see <a href="https://learning.postman.com/docs/writing-scripts/script-references/postman-sandbox-api-reference/">Postman Scripting API Reference</a>
 */
@Slf4j
public class PostmanApiContext {

    /**
     * 测试结果列表 - 存储所有 pm.test() 调用的结果
     */
    public List<TestResult> testResults = new ArrayList<>();

    /**
     * 环境变量对象 - 对应 pm.environment
     */
    public Environment environment;

    /**
     * 环境变量别名 - Postman 中 pm.env 和 pm.environment 指向同一对象
     */
    public Environment env;

    /**
     * 响应对象 - 对应 pm.response，仅在后置脚本中可用
     */
    public ResponseAssertion response;

    /**
     * 临时变量管理器 - 对应 pm.variables，用于存储请求生命周期内的变量
     */
    public TemporaryVariablesApi variables = new TemporaryVariablesApi();

    /**
     * 请求对象包装器 - 对应 pm.request，提供对请求的 JavaScript 访问接口
     */
    public ScriptRequestAccessor request;

    /**
     * Cookie 管理器 - 对应 pm.cookies，提供 Cookie 操作接口
     */
    public CookieApi cookies;

    /**
     * 测试 API - 对应 pm.test，支持 pm.test() 调用和 pm.test.index() 方法
     */
    public TestApi test;

    /**
     * 构造 Postman API 上下文
     *
     * @param environment 当前激活的环境对象
     */
    public PostmanApiContext(Environment environment) {
        this.environment = environment;
        this.env = environment; // Postman 中 env 和 environment 是同一个对象
        this.cookies = new CookieApi(); // 初始化 cookies
        this.test = new TestApi(this); // 初始化 test API
    }

    /**
     * 设置环境变量
     * 对应脚本中的: pm.environment.set(key, value)
     *
     * @param key   变量名
     * @param value 变量值
     */
    public void setEnvironmentVariable(String key, String value) {
        environment.set(key, value);
        EnvironmentService.saveEnvironment(environment);
    }

    /**
     * 设置环境变量（支持任意类型）
     * 对应脚本中的: pm.environment.set(key, value)
     *
     * @param key   变量名
     * @param value 变量值（任意类型）
     */
    public void setEnvironmentVariable(String key, Object value) {
        environment.set(key, value);
        EnvironmentService.saveEnvironment(environment);
    }

    /**
     * 获取环境变量
     * 对应脚本中的: pm.environment.get(key)
     *
     * @param key 变量名
     * @return 变量值，不存在则返回 null
     */
    public String getEnvironmentVariable(String key) {
        return environment.get(key);
    }

    /**
     * 设置全局变量（实际使用环境变量实现）
     * 对应脚本中的: pm.globals.set(key, value)
     *
     * @param key   变量名
     * @param value 变量值
     */
    public void setGlobalVariable(String key, String value) {
        setEnvironmentVariable(key, value);
    }

    /**
     * 设置全局变量（支持任意类型）
     * 对应脚本中的: pm.globals.set(key, value)
     *
     * @param key   变量名
     * @param value 变量值（任意类型）
     */
    public void setGlobalVariable(String key, Object value) {
        setEnvironmentVariable(key, value);
    }

    /**
     * 获取全局变量（实际使用环境变量实现）
     * 对应脚本中的: pm.globals.get(key)
     *
     * @param key 变量名
     * @return 变量值，不存在则返回 null
     */
    public String getGlobalVariable(String key) {
        return getEnvironmentVariable(key);
    }

    /**
     * 执行测试断言（保持向后兼容）
     * 对应脚本中的: pm.test(name, function)
     *
     * <p>示例：
     * <pre>{@code
     * pm.test("Response time is less than 200ms", function() {
     *     pm.expect(pm.response.responseTime).to.be.below(200);
     * });
     * }</pre>
     *
     * @param name         测试名称
     * @param testFunction 测试函数，使用 GraalVM Value 类型以支持 JavaScript 函数
     * @deprecated 推荐使用 test 对象，但保留此方法以保持向后兼容
     */
    public void test(String name, Value testFunction) {
        // 委托给 TestApi
        if (test != null) {
            test.run(name, testFunction);
        }
    }

    /**
     * 设置请求对象
     * 在脚本执行前由框架调用，注入当前请求信息
     *
     * @param preparedRequest 准备好的请求对象
     */
    public void setRequest(PreparedRequest preparedRequest) {
        this.request = new ScriptRequestAccessor(preparedRequest);
    }

    /**
     * 设置响应对象
     * 在后置脚本执行前由框架调用，注入响应信息
     *
     * @param httpResponse HTTP 响应对象
     */
    public void setResponse(HttpResponse httpResponse) {
        this.response = new ResponseAssertion(httpResponse);

        // 自动将响应中的 Cookie 填充到 pm.cookies，使得 pm.cookies.get() 能够工作
        populateResponseCookies(httpResponse);
    }

    /**
     * 从响应头中提取所有 Cookie 并填充到 pm.cookies
     *
     * @param httpResponse HTTP 响应对象
     */
    private void populateResponseCookies(HttpResponse httpResponse) {
        if (httpResponse == null || httpResponse.headers == null || cookies == null) {
            return;
        }

        // 清空旧的响应 Cookie（保留用户手动设置的）
        // 注意：这里不清空，而是覆盖同名的 Cookie

        // 从响应头中查找 Set-Cookie
        List<String> setCookieHeaders = null;
        for (Map.Entry<String, List<String>> entry : httpResponse.headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase("Set-Cookie")) {
                setCookieHeaders = entry.getValue();
                break;
            }
        }

        if (setCookieHeaders == null || setCookieHeaders.isEmpty()) {
            return;
        }

        // 解析所有 Set-Cookie 头并添加到 cookies
        for (String setCookieValue : setCookieHeaders) {
            Cookie cookie = parseCookie(setCookieValue);
            if (cookie != null && cookie.name != null) {
                cookies.set(cookie);
                log.debug("Populated response cookie to pm.cookies: {} = {}", cookie.name, cookie.value);
            }
        }
    }

    /**
     * 生成 UUID
     * 对应脚本中的: pm.uuid()
     *
     * @return UUID 字符串
     */
    public String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成 UUID（别名方法）
     * 对应脚本中的: pm.generateUUID()
     *
     * @return UUID 字符串
     */
    public String generateUUID() {
        return uuid();
    }

    /**
     * 获取当前时间戳（毫秒）
     * 对应脚本中的: pm.getTimestamp()
     *
     * @return 当前时间戳（毫秒）
     */
    public long getTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 设置临时变量
     * 对应脚本中的: pm.setVariable(key, value)
     *
     * @param key   变量名
     * @param value 变量值
     */
    public void setVariable(String key, String value) {
        variables.set(key, value);
    }

    /**
     * 获取临时变量
     * 对应脚本中的: pm.getVariable(key)
     *
     * @param key 变量名
     * @return 变量值，不存在则返回 null
     */
    public String getVariable(String key) {
        return variables.get(key);
    }

    /**
     * 创建断言期望对象
     * 对应脚本中的: pm.expect(actual)
     *
     * <p>示例：
     * <pre>{@code
     * pm.expect(pm.response.code).to.equal(200);
     * pm.expect(pm.response.json().name).to.eql("John");
     * pm.expect(pm.response.text()).to.include("success");
     * pm.expect(pm.response.responseTime).to.be.below(1000);
     * }</pre>
     *
     * @param actual 要断言的实际值
     * @return Expectation 对象，支持链式断言
     */
    public Expectation expect(Object actual) {
        return new Expectation(actual);
    }

    /**
     * 从响应头中获取指定名称的 Cookie
     * 对应脚本中的: pm.getResponseCookie(name)
     *
     * <p>该方法从 HTTP 响应的 Set-Cookie 头中解析并返回指定名称的 Cookie。
     * 常用于在后置脚本中提取服务器设置的 Cookie 值，例如 session ID、token 等。</p>
     *
     * <p>示例：
     * <pre>{@code
     * // 获取响应中的 JSESSIONID cookie
     * var jsessionId = pm.getResponseCookie('JSESSIONID');
     * if (jsessionId) {
     *     pm.environment.set('session_id', jsessionId.value);
     *     console.log('JSESSIONID:', jsessionId.value);
     * }
     * }</pre>
     *
     * @param name Cookie 名称
     * @return Cookie 对象，包含 name、value、domain 等属性；如果不存在则返回 null
     */
    public Cookie getResponseCookie(String name) {
        if (name == null || response == null) {
            return null;
        }

        HttpResponse httpResponse = response.getHttpResponse();
        if (httpResponse == null) {
            return null;
        }
        if (httpResponse.headers == null) {
            return null;
        }

        // 从响应头中查找 Set-Cookie
        List<String> setCookieHeaders = null;
        for (Map.Entry<String, List<String>> entry : httpResponse.headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase("Set-Cookie")) {
                setCookieHeaders = entry.getValue();
                break;
            }
        }

        if (setCookieHeaders == null || setCookieHeaders.isEmpty()) {
            return null;
        }

        // 解析 Set-Cookie 头，查找指定名称的 Cookie
        for (String setCookieValue : setCookieHeaders) {
            Cookie cookie = parseCookie(setCookieValue);
            if (cookie != null && name.equals(cookie.name)) {
                return cookie;
            }
        }

        return null;
    }

    /**
     * 解析 Set-Cookie 头字符串为 Cookie 对象
     *
     * @param setCookieValue Set-Cookie 头的值
     * @return Cookie 对象
     */
    private Cookie parseCookie(String setCookieValue) {
        if (setCookieValue == null || setCookieValue.trim().isEmpty()) {
            return null;
        }

        Cookie cookie = new Cookie();
        String[] parts = setCookieValue.split(";");

        // 解析第一部分：name=value
        if (parts.length > 0) {
            String[] nameValue = parts[0].trim().split("=", 2);
            if (nameValue.length == 2) {
                cookie.name = nameValue[0].trim();
                cookie.value = nameValue[1].trim();
            } else if (nameValue.length == 1) {
                cookie.name = nameValue[0].trim();
                cookie.value = "";
            }
        }

        // 解析其他属性
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            String[] attrValue = part.split("=", 2);
            String attrName = attrValue[0].trim().toLowerCase();

            switch (attrName) {
                case "domain":
                    if (attrValue.length == 2) {
                        cookie.domain = attrValue[1].trim();
                    }
                    break;
                case "path":
                    if (attrValue.length == 2) {
                        cookie.path = attrValue[1].trim();
                    }
                    break;
                case "expires":
                    if (attrValue.length == 2) {
                        cookie.expires = attrValue[1].trim();
                    }
                    break;
                case "max-age":
                    if (attrValue.length == 2) {
                        try {
                            cookie.maxAge = Integer.parseInt(attrValue[1].trim());
                        } catch (NumberFormatException e) {
                            log.debug("Failed to parse Max-Age: {}", attrValue[1]);
                        }
                    }
                    break;
                case "secure":
                    cookie.secure = true;
                    break;
                case "httponly":
                    cookie.httpOnly = true;
                    break;
                case "samesite":
                    if (attrValue.length == 2) {
                        cookie.sameSite = attrValue[1].trim();
                    }
                    break;
            }
        }

        return cookie;
    }

    /**
     * 发送 HTTP 请求
     * 对应脚本中的: pm.sendRequest(requestOptions, callback)
     *
     * @param requestOptions 请求配置，可以是 URL 字符串或包含请求详情的对象
     * @param callback       回调函数，接收 (error, response) 两个参数
     */
    public void sendRequest(Object requestOptions, Value callback) {
        if (requestOptions == null) {
            log.warn("pm.sendRequest: requestOptions is null");
            return;
        }

        try {
            PreparedRequest preparedRequest = buildPreparedRequest(requestOptions);

            if (preparedRequest.url == null || preparedRequest.url.isEmpty()) {
                throw new IllegalArgumentException("pm.sendRequest: URL is required");
            }
            if (preparedRequest.method == null || preparedRequest.method.isEmpty()) {
                throw new IllegalArgumentException("pm.sendRequest: HTTP method is required");
            }

            HttpResponse httpResponse = HttpService.sendRequest(preparedRequest, null);
            ResponseAssertion responseWrapper = new ResponseAssertion(httpResponse);

            if (callback != null && callback.canExecute()) {
                callback.execute(Value.asValue(null), responseWrapper);
            }

        } catch (Exception e) {
            log.error("pm.sendRequest failed: {}", e.getMessage(), e);

            if (callback != null && callback.canExecute()) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", e.getMessage());
                error.put("name", e.getClass().getSimpleName());

                try {
                    callback.execute(Value.asValue(error), Value.asValue(null));
                } catch (Exception callbackEx) {
                    log.error("Error executing callback: {}", callbackEx.getMessage());
                }
            }
        }
    }

    /**
     * 从请求配置对象构建 PreparedRequest
     *
     * @param requestOptions 请求配置（可以是字符串 URL 或配置对象）
     * @return PreparedRequest
     */
    private PreparedRequest buildPreparedRequest(Object requestOptions) {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.headersList = new ArrayList<>();
        preparedRequest.paramsList = new ArrayList<>();
        preparedRequest.method = "GET";

        if (requestOptions instanceof String urlStr) {
            preparedRequest.url = urlStr;
        } else if (requestOptions instanceof Map<?, ?> optionsMap) {
            // 提取 URL
            Object urlObj = optionsMap.get("url");
            if (urlObj != null) {
                String url = String.valueOf(urlObj);
                if (!url.isEmpty() && !url.equals("null") && !url.equals("undefined")) {
                    preparedRequest.url = url;
                }
            }

            // 提取 method
            Object methodObj = optionsMap.get("method");
            if (methodObj != null) {
                String method = String.valueOf(methodObj);
                if (!method.isEmpty() && !method.equals("null") && !method.equals("undefined")) {
                    preparedRequest.method = method.toUpperCase();
                }
            }

            // 提取 header
            Object headerObj = optionsMap.get("header");
            if (headerObj instanceof Map) {
                Map<?, ?> headerMap = (Map<?, ?>) headerObj;
                for (Map.Entry<?, ?> entry : headerMap.entrySet()) {
                    HttpHeader header = new HttpHeader();
                    header.setEnabled(true);
                    header.setKey(String.valueOf(entry.getKey()));
                    header.setValue(String.valueOf(entry.getValue()));
                    preparedRequest.headersList.add(header);
                }
            } else if (headerObj instanceof List) {
                List<?> headerList = (List<?>) headerObj;
                for (Object item : headerList) {
                    if (item instanceof Map) {
                        Map<?, ?> headerItem = (Map<?, ?>) item;
                        Object keyObj = headerItem.get("key");
                        Object valueObj = headerItem.get("value");
                        if (keyObj != null && valueObj != null) {
                            HttpHeader header = new HttpHeader();
                            header.setEnabled(true);
                            header.setKey(String.valueOf(keyObj));
                            header.setValue(String.valueOf(valueObj));
                            preparedRequest.headersList.add(header);
                        }
                    }
                }
            }

            // 提取 body
            Object bodyObj = optionsMap.get("body");
            if (bodyObj instanceof Map) {
                Map<?, ?> bodyMap = (Map<?, ?>) bodyObj;
                Object modeObj = bodyMap.get("mode");

                if (modeObj != null) {
                    String mode = String.valueOf(modeObj);

                    if ("raw".equals(mode)) {
                        Object rawObj = bodyMap.get("raw");
                        if (rawObj != null) {
                            preparedRequest.bodyType = "raw";
                            preparedRequest.body = String.valueOf(rawObj);
                        }
                    } else if ("formdata".equals(mode)) {
                        Object formDataObj = bodyMap.get("formdata");
                        if (formDataObj instanceof List) {
                            preparedRequest.bodyType = "formdata";
                            preparedRequest.formDataList = new ArrayList<>();
                            List<?> formDataList = (List<?>) formDataObj;
                            for (Object item : formDataList) {
                                if (item instanceof Map) {
                                    Map<?, ?> formDataItem = (Map<?, ?>) item;
                                    Object keyObj = formDataItem.get("key");
                                    Object valueObj = formDataItem.get("value");
                                    if (keyObj != null && valueObj != null) {
                                        HttpFormData formData = new HttpFormData();
                                        formData.setEnabled(true);
                                        formData.setKey(String.valueOf(keyObj));
                                        formData.setValue(String.valueOf(valueObj));
                                        formData.setType(HttpFormData.TYPE_TEXT);
                                        preparedRequest.formDataList.add(formData);
                                    }
                                }
                            }
                        }
                    } else if ("urlencoded".equals(mode)) {
                        Object urlencodedObj = bodyMap.get("urlencoded");
                        if (urlencodedObj instanceof List) {
                            preparedRequest.bodyType = "x-www-form-urlencoded";
                            preparedRequest.urlencodedList = new ArrayList<>();
                            List<?> urlencodedList = (List<?>) urlencodedObj;
                            for (Object item : urlencodedList) {
                                if (item instanceof Map) {
                                    Map<?, ?> urlencodedItem = (Map<?, ?>) item;
                                    Object keyObj = urlencodedItem.get("key");
                                    Object valueObj = urlencodedItem.get("value");
                                    if (keyObj != null && valueObj != null) {
                                        HttpFormUrlencoded urlencoded = new HttpFormUrlencoded();
                                        urlencoded.setEnabled(true);
                                        urlencoded.setKey(String.valueOf(keyObj));
                                        urlencoded.setValue(String.valueOf(valueObj));
                                        preparedRequest.urlencodedList.add(urlencoded);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return preparedRequest;
    }

}
