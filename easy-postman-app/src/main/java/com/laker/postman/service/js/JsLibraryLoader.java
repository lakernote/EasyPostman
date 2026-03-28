package com.laker.postman.service.js;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.laker.postman.model.Workspace;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.SystemUtil;

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
    private static final Map<String, String> BUILTIN_LIBRARY_CACHE = new ConcurrentHashMap<>();

    /**
     * 内置库别名 -> 规范名称
     */
    private static final Map<String, String> BUILTIN_LIBRARY_IDS = new HashMap<>();

    /**
     * 内置库映射表 - 规范名称 -> 资源路径
     */
    private static final Map<String, String> BUILTIN_LIBRARY_PATHS = new HashMap<>();

    /**
     * 库路径常量 - 统一存放在 js-libs 目录
     */
    private static final String JS_LIBS_DIR = "/js-libs/";
    private static final String CRYPTO_JS_PATH = JS_LIBS_DIR + "crypto-js.min.js";
    private static final String LODASH_PATH = JS_LIBS_DIR + "lodash.min.js";
    private static final String MOMENT_PATH = JS_LIBS_DIR + "moment.min.js";

    static {
        // 注册内置库 - CryptoJS
        registerBuiltInLibrary("crypto-js", CRYPTO_JS_PATH, "crypto-js", "cryptojs", "CryptoJS");

        // 注册内置库 - Lodash
        registerBuiltInLibrary("lodash", LODASH_PATH, "lodash", "_");

        // 注册内置库 - Moment
        registerBuiltInLibrary("moment", MOMENT_PATH, "moment", "Moment");


        log.debug("Registered {} built-in JavaScript libraries", BUILTIN_LIBRARY_PATHS.size());
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
                globalThis.__epRequireCache = Object.create(null);

                globalThis.__easyPostmanRequire = function(moduleName, parentModuleId) {
                    var loadedModule;
                    try {
                        loadedModule = __loadLibrary(moduleName, parentModuleId || '');
                    } catch (e) {
                        throw new Error(e.message || ('Cannot find module: ' + moduleName));
                    }
                    if (!loadedModule || !loadedModule.id || !loadedModule.code) {
                        throw new Error('Cannot find module: ' + moduleName);
                    }

                    var resolvedId = loadedModule.id;
                    if (Object.prototype.hasOwnProperty.call(globalThis.__epRequireCache, resolvedId)) {
                        return globalThis.__epRequireCache[resolvedId];
                    }

                    var module = { exports: {}, id: resolvedId };
                    globalThis.__epRequireCache[resolvedId] = module.exports;
                    var localRequire = function(childModuleName) {
                        return globalThis.__easyPostmanRequire(childModuleName, resolvedId);
                    };

                    try {
                        var moduleFactory = new Function('module', 'exports', 'require', loadedModule.code);
                        moduleFactory(module, module.exports, localRequire);
                    } catch (e) {
                        delete globalThis.__epRequireCache[resolvedId];
                        throw new Error('Failed to load module ' + moduleName + ': ' + e.message);
                    }

                    globalThis.__epRequireCache[resolvedId] = module.exports;
                    return module.exports;
                };

                globalThis.require = function(moduleName) {
                    return globalThis.__easyPostmanRequire(moduleName, '');
                };
                """;

        // 注入 __loadLibrary 辅助函数（Java 端实现）
        ProxyExecutable loadLibraryFunc = args -> {
            String moduleName = args[0].asString();
            String parentModuleId = args.length > 1 ? args[1].asString() : null;
            try {
                LoadedLibrary library = loadLibrary(moduleName, parentModuleId);
                return library.toJsObject();
            } catch (Exception e) {
                log.debug("Failed to load library {}: {}", moduleName, e.getMessage());
                throw new IllegalStateException(e.getMessage(), e);
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
     * @param parentModuleId 父模块标识，用于解析相对路径
     * @return 库的 JavaScript 代码与规范 ID
     */
    private static LoadedLibrary loadLibrary(String moduleName, String parentModuleId) throws IOException {
        String builtInId = resolveBuiltinLibraryId(moduleName);
        if (builtInId != null) {
            return new LoadedLibrary("builtin:" + builtInId, loadBuiltinLibrary(builtInId));
        }

        if (isRemoteModule(moduleName, parentModuleId)) {
            String normalizedUrl = resolveRemoteUrl(moduleName, parentModuleId);
            return loadFromUrl(normalizedUrl);
        }

        Path filePath = resolveFilePath(moduleName, parentModuleId);
        return loadFromFile(filePath);
    }

    /**
     * 加载内置库
     *
     * @param libraryName 库名
     * @return 库的 JavaScript 代码
     */
    private static String loadBuiltinLibrary(String libraryName) throws IOException {
        // 检查缓存
        String cached = BUILTIN_LIBRARY_CACHE.get(libraryName);
        if (cached != null) {
            return cached;
        }

        String resourcePath = BUILTIN_LIBRARY_PATHS.get(libraryName);
        if (resourcePath == null) {
            throw new IOException("Unknown built-in library: " + libraryName);
        }

        // 从 resources 加载
        try (InputStream is = JsLibraryLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Built-in library not found: " + resourcePath);
            }
            String code = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            BUILTIN_LIBRARY_CACHE.put(libraryName, code);
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
    private static LoadedLibrary loadFromFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + path);
        }
        String code = Files.readString(path, StandardCharsets.UTF_8);
        String normalizedPath = path.toAbsolutePath().normalize().toString();
        log.debug("Loaded library from file: {}", normalizedPath);
        return new LoadedLibrary(normalizedPath, code);
    }

    /**
     * 从 URL 加载 JS 库
     *
     * @param urlString URL 地址
     * @return URL 内容
     */
    private static LoadedLibrary loadFromUrl(String urlString) throws IOException {
        validateRemoteUrl(urlString);
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(SettingManager.getRemoteJsRequireConnectTimeoutMs());
        connection.setReadTimeout(SettingManager.getRemoteJsRequireReadTimeoutMs());
        try (InputStream is = connection.getInputStream()) {
            String code = readUtf8WithLimit(is, SettingManager.getRemoteJsRequireMaxBytes());
            log.debug("Loaded library from URL: {}", urlString);
            return new LoadedLibrary(url.toExternalForm(), code);
        }
    }

    /**
     * 清空库缓存（测试用）
     */
    public static void clearCache() {
        BUILTIN_LIBRARY_CACHE.clear();
        log.debug("Library cache cleared");
    }

    /**
     * 获取已注册的内置库列表
     *
     * @return 内置库名称列表
     */
    public static String[] getBuiltinLibraries() {
        return BUILTIN_LIBRARY_PATHS.keySet().toArray(new String[0]);
    }

    private static void registerBuiltInLibrary(String canonicalId, String resourcePath, String... aliases) {
        BUILTIN_LIBRARY_PATHS.put(canonicalId, resourcePath);
        Arrays.stream(aliases).forEach(alias -> BUILTIN_LIBRARY_IDS.put(alias, canonicalId));
    }

    private static String resolveBuiltinLibraryId(String moduleName) {
        String direct = BUILTIN_LIBRARY_IDS.get(moduleName);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, String> entry : BUILTIN_LIBRARY_IDS.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(moduleName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean isRemoteModule(String moduleName, String parentModuleId) {
        return moduleName.startsWith("http://")
                || moduleName.startsWith("https://")
                || (isRemoteModuleId(parentModuleId) && isRelativePath(moduleName));
    }

    private static boolean isRemoteModuleId(String moduleId) {
        return moduleId != null && (moduleId.startsWith("http://") || moduleId.startsWith("https://"));
    }

    private static boolean isRelativePath(String moduleName) {
        return moduleName.startsWith("./") || moduleName.startsWith("../");
    }

    private static String resolveRemoteUrl(String moduleName, String parentModuleId) throws IOException {
        if (moduleName.startsWith("http://") || moduleName.startsWith("https://")) {
            return new URL(moduleName).toExternalForm();
        }
        if (!isRemoteModuleId(parentModuleId)) {
            throw new IOException("Cannot resolve remote module: " + moduleName);
        }
        return new URL(new URL(parentModuleId), moduleName).toExternalForm();
    }

    private static Path resolveFilePath(String moduleName, String parentModuleId) throws IOException {
        if (moduleName.startsWith("/")) {
            return Paths.get(moduleName).toAbsolutePath().normalize();
        }

        if (!isRelativePath(moduleName)) {
            throw new IOException("Cannot find module: " + moduleName);
        }

        Path baseDirectory = resolveBaseDirectory(parentModuleId);
        return baseDirectory.resolve(moduleName).normalize().toAbsolutePath();
    }

    private static Path resolveBaseDirectory(String parentModuleId) {
        if (parentModuleId != null && !parentModuleId.isBlank() && !isRemoteModuleId(parentModuleId)
                && !parentModuleId.startsWith("builtin:")) {
            Path parentPath = Paths.get(parentModuleId);
            Path parentDirectory = Files.isDirectory(parentPath) ? parentPath : parentPath.getParent();
            if (parentDirectory != null) {
                return parentDirectory;
            }
        }

        try {
            Workspace currentWorkspace = WorkspaceService.getInstance().getCurrentWorkspace();
            if (currentWorkspace != null && currentWorkspace.getPath() != null && !currentWorkspace.getPath().isBlank()) {
                return Paths.get(currentWorkspace.getPath()).toAbsolutePath().normalize();
            }
        } catch (Exception e) {
            log.debug("Falling back to default directory for local JS require(): {}", e.getMessage());
        }

        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            return Paths.get(userDir).toAbsolutePath().normalize();
        }

        return Paths.get(SystemUtil.getEasyPostmanPath()).toAbsolutePath().normalize();
    }

    private static void validateRemoteUrl(String urlString) throws IOException {
        if (!SettingManager.isRemoteJsRequireEnabled()) {
            throw new IOException("Remote JavaScript require() is disabled. Enable it in Settings > Request > Script Loading.");
        }

        URL url = new URL(urlString);
        String protocol = url.getProtocol().toLowerCase(Locale.ROOT);
        boolean allowInsecureRemote = SettingManager.isInsecureRemoteJsRequireEnabled();
        if (!"https".equals(protocol) && !(allowInsecureRemote && "http".equals(protocol))) {
            throw new IOException("Only HTTPS remote modules are allowed by default: " + urlString);
        }

        Set<String> allowedHosts = getAllowedRemoteHosts();
        if (!allowedHosts.isEmpty() && !allowedHosts.contains(url.getHost().toLowerCase(Locale.ROOT))) {
            throw new IOException("Remote host is not in the allowlist: " + url.getHost());
        }
    }

    private static Set<String> getAllowedRemoteHosts() {
        String configuredHosts = SettingManager.getRemoteJsRequireAllowedHosts();
        if (configuredHosts.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> hosts = ConcurrentHashMap.newKeySet();
        Arrays.stream(configuredHosts.split(","))
                .map(String::trim)
                .filter(host -> !host.isEmpty())
                .map(host -> host.toLowerCase(Locale.ROOT))
                .forEach(hosts::add);
        return hosts;
    }

    private static String readUtf8WithLimit(InputStream inputStream, int maxBytes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int totalRead = 0;
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            totalRead += bytesRead;
            if (totalRead > maxBytes) {
                throw new IOException("Remote JavaScript module exceeds max size: " + maxBytes + " bytes");
            }
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private record LoadedLibrary(String id, String code) {
        private Map<String, String> toJsObject() {
            Map<String, String> result = new HashMap<>();
            result.put("id", id);
            result.put("code", code);
            return result;
        }
    }
}
