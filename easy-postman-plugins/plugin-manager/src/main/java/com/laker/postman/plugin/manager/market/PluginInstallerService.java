package com.laker.postman.plugin.manager.market;

import com.laker.postman.plugin.api.PluginDescriptor;
import com.laker.postman.plugin.runtime.PluginCompatibility;
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

    private static final int CONNECT_TIMEOUT = 5_000;
    private static final int READ_TIMEOUT = 300_000;
    private static final int DOWNLOAD_BUFFER_SIZE = 8192;
    private static final long PROGRESS_EMIT_INTERVAL_NANOS = 150_000_000L;

    public static PluginFileInfo installPluginJar(Path sourceJar) throws IOException {
        PluginDescriptor descriptor = PluginRuntime.inspectPluginJar(sourceJar);
        if (descriptor == null) {
            throw new IllegalArgumentException("Invalid plugin jar");
        }
        validateCompatibility(descriptor);
        return installPreparedJar(sourceJar, descriptor, true);
    }

    public static PluginFileInfo installCatalogPlugin(PluginCatalogEntry entry) throws Exception {
        return installCatalogPlugin(entry, PluginInstallProgressListener.NO_OP, new PluginInstallController());
    }

    public static PluginFileInfo installCatalogPlugin(PluginCatalogEntry entry,
                                                      PluginInstallProgressListener progressListener) throws Exception {
        return installCatalogPlugin(entry, progressListener, new PluginInstallController());
    }

    public static PluginFileInfo installCatalogPlugin(PluginCatalogEntry entry,
                                                      PluginInstallProgressListener progressListener,
                                                      PluginInstallController installController) throws Exception {
        PluginInstallProgressListener listener = progressListener == null
                ? PluginInstallProgressListener.NO_OP
                : progressListener;
        PluginInstallController controller = installController == null
                ? new PluginInstallController()
                : installController;
        Path tempJar = downloadToTempFile(entry, listener, controller);
        try {
            controller.checkCancelled();
            listener.onProgress(new PluginInstallProgress(
                    PluginInstallProgress.Stage.VERIFYING,
                    entry.id(),
                    entry.installUrl(),
                    Files.size(tempJar),
                    Files.size(tempJar),
                    0
            ));
            controller.checkCancelled();
            PluginDescriptor descriptor = PluginRuntime.inspectPluginJar(tempJar);
            if (descriptor == null) {
                throw new IllegalArgumentException("Downloaded file is not a valid plugin jar");
            }
            if (!entry.id().equals(descriptor.id())) {
                throw new IllegalStateException("Plugin id mismatch: " + entry.id() + " != " + descriptor.id());
            }
            if (entry.version() != null
                    && !entry.version().isBlank()
                    && !entry.version().equals(descriptor.version())) {
                throw new IllegalStateException("Plugin version mismatch: " + entry.version() + " != " + descriptor.version());
            }
            verifySha256(tempJar, entry.sha256());
            validateCompatibility(descriptor);
            controller.checkCancelled();
            listener.onProgress(new PluginInstallProgress(
                    PluginInstallProgress.Stage.INSTALLING,
                    descriptor.id(),
                    entry.installUrl(),
                    Files.size(tempJar),
                    Files.size(tempJar),
                    0
            ));
            PluginFileInfo installed = installPreparedJar(tempJar, descriptor, true);
            listener.onProgress(new PluginInstallProgress(
                    PluginInstallProgress.Stage.COMPLETED,
                    descriptor.id(),
                    entry.installUrl(),
                    Files.size(tempJar),
                    Files.size(tempJar),
                    0
            ));
            return installed;
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

    private static Path downloadToTempFile(PluginCatalogEntry entry,
                                           PluginInstallProgressListener progressListener,
                                           PluginInstallController installController) throws Exception {
        String installUrl = entry.installUrl();
        String fileName = extractFileName(installUrl, entry.id(), entry.version());
        String suffix = fileName.endsWith(".jar") ? ".jar" : "-" + fileName;
        Path tempFile = Files.createTempFile("easy-postman-plugin-", suffix);

        installController.checkCancelled();
        progressListener.onProgress(new PluginInstallProgress(
                PluginInstallProgress.Stage.CONNECTING,
                entry.id(),
                installUrl,
                0,
                -1,
                0
        ));

        URLConnection connection = new URL(installUrl).openConnection();
        if (connection instanceof HttpURLConnection httpConnection) {
            httpConnection.setConnectTimeout(CONNECT_TIMEOUT);
            httpConnection.setReadTimeout(READ_TIMEOUT);
            httpConnection.setRequestMethod("GET");
            httpConnection.setInstanceFollowRedirects(true);
            httpConnection.setUseCaches(false);
            httpConnection.setRequestProperty("User-Agent", "EasyPostman-PluginInstaller");
            installController.attachConnection(httpConnection);
            try {
                int code = httpConnection.getResponseCode();
                if (code < 200 || code >= 300) {
                    Files.deleteIfExists(tempFile);
                    throw new IllegalStateException("HTTP error code: " + code + ", url: " + installUrl);
                }
            } catch (Exception e) {
                if (installController.isCancelled()) {
                    Files.deleteIfExists(tempFile);
                    throw new java.util.concurrent.CancellationException("Plugin installation cancelled");
                }
                throw e;
            }
        }

        long totalBytes = connection.getContentLengthLong();
        long startNanos = System.nanoTime();
        progressListener.onProgress(new PluginInstallProgress(
                PluginInstallProgress.Stage.DOWNLOADING,
                entry.id(),
                installUrl,
                0,
                totalBytes,
                0
        ));

        try (InputStream inputStream = connection.getInputStream();
             OutputStream outputStream = Files.newOutputStream(tempFile)) {
            byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
            long downloadedBytes = 0;
            long lastEmitNanos = startNanos;
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                installController.checkCancelled();
                outputStream.write(buffer, 0, read);
                downloadedBytes += read;
                long now = System.nanoTime();
                long elapsedNanos = now - startNanos;
                double bytesPerSecond = elapsedNanos > 0
                        ? downloadedBytes * 1_000_000_000.0 / elapsedNanos
                        : 0;
                boolean completed = totalBytes > 0 && downloadedBytes >= totalBytes;
                if (completed || now - lastEmitNanos >= PROGRESS_EMIT_INTERVAL_NANOS) {
                    progressListener.onProgress(new PluginInstallProgress(
                            PluginInstallProgress.Stage.DOWNLOADING,
                            entry.id(),
                            installUrl,
                            downloadedBytes,
                            totalBytes,
                            bytesPerSecond
                    ));
                    lastEmitNanos = now;
                }
            }
        } catch (java.util.concurrent.CancellationException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        } catch (Exception e) {
            Files.deleteIfExists(tempFile);
            if (installController.isCancelled()) {
                throw new java.util.concurrent.CancellationException("Plugin installation cancelled");
            }
            throw e;
        } finally {
            if (connection instanceof HttpURLConnection httpConnection) {
                installController.detachConnection(httpConnection);
            }
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

    private static void validateCompatibility(PluginDescriptor descriptor) {
        // 安装阶段先拦截不兼容插件，避免出现“安装成功但重启后被运行时跳过”的假成功状态。
        PluginCompatibility compatibility = PluginRuntime.evaluateCompatibility(descriptor);
        if (compatibility.compatible()) {
            return;
        }
        throw new IllegalStateException(buildCompatibilityErrorMessage(descriptor, compatibility));
    }

    private static String buildCompatibilityErrorMessage(PluginDescriptor descriptor, PluginCompatibility compatibility) {
        return "Plugin " + descriptor.id() + " " + descriptor.version()
                + " is not compatible with current EasyPostman. "
                + "App range: " + versionRange(compatibility.minAppVersion(), compatibility.maxAppVersion())
                + ", current app: " + compatibility.currentAppVersion()
                + "; plugin platform range: " + versionRange(compatibility.minPlatformVersion(), compatibility.maxPlatformVersion())
                + ", current platform: " + compatibility.currentPlatformVersion();
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

    private static String versionRange(String minVersion, String maxVersion) {
        String min = minVersion == null || minVersion.isBlank() ? "*" : minVersion;
        String max = maxVersion == null || maxVersion.isBlank() ? "*" : maxVersion;
        return min + " - " + max;
    }
}
