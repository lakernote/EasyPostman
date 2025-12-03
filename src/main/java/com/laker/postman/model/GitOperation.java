package com.laker.postman.model;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import java.awt.*;

@Getter
public enum GitOperation {
    COMMIT(I18nUtil.getMessage(MessageKeys.GIT_OPERATION_COMMIT), "icons/git-commit.svg", ModernColors.GIT_COMMIT),
    PUSH(I18nUtil.getMessage(MessageKeys.GIT_OPERATION_PUSH), "icons/git-push.svg", ModernColors.GIT_PUSH),
    PULL(I18nUtil.getMessage(MessageKeys.GIT_OPERATION_PULL), "icons/git-pull.svg", ModernColors.GIT_PULL);

    private final String displayName;
    private final String iconName;
    private final Color color;

    GitOperation(String displayName, String iconName, Color color) {
        this.displayName = displayName;
        this.iconName = iconName;
        this.color = color;
    }
}