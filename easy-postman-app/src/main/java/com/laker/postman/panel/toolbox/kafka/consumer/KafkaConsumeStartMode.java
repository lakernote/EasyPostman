package com.laker.postman.panel.toolbox.kafka.consumer;

public enum KafkaConsumeStartMode {
    LATEST("latest", false),
    EARLIEST("earliest", false),
    NONE("none", false),
    TIMESTAMP("latest", true),
    OFFSET("latest", true);

    private final String autoOffsetReset;
    private final boolean requiresStartValue;

    KafkaConsumeStartMode(String autoOffsetReset, boolean requiresStartValue) {
        this.autoOffsetReset = autoOffsetReset;
        this.requiresStartValue = requiresStartValue;
    }

    public String autoOffsetReset() {
        return autoOffsetReset;
    }

    public boolean requiresStartValue() {
        return requiresStartValue;
    }
}
