package com.laker.postman.service.update.source;

/**
 * GitHub 更新源实现
 */
public class GitHubUpdateSource extends AbstractUpdateSource {

    private static final String API_URL = "https://api.github.com/repos/lakernote/easy-postman/releases/latest";
    private static final String ALL_RELEASES_API_URL = "https://api.github.com/repos/lakernote/easy-postman/releases";
    private static final String WEB_URL = "https://github.com/lakernote/easy-postman/releases";
    private static final String NAME = "GitHub";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getApiUrl() {
        return API_URL;
    }

    @Override
    public String getAllReleasesApiUrl() {
        return ALL_RELEASES_API_URL;
    }

    @Override
    public String getWebUrl() {
        return WEB_URL;
    }
}

