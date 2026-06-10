package com.laker.postman.service.js;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.LinkedHashMap;
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
     * 普通 collection/function 脚本共享池。压测使用 PooledScriptExecutor 的运行级独立池。
     */
    static final int DEFAULT_SHARED_CONTEXT_POOL_SIZE = Math.max(
            4,
            Math.min(16, Runtime.getRuntime().availableProcessors())
    );
    private static final int DEFAULT_SHARED_CONTEXT_ACQUIRE_TIMEOUT_MS = 1_000;
    private static volatile JsContextPool contextPool;
    private static volatile int contextPoolSize;
    private static volatile int contextAcquireTimeoutMs; // 获取 Context 超时时间
    private static final Object CONTEXT_POOL_LOCK = new Object();
    private static final int SCRIPT_SOURCE_CACHE_MAX_SIZE = 512;
    private static final Map<String, Source> SCRIPT_SOURCE_CACHE = new LinkedHashMap<>(
            SCRIPT_SOURCE_CACHE_MAX_SIZE,
            0.75f,
            true
    ) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Source> eldest) {
            return size() > SCRIPT_SOURCE_CACHE_MAX_SIZE;
        }
    };

    /**
     * ThreadLocal 存储当前正在执行的原始脚本，用于错误报告
     */
    private static final ThreadLocal<String> CURRENT_SCRIPT = new ThreadLocal<>();

    @FunctionalInterface
    public interface ScriptExecutor {
        void execute(ScriptExecutionContext context) throws ScriptExecutionException;
    }

    public static final class PooledScriptExecutor implements ScriptExecutor, AutoCloseable {
        private final JsContextPool pool;
        private final int acquireTimeoutMs;
        private final Map<String, Source> scriptSourceCache = new LinkedHashMap<>(
                SCRIPT_SOURCE_CACHE_MAX_SIZE,
                0.75f,
                true
        ) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Source> eldest) {
                return size() > SCRIPT_SOURCE_CACHE_MAX_SIZE;
            }
        };

        public PooledScriptExecutor(int poolSize, int acquireTimeoutMs) {
            this.pool = new JsContextPool(Math.max(1, poolSize));
            this.acquireTimeoutMs = Math.max(1, acquireTimeoutMs);
        }

        @Override
        public void execute(ScriptExecutionContext context) throws ScriptExecutionException {
            executeScript(context, pool, acquireTimeoutMs, scriptSourceCache);
        }

        @Override
        public void close() {
            pool.shutdown();
            synchronized (scriptSourceCache) {
                scriptSourceCache.clear();
            }
        }
    }

    static {
        // 注册 shutdown hook，在应用退出时关闭池
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down JS Context Pool...");
            JsContextPool pool = contextPool;
            if (pool != null) {
                pool.shutdown();
            }
        }));
    }

    public static void reconfigureContextPoolFromSettings() {
        int resolvedPoolSize = DEFAULT_SHARED_CONTEXT_POOL_SIZE;
        int resolvedAcquireTimeoutMs = DEFAULT_SHARED_CONTEXT_ACQUIRE_TIMEOUT_MS;

        synchronized (CONTEXT_POOL_LOCK) {
            contextAcquireTimeoutMs = resolvedAcquireTimeoutMs;
            if (contextPool == null || contextPoolSize != resolvedPoolSize) {
                JsContextPool oldPool = contextPool;
                contextPool = new JsContextPool(resolvedPoolSize);
                contextPoolSize = resolvedPoolSize;
                log.info("Initialized shared JS Context Pool with size: {}, acquire timeout: {}ms",
                        resolvedPoolSize, resolvedAcquireTimeoutMs);
                if (oldPool != null) {
                    oldPool.retire();
                }
            } else {
                log.info("Updated shared JS Context Pool acquire timeout: {}ms", resolvedAcquireTimeoutMs);
            }
        }
    }

    /**
     * 执行JS脚本（使用上下文对象）
     *
     * @param context 脚本执行上下文
     * @throws ScriptExecutionException 脚本执行异常
     */
    public static void executeScript(ScriptExecutionContext context) throws ScriptExecutionException {
        executeScript(context, null, 0, SCRIPT_SOURCE_CACHE);
    }

    private static void executeScript(ScriptExecutionContext context,
                                      JsContextPool pool,
                                      int acquireTimeoutMs,
                                      Map<String, Source> scriptSourceCache) throws ScriptExecutionException {
        if (context == null || context.getScript() == null || context.getScript().isBlank()) {
            log.debug("Script is empty, skipping execution");
            return;
        }

        try {
            executeScript(
                    context.getScript(),
                    context.getBindings(),
                    context.getOutputCallback(),
                    pool,
                    acquireTimeoutMs,
                    scriptSourceCache
            );
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
     * 使用 IIFE (Immediately Invoked Function Expression) 包装用户脚本，
     * 避免 let/const 变量污染全局作用域，使 Context 可以安全复用。
     * </p>
     *
     * @param script         脚本内容
     * @param bindings       需要注入的变量（变量名->对象），如 pm
     * @param outputCallback 输出回调（可为null）
     * @throws ScriptExecutionException 脚本执行异常
     */
    public static void executeScript(String script, Map<String, Object> bindings, OutputCallback outputCallback)
            throws ScriptExecutionException {
        executeScript(script, bindings, outputCallback, null, 0, SCRIPT_SOURCE_CACHE);
    }

    private static void executeScript(String script,
                                      Map<String, Object> bindings,
                                      OutputCallback outputCallback,
                                      JsContextPool pool,
                                      int acquireTimeoutMs,
                                      Map<String, Source> scriptSourceCache)
            throws ScriptExecutionException {
        if (script == null || script.isBlank()) {
            return;
        }

        JsContextPool.PooledContext pooledContext = null;
        JsContextPool borrowedPool = null;

        try {
            // 保存原始脚本到 ThreadLocal，用于错误报告
            CURRENT_SCRIPT.set(script);

            // 从池中获取 Context
            while (pooledContext == null) {
                borrowedPool = pool == null ? sharedContextPool() : pool;
                if (borrowedPool == null) {
                    throw new ScriptExecutionException("JS context pool is not initialized", null);
                }
                int resolvedAcquireTimeoutMs = pool == null ? contextAcquireTimeoutMs : acquireTimeoutMs;
                try {
                    pooledContext = borrowedPool.borrowContext(resolvedAcquireTimeoutMs);
                } catch (IllegalStateException e) {
                    if (pool == null && borrowedPool != contextPool && borrowedPool.isRetired()) {
                        log.debug("Retrying JS context borrow after old pool was retired");
                        continue;
                    }
                    throw e;
                }
            }
            Context context = pooledContext.getContext();

            // 注入输出回调（如果有）
            if (outputCallback != null) {
                injectConsoleLog(context, outputCallback);
            }

            // 注入变量（polyfill 已在池创建时注入）
            injectBindings(context, bindings);

            context.eval(getCachedScriptSource(script, scriptSourceCache));

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
            // 清理 ThreadLocal
            CURRENT_SCRIPT.remove();

            // 归还 Context 到池中
            if (pooledContext != null) {
                borrowedPool.returnContext(pooledContext);
            }
        }
    }

    private static JsContextPool sharedContextPool() {
        JsContextPool pool = contextPool;
        if (pool == null) {
            reconfigureContextPoolFromSettings();
            pool = contextPool;
        }
        return pool;
    }

    /**
     * 用户脚本在包装后的起始行号偏移量
     * IIFE 包装器会添加一行: (function() {
     * 因此用户脚本从第 2 行开始
     */
    private static final int USER_SCRIPT_LINE_OFFSET = 1;

    /**
     * 使用 IIFE 包装脚本，避免 let/const 变量污染全局作用域
     * <p>
     * 原理：将用户脚本包装在一个立即执行的函数中，
     * 使所有 let/const/var 声明的变量都成为局部变量。
     * </p>
     * <p>
     * 包装格式：
     * 第1行: (function() {
     * 第2行开始: 用户脚本
     * 最后: })();
     * </p>
     *
     * @param script 原始脚本
     * @return 包装后的脚本
     */
    private static String wrapScriptWithIIFE(String script) {
        return "(function() {\n" + script + "\n})();";
    }

    private static Source getCachedScriptSource(String script) {
        return getCachedScriptSource(script, SCRIPT_SOURCE_CACHE);
    }

    private static Source getCachedScriptSource(String script, Map<String, Source> scriptSourceCache) {
        Map<String, Source> resolvedCache = scriptSourceCache == null ? SCRIPT_SOURCE_CACHE : scriptSourceCache;
        synchronized (resolvedCache) {
            Source source = resolvedCache.get(script);
            if (source != null) {
                return source;
            }

            Source newSource = Source.newBuilder("js", wrapScriptWithIIFE(script), buildSourceName(script))
                    .cached(true)
                    .buildLiteral();
            resolvedCache.put(script, newSource);
            return newSource;
        }
    }

    private static String buildSourceName(String script) {
        return "easypostman-user-script-" + Integer.toUnsignedString(script.hashCode(), 16) + ".js";
    }

    /**
     * 注入自定义的 console 方法实现（使用 Java 回调）
     * 支持 console.log, console.error, console.warn, console.info, console.debug
     */
    private static void injectConsoleLog(Context context, OutputCallback outputCallback) {
        if (outputCallback == null) {
            return;
        }

        // 确保 console 对象存在
        context.eval("js", """
                if (typeof console === 'undefined') {
                    globalThis.console = {};
                }
                """);

        // 批量注入所有 console 方法
        var consoleObj = context.getBindings("js").getMember("console");
        for (ConsoleType type : ConsoleType.values()) {
            consoleObj.putMember(type.getMethodName(), createConsoleFunc(outputCallback, type));
        }
    }

    /**
     * 创建 console 方法的 ProxyExecutable
     *
     * @param callback    输出回调
     * @param consoleType Console 方法类型
     * @return ProxyExecutable 实例
     */
    private static ProxyExecutable createConsoleFunc(OutputCallback callback, ConsoleType consoleType) {
        return args -> {
            if (args.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) sb.append(" ");
                    sb.append(args[i].toString());
                }
                callback.onOutput(sb.toString(), consoleType);
            }
            return null;
        };
    }


    /**
     * 格式化Polyglot异常信息
     * <p>
     * 注意：由于脚本可能被 Group/Request 合并，以及 IIFE 包装，
     * 行号可能不准确。因此主要依赖显示错误代码内容来帮助定位问题。
     */
    private static String formatPolyglotError(PolyglotException e) {
        if (e.isHostException()) {
            return "Host exception: " + e.getMessage();
        }
        if (e.isGuestException()) {
            StringBuilder sb = new StringBuilder();
            sb.append(e.getMessage());

            if (e.getSourceLocation() != null) {
                int reportedLine = e.getSourceLocation().getStartLine();

                // 尝试获取错误代码行（基于包装后的行号）
                int adjustedLine = reportedLine - USER_SCRIPT_LINE_OFFSET;
                adjustedLine = Math.max(1, adjustedLine);
                String codeAtLine = getCodeLineFromScript(adjustedLine);

                // 显示错误代码（这是最重要的，用户可以直接搜索定位）
                if (codeAtLine != null && !codeAtLine.trim().isEmpty()) {
                    sb.append("\n>>> ").append(codeAtLine.trim());
                }
            }
            return sb.toString();
        }
        return e.getMessage();
    }

    /**
     * 从原始脚本中获取指定行号的代码
     *
     * @param lineNumber 行号（1-based）
     * @return 代码行内容，如果无法获取则返回 null
     */
    private static String getCodeLineFromScript(int lineNumber) {
        try {
            String script = CURRENT_SCRIPT.get();
            if (script == null) {
                return null;
            }

            String[] lines = script.split("\n");
            if (lineNumber > 0 && lineNumber <= lines.length) {
                return lines[lineNumber - 1];
            }
        } catch (Exception ex) {
            log.trace("Failed to get code line: {}", ex.getMessage());
        }
        return null;
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
     * Console 方法类型枚举
     */
    @Getter
    public enum ConsoleType {
        LOG("log"),
        ERROR("error"),
        WARN("warn"),
        INFO("info"),
        DEBUG("debug");

        private final String methodName;

        ConsoleType(String methodName) {
            this.methodName = methodName;
        }

    }

    /**
     * 输出回调接口
     */
    public interface OutputCallback {
        void onOutput(String output);

        /**
         * 带日志类型的输出回调
         *
         * @param output      输出内容
         * @param consoleType Console 方法类型
         */
        default void onOutput(String output, ConsoleType consoleType) {
            onOutput(output); // 默认回退到无类型的方法
        }
    }
}
