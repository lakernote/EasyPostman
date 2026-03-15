package com.laker.postman.service.update.source;

/**
 * Gitee 更新源实现
 */
public class GiteeUpdateSource extends AbstractUpdateSource {

    private static final String API_URL = "https://gitee.com/api/v5/repos/lakernote/easy-postman/releases/latest";
    private static final String ALL_RELEASES_API_URL = "https://gitee.com/api/v5/repos/lakernote/easy-postman/releases";
    private static final String WEB_URL = "https://gitee.com/lakernote/easy-postman/releases";
    private static final String NAME = "Gitee";

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
