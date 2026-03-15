package com.laker.postman.service.http;

import com.laker.postman.model.HttpParam;
import lombok.experimental.UtilityClass;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * URL 构建工具类
 *
 * <p>职责划分：
 * <ul>
 *   <li>{@link #buildEncodedUrl} — 发请求前调用，把 URL + paramsList 合并并做完整 URL 编码，生成可直接发送的合法 URL</li>
 *   <li>{@link #extractBaseUri} — 提取协议+host+port，用于 OkHttpClient 连接池复用</li>
 * </ul>
 *
 * <p>UI 层（Params ↔ URL 栏双向联动）不使用本类，使用 {@link HttpUtil#buildUrlFromParamsList} /
 * {@link HttpUtil#getParamsListFromUrl}，这两个方法不做编码，保持原始可读文本。
 */
@UtilityClass
public class HttpRequestUtil {

    /**
     * 构建发请求用的完整编码 URL。
     *
     * <p>步骤：
     * <ol>
     *   <li>对 URL 中已有的 query string 做编码（空格 → %20 等）</li>
     *   <li>把 paramsList 中启用且不与 URL 已有 key 重复的参数追加到 URL 末尾并编码</li>
     * </ol>
     *
     * @param rawUrl     原始 URL（可含未编码的空格等字符，可含 ? 已有参数）
     * @param paramsList 来自 Params 面板的参数列表（已完成变量替换）
     * @return 编码后的完整 URL，可直接传给 OkHttp
     */
    public static String buildEncodedUrl(String rawUrl, List<HttpParam> paramsList) {
        if (rawUrl == null) return "";

        // 第一步：对 URL 中已有的 query string 做编码
        String encodedUrl = encodeExistingQueryString(rawUrl);

        // 第二步：追加 paramsList 中的参数（跳过与 URL 中已有 key 重复的）
        if (paramsList == null || paramsList.isEmpty()) {
            return encodedUrl;
        }

        // 提取 URL 中已有的 key（已编码形式），用于去重
        boolean hasQuery = encodedUrl.contains("?");
        Set<String> existingKeys = extractUrlParamKeys(encodedUrl, hasQuery);

        StringBuilder sb = new StringBuilder(encodedUrl);
        for (HttpParam param : paramsList) {
            if (!param.isEnabled()) continue;
            String key = param.getKey();
            if (key == null || key.isEmpty()) continue;
            // 跳过与 URL 已有参数重复的 key（URL 中的优先，paramsList 不覆盖）
            if (existingKeys.contains(HttpUtil.encodeURIComponent(key))) continue;

            sb.append(hasQuery ? "&" : "?");
            hasQuery = true;
            sb.append(HttpUtil.encodeURIComponent(key))
              .append("=")
              .append(HttpUtil.encodeURIComponent(param.getValue() != null ? param.getValue() : ""));
        }
        return sb.toString();
    }

    /**
     * 对 URL 中已有的 query string 做编码，base URL 部分不变。
     * 幂等：已编码的 %XX 不会被二次编码。
     */
    private static String encodeExistingQueryString(String url) {
        if (url == null || !url.contains("?")) return url;
        int idx = url.indexOf('?');
        String baseUrl = url.substring(0, idx);
        String paramStr = url.substring(idx + 1);
        StringBuilder sb = new StringBuilder(baseUrl).append("?");
        String[] pairs = paramStr.split("&", -1);
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            int eqIdx = pair.indexOf('=');
            if (eqIdx > 0) {
                sb.append(HttpUtil.encodeURIComponent(pair.substring(0, eqIdx)))
                  .append("=")
                  .append(HttpUtil.encodeURIComponent(pair.substring(eqIdx + 1)));
            } else if (!pair.isEmpty()) {
                sb.append(HttpUtil.encodeURIComponent(pair));
            }
            if (i < pairs.length - 1) sb.append("&");
        }
        return sb.toString();
    }

    /**
     * 从 URL 中提取已有的 query param key 集合，用于去重。
     */
    static Set<String> extractUrlParamKeys(String url, boolean hasQuestionMark) {
        Set<String> keys = new LinkedHashSet<>();
        if (!hasQuestionMark) return keys;
        String paramStr = url.substring(url.indexOf('?') + 1);
        for (String pair : paramStr.split("&", -1)) {
            int eqIdx = pair.indexOf('=');
            String k = eqIdx > 0 ? pair.substring(0, eqIdx) : pair;
            if (!k.isEmpty()) keys.add(k);
        }
        return keys;
    }

    /**
     * 提取 URL 的基本 URI（协议 + 主机 + 端口），用于 OkHttpClient 连接池复用。
     * 端口为 -1 时补全默认端口，确保与 Chrome 行为一致。
     */
    public static String extractBaseUri(String urlString) {
        try {
            URI uri = URI.create(urlString);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            int defaultPort = "https".equals(scheme) ? 443 : 80;
            int usePort = (port == -1) ? defaultPort : port;
            String portPart = (port == -1
                    || ("http".equals(scheme) && usePort == 80)
                    || ("https".equals(scheme) && usePort == 443))
                    ? "" : (":" + usePort);
            return scheme + "://" + host + portPart;
        } catch (Exception e) {
            return urlString;
        }
    }
}