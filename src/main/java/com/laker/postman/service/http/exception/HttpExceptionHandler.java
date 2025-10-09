package com.laker.postman.service.http.exception;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * HTTP异常处理工具类
 * 职责：统一处理和解析HTTP请求过程中的各种异常
 */
@Slf4j
public class HttpExceptionHandler {

    /**
     * HTTP异常类型枚举
     */
    public enum HttpErrorType {
        CONNECTION_TIMEOUT("Connection timeout"),
        READ_TIMEOUT("Read timeout"),
        UNKNOWN_HOST("Unknown host"),
        CONNECTION_REFUSED("Connection refused"),
        SSL_ERROR("SSL/TLS error"),
        NETWORK_ERROR("Network error"),
        CANCELLED("Request cancelled"),
        UNKNOWN("Unknown error");

        private final String description;

        HttpErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 解析异常并返回用户友好的错误信息
     */
    public static String getUserFriendlyMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error occurred";
        }

        HttpErrorType errorType = parseErrorType(throwable);
        String baseMessage = errorType.getDescription();
        String detailMessage = extractDetailMessage(throwable);

        if (detailMessage != null && !detailMessage.isEmpty()) {
            return baseMessage + ": " + detailMessage;
        }

        return baseMessage;
    }

    /**
     * 解析异常类型
     */
    public static HttpErrorType parseErrorType(Throwable throwable) {
        if (throwable == null) {
            return HttpErrorType.UNKNOWN;
        }

        // SSL相关异常
        if (throwable instanceof SSLHandshakeException ||
            throwable instanceof SSLPeerUnverifiedException ||
            throwable instanceof SSLException) {
            return HttpErrorType.SSL_ERROR;
        }

        // 连接超时
        if (throwable instanceof SocketTimeoutException) {
            String message = throwable.getMessage();
            if (message != null && message.toLowerCase().contains("connect")) {
                return HttpErrorType.CONNECTION_TIMEOUT;
            }
            return HttpErrorType.READ_TIMEOUT;
        }

        // 主机名解析失败
        if (throwable instanceof UnknownHostException) {
            return HttpErrorType.UNKNOWN_HOST;
        }

        // 连接被拒绝
        if (throwable instanceof ConnectException) {
            return HttpErrorType.CONNECTION_REFUSED;
        }

        // 请求被取消
        if (throwable instanceof IOException &&
            throwable.getMessage() != null &&
            throwable.getMessage().toLowerCase().contains("cancel")) {
            return HttpErrorType.CANCELLED;
        }

        // 其他网络错误
        if (throwable instanceof IOException) {
            return HttpErrorType.NETWORK_ERROR;
        }

        // 检查原因链
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            return parseErrorType(cause);
        }

        return HttpErrorType.UNKNOWN;
    }

    /**
     * 提取详细错误信息
     */
    private static String extractDetailMessage(Throwable throwable) {
        String message = throwable.getMessage();

        if (message == null || message.isEmpty()) {
            Throwable cause = throwable.getCause();
            if (cause != null && cause != throwable) {
                return extractDetailMessage(cause);
            }
            return throwable.getClass().getSimpleName();
        }

        // 清理一些技术性的堆栈信息
        message = cleanupTechnicalDetails(message);

        return message;
    }

    /**
     * 清理技术细节，返回更易读的信息
     */
    private static String cleanupTechnicalDetails(String message) {
        if (message == null) {
            return null;
        }

        // 移除常见的技术前缀
        message = message.replaceAll("^java\\..*?:\\s*", "");
        message = message.replaceAll("^javax\\..*?:\\s*", "");

        // 限制长度
        if (message.length() > 200) {
            message = message.substring(0, 197) + "...";
        }

        return message;
    }

    /**
     * 判断是否为可重试的错误
     */
    public static boolean isRetryable(Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        HttpErrorType errorType = parseErrorType(throwable);

        switch (errorType) {
            case CONNECTION_TIMEOUT:
            case READ_TIMEOUT:
            case NETWORK_ERROR:
                return true;
            case SSL_ERROR:
            case UNKNOWN_HOST:
            case CONNECTION_REFUSED:
            case CANCELLED:
            default:
                return false;
        }
    }

    /**
     * 记录异常到日志
     */
    public static void logException(String context, Throwable throwable) {
        HttpErrorType errorType = parseErrorType(throwable);
        String message = getUserFriendlyMessage(throwable);

        log.error("[{}] {} - {}", context, errorType, message);

        // 只在需要详细信息时才输出完整堆栈
        if (log.isDebugEnabled()) {
            log.debug("Full exception details:", throwable);
        }
    }
}

