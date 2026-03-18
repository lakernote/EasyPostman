package com.laker.postman.service.js;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;

import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JS Context 对象池
 * <p>
 * 通过复用 GraalVM Context 对象来提升性能，避免频繁创建和销毁 Context。
 * 使用 IIFE (Immediately Invoked Function Expression) 包装用户脚本，实现变量隔离，
 * 避免 let/const 变量污染全局作用域，使 Context 可以安全复用。
 * </p>
 *
 * <h3>原理：</h3>
 * <pre>
 * 用户脚本：
 *   let env = "test";
 *   console.log(env);
 *
 * 自动包装为：
 *   (function() {
 *     let env = "test";  // 局部变量，不污染全局作用域
 *     console.log(env);
 *   })();
 * </pre>
 *
 * @author laker
 */
@Slf4j
public class JsContextPool {

    private static final Engine ENGINE = Engine.newBuilder()
            .option("engine.WarnInterpreterOnly", "false")
            .build();

    private final BlockingQueue<PooledContext> pool;
    private final int maxSize;
    private final AtomicInteger currentSize = new AtomicInteger(0);
    private final AtomicInteger totalCreated = new AtomicInteger(0);
    private final AtomicInteger totalReused = new AtomicInteger(0);
    private volatile boolean closed = false;

    /**
     * 包装的 Context 对象，带有统计信息
     */
    public static class PooledContext {
        final Context context;
        final long createdTime;
        final AtomicInteger useCount = new AtomicInteger(0);

        PooledContext(Context context) {
            this.context = context;
            this.createdTime = System.currentTimeMillis();
        }

        public Context getContext() {
            useCount.incrementAndGet();
            return context;
        }

        public void close() {
            try {
                context.close();
            } catch (Exception e) {
                log.warn("Failed to close context: {}", e.getMessage());
            }
        }
    }

    /**
     * 创建 Context 池
     *
     * @param maxSize 最大池大小（建议设置为 CPU 核心数的 2-4 倍）
     */
    public JsContextPool(int maxSize) {
        this.maxSize = maxSize;
        this.pool = new LinkedBlockingQueue<>(maxSize);
        log.info("Created JsContextPool with max size: {}", maxSize);
    }

    /**
     * 获取 Context（带超时）
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return Context 对象
     * @throws InterruptedException 如果等待被中断
     */
    public PooledContext borrowContext(long timeoutMs) throws InterruptedException {
        if (closed) {
            throw new IllegalStateException("Context pool is closed");
        }

        // 1. 尝试从池中快速获取（复用）
        PooledContext pooled = pool.poll();
        if (pooled != null) {
            totalReused.incrementAndGet();
            log.debug("Reused context from pool, reuse count: {}", totalReused.get());
            return pooled;
        }

        // 2. 池为空，检查是否可以创建新的 Context
        int current = currentSize.get();
        if (current < maxSize) {
            // 尝试原子性地增加计数
            if (currentSize.compareAndSet(current, current + 1)) {
                try {
                    pooled = createNewContext();
                    totalCreated.incrementAndGet();
                    log.debug("Created new context #{}, pool size: {}/{}",
                            totalCreated.get(), currentSize.get(), maxSize);
                    return pooled;
                } catch (Exception e) {
                    // 创建失败，回滚计数
                    currentSize.decrementAndGet();
                    throw e;
                }
            }
        }

        // 3. 达到最大数量限制，阻塞等待空闲 Context
        log.debug("Context pool exhausted ({}), waiting for available context...", current);
        pooled = pool.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (pooled == null) {
            throw new IllegalStateException(
                    String.format("Failed to acquire context within timeout: %dms. Pool size: %d/%d",
                            timeoutMs, currentSize.get(), maxSize));
        }
        totalReused.incrementAndGet();
        return pooled;
    }

    /**
     * 归还 Context 到池中（复用）
     *
     * @param pooled Context 对象
     */
    public void returnContext(PooledContext pooled) {
        if (pooled == null || closed) {
            return;
        }

        try {
            // 清理全局变量（只清理注入的变量，保留内置对象）
            cleanupGlobalVariables(pooled.context);

            // 归还到池中供复用
            if (!pool.offer(pooled, 100, TimeUnit.MILLISECONDS)) {
                // 池已满，关闭 Context
                pooled.close();
                currentSize.decrementAndGet();
                log.debug("Pool is full, closed context. Pool size: {}/{}", currentSize.get(), maxSize);
            }
        } catch (Exception e) {
            log.warn("Failed to return context to pool: {}", e.getMessage());
            // 归还失败，关闭 Context
            pooled.close();
            currentSize.decrementAndGet();
        }
    }

    /**
     * 清理全局变量（只清理注入的变量，保留内置对象）
     * <p>
     * 注意：由于用户脚本使用 IIFE 包装，let/const 变量都是局部变量，
     * 这里只需要清理我们注入的全局变量（pm、request 等）
     * </p>
     */
    private void cleanupGlobalVariables(Context context) {
        try {
            context.eval("js", """
                    // 只删除我们注入的全局变量，保留所有内置对象
                    (function() {
                        const injectedVars = ['pm', 'request', 'environment', 'globals',
                                              'responseBody', 'tests', 'iterationData'];
                        injectedVars.forEach(varName => {
                            try {
                                delete globalThis[varName];
                            } catch (e) {
                                // 忽略删除失败
                            }
                        });
                    })();
                    """);
        } catch (Exception e) {
            log.warn("Failed to cleanup global variables: {}", e.getMessage());
        }
    }

    /**
     * 创建新的 Context 对象
     */
    private PooledContext createNewContext() {
        try {
            Context context = Context.newBuilder("js")
                    .allowAllAccess(true)
                    .allowNativeAccess(true)
                    .out(OutputStream.nullOutputStream())
                    .err(OutputStream.nullOutputStream())
                    .engine(ENGINE)
                    .build();

            // 预加载 polyfill 和内置库
            JsPolyfillInjector.injectAll(context);

            return new PooledContext(context);
        } catch (Exception e) {
            log.error("Failed to create new context", e);
            throw new RuntimeException("Failed to create JS context", e);
        }
    }

    /**
     * 关闭池，释放所有 Context
     */
    public void shutdown() {
        closed = true;
        PooledContext pooled;
        while ((pooled = pool.poll()) != null) {
            pooled.close();
        }
        currentSize.set(0);
        double reuseRatio = totalCreated.get() > 0 ? (totalReused.get() * 100.0 / totalCreated.get()) : 0;
        log.info("JsContextPool shutdown. Total created: {}, Total reused: {}, Reuse ratio: {}%",
                totalCreated.get(), totalReused.get(), String.format("%.2f", reuseRatio));
    }

    /**
     * 获取统计信息
     */
    public String getStats() {
        return String.format("Pool[size=%d/%d, created=%d, reused=%d, ratio=%.2f%%]",
                currentSize.get(), maxSize, totalCreated.get(), totalReused.get(),
                totalCreated.get() > 0 ? (totalReused.get() * 100.0 / totalCreated.get()) : 0);
    }
}

