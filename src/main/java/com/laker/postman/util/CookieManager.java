package com.laker.postman.util;

import java.util.*;

/**
 * Cookie 管理器，负责存储和管理各主机的 Cookie。
 */
public class CookieManager {
    private final Map<String, Map<String, String>> cookieStore = new HashMap<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public void registerListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregisterListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (Exception ignore) {
            }
        }
    }

    public String getCookieHeader(String host) {
        Map<String, String> cookies = cookieStore.get(host);
        if (cookies == null || cookies.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (!sb.isEmpty()) sb.append("; ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    public void setCookies(String host, List<String> setCookieHeaders) {
        if (host == null || setCookieHeaders == null) return;
        Map<String, String> cookies = cookieStore.computeIfAbsent(host, k -> new HashMap<>());
        for (String header : setCookieHeaders) {
            String[] parts = header.split(";");
            if (parts.length > 0) {
                String[] kv = parts[0].split("=", 2);
                if (kv.length == 2) {
                    cookies.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
        notifyListeners();
    }

    public Map<String, Map<String, String>> getAllCookies() {
        return cookieStore;
    }
}