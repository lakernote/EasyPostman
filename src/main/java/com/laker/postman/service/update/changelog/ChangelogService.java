package com.laker.postman.service.update.changelog;

import cn.hutool.json.JSONArray;
import com.laker.postman.service.update.source.UpdateSource;
import com.laker.postman.service.update.source.UpdateSourceSelector;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 更新日志服务 - 负责获取和格式化更新日志
 */
@Slf4j
public class ChangelogService {

    /**
     * -- GETTER --
     *  获取更新源选择器（用于获取 Web URL）
     */
    @Getter
    private final UpdateSourceSelector sourceSelector;
    private final ChangelogFormatter formatter;
    private static final int DEFAULT_LIMIT = 10;

    public ChangelogService() {
        this.sourceSelector = new UpdateSourceSelector();
        this.formatter = new ChangelogFormatter();
    }

    /**
     * 获取格式化的更新日志
     *
     * @return 格式化后的更新日志文本，失败返回 null
     */
    public String getChangelog() {
        return getChangelog(DEFAULT_LIMIT);
    }

    /**
     * 获取格式化的更新日志
     *
     * @param limit 限制返回的版本数量
     * @return 格式化后的更新日志文本，失败返回 null
     */
    public String getChangelog(int limit) {
        // 选择最佳更新源
        UpdateSource primarySource = sourceSelector.selectBestSource();
        JSONArray releases = primarySource.fetchAllReleases(limit);

        if (releases == null) {
            // 尝试备用源
            UpdateSource fallbackSource = sourceSelector.getFallbackSource(primarySource);
            log.info("Primary source failed, trying fallback source: {}", fallbackSource.getName());
            releases = fallbackSource.fetchAllReleases(limit);
        }

        if (releases == null) {
            return null;
        }

        return formatter.format(releases);
    }

}

