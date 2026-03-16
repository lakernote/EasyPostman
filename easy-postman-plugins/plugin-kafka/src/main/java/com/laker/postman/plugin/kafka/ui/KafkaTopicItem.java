package com.laker.postman.plugin.kafka.ui;

public record KafkaTopicItem(String name, int partitionCount) {
    @Override
    public String toString() {
        return name;
    }
}
