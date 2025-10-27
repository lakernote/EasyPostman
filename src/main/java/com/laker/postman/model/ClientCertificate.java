package com.laker.postman.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 客户端证书配置
 * 用于支持 mTLS (mutual TLS) 认证
 */
@Data
public class ClientCertificate implements Serializable {
    private String id; // 唯一标识符
    private String name; // 证书配置名称（可选）
    private String host; // 主机名或域名匹配模式（支持通配符，如 *.example.com）
    private int port; // 端口号，0 表示匹配所有端口
    private String certPath; // 证书文件路径（PFX/P12 或 PEM 格式）
    private String certType; // 证书类型：PFX, PEM
    private String certPassword; // 证书密码（用于 PFX/P12）
    private String keyPath; // 私钥文件路径（PEM 格式时使用）
    private String keyPassword; // 私钥密码（可选）
    private boolean enabled; // 是否启用此证书配置
    private long createdAt; // 创建时间
    private long updatedAt; // 更新时间

    // 证书类型常量
    public static final String CERT_TYPE_PFX = "PFX";
    public static final String CERT_TYPE_PEM = "PEM";

    public ClientCertificate() {
        this.enabled = true;
        this.port = 0; // 默认匹配所有端口
        this.certType = CERT_TYPE_PFX;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 检查此证书是否匹配指定的主机和端口
     */
    public boolean matches(String targetHost, int targetPort) {
        if (!enabled) {
            return false;
        }

        // 检查端口匹配
        if (port != 0 && port != targetPort) {
            return false;
        }

        // 检查主机名匹配
        if (host == null || host.trim().isEmpty()) {
            return false;
        }

        String pattern = host.trim().toLowerCase();
        String target = targetHost.toLowerCase();

        // 支持通配符匹配
        if (pattern.startsWith("*.")) {
            String domain = pattern.substring(2);
            // 匹配 "api.example.com" 或 "example.com" 本身
            return target.endsWith("." + domain) || target.equals(domain);
        }

        // 精确匹配
        return pattern.equals(target);
    }
}

