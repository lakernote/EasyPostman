package com.laker.postman.model;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

public enum GitOperation {
    COMMIT(MessageKeys.GIT_OPERATION_COMMIT),
    PUSH(MessageKeys.GIT_OPERATION_PUSH),
    PULL(MessageKeys.GIT_OPERATION_PULL);

    private final String displayNameKey;

    GitOperation(String displayNameKey) {
        this.displayNameKey = displayNameKey;
    }

    public String getDisplayName() {
        return I18nUtil.getMessage(displayNameKey);
    }
}
