package com.laker.postman.model;

import com.laker.postman.service.EnvironmentService;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class Postman {
    public List<TestResult> testResults = new ArrayList<>();
    public Environment environment; // Postman 中 env 和 environment 是同一个对象，保持一致性
    public Environment env; // Postman 中 env 和 environment 是同一个对象，保持一致性
    public ResponseAssertion response; // Postman 中 pm.response 对应的对象
    public PostmanVariables variables = new PostmanVariables(); // 局部变量 Postman 中 pm.variables 对应的对象
    public JsRequestWrapper request;
    public PostmanCookies cookies; // Cookie 管理对象


    public Postman(Environment environment) {
        this.environment = environment;
        this.env = environment; // Postman 中 env 和 environment 是同一个对象
        this.cookies = new PostmanCookies(); // 初始化 cookies
    }

    // Postman 脚本中的 pm.environment.set(key, value)
    public void setEnvironmentVariable(String key, String value) {
        environment.set(key, value);
    }

    /**
     * 重载setEnvironmentVariable方法，支持Object类型参数
     * 解决JavaScript中传入数字等非String类型的问题
     */
    // Postman 脚本中的 pm.environment.set(key, value)
    public void setEnvironmentVariable(String key, Object value) {
        if (value != null) {
            environment.set(key, String.valueOf(value));
        }
    }

    public void setResponse(HttpResponse response) {
        this.response = new ResponseAssertion(response);
        // 同时设置响应的 Cookies
        if (this.cookies != null && response != null) {
            this.cookies.setResponse(response);
        }
    }

    public void setRequest(PreparedRequest request) {
        this.request = new JsRequestWrapper(request);
    }

    // Postman 脚本中的 pm.test(name, fn)
    public void test(String name, Value fn) {
        if (this.response == null) {
            testResults.add(new TestResult(name, false, "pm.response is null"));
            return;
        }
        if (fn != null && fn.canExecute()) {
            try {
                fn.executeVoid();
                testResults.add(new TestResult(name, true, null));
            } catch (Exception e) {
                testResults.add(new TestResult(name, false, e.getMessage()));
                throw e;
            }
        }
    }

    // pm.expect 断言入口
    public Expectation expect(Object actual) {
        if (this.response != null) {
            return this.response.expect(actual);
        }
        return new Expectation(actual);
    }


    /**
     * 设置一个请求变量，仅在当前请求有效
     */
    public void setVariable(String key, Object value) {
        variables.set(key, value != null ? value.toString() : null);
    }

    /**
     * 获取请求变量
     */
    public Object getVariable(String key) {
        return variables.get(key);
    }

    /**
     * 检查请求变量是否存在
     */
    public boolean hasVariable(String key) {
        return variables.has(key);
    }

    /**
     * 删除请求变量
     */
    public void unsetVariable(String key) {
        variables.unset(key);
    }

    /**
     * 清除所有请求变量
     */
    public void clearVariables() {
        variables.clear();
    }

    // 内部类，用于支持 pm.variables.set() 语法
    public static class PostmanVariables {
        /**
         * 设置局部变量 - 对应 pm.variables.set()
         * 支持多种数据类型
         */
        public void set(String key, Object value) {
            if (value != null) {
                EnvironmentService.setTemporaryVariable(key, String.valueOf(value));
            } else {
                EnvironmentService.setTemporaryVariable(key, null);
            }
        }

        /**
         * 重载方法，保持与原有 String 参数的兼容性
         */
        public void set(String key, String value) {
            EnvironmentService.setTemporaryVariable(key, value);
        }

        /**
         * 获取局部变量 - 对应 pm.variables.get()
         */
        public String get(String key) {
            return EnvironmentService.getTemporaryVariable(key);
        }

        /**
         * 检查局部变量是否存在 - 对应 pm.variables.has()
         */
        public boolean has(String key) {
            return EnvironmentService.getTemporaryVariable(key) != null;
        }

        /**
         * 删除局部变量 - 对应 pm.variables.unset()
         */
        public void unset(String key) {
            EnvironmentService.setTemporaryVariable(key, null);
        }

        /**
         * 清除所有局部变量 - 对应 pm.variables.clear()
         */
        public void clear() {
            EnvironmentService.clearTemporaryVariables();
        }
    }

    /**
     * 生成UUID
     */
    public String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成时间戳
     */
    public long getTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取响应 Cookie (Postman 兼容方法)
     * 用法: pm.getResponseCookie("JSESSIONID")
     */
    public PostmanCookie getResponseCookie(String cookieName) {
        if (this.cookies != null) {
            return this.cookies.get(cookieName);
        }
        return null;
    }

    /**
     * Cookie 管理类 - 支持 pm.cookies 和 pm.getResponseCookie()
     */
    public static class PostmanCookies {
        private HttpResponse response;
        private static CookieJar cookieJarInstance; // 单例

        public void setResponse(HttpResponse response) {
            this.response = response;
        }

        /**
         * 获取 Cookie Jar - Postman 官方 API
         * 用法: const jar = pm.cookies.jar();
         *
         * @return Cookie Jar 对象，用于设置和管理 Cookie
         */
        public CookieJar jar() {
            if (cookieJarInstance == null) {
                cookieJarInstance = new CookieJar();
            }
            return cookieJarInstance;
        }

        /**
         * 获取指定名称的响应 Cookie
         * 用法: pm.cookies.get("JSESSIONID")
         */
        public PostmanCookie get(String cookieName) {
            if (response == null || response.headers == null || cookieName == null) {
                return null;
            }

            // 从响应头中查找 Set-Cookie
            List<String> setCookieHeaders = response.headers.get("Set-Cookie");
            if (setCookieHeaders == null || setCookieHeaders.isEmpty()) {
                return null;
            }

            // 解析所有 Set-Cookie 头
            for (String setCookieHeader : setCookieHeaders) {
                PostmanCookie cookie = parseCookie(setCookieHeader);
                if (cookie != null && cookieName.equals(cookie.name)) {
                    return cookie;
                }
            }

            return null;
        }

        /**
         * 获取所有响应 Cookies
         * 用法: pm.cookies.all()
         */
        public List<PostmanCookie> all() {
            List<PostmanCookie> cookies = new ArrayList<>();

            if (response == null || response.headers == null) {
                return cookies;
            }

            List<String> setCookieHeaders = response.headers.get("Set-Cookie");
            if (setCookieHeaders == null || setCookieHeaders.isEmpty()) {
                return cookies;
            }

            for (String setCookieHeader : setCookieHeaders) {
                PostmanCookie cookie = parseCookie(setCookieHeader);
                if (cookie != null) {
                    cookies.add(cookie);
                }
            }

            return cookies;
        }

        /**
         * 检查指定名称的 Cookie 是否存在
         * 用法: pm.cookies.has("JSESSIONID")
         */
        public boolean has(String cookieName) {
            return get(cookieName) != null;
        }

        /**
         * 解析 Set-Cookie 头
         * 格式: JSESSIONID=ABC123; Path=/; HttpOnly; Secure
         */
        private PostmanCookie parseCookie(String setCookieHeader) {
            if (setCookieHeader == null || setCookieHeader.isEmpty()) {
                return null;
            }

            String[] parts = setCookieHeader.split(";");
            if (parts.length == 0) {
                return null;
            }

            // 第一部分是 name=value
            String[] nameValue = parts[0].trim().split("=", 2);
            if (nameValue.length < 2) {
                return null;
            }

            PostmanCookie cookie = new PostmanCookie();
            cookie.name = nameValue[0].trim();
            cookie.value = nameValue[1].trim();

            // 解析其他属性
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i].trim();
                String[] attr = part.split("=", 2);
                String attrName = attr[0].trim().toLowerCase();

                switch (attrName) {
                    case "domain":
                        cookie.domain = attr.length > 1 ? attr[1].trim() : null;
                        break;
                    case "path":
                        cookie.path = attr.length > 1 ? attr[1].trim() : null;
                        break;
                    case "expires":
                        cookie.expires = attr.length > 1 ? attr[1].trim() : null;
                        break;
                    case "max-age":
                        cookie.maxAge = attr.length > 1 ? attr[1].trim() : null;
                        break;
                    case "httponly":
                        cookie.httpOnly = true;
                        break;
                    case "secure":
                        cookie.secure = true;
                        break;
                    case "samesite":
                        cookie.sameSite = attr.length > 1 ? attr[1].trim() : null;
                        break;
                }
            }

            return cookie;
        }
    }

    /**
     * Cookie 对象 - 兼容 Postman 的 Cookie 结构
     */
    public static class PostmanCookie {
        public String name;
        public String value;
        public String domain;
        public String path;
        public String expires;
        public String maxAge;
        public boolean httpOnly = false;
        public boolean secure = false;
        public String sameSite;

        @Override
        public String toString() {
            return String.format("Cookie{name='%s', value='%s', domain='%s', path='%s'}",
                    name, value, domain, path);
        }
    }

    /**
     * Cookie Jar - 支持设置和管理 Cookie (Postman 兼容 API)
     */
    public static class CookieJar {
        /**
         * 设置 Cookie - Postman 兼容 API
         * 用法: pm.cookies.jar().set(url, cookieName, cookieValue)
         * 用法: pm.cookies.jar().set(url, {name, value, domain, path, ...})
         *
         * @param url           Cookie 关联的 URL
         * @param nameOrOptions Cookie 名称（String）或 Cookie 配置对象（Map）
         * @param value         Cookie 值（如果第二个参数是 String）
         */
        public void set(String url, Object nameOrOptions, Object value) {
            try {
                String cookieName;
                String cookieValue;
                String domain = null;
                String path = "/";
                boolean secure = false;
                boolean httpOnly = false;

                // 解析 URL 获取默认 domain
                if (url != null && !url.isEmpty()) {
                    try {
                        java.net.URI uri = new java.net.URI(url);
                        domain = uri.getHost();
                    } catch (Exception e) {
                        log.error("Invalid URL for cookie: " + url, e);
                    }
                }

                // 判断是简单模式还是对象模式
                if (nameOrOptions instanceof String) {
                    // 简单模式: set(url, name, value)
                    cookieName = (String) nameOrOptions;
                    cookieValue = value != null ? String.valueOf(value) : "";
                } else if (nameOrOptions instanceof Map) {
                    // 对象模式: set(url, {name, value, domain, path, ...})
                    Map<String, Object> options = (Map<String, Object>) nameOrOptions;
                    cookieName = String.valueOf(options.get("name"));
                    cookieValue = String.valueOf(options.get("value"));

                    // 覆盖默认值
                    if (options.containsKey("domain")) {
                        domain = String.valueOf(options.get("domain"));
                    }
                    if (options.containsKey("path")) {
                        path = String.valueOf(options.get("path"));
                    }
                    if (options.containsKey("secure")) {
                        secure = Boolean.TRUE.equals(options.get("secure"));
                    }
                    if (options.containsKey("httpOnly")) {
                        httpOnly = Boolean.TRUE.equals(options.get("httpOnly"));
                    }
                } else {
                    log.error("Invalid cookie options type");
                    return;
                }

                // 调用 CookieService 添加 Cookie
                com.laker.postman.service.http.CookieService.addCookie(
                        cookieName, cookieValue, domain, path, secure, httpOnly
                );

                log.info("Cookie set: {}={} (domain={}, path={})",
                        cookieName, cookieValue, domain, path);

            } catch (Exception e) {
                log.error("Failed to set cookie", e);
            }
        }

        /**
         * 重载方法：两个参数版本（使用对象配置）
         */
        public void set(String url, Object options) {
            set(url, options, null);
        }

        /**
         * 获取 Cookie - Postman 兼容 API
         * 用法: pm.cookies.jar().get(url, cookieName, callback)
         *
         * @param url        Cookie 关联的 URL
         * @param cookieName Cookie 名称
         * @param callback   回调函数 (error, cookie)
         */
        public void get(String url, String cookieName, org.graalvm.polyglot.Value callback) {
            try {
                String domain = null;
                if (url != null && !url.isEmpty()) {
                    try {
                        java.net.URI uri = new java.net.URI(url);
                        domain = uri.getHost();
                    } catch (Exception e) {
                        log.error("Invalid URL for cookie: " + url, e);
                    }
                }

                // 从 CookieService 获取所有 Cookie 并过滤
                List<CookieInfo> allCookies = com.laker.postman.service.http.CookieService.getAllCookieInfos();
                CookieInfo foundCookie = null;

                for (CookieInfo cookie : allCookies) {
                    if (cookie.name.equals(cookieName)) {
                        // 如果指定了 domain，检查匹配
                        if (domain == null || domain.equals(cookie.domain) ||
                                domain.endsWith("." + cookie.domain) ||
                                cookie.domain.endsWith("." + domain)) {
                            foundCookie = cookie;
                            break;
                        }
                    }
                }

                // 执行回调
                if (callback != null && callback.canExecute()) {
                    if (foundCookie != null) {
                        // 成功: callback(null, cookie)
                        PostmanCookie pmCookie = new PostmanCookie();
                        pmCookie.name = foundCookie.name;
                        pmCookie.value = foundCookie.value;
                        pmCookie.domain = foundCookie.domain;
                        pmCookie.path = foundCookie.path;
                        pmCookie.httpOnly = foundCookie.httpOnly;
                        pmCookie.secure = foundCookie.secure;
                        callback.execute(null, pmCookie);
                    } else {
                        // Cookie 不存在: callback(null, null)
                        callback.execute(null, null);
                    }
                }

            } catch (Exception e) {
                log.error("Failed to get cookie", e);
                if (callback != null && callback.canExecute()) {
                    callback.execute(e.getMessage(), null);
                }
            }
        }

        /**
         * 删除 Cookie - Postman 兼容 API
         * 用法: pm.cookies.jar().unset(url, cookieName, callback)
         *
         * @param url        Cookie 关联的 URL
         * @param cookieName Cookie 名称
         * @param callback   回调函数 (error)
         */
        public void unset(String url, String cookieName, org.graalvm.polyglot.Value callback) {
            try {
                String domain = null;
                if (url != null && !url.isEmpty()) {
                    try {
                        java.net.URI uri = new java.net.URI(url);
                        domain = uri.getHost();
                    } catch (Exception e) {
                        log.error("Invalid URL for cookie: " + url, e);
                    }
                }

                // 删除 Cookie
                com.laker.postman.service.http.CookieService.removeCookie(cookieName, domain, null);

                log.info("Cookie unset: {} (domain={})", cookieName, domain);

                // 执行回调
                if (callback != null && callback.canExecute()) {
                    callback.execute((Object) null);
                }

            } catch (Exception e) {
                log.error("Failed to unset cookie", e);
                if (callback != null && callback.canExecute()) {
                    callback.execute(e.getMessage());
                }
            }
        }

        /**
         * 删除 Cookie - 无回调版本
         */
        public void unset(String url, String cookieName) {
            unset(url, cookieName, null);
        }

        /**
         * 清除指定 URL 的所有 Cookie - Postman 兼容 API
         * 用法: pm.cookies.jar().clear(url, callback)
         *
         * @param url      要清除 Cookie 的 URL
         * @param callback 回调函数 (error)
         */
        public void clear(String url, org.graalvm.polyglot.Value callback) {
            try {
                String domain = null;
                if (url != null && !url.isEmpty()) {
                    try {
                        java.net.URI uri = new java.net.URI(url);
                        domain = uri.getHost();
                    } catch (Exception e) {
                        log.error("Invalid URL for cookie: " + url, e);
                    }
                }

                // 获取所有 Cookie 并删除匹配的
                List<CookieInfo> allCookies = com.laker.postman.service.http.CookieService.getAllCookieInfos();
                int removedCount = 0;

                for (CookieInfo cookie : allCookies) {
                    if (domain == null || domain.equals(cookie.domain) ||
                            domain.endsWith("." + cookie.domain) ||
                            cookie.domain.endsWith("." + domain)) {
                        com.laker.postman.service.http.CookieService.removeCookie(
                                cookie.name, cookie.domain, cookie.path
                        );
                        removedCount++;
                    }
                }

                log.info("Cleared {} cookies for domain: {}", removedCount, domain);

                // 执行回调
                if (callback != null && callback.canExecute()) {
                    callback.execute((Object) null);
                }

            } catch (Exception e) {
                log.error("Failed to clear cookies", e);
                if (callback != null && callback.canExecute()) {
                    callback.execute(e.getMessage());
                }
            }
        }

        /**
         * 清除指定 URL 的所有 Cookie - 无回调版本
         */
        public void clear(String url) {
            clear(url, null);
        }

        /**
         * 获取指定 URL 的所有 Cookie - Postman 兼容 API
         * 用法: pm.cookies.jar().getAll(url, callback)
         *
         * @param url      Cookie 关联的 URL
         * @param callback 回调函数 (error, cookies)
         */
        public void getAll(String url, org.graalvm.polyglot.Value callback) {
            try {
                String domain = null;
                if (url != null && !url.isEmpty()) {
                    try {
                        java.net.URI uri = new java.net.URI(url);
                        domain = uri.getHost();
                    } catch (Exception e) {
                        log.error("Invalid URL for cookie: " + url, e);
                    }
                }

                // 获取所有匹配的 Cookie
                List<CookieInfo> allCookies = com.laker.postman.service.http.CookieService.getAllCookieInfos();
                List<PostmanCookie> matchedCookies = new ArrayList<>();

                for (CookieInfo cookie : allCookies) {
                    if (domain == null || domain.equals(cookie.domain) ||
                            domain.endsWith("." + cookie.domain) ||
                            cookie.domain.endsWith("." + domain)) {
                        PostmanCookie pmCookie = new PostmanCookie();
                        pmCookie.name = cookie.name;
                        pmCookie.value = cookie.value;
                        pmCookie.domain = cookie.domain;
                        pmCookie.path = cookie.path;
                        pmCookie.httpOnly = cookie.httpOnly;
                        pmCookie.secure = cookie.secure;
                        matchedCookies.add(pmCookie);
                    }
                }

                // 执行回调
                if (callback != null && callback.canExecute()) {
                    callback.execute(null, matchedCookies);
                }

            } catch (Exception e) {
                log.error("Failed to get all cookies", e);
                if (callback != null && callback.canExecute()) {
                    callback.execute(e.getMessage(), null);
                }
            }
        }

        /**
         * 获取所有 Cookie - 无回调版本（同步）
         */
        public List<PostmanCookie> getAll(String url) {
            try {
                String domain = null;
                if (url != null && !url.isEmpty()) {
                    try {
                        java.net.URI uri = new java.net.URI(url);
                        domain = uri.getHost();
                    } catch (Exception e) {
                        log.error("Invalid URL for cookie: " + url, e);
                    }
                }

                List<CookieInfo> allCookies = com.laker.postman.service.http.CookieService.getAllCookieInfos();
                List<PostmanCookie> matchedCookies = new ArrayList<>();

                for (CookieInfo cookie : allCookies) {
                    if (domain == null || domain.equals(cookie.domain) ||
                            domain.endsWith("." + cookie.domain) ||
                            cookie.domain.endsWith("." + domain)) {
                        PostmanCookie pmCookie = new PostmanCookie();
                        pmCookie.name = cookie.name;
                        pmCookie.value = cookie.value;
                        pmCookie.domain = cookie.domain;
                        pmCookie.path = cookie.path;
                        pmCookie.httpOnly = cookie.httpOnly;
                        pmCookie.secure = cookie.secure;
                        matchedCookies.add(pmCookie);
                    }
                }

                return matchedCookies;
            } catch (Exception e) {
                log.error("Failed to get all cookies", e);
                return new ArrayList<>();
            }
        }
    }
}