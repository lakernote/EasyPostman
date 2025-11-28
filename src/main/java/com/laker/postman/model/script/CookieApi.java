package com.laker.postman.model.script;

import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Cookie API (pm.cookies)
 * <p>
 * 提供 Cookie 的读取、设置和删除功能，模拟 Postman 的 Cookie 管理接口。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 获取 Cookie
 * var token = pm.cookies.get("session_token");
 *
 * // 设置 Cookie
 * pm.cookies.set({
 *     name: "user_id",
 *     value: "12345",
 *     domain: "example.com",
 *     path: "/",
 *     secure: true,
 *     httpOnly: true
 * });
 *
 * // 删除 Cookie
 * pm.cookies.delete("session_token");
 *
 * // 使用 Cookie Jar
 * var jar = pm.cookies.jar();
 * jar.unset(pm.request.url, 'session_id', function(error) {
 *     if (error) {
 *         console.error('Failed to delete cookie');
 *     }
 * });
 * }</pre>
 */
public class CookieApi {
    /**
     * Cookie 存储列表
     */
    private final List<Cookie> cookiesList = new ArrayList<>();

    /**
     * Cookie Jar 实例（延迟初始化）
     */
    private CookieJar cookieJar;

    /**
     * 获取指定名称的 Cookie
     *
     * @param name Cookie 名称
     * @return Cookie 对象，不存在则返回 null
     */
    public Cookie get(String name) {
        if (name == null) {
            return null;
        }
        return cookiesList.stream()
                .filter(c -> name.equals(c.name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取所有 Cookie
     *
     * @return Cookie 列表
     */
    public List<Cookie> getAll() {
        return new ArrayList<>(cookiesList);
    }

    /**
     * 设置 Cookie（通过名称和值）
     *
     * @param name  Cookie 名称
     * @param value Cookie 值
     */
    public void set(String name, String value) {
        if (name == null || value == null) {
            return;
        }
        Cookie cookie = new Cookie();
        cookie.name = name;
        cookie.value = value;
        set(cookie);
    }

    /**
     * 设置 Cookie（通过 Cookie 对象）
     *
     * @param cookie Cookie 对象
     */
    public void set(Cookie cookie) {
        if (cookie == null || cookie.name == null) {
            return;
        }
        // 删除同名的旧 Cookie
        delete(cookie.name);
        // 添加新 Cookie
        cookiesList.add(cookie);
    }

    /**
     * 设置 Cookie（通过 GraalVM Value 对象，支持 JavaScript 对象）
     *
     * @param cookieValue JavaScript Cookie 对象
     */
    public void set(Value cookieValue) {
        if (cookieValue == null || !cookieValue.hasMembers()) {
            return;
        }

        Cookie cookie = new Cookie();
        if (cookieValue.hasMember("name")) {
            cookie.name = cookieValue.getMember("name").asString();
        }
        if (cookieValue.hasMember("value")) {
            cookie.value = cookieValue.getMember("value").asString();
        }
        if (cookieValue.hasMember("domain")) {
            cookie.domain = cookieValue.getMember("domain").asString();
        }
        if (cookieValue.hasMember("path")) {
            cookie.path = cookieValue.getMember("path").asString();
        }
        if (cookieValue.hasMember("expires")) {
            cookie.expires = cookieValue.getMember("expires").asString();
        }
        if (cookieValue.hasMember("maxAge")) {
            cookie.maxAge = cookieValue.getMember("maxAge").asInt();
        }
        if (cookieValue.hasMember("secure")) {
            cookie.secure = cookieValue.getMember("secure").asBoolean();
        }
        if (cookieValue.hasMember("httpOnly")) {
            cookie.httpOnly = cookieValue.getMember("httpOnly").asBoolean();
        }
        if (cookieValue.hasMember("sameSite")) {
            cookie.sameSite = cookieValue.getMember("sameSite").asString();
        }

        set(cookie);
    }

    /**
     * 删除指定名称的 Cookie
     *
     * @param name Cookie 名称
     */
    public void delete(String name) {
        if (name == null) {
            return;
        }
        cookiesList.removeIf(c -> name.equals(c.name));
    }

    /**
     * 检查是否存在指定名称的 Cookie
     *
     * @param name Cookie 名称
     * @return 是否存在
     */
    public boolean has(String name) {
        if (name == null) {
            return false;
        }
        return cookiesList.stream().anyMatch(c -> name.equals(c.name));
    }

    /**
     * 清空所有 Cookie
     */
    public void clear() {
        cookiesList.clear();
    }

    /**
     * 转换为 Cookie 对象数组（供 JavaScript 访问）
     *
     * @return Cookie 数组
     */
    public Cookie[] toArray() {
        return cookiesList.toArray(new Cookie[0]);
    }

    /**
     * 获取 Cookie Jar 实例
     * <p>
     * Cookie Jar 提供 Postman 兼容的异步 Cookie 操作接口。
     * </p>
     *
     * @return CookieJar 实例
     */
    public CookieJar jar() {
        if (cookieJar == null) {
            cookieJar = new CookieJar(this);
        }
        return cookieJar;
    }
}