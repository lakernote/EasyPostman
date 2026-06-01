package com.laker.postman.util;

import lombok.experimental.UtilityClass;

/**
 * 全局未捕获异常过滤器的判断逻辑。
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

        return isSshExecutorShutdownError(throwable);
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
