package com.laker.postman.panel.toolbox;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
class ElasticsearchConnectionProfile {
    String id;
    String name;
    String baseUrl;
    boolean authEnabled;
    String username;
    String password;
    @Singular("hostHistoryItem")
    List<String> hostHistory;
}
