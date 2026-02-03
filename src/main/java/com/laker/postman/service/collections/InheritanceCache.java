package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 继承缓存管理器
 * <p>
 * 职责：管理已应用继承的请求缓存
 * <p>
 * 特性：
 * - 线程安全（ConcurrentHashMap）
 * - 版本控制（全局失效）
 * - 精细化失效（单个失效）
 * - 统计监控
 *
 * @author laker
 * @since 4.3.22
 */
@Slf4j
public class InheritanceCache {

    /**
     * 缓存版本号：Collection 修改时递增，使所有缓存失效
     */
    private final AtomicLong version = new AtomicLong(0);

    /**
     * 缓存存储：requestId -> CachedItem
     */
    private final Map<String, CachedItem> cache = new ConcurrentHashMap<>();

    /**
     * 缓存的项
     */
    private static class CachedItem {
        final HttpRequestItem item;
        final long version;
        final long timestamp;

        CachedItem(HttpRequestItem item, long version) {
            this.item = item;
            this.version = version;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 获取缓存的请求
     *
     * @param requestId 请求ID
     * @return 缓存的请求（如果存在且有效）
     */
    public Optional<HttpRequestItem> get(String requestId) {
        if (requestId == null) {
            return Optional.empty();
        }

        CachedItem cached = cache.get(requestId);
        long currentVersion = version.get();

        if (cached != null && cached.version == currentVersion) {
            log.trace("缓存命中: {}", requestId);
            return Optional.of(cached.item);
        }

        log.trace("缓存未命中: {}", requestId);
        return Optional.empty();
    }

    /**
     * 缓存请求
     *
     * @param requestId 请求ID
     * @param item 已应用继承的请求
     */
    public void put(String requestId, HttpRequestItem item) {
        if (requestId != null && item != null) {
            cache.put(requestId, new CachedItem(item, version.get()));
            log.trace("缓存已存储: {}", requestId);
        }
    }

    /**
     * 使所有缓存失效（全局失效）
     * <p>
     * 调用时机：
     * - 修改分组的 auth/headers/scripts
     * - 添加/删除/移动节点
     * - 导入 Collection
     */
    public void invalidateAll() {
        long oldVersion = version.get();
        long newVersion = version.incrementAndGet();
        int size = cache.size();
        cache.clear();

        log.debug("缓存全局失效: 版本 {} -> {}, 清除 {} 项", oldVersion, newVersion, size);
    }

    /**
     * 使特定请求的缓存失效（精细化失效）
     * <p>
     * 调用时机：仅修改单个请求时
     *
     * @param requestId 请求ID
     */
    public void invalidate(String requestId) {
        if (requestId != null) {
            CachedItem removed = cache.remove(requestId);
            if (removed != null) {
                log.debug("缓存精细化失效: {}", requestId);
            }
        }
    }

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    public CacheStats getStats() {
        return new CacheStats(
            version.get(),
            cache.size(),
            calculateAverageAge()
        );
    }

    /**
     * 计算缓存项的平均年龄（毫秒）
     */
    private long calculateAverageAge() {
        if (cache.isEmpty()) {
            return 0;
        }

        long now = System.currentTimeMillis();
        long totalAge = cache.values().stream()
            .mapToLong(item -> now - item.timestamp)
            .sum();

        return totalAge / cache.size();
    }

    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        public final long version;
        public final int size;
        public final long averageAgeMs;

        CacheStats(long version, int size, long averageAgeMs) {
            this.version = version;
            this.size = size;
            this.averageAgeMs = averageAgeMs;
        }

        @Override
        public String toString() {
            return String.format("版本=%d, 缓存项=%d, 平均年龄=%dms",
                version, size, averageAgeMs);
        }
    }
}
