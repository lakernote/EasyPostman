package com.laker.postman.service.js;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;

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
    private final AtomicInteger waitingBorrowCount = new AtomicInteger(0);
    private volatile boolean retired = false;
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

        long effectiveTimeoutMs = Math.max(1, timeoutMs);
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(effectiveTimeoutMs);

        while (true) {
            // 1. 尝试从池中快速获取（复用）
            PooledContext pooled = pool.poll();
            if (pooled != null) {
                totalReused.incrementAndGet();
                log.debug("Reused context from pool, reuse count: {}", totalReused.get());
                return pooled;
            }

            // 2. 池为空，检查是否可以创建新的 Context。CAS 失败时重试，避免突发并发下低于 maxSize 就进入等待。
            int current = currentSize.get();
            if (!retired && current < maxSize) {
                if (currentSize.compareAndSet(current, current + 1)) {
                    try {
                        pooled = createNewContext();
                        totalCreated.incrementAndGet();
                        log.debug("Created new context #{}, pool size: {}/{}",
                                totalCreated.get(), currentSize.get(), maxSize);
                        return pooled;
                    } catch (Exception e) {
                        currentSize.decrementAndGet();
                        throw e;
                    }
                }
                continue;
            }

            // 3. 达到最大数量限制，阻塞等待空闲 Context
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                throw new IllegalStateException(
                        String.format("Failed to acquire context within timeout: %dms. Pool size: %d/%d",
                                effectiveTimeoutMs, currentSize.get(), maxSize));
            }
            log.debug("Context pool exhausted ({}), waiting for available context...", current);
            waitingBorrowCount.incrementAndGet();
            try {
                pooled = pool.poll();
                if (pooled != null) {
                    totalReused.incrementAndGet();
                    return pooled;
                }
                if (closed) {
                    throw new IllegalStateException("Context pool is closed");
                }
                if (retired && currentSize.get() <= 0) {
                    throw new IllegalStateException("Context pool is retired");
                }
                pooled = pool.poll(remainingNanos, TimeUnit.NANOSECONDS);
                if (pooled != null) {
                    totalReused.incrementAndGet();
                    return pooled;
                }
            } finally {
                waitingBorrowCount.decrementAndGet();
                closeRetiredIdleContextsIfUnused();
            }
            throw new IllegalStateException(
                    String.format("Failed to acquire context within timeout: %dms. Pool size: %d/%d",
                            effectiveTimeoutMs, currentSize.get(), maxSize));
        }
    }

    /**
     * 归还 Context 到池中（复用）
     *
     * @param pooled Context 对象
     */
    public void returnContext(PooledContext pooled) {
        if (pooled == null) {
            return;
        }
        if (closed) {
            pooled.close();
            decrementCurrentSize();
            return;
        }

        try {
            // 清理脚本执行期间新增的全局状态，避免复用 Context 时污染下一次执行。
            cleanupGlobalVariables(pooled.context);

            // 归还到池中供复用。退役池只服务已经在旧池等待的借用者，之后逐步关闭。
            if (retired) {
                if (waitingBorrowCount.get() > 0 && pool.offer(pooled, 100, TimeUnit.MILLISECONDS)) {
                    return;
                }
                pooled.close();
                decrementCurrentSize();
                log.debug("Retired pool has no waiters, closed context. Pool size: {}/{}", currentSize.get(), maxSize);
            } else if (!pool.offer(pooled, 100, TimeUnit.MILLISECONDS)) {
                // 池已满，关闭 Context
                pooled.close();
                decrementCurrentSize();
                log.debug("Pool is full, closed context. Pool size: {}/{}", currentSize.get(), maxSize);
            }
        } catch (Exception e) {
            log.warn("Failed to return context to pool: {}", e.getMessage());
            // 归还失败，关闭 Context
            pooled.close();
            decrementCurrentSize();
        }
    }

    private void decrementCurrentSize() {
        currentSize.updateAndGet(value -> Math.max(0, value - 1));
    }

    /**
     * 退役池：不再作为新共享池使用，但允许已经在此池等待的线程拿到归还的 Context。
     */
    void retire() {
        retired = true;
        closeRetiredIdleContextsIfUnused();
    }

    boolean isRetired() {
        return retired;
    }

    int getWaitingBorrowCountForTests() {
        return waitingBorrowCount.get();
    }

    private void closeRetiredIdleContextsIfUnused() {
        if (!retired || closed || waitingBorrowCount.get() > 0) {
            return;
        }
        PooledContext idle;
        while ((idle = pool.poll()) != null) {
            idle.close();
            decrementCurrentSize();
        }
    }

    /**
     * 清理脚本执行期间新增的全局变量。
     * <p>
     * 注意：由于用户脚本使用 IIFE 包装，let/const 变量都是局部变量，
     * 这里主要处理显式写到 globalThis 的变量和我们注入的变量（pm）。
     * </p>
     */
    private void cleanupGlobalVariables(Context context) {
        try {
            context.eval("js", """
                    (function() {
                        const baseline = globalThis.__epBaselineGlobals;
                        if (baseline) {
                            Object.getOwnPropertyNames(globalThis).forEach(name => {
                                if (name === '__epBaselineGlobals') {
                                    return;
                                }
                                if (!baseline[name]) {
                                    try {
                                        delete globalThis[name];
                                    } catch (e) {
                                        // 忽略删除失败
                                    }
                                }
                            });
                        }

                        const injectedVars = ['pm'];
                        injectedVars.forEach(varName => {
                            try {
                                delete globalThis[varName];
                            } catch (e) {
                                // 忽略删除失败
                            }
                        });

                        if (globalThis.__epRequireCache) {
                            Object.keys(globalThis.__epRequireCache).forEach(moduleId => {
                                if (String(moduleId).indexOf('builtin:') !== 0) {
                                    delete globalThis.__epRequireCache[moduleId];
                                }
                            });
                        }
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
                    .allowHostAccess(HostAccess.ALL)
                    .allowHostClassLookup(className -> false)
                    .allowNativeAccess(false)
                    .out(OutputStream.nullOutputStream())
                    .err(OutputStream.nullOutputStream())
                    .engine(ENGINE)
                    .build();

            // 预加载 polyfill 和内置库
            JsPolyfillInjector.injectAll(context);
            recordBaselineGlobals(context);

            return new PooledContext(context);
        } catch (Exception e) {
            log.error("Failed to create new context", e);
            throw new RuntimeException("Failed to create JS context", e);
        }
    }

    private void recordBaselineGlobals(Context context) {
        context.eval("js", """
                (function() {
                    const baseline = Object.create(null);
                    Object.getOwnPropertyNames(globalThis).forEach(name => {
                        baseline[name] = true;
                    });
                    Object.defineProperty(globalThis, '__epBaselineGlobals', {
                        value: baseline,
                        writable: false,
                        configurable: false,
                        enumerable: false
                    });
                })();
                """);
    }

    /**
     * 关闭池，释放所有 Context
     */
    public void shutdown() {
        closed = true;
        PooledContext pooled;
        int closedIdleCount = 0;
        while ((pooled = pool.poll()) != null) {
            pooled.close();
            decrementCurrentSize();
            closedIdleCount++;
        }
        double reuseRatio = totalCreated.get() > 0 ? (totalReused.get() * 100.0 / totalCreated.get()) : 0;
        log.info("JsContextPool shutdown. Total created: {}, Total reused: {}, Closed idle: {}, Active borrowed: {}, Reuse ratio: {}%",
                totalCreated.get(), totalReused.get(), closedIdleCount, currentSize.get(), String.format("%.2f", reuseRatio));
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
