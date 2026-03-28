package com.laker.postman.logging;

import ch.qos.logback.core.PropertyDefinerBase;
import com.laker.postman.util.AppRuntimeLayout;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 根据运行形态解析日志目录，确保便携版日志写入程序所在目录下的 logs。
 */
public class LogHomePropertyDefiner extends PropertyDefinerBase {

    @Override
    public String getPropertyValue() {
        Path logDir = AppRuntimeLayout.logRootDirectory(LogHomePropertyDefiner.class);
        try {
            Files.createDirectories(logDir);
        } catch (Exception ignored) {
            // 目录创建失败时交由 logback 在后续初始化阶段报告具体错误。
        }
        return logDir.toString();
    }
}
