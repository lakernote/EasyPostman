package com.laker.postman.service.http.ssl;

import lombok.extern.slf4j.Slf4j;

import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * SSL 证书验证工具类
 * 职责：证书有效性检查和主机名匹配验证
 */
@Slf4j
public class SSLCertificateValidator {

    /**
     * 验证证书链并返回验证结果
     * @param certificates 证书链
     * @param hostname 目标主机名
     * @return 验证结果对象
     */
    public static SSLValidationResult validateCertificates(List<Certificate> certificates, String hostname) {
        if (certificates == null || certificates.isEmpty()) {
            return SSLValidationResult.success();
        }

        Certificate cert = certificates.get(0);
        if (!(cert instanceof X509Certificate)) {
            return SSLValidationResult.success();
        }

        X509Certificate x509 = (X509Certificate) cert;
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // 1. 检查证书有效期
        validateCertificateValidity(x509, errors);

        // 2. 检查证书即将过期（30天内）
        checkCertificateExpiringSoon(x509, warnings);

        // 3. 检查自签名证书
        if (isSelfSigned(x509)) {
            warnings.add("Self-signed certificate detected");
        }

        // 4. 检查主机名匹配
        if (hostname != null && !hostname.isEmpty()) {
            if (!isHostnameMatch(x509, hostname)) {
                errors.add("Hostname mismatch: certificate does not match '" + hostname + "'");
            }
        }

        // 5. 检查证书链完整性
        if (certificates.size() == 1 && !isSelfSigned(x509)) {
            warnings.add("Incomplete certificate chain");
        }

        if (!errors.isEmpty()) {
            return warnings.isEmpty() ?
                   SSLValidationResult.failure(errors) :
                   SSLValidationResult.failureWithWarnings(errors, warnings);
        }

        return warnings.isEmpty() ?
               SSLValidationResult.success() :
               SSLValidationResult.successWithWarnings(warnings);
    }

    /**
     * 验证证书有效期
     */
    private static void validateCertificateValidity(X509Certificate cert,
                                                    List<String> errors) {
        try {
            cert.checkValidity();
        } catch (CertificateExpiredException e) {
            errors.add("Certificate expired on " + cert.getNotAfter());
        } catch (CertificateNotYetValidException e) {
            errors.add("Certificate not yet valid (valid from " + cert.getNotBefore() + ")");
        }
    }

    /**
     * 检查证书是否即将过期
     */
    private static void checkCertificateExpiringSoon(X509Certificate cert, List<String> warnings) {
        Date now = new Date();
        Date expiryDate = cert.getNotAfter();
        long daysUntilExpiry = (expiryDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);

        if (daysUntilExpiry > 0 && daysUntilExpiry <= 30) {
            warnings.add("Certificate expires soon (in " + daysUntilExpiry + " days)");
        }
    }

    /**
     * 检查证书是否为自签名证书
     */
    private static boolean isSelfSigned(X509Certificate cert) {
        try {
            return cert.getIssuerX500Principal().equals(cert.getSubjectX500Principal());
        } catch (Exception e) {
            log.debug("Error checking self-signed status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查主机名是否与证书匹配（公共方法）
     */
    public static boolean isHostnameMatchPublic(X509Certificate cert, String hostname) {
        return isHostnameMatch(cert, hostname);
    }

    /**
     * 检查主机名是否与证书匹配
     */
    private static boolean isHostnameMatch(X509Certificate cert, String hostname) {
        try {
            // 1. 检查 CN (Common Name)
            String dn = cert.getSubjectX500Principal().getName();
            String cn = extractCN(dn);
            if (cn != null && hostnameMatches(hostname, cn)) {
                return true;
            }

            // 2. 检查 Subject Alternative Names (SAN)
            var altNames = cert.getSubjectAlternativeNames();
            if (altNames != null) {
                for (List<?> altName : altNames) {
                    if (altName.size() >= 2) {
                        Integer type = (Integer) altName.get(0);
                        // Type 2 is dNSName
                        if (type == 2) {
                            String dnsName = (String) altName.get(1);
                            if (hostnameMatches(hostname, dnsName)) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        } catch (Exception e) {
            log.debug("Error checking hostname match: {}", e.getMessage());
            return true; // 无法检查时假设匹配，避免误报
        }
    }

    /**
     * 从 DN 字符串中提取 CN
     */
    private static String extractCN(String dn) {
        if (dn == null) return null;

        String[] parts = dn.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("CN=")) {
                return trimmed.substring(3);
            }
        }
        return null;
    }

    /**
     * 检查主机名是否匹配（支持通配符和 www 前缀）
     */
    private static boolean hostnameMatches(String hostname, String pattern) {
        if (hostname == null || pattern == null) {
            return false;
        }

        hostname = hostname.toLowerCase();
        pattern = pattern.toLowerCase();

        // 1. 完全匹配
        if (hostname.equals(pattern)) {
            return true;
        }

        // 2. www 前缀匹配
        // 允许 csdn.com 匹配证书 CN=www.csdn.com
        // 允许 www.csdn.com 匹配证书 CN=csdn.com
        if (hostname.startsWith("www.") && pattern.equals(hostname.substring(4))) {
            return true;
        }
        if (pattern.startsWith("www.") && hostname.equals(pattern.substring(4))) {
            return true;
        }

        // 3. 通配符匹配 (*.example.com)
        if (pattern.startsWith("*.")) {
            String patternSuffix = pattern.substring(1); // .example.com

            if (!hostname.endsWith(patternSuffix)) {
                return false;
            }

            // 通配符只匹配一级子域名
            String prefix = hostname.substring(0, hostname.length() - patternSuffix.length());

            // prefix 不能为空（排除 example.com 匹配 *.example.com）
            if (prefix.isEmpty()) {
                return false;
            }

            // prefix 不能包含点号（排除多级子域名）
            return !prefix.contains(".");
        }

        return false;
    }
}
