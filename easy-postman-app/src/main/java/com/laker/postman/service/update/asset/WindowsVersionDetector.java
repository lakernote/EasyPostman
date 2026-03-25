package com.laker.postman.service.update.asset;

import com.laker.postman.util.AppRuntimeLayout;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Windows 版本检测器 - 判断是便携版还是安装版
 */
@Slf4j
@UtilityClass
public class WindowsVersionDetector {

    /**
     * 检测是否为便携版
     *
     * <p>判断依据：</p>
     * <ol>
     *   <li>检查应用根目录是否存在 .portable 标识文件（优先级最高）</li>
     *   <li>检查是否从 Program Files 或 AppData 目录运行（安装版特征）</li>
     *   <li>默认认为是安装版</li>
     * </ol>
     *
     * @return true 表示便携版，false 表示安装版
     */
    public static boolean isPortableVersion() {
        try {
            boolean portable = AppRuntimeLayout.isPortableMode(WindowsVersionDetector.class);
            log.info("Detected Windows package mode: {}", portable ? "portable" : "installed");
            return portable;
        } catch (Exception e) {
            log.warn("Failed to detect Windows version type: {}", e.getMessage());
            return false;
        }
    }
}
