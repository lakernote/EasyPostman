package com.laker.postman.service.http.okhttp;

import com.laker.postman.model.TrustedCertificateEntry;
import com.laker.postman.service.http.ssl.SSLConfigurationUtil;
import com.laker.postman.service.setting.SettingManager;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.Authenticator;

import java.net.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.laker.postman.service.http.HttpRequestUtil.extractBaseUri;

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

    // Dispatcher 并发参数（OkHttp默认：maxRequests=64, maxRequestsPerHost=5）
    private static final int DEFAULT_MAX_REQUESTS = 64;
    private static final int DEFAULT_MAX_REQUESTS_PER_HOST = 5;

    // 连接池参数（可动态调整）
    private static volatile int maxIdleConnections = MAX_IDLE_CONNECTIONS;
    private static volatile long keepAliveDuration = KEEP_ALIVE_DURATION;

    // Dispatcher 并发参数（可动态调整，压测时需调大）
    private static volatile int maxRequests = DEFAULT_MAX_REQUESTS;
    private static volatile int maxRequestsPerHost = DEFAULT_MAX_REQUESTS_PER_HOST;

    // 全局 CookieManager，支持标准 CookiePolicy
    private static final CookieManager GLOBAL_COOKIE_MANAGER = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private static final JavaNetCookieJar GLOBAL_COOKIE_JAR = new JavaNetCookieJar(GLOBAL_COOKIE_MANAGER);

    public record ProxyInspection(boolean resolved, Proxy proxy) {
        public boolean active() {
            return resolved && !isDirectProxy(proxy);
        }

        public String description() {
            return OkHttpClientManager.describeProxy(proxy);
        }
    }

    /**
     * 动态设置连接池参数（压测时可调大）
     */
    public static void setConnectionPoolConfig(int maxIdle, long keepAliveSeconds) {
        maxIdleConnections = maxIdle;
        keepAliveDuration = keepAliveSeconds;
        clearClientCache();
    }

    /**
     * 动态设置 Dispatcher 并发参数（压测时可调大）
     * @param maxReq 最大并发请求数（所有主机总和）
     * @param maxReqPerHost 单个主机最大并发请求数
     */
    public static void setDispatcherConfig(int maxReq, int maxReqPerHost) {
        maxRequests = maxReq;
        maxRequestsPerHost = maxReqPerHost;
        clearClientCache();
    }

    /**
     * 动态设置连接池参数（压测时可调大）
     */
    public static void setDefaultConnectionPoolConfig() {
        maxIdleConnections = MAX_IDLE_CONNECTIONS;
        keepAliveDuration = KEEP_ALIVE_DURATION;
        maxRequests = DEFAULT_MAX_REQUESTS;
        maxRequestsPerHost = DEFAULT_MAX_REQUESTS_PER_HOST;
        clearClientCache();
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
     * 取消所有正在执行和排队的 HTTP 请求
     * 用于性能测试停止时快速中断所有网络请求
     */
    public static void cancelAllCalls() {
        int totalRunningCount = 0;
        int totalQueuedCount = 0;

        for (Map.Entry<String, OkHttpClient> entry : clientMap.entrySet()) {
            String clientKey = entry.getKey();
            OkHttpClient client = entry.getValue();

            int clientRunningCount = 0;
            int clientQueuedCount = 0;

            // 取消所有正在执行的请求
            for (okhttp3.Call call : client.dispatcher().runningCalls()) {
                call.cancel();
                clientRunningCount++;
            }

            // 取消所有排队的请求
            for (okhttp3.Call call : client.dispatcher().queuedCalls()) {
                call.cancel();
                clientQueuedCount++;
            }

            // 只在有请求被取消时才打印该客户端的日志
            if (clientRunningCount > 0 || clientQueuedCount > 0) {
                // 提取可读的 baseUri（去掉 followRedirects 和 proxy 配置信息）
                String baseUri = clientKey.split("\\|")[0];
                log.info("Client [{}] 取消了 {} 个正在执行的请求和 {} 个排队的请求",
                        baseUri, clientRunningCount, clientQueuedCount);
            }

            totalRunningCount += clientRunningCount;
            totalQueuedCount += clientQueuedCount;
        }

        log.info("总共取消了 {} 个正在执行的请求和 {} 个排队的请求", totalRunningCount, totalQueuedCount);
    }

    /**
     * 获取或创建指定 baseUri 的 OkHttpClient 实例
     */
    public static OkHttpClient getClient(String baseUri, boolean followRedirects) {
        // 将代理配置也作为客户端缓存key的一部分，确保代理设置变更时重新创建客户端
        String proxyKey = getProxyConfigKey(baseUri);
        String key = baseUri + "|" + followRedirects + "|" + proxyKey;

        return clientMap.computeIfAbsent(key, k -> createClient(
                baseUri,
                followRedirects,
                resolveSslVerificationMode(baseUri)
        ));
    }

    /**
     * 创建一个不参与缓存的客户端，用于请求级 SSL 模式覆盖。
     */
    public static OkHttpClient createClientForSslMode(String baseUri,
                                                      boolean followRedirects,
                                                      SSLConfigurationUtil.SSLVerificationMode sslMode) {
        Dispatcher sharedDispatcher = getClient(baseUri, followRedirects).dispatcher();
        return createClient(baseUri, followRedirects, sslMode, sharedDispatcher);
    }

    /**
     * 基于目标 URL 获取已应用全局代理/SSL 配置的客户端，并按需覆盖超时参数。
     */
    public static OkHttpClient getClientForUrl(String url,
                                               boolean followRedirects,
                                               int connectTimeoutMs,
                                               int readTimeoutMs,
                                               int writeTimeoutMs) {
        OkHttpClient baseClient = getClient(extractBaseUri(url), followRedirects);
        return baseClient.newBuilder()
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * 配置网络代理
     */
    private static void configureProxy(OkHttpClient.Builder builder, String baseUri) {
        if (!SettingManager.isProxyEnabled()) {
            // The JVM may still expose OS proxy settings via the default ProxySelector.
            // Force direct connections when the in-app proxy toggle is off.
            builder.proxy(Proxy.NO_PROXY);
            return;
        }

        if (SettingManager.isSystemProxyMode()) {
            configureSystemProxy(builder);
            return;
        }

        try {
            Proxy proxy = createManualProxy();
            if (proxy == null) {
                return;
            }

            builder.proxy(proxy);
            configureProxyAuthenticator(builder);
        } catch (Exception e) {
            log.error("Failed to configure proxy for {}: {}", baseUri, e.getMessage(), e);
        }
    }

    private static Proxy createManualProxy() {
        String proxyHost = normalizedProxyHost();
        if (proxyHost.isEmpty()) {
            return null;
        }

        int proxyPort = SettingManager.getProxyPort();
        String proxyType = SettingManager.getProxyType();
        Proxy.Type type = SettingManager.PROXY_TYPE_SOCKS.equalsIgnoreCase(proxyType) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
        return new Proxy(type, new InetSocketAddress(proxyHost, proxyPort));
    }

    private static void configureSystemProxy(OkHttpClient.Builder builder) {
        ProxySelector selector = getSystemProxySelector();
        if (selector == null) {
            log.debug("System proxy auto-detection is unavailable, using direct connection");
            return;
        }

        builder.proxySelector(selector);
        configureProxyAuthenticator(builder);
    }

    private static void configureProxyAuthenticator(OkHttpClient.Builder builder) {
        String proxyUsername = SettingManager.getProxyUsername();
        String proxyPassword = SettingManager.getProxyPassword();

        if (proxyUsername.trim().isEmpty() || proxyPassword.trim().isEmpty()) {
            return;
        }

        Authenticator proxyAuthenticator = (route, response) -> {
            String credential = Credentials.basic(proxyUsername.trim(), proxyPassword);
            return response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
        };
        builder.proxyAuthenticator(proxyAuthenticator);
    }

    /**
     * 配置SSL设置（统一处理所有HTTPS连接的SSL配置）
     */
    private static OkHttpClient createClient(String baseUri,
                                             boolean followRedirects,
                                             SSLConfigurationUtil.SSLVerificationMode sslMode) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(maxRequests);
        dispatcher.setMaxRequestsPerHost(maxRequestsPerHost);
        return createClient(baseUri, followRedirects, sslMode, dispatcher);
    }

    private static OkHttpClient createClient(String baseUri,
                                             boolean followRedirects,
                                             SSLConfigurationUtil.SSLVerificationMode sslMode,
                                             Dispatcher dispatcher) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(0, TimeUnit.MILLISECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(0, TimeUnit.MILLISECONDS)
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.SECONDS))
                .retryOnConnectionFailure(true)
                .followRedirects(followRedirects)
                .cache(null)
                .pingInterval(30, TimeUnit.SECONDS);

        builder.cookieJar(GLOBAL_COOKIE_JAR);
        configureProxy(builder, baseUri);
        configureSSLSettings(builder, baseUri, sslMode);
        return builder.build();
    }

    private static void configureSSLSettings(OkHttpClient.Builder builder,
                                             String baseUri,
                                             SSLConfigurationUtil.SSLVerificationMode mode) {
        URI uri = tryParseUri(baseUri, "SSL configuration");
        if (uri == null || !isSecureScheme(uri.getScheme())) {
            return;
        }

        if (mode == SSLConfigurationUtil.SSLVerificationMode.LENIENT) {
            log.warn("SSL verification disabled by user settings");
        }

        SSLConfigurationUtil.configureSSL(builder, mode, uri.getHost(), resolveSecurePort(uri));
    }

    private static String getProxyConfigKey(String baseUri) {
        String proxyPart = SettingManager.isProxyEnabled()
                ? buildProxyConfigPart(baseUri)
                : "proxy:disabled";

        StringBuilder trustPart = new StringBuilder();
        for (TrustedCertificateEntry entry : SettingManager.getCustomTrustMaterialEntries()) {
            String trustPath = entry.getPath();
            File trustFile = (trustPath == null || trustPath.isBlank()) ? null : new File(trustPath);
            long trustLastModified = (trustFile != null && trustFile.exists()) ? trustFile.lastModified() : -1L;
            long trustFileLength = (trustFile != null && trustFile.exists()) ? trustFile.length() : -1L;
            trustPart.append(entry.isEnabled())
                    .append(':')
                    .append(trustPath)
                    .append(':')
                    .append(trustLastModified)
                    .append(':')
                    .append(trustFileLength)
                    .append(':')
                    .append(entry.getPassword() == null ? 0 : entry.getPassword().hashCode())
                    .append(';');
        }
        boolean effectiveProxySslDisabled = SettingManager.isProxyEnabled()
                && SettingManager.isProxySslVerificationDisabled();
        return String.format("%s|ssl:%b:%b|customTrust:%b:%s",
                proxyPart,
                effectiveProxySslDisabled,
                SettingManager.isRequestSslVerificationDisabled(),
                SettingManager.isCustomTrustMaterialEnabled(),
                trustPart);
    }

    private static String buildProxyConfigPart(String baseUri) {
        if (SettingManager.isSystemProxyMode()) {
            return "proxy:system:" + describeProxy(selectSystemProxy(baseUri)) + ":" + SettingManager.getProxyUsername();
        }

        return String.format("proxy:manual:%s:%s:%d:%s",
                SettingManager.getProxyType(),
                SettingManager.getProxyHost(),
                SettingManager.getProxyPort(),
                SettingManager.getProxyUsername());
    }

    private static ProxySelector getSystemProxySelector() {
        return ProxySelector.getDefault();
    }

    static Proxy selectSystemProxy(String baseUri) {
        ProxySelector selector = getSystemProxySelector();
        if (selector == null) {
            return Proxy.NO_PROXY;
        }

        URI uri = tryParseUri(baseUri, "system proxy selection");
        if (uri == null) {
            return Proxy.NO_PROXY;
        }

        List<Proxy> proxies = selectProxyCandidates(selector, uri, baseUri);
        if (proxies == null || proxies.isEmpty()) {
            return Proxy.NO_PROXY;
        }

        for (Proxy proxy : proxies) {
            if (proxy == null) {
                continue;
            }
            if (isDirectProxy(proxy)) {
                return Proxy.NO_PROXY;
            }
            if (proxy.address() instanceof InetSocketAddress) {
                return proxy;
            }
        }
        return Proxy.NO_PROXY;
    }

    public static ProxyInspection inspectSystemProxyForUrl(String url) {
        if (url == null || url.isBlank()) {
            return new ProxyInspection(false, Proxy.NO_PROXY);
        }

        String baseUri = extractBaseUriSafely(url, "system proxy inspection");
        if (baseUri == null) {
            return new ProxyInspection(false, Proxy.NO_PROXY);
        }

        return new ProxyInspection(true, selectSystemProxy(baseUri));
    }

    public static boolean isProxyActiveForUrl(String url) {
        if (!SettingManager.isProxyEnabled()) {
            return false;
        }

        String baseUri = extractBaseUriSafely(url, "proxy check");
        if (baseUri == null) {
            return false;
        }

        return isProxyActiveForBaseUri(baseUri);
    }

    private static boolean isProxyActiveForBaseUri(String baseUri) {
        if (!SettingManager.isProxyEnabled()) {
            return false;
        }
        if (!SettingManager.isSystemProxyMode()) {
            return createManualProxy() != null;
        }

        Proxy proxy = selectSystemProxy(baseUri);
        return !isDirectProxy(proxy);
    }

    private static String describeProxy(Proxy proxy) {
        if (isDirectProxy(proxy)) {
            return "direct";
        }

        SocketAddress address = proxy.address();
        if (address instanceof InetSocketAddress socketAddress) {
            String host = socketAddress.getHostString();
            return proxy.type() + ":" + host + ":" + socketAddress.getPort();
        }
        return proxy.type() + ":unknown";
    }

    public static CookieManager getGlobalCookieManager() {
        return GLOBAL_COOKIE_MANAGER;
    }

    private static SSLConfigurationUtil.SSLVerificationMode resolveSslVerificationMode(String baseUri) {
        boolean proxySslDisabled = isProxyActiveForBaseUri(baseUri)
                && SettingManager.isProxySslVerificationDisabled();
        if (SettingManager.isRequestSslVerificationDisabled() || proxySslDisabled) {
            return SSLConfigurationUtil.SSLVerificationMode.LENIENT;
        }
        return SSLConfigurationUtil.SSLVerificationMode.STRICT;
    }

    private static int resolveSecurePort(URI uri) {
        int port = uri.getPort();
        return port == -1 ? 443 : port;
    }

    private static boolean isSecureScheme(String scheme) {
        return "https".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme);
    }

    private static String normalizedProxyHost() {
        String proxyHost = SettingManager.getProxyHost();
        return proxyHost == null ? "" : proxyHost.trim();
    }

    private static URI tryParseUri(String value, String context) {
        try {
            return URI.create(value);
        } catch (Exception e) {
            log.debug("Failed to parse URI for {}: {}", context, value, e);
            return null;
        }
    }

    private static List<Proxy> selectProxyCandidates(ProxySelector selector, URI uri, String rawTarget) {
        try {
            return selector.select(uri);
        } catch (Exception e) {
            log.warn("Failed to auto-detect system proxy for {}", rawTarget, e);
            return null;
        }
    }

    private static String extractBaseUriSafely(String url, String context) {
        try {
            return extractBaseUri(url);
        } catch (Exception e) {
            log.debug("Failed to extract baseUri for {}: {}", context, url, e);
            return null;
        }
    }

    private static boolean isDirectProxy(Proxy proxy) {
        return proxy == null || proxy == Proxy.NO_PROXY || proxy.type() == Proxy.Type.DIRECT;
    }
}
