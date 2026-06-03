package com.laker.postman.plugin.redis;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
class RedisConnectionProfile {
    String id;
    String name;
    String host;
    int port;
    int database;
    String username;
    String password;
    @Singular("hostHistoryItem")
    List<String> hostHistory;
}
