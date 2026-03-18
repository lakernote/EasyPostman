package com.laker.postman.plugin.manager.market;

import com.laker.postman.plugin.runtime.PluginRuntime;
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
    public static final String OFFICIAL_GITHUB_CATALOG_URL =
            "https://raw.githubusercontent.com/lakernote/easy-postman/master/plugin-catalog/catalog-github.json";
    public static final String OFFICIAL_GITEE_CATALOG_URL =
            "https://gitee.com/lakernote/easy-postman/raw/master/plugin-catalog/catalog-gitee.json";
    public static final String OFFICIAL_GITHUB_CATALOG_RESOURCE = "/plugin-catalog/catalog-github.json";
    public static final String OFFICIAL_GITEE_CATALOG_RESOURCE = "/plugin-catalog/catalog-gitee.json";
    private static final String CATALOG_URL_PROPERTY = "easyPostman.plugins.catalogUrl";
    private static final int CONNECT_TIMEOUT = 5_000;
    private static final int READ_TIMEOUT = 15_000;
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

    public static String getOfficialCatalogUrl(String source) {
        if ("github".equalsIgnoreCase(source)) {
            return OFFICIAL_GITHUB_CATALOG_URL;
        }
        return OFFICIAL_GITEE_CATALOG_URL;
    }

    public static String detectOfficialCatalogSource(String catalogUrl) {
        String normalized = normalizeCatalogLocation(catalogUrl);
        if (normalized.equals(normalizeCatalogLocation(OFFICIAL_GITHUB_CATALOG_URL))) {
            return "github";
        }
        if (normalized.equals(normalizeCatalogLocation(OFFICIAL_GITEE_CATALOG_URL))) {
            return "gitee";
        }
        return "";
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
        return parseCatalog(json, URI.create(normalizedUrl).resolve("."));
    }

    public static List<PluginCatalogEntry> loadBundledOfficialCatalog(String source) throws Exception {
        String resolvedSource = "github".equalsIgnoreCase(source) ? "github" : "gitee";
        String resourcePath = "github".equalsIgnoreCase(resolvedSource)
                ? OFFICIAL_GITHUB_CATALOG_RESOURCE
                : OFFICIAL_GITEE_CATALOG_RESOURCE;
        try (InputStream inputStream = PluginCatalogService.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Bundled catalog resource not found: " + resourcePath);
            }
            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
                String json = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                String baseUrl = getOfficialCatalogUrl(resolvedSource);
                return parseCatalog(json, URI.create(baseUrl).resolve("."));
            }
        }
    }

    static List<PluginCatalogEntry> parseCatalogForTests(String json, String baseUrl) throws Exception {
        return parseCatalog(json, URI.create(baseUrl).resolve("."));
    }

    static List<PluginCatalogEntry> parseCatalogForTests(String json,
                                                         String baseUrl,
                                                         String currentAppVersion,
                                                         String currentPlatformVersion) throws Exception {
        return parseCatalog(json, URI.create(baseUrl).resolve("."), currentAppVersion, currentPlatformVersion);
    }

    private static List<PluginCatalogEntry> parseCatalog(String json, URI catalogBaseUri) throws Exception {
        return parseCatalog(
                json,
                catalogBaseUri,
                PluginRuntime.getCurrentAppVersion(),
                PluginRuntime.getCurrentPluginPlatformVersion()
        );
    }

    private static List<PluginCatalogEntry> parseCatalog(String json,
                                                         URI catalogBaseUri,
                                                         String currentAppVersion,
                                                         String currentPlatformVersion) throws Exception {
        JsonNode root = MAPPER.readTree(json);
        JsonNode plugins = root.get("plugins");
        List<PluginCatalogEntry> candidates = new ArrayList<>();
        if (plugins == null || !plugins.isArray()) {
            return candidates;
        }
        for (JsonNode pluginJson : plugins) {
            candidates.addAll(parsePluginEntries(pluginJson, catalogBaseUri));
        }
        return PluginCatalogVersionSelector.selectForHost(
                candidates,
                currentAppVersion,
                currentPlatformVersion
        );
    }

    private static List<PluginCatalogEntry> parsePluginEntries(JsonNode pluginJson, URI catalogBaseUri) {
        String id = text(pluginJson, "id", "");
        String entryName = text(pluginJson, "name", id);
        String description = text(pluginJson, "description", "");
        String homepage = text(pluginJson, "homepage", text(pluginJson, "homepageUrl", ""));
        if (id.isBlank()) {
            log.warn("Skip invalid plugin catalog entry without id: {}", pluginJson);
            return List.of();
        }

        JsonNode releases = pluginJson.get("releases");
        if (releases != null && releases.isArray()) {
            List<PluginCatalogEntry> entries = new ArrayList<>();
            for (JsonNode releaseJson : releases) {
                PluginCatalogEntry entry = parseReleaseEntry(id, entryName, description, homepage, releaseJson, catalogBaseUri);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            return entries;
        }

        PluginCatalogEntry legacyEntry = parseFlatEntry(pluginJson, catalogBaseUri);
        return legacyEntry == null ? List.of() : List.of(legacyEntry);
    }

    private static PluginCatalogEntry parseFlatEntry(JsonNode pluginJson, URI catalogBaseUri) {
        String downloadUrl = text(pluginJson, "downloadUrl", text(pluginJson, "download_url", ""));
        String id = text(pluginJson, "id", "");
        String entryName = text(pluginJson, "name", id);
        String version = text(pluginJson, "version", "dev");
        if (id.isBlank() || downloadUrl.isBlank()) {
            log.warn("Skip invalid plugin catalog entry: {}", pluginJson);
            return null;
        }
        String resolvedDownloadUrl = resolveLocation(catalogBaseUri, downloadUrl);
        return new PluginCatalogEntry(
                id,
                entryName,
                version,
                text(pluginJson, "description", ""),
                downloadUrl,
                text(pluginJson, "homepage", text(pluginJson, "homepageUrl", "")),
                text(pluginJson, "sha256", ""),
                resolvedDownloadUrl,
                text(pluginJson, "minAppVersion", ""),
                text(pluginJson, "maxAppVersion", ""),
                text(pluginJson, "minPlatformVersion", ""),
                text(pluginJson, "maxPlatformVersion", "")
        );
    }

    private static PluginCatalogEntry parseReleaseEntry(String id,
                                                        String entryName,
                                                        String description,
                                                        String homepage,
                                                        JsonNode releaseJson,
                                                        URI catalogBaseUri) {
        String downloadUrl = text(releaseJson, "downloadUrl", text(releaseJson, "download_url", ""));
        String version = text(releaseJson, "version", "dev");
        if (downloadUrl.isBlank()) {
            log.warn("Skip invalid plugin release entry: {} {}", id, releaseJson);
            return null;
        }
        String resolvedDownloadUrl = resolveLocation(catalogBaseUri, downloadUrl);
        return new PluginCatalogEntry(
                id,
                entryName,
                version,
                text(releaseJson, "description", description),
                downloadUrl,
                text(releaseJson, "homepage", text(releaseJson, "homepageUrl", homepage)),
                text(releaseJson, "sha256", ""),
                resolvedDownloadUrl,
                text(releaseJson, "minAppVersion", ""),
                text(releaseJson, "maxAppVersion", ""),
                text(releaseJson, "minPlatformVersion", ""),
                text(releaseJson, "maxPlatformVersion", "")
        );
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
        if (isHttpUrl(value)) {
            return value;
        }
        return catalogBaseUri.resolve(value).toString();
    }

    private static String normalizeLocation(String location) {
        String value = location == null ? "" : location.trim();
        if (value.isBlank()) {
            return "";
        }
        if (!isHttpUrl(value)) {
            throw new IllegalArgumentException("Only http(s) catalog URLs are supported");
        }
        return value;
    }

    private static boolean isHttpUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }
}
