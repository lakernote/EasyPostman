package com.laker.postman.model;

public enum GitOperation {
    COMMIT("提交", "save.svg"),
    PUSH("推送", "upload.svg"),
    PULL("拉取", "download.svg");

    private final String displayName;
    private final String iconName;

    GitOperation(String displayName, String iconName) {
        this.displayName = displayName;
        this.iconName = iconName;
    }

    public String getDisplayName() {
        return displayName;
    }


    public String getIconName() {
        return iconName;
    }
}