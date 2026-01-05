package com.laker.postman.service.js;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * JS 外部库加载器
 * <p>
 * 支持加载内置 JS 库和用户自定义的外部 JS 库，模拟 Node.js 的 require() 机制。
 * 与 Postman 兼容，提供相同的内置库支持。
 * </p>
 *
 * <h3>支持的内置库：</h3>
 * <ul>
 *   <li><b>crypto-js</b>: 加密库，支持 AES、DES、MD5、SHA 等算法</li>
 *   <li><b>lodash</b>: JavaScript 实用工具库，提供丰富的函数式编程辅助方法</li>
 *   <li><b>moment</b>: 日期时间处理库，格式化、解析和操作日期</li>
 * </ul>
 *
 * <h3>Java 实现的全局函数（由 JsPolyfillInjector 提供）：</h3>
 * <ul>
 *   <li><b>uuid()</b>: UUID 生成器</li>
 *   <li><b>atob/btoa</b>: Base64 编码解码</li>
 *   <li><b>MD5/SHA1/SHA256/SHA512</b>: 哈希函数</li>
 *   <li><b>timestamp()</b>: 时间戳函数</li>
 *   <li><b>encodeURIComponent/decodeURIComponent</b>: URL 编解码</li>
 * </ul>
 *
 * <h3>使用方式：</h3>
 * <pre>{@code
 * // 方式1: 直接使用内置库（自动加载到全局）
 * var encrypted = CryptoJS.AES.encrypt('message', 'secret');
 * var id = uuid.v4();
 * var now = moment().format('YYYY-MM-DD');
 * var result = _.filter([1, 2, 3], n => n > 1);
 *
 * // 方式2: 使用 require() 加载
 * var CryptoJS = require('crypto-js');
 * var _ = require('lodash');
 * var moment = require('moment');
 *
 * // 方式3: 加载自定义外部库（从文件路径）
 * var myLib = require('/path/to/my-library.js');
 *
 * // 方式4: 加载自定义外部库（从 URL）
 * var myLib = require('https://cdn.example.com/library.js');
 * }</pre>
 *
 * @author laker
 */
@Slf4j
@UtilityClass
public class JsLibraryLoader {

    /**
     * 内置库缓存 - 避免重复加载
     */
    private static final Map<String, String> LIBRARY_CACHE = new HashMap<>();

    /**
     * 内置库映射表 - 库名 -> 资源路径
     */
    private static final Map<String, String> BUILTIN_LIBRARIES = new HashMap<>();

    /**
     * 库路径常量 - 统一存放在 js-libs 目录
     */
    private static final String JS_LIBS_DIR = "/js-libs/";
    private static final String CRYPTO_JS_PATH = JS_LIBS_DIR + "crypto-js.min.js";
    private static final String LODASH_PATH = JS_LIBS_DIR + "lodash.min.js";
    private static final String MOMENT_PATH = JS_LIBS_DIR + "moment.min.js";

    static {
        // 注册内置库 - CryptoJS
        BUILTIN_LIBRARIES.put("crypto-js", CRYPTO_JS_PATH);
        BUILTIN_LIBRARIES.put("cryptojs", CRYPTO_JS_PATH);
        BUILTIN_LIBRARIES.put("CryptoJS", CRYPTO_JS_PATH);

        // 注册内置库 - Lodash
        BUILTIN_LIBRARIES.put("lodash", LODASH_PATH);
        BUILTIN_LIBRARIES.put("_", LODASH_PATH);

        // 注册内置库 - Moment
        BUILTIN_LIBRARIES.put("moment", MOMENT_PATH);
        BUILTIN_LIBRARIES.put("Moment", MOMENT_PATH);


        log.debug("Registered {} built-in JavaScript libraries", BUILTIN_LIBRARIES.size());
    }

    /**
     * 注入所有内置库到 JS 上下文
     * <p>
     * 此方法会加载所有内置库并注入到全局作用域，使得用户脚本可以直接使用。
     * 例如：CryptoJS、lodash、moment 等可以直接访问，无需 require()。
     * </p>
     *
     * @param context GraalVM JS 上下文
     */
    public static void injectBuiltinLibraries(Context context) {
        log.debug("Loading built-in JavaScript libraries...");

        // 注入 crypto polyfill（为 CryptoJS 提供随机数生成）
        injectCryptoPolyfill(context);

        // 加载核心库并注入到全局作用域
        loadAndInjectLibrary(context, "crypto-js", "CryptoJS");
        loadAndInjectLibrary(context, "lodash", "_");
        loadAndInjectLibrary(context, "moment", "moment");

        log.debug("All built-in libraries loaded successfully");
    }

    /**
     * 加载库并注入到全局作用域
     *
     * @param context GraalVM JS 上下文
     * @param libraryName 库名
     * @param globalName 全局变量名（如果为 null，则不创建全局变量）
     */
    private static void loadAndInjectLibrary(Context context, String libraryName, String globalName) {
        try {
            String code = loadBuiltinLibrary(libraryName);
            if (code != null) {
                context.eval("js", code);
                log.debug("Loaded built-in library: {} {}", libraryName,
                         globalName != null ? "(as " + globalName + ")" : "");
            }
        } catch (Exception e) {
            log.warn("Failed to load library {}: {}", libraryName, e.getMessage());
        }
    }

