package com.laker.postman.model;

import java.util.List;
import java.util.Map;

/**
 * HTTP 响应类
 */
public class HttpResponse {
    public Map<String, List<String>> headers;
    public String body;
    public int code; // 添加响应状态码字段
    public String threadName; // 添加线程名称字段
    public String connectionInfo; // 连接信息字段
    public String filePath; // 下载文件路径字段
    public long costMs; // 请求耗时，单位毫秒
}