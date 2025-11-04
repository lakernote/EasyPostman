package com.laker.postman.service.update.source;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * 抽象更新源基类 - 提供通用的 HTTP 连接和响应处理逻辑
 *
 * <p>子类只需实现 getApiUrl()、getAllReleasesApiUrl()、getWebUrl() 和 getName() 方法即可。</p>
 */
@Slf4j
public abstract class AbstractUpdateSource implements UpdateSource {

    /** 连接超时时间（毫秒） */
    protected static final int CONNECTION_TIMEOUT = 2500;

    /** 读取超时时间（毫秒） */
    protected static final int READ_TIMEOUT = 2500;

    /** 获取发布信息时的连接超时时间（毫秒） */
    protected static final int FETCH_CONNECTION_TIMEOUT = 10000;

    /** 获取发布信息时的读取超时时间（毫秒） */
    protected static final int FETCH_READ_TIMEOUT = 10000;

    /** 源标识键 */
    protected static final String SOURCE_KEY = "_source";

    @Override
    public long testConnectionSpeed() {
        long startTime = System.currentTimeMillis();
        HttpURLConnection conn = null;
        try {
            URL url = new URL(getApiUrl());
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECTION_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", buildUserAgent());
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Connection", "close");

            int code = conn.getResponseCode();
            long responseTime = System.currentTimeMillis() - startTime;

            if (code == 200 || code == 301 || code == 302) {
                log.debug("{} connection test successful: {} ms (HTTP {})", getName(), responseTime, code);
                return responseTime;
            } else {
                log.debug("{} connection test failed with code: {}", getName(), code);
                return Long.MAX_VALUE;
            }
        } catch (java.net.SocketTimeoutException e) {
            long failedTime = System.currentTimeMillis() - startTime;
            log.debug("{} connection timeout after {} ms", getName(), failedTime);
            return Long.MAX_VALUE;
        } catch (Exception e) {
            long failedTime = System.currentTimeMillis() - startTime;
            log.debug("{} connection test failed after {} ms: {}", getName(), failedTime, e.getMessage());
            return Long.MAX_VALUE;
        } finally {
            closeConnection(conn);
        }
    }

    @Override
    public JSONObject fetchLatestReleaseInfo() {
        return fetchReleaseInfo(getApiUrl());
    }

    @Override
    public JSONArray fetchAllReleases(int limit) {
        HttpURLConnection conn = null;
        try {
            String url = getAllReleasesApiUrl();
            if (limit > 0) {
                url += (url.contains("?") ? "&" : "?") + "per_page=" + limit;
            }

            URL apiUrl = new URL(url);
            conn = (HttpURLConnection) apiUrl.openConnection();
            conn.setConnectTimeout(FETCH_CONNECTION_TIMEOUT);
            conn.setReadTimeout(FETCH_READ_TIMEOUT);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", buildUserAgent());
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setRequestProperty("Cache-Control", "no-cache");

            int code = conn.getResponseCode();
            if (code == 200) {
                String json = readResponse(conn);
                JSONArray releases = new JSONArray(json);
                log.info("Successfully fetched {} releases from {}", releases.size(), getName());
                return releases;
            } else {
                String errorResponse = readErrorResponse(conn);
                log.warn("Failed to fetch releases from {}, HTTP code: {}, response: {}",
                        getName(), code, errorResponse);
                return null;
            }
        } catch (Exception e) {
            log.debug("Error fetching releases from {}: {}", getName(), e.getMessage());
            return null;
        } finally {
            closeConnection(conn);
        }
    }

    /**
     * 获取发布信息
     */
    protected JSONObject fetchReleaseInfo(String apiUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(FETCH_CONNECTION_TIMEOUT);
            conn.setReadTimeout(FETCH_READ_TIMEOUT);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", buildUserAgent());
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setRequestProperty("Cache-Control", "no-cache");

            int code = conn.getResponseCode();
            if (code == 200) {
                String json = readResponse(conn);
                JSONObject releaseInfo = new JSONObject(json);
                releaseInfo.set(SOURCE_KEY, getName());
                log.info("Successfully fetched release info from {}", getName());
                return releaseInfo;
            } else {
                String errorResponse = readErrorResponse(conn);
                log.warn("Failed to fetch release info from {}, HTTP code: {}, response: {}",
                        getName(), code, errorResponse);
                return null;
            }
        } catch (Exception e) {
            log.debug("Error fetching release info from {}: {}", getName(), e.getMessage());
            return null;
        } finally {
            closeConnection(conn);
        }
    }

    /**
     * 读取成功响应
     */
    protected String readResponse(HttpURLConnection conn) throws Exception {
        try (InputStream is = conn.getInputStream();
             Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {
            return scanner.useDelimiter("\\A").next();
        }
    }

    /**
     * 读取错误响应
     */
    protected String readErrorResponse(HttpURLConnection conn) {
        try (InputStream errorStream = conn.getErrorStream()) {
            if (errorStream != null) {
                try (Scanner scanner = new Scanner(errorStream, StandardCharsets.UTF_8)) {
                    if (scanner.hasNext()) {
                        return scanner.useDelimiter("\\A").next();
                    }
                }
            }
        } catch (Exception ignored) {
            // 忽略读取错误响应的异常
        }
        return "";
    }

    /**
     * 构建 User-Agent 字符串
     */
    protected String buildUserAgent() {
        return "Mozilla/5.0 (compatible; EasyPostman/" + SystemUtil.getCurrentVersion() + ")";
    }

    /**
     * 关闭连接
     */
    protected void closeConnection(HttpURLConnection conn) {
        if (conn != null) {
            try {
                conn.disconnect();
            } catch (Exception ignored) {
                // 忽略断开连接时的异常
            }
        }
    }
}