    /**
     * 注入 crypto polyfill，为 CryptoJS 提供随机数生成
     */
    private static void injectCryptoPolyfill(Context context) {
        String cryptoPolyfill = """
                if (typeof crypto === 'undefined') {
                    globalThis.crypto = {
                        getRandomValues: function(array) {
                            for (var i = 0; i < array.length; i++) {
                                array[i] = Math.floor(Math.random() * 256);
                            }
                            return array;
                        }
                    };
                }
                """;
        try {
            context.eval("js", cryptoPolyfill);
            log.debug("Injected crypto polyfill");
        } catch (Exception e) {
            log.warn("Failed to inject crypto polyfill: {}", e.getMessage());
        }
    }

    /**
     * 注入 require() 函数，支持动态加载外部 JS 库
     * <p>
     * 用户可以在脚本中使用 require('library-name') 或 require('/path/to/file.js')
     * 来加载内置库或自定义库。
     * </p>
     *
     * @param context GraalVM JS 上下文
     */
    public static void injectRequireFunction(Context context) {
        // 创建 require() 函数的实现
        String requireFunction = """
                var require = (function() {
                    var cache = {};
                \s\s\s\s
                    return function(moduleName) {
                        // 检查缓存
                        if (cache[moduleName]) {
                            return cache[moduleName];
                        }
                \s\s\s\s
                        // 调用 Java 端加载库
                        var moduleCode = __loadLibrary(moduleName);
                        if (!moduleCode) {
                            throw new Error('Cannot find module: ' + moduleName);
                        }
                \s\s\s\s
                        // 创建模块作用域
                        var module = { exports: {} };
                        var exports = module.exports;
                \s\s\s\s
                        // 执行模块代码
                        try {
                            eval(moduleCode);
                        } catch (e) {
                            throw new Error('Failed to load module ' + moduleName + ': ' + e.message);
                        }
                \s\s\s\s
                        // 缓存模块
                        cache[moduleName] = module.exports;
                        return module.exports;
                    };
                })();
                """;

        // 注入 __loadLibrary 辅助函数（Java 端实现）
        ProxyExecutable loadLibraryFunc = args -> {
            try {
                String moduleName = args[0].asString();
                return loadLibrary(moduleName);
            } catch (Exception e) {
                log.error("Failed to load library: {}", args[0].asString(), e);
                return null;
            }
        };
        context.getBindings("js").putMember("__loadLibrary", loadLibraryFunc);

        // 注入 require() 函数
        context.eval("js", requireFunction);
        log.debug("Injected require() function");
    }

    /**
     * 加载 JS 库（内置或外部）
     *
     * @param moduleName 库名、文件路径或 URL
     * @return 库的 JavaScript 代码
     */
    private static String loadLibrary(String moduleName) throws IOException {
        // 1. 检查是否是内置库
        if (BUILTIN_LIBRARIES.containsKey(moduleName)) {
            return loadBuiltinLibrary(moduleName);
        }

        // 2. 检查是否是 URL
        if (moduleName.startsWith("http://") || moduleName.startsWith("https://")) {
            return loadFromUrl(moduleName);
        }

        // 3. 检查是否是文件路径
        if (moduleName.startsWith("/") || moduleName.startsWith("./") || moduleName.startsWith("../")) {
            return loadFromFile(moduleName);
        }

        // 4. 尝试作为内置库（忽略大小写）
        for (String libName : BUILTIN_LIBRARIES.keySet()) {
            if (libName.equalsIgnoreCase(moduleName)) {
                return loadBuiltinLibrary(libName);
            }
        }

        throw new IOException("Cannot find module: " + moduleName);
    }

    /**
     * 加载内置库
     *
     * @param libraryName 库名
     * @return 库的 JavaScript 代码
     */
    private static String loadBuiltinLibrary(String libraryName) throws IOException {
        // 检查缓存
        if (LIBRARY_CACHE.containsKey(libraryName)) {
            return LIBRARY_CACHE.get(libraryName);
        }

        String resourcePath = BUILTIN_LIBRARIES.get(libraryName);
        if (resourcePath == null) {
            throw new IOException("Unknown built-in library: " + libraryName);
        }

        // 从 resources 加载
        try (InputStream is = JsLibraryLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Built-in library not found: " + resourcePath);
            }
            String code = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            LIBRARY_CACHE.put(libraryName, code);
            log.debug("Loaded built-in library from resources: {}", resourcePath);
            return code;
        }
    }

    /**
     * 从文件路径加载 JS 库
     *
     * @param filePath 文件路径
     * @return 文件内容
     */
    private static String loadFromFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filePath);
        }
        String code = Files.readString(path, StandardCharsets.UTF_8);
        log.debug("Loaded library from file: {}", filePath);
        return code;
    }

    /**
     * 从 URL 加载 JS 库
     *
     * @param urlString URL 地址
     * @return URL 内容
     */
    private static String loadFromUrl(String urlString) throws IOException {
        // 检查缓存
        if (LIBRARY_CACHE.containsKey(urlString)) {
            return LIBRARY_CACHE.get(urlString);
        }

        URL url = new URL(urlString);
        try (InputStream is = url.openStream()) {
            String code = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            LIBRARY_CACHE.put(urlString, code);
            log.debug("Loaded library from URL: {}", urlString);
            return code;
        }
    }

    /**
     * 清空库缓存（测试用）
     */
    public static void clearCache() {
        LIBRARY_CACHE.clear();
        log.debug("Library cache cleared");
    }

    /**
     * 获取已注册的内置库列表
     *
     * @return 内置库名称列表
     */
    public static String[] getBuiltinLibraries() {
        return BUILTIN_LIBRARIES.keySet().toArray(new String[0]);
    }
}

