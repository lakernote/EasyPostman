package com.laker.postman.model.script;

/**
 * Cookie 数据对象
 * <p>
 * 表示一个 HTTP Cookie，包含所有标准 Cookie 属性。
 * </p>
 */
public class Cookie {
    /**
     * Cookie 名称
     */
    public String name;

    /**
     * Cookie 值
     */
    public String value;

    /**
     * Cookie 所属域名
     */
    public String domain;

    /**
     * Cookie 路径
     */
    public String path;

    /**
     * Cookie 过期时间
     */
    public String expires;

    /**
     * Cookie 最大生存时间（秒）
     */
    public Integer maxAge;

    /**
     * 是否仅在 HTTPS 连接中发送
     */
    public Boolean secure;

    /**
     * 是否禁止 JavaScript 访问
     */
    public Boolean httpOnly;

    /**
     * SameSite 属性 (Strict/Lax/None)
     */
    public String sameSite;

    @Override
    public String toString() {
        return String.format("Cookie{name='%s', value='%s', domain='%s', path='%s'}",
                name, value, domain, path);
    }
}