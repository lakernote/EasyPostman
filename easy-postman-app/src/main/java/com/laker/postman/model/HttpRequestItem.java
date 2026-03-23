package com.laker.postman.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * HttpRequestItem 类表示一个HTTP请求的配置项
 * 包含请求的基本信息、头部、参数、认证等
 * 每个参数都要求有默认值，便于前端展示和编辑
 */
@Setter
@Getter
public class HttpRequestItem implements Serializable {
    public static final String HTTP_VERSION_AUTO = "AUTO";
    public static final String HTTP_VERSION_HTTP_1_1 = "HTTP_1_1";
    public static final String HTTP_VERSION_HTTP_2 = "HTTP_2";

    private String id = ""; // 唯一标识符
    private String name = ""; // 请求名称
    private String description = ""; // 请求描述（支持Markdown）
    private String url = ""; // 请求URL
    private String method = "GET"; // 请求方法（GET, POST, PUT, DELETE等）
    private RequestItemProtocolEnum protocol = RequestItemProtocolEnum.HTTP; // 协议类型，默认HTTP
    private List<HttpHeader> headersList = new ArrayList<>();
    private String bodyType = ""; // 请求体类型
    private String body = ""; // 请求体内容（如JSON、表单数据等）
    private List<HttpParam> paramsList = new ArrayList<>();
    private List<HttpFormData> formDataList = new ArrayList<>();
    private List<HttpFormUrlencoded> urlencodedList = new ArrayList<>();
    private String authType = AuthType.INHERIT.getConstant(); // 认证类型（inherit/none/basic/bearer），默认继承
    private String authUsername = ""; // Basic用户名
    private String authPassword = ""; // Basic密码
    private String authToken = "";    // Bearer Token
    private Boolean followRedirects; // 是否自动跟随重定向，null 表示跟随全局设置
    private Boolean cookieJarEnabled; // 是否启用 Cookie Jar，null 表示使用默认值
    private Boolean sslVerificationEnabled; // 是否启用 SSL 证书校验，null 表示跟随全局设置
    private String httpVersion = HTTP_VERSION_AUTO; // HTTP 协议偏好
    private Integer requestTimeoutMs; // 请求超时（毫秒），null 表示跟随全局设置
    // 前置脚本（请求前执行）
    private String prescript = "";
    // 后置脚本（响应后执行）
    private String postscript = "";
    // 保存的响应列表（类似 Postman 的 Examples/Response）
    private List<SavedResponse> response = new ArrayList<>();

    /**
     * 判断该请求是否为新建（未命名）请求
     */
    public boolean isNewRequest() {
        return name == null || name.trim().isEmpty();
    }

    public String resolveHttpVersion() {
        if (httpVersion == null || httpVersion.trim().isEmpty()) {
            return HTTP_VERSION_AUTO;
        }
        if (HTTP_VERSION_HTTP_1_1.equals(httpVersion) || HTTP_VERSION_HTTP_2.equals(httpVersion)) {
            return httpVersion;
        }
        return HTTP_VERSION_AUTO;
    }

}
