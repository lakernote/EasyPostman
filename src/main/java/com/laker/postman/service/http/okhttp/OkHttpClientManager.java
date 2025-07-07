package com.laker.postman.service.http.okhttp;

import com.laker.postman.common.setting.SettingManager;
import okhttp3.ConnectionPool;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * OkHttpClient 管理器，按 baseUri（协议+host+port）分配连接池和 OkHttpClient
 * 连接池参数参考 Chrome：每 host 6 个连接，保活 90 秒
 */
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
        clientMap.clear(); // 清空，确保新参数生效
    }

    /**
     * 动态设置连接池参数（压测时可调大）
     */
    public static void setDefaultConnectionPoolConfig() {
        maxIdleConnections = MAX_IDLE_CONNECTIONS;
        keepAliveDuration = KEEP_ALIVE_DURATION;
        clientMap.clear(); // 清空，确保新参数生效
    }


    /**
     * 获取或创建指定 baseUri 的 OkHttpClient 实例。
     * <p>
     * 1. 按 baseUri（协议+host+port）分配连接池，最大空闲连接数和保活时间参考 Chrome。
     * 2. 支持 followRedirects 配置。
     * 3. 支持企业级常用配置，如连接池、超时、重试、代理、拦截器、SSL、DNS、CookieJar 等扩展点。
     * </p>
     *
     * @param baseUri 协议+host+port
     * @return OkHttpClient
     */
    public static OkHttpClient getClient(String baseUri, boolean followRedirects) {
        int timeoutMs = SettingManager.getRequestTimeout();
        String key = baseUri + "|" + followRedirects;
        return clientMap.computeIfAbsent(key, k -> {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    // 连接超时
                    .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    // 读超时
                    .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    // 写超时
                    .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
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

            return builder.build();
        });
    }

    public static CookieManager getGlobalCookieManager() {
        return GLOBAL_COOKIE_MANAGER;
    }
}