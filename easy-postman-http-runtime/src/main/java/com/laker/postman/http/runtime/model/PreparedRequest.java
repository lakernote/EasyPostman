package com.laker.postman.http.runtime.model;

import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.HttpRequestProxyPolicy;
import com.laker.postman.request.model.TransportAuth;
import com.laker.postman.http.runtime.interaction.DownloadProgressSinkFactory;
import com.laker.postman.http.runtime.interaction.ResponseSizeLimitWarningSink;
import com.laker.postman.http.runtime.observation.HttpLifecycleLogSink;
import com.laker.postman.http.runtime.observation.NetworkLogSink;

import java.util.ArrayList;
import java.util.List;

/**
 * 准备好的请求对象，包含请求的所有必要信息以及替换变量后的内容。
 */
public class PreparedRequest {
    public String id;
    public String name;
    public String url;
    public String method;

    public String body;
    public String bodyType;

    public List<HttpHeader> headersList;
    public List<HttpFormData> formDataList;
    public List<HttpFormUrlencoded> urlencodedList;
    public List<HttpParam> paramsList;

    public boolean isMultipart;
    public boolean followRedirects = true; // 默认自动重定向
    public boolean cookieJarEnabled = true; // 默认启用 Cookie Jar
    public HttpRequestProxyPolicy proxyPolicy = HttpRequestProxyPolicy.DEFAULT; // 默认跟随全局代理设置
    public boolean sslVerificationEnabled = false; // 默认禁用 SSL 校验
    public String httpVersion = HttpRequestItem.HTTP_VERSION_AUTO; // HTTP 协议偏好
    public int requestTimeoutMs = 0; // 0 表示不超时
    public TransportAuth transportAuth; // 发送阶段需要的传输层认证元数据（例如 Digest challenge 认证）

    // 事件监听控制（精细化控制）
    public boolean collectBasicInfo = true; // 收集基本信息（headers、body），默认开启
    public boolean collectMetricsInfo = false; // 仅采集压测统计需要的轻量指标（时间戳、发送/接收字节）
    public boolean collectEventInfo = true; // 收集完整事件信息（DNS、连接、SSL等），默认开启
    public boolean enableNetworkLog = false; // 启用网络日志面板输出，默认关闭
    public transient NetworkLogSink networkLogSink = NetworkLogSink.noop(); // 网络日志输出端口，由 UI 层按需注入
    public transient HttpLifecycleLogSink lifecycleLogSink = HttpLifecycleLogSink.noop();
    public transient DownloadProgressSinkFactory downloadProgressSinkFactory = DownloadProgressSinkFactory.noop();
    public transient ResponseSizeLimitWarningSink responseSizeLimitWarningSink = ResponseSizeLimitWarningSink.noop();
    public boolean notifyCookieChanges = true; // 请求完成后是否发布 Cookie 变更事件
    public ResponseBodyMode responseBodyMode = ResponseBodyMode.FULL;
    public int responseBodyPreviewLimitBytes = 64 * 1024;

    // 脚本字段（已应用 group 继承）
    public String prescript;
    public String postscript;

    public List<HttpHeader> sentHeadersList; // 实际发送的请求头快照
    public String sentRequestBody; // 实际发送的请求体内容
    public transient volatile HttpEventInfo exchangeEventInfo; // 运行期事件信息，仅供 SSE/WS 异步回调读取，不参与持久化

    /**
     * 创建当前对象的浅拷贝
     * 注意：List 和 Map 对象本身不会被深拷贝，只是引用复制
     */
    public PreparedRequest shallowCopy() {
        PreparedRequest copy = new PreparedRequest();
        copy.id = this.id;
        copy.name = this.name;
        copy.url = this.url;
        copy.method = this.method;
        copy.sentHeadersList = this.sentHeadersList == null ? null : new ArrayList<>(this.sentHeadersList);
        // 单次发包的运行态 trace 不随请求配置复制，避免重定向/重发带上上一跳连接信息。
        copy.exchangeEventInfo = null;
        copy.body = this.body;
        copy.bodyType = this.bodyType;
        copy.sentRequestBody = this.sentRequestBody;
        copy.isMultipart = this.isMultipart;
        copy.followRedirects = this.followRedirects;
        copy.cookieJarEnabled = this.cookieJarEnabled;
        copy.proxyPolicy = this.proxyPolicy;
        copy.sslVerificationEnabled = this.sslVerificationEnabled;
        copy.httpVersion = this.httpVersion;
        copy.requestTimeoutMs = this.requestTimeoutMs;
        copy.transportAuth = this.transportAuth != null ? this.transportAuth.shallowCopy() : null;
        copy.collectBasicInfo = this.collectBasicInfo;
        copy.collectMetricsInfo = this.collectMetricsInfo;
        copy.collectEventInfo = this.collectEventInfo;
        copy.enableNetworkLog = this.enableNetworkLog;
        copy.networkLogSink = this.networkLogSink;
        copy.lifecycleLogSink = this.lifecycleLogSink;
        copy.downloadProgressSinkFactory = this.downloadProgressSinkFactory;
        copy.responseSizeLimitWarningSink = this.responseSizeLimitWarningSink;
        copy.notifyCookieChanges = this.notifyCookieChanges;
        copy.responseBodyMode = this.responseBodyMode;
        copy.responseBodyPreviewLimitBytes = this.responseBodyPreviewLimitBytes;
        copy.prescript = this.prescript;
        copy.postscript = this.postscript;
        copy.headersList = this.headersList == null ? null : new ArrayList<>(this.headersList);
        copy.formDataList = this.formDataList == null ? null : new ArrayList<>(this.formDataList);
        copy.urlencodedList = this.urlencodedList == null ? null : new ArrayList<>(this.urlencodedList);
        copy.paramsList = this.paramsList == null ? null : new ArrayList<>(this.paramsList);
        return copy;
    }

    /**
     * 简化对象，将渲染时不需要的字段置为 null，减少内存占用
     * 保留的字段：url, method, sentHeadersList, formDataList, urlencodedList, sentRequestBody
     * 置为 null 的字段：id, name, body, bodyType, transportAuth, headersList, paramsList, exchangeEventInfo
     */
    public void simplify() {
        this.id = null;
        this.name = null;
        this.body = null;
        this.bodyType = null;
        this.transportAuth = null;
        this.headersList = null;  // 渲染用的是 sentHeadersList
        this.paramsList = null;   // 渲染时不显示
        this.exchangeEventInfo = null;
        // isMultipart, followRedirects 和采集开关是基本类型，不占主要内存
    }

    public enum ResponseBodyMode {
        FULL,
        PREVIEW,
        METADATA_ONLY
    }
}
