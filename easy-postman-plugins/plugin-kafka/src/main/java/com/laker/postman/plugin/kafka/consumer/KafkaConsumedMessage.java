package com.laker.postman.plugin.kafka.consumer;

public record KafkaConsumedMessage(
        String receiveTime,
        String recordTime,
        String topic,
        int partition,
        long offset,
        String key,
        String headers,
        String value) {
}
