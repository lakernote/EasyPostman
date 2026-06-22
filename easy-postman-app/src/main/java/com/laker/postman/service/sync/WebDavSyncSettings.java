package com.laker.postman.service.sync;

public record WebDavSyncSettings(
        boolean enabled,
        String serverUrl,
        String remoteDirectory,
        String username,
        String password
) {
    public static final String DEFAULT_REMOTE_DIRECTORY = "EasyPostman";

    public WebDavSyncSettings {
        serverUrl = normalize(serverUrl);
        remoteDirectory = normalize(remoteDirectory);
        if (remoteDirectory.isEmpty()) {
            remoteDirectory = DEFAULT_REMOTE_DIRECTORY;
        }
        username = username == null ? "" : username.trim();
        password = password == null ? "" : password;
    }

    public boolean hasEndpoint() {
        return !serverUrl.isBlank() && !remoteDirectory.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
