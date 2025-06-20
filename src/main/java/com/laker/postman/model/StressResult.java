package com.laker.postman.model;

import java.util.List;
import java.util.Map;

/**
 * 压力测试结果类，封装测试过程中的各项数据。
 */
public class StressResult {
    /**
     * 每个请求的耗时列表（单位：毫秒）
     */
    public List<Long> times;

    /**
     * 测试过程中出现的错误请求数量
     */
    public int errorCount;

    /**
     * 请求编号与对应耗时的映射
     */
    public Map<Integer, Long> timeData;

    /**
     * 总耗时（单位：毫秒）
     */
    public long totalDuration;

    /**
     * 构造函数
     *
     * @param times      请求耗时列表
     * @param errorCount 错误请求数量
     * @param timeData   请求编号与耗时的映射
     */
    public StressResult(List<Long> times, int errorCount, Map<Integer, Long> timeData) {
        this.times = times;
        this.errorCount = errorCount;
        this.timeData = timeData;
    }

    /**
     * 构造函数（含总耗时）
     */
    public StressResult(List<Long> times, int errorCount, Map<Integer, Long> timeData, long totalDuration) {
        this.times = times;
        this.errorCount = errorCount;
        this.timeData = timeData;
        this.totalDuration = totalDuration;
    }
}