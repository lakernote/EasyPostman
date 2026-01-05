package com.laker.postman.service.js;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.Map;

/**
 * JS脚本执行器，使用GraalVM的Polyglot API执行JavaScript脚本。
 * 提供统一的脚本执行入口，支持变量注入、polyfill、输出回调和错误处理。
 * <p>
 * 使用 Context Pool 复用 Context 对象，避免高并发场景下的内存溢出问题。
 * </p>
 */
@Slf4j
@UtilityClass
public class JsScriptExecutor {

    /**
     * Context 池 - 默认大小为 CPU 核心数的 4 倍，最少 16 个
     */
    private static final JsContextPool CONTEXT_POOL;
    private static final int CONTEXT_ACQUIRE_TIMEOUT_MS = 5000; // 获取 Context 超时时间

    static {
        int poolSize = Math.max(16, Runtime.getRuntime().availableProcessors() * 4);
        CONTEXT_POOL = new JsContextPool(poolSize);
        log.info("Initialized JS Context Pool with size: {}", poolSize);

        // 注册 shutdown hook，在应用退出时关闭池
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down JS Context Pool...");
            CONTEXT_POOL.shutdown();
        }));
    }

    /**
     * 执行JS脚本（使用上下文对象）
     *
     * @param context 脚本执行上下文
     * @throws ScriptExecutionException 脚本执行异常
     */
    public static void executeScript(ScriptExecutionContext context) throws ScriptExecutionException {
        if (context == null || context.getScript() == null || context.getScript().isBlank()) {
            log.debug("Script is empty, skipping execution");
            return;
        }

        try {
            executeScript(context.getScript(), context.getBindings(), context.getOutputCallback());
            log.debug("Script executed successfully: {}", context.getScriptType().getDisplayName());
        } catch (Exception e) {
            String errorMsg = String.format("%s execution failed: %s",
                    context.getScriptType().getDisplayName(), e.getMessage());
            log.error(errorMsg, e);
            throw new ScriptExecutionException(errorMsg, e, context.getScriptType());
        }
    }

    /**
     * 执行JS脚本，自动注入所有变量、polyfill，并支持输出回调。
     * <p>
     * 使用 Context Pool 复用 Context 对象，提高性能并避免内存溢出。
     * 注意：由于 GraalVM Context 的输出流在创建时绑定，这里通过自定义 console.log 实现输出捕获。
     * </p>
     *
     * @param script         脚本内容
     * @param bindings       需要注入的变量（变量名->对象），如request/env/postman/responseBody等
     * @param outputCallback 输出回调（可为null）
     * @throws ScriptExecutionException 脚本执行异常
     */
    public static void executeScript(String script, Map<String, Object> bindings, OutputCallback outputCallback)
            throws ScriptExecutionException {
        if (script == null || script.isBlank()) {
            return;
        }

        JsContextPool.PooledContext pooledContext = null;

        try {
            // 从池中获取 Context
            pooledContext = CONTEXT_POOL.borrowContext(CONTEXT_ACQUIRE_TIMEOUT_MS);
            Context context = pooledContext.getContext();

            // 注入输出回调（如果有）
            if (outputCallback != null) {
                injectConsoleLog(context, outputCallback);
            }

            // 注入变量（polyfill 已在池创建时注入）
            injectBindings(context, bindings);

            // 执行脚本
            context.eval("js", script);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = "Script execution interrupted: " + e.getMessage();
            log.error(errorMsg, e);
            throw new ScriptExecutionException(errorMsg, e);
        } catch (PolyglotException e) {
            // GraalVM特定异常
            String errorMsg = formatPolyglotError(e);
            log.error("Script execution error: {}", errorMsg, e);
            throw new ScriptExecutionException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Unexpected error during script execution: " + e.getMessage();
            log.error(errorMsg, e);
            throw new ScriptExecutionException(errorMsg, e);
        } finally {
            // 归还 Context 到池中
            if (pooledContext != null) {
                CONTEXT_POOL.returnContext(pooledContext);
            }
        }
    }

    /**
     * 注入自定义的 console.log 实现（使用 Java 回调）
     */
    private static void injectConsoleLog(Context context, OutputCallback outputCallback) {
        if (outputCallback == null) {
            return;
        }

        // 使用 ProxyExecutable 实现 console.log
        ProxyExecutable logFunc = args -> {
            if (args.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) sb.append(" ");
                    sb.append(args[i].toString());
                }
                outputCallback.onOutput(sb.toString());
            }
            return null;
        };

        // 注入到 console 对象
        context.eval("js", """
                if (typeof console === 'undefined') {
                    globalThis.console = {};
                }
                """);
        context.getBindings("js").getMember("console").putMember("log", logFunc);
    }


    /**
     * 格式化Polyglot异常信息
     */
    private static String formatPolyglotError(PolyglotException e) {
        if (e.isHostException()) {
            return "Host exception: " + e.getMessage();
        }
        if (e.isGuestException()) {
            StringBuilder sb = new StringBuilder();
            sb.append(e.getMessage());
            if (e.getSourceLocation() != null) {
                sb.append(" (Line: ").append(e.getSourceLocation().getStartLine()).append(")");
            }
            return sb.toString();
        }
        return e.getMessage();
    }

    /**
     * 注入变量到JS上下文
     */
    private static void injectBindings(Context context, Map<String, Object> bindings) {
        if (bindings != null && !bindings.isEmpty()) {
            for (var entry : bindings.entrySet()) {
                try {
                    context.getBindings("js").putMember(entry.getKey(), entry.getValue());
                    log.trace("Injected binding: {}", entry.getKey());
                } catch (Exception e) {
                    log.warn("Failed to inject binding: {}, error: {}", entry.getKey(), e.getMessage());
                }
            }
        }
    }

    /**
     * 输出回调接口
     */
    public interface OutputCallback {
        void onOutput(String output);
    }
}