package com.laker.postman.platform.update.model;

import lombok.Getter;

@Getter
public enum UpdateTarget {
    APP("app"),
    PLUGIN("plugin");

    private final String id;

    UpdateTarget(String id) {
        this.id = id;
    }
}
