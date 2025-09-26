package com.laker.postman.model;

import lombok.Getter;

import java.awt.*;

@Getter
public enum GitOperation {
    COMMIT("提交", "save.svg", new Color(34, 139, 34)),
    PUSH("推送", "upload.svg", new Color(30, 144, 255)),
    PULL("拉取", "download.svg", new Color(216, 209, 160));

    private final String displayName;
    private final String iconName;
    private final Color color;

    GitOperation(String displayName, String iconName, Color color) {
        this.displayName = displayName;
        this.iconName = iconName;
        this.color = color;
    }
}