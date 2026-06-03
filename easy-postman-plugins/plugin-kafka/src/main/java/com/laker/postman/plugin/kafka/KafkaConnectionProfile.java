package com.laker.postman.plugin.kafka;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
class KafkaConnectionProfile {
    String id;
    String name;
    String bootstrapServers;
    String clientId;
    String securityProtocol;
    String saslMechanism;
    String username;
    String password;
}
