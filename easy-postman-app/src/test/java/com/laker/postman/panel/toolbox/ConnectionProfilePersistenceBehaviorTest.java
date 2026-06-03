package com.laker.postman.panel.toolbox;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class ConnectionProfilePersistenceBehaviorTest {

    @Test(description = "数据库工具连接成功不应自动保存连接配置，必须由用户点击保存触发")
    public void successfulConnectShouldNotPersistConnectionProfiles() throws Exception {
        String redisPanel = source("easy-postman-plugins/plugin-redis/src/main/java/com/laker/postman/plugin/redis/RedisPanel.java");
        String kafkaPanel = source("easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/KafkaPanel.java");
        String elasticsearchPanel = source("easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/ElasticsearchPanel.java");
        String influxDbPanel = source("easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/InfluxDbPanel.java");

        assertFalse(redisPanel.contains("saveConnectionProfile(host, port, db, user, pass, false);"));
        assertFalse(kafkaPanel.contains("connectionProfileStore.upsertProfile(profileToSave);"));
        assertFalse(elasticsearchPanel.contains("saveConnectionProfile(finalUrl, user, pass, false);"));
        assertFalse(influxDbPanel.contains("saveConnectionProfile(finalBaseUrl, false);"));
    }

    @Test(description = "连接后断开按钮使用短文案，避免工具栏按钮被截断")
    public void disconnectButtonLabelsShouldBeShortInChinese() throws Exception {
        Properties appMessages = properties("easy-postman-app/src/main/resources/messages_zh.properties");
        Properties redisMessages = properties("easy-postman-plugins/plugin-redis/src/main/resources/redis-messages_zh.properties");
        Properties kafkaMessages = properties("easy-postman-plugins/plugin-kafka/src/main/resources/kafka-messages_zh.properties");

        assertEquals(appMessages.getProperty("toolbox.es.disconnect"), "断开");
        assertEquals(appMessages.getProperty("toolbox.influx.disconnect"), "断开");
        assertEquals(redisMessages.getProperty("toolbox.redis.disconnect"), "断开");
        assertEquals(kafkaMessages.getProperty("toolbox.kafka.disconnect"), "断开");
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(projectRoot().resolve(relativePath));
    }

    private static Properties properties(String relativePath) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(projectRoot().resolve(relativePath), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("easy-postman-app/pom.xml"))
                    && Files.exists(current.resolve("easy-postman-plugins"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot find easy-postman project root");
    }
}
