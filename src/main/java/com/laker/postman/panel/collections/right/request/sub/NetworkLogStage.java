package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.constants.ModernColors;
import lombok.Getter;

import java.awt.*;

/**
 * 网络日志阶段枚举
 * 统一管理日志阶段的 emoji 图标、颜色方案和粗体配置
 */
@Getter
public enum NetworkLogStage {
    // ==================== 错误和失败（红色系，粗体）====================
    // 亮色主题使用深红色(239,68,68)，暗色主题使用亮红色(255,130,130)
    FAILED("Failed", "❌", new Color(239, 68, 68), new Color(255, 130, 130), true),
    CALL_FAILED("CallFailed", "💥", new Color(239, 68, 68), new Color(255, 130, 130), true),
    REQUEST_FAILED("RequestFailed", "❌", new Color(239, 68, 68), new Color(255, 130, 130), true),
    RESPONSE_FAILED("ResponseFailed", "❌", new Color(239, 68, 68), new Color(255, 130, 130), true),
    CONNECT_FAILED("ConnectFailed", "⚠️", new Color(239, 68, 68), new Color(255, 130, 130), true),
    CANCELED("Canceled", "🚫", new Color(239, 68, 68), new Color(255, 130, 130), true),

    // ==================== 成功和完成（绿色系）====================
    // 亮色主题使用深绿色(34,197,94)，暗色主题使用亮绿色(140,220,140)
    CALL_START("CallStart", "🚀", new Color(34, 197, 94), new Color(140, 220, 140), true),
    CALL_END("CallEnd", "✅", new Color(34, 197, 94), new Color(140, 220, 140), true),
    CACHE_HIT("CacheHit", "💾", new Color(34, 197, 94), new Color(140, 220, 140), false),
    CACHE_MISS("CacheMiss", "❌", new Color(6, 182, 212), new Color(140, 210, 230), false),
    CACHE_CONDITIONAL_HIT("CacheConditionalHit", "💾", new Color(6, 182, 212), new Color(140, 210, 230), false),
    SATISFACTION_FAILURE("SatisfactionFailure", "⚠️", new Color(245, 158, 11), new Color(255, 200, 100), false),

    // ==================== 安全连接（紫色系）====================
    // 亮色主题使用深紫色，暗色主题使用亮紫色
    SECURE_CONNECT_START("SecureConnectStart", "🔐", new Color(111, 66, 193), new Color(210, 160, 230), false),
    SECURE_CONNECT_END("SecureConnectEnd", "🔒", new Color(111, 66, 193), new Color(210, 160, 230), false),

    // ==================== 连接相关（蓝色系）====================
    // 亮色主题使用深蓝色(0,122,255)，暗色主题使用亮蓝色(140,180,255)
    CONNECT_START("ConnectStart", "🔌", new Color(0, 122, 255), new Color(140, 180, 255), false),
    CONNECT_END("ConnectEnd", "✔️", new Color(0, 122, 255), new Color(140, 180, 255), false),
    CONNECTION_ACQUIRED("ConnectionAcquired", "🔗", new Color(0, 122, 255), new Color(140, 180, 255), false),
    CONNECTION_RELEASED("ConnectionReleased", "🔓", new Color(0, 122, 255), new Color(140, 180, 255), false),

    // ==================== DNS（蓝色系）====================
    DNS_START("DnsStart", "🔍", new Color(0, 122, 255), new Color(140, 180, 255), false),
    DNS_END("DnsEnd", "📍", new Color(0, 122, 255), new Color(140, 180, 255), false),

    // ==================== 代理（蓝色系）====================
    PROXY_SELECT("ProxySelect", "🌐", new Color(0, 122, 255), new Color(140, 180, 255), false),
    PROXY_SELECT_START("ProxySelectStart", "🌐", new Color(0, 122, 255), new Color(140, 180, 255), false),
    PROXY_SELECT_END("ProxySelectEnd", "🌐", new Color(0, 122, 255), new Color(140, 180, 255), false),

    // ==================== 请求（橙色系）====================
    // 保持原有的橙色方案
    REQUEST_HEADERS_START("RequestHeadersStart", "📤", new Color(220, 160, 100), new Color(255, 190, 130), false),
    REQUEST_HEADERS_END("RequestHeadersEnd", "📨", new Color(220, 160, 100), new Color(255, 190, 130), false),
    REQUEST_BODY_START("RequestBodyStart", "📦", new Color(220, 160, 100), new Color(255, 190, 130), false),
    REQUEST_BODY_END("RequestBodyEnd", "✔️", new Color(220, 160, 100), new Color(255, 190, 130), false),

    // ==================== 响应（青色系）====================
    // 亮色主题使用深青色(6,182,212)，暗色主题使用亮青色(140,210,230)
    RESPONSE_HEADERS_START("ResponseHeadersStart", "📥", new Color(6, 182, 212), new Color(140, 210, 230), false),
    RESPONSE_HEADERS_END("ResponseHeadersEnd", "📬", new Color(6, 182, 212), new Color(140, 210, 230), false),
    RESPONSE_HEADERS_END_REDIRECT("ResponseHeadersEnd:Redirect", "🔀", new Color(245, 158, 11), new Color(255, 200, 100), true),
    RESPONSE_BODY_START("ResponseBodyStart", "📄", new Color(6, 182, 212), new Color(140, 210, 230), false),
    RESPONSE_BODY_END("ResponseBodyEnd", "✔️", new Color(6, 182, 212), new Color(140, 210, 230), false),

    // ==================== 重定向（橙色，粗体）====================
    // 亮色主题使用深橙色(245,158,11)，暗色主题使用亮橙色(255,200,100)
    REDIRECT("Redirect", "↪️", new Color(245, 158, 11), new Color(255, 200, 100), true),

    // ==================== 默认 ====================
    // 使用主题适配的文本颜色：亮色主题深色文字，暗色主题浅色文字
    DEFAULT("Default", "📋", new Color(15, 23, 42), new Color(241, 245, 249), false);

    private final String stageName;
    private final String emoji;
    private final Color lightThemeColor;
    private final Color darkThemeColor;
    private final boolean bold;

    NetworkLogStage(String stageName, String emoji, Color lightThemeColor, Color darkThemeColor, boolean bold) {
        this.stageName = stageName;
        this.emoji = emoji;
        this.lightThemeColor = lightThemeColor;
        this.darkThemeColor = darkThemeColor;
        this.bold = bold;
    }

    /**
     * 获取当前主题适配的颜色
     */
    public Color getColor() {
        return ModernColors.isDarkTheme() ? darkThemeColor : lightThemeColor;
    }

    /**
     * 判断是否为失败或错误类型
     */
    public boolean isError() {
        return this == FAILED || this == CALL_FAILED || this == REQUEST_FAILED
                || this == RESPONSE_FAILED || this == CONNECT_FAILED || this == CANCELED;
    }

    /**
     * 判断是否为成功类型
     */
    public boolean isSuccess() {
        return this == CALL_END || this == CACHE_HIT;
    }
}

