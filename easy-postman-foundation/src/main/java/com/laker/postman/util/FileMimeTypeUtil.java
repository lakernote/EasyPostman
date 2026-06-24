package com.laker.postman.util;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.net.URLConnection;
import java.nio.file.Files;

/**
 * 文件 MIME 类型检测工具类。
 */
@UtilityClass
public class FileMimeTypeUtil {

    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    public static File toFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        return new File(filePath.trim());
    }

    public static boolean isReadableRegularFile(String filePath) {
        return isReadableRegularFile(toFile(filePath));
    }

    public static boolean isReadableRegularFile(File file) {
        return file != null && file.isFile() && file.canRead();
    }

    public static String detectMimeType(String filePath) {
        return detectMimeType(toFile(filePath));
    }

    public static String detectMimeType(File file) {
        if (!isReadableRegularFile(file)) {
            return DEFAULT_MIME_TYPE;
        }
        try {
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType != null && !mimeType.isBlank()) {
                return mimeType;
            }
        } catch (Exception ignored) {
        }
        String guessedMimeType = URLConnection.guessContentTypeFromName(file.getName());
        return guessedMimeType != null && !guessedMimeType.isBlank() ? guessedMimeType : DEFAULT_MIME_TYPE;
    }
}
