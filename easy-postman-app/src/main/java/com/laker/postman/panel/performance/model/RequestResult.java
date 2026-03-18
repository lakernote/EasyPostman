package com.laker.postman.panel.performance.model;

/**
 * 请求结果记录
 */
public class RequestResult {
    public long startTime;    // 开始时间（毫秒）
    public long endTime;      // 结束时间（毫秒）
    public boolean success;   // 是否成功
    public String apiId;      // API唯一ID（用于统计和查询）

    /**
     * 主构造函数
     */
    public RequestResult(long startTime, long endTime, boolean success, String apiId) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.success = success;
        this.apiId = apiId;
    }

    /**
     * 获取响应时间（动态计算，不存储）
     *
     * @return 响应时间（毫秒）
     */
    public long getResponseTime() {
        return endTime - startTime;
    }

    /**
     * 获取API名称（通过ApiMetadata查询）
     */
    public String getApiName() {
        return ApiMetadata.getName(apiId);
    }
}