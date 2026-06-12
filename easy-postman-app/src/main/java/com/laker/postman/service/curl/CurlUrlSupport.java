package com.laker.postman.service.curl;

import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.util.HttpUrlUtil;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;

@UtilityClass
class CurlUrlSupport {

    static String normalizeCurlUrlToken(String url) {
        if (url == null || url.indexOf('\\') < 0) {
            return url;
        }

        StringBuilder normalized = new StringBuilder(url.length());
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '\\' && i + 1 < url.length() && isCurlUrlGlobChar(url.charAt(i + 1))) {
                normalized.append(url.charAt(++i));
                continue;
            }
            normalized.append(c);
        }
        return normalized.toString();
    }

    static void addQueryParams(CurlRequest req) {
        if (req.url == null || !req.url.contains("?")) {
            return;
        }
        String[] urlParts = req.url.split("\\?", 2);
        String query = urlParts[1];
        if (req.paramsList == null) {
            req.paramsList = new ArrayList<>();
        }
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2) {
                req.paramsList.add(new HttpParam(true,
                        HttpUrlUtil.decodeComponent(kv[0]),
                        HttpUrlUtil.decodeComponent(kv[1])));
            } else if (kv.length == 1) {
                req.paramsList.add(new HttpParam(true, HttpUrlUtil.decodeComponent(kv[0]), ""));
            }
        }
    }

    static boolean isWebSocketUrl(String url) {
        return url != null && (url.startsWith("ws://") || url.startsWith("wss://"));
    }

    private static boolean isCurlUrlGlobChar(char c) {
        return c == '[' || c == ']' || c == '{' || c == '}';
    }
}
