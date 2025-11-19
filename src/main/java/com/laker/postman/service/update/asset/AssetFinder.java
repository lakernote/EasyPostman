package com.laker.postman.service.update.asset;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;

/**
 * 资源查找器 - 从发布信息中提取合适的下载链接
 */
@Slf4j
public class AssetFinder {

    private static final String BROWSER_DOWNLOAD_URL = "browser_download_url";

    /**
     * 查找 Fat JAR 资源
     *
     * @param assets 资源列表
     * @return 下载 URL，未找到返回 null
     */
    public String findFatJar(JSONArray assets) {
        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getStr("name");
            if (name != null && name.startsWith("easy-postman-") &&
                    name.endsWith(".jar") &&
                    !name.contains("-sources") &&
                    !name.contains("-javadoc")) {
                String url = asset.getStr(BROWSER_DOWNLOAD_URL);
                log.debug("Found fat JAR asset: {} -> {}", name, url);
                return url;
            }
        }
        log.debug("No fat JAR asset found in release");
        return null;
    }

    /**
     * 根据扩展名查找资源
     *
     * @param assets 资源列表
     * @param extension 文件扩展名（如 ".dmg", ".exe"）
     * @return 下载 URL，未找到返回 null
     */
    public String findByExtension(JSONArray assets, String extension) {
        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getStr("name");
            if (name != null && name.endsWith(extension)) {
                String url = asset.getStr(BROWSER_DOWNLOAD_URL);
                log.debug("Found asset: {} -> {}", name, url);
                return url;
            }
        }
        return null;
    }

    /**
     * 根据模式查找资源
     *
     * @param assets 资源列表
     * @param pattern 匹配模式（如 "-portable.zip"）
     * @return 下载 URL，未找到返回 null
     */
    public String findByPattern(JSONArray assets, String pattern) {
        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getStr("name");
            if (name != null && name.contains(pattern)) {
                String url = asset.getStr(BROWSER_DOWNLOAD_URL);
                log.debug("Found asset by pattern '{}': {} -> {}", pattern, name, url);
                return url;
            }
        }
        return null;
    }

    /**
     * 查找通用 DMG 文件（不包含架构后缀）
     *
     * @param assets 资源列表
     * @return 下载 URL，未找到返回 null
     */
    public String findGenericDmg(JSONArray assets) {
        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getStr("name");
            if (name != null && name.endsWith(".dmg") &&
                    !name.endsWith("-intel.dmg") &&
                    !name.endsWith("-arm64.dmg") &&
                    !name.endsWith("-macos-x86_64.dmg") &&
                    !name.endsWith("-macos-arm64.dmg")) {
                String url = asset.getStr(BROWSER_DOWNLOAD_URL);
                log.debug("Found generic DMG (without architecture suffix): {} -> {}", name, url);
                return url;
            }
        }
        return null;
    }
}

