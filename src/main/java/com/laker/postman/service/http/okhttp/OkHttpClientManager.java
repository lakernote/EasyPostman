package com.laker.postman.service.http.okhttp;

import com.laker.postman.common.setting.SettingManager;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * OkHttpClient 管理器，按 baseUri（协议+host+port）分配连接池和 OkHttpClient
 * 连接池参数参考 Chrome：每 host 6 个连接，保活 90 秒
 */
@Slf4j
public class OkHttpClientManager {
    // 每个 baseUri 一个连接池和 OkHttpClient
    private static final Map<String, OkHttpClient> clientMap = new ConcurrentHashMap<>();
    // 连接池参数
    private static final int MAX_IDLE_CONNECTIONS = 6;
    private static final long KEEP_ALIVE_DURATION = 90L;

    // 连接池参数（可动态调整）
    private static volatile int maxIdleConnections = MAX_IDLE_CONNECTIONS;
    private static volatile long keepAliveDuration = KEEP_ALIVE_DURATION;

    // 全局 CookieManager，支持标准 CookiePolicy
    private static final CookieManager GLOBAL_COOKIE_MANAGER = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private static final JavaNetCookieJar GLOBAL_COOKIE_JAR = new JavaNetCookieJar(GLOBAL_COOKIE_MANAGER);

    /**
     * 动态设置连接池参数（压测时可调大）
     */
    public static void setConnectionPoolConfig(int maxIdle, long keepAliveSeconds) {
        maxIdleConnections = maxIdle;
        keepAliveDuration = keepAliveSeconds;
        // 主动关闭所有旧连接，避免资源泄漏
        for (OkHttpClient client : clientMap.values()) {
            client.connectionPool().evictAll(); // 清空旧连接池
        }
        clientMap.clear(); // 清空，确保新参数生效
    }

    /**
     * 动态设置连接池参数（压测时可调大）
     */
    public static void setDefaultConnectionPoolConfig() {
        maxIdleConnections = MAX_IDLE_CONNECTIONS;
        keepAliveDuration = KEEP_ALIVE_DURATION;
        // 主动关闭所有旧连接，避免资源泄漏
        for (OkHttpClient client : clientMap.values()) {
            client.connectionPool().evictAll(); // 清空旧连接池
        }
        clientMap.clear(); // 清空，确保新参数生效
    }

    /**
     * 清理所有客户端缓存，用于代理设置更改后强制重新创建客户端
     */
    public static void clearClientCache() {
        for (OkHttpClient client : clientMap.values()) {
            client.connectionPool().evictAll();
        }
        clientMap.clear();
    }

    /**
     * 获取或创建指定 baseUri 的 OkHttpClient 实例。
     * <p>
     * 1. 按 baseUri（协议+host+port）分配连接池，最大空闲连接数和保活时间参考 Chrome。
     * 2. 支持 followRedirects 配置。
     * 3. 支持企业级常用配置，如连接池、超时、重试、代理、拦截器、SSL、DNS、CookieJar 等扩展点。
     * 4. 支持网络代理配置。
     * </p>
     *
     * @param baseUri 协议+host+port
     * @return OkHttpClient
     */
    public static OkHttpClient getClient(String baseUri, boolean followRedirects) {
        // 将代理配置也作为客户端缓存key的一部分，确保代理设置变更时重新创建客户端
        String proxyKey = getProxyConfigKey();
        String key = baseUri + "|" + followRedirects + "|" + proxyKey;

        return clientMap.computeIfAbsent(key, k -> {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    // 连接超时
                    .connectTimeout(0, TimeUnit.MILLISECONDS) // 设置为 0 表示不超时
                    // 读超时
                    .readTimeout(0, TimeUnit.MILLISECONDS) // 设置为 0 表示不超时
                    // 写超时
                    .writeTimeout(0, TimeUnit.MILLISECONDS) // 设置为 0 表示不超时
                    // 连接池配置
                    .connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.SECONDS))
                    // 失败自动重试
                    .retryOnConnectionFailure(true)
                    // 是否自动跟随重定向
                    .followRedirects(followRedirects)
                    .cache(null)
                    .pingInterval(30, TimeUnit.SECONDS);

            // 使用全局 JavaNetCookieJar，支持完整 Cookie 规范
            builder.cookieJar(GLOBAL_COOKIE_JAR);

            // 配置网络代理
            configureProxy(builder);

            return builder.build();
        });
    }

    /**
     * 配置网络代理
     */
    private static void configureProxy(OkHttpClient.Builder builder) {
        if (!SettingManager.isProxyEnabled()) {
            return; // 代理未启用，使用默认配置
        }

        String proxyHost = SettingManager.getProxyHost();
        int proxyPort = SettingManager.getProxyPort();
        String proxyType = SettingManager.getProxyType();
        String proxyUsername = SettingManager.getProxyUsername();
        String proxyPassword = SettingManager.getProxyPassword();

        if (proxyHost.trim().isEmpty()) {
            return; // 代理主机地址为空，跳过代理配置
        }

        try {
            // 创建代理对象
            Proxy proxy;
            if ("SOCKS".equalsIgnoreCase(proxyType)) {
                proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost.trim(), proxyPort));
            } else {
                // 默认使用HTTP代理
                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost.trim(), proxyPort));
            }

            builder.proxy(proxy);

            // 配置代理认证（如果提供了用户名和密码）
            if (!proxyUsername.trim().isEmpty() && !proxyPassword.trim().isEmpty()) {

                Authenticator proxyAuthenticator = (route, response) -> {
                    String credential = Credentials.basic(proxyUsername.trim(), proxyPassword);
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                };

                builder.proxyAuthenticator(proxyAuthenticator);
            }

        } catch (Exception e) {
            // 代理配置失败时记录错误，但不影响客户端创建
            log.error("Failed to configure proxy: {}", e.getMessage(), e);
        }
    }

    /**
     * 生成代理配置的key，用于客户端缓存
     */
    private static String getProxyConfigKey() {
        if (!SettingManager.isProxyEnabled()) {
            return "no-proxy";
        }

        return String.format("proxy:%s:%s:%d:%s",
                SettingManager.getProxyType(),
                SettingManager.getProxyHost(),
                SettingManager.getProxyPort(),
                SettingManager.getProxyUsername());
    }

    public static CookieManager getGlobalCookieManager() {
        return GLOBAL_COOKIE_MANAGER;
    }
}