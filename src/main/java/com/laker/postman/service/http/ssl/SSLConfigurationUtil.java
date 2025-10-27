package com.laker.postman.service.http.ssl;

import com.laker.postman.model.ClientCertificate;
import com.laker.postman.service.ClientCertificateService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import javax.net.ssl.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * SSL配置工具类
 * 职责：为OkHttpClient配置SSL设置，支持严格模式和宽松模式，以及客户端证书（mTLS）
 */
@Slf4j
public class SSLConfigurationUtil {

    /**
     * SSL验证模式
     */
    public enum SSLVerificationMode {
        /**
         * 严格模式：完全验证证书
         */
        STRICT,
        /**
         * 宽松模式：验证但不阻止连接
         */
        LENIENT,
        /**
         * 信任所有：完全跳过验证（不推荐）
         */
        TRUST_ALL
    }

    // 存储最近的SSL验证结果，按线程区分
    private static final ThreadLocal<SSLValidationResult> lastValidationResult = new ThreadLocal<>();

    /**
     * 获取当前线程的SSL验证结果
     */
    public static SSLValidationResult getLastValidationResult() {
        return lastValidationResult.get();
    }

    /**
     * 清除当前线程的SSL验证结果
     */
    public static void clearValidationResult() {
        lastValidationResult.remove();
    }

    /**
     * 为OkHttpClient配置SSL设置
     *
     * @param builder             OkHttpClient构建器
     * @param disableVerification 是否禁用证书验证
     */
    public static void configureSSL(OkHttpClient.Builder builder, boolean disableVerification) {
        SSLVerificationMode mode = disableVerification ?
                SSLVerificationMode.LENIENT :
                SSLVerificationMode.STRICT;
        configureSSL(builder, mode);
    }

    /**
     * 为OkHttpClient配置SSL设置（推荐使用此方法）
     *
     * @param builder OkHttpClient构建器
     * @param mode    SSL验证模式
     */
    public static void configureSSL(OkHttpClient.Builder builder, SSLVerificationMode mode) {
        configureSSL(builder, mode, null, 0);
    }

