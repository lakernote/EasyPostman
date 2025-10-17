package com.laker.postman.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_NONE;

/**
 * HttpRequestItem 类表示一个HTTP请求的配置项
 * 包含请求的基本信息、头部、参数、认证等
 * 每个参数都要求有默认值，便于前端展示和编辑
 */
@Getter
@Setter
public class HttpRequestItem implements Serializable {
    private String id = ""; // 唯一标识符
    private String name = ""; // 请求名称
    private String url = ""; // 请求URL
    private String method = "GET"; // 请求方法（GET, POST, PUT, DELETE等）
    private RequestItemProtocolEnum protocol = RequestItemProtocolEnum.HTTP; // 协议类型，默认HTTP
    // 新版本：带启用状态的 headers（优先使用）
    private List<HttpHeader> headersList = new ArrayList<>();
    // 旧版本：仅保留用于向后兼容（从旧数据迁移）
    private Map<String, String> headers = new LinkedHashMap<>(); // 请求头

    private String bodyType = ""; // 请求体类型
    private String body = ""; // 请求体内容（如JSON、表单数据等）
    // 新版本：带启用状态的 params（优先使用）
    private List<HttpParam> paramsList = new ArrayList<>();
    // 旧版本：仅保留用于向后兼容（从旧数据迁移）
    private Map<String, String> params = new LinkedHashMap<>(); // 请求参数（查询字符串）

    // 新版本：带启用状态的 form-data（优先使用）
    private List<HttpFormData> formDataList = new ArrayList<>();
    // form-data 普通字段（旧版本，向后兼容）
    private Map<String, String> formData = new LinkedHashMap<>();
    // form-data 文件字段（key=字段名，value=文件绝对路径）
    private Map<String, String> formFiles = new LinkedHashMap<>();

    // 新版本：带启用状态的 urlencoded（优先使用）
    private List<HttpFormUrlencoded> urlencodedList = new ArrayList<>();
    // 旧版本：仅保留用于向后兼容（从旧数据迁移）
    private Map<String, String> urlencoded = new LinkedHashMap<>();

    // 认证相关字段
    private String authType = AUTH_TYPE_NONE; // 认证类型（none/basic/bearer）
    private String authUsername = ""; // Basic用户名
    private String authPassword = ""; // Basic密码
    private String authToken = "";    // Bearer Token

    // 前置脚本（请求前执行）
    private String prescript = "";
    // 后置脚本（响应后执行）
    private String postscript = "";

    /**
     * 判断该请求是否为新建（未命名）请求
     */
    public boolean isNewRequest() {
        return name == null || name.trim().isEmpty();
    }
}