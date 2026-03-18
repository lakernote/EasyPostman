package com.laker.postman.service.common;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.laker.postman.model.RequestGroup;
import lombok.Data;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.*;

/**
 * 认证解析工具类
 * 提供统一的认证信息解析逻辑，供各个 Parser 复用
 */
@Slf4j
@UtilityClass
public class AuthParserUtil {

    // 常量定义
    private static final String AUTH_TYPE_BASIC = "basic";
    private static final String AUTH_TYPE_BEARER = "bearer";
    private static final String KEY_VALUE = "value";

    /**
     * Basic认证凭据
     */
    @Data
    public static class BasicAuthCredentials {
        private final String username;
        private final String password;
    }

    /**
     * 从 Postman 格式的 auth 对象解析到 RequestGroup
     *
     * @param auth Postman auth JSON 对象
     * @param group 目标分组对象
     */
    public static void parsePostmanAuthToGroup(JSONObject auth, RequestGroup group) {
        String authType = auth.getStr("type", "");
        if (AUTH_TYPE_BASIC.equals(authType)) {
            group.setAuthType(AUTH_TYPE_BASIC);
            JSONArray basicArr = auth.getJSONArray(AUTH_TYPE_BASIC);
            String username = null;
            String password = null;
            if (basicArr != null) {
                for (Object o : basicArr) {
                    JSONObject oObj = (JSONObject) o;
                    if ("username".equals(oObj.getStr("key"))) {
                        username = oObj.getStr(KEY_VALUE, "");
                    }
                    if ("password".equals(oObj.getStr("key"))) {
                        password = oObj.getStr(KEY_VALUE, "");
                    }
                }
            }
            group.setAuthUsername(username);
            group.setAuthPassword(password);
        } else if (AUTH_TYPE_BEARER.equals(authType)) {
            group.setAuthType(AUTH_TYPE_BEARER);
            JSONArray bearerArr = auth.getJSONArray(AUTH_TYPE_BEARER);
            if (bearerArr != null && !bearerArr.isEmpty()) {
                for (Object o : bearerArr) {
                    JSONObject oObj = (JSONObject) o;
                    if ("token".equals(oObj.getStr("key"))) {
                        group.setAuthToken(oObj.getStr(KEY_VALUE, ""));
                    }
                }
            }
        }
    }

    /**
     * 从 ApiPost 格式的 auth 对象解析到 RequestGroup
     *
     * @param auth ApiPost auth JSON 对象
     * @param group 目标分组对象
     */
    public static void parseApiPostAuthToGroup(JSONObject auth, RequestGroup group) {
        String authType = auth.getStr("type", "");
        if (AUTH_TYPE_BASIC.equals(authType)) {
            group.setAuthType(AUTH_TYPE_BASIC);
            JSONObject basic = auth.getJSONObject(AUTH_TYPE_BASIC);
            if (basic != null) {
                group.setAuthUsername(basic.getStr("username", ""));
                group.setAuthPassword(basic.getStr("password", ""));
            }
        } else if (AUTH_TYPE_BEARER.equals(authType)) {
            group.setAuthType(AUTH_TYPE_BEARER);
            JSONObject bearer = auth.getJSONObject(AUTH_TYPE_BEARER);
            if (bearer != null) {
                group.setAuthToken(bearer.getStr("key", ""));
            }
        } else if ("inherit".equals(authType)) {
            group.setAuthType(AUTH_TYPE_INHERIT);
        } else {
            group.setAuthType(AUTH_TYPE_NONE);
        }
    }

    /**
     * 从 Basic Authorization 头部解析凭据
     * 支持 Base64 编码和变量格式
     *
     * @param authValue Authorization 头部值（例如："Basic dXNlcm5hbWU6cGFzc3dvcmQ="）
     * @return Basic认证凭据，如果解析失败返回null
     */
    public static BasicAuthCredentials parseBasicAuthHeader(String authValue) {
        if (authValue == null || !authValue.startsWith("Basic ")) {
            return null;
        }

        String credentials = authValue.substring(6).trim();

        // 检查是否是变量占位符格式：Basic {{username}} {{password}}
        if (credentials.contains("{{") && credentials.contains("}}")) {
            // 变量格式
            String[] parts = credentials.split("\\s+");
            if (parts.length >= 2) {
                // 格式：Basic {{username}} {{password}}
                return new BasicAuthCredentials(parts[0], parts[1]);
            } else {
                // 格式：Basic {{username}}:{{password}}
                int colonIndex = credentials.indexOf(':');
                if (colonIndex > 0) {
                    return new BasicAuthCredentials(
                        credentials.substring(0, colonIndex),
                        credentials.substring(colonIndex + 1)
                    );
                } else {
                    return new BasicAuthCredentials(credentials, "");
                }
            }
        } else {
            // Base64 编码格式
            try {
                String decoded = new String(Base64.getDecoder().decode(credentials));
                String[] parts = decoded.split(":", 2);
                if (parts.length == 2) {
                    return new BasicAuthCredentials(parts[0], parts[1]);
                } else {
                    return new BasicAuthCredentials(decoded, "");
                }
            } catch (Exception e) {
                log.warn("解析 Basic 认证失败，可能是变量格式", e);
                return new BasicAuthCredentials(credentials, "");
            }
        }
    }
}
