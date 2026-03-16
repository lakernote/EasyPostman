package com.laker.postman.plugin.manager.market;

import com.laker.postman.plugin.api.PluginDescriptor;
import com.laker.postman.plugin.runtime.PluginFileInfo;
import com.laker.postman.plugin.runtime.PluginRuntime;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * 插件安装服务。
 */
@Slf4j
@UtilityClass
public class PluginInstallerService {

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;

    public static PluginFileInfo installPluginJar(Path sourceJar) throws IOException {
        PluginDescriptor descriptor = PluginRuntime.inspectPluginJar(sourceJar);
        if (descriptor == null) {
            throw new IllegalArgumentException("Invalid plugin jar");
        }
        return installPreparedJar(sourceJar, descriptor, true);
    }

    public static PluginFileInfo installCatalogPlugin(PluginCatalogEntry entry) throws Exception {
        Path tempJar = downloadToTempFile(entry);
        try {
            PluginDescriptor descriptor = PluginRuntime.inspectPluginJar(tempJar);
            if (descriptor == null) {
                throw new IllegalArgumentException("Downloaded file is not a valid plugin jar");
            }
            if (!entry.id().equals(descriptor.id())) {
                throw new IllegalStateException("Plugin id mismatch: " + entry.id() + " != " + descriptor.id());
            }
            verifySha256(tempJar, entry.sha256());
            return installPreparedJar(tempJar, descriptor, true);
        } finally {
            Files.deleteIfExists(tempJar);
        }
    }

    private static PluginFileInfo installPreparedJar(Path sourceJar, PluginDescriptor descriptor, boolean removeLegacyVersions)
            throws IOException {
        Path targetDir = PluginRuntime.getManagedPluginDir();
        Path packageDir = PluginRuntime.getPluginPackageDir();
        Files.createDirectories(targetDir);
        Files.createDirectories(packageDir);
        Path targetPath = targetDir.resolve(buildTargetFileName(descriptor));
        Path packagePath = packageDir.resolve(buildTargetFileName(descriptor));
        if (!sourceJar.toAbsolutePath().normalize().equals(packagePath.toAbsolutePath().normalize())) {
            Files.copy(sourceJar, packagePath, StandardCopyOption.REPLACE_EXISTING);
        }
        if (!sourceJar.toAbsolutePath().normalize().equals(targetPath.toAbsolutePath().normalize())) {
            Files.copy(sourceJar, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        if (removeLegacyVersions) {
            cleanupLegacyVersions(descriptor.id(), targetPath);
            cleanupLegacyPackageVersions(descriptor.id(), packagePath);
        }
        PluginRuntime.setPluginEnabled(descriptor.id(), true);
        return new PluginFileInfo(descriptor, targetPath, false);
    }

    private static Path downloadToTempFile(PluginCatalogEntry entry) throws Exception {
        String installUrl = entry.installUrl();
        String fileName = extractFileName(installUrl, entry.id(), entry.version());
        String suffix = fileName.endsWith(".jar") ? ".jar" : "-" + fileName;
        Path tempFile = Files.createTempFile("easy-postman-plugin-", suffix);

        URLConnection connection = new URL(installUrl).openConnection();
        if (connection instanceof HttpURLConnection httpConnection) {
            httpConnection.setConnectTimeout(CONNECT_TIMEOUT);
            httpConnection.setReadTimeout(READ_TIMEOUT);
            httpConnection.setRequestMethod("GET");
            httpConnection.setInstanceFollowRedirects(true);
            httpConnection.setRequestProperty("User-Agent", "EasyPostman-PluginInstaller");
            int code = httpConnection.getResponseCode();
            if (code < 200 || code >= 300) {
                Files.deleteIfExists(tempFile);
                throw new IllegalStateException("HTTP error code: " + code);
            }
        }

        try (InputStream inputStream = connection.getInputStream();
             OutputStream outputStream = Files.newOutputStream(tempFile)) {
            inputStream.transferTo(outputStream);
        } catch (Exception e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
        return tempFile;
    }

    private static void cleanupLegacyVersions(String pluginId, Path keepPath) {
        List<PluginFileInfo> managedPlugins = PluginRuntime.getManagedPluginFiles();
        for (PluginFileInfo info : managedPlugins) {
            if (!pluginId.equals(info.descriptor().id()) || info.jarPath().equals(keepPath)) {
                continue;
            }
            try {
                Files.deleteIfExists(info.jarPath());
            } catch (IOException e) {
                log.warn("Failed to delete legacy plugin file: {}", info.jarPath(), e);
            }
        }
    }

    private static void cleanupLegacyPackageVersions(String pluginId, Path keepPath) {
        List<PluginFileInfo> packagedPlugins = PluginRuntime.getPluginPackageFiles();
        for (PluginFileInfo info : packagedPlugins) {
            if (!pluginId.equals(info.descriptor().id()) || info.jarPath().equals(keepPath)) {
                continue;
            }
            try {
                Files.deleteIfExists(info.jarPath());
            } catch (IOException e) {
                log.warn("Failed to delete legacy packaged plugin file: {}", info.jarPath(), e);
            }
        }
    }

    private static void verifySha256(Path file, String expectedSha256) throws Exception {
        if (expectedSha256 == null || expectedSha256.isBlank()) {
            return;
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream inputStream = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        String actual = HexFormat.of().formatHex(digest.digest());
        if (!actual.equalsIgnoreCase(expectedSha256.trim())) {
            throw new IllegalStateException("Plugin sha256 mismatch");
        }
    }

    private static String buildTargetFileName(PluginDescriptor descriptor) {
        return sanitizeFileName(descriptor.id()) + "-" + sanitizeFileName(descriptor.version()) + ".jar";
    }

    private static String extractFileName(String downloadUrl, String pluginId, String version) {
        try {
            String path = URI.create(downloadUrl).getPath();
            if (path != null && !path.isBlank()) {
                String candidate = path.substring(path.lastIndexOf('/') + 1);
                if (!candidate.isBlank()) {
                    return candidate;
                }
            }
        } catch (Exception ignored) {
            // fallback below
        }
        return sanitizeFileName(pluginId) + "-" + sanitizeFileName(version) + ".jar";
    }

    private static String sanitizeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "plugin";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "-");
    }
}
