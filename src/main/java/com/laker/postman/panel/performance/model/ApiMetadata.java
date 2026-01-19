package com.laker.postman.panel.performance.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API元数据管理器
 * 集中管理API的ID和名称映射，避免在每个RequestResult中重复存储
 */
public class ApiMetadata {
    // API ID -> API Name 的映射
    private static final Map<String, String> API_METADATA = new ConcurrentHashMap<>();

    /**
     * 注册API元数据
     *
     * @param apiId   API唯一ID
     * @param apiName API名称
     */
    public static void register(String apiId, String apiName) {
        if (apiId != null && apiName != null) {
            API_METADATA.put(apiId, apiName);
        }
    }

    /**
     * 获取API名称
     *
     * @param apiId API唯一ID
     * @return API名称，如果不存在则返回ID本身
     */
    public static String getName(String apiId) {
        return API_METADATA.getOrDefault(apiId, apiId);
    }

    /**
     * 清除所有元数据（测试结束时调用）
     */
    public static void clear() {
        API_METADATA.clear();
    }

    /**
     * 获取已注册的API数量
     */
    public static int size() {
        return API_METADATA.size();
    }
}
