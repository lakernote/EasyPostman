package com.laker.postman.model;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import java.awt.*;

@Getter
public enum GitOperation {
    COMMIT(I18nUtil.getMessage(MessageKeys.GIT_OPERATION_COMMIT), "save.svg", new Color(34, 139, 34)),
    PUSH(I18nUtil.getMessage(MessageKeys.GIT_OPERATION_PUSH), "upload.svg", new Color(30, 144, 255)),
    PULL(I18nUtil.getMessage(MessageKeys.GIT_OPERATION_PULL), "download.svg", new Color(216, 209, 160));

    private final String displayName;
    private final String iconName;
    private final Color color;

    GitOperation(String displayName, String iconName, Color color) {
        this.displayName = displayName;
        this.iconName = iconName;
        this.color = color;
    }
}