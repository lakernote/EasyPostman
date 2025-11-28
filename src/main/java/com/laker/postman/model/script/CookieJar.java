package com.laker.postman.model.script;

import org.graalvm.polyglot.Value;

/**
 * Cookie Jar API
 * <p>
 * 提供类似 Postman 的 Cookie Jar 功能，支持异步回调方式操作 Cookie。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 获取 Cookie Jar
 * var jar = pm.cookies.jar();
 *
 * // 设置 Cookie
 * jar.set(pm.request.url, {
 *     name: "session_id",
 *     value: "abc123"
 * }, function(error) {
 *     if (error) {
 *         console.error('Failed to set cookie:', error);
 *     } else {
 *         console.log('Cookie set successfully');
 *     }
 * });
 *
 * // 获取 Cookie
 * jar.get(pm.request.url, 'session_id', function(error, cookie) {
 *     if (error) {
 *         console.error('Failed to get cookie:', error);
 *     } else {
 *         console.log('Cookie value:', cookie);
 *     }
 * });
 *
 * // 删除 Cookie
 * jar.unset(pm.request.url, 'session_id', function(error) {
 *     if (error) {
 *         console.error('Failed to delete cookie:', error);
 *     } else {
 *         console.log('Cookie deleted successfully');
 *     }
 * });
 *
 * // 获取所有 Cookie
 * jar.getAll(pm.request.url, function(error, cookies) {
 *     if (error) {
 *         console.error('Failed to get cookies:', error);
 *     } else {
 *         console.log('All cookies:', cookies);
 *     }
 * });
 *
 * // 清空所有 Cookie
 * jar.clear(pm.request.url, function(error) {
 *     if (error) {
 *         console.error('Failed to clear cookies:', error);
 *     } else {
 *         console.log('All cookies cleared');
 *     }
 * });
 * }</pre>
 */
public class CookieJar {
    private final CookieApi cookieApi;

    public CookieJar(CookieApi cookieApi) {
        this.cookieApi = cookieApi;
    }

    /**
     * 设置 Cookie（支持回调）
     *
     * @param url      目标 URL（兼容参数，当前实现忽略）
     * @param cookie   Cookie 对象或名称
     * @param callback 回调函数
     */
    public void set(String url, Value cookie, Value callback) {
        try {
            if (cookie.isString()) {
                // 如果第二个参数是字符串，则假定它是 cookie 名称，需要第三个参数是值
                throw new IllegalArgumentException("set() requires a cookie object, not a string");
            } else if (cookie.hasMembers()) {
                cookieApi.set(cookie);
            }
            invokeCallback(callback, null);
        } catch (Exception e) {
            invokeCallback(callback, e.getMessage());
        }
    }

    /**
     * 设置 Cookie（3 参数版本：url, name, value）
     *
     * @param url      目标 URL
     * @param name     Cookie 名称
     * @param value    Cookie 值
     */
    public void set(String url, String name, String value) {
        cookieApi.set(name, value);
    }

    /**
     * 设置 Cookie（4 参数版本：url, name, value, callback）
     *
     * @param url      目标 URL
     * @param name     Cookie 名称
     * @param value    Cookie 值
     * @param callback 回调函数
     */
    public void set(String url, String name, String value, Value callback) {
        try {
            cookieApi.set(name, value);
            invokeCallback(callback, null);
        } catch (Exception e) {
            invokeCallback(callback, e.getMessage());
        }
    }

    /**
     * 获取 Cookie（支持回调）
     *
     * @param url      目标 URL（兼容参数，当前实现忽略）
     * @param name     Cookie 名称
     * @param callback 回调函数，接收 (error, cookie) 参数
     */
    public void get(String url, String name, Value callback) {
        try {
            Cookie cookie = cookieApi.get(name);
            invokeCallback(callback, null, cookie);
        } catch (Exception e) {
            invokeCallback(callback, e.getMessage(), null);
        }
    }

    /**
     * 删除 Cookie（支持回调）
     *
     * @param url      目标 URL（兼容参数，当前实现忽略）
     * @param name     Cookie 名称
     * @param callback 回调函数
     */
    public void unset(String url, String name, Value callback) {
        try {
            cookieApi.delete(name);
            invokeCallback(callback, null);
        } catch (Exception e) {
            invokeCallback(callback, e.getMessage());
        }
    }

    /**
     * 获取所有 Cookie（支持回调）
     *
     * @param url      目标 URL（兼容参数，当前实现忽略）
     * @param callback 回调函数，接收 (error, cookies) 参数
     */
    public void getAll(String url, Value callback) {
        try {
            Cookie[] cookies = cookieApi.toArray();
            invokeCallback(callback, null, cookies);
        } catch (Exception e) {
            invokeCallback(callback, e.getMessage(), null);
        }
    }

    /**
     * 清空所有 Cookie（支持回调）
     *
     * @param url      目标 URL（兼容参数，当前实现忽略）
     * @param callback 回调函数
     */
    public void clear(String url, Value callback) {
        try {
            cookieApi.clear();
            invokeCallback(callback, null);
        } catch (Exception e) {
            invokeCallback(callback, e.getMessage());
        }
    }

    /**
     * 调用回调函数（单参数：error）
     *
     * @param callback 回调函数
     * @param error    错误信息，null 表示成功
     */
    private void invokeCallback(Value callback, String error) {
        if (callback != null && callback.canExecute()) {
            callback.executeVoid(error);
        }
    }

    /**
     * 调用回调函数（双参数：error, result）
     *
     * @param callback 回调函数
     * @param error    错误信息，null 表示成功
     * @param result   结果数据
     */
    private void invokeCallback(Value callback, String error, Object result) {
        if (callback != null && callback.canExecute()) {
            callback.executeVoid(error, result);
        }
    }
}

