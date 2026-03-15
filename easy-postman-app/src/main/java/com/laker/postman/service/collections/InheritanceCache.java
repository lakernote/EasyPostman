package com.laker.postman.service.collections;

import com.laker.postman.model.RequestGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 继承缓存管理器
 * <p>
 * 缓存的是请求对应的 Group 链（List<RequestGroup>），而不是合并后的整个 HttpRequestItem。
 * <p>
 * 原因：若缓存整个 HttpRequestItem（含 url/params），当用户在 editSubPanel 修改请求后，
 * 即便节点数据已更新，缓存命中时仍会返回包含旧 url/params 的对象，导致发送旧数据。
 * <p>
 * 缓存 Group 链后，每次 build 都会把最新 item 的 url/params 与缓存的 Group 继承部分
 * （auth/headers/scripts）重新合并，确保 url/params 始终是最新的。
 */
@Slf4j
public class InheritanceCache {

    /**
     * 缓存存储：requestId -> Group 链（从外到内的分组列表）
     */
    private final Map<String, List<RequestGroup>> cache = new ConcurrentHashMap<>();

    /**
     * 原子性地获取或计算 Group 链缓存
     *
     * @param requestId       请求ID
     * @param mappingFunction 计算函数（仅在缓存未命中时执行），返回该请求对应的 Group 链
     * @return 缓存的或新计算的 Group 链
     */
    public List<RequestGroup> computeIfAbsent(String requestId, Function<String, List<RequestGroup>> mappingFunction) {
        if (requestId == null || requestId.trim().isEmpty()) {
            log.warn("无效的 requestId，跳过缓存");
            return mappingFunction.apply(requestId);
        }

        if (mappingFunction == null) {
            throw new IllegalArgumentException("mappingFunction 不能为 null");
        }

        return cache.computeIfAbsent(requestId, id -> {
            log.debug("GroupChain 缓存未命中，开始计算: {}", requestId);
            return mappingFunction.apply(id);
        });
    }

    /**
     * 清空所有缓存
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
     *
     * @param requestId 请求ID
     */
    public void remove(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return;
        }
        List<RequestGroup> removed = cache.remove(requestId);
        if (removed != null) {
            log.debug("缓存已移除: {}", requestId);
        }
    }
}
