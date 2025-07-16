package com.laker.postman.service.js;

import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 提供 JS 脚本常用函数注入（如 btoa/atob/MD5/SHA256）
 */
@Slf4j
public class JsPolyfillInjector {
    /**
     * 注入所有常用 polyfill
     */
    public static void injectAll(Context context) {
        injectBtoa(context);
        injectAtob(context);
        injectMD5(context);
        injectSHA256(context);
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

    /**
     * 注入MD5加密函数
     */
    private static void injectMD5(Context context) {
        context.getBindings("js").putMember("MD5", (ProxyExecutable) args -> {
            String str = args[0].asString();
            return DigestUtil.md5Hex(str);
        });
    }

    /**
     * 注入SHA256加密函数
     */
    private static void injectSHA256(Context context) {
        context.getBindings("js").putMember("SHA256", (ProxyExecutable) args -> {
            String str = args[0].asString();
            return DigestUtil.sha256Hex(str);
        });
    }
}