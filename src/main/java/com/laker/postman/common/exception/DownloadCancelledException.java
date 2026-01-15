package com.laker.postman.common.exception;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import java.io.IOException;

/**
 * 下载取消异常
 * 当用户主动取消下载时抛出此异常，表示这是正常的用户行为，而非错误
 */
public class DownloadCancelledException extends IOException {

    public DownloadCancelledException() {
        super(I18nUtil.getMessage(MessageKeys.DOWNLOAD_CANCELLED));
    }

    public DownloadCancelledException(String message) {
        super(message);
    }
}

