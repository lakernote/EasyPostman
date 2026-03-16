package com.laker.postman.plugin.manager;

/**
 * 当前已安装插件的来源元数据。
 */
public record PluginInstallSource(
        String type,
        String location
) {

    public static final String TYPE_LOCAL = "local";
    public static final String TYPE_MARKET = "market";

    public boolean hasLocation() {
        return location != null && !location.isBlank();
    }

    public boolean isLocal() {
        return TYPE_LOCAL.equalsIgnoreCase(type);
    }

    public boolean isMarket() {
        return TYPE_MARKET.equalsIgnoreCase(type);
    }
}
