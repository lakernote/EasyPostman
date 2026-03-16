package com.laker.postman.model;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import java.awt.*;

@Getter
public enum GitOperation {
    COMMIT(I18nUtil.getMessage(MessageKeys.GIT_OPERATION_COMMIT), "icons/git-commit.svg", new Color(34, 197, 94)),
    PUSH(I18nUtil.getMessage(MessageKeys.GIT_OPERATION_PUSH), "icons/git-push.svg", new Color(59, 130, 246)),
    PULL(I18nUtil.getMessage(MessageKeys.GIT_OPERATION_PULL), "icons/git-pull.svg", new Color(168, 85, 247));

    private final String displayName;
    private final String iconName;
    private final Color color;

    GitOperation(String displayName, String iconName, Color color) {
        this.displayName = displayName;
        this.iconName = iconName;
        this.color = color;
    }
}
