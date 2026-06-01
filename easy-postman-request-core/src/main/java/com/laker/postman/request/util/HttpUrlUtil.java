package com.laker.postman.request.util;

import com.laker.postman.request.model.HttpParam;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@UtilityClass
public class HttpUrlUtil {

    public static String buildEncodedUrl(String rawUrl, List<HttpParam> paramsList) {
        if (rawUrl == null) {
            return "";
        }

        String encodedUrl = encodeExistingQueryString(rawUrl);
        if (paramsList == null || paramsList.isEmpty()) {
            return encodedUrl;
        }

        boolean hasQuery = encodedUrl.contains("?");
        Set<String> existingKeys = extractQueryParamKeys(encodedUrl, hasQuery);

        StringBuilder sb = new StringBuilder(encodedUrl);
        for (HttpParam param : paramsList) {
            if (!param.isEnabled()) {
                continue;
            }
            String key = param.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            if (existingKeys.contains(encodeComponent(key))) {
                continue;
            }

            sb.append(hasQuery ? "&" : "?");
            hasQuery = true;
            sb.append(encodeComponent(key))
                    .append("=")
                    .append(encodeComponent(param.getValue() != null ? param.getValue() : ""));
        }
        return sb.toString();
    }

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

    public static String encodeComponent(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < value.length()) {
            int codePoint = value.codePointAt(i);
            char c = value.charAt(i);
            int charCount = Character.charCount(codePoint);
            if (c == '%' && i + 2 < value.length()
                    && isHexChar(value.charAt(i + 1))
                    && isHexChar(value.charAt(i + 2))) {
                sb.append(value, i, i + 3);
                i += 3;
            } else if (shouldPercentEncode(codePoint)) {
                appendPercentEncodedUtf8(sb, new String(Character.toChars(codePoint)));
                i += charCount;
            } else {
                sb.appendCodePoint(codePoint);
                i += charCount;
            }
        }
        return sb.toString();
    }

    public static String decodeComponent(String value) {
        if (value == null || !value.contains("%")) {
            return value;
        }

        StringBuilder decoded = new StringBuilder();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == '%' && i + 2 < value.length()
                    && isHexChar(value.charAt(i + 1))
                    && isHexChar(value.charAt(i + 2))) {
                bytes.reset();
                while (i + 2 < value.length() && value.charAt(i) == '%'
                        && isHexChar(value.charAt(i + 1))
                        && isHexChar(value.charAt(i + 2))) {
                    bytes.write(Integer.parseInt(value.substring(i + 1, i + 3), 16));
                    i += 3;
                }
                decoded.append(bytes.toString(StandardCharsets.UTF_8));
            } else {
                decoded.append(c);
                i++;
            }
        }
        return decoded.toString();
    }

    public static String decodeQueryForDisplay(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        int queryIndex = url.indexOf('?');
        if (queryIndex < 0 || queryIndex == url.length() - 1) {
            return url;
        }

        int fragmentIndex = url.indexOf('#', queryIndex + 1);
        String prefix = url.substring(0, queryIndex + 1);
        String query = fragmentIndex >= 0
                ? url.substring(queryIndex + 1, fragmentIndex)
                : url.substring(queryIndex + 1);
        String suffix = fragmentIndex >= 0 ? url.substring(fragmentIndex) : "";

        StringBuilder decodedQuery = new StringBuilder();
        String[] pairs = query.split("&", -1);
        for (int idx = 0; idx < pairs.length; idx++) {
            String pair = pairs[idx];
            int eqIdx = pair.indexOf('=');
            if (eqIdx >= 0) {
                decodedQuery.append(decodeComponent(pair.substring(0, eqIdx)))
                        .append("=")
                        .append(decodeComponent(pair.substring(eqIdx + 1)));
            } else {
                decodedQuery.append(decodeComponent(pair));
            }
            if (idx < pairs.length - 1) {
                decodedQuery.append("&");
            }
        }

        return prefix + decodedQuery + suffix;
    }

    public static List<HttpParam> parseQueryParams(String url) {
        if (url == null) {
            return Collections.emptyList();
        }
        int idx = url.indexOf('?');
        if (idx < 0 || idx == url.length() - 1) {
            return Collections.emptyList();
        }
        String paramStr = url.substring(idx + 1);

        List<HttpParam> urlParams = new ArrayList<>();
        int last = 0;
        while (last < paramStr.length()) {
            int amp = paramStr.indexOf('&', last);
            String pair = (amp == -1) ? paramStr.substring(last) : paramStr.substring(last, amp);
            int eqIdx = pair.indexOf('=');
            String key;
            String value;
            if (eqIdx >= 0) {
                key = pair.substring(0, eqIdx);
                value = pair.substring(eqIdx + 1);
            } else {
                key = pair;
                value = "";
            }

            if (isNotBlank(key)) {
                urlParams.add(new HttpParam(true, decodeComponent(key.trim()), decodeComponent(value)));
            }

            if (amp == -1) {
                break;
            }
            last = amp + 1;
        }

        if (urlParams.isEmpty()) {
            return Collections.emptyList();
        }
        return urlParams;
    }

    public static String baseUrlWithoutQuery(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        int queryIndex = url.indexOf('?');
        if (queryIndex != -1) {
            return url.substring(0, queryIndex);
        }
        return url;
    }

    public static String buildUrl(String baseUrl, List<HttpParam> params) {
        if (baseUrl == null || baseUrl.trim().isEmpty() || params == null || params.isEmpty()) {
            return baseUrl;
        }

        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        boolean isFirst = true;
        for (HttpParam param : params) {
            if (param.isEnabled() && isNotBlank(param.getKey())) {
                urlBuilder.append(isFirst ? "?" : "&");
                isFirst = false;
                urlBuilder.append(param.getKey());
                if (isNotBlank(param.getValue())) {
                    urlBuilder.append("=").append(param.getValue());
                }
            }
        }
        return urlBuilder.toString();
    }

    private static String encodeExistingQueryString(String url) {
        if (url == null || !url.contains("?")) {
            return url;
        }
        int idx = url.indexOf('?');
        String baseUrl = url.substring(0, idx);
        String paramStr = url.substring(idx + 1);
        StringBuilder sb = new StringBuilder(baseUrl).append("?");
        String[] pairs = paramStr.split("&", -1);
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            int eqIdx = pair.indexOf('=');
            if (eqIdx > 0) {
                sb.append(encodeComponent(pair.substring(0, eqIdx)))
                        .append("=")
                        .append(encodeComponent(pair.substring(eqIdx + 1)));
            } else if (!pair.isEmpty()) {
                sb.append(encodeComponent(pair));
            }
            if (i < pairs.length - 1) {
                sb.append("&");
            }
        }
        return sb.toString();
    }

    public static Set<String> extractQueryParamKeys(String url, boolean hasQuestionMark) {
        Set<String> keys = new LinkedHashSet<>();
        if (!hasQuestionMark) {
            return keys;
        }
        String paramStr = url.substring(url.indexOf('?') + 1);
        for (String pair : paramStr.split("&", -1)) {
            int eqIdx = pair.indexOf('=');
            String key = eqIdx > 0 ? pair.substring(0, eqIdx) : pair;
            if (!key.isEmpty()) {
                keys.add(key);
            }
        }
        return keys;
    }

    private static boolean shouldPercentEncode(int codePoint) {
        return codePoint <= 0x20
                || codePoint >= 0x7F
                || codePoint == '%'
                || codePoint == '&'
                || codePoint == '='
                || codePoint == '?'
                || codePoint == '#';
    }

    private static void appendPercentEncodedUtf8(StringBuilder sb, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            sb.append(String.format("%%%02X", b & 0xFF));
        }
    }

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
