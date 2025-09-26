package com.laker.postman.service.http.ssl;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import javax.net.ssl.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * SSL配置工具类，用于处理代理环境下的SSL证书验证问题
 */
@Slf4j
public class SSLConfigurationUtil {

    /**
     * 创建一个信任所有证书的TrustManager
     * 注意：这会禁用SSL证书验证，仅在代理环境下使用
     */
    public static X509TrustManager createTrustAllManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                // 信任所有客户端证书
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                // 信任所有服务器证书
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

    /**
     * 创建一个信任所有主机名的HostnameVerifier
     * 注意：这会禁用主机名验证，仅在代理环境下使用
     */
    public static HostnameVerifier createTrustAllHostnameVerifier() {
        return (hostname, session) -> true;
    }

    /**
     * 为OkHttpClient.Builder配置SSL，支持禁用证书验证
     */
    public static void configureSSL(OkHttpClient.Builder builder, boolean disableVerification) {
        if (!disableVerification) {
            // 使用默认的SSL配置
            return;
        }

        try {
            // 创建信任所有证书的TrustManager
            X509TrustManager trustAllManager = createTrustAllManager();

            // 创建SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAllManager}, new java.security.SecureRandom());

            // 配置OkHttpClient
            builder.sslSocketFactory(sslContext.getSocketFactory(), trustAllManager);
            builder.hostnameVerifier(createTrustAllHostnameVerifier());

            log.warn("SSL certificate verification has been disabled. This should only be used in proxy environments.");

        } catch (Exception e) {
            log.error("Failed to configure SSL settings", e);
            throw new RuntimeException("Failed to configure SSL settings", e);
        }
    }

    /**
     * 创建一个混合的TrustManager，在代理环境下更宽松地处理证书验证
     * 这是一个更安全的替代方案，相比完全禁用证书验证
     */
    public static X509TrustManager createProxyFriendlyTrustManager() {
        try {
            // 获取默认的TrustManager
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((java.security.KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

            X509TrustManager defaultTrustManager = null;
            for (TrustManager tm : trustManagers) {
                if (tm instanceof X509TrustManager) {
                    defaultTrustManager = (X509TrustManager) tm;
                    break;
                }
            }

            final X509TrustManager finalDefaultTrustManager = defaultTrustManager;

            return new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    if (finalDefaultTrustManager != null) {
                        try {
                            finalDefaultTrustManager.checkClientTrusted(chain, authType);
                        } catch (CertificateException e) {
                            log.warn("Client certificate validation failed, but allowing due to proxy configuration: {}", e.getMessage());
                            // 在代理环境下允许通过
                        }
                    }
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    if (finalDefaultTrustManager != null) {
                        try {
                            finalDefaultTrustManager.checkServerTrusted(chain, authType);
                        } catch (CertificateException e) {
                            log.warn("Server certificate validation failed, but allowing due to proxy configuration: {}", e.getMessage());
                            // 在代理环境下允许通过
                        }
                    }
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return finalDefaultTrustManager != null ? finalDefaultTrustManager.getAcceptedIssuers() : new X509Certificate[0];
                }
            };

        } catch (Exception e) {
            log.error("Failed to create proxy-friendly trust manager, falling back to trust-all", e);
            return createTrustAllManager();
        }
    }

    /**
     * 为OkHttpClient.Builder配置代理友好的SSL设置
     * 这是一个更安全的选项，相比完全禁用证书验证
     */
    public static void configureProxyFriendlySSL(OkHttpClient.Builder builder) {
        try {
            X509TrustManager proxyFriendlyTrustManager = createProxyFriendlyTrustManager();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{proxyFriendlyTrustManager}, new java.security.SecureRandom());

            builder.sslSocketFactory(sslContext.getSocketFactory(), proxyFriendlyTrustManager);

            log.info("Configured proxy-friendly SSL settings");

        } catch (Exception e) {
            log.error("Failed to configure proxy-friendly SSL settings", e);
            throw new RuntimeException("Failed to configure proxy-friendly SSL settings", e);
        }
    }
}
