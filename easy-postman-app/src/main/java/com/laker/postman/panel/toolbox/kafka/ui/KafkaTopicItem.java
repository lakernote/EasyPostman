package com.laker.postman.panel.toolbox.kafka.ui;

public record KafkaTopicItem(String name, int partitionCount) {
    @Override
    public String toString() {
        return name;
    }
}
