package com.laker.postman.service.http.ssl;

/**
 * SSL相关异常的统一封装
 */
public class SSLException extends RuntimeException {

    private final SSLErrorType errorType;

    public SSLException(String message, SSLErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public SSLException(String message, Throwable cause, SSLErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }

    public SSLErrorType getErrorType() {
        return errorType;
    }

    /**
     * SSL错误类型枚举
     */
    public enum SSLErrorType {
        /** 证书过期 */
        CERTIFICATE_EXPIRED("Certificate expired"),
        /** 证书未生效 */
        CERTIFICATE_NOT_YET_VALID("Certificate not yet valid"),
        /** 不受信任的证书 */
        UNTRUSTED_CERTIFICATE("Untrusted certificate"),
        /** 主机名不匹配 */
        HOSTNAME_MISMATCH("Hostname mismatch"),
        /** 证书链不完整 */
        INCOMPLETE_CHAIN("Incomplete certificate chain"),
        /** 自签名证书 */
        SELF_SIGNED_CERTIFICATE("Self-signed certificate"),
        /** 证书路径验证失败 */
        PATH_VALIDATION_FAILED("Certificate path validation failed"),
        /** SSL配置错误 */
        CONFIGURATION_ERROR("SSL configuration error"),
        /** 未知SSL错误 */
        UNKNOWN("Unknown SSL error");

        private final String description;

        SSLErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}

