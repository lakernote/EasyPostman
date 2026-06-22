package com.laker.postman.service.sync;

import com.laker.postman.http.runtime.okhttp.OkHttpClientManager;
import com.laker.postman.util.JsonUtil;
import com.laker.postman.util.SystemUtil;
import okhttp3.OkHttpClient;
import okhttp3.HttpUrl;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class WebDavSyncService {
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 60_000;
    private static final int WRITE_TIMEOUT_MS = 60_000;

    private final Path dataRoot;
    private final WebDavSnapshotService snapshotService;
    private final WebDavClientFactory clientFactory;

    public WebDavSyncService() {
        this(
                Path.of(SystemUtil.getEasyPostmanPath()),
                new WebDavSnapshotService(),
                WebDavSyncService::createRuntimeClient
        );
    }

    WebDavSyncService(Path dataRoot,
                      WebDavSnapshotService snapshotService,
                      WebDavClientFactory clientFactory) {
        this.dataRoot = Objects.requireNonNull(dataRoot, "dataRoot").toAbsolutePath().normalize();
        this.snapshotService = Objects.requireNonNull(snapshotService, "snapshotService");
        this.clientFactory = Objects.requireNonNull(clientFactory, "clientFactory");
    }

    public void testConnection(WebDavSyncSettings settings) throws IOException {
        createClient(validate(settings)).testConnection();
    }

    public Optional<WebDavRemoteSnapshot> fetchRemoteSnapshot(WebDavSyncSettings settings) throws IOException {
        WebDavClient client = createClient(validate(settings));
        Optional<byte[]> manifest = client.downloadManifestIfPresent();
        if (manifest.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(WebDavRemoteSnapshot.fromJson(new String(manifest.get(), StandardCharsets.UTF_8)));
        } catch (RuntimeException e) {
            throw new IOException("Invalid WebDAV manifest", e);
        }
    }

    public void uploadSnapshot(WebDavSyncSettings settings) throws IOException {
        WebDavSyncSettings validatedSettings = validate(settings);
        Path snapshot = Files.createTempFile("easypostman-webdav-upload-", ".zip");
        try {
            snapshotService.createSnapshot(dataRoot, snapshot);
            long snapshotBytes = Files.size(snapshot);
            WebDavClient client = createClient(validatedSettings);
            client.testConnection();
            client.uploadSnapshot(snapshot);
            client.uploadManifest(createManifest(snapshotBytes));
        } finally {
            Files.deleteIfExists(snapshot);
        }
    }

    public WebDavRestoreResult restoreSnapshot(WebDavSyncSettings settings) throws IOException {
        WebDavClient client = createClient(validate(settings));
        Path snapshot = Files.createTempFile("easypostman-webdav-restore-", ".zip");
        try {
            client.downloadSnapshot(snapshot);
            return snapshotService.restoreSnapshot(snapshot, dataRoot);
        } finally {
            Files.deleteIfExists(snapshot);
        }
    }

    private WebDavClient createClient(WebDavSyncSettings settings) {
        return clientFactory.create(
                settings.serverUrl(),
                settings.remoteDirectory(),
                settings.username(),
                settings.password()
        );
    }

    private static WebDavClient createRuntimeClient(String serverUrl,
                                                    String remoteDirectory,
                                                    String username,
                                                    String password) {
        OkHttpClient okHttpClient = OkHttpClientManager.getClientForUrl(
                serverUrl,
                true,
                CONNECT_TIMEOUT_MS,
                READ_TIMEOUT_MS,
                WRITE_TIMEOUT_MS
        );
        return new WebDavClient(okHttpClient, serverUrl, remoteDirectory, username, password);
    }

    private static WebDavSyncSettings validate(WebDavSyncSettings settings) {
        WebDavSyncSettings normalized = settings == null
                ? new WebDavSyncSettings(false, "", WebDavSyncSettings.DEFAULT_REMOTE_DIRECTORY, "", "")
                : settings;
        if (!normalized.hasEndpoint()) {
            throw new IllegalArgumentException("WebDAV server URL is required");
        }
        HttpUrl parsedUrl = HttpUrl.parse(normalized.serverUrl());
        if (parsedUrl == null || !isHttpScheme(parsedUrl.scheme())) {
            throw new IllegalArgumentException("Invalid WebDAV server URL");
        }
        return normalized;
    }

    private static boolean isHttpScheme(String scheme) {
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private static String createManifest(long snapshotBytes) {
        ObjectNode root = JsonUtil.createJsonNode();
        root.put("schemaVersion", 1);
        root.put("createdAt", Instant.now().toString());
        root.put("appVersion", SystemUtil.getCurrentVersion());
        root.put("snapshotFile", "snapshot.zip");
        root.put("snapshotBytes", snapshotBytes);
        return JsonUtil.toJsonPrettyStr(root);
    }

    @FunctionalInterface
    interface WebDavClientFactory {
        WebDavClient create(String serverUrl, String remoteDirectory, String username, String password);
    }
}
