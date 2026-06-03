package com.laker.postman.panel.toolbox;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
class InfluxDbConnectionProfile {
    String id;
    String name;
    String baseUrl;
    String mode;
    String token;
    String org;
    String database;
    String measurement;
    String username;
    String password;
    @Singular("hostHistoryItem")
    List<String> hostHistory;
}
