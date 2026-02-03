package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 继承缓存管理器
 * <p>
 * 设计原则：简单、直观、易于理解
 * <p>
 * 核心职责：
 * - 缓存已应用继承规则的请求对象（避免重复计算）
 * - 提供快速的缓存失效机制
 * <p>
 * 使用场景：
 * - 发送请求前，先查缓存，避免重复计算继承规则
 * - 修改分组设置后，清空缓存，确保数据一致性
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
     * 获取缓存的请求
     * <p>
     * 逻辑：
     * 1. 检查 requestId 是否为空
     * 2. 从 Map 中查找
     * 3. 返回 Optional（避免 null）
     *
     * @param requestId 请求ID
     * @return 缓存的请求（如果存在）
     */
    public Optional<HttpRequestItem> get(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return Optional.empty();
        }

        HttpRequestItem cached = cache.get(requestId);

        if (cached != null) {
            return Optional.of(cached);
        }

        log.warn("缓存未命中: {}", requestId);
        return Optional.empty();
    }

    /**
     * 缓存请求
     * <p>
     * 逻辑：
     * 1. 检查参数有效性
     * 2. 直接放入 Map
     *
     * @param requestId 请求ID
     * @param item      已应用继承的请求对象
     */
    public void put(String requestId, HttpRequestItem item) {
        if (requestId == null || requestId.trim().isEmpty() || item == null) {
            log.warn("无效的缓存参数: requestId={}, item={}", requestId, item);
            return;
        }

        cache.put(requestId, item);
        log.info("缓存已存储: {}", requestId);
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

    /**
     * 获取缓存大小
     *
     * @return 缓存中的请求数量
     */
    public int size() {
        return cache.size();
    }

    /**
     * 是否为空
     *
     * @return true 如果缓存为空
     */
    public boolean isEmpty() {
        return cache.isEmpty();
    }

}
