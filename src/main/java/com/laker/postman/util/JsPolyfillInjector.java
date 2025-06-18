package com.laker.postman.util;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 提供 JS 脚本常用函数注入（如 btoa/atob/encodeURIComponent/decodeURIComponent）
 */
@Slf4j
public class JsPolyfillInjector {
    /**
     * 注入所有常用 polyfill
     */
    public static void injectAll(Context context) {
        injectBtoa(context);
        injectAtob(context);
        injectEncodeURIComponent(context);
        injectDecodeURIComponent(context);
    }

    private static void injectBtoa(Context context) {
        context.getBindings("js").putMember("btoa", (ProxyExecutable) args -> {
            String str = args[0].asString();
            return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
        });
    }

    private static void injectAtob(Context context) {
        context.getBindings("js").putMember("atob", (ProxyExecutable) args -> {
            String str = args[0].asString();
            byte[] decoded = Base64.getDecoder().decode(str);
            return new String(decoded, StandardCharsets.UTF_8);
        });
    }

    private static void injectEncodeURIComponent(Context context) {
        context.getBindings("js").putMember("encodeURIComponent", (ProxyExecutable) args -> {
            String str = args[0].asString();
            try {
                return URLEncoder.encode(str, StandardCharsets.UTF_8)
                        .replace("+", "%20")
                        .replace("%21", "!")
                        .replace("%27", "'")
                        .replace("%28", "(")
                        .replace("%29", ")")
                        .replace("%7E", "~");
            } catch (Exception e) {
                log.error("Error encoding URI component: {}", str, e);
                return str;
            }
        });
    }

    private static void injectDecodeURIComponent(Context context) {
        context.getBindings("js").putMember("decodeURIComponent", (ProxyExecutable) args -> {
            String str = args[0].asString();
            try {
                return URLDecoder.decode(str, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("Error decoding URI component: {}", str, e);
                return str;
            }
        });
    }
}

