package com.laker.postman.model;

import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_NONE;
import static java.util.stream.Collectors.toMap;

/**
 * HttpRequestItem 类表示一个HTTP请求的配置项
 * 包含请求的基本信息、头部、参数、认证等
 * 每个参数都要求有默认值，便于前端展示和编辑
 */
@Getter
public class HttpRequestItem implements Serializable {
    private String id = ""; // 唯一标识符
    private String name = ""; // 请求名称
    private String url = ""; // 请求URL
    private String method = "GET"; // 请求方法（GET, POST, PUT, DELETE等）
    private RequestItemProtocolEnum protocol = RequestItemProtocolEnum.HTTP; // 协议类型，默认HTTP
    // 新版本：带启用状态的 headers（优先使用）
    private List<HttpHeader> headersList = new ArrayList<>();

    private String bodyType = ""; // 请求体类型
    private String body = ""; // 请求体内容（如JSON、表单数据等）
    // 新版本：带启用状态的 params（优先使用）
    private List<HttpParam> paramsList = new ArrayList<>();

    // 新版本：带启用状态的 form-data（优先使用）
    private List<HttpFormData> formDataList = new ArrayList<>();

    // 新版本：带启用状态的 urlencoded（优先使用）
    private List<HttpFormUrlencoded> urlencodedList = new ArrayList<>();

    // 认证相关字段
    private String authType = AUTH_TYPE_NONE; // 认证类型（none/basic/bearer）
    private String authUsername = ""; // Basic用户名
    private String authPassword = ""; // Basic密码
    private String authToken = "";    // Bearer Token

    // 前置脚本（请求前执行）
    private String prescript = "";
    // 后置脚本（响应后执行）
    private String postscript = "";

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setProtocol(RequestItemProtocolEnum protocol) {
        this.protocol = protocol;
    }

    public void setHeadersList(List<HttpHeader> headersList) {
        this.headersList = headersList;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setParamsList(List<HttpParam> paramsList) {
        this.paramsList = paramsList;
    }

    public void setFormDataList(List<HttpFormData> formDataList) {
        this.formDataList = formDataList;
    }

    public void setUrlencodedList(List<HttpFormUrlencoded> urlencodedList) {
        this.urlencodedList = urlencodedList;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public void setPrescript(String prescript) {
        this.prescript = prescript;
    }

    public void setPostscript(String postscript) {
        this.postscript = postscript;
    }

    /**
     * 判断该请求是否为新建（未命名）请求
     */
    public boolean isNewRequest() {
        return name == null || name.trim().isEmpty();
    }

    public Map<String, String> getHeaders() {
        return headersList.stream()
                .filter(HttpHeader::isEnabled)
                .collect(toMap(
                        HttpHeader::getKey,
                        HttpHeader::getValue,
                        (v1, v2) -> v2,
                        LinkedHashMap::new
                ));
    }

    public Map<String, String> getParams() {
        return paramsList.stream()
                .filter(HttpParam::isEnabled)
                .collect(toMap(
                        HttpParam::getKey,
                        HttpParam::getValue,
                        (v1, v2) -> v2,
                        LinkedHashMap::new
                ));
    }

    public Map<String, String> getFormData() {
        return formDataList.stream()
                .filter(HttpFormData::isEnabled)
                .filter(data -> "text".equals(data.getType()))
                .collect(toMap(
                        HttpFormData::getKey,
                        HttpFormData::getValue,
                        (v1, v2) -> v2,
                        LinkedHashMap::new
                ));
    }

    public Map<String, String> getFormFiles() {
        return formDataList.stream()
                .filter(HttpFormData::isEnabled)
                .filter(data -> "file".equals(data.getType()))
                .collect(toMap(
                        HttpFormData::getKey,
                        HttpFormData::getValue,
                        (v1, v2) -> v2,
                        LinkedHashMap::new
                ));
    }

    public Map<String, String> getUrlencoded() {
        return urlencodedList.stream()
                .filter(HttpFormUrlencoded::isEnabled)
                .collect(toMap(
                        HttpFormUrlencoded::getKey,
                        HttpFormUrlencoded::getValue,
                        (v1, v2) -> v2,
                        LinkedHashMap::new
                ));
    }

}