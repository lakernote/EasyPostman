package com.laker.postman.service.http.ssl;

import lombok.extern.slf4j.Slf4j;

import java.security.cert.CertificateException;

/**
 * 证书错误信息解析器
 * 负责将各种证书异常转换为易读的错误信息
 */
@Slf4j
public class CertificateErrorParser {

    private CertificateErrorParser() {
        // 工具类，隐藏构造函数
    }

    /**
     * 解析证书异常，返回错误类型
     */
    public static SSLException.SSLErrorType parseErrorType(CertificateException e) {
        String message = e.getMessage();
        if (message == null) {
            return SSLException.SSLErrorType.UNKNOWN;
        }

        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("certificate expired") || lowerMessage.contains("notafter")) {
            return SSLException.SSLErrorType.CERTIFICATE_EXPIRED;
        } else if (lowerMessage.contains("notbefore") || lowerMessage.contains("not yet valid")) {
            return SSLException.SSLErrorType.CERTIFICATE_NOT_YET_VALID;
        } else if (lowerMessage.contains("unable to find valid certification path") ||
                   lowerMessage.contains("pkix path building failed")) {
            return SSLException.SSLErrorType.UNTRUSTED_CERTIFICATE;
        } else if (lowerMessage.contains("pkix path validation failed")) {
            return SSLException.SSLErrorType.PATH_VALIDATION_FAILED;
        }

        return SSLException.SSLErrorType.UNKNOWN;
    }

    /**
     * 提取用户友好的错误信息
     */
    public static String extractUserFriendlyMessage(CertificateException e) {
        SSLException.SSLErrorType errorType = parseErrorType(e);

        return switch (errorType) {
            case CERTIFICATE_EXPIRED -> "Certificate has expired";
            case CERTIFICATE_NOT_YET_VALID -> "Certificate is not yet valid";
            case UNTRUSTED_CERTIFICATE -> "Certificate is not trusted (untrusted root certificate)";
            case PATH_VALIDATION_FAILED -> "Certificate path validation failed";
            default -> {
                String message = e.getMessage();
                yield message != null ? message : "Certificate validation failed";
            }
        };
    }
}
