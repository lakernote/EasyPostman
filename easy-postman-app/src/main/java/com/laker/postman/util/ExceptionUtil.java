package com.laker.postman.util;

import lombok.experimental.UtilityClass;

/**
 * 异常处理工具类
 * 提供异常检查、分析、过滤等工具方法
 */
@UtilityClass
public class ExceptionUtil {

    /**
     * 检查异常是否应该被忽略（不展示给用户）
     * <p>
     * 此方法用于过滤掉那些不影响用户操作、由第三方库内部清理过程产生的无害异常。
     * </p>
     *
     * @param throwable 要检查的异常
     * @return 如果异常应该被忽略返回true，否则返回false
     */
    public static boolean shouldIgnoreException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        // 1. 检查SSH executor shutdown错误（JGit + Apache SSHD）
        if (isSshExecutorShutdownError(throwable)) {
            return true;
        }

        // 2. 可以在这里添加更多需要忽略的异常类型
        // 例如：InterruptedException in background threads, etc.

        return false;
    }

    /**
     * 检查是否是SSH executor shutdown错误
     * <p>
     * 这些错误通常发生在Git SSH操作完成后，异步IO操作尝试使用已关闭的executor。
     * 这是Apache SSHD + JGit的已知行为，不影响实际功能。
     * </p>
     *
     * @param throwable 要检查的异常
     * @return 如果是SSH shutdown错误返回true，否则返回false
     */
    private static boolean isSshExecutorShutdownError(Throwable throwable) {
        // 检查是否是 IllegalStateException 且消息包含特定内容
        if (!(throwable instanceof IllegalStateException)) {
            return false;
        }

        String message = throwable.getMessage();
        if (message == null || !message.contains("Executor has been shut down")) {
            return false;
        }

        // 检查调用栈是否包含SSH相关的类
        return stackTraceContains(throwable,
                "org.apache.sshd",
                "NoCloseExecutor",
                "AsynchronousChannelGroup",
                "AsynchronousSocketChannel");
    }

    /**
     * 获取异常的根本原因
     *
     * @param throwable 异常对象
     * @return 根本原因异常，如果没有cause则返回原异常本身
     */
    public static Throwable getRootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * 检查异常链中是否包含特定类型的异常
     *
     * @param throwable     要检查的异常
     * @param exceptionType 目标异常类型
     * @return 如果异常链中包含指定类型的异常返回true
     */
    public static boolean containsException(Throwable throwable, Class<? extends Throwable> exceptionType) {
        if (throwable == null || exceptionType == null) {
            return false;
        }

        Throwable current = throwable;
        while (current != null) {
            if (exceptionType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
            // 防止循环引用
            if (current == throwable) {
                break;
            }
        }
        return false;
    }

    /**
     * 检查异常的调用栈中是否包含指定的类名片段
     *
     * @param throwable      要检查的异常
     * @param classNameParts 类名片段（可变参数）
     * @return 如果调用栈中包含任一指定的类名片段返回true
     */
    private static boolean stackTraceContains(Throwable throwable, String... classNameParts) {
        if (throwable == null || classNameParts == null || classNameParts.length == 0) {
            return false;
        }

        for (StackTraceElement element : throwable.getStackTrace()) {
            String className = element.getClassName();
            for (String part : classNameParts) {
                if (className.contains(part)) {
                    return true;
                }
            }
        }
        return false;
    }
}