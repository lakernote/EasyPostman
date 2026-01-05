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
 * 用于复用 GraalVM Context 对象，避免在高并发场景下频繁创建和销毁 Context 导致的内存溢出。
 * Context 的创建成本较高（需要加载 JS 库、初始化环境等），通过对象池可以显著提升性能。
 * </p>
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
    private volatile boolean closed = false;

    /**
     * 包装的 Context 对象，带有创建时间和使用计数
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

        public void reset() {
            // 清理全局变量（保留内置库和 polyfill）
            try {
                context.eval("js", """
                        // 清理可能被脚本修改的全局变量
                        if (typeof pm !== 'undefined') delete globalThis.pm;
                        if (typeof request !== 'undefined') delete globalThis.request;
                        if (typeof environment !== 'undefined') delete globalThis.environment;
                        if (typeof globals !== 'undefined') delete globalThis.globals;
                        if (typeof responseBody !== 'undefined') delete globalThis.responseBody;
                        if (typeof tests !== 'undefined') delete globalThis.tests;
                        """);
            } catch (Exception e) {
                log.warn("Failed to reset context: {}", e.getMessage());
            }
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
     * 优化版本：减少锁竞争，提高并发性能
     *
     * @param timeoutMs 超时时间（毫秒）
     * @return Context 对象
     */
    public PooledContext borrowContext(long timeoutMs) throws InterruptedException {
        // 先尝试快速获取（非阻塞）
        PooledContext pooled = pool.poll();

        if (pooled != null) {
            // 从池中获取到了 Context（最快路径）
            return pooled;
        }

        // 池为空，检查是否可以创建新的 Context
        int current = currentSize.get();
        if (current < maxSize) {
            // 尝试原子性地增加计数（无锁，避免竞争）
            if (currentSize.compareAndSet(current, current + 1)) {
                try {
                    pooled = createNewContext();
                    log.debug("Created new context, pool size: {}/{}", currentSize.get(), maxSize);
                    return pooled;
                } catch (Exception e) {
                    // 创建失败，回滚计数
                    currentSize.decrementAndGet();
                    throw e;
                }
            }
        }

        // 达到最大数量限制，阻塞等待空闲 Context
        log.debug("Context pool exhausted ({}), waiting for available context...", current);
        pooled = pool.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (pooled == null) {
            throw new IllegalStateException("Failed to acquire context within timeout: " + timeoutMs + "ms. Pool size: " + currentSize.get());
        }
        return pooled;
    }

    /**
     * 归还 Context 到池中
     *
     * @param pooled Context 对象
     */
    public void returnContext(PooledContext pooled) {
        if (pooled == null || closed) {
            return;
        }

        try {
            // 重置 Context 状态
            pooled.reset();

            // 归还到池中
            if (!pool.offer(pooled, 100, TimeUnit.MILLISECONDS)) {
                // 归还失败（池已满），关闭 Context
                pooled.close();
                currentSize.decrementAndGet();
                log.debug("Pool is full, closed context. Pool size: {}/{}", currentSize.get(), maxSize);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pooled.close();
            currentSize.decrementAndGet();
        }
    }

    /**
     * 创建新的 Context 对象
     * 注意：Context 创建时使用 nullOutputStream，实际的输出流在 JsScriptExecutor 中处理
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
        log.info("JsContextPool shutdown, all contexts closed");
    }
}

