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
    public String filePath; // 临时文件下载路径字段
    public String fileName; // 如果是文件下载，从响应头中获取的文件名字段
    public long costMs; // 请求耗时，单位毫秒
    public long endTime; // 响应结束时间，单位毫秒
    public String protocol; // 协议类型字段，例如 HTTP/1.1 或 HTTP/2
    public int idleConnectionCount; // 空闲连接数
    public int connectionCount; // 连接总数
    public HttpEventInfo httpEventInfo;
    public long bodySize; // 响应体字节数
    public long headersSize; // 响应头字节数
    public boolean isSse = false; // 是否为SSE响应

    public void addHeader(String name, List<String> value) {
        if (headers == null) {
            return;
        }
        if ("Easy-Content-Encoding".equalsIgnoreCase(name)) {
            headers.put("Content-Encoding", value);
            headers.remove("Easy-Content-Encoding");
        } else if ("Easy-Content-Length".equalsIgnoreCase(name)) {
            headers.put("Content-Length", value);
            headers.remove("Easy-Content-Length");
        } else {
            headers.put(name, value);
        }
    }

    /**
     * 简化对象，将渲染时不需要的字段置为 null，减少内存占用
     * 保留的字段：code, protocol, threadName, httpEventInfo, headers, body, costMs, endTime, bodySize, headersSize, idleConnectionCount, connectionCount
     * 置为 null 的字段：filePath, fileName
     */
    public void simplify() {
        this.filePath = null;    // 文件下载路径，渲染时不需要
        this.fileName = null;    // 文件下载名称，渲染时不需要
        // isSse, idleConnectionCount, connectionCount 保留（Timing标签页可能需要）
    }
}
