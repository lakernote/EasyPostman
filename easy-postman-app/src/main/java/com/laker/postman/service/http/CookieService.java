package com.laker.postman.service.http;

import com.laker.postman.model.CookieInfo;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;

import javax.swing.SwingUtilities;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cookie 管理相关工具类，负责监听、获取、设置 Cookie
 */
public class CookieService {
    // 监听器列表
    private static final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean notificationPending = new AtomicBoolean(false);
    // 全局 CookieManager
    private static final CookieManager GLOBAL_COOKIE_MANAGER = OkHttpClientManager.getGlobalCookieManager();

    public static void registerCookieChangeListener(Runnable listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    public static void unregisterCookieChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public static List<CookieInfo> getAllCookieInfos() {
        List<CookieInfo> list = new ArrayList<>();
        CookieStore store = GLOBAL_COOKIE_MANAGER.getCookieStore();
        for (HttpCookie cookie : store.getCookies()) {
            if (cookie.hasExpired()) continue;
            CookieInfo info = new CookieInfo();
            info.name = cookie.getName();
            info.value = cookie.getValue();
            info.domain = cookie.getDomain();
            info.path = cookie.getPath();
            info.expires = cookie.getMaxAge() > 0 ? System.currentTimeMillis() + cookie.getMaxAge() * 1000 : -1;
            info.secure = cookie.getSecure();
            info.httpOnly = cookie.isHttpOnly();
            list.add(info);
        }
        return list;
    }

    public static void removeCookie(String name, String domain, String path) {
        CookieStore store = GLOBAL_COOKIE_MANAGER.getCookieStore();
        List<HttpCookie> toRemove = new ArrayList<>();
        for (HttpCookie cookie : store.getCookies()) {
            if (cookie.getName().equals(name)
                    && (domain == null || domain.equals(cookie.getDomain()))
                    && (path == null || path.equals(cookie.getPath()))) {
                toRemove.add(cookie);
            }
        }
        for (HttpCookie cookie : toRemove) {
            store.remove(null, cookie);
        }
        notifyCookieChanged();
    }

    public static void clearAllCookies() {
        GLOBAL_COOKIE_MANAGER.getCookieStore().removeAll();
        notifyCookieChanged();
    }

    public static void notifyCookieChanged() {
        if (SwingUtilities.isEventDispatchThread()) {
            notifyListeners();
            return;
        }
        if (!notificationPending.compareAndSet(false, true)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            notificationPending.set(false);
            notifyListeners();
        });
    }

    private static void notifyListeners() {
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (Exception ignore) {
            }
        }
    }

    public static void addCookie(String name, String value, String domain, String path, boolean secure, boolean httpOnly) {
        try {
            HttpCookie cookie = new HttpCookie(name, value);
            cookie.setDomain(domain);
            cookie.setPath(path == null || path.isEmpty() ? "/" : path);
            cookie.setSecure(secure);
            cookie.setHttpOnly(httpOnly);
            GLOBAL_COOKIE_MANAGER.getCookieStore().add(null, cookie);
            notifyCookieChanged();
        } catch (Exception ignore) {
        }
    }

    public static void refreshCookies() {
        notifyCookieChanged();
    }
}