    /**
     * 为OkHttpClient配置SSL设置，支持客户端证书（mTLS）
     *
     * @param builder OkHttpClient构建器
     * @param mode    SSL验证模式
     * @param host    目标主机名（用于匹配客户端证书）
     * @param port    目标端口（用于匹配客户端证书）
     */
    public static void configureSSL(OkHttpClient.Builder builder, SSLVerificationMode mode,
                                   String host, int port) {
        try {
            // 查找并加载匹配的客户端证书
            KeyManager[] keyManagers = loadClientCertificate(host, port);

            // 配置 TrustManager
            X509TrustManager trustManager = configureTrustManager(mode, keyManagers);

            // 严格模式且没有客户端证书：使用默认配置
            if (trustManager == null) {
                return;
            }

            // 初始化 SSL 上下文
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, new TrustManager[]{trustManager}, new java.security.SecureRandom());

            // 使用自定义的 SSLSocketFactory 来捕获证书信息
            CertificateCapturingSSLSocketFactory socketFactory =
                    new CertificateCapturingSSLSocketFactory(sslContext);

            builder.sslSocketFactory(socketFactory, trustManager);

            // 配置 HostnameVerifier
            if (mode != SSLVerificationMode.STRICT) {
                builder.hostnameVerifier(createHostnameVerifier(mode));
                log.warn("SSL verification mode set to: {}. Use with caution in production.", mode);
            }

        } catch (Exception e) {
            log.error("Failed to configure SSL settings", e);
            throw new SSLException("Failed to configure SSL settings", e,
                    SSLException.SSLErrorType.CONFIGURATION_ERROR);
        }
    }

    /**
     * 加载客户端证书
     */
    private static KeyManager[] loadClientCertificate(String host, int port) {
        if (host == null || host.isEmpty()) {
            return new KeyManager[0];
        }

        ClientCertificate clientCert = ClientCertificateService.findMatchingCertificate(host, port);
        if (clientCert == null || !ClientCertificateService.validateCertificatePaths(clientCert)) {
            return new KeyManager[0];
        }

        try {
            KeyManager[] keyManagers = ClientCertificateLoader.createKeyManagers(clientCert);
            log.info("Using client certificate for host: {} ({})", host, clientCert.getName());
            return keyManagers;
        } catch (Exception e) {
            log.error("Failed to load client certificate for host: {}", host, e);
            return new KeyManager[0];
        }
    }

    /**
     * 配置 TrustManager
     * @return TrustManager 或 null（表示使用默认配置）
     */
    private static X509TrustManager configureTrustManager(SSLVerificationMode mode, KeyManager[] keyManagers)
            throws SSLConfigurationException {
        if (mode == SSLVerificationMode.STRICT && keyManagers == null) {
            // 严格模式且没有客户端证书：使用默认配置
            return null;
        } else if (mode == SSLVerificationMode.STRICT) {
            // 严格模式但有客户端证书：需要配置 KeyManager
            return getDefaultTrustManager();
        } else {
            // 宽松模式或信任所有模式
            return createTrustManager(mode);
        }
    }

    /**
     * 创建TrustManager
     */
    private static X509TrustManager createTrustManager(SSLVerificationMode mode) {
        try {
            // 获取默认的TrustManager用于验证
            X509TrustManager defaultTrustManager = getDefaultTrustManager();

            if (mode == SSLVerificationMode.TRUST_ALL) {
                return new TrustAllManager();
            } else {
                return new LenientTrustManager(defaultTrustManager);
            }
        } catch (Exception e) {
            log.error("Failed to create TrustManager", e);
            throw new SSLException("Failed to create TrustManager", e,
                    SSLException.SSLErrorType.CONFIGURATION_ERROR);
        }
    }

    /**
     * 获取系统默认的TrustManager
     */
    private static X509TrustManager getDefaultTrustManager() throws SSLConfigurationException {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            factory.init((java.security.KeyStore) null);

            for (TrustManager tm : factory.getTrustManagers()) {
                if (tm instanceof X509TrustManager x509tm) {
                    return x509tm;
                }
            }
            throw new SSLConfigurationException("No X509TrustManager found");
        } catch (Exception e) {
            throw new SSLConfigurationException("Failed to get default TrustManager", e);
        }
    }

    /**
     * 创建HostnameVerifier
     */
    private static HostnameVerifier createHostnameVerifier(SSLVerificationMode mode) {
        if (mode == SSLVerificationMode.TRUST_ALL) {
            return (hostname, session) -> true;
        } else {
            return new LenientHostnameVerifier();
        }
    }

    /**
     * 宽松的TrustManager：先验证，记录错误，但允许连接继续
     */
    private static class LenientTrustManager implements X509TrustManager {
        private final X509TrustManager defaultTrustManager;

        public LenientTrustManager(X509TrustManager defaultTrustManager) {
            this.defaultTrustManager = defaultTrustManager;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            log.debug("Client certificate check (lenient mode)");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            if (chain == null || chain.length == 0) {
                log.warn("No certificate chain provided");
                return;
            }

            log.debug("Validating certificate chain with {} certificates", chain.length);
            X509Certificate serverCert = chain[0];
            log.debug("Server certificate: Subject={}, Issuer={}",
                    serverCert.getSubjectX500Principal().getName(),
                    serverCert.getIssuerX500Principal().getName());

            // 尝试用默认TrustManager进行真实验证
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
                lastValidationResult.remove(); // 验证通过，清除错误
                log.debug("✅ Certificate validation passed");
            } catch (CertificateException e) {
                // 验证失败，记录但不抛出异常
                String errorMessage = CertificateErrorParser.extractUserFriendlyMessage(e);

                log.warn("⚠️ Certificate validation failed (allowing anyway): {}", errorMessage);

                // 保存验证结果供后续使用
                lastValidationResult.set(SSLValidationResult.failure(
                        java.util.Collections.singletonList(errorMessage)));
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return defaultTrustManager != null ?
                    defaultTrustManager.getAcceptedIssuers() :
                    new X509Certificate[0];
        }
    }

    /**
     * 信任所有证书的TrustManager（不推荐使用）
     */
    private static class TrustAllManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            log.debug("Client certificate trusted (trust-all mode)");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            if (chain != null && chain.length > 0) {
                log.debug("Server certificate trusted (trust-all mode): {}",
                        chain[0].getSubjectX500Principal().getName());
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /**
     * 宽松的HostnameVerifier：验证主机名，但允许不匹配的连接
     */
    private static class LenientHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            try {
                java.security.cert.Certificate[] peerCertificates = session.getPeerCertificates();
                if (peerCertificates != null && peerCertificates.length > 0) {
                    java.security.cert.Certificate cert = peerCertificates[0];
                    if (cert instanceof X509Certificate x509) {
                        boolean valid = SSLCertificateValidator.isHostnameMatchPublic(x509, hostname);

                        if (!valid) {
                            log.warn("⚠️ Hostname verification failed (allowing anyway): " +
                                    "Certificate does not match hostname '{}'", hostname);

                            // 将主机名错误追加到现有验证结果
                            SSLValidationResult existing = lastValidationResult.get();
                            String hostnameError = "Hostname mismatch: certificate does not match '" + hostname + "'";

                            if (existing != null && existing.hasErrors()) {
                                java.util.List<String> errors = new java.util.ArrayList<>(existing.getErrors());
                                errors.add(hostnameError);
                                lastValidationResult.set(SSLValidationResult.failure(errors));
                            } else {
                                lastValidationResult.set(SSLValidationResult.failure(
                                        java.util.Collections.singletonList(hostnameError)));
                            }
                        } else {
                            log.debug("✅ Hostname verification passed for: {}", hostname);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Error during hostname verification: {}", e.getMessage());
            }

            // 总是返回 true，允许连接继续
            return true;
        }
    }

    /**
     * SSL配置异常
     */
    private static class SSLConfigurationException extends RuntimeException {
        public SSLConfigurationException(String message) {
            super(message);
        }

        public SSLConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
