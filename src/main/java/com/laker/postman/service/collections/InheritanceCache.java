package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 继承缓存管理器
 * <p>
 * 设计原则：简单、直观、易于理解
 * <p>
 * 核心职责：
 * - 缓存已应用继承规则的请求对象（避免重复计算）
 * - 提供快速的缓存失效机制
 * <p>
 *
 * @author laker
 * @since 4.3.22
 */
@Slf4j
public class InheritanceCache {

    /**
     * 缓存存储：requestId -> 已应用继承的请求对象
     * <p>
     * 为什么用 ConcurrentHashMap？
     * - 支持高并发读写
     * - get/put 操作 O(1) 时间复杂度
     * - 不需要显式加锁
     */
    private final Map<String, HttpRequestItem> cache = new ConcurrentHashMap<>();

    /**
     * 原子性地获取或计算缓存
     * <p>
     * 并发安全性：
     * - 使用 ConcurrentHashMap.computeIfAbsent 实现原子操作
     * - 如果多个线程同时请求同一个 key，只有一个线程会执行 mappingFunction
     * - 其他线程会等待并获取第一个线程计算的结果
     * <p>
     * 性能优势：
     * - 避免重复计算（check-then-act 竞态条件）
     * - 不需要显式加锁
     * <p>
     *
     * @param requestId       请求ID
     * @param mappingFunction 计算函数（仅在缓存未命中时执行）
     * @return 缓存的或新计算的请求对象
     */
    public HttpRequestItem computeIfAbsent(String requestId, Function<String, HttpRequestItem> mappingFunction) {
        if (requestId == null || requestId.trim().isEmpty()) {
            log.warn("无效的 requestId，跳过缓存");
            return mappingFunction.apply(requestId);
        }

        if (mappingFunction == null) {
            throw new IllegalArgumentException("mappingFunction 不能为 null");
        }

        // ConcurrentHashMap.computeIfAbsent 保证原子性：
        // 1. 检查缓存是否存在
        // 2. 如果不存在，执行 mappingFunction 并存储结果
        // 3. 返回缓存的或新计算的值
        //
        // 多线程场景：
        // - 线程A、B同时请求同一个key
        // - 只有一个线程执行 mappingFunction
        // - 另一个线程等待并获取结果
        return cache.computeIfAbsent(requestId, id -> {
            log.info("缓存未命中，开始计算: {}", requestId);
            return mappingFunction.apply(id);
        });
    }

    /**
     * 清空所有缓存
     * <p>
     * 调用时机（重要！）：
     * 1. 修改分组的认证设置
     * 2. 修改分组的脚本（前置/后置）
     * 3. 修改分组的请求头
     * 4. 添加/删除/移动树节点
     * 5. 导入/删除 Collection
     * <p>
     * 设计思路：
     * - 采用粗粒度失效（全部清空）
     * - 简单可靠，不会遗漏
     * - ConcurrentHashMap.clear() 是线程安全的
     */
    public void clear() {
        int sizeBefore = cache.size();
        cache.clear();

        if (sizeBefore > 0) {
            log.debug("缓存已清空: 移除 {} 项", sizeBefore);
        }
    }

    /**
     * 移除单个请求的缓存
     * <p>
     * 调用时机：
     * - 修改单个请求的设置（不影响继承）
     * - 删除单个请求
     * <p>
     * 注意：如果不确定是否会影响其他请求，建议使用 clear()
     *
     * @param requestId 请求ID
     */
    public void remove(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return;
        }

        HttpRequestItem removed = cache.remove(requestId);
        if (removed != null) {
            log.debug("缓存已移除: {}", requestId);
        }
    }

}
