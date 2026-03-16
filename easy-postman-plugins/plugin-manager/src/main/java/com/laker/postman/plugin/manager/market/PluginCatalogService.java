package com.laker.postman.plugin.manager.market;

import com.laker.postman.plugin.runtime.PluginSettingsStore;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 插件市场目录服务。
 */
@Slf4j
@UtilityClass
public class PluginCatalogService {

    public static final String SETTINGS_KEY_CATALOG_URL = "plugin.market.catalogUrl";
    private static final String CATALOG_URL_PROPERTY = "easyPostman.plugins.catalogUrl";
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 10000;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String getCatalogUrl() {
        String override = System.getProperty(CATALOG_URL_PROPERTY);
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        String saved = PluginSettingsStore.getString(SETTINGS_KEY_CATALOG_URL);
        return saved == null ? "" : saved.trim();
    }

    public static void saveCatalogUrl(String catalogUrl) {
        PluginSettingsStore.putString(SETTINGS_KEY_CATALOG_URL, catalogUrl == null ? "" : catalogUrl.trim());
    }

    public static String normalizeCatalogLocation(String catalogUrl) {
        return normalizeLocation(catalogUrl);
    }

    public static List<PluginCatalogEntry> loadCatalog(String catalogUrl) throws Exception {
        String normalizedUrl = normalizeCatalogLocation(catalogUrl);
        if (normalizedUrl.isBlank()) {
            throw new IllegalArgumentException("Catalog URL is blank");
        }
        String json = readText(normalizedUrl);
        URI catalogBaseUri = URI.create(normalizedUrl).resolve(".");
        JsonNode root = MAPPER.readTree(json);
        JsonNode plugins = root.get("plugins");
        List<PluginCatalogEntry> entries = new ArrayList<>();
        if (plugins == null || !plugins.isArray()) {
            return entries;
        }
        for (JsonNode pluginJson : plugins) {
            String downloadUrl = text(pluginJson, "downloadUrl", text(pluginJson, "download_url", ""));
            String id = text(pluginJson, "id", "");
            String entryName = text(pluginJson, "name", id);
            String version = text(pluginJson, "version", "dev");
            if (id.isBlank() || downloadUrl.isBlank()) {
                log.warn("Skip invalid plugin catalog entry: {}", pluginJson);
                continue;
            }
            String resolvedDownloadUrl = resolveLocation(catalogBaseUri, downloadUrl);
            entries.add(new PluginCatalogEntry(
                    id,
                    entryName,
                    version,
                    text(pluginJson, "description", ""),
                    downloadUrl,
                    text(pluginJson, "homepage", text(pluginJson, "homepageUrl", "")),
                    text(pluginJson, "sha256", ""),
                    resolvedDownloadUrl
            ));
        }
        return entries;
    }

    private static String readText(String url) throws Exception {
        URLConnection connection = new URL(url).openConnection();
        if (connection instanceof HttpURLConnection httpConnection) {
            httpConnection.setConnectTimeout(CONNECT_TIMEOUT);
            httpConnection.setReadTimeout(READ_TIMEOUT);
            httpConnection.setRequestMethod("GET");
            httpConnection.setRequestProperty("Accept", "application/json");
            httpConnection.setRequestProperty("User-Agent", "EasyPostman-PluginMarket");
            int code = httpConnection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("HTTP error code: " + code);
            }
        }
        try (InputStream inputStream = connection.getInputStream();
             Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
            return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        }
    }

    private static String text(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? defaultValue : value.asText(defaultValue);
    }

    private static String resolveLocation(URI catalogBaseUri, String location) {
        String value = location == null ? "" : location.trim();
        if (value.isBlank()) {
            return "";
        }
        if (isUri(value)) {
            return value;
        }
        Path path = toPathIfPossible(value);
        if (path != null && path.isAbsolute()) {
            return path.normalize().toUri().toString();
        }
        return catalogBaseUri.resolve(value).toString();
    }

    private static String normalizeLocation(String location) {
        String value = location == null ? "" : location.trim();
        if (value.isBlank()) {
            return "";
        }
        if (isUri(value)) {
            return value;
        }
        Path path = toPathIfPossible(value);
        if (path == null) {
            return value;
        }
        Path normalizedPath = path.normalize();
        if (!normalizedPath.isAbsolute()) {
            normalizedPath = normalizedPath.toAbsolutePath().normalize();
        }
        if (Files.isDirectory(normalizedPath)) {
            normalizedPath = normalizedPath.resolve("catalog.json").normalize();
        }
        return normalizedPath.toUri().toString();
    }

    private static boolean isUri(String value) {
        return value.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*") && !value.matches("^[a-zA-Z]:[\\\\/].*");
    }

    private static Path toPathIfPossible(String value) {
        String expanded = expandUserHome(value);
        try {
            Path path = Paths.get(expanded);
            if (path.isAbsolute() || Files.exists(path)) {
                return path;
            }
            return path;
        } catch (Exception e) {
            return null;
        }
    }

    private static String expandUserHome(String value) {
        if (value.startsWith("~/") || value.startsWith("~\\")) {
            return Paths.get(System.getProperty("user.home"), value.substring(2)).toString();
        }
        return value;
    }
}
