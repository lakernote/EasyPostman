package com.laker.postman.service.js;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * JS脚本Polyfill注入器
 * 提供常用函数注入（编码、加密、工具函数等）
 * <p>
 * 支持的函数：
 * - Base64编解码: btoa, atob
 * - 哈希函数: MD5, SHA1, SHA256, SHA512
 * - 工具函数: uuid, timestamp
 */
@Slf4j
@UtilityClass
public class JsPolyfillInjector {

    /**
     * 注入所有常用 polyfill
     */
    public static void injectAll(Context context) {
        log.debug("Injecting JavaScript polyfills...");

        // Base64编解码
        injectBase64Functions(context);

        // 哈希函数
        injectHashFunctions(context);

        // 工具函数
        injectUtilityFunctions(context);

        log.debug("Polyfills injected successfully");
    }

    /**
     * 注入Base64相关函数
     */
    private static void injectBase64Functions(Context context) {
        // btoa - 字符串转Base64
        injectFunction(context, "btoa", args -> {
            String str = args[0].asString();
            return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
        });

        // atob - Base64转字符串
        injectFunction(context, "atob", args -> {
            String str = args[0].asString();
            byte[] decoded = Base64.getDecoder().decode(str);
            return new String(decoded, StandardCharsets.UTF_8);
        });
    }

    /**
     * 注入哈希函数
     */
    private static void injectHashFunctions(Context context) {
        // MD5
        injectFunction(context, "MD5", args -> {
            String str = args[0].asString();
            return DigestUtils.md5Hex(str);
        });

        // SHA1
        injectFunction(context, "SHA1", args -> {
            String str = args[0].asString();
            return DigestUtils.sha1Hex(str);
        });

        // SHA256
        injectFunction(context, "SHA256", args -> {
            String str = args[0].asString();
            return DigestUtils.sha256Hex(str);
        });

        // SHA512
        injectFunction(context, "SHA512", args -> {
            String str = args[0].asString();
            return DigestUtils.sha512Hex(str);
        });
    }

    /**
     * 注入工具函数
     */
    private static void injectUtilityFunctions(Context context) {
        // uuid - 生成UUID
        injectFunction(context, "uuid", args -> UUID.randomUUID().toString());

        // timestamp - 获取当前时间戳（毫秒）
        injectFunction(context, "timestamp", args -> System.currentTimeMillis());
    }

    /**
     * 注入单个函数
     */
    private static void injectFunction(Context context, String name, ProxyExecutable function) {
        try {
            context.getBindings("js").putMember(name, function);
            log.trace("Injected function: {}", name);
        } catch (Exception e) {
            log.warn("Failed to inject function: {}, error: {}", name, e.getMessage());
        }
    }
}