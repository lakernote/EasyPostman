package com.laker.postman.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_NONE;

/**
 * RequestGroup 类表示一个请求分组的配置项
 * 包含分组的名称、认证、脚本等信息
 * 分组级别的认证和脚本会被其下的请求继承
 */
@Getter
@Setter
public class RequestGroup implements Serializable {
    private String id = ""; // 唯一标识符
    private String name = ""; // 分组名称
    private String authType = AUTH_TYPE_NONE; // 认证类型（none/basic/bearer）
    private String authUsername = ""; // Basic用户名
    private String authPassword = ""; // Basic密码
    private String authToken = "";    // Bearer Token
    // 前置脚本（请求前执行）
    private String prescript = "";
    // 后置脚本（响应后执行）
    private String postscript = "";

    public RequestGroup() {
        this.id = UUID.randomUUID().toString();
    }

    public RequestGroup(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    /**
     * 判断分组是否有认证配置
     */
    public boolean hasAuth() {
        return authType != null && !AUTH_TYPE_NONE.equals(authType);
    }

    /**
     * 判断分组是否有前置脚本
     */
    public boolean hasPreScript() {
        return prescript != null && !prescript.trim().isEmpty();
    }

    /**
     * 判断分组是否有后置脚本
     */
    public boolean hasPostScript() {
        return postscript != null && !postscript.trim().isEmpty();
    }
}

